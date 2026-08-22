// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.meter;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.meter.Meters;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The two instruments a builder holds up to a block.
 *
 * <p>Charcoal reads how much redstone power a block carries; glowstone dust reads how much light
 * falls on the face that was clicked. One listener because they are the same gesture, and the item
 * in hand is the whole of what tells them apart.
 *
 * <p>Neither changes anything. Both cancel the click they answered, so reading a lever does not
 * also throw it.
 */
@NullMarked
public final class MeterListener implements Listener {

    private final Configuration configuration;

    public MeterListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }

        MechanicSettings settings = configuration.settings().mechanics();
        Player player = event.getPlayer();
        Material held = player.getInventory().getItemInMainHand().getType();
        Block clicked = event.getClickedBlock();

        if (held == materialOf(settings.ammeterItem())
                && settings.allows(Meters.NAME_AMMETER)
                && player.hasPermission(Meters.AMMETER_USE)) {
            player.sendMessage(Meters.power(clicked.getBlockPower()));
            event.setCancelled(true);
            return;
        }

        if (held == materialOf(settings.lightStoneItem())
                && settings.allows(Meters.NAME_LIGHT_STONE)
                && player.hasPermission(Meters.LIGHT_STONE_USE)) {
            // The face that was clicked rather than the block itself, since a solid block has no
            // light in it and the question is how bright the side you are looking at is.
            Block face = clicked.getRelative(event.getBlockFace());
            player.sendMessage(Meters.light(face.getLightLevel()));
            event.setCancelled(true);
        }
    }

    /** The material a block name means, or nothing where the server has no such item. */
    private static @Nullable Material materialOf(Key item) {
        NamespacedKey key = NamespacedKey.fromString(item.asString());
        return key == null ? null : Registry.MATERIAL.get(key);
    }
}
