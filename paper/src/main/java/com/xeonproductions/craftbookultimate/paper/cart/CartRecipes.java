// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.cart.CartRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jspecify.annotations.NullMarked;

/**
 * The server's recipes, as a cart crafter needs them.
 *
 * <p>Read once and kept, because a cart rolling over a crafter cannot afford to walk the whole
 * recipe registry, and because the answer does not change while the server is running.
 *
 * <p>Only the shaped and shapeless recipes are here. Everything else — smelting, brewing, smithing
 * — needs a workstation and a wait, neither of which a cart has.
 */
@NullMarked
public final class CartRecipes {

    /** How many recipes a search reports before it stops listing them. */
    private static final int MAX_RESULTS = 100;

    private final Map<String, CartRecipe> byName;

    private CartRecipes(Map<String, CartRecipe> byName) {
        this.byName = byName;
    }

    /** Nothing at all, which is what a crafter sees before the recipes have been read. */
    public static CartRecipes empty() {
        return new CartRecipes(Map.of());
    }

    /**
     * Reads every recipe the server knows.
     *
     * <p>Where two recipes flatten to the same name the first read wins, so a datapack adding a
     * second way of making something does not quietly take over the signs naming the first.
     */
    public static CartRecipes readFrom(Server server) {
        Map<String, CartRecipe> found = new LinkedHashMap<>();
        Iterator<Recipe> recipes = server.recipeIterator();

        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            flatten(recipe).ifPresent(flattened -> found.putIfAbsent(flattened.name(), flattened));
        }
        return new CartRecipes(Map.copyOf(found));
    }

    /** The recipe a sign names, if there is one. */
    public Optional<CartRecipe> byName(String signName) {
        return Optional.ofNullable(byName.get(signName.toLowerCase(Locale.ROOT)));
    }

    /** How many recipes a crafter can make. */
    public int size() {
        return byName.size();
    }

    /**
     * The names matching a search, best matches first.
     *
     * <p>An exact match, then the names beginning with it, then the names merely containing it,
     * which is the order somebody hunting for a name wants them in.
     */
    public List<String> search(String query) {
        String wanted = query.toLowerCase(Locale.ROOT);
        List<String> exact = new ArrayList<>();
        List<String> beginning = new ArrayList<>();
        List<String> containing = new ArrayList<>();

        for (String name : byName.keySet()) {
            if (name.equals(wanted)) {
                exact.add(name);
            } else if (name.startsWith(wanted)) {
                beginning.add(name);
            } else if (name.contains(wanted)) {
                containing.add(name);
            }
        }

        List<String> results = new ArrayList<>(exact);
        beginning.sort(null);
        containing.sort(null);
        results.addAll(beginning);
        results.addAll(containing);
        return results.size() > MAX_RESULTS ? results.subList(0, MAX_RESULTS) : results;
    }

    /**
     * Turns a recipe into what it consumes and what it makes.
     *
     * <p>The grid is thrown away: a cart has no grid, so two planks side by side and two planks
     * stacked are the same recipe here.
     */
    private static Optional<CartRecipe> flatten(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) {
            return Optional.empty();
        }

        List<RecipeChoice> choices;
        if (recipe instanceof ShapedRecipe shaped) {
            choices = new ArrayList<>();
            for (String row : shaped.getShape()) {
                for (char cell : row.toCharArray()) {
                    RecipeChoice choice = shaped.getChoiceMap().get(cell);
                    if (choice != null) {
                        choices.add(choice);
                    }
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            choices = shapeless.getChoiceList();
        } else {
            return Optional.empty();
        }

        Map<Key, Integer> ingredients = new HashMap<>();
        for (RecipeChoice choice : choices) {
            Optional<Key> item = firstOf(choice);
            if (item.isEmpty()) {
                // A choice nothing satisfies cannot be paid for, so the whole recipe is skipped
                // rather than being made available for less than it costs.
                return Optional.empty();
            }
            ingredients.merge(item.get(), 1, Integer::sum);
        }
        if (ingredients.isEmpty()) {
            return Optional.empty();
        }

        ItemStack result = recipe.getResult();
        return Optional.of(new CartRecipe(
                CartRecipe.signNameOf(keyed.getKey().toString()),
                ingredients,
                result.getType().getKey(),
                result.getAmount()));
    }

    /**
     * The item a choice is paid with.
     *
     * <p>A recipe taking any of a set — any plank, any log — is paid with the first of them, since
     * a cart cannot ask which the builder meant.
     */
    private static Optional<Key> firstOf(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice materials) {
            return materials.getChoices().isEmpty()
                    ? Optional.empty()
                    : Optional.of(materials.getChoices().getFirst().getKey());
        }
        if (choice instanceof RecipeChoice.ExactChoice exact) {
            return exact.getChoices().isEmpty()
                    ? Optional.empty()
                    : Optional.of(exact.getChoices().getFirst().getType().getKey());
        }
        // Anything else is a kind of choice this does not understand, so the recipe is left out
        // rather than being made available for an ingredient that might be wrong.
        return Optional.empty();
    }
}
