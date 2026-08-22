// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.adapter;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

/** Places, said in both vocabularies. */
@NullMarked
public final class Positions {

    private Positions() {}

    public static Vec3i toDomain(Vector3i position) {
        return new Vec3i(position.x(), position.y(), position.z());
    }

    public static Vec3i toDomain(ServerLocation location) {
        return toDomain(location.blockPosition());
    }

    public static Vec3d toDomainExact(Vector3d position) {
        return new Vec3d(position.x(), position.y(), position.z());
    }

    /**
     * The block a point is in.
     *
     * <p>Floored rather than truncated, because a coordinate of -0.5 is in the block at -1 and
     * truncating would put it at 0 — one block out, and only ever on the negative side of an axis,
     * which is exactly the kind of fault that goes unnoticed until somebody builds west of spawn.
     */
    public static Vec3i toDomain(Vector3d position) {
        return new Vec3i(
                (int) Math.floor(position.x()),
                (int) Math.floor(position.y()),
                (int) Math.floor(position.z()));
    }

    public static Vector3i toServer(Vec3i position) {
        return new Vector3i(position.x(), position.y(), position.z());
    }

    public static Vector3d toServer(Vec3d position) {
        return new Vector3d(position.x(), position.y(), position.z());
    }

    public static ServerLocation toLocation(ServerWorld world, Vec3i position) {
        return ServerLocation.of(world, position.x(), position.y(), position.z());
    }

    public static ServerLocation toLocation(ServerWorld world, Vec3d position) {
        return ServerLocation.of(world, toServer(position));
    }

    /** The middle of a block, which is where anything put into the world belongs. */
    public static ServerLocation toCentre(ServerWorld world, Vec3i position) {
        return ServerLocation.of(
                world, new Vector3d(position.x() + 0.5, position.y() + 0.5, position.z() + 0.5));
    }
}
