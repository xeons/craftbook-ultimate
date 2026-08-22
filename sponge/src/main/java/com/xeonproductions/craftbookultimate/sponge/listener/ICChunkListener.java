// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.listener;

import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import com.xeonproductions.craftbookultimate.sponge.ic.ICManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

/**
 * Starting chips as their blocks arrive, and letting them go as those blocks leave.
 *
 * <p>A chip is only ever loaded from a sign already in the world, which is why nothing here reviews
 * what is written: a rule added after a sign was built must not invalidate it. What a loaded chip
 * cannot do is said on the sign instead, by the title being written red.
 *
 * <p>Both halves listen to the events that hand over a whole chunk rather than the ones that hand
 * over its blocks. A sign is a block entity, and the blocks-only events fire while the chunk is
 * still being assembled — asking one of those for its block entities gets an empty chunk that
 * throws rather than an empty answer. {@link ChunkEvent.Load} is the one that fires on the server
 * thread once the chunk is finished and ready to tick, which is also the only point at which
 * starting a chip is safe.
 */
@NullMarked
public final class ICChunkListener {

    private final ICManager manager;

    public ICChunkListener(ICManager manager) {
        this.manager = manager;
    }

    @Listener
    public void onChunkLoad(ChunkEvent.Load event) {
        Optional<ServerWorld> world = worldOf(event);
        if (world.isEmpty()) {
            return;
        }

        // Copied before anything is loaded, because starting a chip writes to the world and the
        // chunk's own collection is no place to be standing when that happens.
        List<BlockEntity> signs = new ArrayList<>();
        for (BlockEntity entity : event.chunk().blockEntities()) {
            if (entity instanceof Sign) {
                signs.add(entity);
            }
        }

        for (BlockEntity sign : signs) {
            manager.load(world.get(), Positions.toDomain(sign.serverLocation()));
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
