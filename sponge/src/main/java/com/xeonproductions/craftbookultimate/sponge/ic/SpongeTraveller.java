// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import com.xeonproductions.craftbookultimate.sponge.adapter.Directions;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Humanoid;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

/** Somebody who can be moved from one place to another. */
@NullMarked
public record SpongeTraveller(Humanoid entity) implements Traveller {

    @Override
    public Vec3i position() {
        return Positions.toDomain(entity.position());
    }

    @Override
    public boolean moveTo(Landing landing) {
        Optional<ServerWorld> target = Sponge.server().worldManager().worlds().stream()
                .filter(world -> world.uniqueId().equals(landing.world()))
                .findFirst();
        if (target.isEmpty()) {
            return false;
        }

        // Passengers come along with whatever they are riding, so moving the outermost vehicle
        // moves the whole stack.
        Entity vehicle = rootVehicle();
        if (vehicle.isRemoved()) {
            return false;
        }

        vehicle.setLocation(arrivalAt(target.get(), landing));
        vehicle.setRotation(new Vector3d(0, Directions.yawOf(landing.facing()), 0));
        return true;
    }

    /**
     * Where somebody lands.
     *
     * <p>The middle of the block across, so they are not standing in a wall, and the bottom of it
     * up, so they are standing on the floor rather than falling into it.
     */
    private static ServerLocation arrivalAt(ServerWorld world, Landing landing) {
        return ServerLocation.of(
                world,
                new Vector3d(
                        landing.block().x() + 0.5, landing.block().y(), landing.block().z() + 0.5));
    }

    private Entity rootVehicle() {
        Entity root = entity;
        Optional<Entity> vehicle = root.get(Keys.VEHICLE);
        while (vehicle.isPresent()) {
            root = vehicle.get();
            vehicle = root.get(Keys.VEHICLE);
        }
        return root;
    }
}
