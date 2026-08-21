// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Starts and stops chips as the chunks holding their signs come and go.
 *
 * <p>A chip only exists while its sign is loaded, so this is what brings a world's existing
 * circuitry back to life after a restart, and what stops chips ticking once nobody is nearby.
 */
@NullMarked
public final class ICChunkListener implements Listener {

    private final ICManager manager;

    public ICChunkListener(ICManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        manager.loadAll(signsIn(event.getChunk()));
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        manager.unloadChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    /**
     * The sign blocks in a chunk.
     *
     * <p>Reads the chunk's captured states rather than its live ones, which avoids loading the
     * blocks individually just to find out which are signs.
     */
    private static List<Block> signsIn(Chunk chunk) {
        List<Block> signs = new ArrayList<>();
        for (BlockState state : chunk.getTileEntities(false)) {
            if (state instanceof Sign) {
                signs.add(state.getBlock());
            }
        }
        return signs;
    }
}
