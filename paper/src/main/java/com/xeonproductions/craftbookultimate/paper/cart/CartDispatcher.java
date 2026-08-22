// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.cart.DispatchedCartVisit;
import com.xeonproductions.craftbookultimate.core.cart.Stations;
import com.xeonproductions.craftbookultimate.core.cart.Wiring;
import com.xeonproductions.craftbookultimate.core.cart.mechanic.CartMechanics;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Minecart;
import org.jspecify.annotations.NullMarked;

/**
 * Runs every cart mechanic that applies where a cart has arrived.
 *
 * <p>One place that resolves the mechanism, reads its wiring and fans out, rather than each
 * mechanic watching carts for itself. A cart crossing one block sends a dozen move events, and
 * resolving the same three blocks once per mechanic per event would be the most expensive thing
 * on a busy railway.
 *
 * <p>Every applicable mechanic runs even after one has asked for the cart to be held, which is how
 * they behaved when each watched independently.
 */
@NullMarked
public final class CartDispatcher {

    /**
     * How far a cart may move in one tick and still be treated as having rolled there.
     *
     * <p>Squared, so two blocks. A rolling cart never comes near it, so this only rejects a cart
     * that has been teleported or has changed world, which is not something a mechanic on the
     * blocks it left should act on.
     */
    private static final double MAX_MOVE_DISTANCE_SQUARED = 2 * 2;

    private final Configuration configuration;
    private final Stations stations;
    private final RegionSchedulers schedulers;
    private final CartRecipes recipes;

    public CartDispatcher(
            Configuration configuration,
            Stations stations,
            RegionSchedulers schedulers,
            CartRecipes recipes) {
        this.configuration = configuration;
        this.stations = stations;
        this.schedulers = schedulers;
        this.recipes = recipes;
    }

    /** Where every rider has said they are going. */
    public Stations stations() {
        return stations;
    }

    /** The recipes a cart crafter can make. */
    public CartRecipes recipes() {
        return recipes;
    }

    /**
     * Runs the mechanics where a cart has just moved to.
     *
     * @param minecart the cart that moved
     * @param from where it was
     * @param to where it now is
     */
    public void onMove(Minecart minecart, Location from, Location to) {
        if (from.getWorld() != to.getWorld()
                || from.distanceSquared(to) > MAX_MOVE_DISTANCE_SQUARED) {
            return;
        }

        boolean minor = from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();

        visitAt(minecart, to.getBlock(), minor, new Vec3d(from.getX(), from.getY(), from.getZ()))
                .ifPresent(CartDispatcher::runEveryMechanic);
    }

    /**
     * Runs a single mechanic where a cart is standing, for the moments no move reports.
     *
     * <p>Somebody climbing into a cart and a station's power coming on are both things that happen
     * to a cart that is not moving, so neither reaches the mechanics any other way.
     *
     * @param action what to do with the visit, once there is one
     */
    public void atCart(Minecart minecart, java.util.function.Consumer<CartVisit> action) {
        visitAt(minecart, minecart.getLocation().getBlock(), false, positionOf(minecart))
                .ifPresent(action);
    }

    /** Everything a mechanic needs, where a cart is standing. */
    public Optional<CartVisit> visitAt(Minecart minecart, Block at, boolean minor, Vec3d from) {
        Optional<CartMechanism> mechanism = CartMechanisms.atRail(at);
        if (mechanism.isEmpty()) {
            return Optional.empty();
        }

        World world = at.getWorld();
        if (!configuration.settings().allowsWorld(world.getName())) {
            return Optional.empty();
        }

        Wiring wiring = CartMechanisms.wiringOf(at, mechanism.get());
        return Optional.of(new DispatchedCartVisit(
                new BukkitCart(minecart),
                mechanism.get(),
                minor,
                from,
                wiring,
                new BukkitCartWorld(world, recipes),
                stations,
                configuration.settings(),
                schedulers.at(at.getLocation())));
    }

    /**
     * Runs every mechanic built here.
     *
     * <p>A mechanic asking for the cart to be held gets its way by having already stopped it: the
     * server reports a cart's move after the fact rather than offering it for approval, so
     * stopping the cart dead is the whole of holding it.
     */
    private static void runEveryMechanic(CartVisit visit) {
        for (CartMechanic mechanic : CartMechanics.all()) {
            if (mechanic.appliesTo(visit.mechanism(), visit.settings())) {
                mechanic.onCart(visit);
            }
        }
    }

    private static Vec3d positionOf(Minecart minecart) {
        Location at = minecart.getLocation();
        return new Vec3d(at.getX(), at.getY(), at.getZ());
    }
}
