// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.paper.mechanic.MechanicDispatcher;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Works a sign mechanic when somebody puts a hand on it.
 *
 * <p>Only the main hand, so a click is not delivered twice, and only a right click, since that is
 * how every one of these mechanics has always been worked. A click a mechanic claims goes no
 * further, which is what stops somebody placing a block against a bridge's sign while opening it.
 */
@NullMarked
public final class MechanicInteractListener implements Listener {

    private final MechanicDispatcher dispatcher;

    public MechanicInteractListener(MechanicDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        @Nullable Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        if (dispatcher.onInteract(
                clicked, Optional.ofNullable(event.getInteractionPoint()), event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
