// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.physics;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.physics.FallingLadders;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Ladders that come down when what they stood on goes away.
 *
 * <p>Three things can leave a ladder with nothing beneath it — a block broken, a block placed, and
 * the game's own physics running — and all three are the same question asked of the same block.
 *
 * <p>The falling itself always happens a tick later, since no block may be written during a physics
 * event. When the <em>asking</em> happens differs by event, and it has to: during a break the block
 * being mined is still in the world, so a ladder asked then would see itself standing on something,
 * while a physics update has already landed and can be read straight away.
 *
 * <p>That difference is worth the extra parameter. A wall of ladders gets a physics update every
 * time anything near it changes, and scheduling a task for each of them to discover that nothing has
 * happened is the one way this mechanic could cost a server something.
 *
 * <p>Each rung that falls asks about the rung above it, so a column unzips itself upward one rung at
 * a time and every falling block lands on whatever the last one settled into.
 */
@NullMarked
public final class BetterPhysicsListener implements Listener {

    private final Configuration configuration;
    private final RegionSchedulers schedulers;

    public BetterPhysicsListener(Configuration configuration, RegionSchedulers schedulers) {
        this.configuration = configuration;
        this.schedulers = schedulers;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        // The mined block is still there, so whether the ladder above it is now unsupported
        // cannot be answered until the tick turns over.
        check(event.getBlock().getRelative(BlockFace.UP), false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent event) {
        check(event.getBlock().getRelative(BlockFace.UP), false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPhysics(BlockPhysicsEvent event) {
        // A physics update means the change has already landed, so this can be answered now — and
        // it has to be, since almost every one of these is a ladder that is still supported.
        check(event.getBlock(), true);
    }

    /**
     * Drops a ladder that has nothing under it any more, a tick from now.
     *
     * @param settled whether the world already reflects whatever changed
     */
    private void check(Block block, boolean settled) {
        if (block.getType() != Material.LADDER || !runsHere(block)) {
            return;
        }
        if (settled && !isUnsupported(block)) {
            return;
        }
        schedulers.at(block.getWorld(), Positions.toDomain(block)).runLater(() -> fall(block), 1);
    }

    /** Whether a ladder has nothing beneath it, asked of the world as it stands. */
    private static boolean isUnsupported(Block ladder) {
        return FallingLadders.falls(
                ladder.getType().getKey(),
                ladder.getRelative(BlockFace.DOWN).getType().getKey());
    }

    /** Turns one rung into a falling block, and asks about the rung above it. */
    private void fall(Block ladder) {
        if (!isUnsupported(ladder)) {
            return;
        }

        ladder.getWorld().spawnFallingBlock(
                ladder.getLocation().add(0.5, 0, 0.5), ladder.getBlockData());
        ladder.setType(Material.AIR, false);

        // The rung above has just lost what it stood on, and the world already says so.
        check(ladder.getRelative(BlockFace.UP), true);
    }

    private boolean runsHere(Block block) {
        Settings settings = configuration.settings();
        return settings.mechanics().fallingLadders()
                && settings.runsMechanicIn(FallingLadders.NAME, block.getWorld().getName());
    }
}
