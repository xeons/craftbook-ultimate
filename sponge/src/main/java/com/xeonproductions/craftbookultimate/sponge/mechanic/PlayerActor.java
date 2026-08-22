// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.mechanic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.mechanic.Actor;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import com.xeonproductions.craftbookultimate.sponge.adapter.Directions;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

/** Whoever set a mechanic off, where that was a person rather than redstone. */
@NullMarked
public record PlayerActor(ServerPlayer player) implements Actor {

    @Override
    public String name() {
        return player.name();
    }

    @Override
    public void tell(Component message) {
        player.sendMessage(message);
    }

    @Override
    public boolean mayUse(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean isSneaking() {
        return player.get(Keys.IS_SNEAKING).orElse(false);
    }

    @Override
    public Optional<Vec3i> position() {
        return Optional.of(Positions.toDomain(player.position()));
    }

    @Override
    public boolean moveTo(Landing landing) {
        Optional<ServerWorld> target = Sponge.server().worldManager().worlds().stream()
                .filter(world -> world.uniqueId().equals(landing.world()))
                .findFirst();
        if (target.isEmpty()) {
            return false;
        }

        // Whatever they are riding is what moves, so the whole stack arrives together.
        Entity vehicle = rootVehicle();
        if (vehicle.isRemoved()) {
            return false;
        }

        vehicle.setLocation(arrivalAt(target.get(), landing));
        vehicle.setRotation(rotationFor(landing));
        return true;
    }

    /**
     * Where somebody lands.
     *
     * <p>The middle of the block across and the bottom of it up, so they arrive standing on the
     * floor rather than inside it.
     */
    private static ServerLocation arrivalAt(ServerWorld world, Landing landing) {
        return ServerLocation.of(
                world,
                new Vector3d(
                        landing.block().x() + 0.5, landing.block().y(), landing.block().z() + 0.5));
    }

    /** Which way they end up looking, or the way they already were where the landing says nothing. */
    private Vector3d rotationFor(Landing landing) {
        if (landing.facing() == BlockFace.SELF) {
            return player.rotation();
        }
        return new Vector3d(0, Directions.yawOf(landing.facing()), 0);
    }

    private Entity rootVehicle() {
        Entity root = player;
        Optional<Entity> vehicle = root.get(Keys.VEHICLE);
        while (vehicle.isPresent()) {
            root = vehicle.get();
            vehicle = root.get(Keys.VEHICLE);
        }
        return root;
    }
}
