// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.adapter;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.BlockKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;

/** Converts between server positions and the platform-independent {@link Vec3i}. */
@NullMarked
public final class Positions {

    private Positions() {}

    /** The block position of a block. */
    public static Vec3i toDomain(Block block) {
        return new Vec3i(block.getX(), block.getY(), block.getZ());
    }

    /** The block position containing a location. */
    public static Vec3i toDomain(Location location) {
        return new Vec3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /** The block at a position in a world. */
    public static Block toBlock(World world, Vec3i position) {
        return world.getBlockAt(position.x(), position.y(), position.z());
    }

    /** The corner of the block at a position. */
    public static Location toLocation(World world, Vec3i position) {
        return new Location(world, position.x(), position.y(), position.z());
    }

    /** How a block is named where one has to be remembered rather than held. */
    public static BlockKey keyOf(Block block) {
        return new BlockKey(
                block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockKey keyOf(World world, Vec3i position) {
        return BlockKey.of(world.getUID(), position);
    }

    /** The centre of the block at a position, which is where entities should be placed. */
    public static Location toCentre(World world, Vec3i position) {
        return new Location(world, position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
    }
}
