// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.mechanic.CartDispensers;
import com.xeonproductions.craftbookultimate.core.cart.mechanic.CartSpeed;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.paper.cart.BukkitCart;
import com.xeonproductions.craftbookultimate.paper.cart.BukkitCartWorld;
import com.xeonproductions.craftbookultimate.paper.cart.CartBlocks;
import com.xeonproductions.craftbookultimate.paper.cart.CartDispatcher;
import com.xeonproductions.craftbookultimate.paper.cart.SpreadStockpile;
import java.util.List;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NullMarked;

/**
 * Sets the cart mechanics going as carts move and as people climb in and out.
 *
 * <p>A cart's own movement is the usual way a mechanic runs. The other two moments here are the
 * ones no move reports: somebody getting into a cart, which is what a launcher waits for, and
 * somebody leaving the server, whose destination there is no longer any point remembering.
 */
@NullMarked
public final class CartListener implements Listener {

    private final CartDispatcher dispatcher;
    private final Configuration configuration;

    public CartListener(CartDispatcher dispatcher, Configuration configuration) {
        this.dispatcher = dispatcher;
        this.configuration = configuration;
    }

    /** Runs the mechanics wherever a cart has rolled to. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (event.getVehicle() instanceof Minecart minecart) {
            dispatcher.onMove(minecart, event.getFrom(), event.getTo());
        }
    }

    /**
     * Sends a cart off when somebody climbs into it on a launcher.
     *
     * <p>Runs a tick later, because the passenger is not aboard until after this event and a
     * launcher's rules ask who is riding.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Minecart minecart) || !(event.getEntered() instanceof Player)) {
            return;
        }
        dispatcher.atCart(minecart, visit -> visit.scheduler()
                .runLater(() -> CartSpeed.launchOnMount(visit), 1));
    }

    /**
     * Puts a cart away when it runs into a dispenser's chest.
     *
     * <p>The other half of the dispenser: one chest hands carts out as people arrive and takes
     * them back as they leave, so a station never runs out and never silts up.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleBlockCollision(VehicleBlockCollisionEvent event) {
        if (!(event.getVehicle() instanceof Minecart minecart)) {
            return;
        }
        Block hit = event.getBlock();
        if (!(hit.getState(false) instanceof InventoryHolder holder)) {
            return;
        }
        Optional<CartMechanism.MechanismSign> sign = signUnder(hit);
        if (sign.isEmpty() || !CartDispensers.isDispenser(sign.get(), configuration.settings())) {
            return;
        }

        CartDispensers.store(
                new BukkitCart(minecart),
                new SpreadStockpile(List.of(holder.getInventory())),
                new BukkitCartWorld(hit.getWorld(), dispatcher.recipes()));
    }

    /** The sign a dispenser's chest sits on. */
    private static Optional<CartMechanism.MechanismSign> signUnder(Block chest) {
        for (int down = 1; down <= 2; down++) {
            Block below = chest.getRelative(BlockFace.DOWN, down);
            if (CartBlocks.isSign(below)) {
                return CartBlocks.readSign(below);
            }
        }
        return Optional.empty();
    }

    /**
     * Forgets where somebody was going when they leave.
     *
     * <p>A destination only lasts a session, so keeping one for somebody who has logged out would
     * route them somewhere they set days ago the next time they got in a cart.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dispatcher.stations().clearDestination(event.getPlayer().getUniqueId());
    }
}
