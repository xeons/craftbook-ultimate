// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.powerable;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.powerable.Powerable;
import com.xeonproductions.craftbookultimate.core.powerable.Powerables;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The blocks that answer redstone without anything being built.
 *
 * <p>A glowstone, a carved pumpkin or a piece of netherrack next to a redstone signal. There is no
 * sign and no index of where they are, so they are found by looking at what is beside whatever
 * changed — which is why every one of these hangs off the redstone event rather than off a
 * dispatcher.
 *
 * <p>What each does is in {@link Powerables}; what is here is reading the power and writing the
 * block.
 */
@NullMarked
public final class PowerableListener implements Listener {

    /** The faces a redstone change can reach a block through, which is all six. */
    private static final List<BlockFace> AROUND = List.of(
            BlockFace.UP, BlockFace.DOWN,
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    private final Configuration configuration;

    /**
     * The blocks left alone because the redstone feeding them was mined rather than switched off.
     *
     * <p>Breaking a redstone source raises the break first and the power change a moment later, on
     * the same tick. This remembers what the break said so the power change knows not to act on it,
     * which is the whole of how "power it, then mine the redstone" leaves a light burning.
     */
    private final Set<Block> spared = new HashSet<>();

    public PowerableListener(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Notes what a break is about to take the power away from.
     *
     * <p>Only when an operator has left the default alone. Where they have asked for blocks to go
     * out when their source is mined, nothing is spared and the power change does the ordinary
     * thing.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        MechanicSettings settings = configuration.settings().mechanics();
        if (settings.depowerOnSourceRemoval() || !isPowerSource(event.getBlock().getType())) {
            return;
        }

        List<Powerable> powerables = Powerables.all(settings);
        if (powerables.isEmpty()) {
            return;
        }

        for (BlockFace face : AROUND) {
            Block beside = event.getBlock().getRelative(face);
            if (Powerables.workingOn(powerables, keyOf(beside)).isPresent()) {
                spared.add(beside);
            }
        }
    }

    /** Turns whatever is beside a changed signal on or off. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (event.getOldCurrent() == event.getNewCurrent()) {
            return;
        }

        MechanicSettings settings = configuration.settings().mechanics();
        List<Powerable> powerables = Powerables.all(settings);
        if (powerables.isEmpty()) {
            spared.clear();
            return;
        }

        for (BlockFace face : AROUND) {
            Block beside = event.getBlock().getRelative(face);
            if (spared.remove(beside)) {
                continue;
            }
            Powerables.workingOn(powerables, keyOf(beside))
                    .ifPresent(powerable -> apply(powerable, beside));
        }
        spared.clear();
    }

    /**
     * Makes a block match whether power is reaching it.
     *
     * <p>The power is read off the block itself rather than taken from the event, because a block
     * with two sources beside it stays on while either is on and the event only speaks for one of
     * them.
     */
    private static void apply(Powerable powerable, Block block) {
        boolean powered = block.isBlockIndirectlyPowered() || block.isBlockPowered();

        switch (powerable) {
            case Powerable.Swap swap -> {
                Material wanted = materialOf(swap.wanted(powered));
                if (wanted == null || block.getType() == wanted) {
                    return;
                }
                // Kept so a carved pumpkin does not turn to face north the moment it is lit.
                BlockData was = block.getBlockData();
                block.setType(wanted, false);
                if (was instanceof Directional facing
                        && block.getBlockData() instanceof Directional now
                        && now.getFaces().contains(facing.getFacing())) {
                    now.setFacing(facing.getFacing());
                    block.setBlockData(now, false);
                }
            }
            case Powerable.Fire fire -> {
                Block above = block.getRelative(BlockFace.UP);
                if (powered && above.getType() == Material.AIR) {
                    above.setType(Material.FIRE, true);
                } else if (!powered && above.getType() == Material.FIRE) {
                    above.setType(Material.AIR, false);
                }
            }
        }
    }

    /** Whether breaking this block could take a redstone signal away with it. */
    private static boolean isPowerSource(Material material) {
        return material == Material.REDSTONE_WIRE
                || material == Material.REDSTONE_TORCH
                || material == Material.REDSTONE_WALL_TORCH
                || material == Material.REDSTONE_BLOCK
                || material == Material.LEVER
                || material == Material.REPEATER
                || material == Material.COMPARATOR
                || material == Material.DAYLIGHT_DETECTOR
                || material.name().endsWith("_BUTTON")
                || material.name().endsWith("_PRESSURE_PLATE");
    }

    private static Key keyOf(Block block) {
        return block.getType().getKey();
    }

    /** The material a block name means, or nothing where the server has no such block. */
    private static @Nullable Material materialOf(Key block) {
        NamespacedKey key = NamespacedKey.fromString(block.asString());
        return key == null ? null : Registry.MATERIAL.get(key);
    }
}
