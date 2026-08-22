// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.copier;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.copier.SignClipboard;
import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

/**
 * Copying what one sign says onto another.
 *
 * <p>The odd one out among the copiers: there is no sign saying what it is and nothing is built.
 * What makes it work is an item — black dye, as the fork had it — and holding that item turns every
 * sign in the world into something you can copy from and paste onto.
 *
 * <p>Left-click a sign to copy what it says, right-click one to write that onto it. That way round
 * because pasting is the destructive half, and a right-click is the deliberate one: a builder
 * mining near a sign with dye in hand overwrites nothing.
 *
 * <p>What is remembered is per player and only while they are here. A clipboard is something
 * somebody is in the middle of doing, and one restored a week later would paste text its owner had
 * long forgotten copying.
 */
@NullMarked
public final class SignCopierListener implements Listener {

    /** What this mechanic is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.SIGN_COPIER;

    /** The permission to copy and paste a sign. */
    public static final String USE = "craftbook.signcopier.use";

    /** The item that does it, which is what the fork used. */
    public static final Material TOOL = Material.BLACK_DYE;

    private final Configuration configuration;
    private final SignClipboard clipboard;

    public SignCopierListener(Configuration configuration, SignClipboard clipboard) {
        this.configuration = configuration;
        this.clipboard = clipboard;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != TOOL
                || !configuration.settings().runsMechanicIn(
                        NAME, event.getClickedBlock().getWorld().getName())
                || !player.hasPermission(USE)) {
            return;
        }
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) {
            return;
        }

        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> {
                clipboard.put(player.getUniqueId(), Signs.read(sign));
                player.sendMessage(Component.text("Sign copied.", NamedTextColor.GREEN));
                event.setCancelled(true);
            }
            case RIGHT_CLICK_BLOCK -> paste(player, sign, event);
            default -> {
            }
        }
    }

    /** Forgets what somebody had copied as they leave. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clipboard.forget(event.getPlayer().getUniqueId());
    }

    private void paste(Player player, Sign sign, PlayerInteractEvent event) {
        clipboard.get(player.getUniqueId()).ifPresentOrElse(
                lines -> {
                    write(sign, lines);
                    player.sendMessage(Component.text("Sign pasted.", NamedTextColor.GREEN));
                    event.setCancelled(true);
                },
                () -> player.sendMessage(Component.text(
                        "You have not copied a sign yet. Left-click one first.",
                        NamedTextColor.RED)));
    }

    private static void write(Sign sign, SignLines lines) {
        Signs.write(sign, lines);
    }
}
