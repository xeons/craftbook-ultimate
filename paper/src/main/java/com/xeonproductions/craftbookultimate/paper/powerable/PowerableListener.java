// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.powerable;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.powerable.Powerable;
import com.xeonproductions.craftbookultimate.core.powerable.Powerables;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.ArrayList;
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

    /** How long to wait for the broken block to actually be gone before reading the power again. */
    private static final long AFTER_THE_BREAK = 1;

    /** How long to wait for a redstone change to reach the blocks around it. */
    private static final long ONCE_THE_WORLD_HAS_SETTLED = 1;

    private final Configuration configuration;
    private final RegionSchedulers schedulers;

    /**
     * The blocks left alone because the redstone feeding them was mined rather than switched off.
     *
     * <p>Breaking a redstone source raises the break first and the power change a moment later, on
     * the same tick. This remembers what the break said so the power change knows not to act on it,
     * which is the whole of how "power it, then mine the redstone" leaves a light burning.
     */
    private final Set<Block> spared = new HashSet<>();

    public PowerableListener(Configuration configuration, RegionSchedulers schedulers) {
        this.configuration = configuration;
        this.schedulers = schedulers;
    }

    /**
     * Answers a redstone source being mined out of the world.
     *
     * <p>Both halves of the setting are decided here, because the server will not decide either of
     * them for us. {@code BlockRedstoneEvent} is raised by a source when its own power changes —
     * a lever pulled, a repeater turning over, wire recalculated — and breaking one goes through
     * none of those paths. So a block left powered by something that has been mined away hears
     * nothing at all unless this says something.
     *
     * <p>Left alone by default, which is the old behaviour and the one builders use: power a
     * light, mine the redstone, keep the light. Where an operator has asked for the other, the
     * blocks beside are read again a tick later, once the broken block has actually gone and the
     * power they can see is the power that is left.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isPowerSource(event.getBlock().getType())
                || !configuration.settings()
                        .allowsWorld(event.getBlock().getWorld().getName())) {
            return;
        }

        MechanicSettings settings = configuration.settings().mechanics();

        List<Powerable> powerables = Powerables.all(settings);
        if (powerables.isEmpty()) {
            return;
        }

        Block broken = event.getBlock();
        if (!settings.depowerOnSourceRemoval()) {
            List<Block> sparing = new ArrayList<>(AROUND.size());
            for (BlockFace face : AROUND) {
                Block beside = broken.getRelative(face);
                if (Powerables.workingOn(powerables, keyOf(beside)).isPresent()) {
                    sparing.add(beside);
                }
            }
            if (sparing.isEmpty()) {
                return;
            }

            spared.addAll(sparing);
            // Sparing lasts exactly as long as the break that caused it. Emptying the set only
            // when a power change happened to arrive left entries behind for good when none did.
            schedulers.at(broken.getLocation()).runLater(
                    () -> spared.removeAll(sparing), AFTER_THE_BREAK);
            return;
        }

        schedulers.at(broken.getLocation()).runLater(() -> {
            for (BlockFace face : AROUND) {
                Block beside = broken.getRelative(face);
                Powerables.workingOn(powerables, keyOf(beside))
                        .ifPresent(powerable -> apply(powerable, beside));
            }
        }, AFTER_THE_BREAK);
    }

    /**
     * Turns whatever is beside a changed signal on or off.
     *
     * <p>The blocks are noted here and read a tick later, and that is not an optimisation — it is
     * the whole of what makes this work. {@code BlockRedstoneEvent} is raised by a source while it
     * is changing, before the world around it reflects the change, so asking a neighbour whether it
     * is powered during the event answers for the power that is going away rather than the power
     * that is arriving. Acting on that answer lights a lamp when its lever is switched off and
     * leaves it dark when it is switched on, which is exactly backwards.
     *
     * <p>Nothing is scheduled unless a block that cares is actually beside the change, so an
     * ordinary redstone machine costs six lookups and no task at all.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (event.getOldCurrent() == event.getNewCurrent()
                || !configuration.settings()
                        .allowsWorld(event.getBlock().getWorld().getName())) {
            return;
        }

        MechanicSettings settings = configuration.settings().mechanics();
        List<Powerable> powerables = Powerables.all(settings);
        if (powerables.isEmpty()) {
            return;
        }

        List<Block> changed = new ArrayList<>(AROUND.size());
        for (BlockFace face : AROUND) {
            Block beside = event.getBlock().getRelative(face);
            if (!spared.contains(beside)
                    && Powerables.workingOn(powerables, keyOf(beside)).isPresent()) {
                changed.add(beside);
            }
        }
        if (changed.isEmpty()) {
            return;
        }

        schedulers.at(event.getBlock().getLocation()).runLater(() -> {
            for (Block beside : changed) {
                if (spared.contains(beside)) {
                    continue;
                }
                Powerables.workingOn(powerables, keyOf(beside))
                        .ifPresent(powerable -> apply(powerable, beside));
            }
        }, ONCE_THE_WORLD_HAS_SETTLED);
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
