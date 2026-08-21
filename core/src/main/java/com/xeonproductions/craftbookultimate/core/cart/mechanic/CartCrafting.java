// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartRecipe;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The mechanic that crafts inside a passing cart.
 *
 * <p>A chest or hopper cart rolling over one is crafted from once, out of whatever it is already
 * carrying. Chain several and a cart of logs comes out the far end as a cart of chests.
 *
 * <p>There is no crafting grid in a cart, so the recipe is flattened to what it consumes and what
 * it makes. Two planks side by side and two planks stacked are the same recipe here.
 */
@NullMarked
public final class CartCrafting {

    /** The first of the two lines carrying the recipe's name. */
    private static final int FIRST_NAME_LINE = 2;

    /** The second line carrying the recipe's name, read straight on from the first. */
    private static final int SECOND_NAME_LINE = 3;

    private CartCrafting() {}

    /**
     * Crafts one of something into a passing cart.
     *
     * <p>Lines 3 and 4 hold the recipe's name, read as one, with its underscores taken out so a
     * long name fits: {@code goldenapple}, or {@code goldenc} and {@code arrot} across the two
     * lines.
     *
     * <p>Nothing happens unless the cart holds all of the ingredients and has room for the result,
     * so a cart that is short or full rolls on untouched.
     */
    public static CartMechanic crafter() {
        return new Crafter();
    }

    /** Crafts one of something into a passing cart. */
    private record Crafter() implements CartMechanic {

        @Override
        public String name() {
            return "Craft";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || !visit.mechanism().isNamed(name())) {
                return false;
            }

            Optional<Stockpile> hold = visit.cart().contents();
            if (hold.isEmpty()) {
                return false;
            }

            String written = visit.mechanism().line(FIRST_NAME_LINE)
                    + visit.mechanism().line(SECOND_NAME_LINE);
            Optional<CartRecipe> recipe = visit.world().recipeNamed(CartRecipe.signNameOf(written));
            if (recipe.isEmpty()) {
                return false;
            }

            craft(visit, hold.get(), recipe.get());
            return false;
        }

        /**
         * Makes one of the recipe, if the cart can pay for it and has somewhere to put it.
         *
         * <p>Everything is checked before anything is taken, so a cart that turns out to be one
         * plank short is not left having already lost the rest.
         */
        private static void craft(CartVisit visit, Stockpile hold, CartRecipe recipe) {
            for (Map.Entry<Key, Integer> ingredient : recipe.ingredients().entrySet()) {
                if (!hold.has(ingredient.getKey(), ingredient.getValue())) {
                    return;
                }
            }

            Map<Key, Integer> given = recipe.byproducts(visit.settings().carts().returnWaterBuckets());
            if (!hold.hasRoomFor(recipe.result(), recipe.resultCount())) {
                return;
            }

            for (Map.Entry<Key, Integer> ingredient : recipe.ingredients().entrySet()) {
                hold.take(ingredient.getKey(), ingredient.getValue());
            }
            hold.give(recipe.result(), recipe.resultCount());
            given.forEach(hold::give);
        }
    }
}
