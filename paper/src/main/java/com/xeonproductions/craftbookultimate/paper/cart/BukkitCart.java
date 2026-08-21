// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.cart.Cart;
import com.xeonproductions.craftbookultimate.core.cart.CartType;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitBystander;
import com.xeonproductions.craftbookultimate.paper.stock.ContainerStockpile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

/**
 * A minecart on a real server.
 *
 * <p>Wraps one entity and nothing else, so a mechanic acting on it only ever touches the cart in
 * front of it. That keeps every mechanic inside the region owning its own blocks.
 */
@NullMarked
public record BukkitCart(Minecart minecart) implements Cart {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Override
    public CartType type() {
        return typeOf(minecart);
    }

    /** What kind of cart an entity is. */
    public static CartType typeOf(Minecart minecart) {
        if (minecart instanceof StorageMinecart) {
            return CartType.CHEST;
        }
        if (minecart instanceof HopperMinecart) {
            return CartType.HOPPER;
        }
        if (minecart instanceof PoweredMinecart) {
            return CartType.FURNACE;
        }
        if (minecart instanceof ExplosiveMinecart) {
            return CartType.TNT;
        }
        if (minecart instanceof SpawnerMinecart) {
            return CartType.SPAWNER;
        }
        if (minecart instanceof CommandMinecart) {
            return CartType.COMMAND_BLOCK;
        }
        // A rideable minecart is the plain one, and is also the sensible answer for anything a
        // later version adds that this does not recognise.
        return CartType.RIDEABLE;
    }

    @Override
    public Vec3d position() {
        Location at = minecart.getLocation();
        return new Vec3d(at.getX(), at.getY(), at.getZ());
    }

    @Override
    public Vec3d velocity() {
        Vector going = minecart.getVelocity();
        return new Vec3d(going.getX(), going.getY(), going.getZ());
    }

    @Override
    public boolean setVelocity(Vec3d velocity) {
        if (!minecart.isValid()) {
            return false;
        }
        minecart.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z()));
        return true;
    }

    @Override
    public double maximumSpeed() {
        return minecart.getMaxSpeed();
    }

    @Override
    public List<Bystander> riders() {
        List<Bystander> riding = new ArrayList<>();
        for (Entity passenger : minecart.getPassengers()) {
            riding.add(new BukkitBystander(passenger));
        }
        return riding;
    }

    @Override
    public Optional<String> customName() {
        return Optional.ofNullable(minecart.customName()).map(PLAIN::serialize);
    }

    @Override
    public Optional<Stockpile> contents() {
        if (!(minecart instanceof InventoryHolder holder)) {
            return Optional.empty();
        }
        return Optional.of(new ContainerStockpile(holder.getInventory()));
    }

    @Override
    public Optional<ItemView> firstStoredItem() {
        if (!(minecart instanceof InventoryHolder holder)) {
            return Optional.empty();
        }
        org.bukkit.inventory.ItemStack first = holder.getInventory().getItem(0);
        return first == null ? Optional.empty() : BukkitBystander.viewOf(first);
    }

    @Override
    public boolean board(Bystander rider) {
        if (!minecart.isValid() || !(rider instanceof BukkitBystander riding)) {
            return false;
        }
        return minecart.addPassenger(riding.entity());
    }

    @Override
    public List<Bystander> ejectRiders() {
        if (!minecart.isValid()) {
            return List.of();
        }
        List<Bystander> aboard = riders();
        minecart.eject();
        return aboard;
    }

    @Override
    public boolean isPresent() {
        return minecart.isValid();
    }

    @Override
    public boolean remove() {
        if (!minecart.isValid()) {
            return false;
        }
        minecart.remove();
        return true;
    }

    @Override
    public boolean teleport(Vec3d to) {
        if (!minecart.isValid()) {
            return false;
        }
        Location where = minecart.getLocation();
        // Keeps the cart in its own world, which is the only one this thread may touch.
        minecart.teleport(new Location(where.getWorld(), to.x(), to.y(), to.z(),
                where.getYaw(), where.getPitch()));
        return true;
    }
}
