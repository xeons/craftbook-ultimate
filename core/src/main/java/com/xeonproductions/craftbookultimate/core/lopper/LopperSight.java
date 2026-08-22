// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.lopper;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * As much of the world as a lopper run needs to see.
 *
 * <p>Two questions, because a run is only ever a walk over block names. Nothing here can change
 * anything: what to take is decided in full before the first block is broken, which is what lets
 * the decision be exercised against a world written in a test.
 */
@NullMarked
public interface LopperSight {

    /** What block is at a place. */
    Key blockAt(Vec3i position);

    /**
     * Whether a place may be read at all.
     *
     * <p>A tree at the edge of the loaded world stops at that edge rather than dragging the next
     * chunk in, and on a regionised server a run never reaches past what its own thread owns.
     */
    default boolean isReadable(Vec3i position) {
        return true;
    }
}
