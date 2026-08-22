// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.boat.BoatBehaviour;
import com.xeonproductions.craftbookultimate.core.config.BoatHabits;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.paper.boat.BukkitBoat;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitBystander;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

/**
 * The habits every boat has, in one listener.
 *
 * <p>The counterpart of {@link CartHabitListener}, and together for the same reason: two of these
 * answer the same dismount. What each one decides is in {@link BoatBehaviour}; what is here is the
 * asking and the doing.
 *
 * <p>All of it is off until an operator says otherwise, so on a server that has never been
 * configured every one of these returns immediately.
 */
@NullMarked
public final class BoatHabitListener implements Listener {

    /**
     * How long taking a boat away waits after its rider steps out.
     *
     * <p>Long enough for the dismount to finish, so the rider is not left standing inside a boat
     * that is being removed underneath them.
     */
    private static final long REMOVAL_DELAY_TICKS = 2;

    private final Configuration configuration;
    private final RegionSchedulers schedulers;

    public BoatHabitListener(Configuration configuration, RegionSchedulers schedulers) {
        this.configuration = configuration;
        this.schedulers = schedulers;
    }

    /**
     * Takes a boat away when its rider steps out, or starts the clock on it.
     *
     * <p>Removal wins over decay where both are asked for, since a boat that is going now will
     * never be a boat that has sat empty.
     */
    @EventHandler(ignoreCancelled = true)
    public void onExit(VehicleExitEvent event) {
        BoatHabits habits = habits();
        if (!(event.getVehicle() instanceof Boat boat)) {
            return;
        }

        if (habits.removeOnExit()) {
            LivingEntity leaving = event.getExited();
            schedulers.at(boat.getLocation()).runLater(
                    () -> giveBackAndRemove(boat, leaving, habits), REMOVAL_DELAY_TICKS);
            return;
        }
        if (habits.decaysEmptyBoats()) {
            waitForDecay(boat, habits);
        }
    }

    /**
     * Keeps a boat off dry land, and starts the clock on one just put out.
     *
     * <p>Both hang off the same event because both are about a boat that has only just appeared,
     * and the placing has to be refused before anything is waited on.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreate(VehicleCreateEvent event) {
        BoatHabits habits = habits();
        if (!(event.getVehicle() instanceof Boat boat)) {
            return;
        }

        if (habits.waterPlaceOnly() && !isOnWater(boat)) {
            event.setCancelled(true);
            return;
        }

        if (habits.decaysEmptyBoats() && !habits.decayOnlyAfterExit()) {
            waitForDecay(boat, habits);
        }
    }

    /** Runs down whatever an occupied boat hits. */
    @EventHandler(ignoreCancelled = true)
    public void onCollide(VehicleEntityCollisionEvent event) {
        BoatHabits habits = habits();
        if (!habits.runDownEntities() || !(event.getVehicle() instanceof Boat boat)) {
            return;
        }

        Entity hit = event.getEntity();
        BoatBehaviour.runDown(new BukkitBoat(boat), new BukkitBystander(hit), habits)
                .ifPresent(what -> carryOut(what, hit));
    }

    /**
     * Whether a boat is floating rather than sitting on the ground.
     *
     * <p>Read off the block it is in rather than off the boat's own status, because the status of a
     * boat in the tick it is created is not yet what it will settle to. Waterlogged blocks count:
     * a boat put down in a flooded stairwell is on water by any reading a builder would accept.
     */
    private static boolean isOnWater(Boat boat) {
        Material block = boat.getLocation().getBlock().getType();
        if (block == Material.WATER) {
            return true;
        }
        Material below = boat.getLocation().add(0, -1, 0).getBlock().getType();
        return below == Material.WATER || Tag.ICE.isTagged(below);
    }

    /** Waits out the decay, then takes the boat away if it is still sitting there empty. */
    private void waitForDecay(Boat boat, BoatHabits habits) {
        schedulers.at(boat.getLocation()).runLater(() -> {
            if (BoatBehaviour.hasSatEmpty(new BukkitBoat(boat), habits)) {
                boat.remove();
            }
        }, habits.decayEmptyAfter());
    }

    /**
     * Takes a boat away, handing back the boat itself where that was asked for.
     *
     * <p>Somebody else climbing in during the couple of ticks this waits keeps the boat, since
     * taking it away underneath them would be worse than leaving one behind.
     */
    private static void giveBackAndRemove(Boat boat, LivingEntity leaving, BoatHabits habits) {
        if (!boat.isValid() || !boat.getPassengers().isEmpty()) {
            return;
        }
        if (habits.giveBoatBack() && leaving instanceof Player player
                && player.getGameMode() != GameMode.CREATIVE) {
            hand(player, new ItemStack(boat.getBoatMaterial()));
        }
        boat.remove();
    }

    /** Puts an item in somebody's hands, or at their feet where they have no room for it. */
    private static void hand(Player player, ItemStack stack) {
        Location where = player.getLocation();
        player.getInventory().addItem(stack).values()
                .forEach(left -> where.getWorld().dropItem(where.clone().add(0, 1, 0), left));
    }

    /** Hurts or removes whatever a boat has run into. */
    private static void carryOut(BoatBehaviour.RunDown what, Entity hit) {
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

    private BoatHabits habits() {
        return configuration.settings().vehicles().boats();
    }
}
