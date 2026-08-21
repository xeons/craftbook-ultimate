// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.pipe.PipeFilter;
import com.xeonproductions.craftbookultimate.core.pipe.PipeStyle;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.pipe.BukkitPipeWorld;
import com.xeonproductions.craftbookultimate.paper.pipe.PipeDispatcher;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Sets pipes going, and forgets what it knew about them when they are built or broken.
 *
 * <p>Three moments: power arriving at something that starts a pipe, a block changing anywhere a
 * pipe might have been, and a filter being written on a sign. A pipe has no ticking of its own —
 * it does nothing at all until it is powered.
 */
@NullMarked
public final class PipeListener implements Listener {

    /** The permission to write a filter on a pipe's sign. */
    public static final String BUILD = "craftbook.pipes";

    private final PipeDispatcher dispatcher;
    private final Configuration configuration;

    public PipeListener(PipeDispatcher dispatcher, Configuration configuration) {
        this.dispatcher = dispatcher;
        this.configuration = configuration;
    }

    /**
     * Sets a pipe going when power arrives at its input.
     *
     * <p>Only on the rising edge. Power leaving is what a piston does at the end of a pulse, and
     * carrying twice for one button press would empty a chest at twice the rate a builder asked
     * for.
     */
    @EventHandler(ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        if (event.getOldCurrent() > 0 || event.getNewCurrent() <= 0) {
            return;
        }
        Block block = event.getBlock();
        if (dispatcher.couldBeInput(block)) {
            dispatcher.run(block);
        }
    }

    /**
     * Forgets every pipe reaching into a chunk as it goes away.
     *
     * <p>What is kept is an answer rather than a picture of the world, so letting one go costs a
     * walk if that pipe is ever powered again and nothing at all if it is not. Without this a pipe
     * nobody visits again would hold its share of the index until the server stopped.
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        dispatcher.networks().forgetChunk(
                event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ());
    }

    /** Forgets every pipe in a world as it goes away. */
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        dispatcher.networks().forgetWorld(event.getWorld().getUID());
    }

    /** Forgets what was known about any pipe a placed block might belong to. */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        forgetAbout(event.getBlock());
    }

    /** Forgets what was known about any pipe a broken block belonged to. */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        forgetAbout(event.getBlock());
    }

    /**
     * Checks a filter as it is written.
     *
     * <p>A name that means nothing would otherwise leave a sorter quietly passing everything, and
     * the builder is standing right there with the means to fix it.
     */
    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (event.getSide() != Side.FRONT) {
            return;
        }

        SignLines lines = SignLines.of(event.lines());
        if (!PipeStyle.marksAFilter(lines) && !PipeStyle.marksAnExtractor(lines)) {
            return;
        }

        Player builder = event.getPlayer();
        if (!builder.hasPermission(BUILD)) {
            builder.sendMessage(Component.text(
                    "You may not build pipes.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        BukkitPipeWorld world = new BukkitPipeWorld(event.getBlock().getWorld());
        Optional<String> problem = PipeFilter.problemWith(lines, world::resolveItem);
        if (problem.isPresent()) {
            builder.sendMessage(Component.text(problem.get(), NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
        forgetAbout(event.getBlock());
    }

    private void forgetAbout(Block block) {
        if (!configuration.settings().pipes().enabled()) {
            return;
        }
        dispatcher.networks().forgetAbout(
                block.getWorld().getUID(), Positions.toDomain(block));
    }
}
