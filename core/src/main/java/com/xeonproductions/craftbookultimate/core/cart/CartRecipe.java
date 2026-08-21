// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A recipe as a cart crafter needs it: what goes in, and what comes out.
 *
 * <p>Flattened from whatever shape the recipe has on the crafting grid, because a cart has no
 * grid. Two planks side by side and two planks stacked are the same thing here, and so are the
 * shaped and shapeless ways of writing the same recipe.
 *
 * @param name the recipe's name with its underscores removed, which is what a sign carries
 * @param ingredients what it consumes, and how much of each
 * @param result what it makes
 * @param resultCount how many it makes
 */
@NullMarked
public record CartRecipe(String name, Map<Key, Integer> ingredients, Key result, int resultCount) {

    /** The ingredients that leave a container behind when they are used. */
    private static final Set<Key> EMPTIES_INTO_A_BUCKET = Set.of(
            Blocks.key("milk_bucket"), Blocks.key("lava_bucket"), Blocks.key("water_bucket"));

    /** What is left when a bucket is emptied into something. */
    private static final Key BUCKET = Blocks.key("bucket");

    /** What a water bucket may be given back as, when an operator is feeling generous. */
    private static final Key WATER_BUCKET = Blocks.key("water_bucket");

    /** Copies the ingredients so nothing can change a recipe after it has been read. */
    public CartRecipe {
        ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
    }

    /**
     * The name a sign would carry for this recipe.
     *
     * <p>Underscores are dropped because a sign has only fifteen characters a line and every one
     * of them counts, and the two lines are read as one name so that a longer recipe still fits.
     */
    public static String signNameOf(String recipeName) {
        String bare = recipeName.contains(":")
                ? recipeName.substring(recipeName.indexOf(':') + 1)
                : recipeName;
        return bare.replace("_", "").toLowerCase(Locale.ROOT);
    }

    /**
     * What crafting this gives back besides the result.
     *
     * <p>A recipe using a filled bucket leaves the bucket behind, the way it does on a crafting
     * table, so a cart making cake does not swallow three buckets a time.
     *
     * @param returnWaterBuckets whether a water bucket comes back full rather than empty, which is
     *     a kindness to anybody crafting in bulk and is a setting because it is not what vanilla
     *     does
     */
    public Map<Key, Integer> byproducts(boolean returnWaterBuckets) {
        Map<Key, Integer> given = new LinkedHashMap<>();
        for (Map.Entry<Key, Integer> ingredient : ingredients.entrySet()) {
            if (!EMPTIES_INTO_A_BUCKET.contains(ingredient.getKey())) {
                continue;
            }
            Key back = returnWaterBuckets && ingredient.getKey().equals(WATER_BUCKET)
                    ? WATER_BUCKET
                    : BUCKET;
            given.merge(back, ingredient.getValue(), Integer::sum);
        }
        return given;
    }
}
