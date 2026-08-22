// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.chair;

import com.xeonproductions.craftbookultimate.core.chair.Chairs;
import com.xeonproductions.craftbookultimate.core.config.ChairSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The invisible markers people are sitting on.
 *
 * <p>A seat is an armour stand with no gravity, no visibility and the marker flag set, which is
 * what leaves it with no hitbox at all: a piston cannot push it, an explosion cannot move it, and a
 * builder can still put a block where it is standing. The game does the actual sitting, so a seat
 * needs nothing done to it from one tick to the next.
 *
 * <p>Nothing is kept in memory about who is sitting where. A seat is found by asking the world
 * what is standing in a block, which means a seat left behind by a server that stopped badly is
 * still recognised as one when its chunk comes back, and there is no map to fall out of step with
 * the world. Whether an entity is a seat is written into the entity itself, and so is where its
 * rider should be put down; both survive a restart for the same reason.
 *
 * <p>Every armour stand this makes is named, so an operator who somehow ends up with a stray one
 * can clear the lot with the game's own selector rather than hunting them.
 */
@NullMarked
public final class Seats {

    /** What a stray seat can be found and killed by, if it ever comes to that. */
    public static final String SEAT_NAME = "CraftBookChair";

    /** How far from a block's corner a seat has to be to count as that block's seat. */
    private static final double SAME_BLOCK = 0.5;

    private final NamespacedKey seatKey;
    private final NamespacedKey healingKey;
    private final NamespacedKey exitKey;

    public Seats(Plugin plugin) {
        this.seatKey = new NamespacedKey(plugin, "chair");
        this.healingKey = new NamespacedKey(plugin, "chair-heals");
        this.exitKey = new NamespacedKey(plugin, "chair-exit");
    }

    /** Whether an entity is one of these. */
    public boolean isSeat(Entity entity) {
        return entity.getPersistentDataContainer().has(seatKey, PersistentDataType.BYTE);
    }

    /** Whether a seat heals whoever is in it. */
    public boolean heals(Entity seat) {
        return seat.getPersistentDataContainer().has(healingKey, PersistentDataType.BYTE);
    }

    /** The seat somebody is sitting on, if they are sitting on one at all. */
    public Optional<Entity> under(Player player) {
        Entity vehicle = player.getVehicle();
        return vehicle != null && isSeat(vehicle) ? Optional.of(vehicle) : Optional.empty();
    }

    /**
     * The seat in a block, if anybody is already sitting there.
     *
     * <p>Asked of the world rather than of a map, so a seat nobody remembers making still counts
     * as somebody in the chair.
     */
    public Optional<Entity> in(World world, Vec3i block) {
        Vec3d seat = Chairs.seatIn(block);
        Location at = new Location(world, seat.x(), seat.y(), seat.z());
        for (Entity nearby : world.getNearbyEntities(at, SAME_BLOCK, SAME_BLOCK, SAME_BLOCK)) {
            if (isSeat(nearby)) {
                return Optional.of(nearby);
            }
        }
        return Optional.empty();
    }

    /**
     * Sits somebody down in a block, or says why not.
     *
     * @param player who is sitting down
     * @param block the chair
     * @param facing which way to turn them, or nothing to leave them as they are
     * @param healing whether the chair heals
     * @return the seat, or nothing where the game refused to make one
     */
    public Optional<Entity> sit(
            Player player, Block block, @Nullable Float facing, boolean healing) {

        Vec3d seat = Chairs.seatIn(new Vec3i(block.getX(), block.getY(), block.getZ()));
        Location at = new Location(block.getWorld(), seat.x(), seat.y(), seat.z());
        at.setYaw(facing == null ? player.getLocation().getYaw() : facing);

        Location from = player.getLocation();
        ArmorStand stand = block.getWorld().spawn(at, ArmorStand.class, spawning -> {
            spawning.setMarker(true);
            spawning.setVisible(false);
            spawning.setGravity(false);
            spawning.setInvulnerable(true);
            spawning.setPersistent(true);
            spawning.customName(Component.text(SEAT_NAME));
            mark(spawning, healing, from);
        });

        if (!stand.addPassenger(player)) {
            stand.remove();
            return Optional.empty();
        }

        if (facing != null) {
            player.setRotation(facing, player.getLocation().getPitch());
        }
        return Optional.of(stand);
    }

    /**
     * Where somebody sitting on a seat should be put down.
     *
     * <p>Only asked where the settings say to return them; otherwise the seat itself is where they
     * stand up.
     */
    public Optional<Location> exitOf(Entity seat, ChairSettings settings) {
        if (!settings.exitAtEntry()) {
            return Optional.empty();
        }
        String written = seat.getPersistentDataContainer()
                .get(exitKey, PersistentDataType.STRING);
        return written == null
                ? Optional.empty()
                : parse(seat.getWorld(), written);
    }

    /** Takes a seat away, whether or not anybody is still on it. */
    public void clear(Entity seat) {
        for (Entity rider : seat.getPassengers()) {
            seat.removePassenger(rider);
        }
        seat.remove();
    }

    /** Says nicely that somebody has sat down. */
    public static Component satDown() {
        return Component.text("You sit down.", NamedTextColor.YELLOW);
    }

    /** Says nicely that somebody has stood up. */
    public static Component stoodUp() {
        return Component.text("You stand up.", NamedTextColor.YELLOW);
    }

    private void mark(ArmorStand stand, boolean healing, Location from) {
        stand.getPersistentDataContainer().set(seatKey, PersistentDataType.BYTE, (byte) 1);
        if (healing) {
            stand.getPersistentDataContainer().set(healingKey, PersistentDataType.BYTE, (byte) 1);
        }
        stand.getPersistentDataContainer().set(exitKey, PersistentDataType.STRING,
                from.getX() + "," + from.getY() + "," + from.getZ()
                        + "," + from.getYaw() + "," + from.getPitch());
    }

    /**
     * Where a rider came from, as it was written into the seat.
     *
     * <p>Anything unreadable is nothing at all rather than a place, because the only thing an
     * unreadable entry could come from is another plugin's armour stand or a hand-edited file,
     * and dropping somebody at nought is worse than standing them where the chair was.
     */
    private static Optional<Location> parse(World world, String written) {
        String[] parts = written.split(",");
        if (parts.length != 5) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Location(world,
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Float.parseFloat(parts[3]),
                    Float.parseFloat(parts[4])));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
