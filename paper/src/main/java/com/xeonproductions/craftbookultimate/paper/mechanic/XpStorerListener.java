// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.mechanic.XpStorers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The block that turns the experience somebody is carrying into bottles.
 *
 * <p>The arithmetic is in {@link XpStorers}; what is here is reading a player's experience, which
 * only a server can do, and handing back what the bottles did not pay for.
 *
 * <p>The fork's automatic mode — an {@code [XP]} sign and a chest above the block, gathering loose
 * experience orbs from a distance — is not here. It is off by default there too, so a server
 * behaves the same out of the box; what is missing is an operator's ability to switch it on.
 */
@NullMarked
public final class XpStorerListener implements Listener {

    private final Configuration configuration;

    public XpStorerListener(Configuration configuration) {
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
        if (!settings.allows(XpStorers.NAME)) {
            return;
        }

        Material block = materialOf(settings);
        if (block == null || event.getClickedBlock().getType() != block) {
            return;
        }

        Player player = event.getPlayer();
        if (!settings.xpSneakState().passes(player.isSneaking())
                || !player.hasPermission(XpStorers.USE)) {
            return;
        }
        event.setCancelled(true);

        bottle(player, settings);
    }

    /** Fills as many bottles as the player's experience pays for, and keeps the change. */
    private static void bottle(Player player, MechanicSettings settings) {
        int experience = player.calculateTotalExperiencePoints();
        int perBottle = settings.xpPerBottle();

        int held = settings.xpRequiresBottle()
                ? player.getInventory().all(Material.GLASS_BOTTLE).values().stream()
                        .mapToInt(ItemStack::getAmount).sum()
                : Integer.MAX_VALUE;

        if (settings.xpRequiresBottle() && held == 0) {
            player.sendMessage(Component.text(
                    "You need an empty bottle for this.", NamedTextColor.RED));
            return;
        }

        int bottles = XpStorers.bottlesFor(experience, perBottle, held);
        if (bottles == 0) {
            player.sendMessage(Component.text(
                    "You do not have enough experience to fill a bottle.", NamedTextColor.RED));
            return;
        }

        // Made before anything is taken, so somebody with no room loses neither their experience
        // nor their bottles.
        ItemStack made = new ItemStack(Material.EXPERIENCE_BOTTLE, bottles);
        if (!player.getInventory().addItem(made).isEmpty()) {
            player.sendMessage(Component.text(
                    "You have no room for them.", NamedTextColor.RED));
            return;
        }

        if (settings.xpRequiresBottle()) {
            player.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, bottles));
        }

        // What will not pay for a whole bottle stays with the player. Setting the total outright
        // is what keeps the change: taking levels off would round somewhere else.
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        player.giveExp(XpStorers.remainderAfter(experience, perBottle, bottles));

        player.sendMessage(Component.text(
                "Filled " + bottles + (bottles == 1 ? " bottle." : " bottles."),
                NamedTextColor.GREEN));
    }

    /** The block this works on, or nothing where the server has no such block. */
    private static @Nullable Material materialOf(MechanicSettings settings) {
        NamespacedKey key = NamespacedKey.fromString(settings.xpStorerBlock().asString());
        return key == null ? null : Registry.MATERIAL.get(key);
    }
}
