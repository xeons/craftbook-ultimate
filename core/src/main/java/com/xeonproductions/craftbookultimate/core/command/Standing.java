// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * Where somebody is: which world, and where in it.
 *
 * <p>The world is its identifier rather than the world itself, so a command may be answered without
 * anything holding a world open, and so nothing here reaches across a region boundary.
 */
@NullMarked
public record Standing(UUID world, Vec3i at) {

    /**
     * How far this is from a place, for putting the nearest thing first.
     *
     * <p>Squared, so no root is taken. Somewhere in another world is further than anywhere in this
     * one, however many blocks away it is, which is what sorts a distant chip in the same world
     * above a near one somebody would have to travel to reach.
     */
    public long distanceTo(UUID world, Vec3i position) {
        if (!this.world.equals(world)) {
            return Long.MAX_VALUE;
        }
        long dx = (long) position.x() - at.x();
        long dy = (long) position.y() - at.y();
        long dz = (long) position.z() - at.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
