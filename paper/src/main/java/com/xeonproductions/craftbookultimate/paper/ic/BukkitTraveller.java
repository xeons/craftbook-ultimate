// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;

/**
 * A player, or anything else shaped like one, that a chip can move.
 *
 * <p>Whatever the traveller is riding is what actually moves, so someone in a minecart arrives
 * still in the minecart. Moving is asked of the server rather than done here, because the arrival
 * point may belong to another thread or another world, and only the server can carry an entity
 * across that boundary safely.
 */
@NullMarked
public record BukkitTraveller(HumanEntity entity) implements Traveller {

    @Override
    public Vec3i position() {
        return Positions.toDomain(entity.getLocation());
    }

    @Override
    public boolean moveTo(Landing landing) {
        World target = Bukkit.getWorld(landing.world());
        if (target == null) {
            return false;
        }

        Entity vehicle = rootVehicle();
        if (!vehicle.isValid()) {
            return false;
        }

        // Passengers come along with whatever they are riding, so moving the outermost vehicle
        // moves the whole stack.
        vehicle.teleportAsync(arrivalAt(target, landing), PlayerTeleportEvent.TeleportCause.PLUGIN);
        return true;
    }

    /** Where in the world a traveller is put down: on the block's floor, facing along its face. */
    private static Location arrivalAt(World world, Landing landing) {
        Location arrival = Positions.toCentre(world, landing.block());
        arrival.setY(landing.block().y());
        arrival.setYaw(Directions.yawOf(landing.facing()));
        arrival.setPitch(0);
        return arrival;
    }

    /**
     * The outermost thing this traveller is riding, or the traveller when they are riding nothing.
     *
     * <p>Moving a passenger out of its vehicle would leave the vehicle behind and the rider on
     * foot, so the whole stack is moved from the top.
     */
    private Entity rootVehicle() {
        Entity root = entity;
        Entity vehicle = root.getVehicle();
        while (vehicle != null) {
            root = vehicle;
            vehicle = root.getVehicle();
        }
        return root;
    }
}
