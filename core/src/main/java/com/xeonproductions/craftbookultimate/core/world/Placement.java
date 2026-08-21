// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.world;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * How a block should be put down, for the few chips that care.
 *
 * <p>Almost every chip places a block the obvious way: in its default state, telling the
 * neighbours about it so that sand falls and water flows. The exceptions are worth naming rather
 * than passing flags about.
 *
 * @param facing which way the block should point, for the blocks that have a front
 * @param notifyNeighbours whether the surrounding blocks should be told the world changed
 */
@NullMarked
public record Placement(Optional<BlockFace> facing, boolean notifyNeighbours) {

    /** The ordinary way: default state, neighbours told. */
    public static final Placement NORMAL = new Placement(Optional.empty(), true);

    /**
     * Places without telling the neighbours.
     *
     * <p>Leaves the world looking changed without anything reacting to it, which is what a chip
     * putting up scenery wants: no sand falling, no water flowing, no redstone waking up.
     */
    public static final Placement SILENT = new Placement(Optional.empty(), false);

    /** Places a block pointing a particular way, telling the neighbours as usual. */
    public static Placement facing(BlockFace face) {
        return new Placement(Optional.of(face), true);
    }

    /** This placement, with the neighbours told or not as asked. */
    public Placement withNotifications(boolean notify) {
        return new Placement(facing, notify);
    }
}
