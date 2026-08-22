// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.powerable;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.powerable.LightSwitches;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

/**
 * The sign that turns every torch around it on or off at once.
 *
 * <p>A torch on top of the sign is both the switch and the sign of which way it is thrown. Clicking
 * the sign turns every torch within reach into the other kind, the one above it included, so the
 * indicator and the lights it stands for can never disagree.
 */
@NullMarked
public final class LightSwitchListener implements Listener {

    /** The permission to throw a light switch. */
    public static final String SWITCH_USE = "craftbook.lightswitch.use";

    /** The permission to build one. */
    public static final String SWITCH_BUILD = "craftbook.lightswitch";

    private final Configuration configuration;

    public LightSwitchListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }

        if (!configuration.settings().runsMechanicIn(
                LightSwitches.NAME, event.getClickedBlock().getWorld().getName())) {
            return;
        }

        MechanicSettings settings = configuration.settings().mechanics();
        if (event.getClickedBlock().getState() instanceof Sign sign
                && throwSwitch(event, sign, settings)) {
            event.setCancelled(true);
        }
    }

    /**
     * Turns every torch within reach of a light switch.
     *
     * <p>The torch above the sign is within reach of itself, so it turns with the rest and the
     * switch can never disagree with what it is switching.
     */
    private static boolean throwSwitch(
            PlayerInteractEvent event, Sign sign, MechanicSettings settings) {

        SignLines lines = Signs.read(sign);
        if (!LightSwitches.claims(lines.trimmedText(1))) {
            return false;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(SWITCH_USE)) {
            return false;
        }

        Block above = sign.getBlock().getRelative(BlockFace.UP);
        Material indicator = above.getType();
        if (indicator != Material.TORCH && indicator != Material.REDSTONE_TORCH
                && indicator != Material.WALL_TORCH
                && indicator != Material.REDSTONE_WALL_TORCH) {
            player.sendMessage(Component.text(
                    "That switch has no torch above it to say which way it is thrown.",
                    NamedTextColor.RED));
            return true;
        }

        boolean on = indicator == Material.REDSTONE_TORCH
                || indicator == Material.REDSTONE_WALL_TORCH;
        int turned = turnTorches(
                sign.getBlock(), on,
                LightSwitches.rangeOf(lines, settings),
                LightSwitches.lightsOf(lines, settings));

        player.sendMessage(Component.text(
                "Turned " + turned + (turned == 1 ? " light " : " lights ") + (on ? "off." : "on."),
                NamedTextColor.YELLOW));
        return true;
    }

    /**
     * Turns torches of one kind into the other, nearest the switch first.
     *
     * <p>Stops at the limit rather than one past it. The fork checked after turning one, so a
     * switch allowed twenty turned twenty-one.
     */
    private static int turnTorches(Block at, boolean on, int range, int limit) {
        Material looking = on ? Material.REDSTONE_TORCH : Material.TORCH;
        Material lookingWall = on ? Material.REDSTONE_WALL_TORCH : Material.WALL_TORCH;
        Material becomes = on ? Material.TORCH : Material.REDSTONE_TORCH;
        Material becomesWall = on ? Material.WALL_TORCH : Material.REDSTONE_WALL_TORCH;

        int turned = 0;
        for (Vec3i offset : LightSwitches.reach(range)) {
            if (turned >= limit) {
                return turned;
            }

            Block block = at.getRelative(offset.x(), offset.y(), offset.z());
            Material found = block.getType();
            if (found != looking && found != lookingWall) {
                continue;
            }

            // A wall torch keeps the wall it is on; a standing one has no facing to keep.
            BlockData was = block.getBlockData();
            block.setType(found == lookingWall ? becomesWall : becomes, false);
            if (was instanceof Directional facing
                    && block.getBlockData() instanceof Directional now
                    && now.getFaces().contains(facing.getFacing())) {
                now.setFacing(facing.getFacing());
                block.setBlockData(now, false);
            }
            turned++;
        }
        return turned;
    }

}
