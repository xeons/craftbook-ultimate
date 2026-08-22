// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.debug.DebugMode;
import com.xeonproductions.craftbookultimate.paper.debug.DebugActions;
import com.xeonproductions.craftbookultimate.paper.debug.DebugStick;
import com.xeonproductions.craftbookultimate.paper.ic.ICInstance;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Makes the debug stick work.
 *
 * <p>Two gestures. Right-clicking a chip's sign runs whatever mode the stick is in against that
 * chip; crouching and right-clicking the air moves the stick to the next mode its holder is allowed
 * to use.
 *
 * <p>Both cancel the event they came from, so a stick never places a block, never opens what it is
 * pointed at, and never puts a chip's sign into edit mode — which would be the fastest way to
 * destroy the thing being debugged.
 */
@NullMarked
public final class DebugStickListener implements Listener {

    private final ICManager manager;
    private final DebugStick sticks;
    private final DebugActions actions;

    public DebugStickListener(ICManager manager, DebugStick sticks, DebugActions actions) {
        this.manager = manager;
        this.sticks = sticks;
        this.actions = actions;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        // The off hand fires its own copy of this event, and acting on both would run every mode
        // twice for anybody holding two sticks.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack held = event.getItem();
        if (held == null || !sticks.isOne(held)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(DebugMode.PERMISSION)) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);
            useOn(player, held, event.getClickedBlock());
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR && player.isSneaking()) {
            event.setCancelled(true);
            cycle(player, held);
        }
    }

    /** Runs the stick's mode against whatever was clicked, if it is a chip. */
    private void useOn(Player player, ItemStack held, Block block) {
        DebugMode mode = sticks.modeOf(held);
        if (!player.hasPermission(mode.permission())) {
            player.sendMessage(Component.text(
                    "You may not use the " + mode.title() + " mode.", NamedTextColor.RED));
            return;
        }

        // A pipe has no sign and no chip behind it, so this mode is pointed at an ordinary block
        // and must not be turned away for not being a chip.
        if (mode.readsAnyBlock()) {
            actions.pipe(player, block);
            return;
        }

        Optional<ICInstance> chip = manager.at(block);
        if (chip.isEmpty()) {
            player.sendMessage(Component.text(
                    "There is no chip here. Point the stick at a chip's own sign.",
                    NamedTextColor.RED));
            return;
        }
        if (!actions.applies(mode, chip.get())) {
            player.sendMessage(Component.text(
                    "The " + mode.title() + " mode has nothing to say about a "
                            + chip.get().definition().name() + ".",
                    NamedTextColor.RED));
            return;
        }

        actions.run(mode, player, chip.get());
    }

    /** Moves the stick to the next mode its holder may use. */
    private void cycle(Player player, ItemStack held) {
        sticks.cycle(held, player).ifPresentOrElse(
                mode -> player.sendMessage(Component.text(mode.title(), NamedTextColor.GOLD)
                        .append(Component.text("  " + mode.description(), NamedTextColor.GRAY))),
                () -> player.sendMessage(Component.text(
                        "You may not use any other mode.", NamedTextColor.RED)));
    }
}
