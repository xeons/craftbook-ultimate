// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * How snow behaves, which is nothing at all until an operator asks for something.
 *
 * <p>Its own record rather than six more fields on {@link MechanicSettings}, because snow is not a
 * mechanic anybody builds. Like the vehicle habits it changes the world everywhere the moment it is
 * switched on, and like them every one of these starts off.
 *
 * <p>The pieces are separate because they are wanted separately. A server may want deep drifts
 * without wanting them to melt, or melting without the snow shifting about as it piles.
 *
 * @param piling whether snow keeps piling past the height the game stops at
 * @param dispersion whether a pile slumps into the lower ground beside it
 * @param freezesWater whether water under snow turns to ice
 * @param meltsInSunlight whether snow in the warm and under open sky goes away
 * @param partialMeltOnly whether melting stops at the height the game would have left it, rather
 *     than clearing the ground
 * @param snowballsPile whether a thrown snowball leaves snow where it lands
 */
@NullMarked
public record SnowSettings(
        boolean piling,
        boolean dispersion,
        boolean freezesWater,
        boolean meltsInSunlight,
        boolean partialMeltOnly,
        boolean snowballsPile) {

    /** Nothing switched on: snow behaves as the game runs it. */
    public static final SnowSettings DEFAULTS =
            new SnowSettings(false, false, false, false, false, false);

    /** Whether any of it is switched on, which is what decides if anything listens at all. */
    public boolean anythingAtAll() {
        return piling || dispersion || freezesWater || meltsInSunlight || snowballsPile;
    }
}
