// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import org.jspecify.annotations.NullMarked;

/**
 * The block that turns the experience somebody is carrying into bottles.
 *
 * <p>A monster spawner by default, though an operator may name another block. Clicking it hands
 * over as many bottles of enchanting as the experience will pay for and keeps the remainder, so
 * nothing is lost to rounding.
 *
 * <p>Kept despite the game having its own way to bottle experience, because this one is a fixture
 * a builder puts somewhere: an experience bank at a mob farm is a thing people build, and a villager
 * who happens to sell bottles is not the same thing.
 */
@NullMarked
public final class XpStorers {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.XP_STORER;

    /** The sign a builder may put on one. */
    public static final String SIGN_NAME = "[XP]";

    /** The permission to build one. */
    public static final String BUILD = "craftbook.xp-storer";

    /** The permission to use one. */
    public static final String USE = "craftbook.xp-storer.use";

    /** How much experience a bottle is worth, unless an operator says otherwise. */
    public static final int DEFAULT_PER_BOTTLE = 16;

    private XpStorers() {
    }

    /**
     * How many bottles an amount of experience pays for.
     *
     * @param experience how much the player is carrying
     * @param perBottle how much one bottle costs
     * @param bottlesHeld how many empty bottles they have, or {@link Integer#MAX_VALUE} where the
     *     mechanic does not ask for any
     */
    public static int bottlesFor(int experience, int perBottle, int bottlesHeld) {
        if (experience <= 0 || perBottle <= 0 || bottlesHeld <= 0) {
            return 0;
        }
        return Math.min(bottlesHeld, experience / perBottle);
    }

    /**
     * What is left over once the bottles are paid for.
     *
     * <p>Handed back rather than taken, which is the point of doing the arithmetic here at all: the
     * fork set the player's experience to zero and then gave back what it had worked out, so a
     * builder with nineteen points and a sixteen-point bottle lost the other three without being
     * told.
     */
    public static int remainderAfter(int experience, int perBottle, int bottles) {
        if (bottles <= 0) {
            return Math.max(0, experience);
        }
        return Math.max(0, experience - bottles * perBottle);
    }
}
