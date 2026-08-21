// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.pipe;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.pipe.PipeWorld;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.ic.LegacyBlocks;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NullMarked;

/**
 * The world a pipe is followed through, on a real server.
 *
 * <p>Bound to one world, and only ever asked about blocks touching the pipe itself, so following
 * one never leaves the region that owns it.
 *
 * @param world the world the pipe is in
 */
@NullMarked
public record BukkitPipeWorld(World world) implements PipeWorld {

    @Override
    public Key blockAt(Vec3i position) {
        return at(position).getType().getKey();
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return world.isChunkLoaded(position.x() >> 4, position.z() >> 4);
    }

    @Override
    public Optional<com.xeonproductions.craftbookultimate.core.math.BlockFace> facingAt(
            Vec3i position) {
        BlockData data = at(position).getBlockData();
        return data instanceof Directional directional
                ? Directions.toDomain(directional.getFacing())
                : Optional.empty();
    }

    @Override
    public boolean holdsItemsAt(Vec3i position) {
        return at(position).getState(false) instanceof InventoryHolder;
    }

    @Override
    public Optional<SignLines> signOn(Vec3i position) {
        Block block = at(position);
        for (org.bukkit.block.BlockFace side : org.bukkit.block.BlockFace.values()) {
            if (!side.isCartesian() || side == org.bukkit.block.BlockFace.SELF) {
                continue;
            }
            Block beside = block.getRelative(side);
            Optional<SignLines> lines = Signs.at(beside).map(Signs::read);
            if (lines.isPresent() && holdsUp(beside, block)) {
                return lines;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Key> resolveItem(String written) {
        return LegacyBlocks.resolveItem(written);
    }

    /** Whether a sign is fixed to a particular block rather than merely near it. */
    private static boolean holdsUp(Block sign, Block block) {
        BlockData data = sign.getBlockData();
        if (data instanceof org.bukkit.block.data.type.WallSign wall) {
            return sign.getRelative(wall.getFacing().getOppositeFace()).equals(block);
        }
        return sign.getRelative(org.bukkit.block.BlockFace.DOWN).equals(block);
    }

    private Block at(Vec3i position) {
        return Positions.toBlock(world, position);
    }
}
