// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the lifts.
 *
 * <p>A lift builds nothing, so none of the building limits reach it. What is here is the two extra
 * ways of working one besides a hand on its sign, and how far it will look for a floor.
 *
 * @param jumping whether jumping and crouching work a lift
 * @param buttons whether a button in front of a lift's sign works it
 * @param tolerance how far a lift will drop somebody to find them a floor
 */
@NullMarked
public record ElevatorSettings(boolean jumping, boolean buttons, int tolerance) {

    /** The lifts as they have always been worked. */
    public static final ElevatorSettings DEFAULTS = new ElevatorSettings(true, true, 5);

    /** Holds the drop to something a lift can work with. */
    public ElevatorSettings {
        tolerance = Math.max(1, tolerance);
    }

    /** These settings with jump lifts allowed or refused. */
    public ElevatorSettings withJumping(boolean allowed) {
        return new ElevatorSettings(allowed, buttons, tolerance);
    }

    /** These settings with button lifts allowed or refused. */
    public ElevatorSettings withButtons(boolean allowed) {
        return new ElevatorSettings(jumping, allowed, tolerance);
    }

    /** These settings with lifts dropping somebody a different distance to find a floor. */
    public ElevatorSettings withTolerance(int drop) {
        return new ElevatorSettings(jumping, buttons, drop);
    }
}
