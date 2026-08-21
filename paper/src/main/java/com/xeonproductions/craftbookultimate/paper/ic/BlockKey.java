// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;

/**
 * Identifies one block in one world.
 *
 * <p>Used as a map key for looking chips up by their sign or by their pins. The world is held by
 * id rather than by reference so that a key does not keep an unloaded world alive.
 *
 * @param world the world's unique id
 * @param x the block x coordinate
 * @param y the block y coordinate
 * @param z the block z coordinate
 */
@NullMarked
public record BlockKey(UUID world, int x, int y, int z) {

    /** The key for a block. */
    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    /** The key for a position in a world. */
    public static BlockKey of(World world, Vec3i position) {
        return new BlockKey(world.getUID(), position.x(), position.y(), position.z());
    }

    /** The position part of this key, without the world. */
    public Vec3i position() {
        return new Vec3i(x, y, z);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z + " in " + world;
    }
}
