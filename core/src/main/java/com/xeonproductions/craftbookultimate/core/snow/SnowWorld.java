// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.snow;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import org.jspecify.annotations.NullMarked;

/**
 * The world as the snow sees it.
 *
 * <p>Depth rather than blocks: a place with no snow is nought, a layer is one to seven, and a full
 * block of snow is eight. That is the one idea the whole mechanic turns on, and stating it here
 * means the piling, melting and slumping can all be written as arithmetic and tested without a
 * server.
 */
@NullMarked
public interface SnowWorld {

    /** A full block of snow, and the most any one place holds. */
    int FULL = 8;

    /** How deep the snow is somewhere: nought for none, up to {@link #FULL}. */
    int depthAt(Vec3i at);

    /** Puts snow somewhere, or clears it where the depth is nought. */
    void setDepth(Vec3i at, int depth);

    /** Whether a place is empty enough for snow to fall through or settle in. */
    boolean isClear(Vec3i at);

    /** Whether snow could rest on top of whatever is at a place. */
    boolean canRestOn(Vec3i at);

    /** Whether a place holds water that snow above it could freeze. */
    boolean isWater(Vec3i at);

    /** Turns water to ice. */
    void freeze(Vec3i at);

    /** Whether it is cold enough here for snow to gather. */
    boolean isFreezing(Vec3i at);

    /** Whether it is warm enough here for snow to go away. */
    boolean isWarm(Vec3i at);

    /** Whether the sky can be seen from a place, which is what lets snow fall and melt. */
    boolean seesSky(Vec3i at);

    /** The lowest a block may be in this world, so nothing walks off the bottom of it. */
    int floor();

    /** The highest a block may be. */
    int ceiling();
}
