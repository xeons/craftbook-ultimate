// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.mechanic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.mechanic.Actor;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;

/**
 * A player working a mechanic by hand.
 *
 * <p>Moving is asked of the server rather than done here, because a lift may cross into a chunk
 * another thread owns. Whatever the player is riding goes with them, so somebody in a boat
 * arrives still in the boat.
 */
@NullMarked
public record PlayerActor(Player player) implements Actor {

    @Override
    public String name() {
        return player.getName();
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
        return player.isSneaking();
    }

    @Override
    public Optional<Vec3i> position() {
        return Optional.of(Positions.toDomain(player.getLocation()));
    }

    @Override
    public Set<Key> held() {
        Set<Key> holding = new LinkedHashSet<>(2);
        holding.add(player.getInventory().getItemInMainHand().getType().getKey());
        holding.add(player.getInventory().getItemInOffHand().getType().getKey());
        return holding;
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
        vehicle.teleportAsync(arrivalAt(target, landing), PlayerTeleportEvent.TeleportCause.PLUGIN);
        return true;
    }

    /**
     * Where the player is put down.
     *
     * <p>On the block's floor, in the middle of it. A landing that names no direction leaves them
     * looking the way they already were, which is what a lift wants: somebody stepping into a
     * shaft facing the door should step out of it facing the door.
     */
    private Location arrivalAt(World world, Landing landing) {
        Location arrival = Positions.toCentre(world, landing.block());
        arrival.setY(landing.block().y());
        if (landing.facing() == BlockFace.SELF) {
            arrival.setYaw(player.getLocation().getYaw());
            arrival.setPitch(player.getLocation().getPitch());
        } else {
            arrival.setYaw(Directions.yawOf(landing.facing()));
            arrival.setPitch(0);
        }
        return arrival;
    }

    /** The outermost thing the player is riding, or the player when they are riding nothing. */
    private Entity rootVehicle() {
        Entity root = player;
        Entity vehicle = root.getVehicle();
        while (vehicle != null) {
            root = vehicle;
            vehicle = root.getVehicle();
        }
        return root;
    }
}
