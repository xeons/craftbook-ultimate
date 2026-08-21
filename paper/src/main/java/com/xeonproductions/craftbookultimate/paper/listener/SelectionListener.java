// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.paper.area.Selections;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Forgets the corners somebody had picked out when they leave.
 *
 * <p>A selection is a convenience for saving an area, not something anybody should come back to a
 * week later, so it goes with the player and the map does not grow with the server's uptime.
 */
@NullMarked
public final class SelectionListener implements Listener {

    private final Selections selections;

    public SelectionListener(Selections selections) {
        this.selections = selections;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selections.forget(event.getPlayer());
    }
}
