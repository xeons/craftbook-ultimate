// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.listener;

import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import com.xeonproductions.craftbookultimate.sponge.ic.ICManager;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

/**
 * Starting chips as their blocks arrive, and letting them go as those blocks leave.
 *
 * <p>A chip is only ever loaded from a sign already in the world, which is why nothing here reviews
 * what is written: a rule added after a sign was built must not invalidate it. What a loaded chip
 * cannot do is said on the sign instead, by the title being written red.
 */
@NullMarked
public final class ICChunkListener {

    private final ICManager manager;

    public ICChunkListener(ICManager manager) {
        this.manager = manager;
    }

    @Listener
    public void onChunkLoad(ChunkEvent.Blocks.Load event) {
        Optional<ServerWorld> world = worldOf(event);
        if (world.isEmpty()) {
            return;
        }

        // The event hands over the chunk's blocks alone, and a sign is a block entity, so the
        // loaded chunk is asked for from the world instead.
        Vector3i position = event.chunkPosition();
        WorldChunk chunk = world.get().chunk(position.x(), position.y(), position.z());

        for (BlockEntity entity : chunk.blockEntities()) {
            if (entity instanceof Sign) {
                manager.load(world.get(), Positions.toDomain(entity.serverLocation()));
            }
        }
    }

    /**
     * Lets chips go as their chunk leaves.
     *
     * <p>Before rather than after, because unloading a chip reads its own sign to tell its logic it
     * is going, and after the chunk has gone there is nothing there to read.
     */
    @Listener
    public void onChunkUnload(ChunkEvent.Unload.Pre event) {
        Optional<ServerWorld> world = worldOf(event);
        if (world.isEmpty()) {
            return;
        }

        Vector3i chunk = event.chunkPosition();
        manager.unloadChunk(world.get().uniqueId(), chunk.x(), chunk.z());
    }

    private static Optional<ServerWorld> worldOf(ChunkEvent.WorldScoped event) {
        return Sponge.server().worldManager().world(event.worldKey());
    }
}
