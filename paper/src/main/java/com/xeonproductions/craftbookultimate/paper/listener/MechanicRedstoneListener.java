// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.paper.mechanic.MechanicDispatcher;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Works the sign mechanics when the redstone beside them changes.
 *
 * <p>A mechanic follows the signal rather than counting changes in it: power arriving puts a
 * bridge out and closes a gate, power leaving pulls them back. That means a mechanic wired to a
 * lever always agrees with the lever, however it was left by the last person to work it by hand.
 */
@NullMarked
public final class MechanicRedstoneListener implements Listener {

    private final MechanicDispatcher dispatcher;

    public MechanicRedstoneListener(MechanicDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        boolean was = event.getOldCurrent() > 0;
        boolean now = event.getNewCurrent() > 0;

        // Only the moment power arrives or leaves matters. A signal changing strength while
        // staying on leaves every mechanic where it is.
        if (was == now) {
            return;
        }
        dispatcher.onRedstone(event.getBlock(), now);
    }
}
