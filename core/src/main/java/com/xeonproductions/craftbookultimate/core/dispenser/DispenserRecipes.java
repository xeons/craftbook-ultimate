// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.dispenser;

import com.xeonproductions.craftbookultimate.core.config.DispenserSettings;
import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Dispensers loaded in a pattern that makes them do something other than dispense.
 *
 * <p>How far a fan reaches and how hard a cannon throws are here rather than in the settings,
 * because they are what the machine <em>is</em> — a builder who has loaded a fan and found how far
 * it blows has learnt something that should still be true on the next server.
 */
@NullMarked
public final class DispenserRecipes {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.DISPENSER_RECIPES;

    /** How far a fan or a vacuum reaches, in blocks of open air. */
    public static final int DRAUGHT_REACH = 5;

    /** How fast a cannon throws its dynamite. */
    public static final double CANNON_SPEED = 2.0;

    /** How fast an arrow, a snowball or a bottle leaves the dispenser. */
    public static final double SHOT_SPEED = 1.5;

    /** How much of a lift everything shot out of a dispenser is given. */
    public static final double SHOT_RISE = 0.1;

    /** How long a fire arrow burns for, in ticks. */
    public static final int ARROW_BURN = 5000;

    private DispenserRecipes() {
    }

    /**
     * The machine a loaded dispenser is, or nothing where it is only a dispenser.
     *
     * @param loaded what is in each of the nine slots, with nothing for an empty one
     * @param settings which of the recipes an operator allows
     */
    public static Optional<DispenserRecipe> matching(
            List<@Nullable Key> loaded, DispenserSettings settings) {
        for (DispenserRecipe recipe : DispenserRecipe.values()) {
            if (settings.allows(recipe) && recipe.matches(loaded)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /**
     * How hard a fan or a vacuum pushes something a given distance away.
     *
     * <p>Strongest against the dispenser's face and weaker with every block, so a fan is something
     * to stand near rather than a wall of wind. Negative for a vacuum, which is the same push
     * turned round.
     */
    public static double draught(int blocksAway, boolean pulling) {
        double strength = Math.max(0, DRAUGHT_REACH - blocksAway);
        return pulling ? -strength : strength;
    }
}
