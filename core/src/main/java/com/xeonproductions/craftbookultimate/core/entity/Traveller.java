// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.entity;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import org.jspecify.annotations.NullMarked;

/**
 * Someone a chip can move from one place to another.
 *
 * <p>Only the player-like entities count. A chip that moves people is not meant to move the
 * livestock standing next to them, and mechanics that herd other entities do their own thing.
 *
 * <p>Whatever a traveller is riding goes with them, so a player in a minecart arrives still in
 * the minecart rather than being pulled out of it.
 */
@NullMarked
public interface Traveller {

    /** The block this traveller is standing in. */
    Vec3i position();

    /**
     * Sends this traveller somewhere.
     *
     * <p>Arriving may not be instantaneous: crossing to a place that belongs to another thread,
     * or to a world whose chunks are not loaded, takes as long as it takes. A true result means
     * the journey has started, not that it has finished.
     *
     * @param landing where to arrive and which way to look
     * @return true if the traveller was sent
     */
    boolean moveTo(Landing landing);
}
