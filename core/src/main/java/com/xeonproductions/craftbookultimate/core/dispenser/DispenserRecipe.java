// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.dispenser;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A dispenser loaded in a pattern, and what that pattern makes it do.
 *
 * <p>The nine slots of a dispenser are read as the three-by-three they are shown as, so a recipe
 * is written here in the shape a builder loads it in. Nothing is crafted and nothing is consumed
 * beyond one of every stack: the dispenser keeps its pattern and goes on doing the same thing
 * every time it is powered, which is what makes it a machine rather than a recipe.
 *
 * <p>An empty slot is written as air rather than as nothing, so a pattern is nine blocks with no
 * holes in it and there is one comparison rather than two.
 *
 * <p>All six come from the fork being ported. Upstream carries four of them and an item shooter
 * this does not; the two it lacks — the vacuum, and a fan that reaches five blocks rather than one
 * — are the fork's, and the fork's is the behaviour kept.
 */
@NullMarked
public enum DispenserRecipe {

    /** Throws a lit stick of dynamite, hard. */
    CANNON(
            "fire_charge", "gunpowder", "fire_charge",
            "gunpowder", "tnt", "gunpowder",
            "fire_charge", "gunpowder", "fire_charge"),

    /** Blows whatever is in front of it away, weaker the further off it is. */
    FAN(
            "cobweb", "oak_leaves", "cobweb",
            "oak_leaves", "piston", "oak_leaves",
            "cobweb", "oak_leaves", "cobweb"),

    /** Drags whatever is in front of it closer, weaker the further off it is. */
    VACUUM(
            "cobweb", "oak_leaves", "cobweb",
            "oak_leaves", "sticky_piston", "oak_leaves",
            "cobweb", "oak_leaves", "cobweb"),

    /** Shoots an arrow that is on fire. */
    FIRE_ARROWS(
            "air", "fire_charge", "air",
            "fire_charge", "arrow", "fire_charge",
            "air", "fire_charge", "air"),

    /** Shoots a snowball. */
    SNOW_SHOOTER(
            "air", "snow", "air",
            "snow", "potion", "snow",
            "air", "snow", "air"),

    /** Throws a bottle of experience. */
    XP_SHOOTER(
            "air", "redstone", "air",
            "redstone", "glass_bottle", "redstone",
            "air", "redstone", "air");

    /** How many slots a dispenser has. */
    public static final int SLOTS = 9;

    /** What each slot must hold, with air for a slot that must be empty. */
    private final List<Key> pattern;

    DispenserRecipe(String... items) {
        List<Key> slots = new ArrayList<>(SLOTS);
        for (String item : items) {
            slots.add(Key.key("minecraft", item));
        }
        this.pattern = List.copyOf(slots);
    }

    /** The name an operator switches this one off by. */
    public String settingName() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Whether a dispenser loaded like this is one of these.
     *
     * <p>Only what sort of thing is in each slot is looked at — never how many, nor its name, its
     * damage or its enchantments — so a machine built out of a stack of gunpowder goes on working
     * as that stack is used up.
     *
     * @param loaded what is in each of the nine slots, with air for an empty one
     */
    public boolean matches(List<Key> loaded) {
        return loaded.size() == SLOTS && pattern.equals(loaded);
    }

    /** What has to be in each slot, for a page that shows a builder how to load one. */
    public List<Key> pattern() {
        return pattern;
    }

    /** What an empty slot reads as. */
    public static Key empty() {
        return Blocks.AIR_KEY;
    }
}
