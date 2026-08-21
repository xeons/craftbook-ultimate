// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.cart.CartBehaviour;
import com.xeonproductions.craftbookultimate.core.config.CartHabits;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.cart.BukkitCart;
import com.xeonproductions.craftbookultimate.paper.cart.BukkitCartWorld;
import com.xeonproductions.craftbookultimate.paper.cart.CartDispatcher;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitBystander;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

/**
 * The habits an operator may give every cart on the server.
 *
 * <p>One listener for all seven because they overlap: three of them answer the same collision, two
 * the same dismount, and two the same move. None of them is a mechanism, so none of them goes
 * through {@link CartDispatcher}, which resolves a mechanic from a block and a sign.
 *
 * <p>Everything decided here is decided in {@link CartBehaviour}. What is left is asking the server
 * to carry it out.
 *
 * <p>All of it is off unless an operator has switched it on, so on an unconfigured server every
 * handler here leaves at its first line.
 */
@NullMarked
public final class CartHabitListener implements Listener {

    /** How long to wait before removing a cart somebody has stepped out of. */
    private static final long REMOVAL_DELAY_TICKS = 2;

    private final CartDispatcher dispatcher;
    private final Configuration configuration;
    private final RegionSchedulers schedulers;

    public CartHabitListener(
            CartDispatcher dispatcher, Configuration configuration, RegionSchedulers schedulers) {
        this.dispatcher = dispatcher;
        this.configuration = configuration;
        this.schedulers = schedulers;
    }

    /**
     * Keeps creatures out of carts.
     *
     * <p>Only riding is refused, so a creature may still be pushed along by one and a cart may
     * still be ridden by anybody who is a person.
     */
    @EventHandler(ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        CartHabits habits = habits();
        if (!habits.blockMobs() || !(event.getMount() instanceof Minecart)) {
            return;
        }
        if (!CartBehaviour.mayRide(new BukkitBystander(event.getEntity()), habits)) {
            event.setCancelled(true);
        }
    }

    /**
     * Takes a cart away once its rider has gone.
     *
     * <p>Both habits that watch for somebody getting out. Removing on exit is the quick one, a
     * couple of ticks later so the rider is properly clear; decay is the patient one, and asks
     * again when the waiting is over rather than trusting what it saw at the start.
     */
    @EventHandler(ignoreCancelled = true)
    public void onExit(VehicleExitEvent event) {
        CartHabits habits = habits();
        if (!(event.getVehicle() instanceof Minecart minecart)) {
            return;
        }

        if (habits.removeOnExit()) {
            LivingEntity leaving = event.getExited();
            schedulers.at(minecart.getLocation()).runLater(
                    () -> giveBackAndRemove(minecart, leaving, habits), REMOVAL_DELAY_TICKS);
            return;
        }
        if (habits.decaysEmptyCarts()) {
            waitForDecay(minecart, habits);
        }
    }

    /**
     * Starts the clock on a cart that has only just been put out.
     *
     * <p>Only where an operator has asked for every cart to decay rather than merely the ones
     * somebody has ridden, since this takes away a cart nobody has touched.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreate(VehicleCreateEvent event) {
        CartHabits habits = habits();
        if (habits.decaysEmptyCarts()
                && !habits.decayOnlyAfterExit()
                && event.getVehicle() instanceof Minecart minecart) {
            waitForDecay(minecart, habits);
        }
    }

    /**
     * Lets a cart through what it runs into, or runs it down.
     *
     * <p>Passing through wins where both are asked for: a cart cannot both ignore something and
     * hurt it, and being able to build a line whose carts never touch is the more useful of the
     * two.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCollide(VehicleEntityCollisionEvent event) {
        CartHabits habits = habits();
        if (!(event.getVehicle() instanceof Minecart minecart)) {
            return;
        }

        Entity hit = event.getEntity();
        if (hit instanceof Minecart other && CartBehaviour.passesThrough(
                new BukkitCart(other), habits)) {
            event.setCancelled(true);
            return;
        }

        CartBehaviour.runDown(new BukkitCart(minecart), new BukkitBystander(hit), habits)
                .ifPresent(what -> carryOut(what, hit));
    }

    /**
     * Pushes a cart along, and gathers up what it passes.
     *
     * <p>The two habits a cart's own movement sets off. Neither is a mechanism, so neither goes
     * near the dispatcher; they only want to know which block the cart has rolled into.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onMove(VehicleMoveEvent event) {
        CartHabits habits = habits();
        boolean pushing = habits.climbsWalls() || habits.plateIntersections();
        if (!(pushing || habits.pickUpItems()) || !(event.getVehicle() instanceof Minecart minecart)) {
            return;
        }

        BukkitCart cart = new BukkitCart(minecart);
        BukkitCartWorld world = new BukkitCartWorld(minecart.getWorld(), dispatcher.recipes());

        if (pushing) {
            Vec3i at = Positions.toDomain(event.getTo());
            CartBehaviour.pushFrom(world, at, cart.velocity(), habits)
                    .ifPresent(cart::setVelocity);
        }
        CartBehaviour.gatherItems(cart, world, habits);
    }

    /** Starts the wait after which a cart standing empty is taken away. */
    private void waitForDecay(Minecart minecart, CartHabits habits) {
        schedulers.at(minecart.getLocation()).runLater(() -> {
            if (CartBehaviour.hasStoodEmpty(new BukkitCart(minecart), habits)) {
                minecart.remove();
            }
        }, habits.decayEmptyAfter());
    }

    /**
     * Takes a cart away, handing back the cart itself where that was asked for.
     *
     * <p>Somebody else climbing in during the couple of ticks this waits keeps the cart, since
     * taking it away underneath them would be worse than leaving one behind.
     */
    private static void giveBackAndRemove(
            Minecart minecart, LivingEntity leaving, CartHabits habits) {
        if (!minecart.isValid() || !minecart.getPassengers().isEmpty()) {
            return;
        }
        if (habits.giveCartBack() && leaving instanceof Player player
                && player.getGameMode() != GameMode.CREATIVE) {
            hand(player, itemFor(minecart));
        }
        minecart.remove();
    }

    /** Puts an item in somebody's hands, or at their feet where they have no room for it. */
    private static void hand(Player player, ItemStack stack) {
        Location where = player.getLocation();
        player.getInventory().addItem(stack).values()
                .forEach(left -> where.getWorld().dropItem(where.add(0, 1, 0), left));
    }

    /** The item a cart is picked back up as. */
    private static ItemStack itemFor(Minecart minecart) {
        Material material = switch (minecart.getType()) {
            case CHEST_MINECART -> Material.CHEST_MINECART;
            case FURNACE_MINECART -> Material.FURNACE_MINECART;
            case HOPPER_MINECART -> Material.HOPPER_MINECART;
            case TNT_MINECART -> Material.TNT_MINECART;
            case COMMAND_BLOCK_MINECART -> Material.COMMAND_BLOCK_MINECART;
            default -> Material.MINECART;
        };
        return new ItemStack(material);
    }

    /** Hurts or removes whatever a cart has run into. */
    private static void carryOut(CartBehaviour.RunDown what, Entity hit) {
        if (what.removes()) {
            hit.remove();
            return;
        }
        if (hit instanceof LivingEntity living) {
            living.damage(what.damage());
        }
        Vec3d thrown = what.thrownClear();
        if (thrown.length() > 0) {
            hit.setVelocity(new Vector(thrown.x(), thrown.y(), thrown.z()));
        }
    }

    private CartHabits habits() {
        return configuration.settings().carts().habits();
    }
}
