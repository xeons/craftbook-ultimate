// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * How every boat on the server behaves, wherever it is floating.
 *
 * <p>The counterpart of {@link CartHabits}, and off out of the box for the same reason: a habit
 * changes every boat in the world the moment it is switched on, with nothing built and nothing for
 * a player to see.
 *
 * <p>Three of these are the same habits the carts have, and they are deliberately their own
 * settings rather than shared ones. The legacy fork kept them apart, and a server wanting empty
 * carts cleared off its stations has said nothing at all about boats left on a lake.
 *
 * <p>The fork carried two more — boats that work on land, and boats with a speed and a
 * deceleration set on them. Neither is here, because neither can be done: the values those settings
 * write are dead on both platforms. See `FINDINGS.md`.
 *
 * @param decayEmptyAfter how many ticks a boat may sit empty before it is taken away, 0 to leave
 *     empty boats alone
 * @param decayOnlyAfterExit whether only a boat somebody has got out of decays, rather than every
 *     boat from the moment it is placed
 * @param removeOnExit whether a boat is taken away the moment its rider gets out
 * @param giveBoatBack whether taking it away hands the rider the boat back
 * @param waterPlaceOnly whether a boat may only be put down on water
 * @param runDownEntities whether an occupied boat hurts what it runs into
 * @param runDownOnlyHurts whether running something down stops at hurting it rather than removing it
 * @param runDownOtherBoats whether an occupied boat runs down other boats as well as creatures
 */
@NullMarked
public record BoatHabits(
        long decayEmptyAfter,
        boolean decayOnlyAfterExit,
        boolean removeOnExit,
        boolean giveBoatBack,
        boolean waterPlaceOnly,
        boolean runDownEntities,
        boolean runDownOnlyHurts,
        boolean runDownOtherBoats) {

    /** How long the legacy fork waited before taking an empty boat away. */
    public static final long CUSTOMARY_DECAY_TICKS = 40;

    /** Nothing switched on: boats behave as the game runs them. */
    public static final BoatHabits DEFAULTS =
            new BoatHabits(0, true, false, true, false, false, false, false);

    /** Holds the wait to something that means anything. */
    public BoatHabits {
        decayEmptyAfter = Math.max(0, decayEmptyAfter);
    }

    /** Whether a boat left sitting empty is ever taken away. */
    public boolean decaysEmptyBoats() {
        return decayEmptyAfter > 0;
    }

    /** These habits with empty boats decaying, or not. */
    public BoatHabits withDecay(long afterTicks, boolean onlyAfterExit) {
        return new BoatHabits(afterTicks, onlyAfterExit, removeOnExit, giveBoatBack, waterPlaceOnly,
                runDownEntities, runDownOnlyHurts, runDownOtherBoats);
    }

    /** These habits with a boat taken away when its rider leaves, or not. */
    public BoatHabits withExitRemoval(boolean remove, boolean giveBack) {
        return new BoatHabits(decayEmptyAfter, decayOnlyAfterExit, remove, giveBack, waterPlaceOnly,
                runDownEntities, runDownOnlyHurts, runDownOtherBoats);
    }

    /** These habits with boats kept off dry land, or not. */
    public BoatHabits withWaterPlaceOnly(boolean onlyOnWater) {
        return new BoatHabits(decayEmptyAfter, decayOnlyAfterExit, removeOnExit, giveBoatBack,
                onlyOnWater, runDownEntities, runDownOnlyHurts, runDownOtherBoats);
    }

    /** These habits with occupied boats hurting what they hit, or not. */
    public BoatHabits withRunDown(boolean runDown, boolean onlyHurts, boolean otherBoats) {
        return new BoatHabits(decayEmptyAfter, decayOnlyAfterExit, removeOnExit, giveBoatBack,
                waterPlaceOnly, runDown, onlyHurts, otherBoats);
    }
}
