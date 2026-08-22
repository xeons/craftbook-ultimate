// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.chair;

import com.xeonproductions.craftbookultimate.core.chair.Chairs;
import com.xeonproductions.craftbookultimate.core.config.ChairSettings;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Everything that happens around a seat.
 *
 * <p>One place rather than two, because a hand on a chair and {@code /sit} do the same thing and
 * used to disagree about the details in the fork they come from — the command skipped the
 * occupancy check the click did, and only the click told anybody it had worked.
 *
 * <p>Two things are remembered here and neither is worth persisting. The wait between sitting down
 * twice is a few seconds long, so a restart forgetting it costs nothing. Whether somebody wants to
 * sit by clicking is kept in the player rather than here: the game already writes a player's own
 * data out, so the preference follows them to another server in a group and survives a restart
 * without this plugin owning a file.
 */
@NullMarked
public final class Sitting {

    /** How high above a floor somebody is put when they stand up. */
    private static final double ON_TOP = 1;

    private final Plugin plugin;
    private final Configuration configuration;
    private final Seats seats;
    private final Clock clock;
    private final NamespacedKey noClickingKey;

    /** When each player last sat down or stood up, in seconds since the epoch. */
    private final Map<UUID, Long> lastMoved = new ConcurrentHashMap<>();

    public Sitting(Plugin plugin, Configuration configuration, Seats seats, Clock clock) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.seats = seats;
        this.clock = clock;
        this.noClickingKey = new NamespacedKey(plugin, "no-click-sit");
    }

    /** The plugin these tasks belong to. */
    public Plugin plugin() {
        return plugin;
    }

    /** Whether somebody has asked not to be sat down by clicking. */
    public boolean clickingAllowed(Player player) {
        return !player.getPersistentDataContainer().has(noClickingKey, PersistentDataType.BYTE);
    }

    /**
     * Turns clicking to sit on or off for one player.
     *
     * @return whether they may now sit by clicking
     */
    public boolean toggleClicking(Player player) {
        if (clickingAllowed(player)) {
            player.getPersistentDataContainer()
                    .set(noClickingKey, PersistentDataType.BYTE, (byte) 1);
            return false;
        }
        player.getPersistentDataContainer().remove(noClickingKey);
        return true;
    }

    /**
     * Sits somebody on a block, saying why not if they cannot be.
     *
     * @return whether they sat down
     */
    public boolean sitOn(Player player, Block block, boolean healing) {
        ChairSettings chairs = configuration.settings().mechanics().chair();
        Vec3i at = new Vec3i(block.getX(), block.getY(), block.getZ());

        if (seats.in(block.getWorld(), at).isPresent()) {
            player.sendMessage(ChairListener.occupied());
            return false;
        }
        if (!mayMove(player)) {
            player.sendMessage(tooSoon());
            return false;
        }

        Float facing = chairs.faceCorrectDirection() ? ChairBlocks.facingOf(block) : null;
        Optional<Entity> seat = seats.sit(player, block, facing, healing && chairs.heals());
        if (seat.isEmpty()) {
            player.sendMessage(Component.text(
                    "Something is in the way of that chair.", NamedTextColor.RED));
            return false;
        }

        noteStood(player);
        player.sendMessage(Seats.satDown());
        if (healing && chairs.heals()) {
            startHealing(seat.get(), chairs);
        }
        return true;
    }

    /**
     * Sits somebody down where they are standing, for the command.
     *
     * <p>The block under their feet is the chair, whatever it is, so this does not go through the
     * list of what may be sat on. What it does check is that they are on the ground: sitting in
     * mid-air would be a way of cancelling a fall.
     *
     * @return whether they sat down
     */
    public boolean sitWhereTheyStand(Player player) {
        if (player.getVehicle() != null) {
            player.sendMessage(Component.text(
                    "You are already riding something.", NamedTextColor.RED));
            return false;
        }
        // Not the client's own word for it, which is spoofable, but the two things that matter:
        // something under their feet, and not part way through a fall. Sitting in mid-air would
        // otherwise be a way of arriving at the bottom of a drop unhurt.
        Block footing = player.getLocation().getBlock().getRelative(0, -1, 0);
        if (!footing.getType().isSolid() || player.getFallDistance() > 0) {
            player.sendMessage(Component.text(
                    "You have to be standing on something to sit down.", NamedTextColor.RED));
            return false;
        }
        return sitOn(player, footing, false);
    }

    /**
     * Puts somebody down after they have stood up.
     *
     * <p>Where they sat down from if the settings say so and it is still somewhere they can be,
     * and otherwise the nearest footing to the chair. Failing both, they are left exactly where
     * the game put them, which is standing in the chair — untidy, but never inside a wall.
     */
    public void standUp(Player player, @Nullable Location exit) {
        if (exit != null && standable(exit.getBlock())) {
            player.teleportAsync(exit);
            return;
        }
        nearestFooting(player.getLocation()).ifPresent(found -> {
            found.setYaw(player.getLocation().getYaw());
            found.setPitch(player.getLocation().getPitch());
            player.teleportAsync(found);
        });
    }

    /** Takes away whatever seat is in a block, if there is one. */
    public void clearAt(Block block) {
        seats.in(block.getWorld(), new Vec3i(block.getX(), block.getY(), block.getZ()))
                .ifPresent(seats::clear);
    }

    /** Notes that somebody has just sat down or stood up. */
    public void noteStood(Player player) {
        lastMoved.put(player.getUniqueId(), clock.instant().getEpochSecond());
    }

    /** Forgets a player who has left. */
    public void forget(Player player) {
        lastMoved.remove(player.getUniqueId());
    }

    /** Forgets everybody, for a reload or a shutdown. */
    public void forgetEverybody() {
        lastMoved.clear();
    }

    /** Whether somebody has waited long enough since they last sat down. */
    private boolean mayMove(Player player) {
        Long last = lastMoved.get(player.getUniqueId());
        return last == null || Chairs.mayStandAgain(last, clock.instant().getEpochSecond());
    }

    /**
     * Heals whoever is in a chair, for as long as they stay in it.
     *
     * <p>Hung off the seat's own scheduler rather than a task that walks a list of everybody
     * sitting down. That way the work belongs to the region the chair is in, which is what makes
     * it safe on a regionised server, and it stops of its own accord when the seat goes: there is
     * no list to fall out of step with the world.
     */
    private void startHealing(Entity seat, ChairSettings chairs) {
        seat.getScheduler().runAtFixedRate(plugin, task -> {
            if (!seat.isValid()) {
                task.cancel();
                return;
            }
            ChairSettings current = configuration.settings().mechanics().chair();
            if (!current.heals()) {
                task.cancel();
                return;
            }
            for (Entity rider : seat.getPassengers()) {
                if (rider instanceof Player sitting) {
                    heal(sitting, current.healAmount());
                }
            }
        }, null, chairs.healRate(), chairs.healRate());
    }

    /** Heals somebody a little, and keeps them from getting hungry while they rest. */
    private static void heal(Player player, double amount) {
        double maximum = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                ? player.getHealth()
                : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Chairs.healed(player.getHealth(), maximum, amount));
        player.setExhaustion(Math.max(0, player.getExhaustion() - 0.1f));
    }

    /** The nearest place to a seat that somebody can stand. */
    private Optional<Location> nearestFooting(Location from) {
        World world = from.getWorld();
        Vec3i seat = new Vec3i(from.getBlockX(), from.getBlockY(), from.getBlockZ());
        for (Vec3i place : Chairs.standingPlaces(seat)) {
            Block block = world.getBlockAt(place.x(), place.y(), place.z());
            if (standable(block)) {
                return Optional.of(new Location(world,
                        place.x() + 0.5, place.y() + ON_TOP, place.z() + 0.5));
            }
        }
        return Optional.empty();
    }

    /** Whether somebody can stand in a block: room for them, and something under their feet. */
    private static boolean standable(Block block) {
        return block.isPassable()
                && block.getRelative(0, 1, 0).isPassable()
                && block.getRelative(0, -1, 0).getType().isSolid();
    }

    private static Component tooSoon() {
        return Component.text("You cannot sit down again that quickly.", NamedTextColor.RED);
    }
}
