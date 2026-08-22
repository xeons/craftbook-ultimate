// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.boat;

import com.xeonproductions.craftbookultimate.core.boat.Boat;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitBystander;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

/** A boat in the world, as the habits see one. */
@NullMarked
public record BukkitBoat(org.bukkit.entity.Boat boat) implements Boat {

    @Override
    public Vec3d position() {
        return new Vec3d(boat.getX(), boat.getY(), boat.getZ());
    }

    @Override
    public Vec3d velocity() {
        Vector moving = boat.getVelocity();
        return new Vec3d(moving.getX(), moving.getY(), moving.getZ());
    }

    @Override
    public List<Bystander> riders() {
        List<Bystander> aboard = new ArrayList<>();
        for (Entity passenger : boat.getPassengers()) {
            aboard.add(new BukkitBystander(passenger));
        }
        return aboard;
    }

    @Override
    public boolean isPresent() {
        return boat.isValid();
    }
}
