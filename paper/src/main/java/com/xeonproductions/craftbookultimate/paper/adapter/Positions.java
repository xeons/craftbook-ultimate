package com.xeonproductions.craftbookultimate.paper.adapter;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
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

    /** The centre of the block at a position, which is where entities should be placed. */
    public static Location toCentre(World world, Vec3i position) {
        return new Location(world, position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
    }
}
