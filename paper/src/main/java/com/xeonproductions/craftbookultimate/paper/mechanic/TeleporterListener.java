// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.mechanic.Teleporters;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import io.papermc.paper.entity.TeleportFlag;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

/**
 * The sign that sends somebody somewhere else in the same world.
 *
 * <p>Coordinates on the third line send whoever clicks it; a sign reading {@code ARRIVAL} is
 * somewhere to land rather than somewhere to leave from, and says so when clicked instead of
 * doing nothing.
 *
 * <p>A button may stand in for the sign, so a builder can put the sign behind a wall and leave only
 * the button showing. The button is followed twice back along the way it faces, which is where the
 * fork looked and is the arrangement existing builds use.
 */
@NullMarked
public final class TeleporterListener implements Listener {

    /** How far the far end may be from where somebody lands, looking for somewhere safe. */
    private static final int SAFE_SEARCH = 2;

    private final Configuration configuration;

    public TeleporterListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }

        MechanicSettings settings = configuration.settings().mechanics();
        if (!settings.allows(Teleporters.NAME)) {
            return;
        }

        Optional<Sign> found = signFor(event.getClickedBlock(), settings);
        if (found.isEmpty()) {
            return;
        }

        Sign sign = found.get();
        SignLines lines = Signs.read(sign);
        if (!lines.trimmedText(1).equalsIgnoreCase(Teleporters.SIGN_NAME)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(Teleporters.USE)) {
            return;
        }
        event.setCancelled(true);

        String written = lines.trimmedText(Teleporters.DESTINATION_LINE);
        if (Teleporters.isArrival(written)) {
            player.sendMessage(Component.text(
                    "You can only arrive here.", NamedTextColor.YELLOW));
            return;
        }

        Optional<Vec3d> going = Teleporters.destination(written);
        if (going.isEmpty()) {
            player.sendMessage(Component.text(
                    "That teleporter's third line is not a place.", NamedTextColor.RED));
            return;
        }

        send(player, sign.getBlock(), going.get(), settings);
    }

    /** Sends somebody, or says why not. */
    private static void send(
            Player player, Block from, Vec3d to, MechanicSettings settings) {

        World world = from.getWorld();
        Vec3d here = new Vec3d(from.getX(), from.getY(), from.getZ());

        if (!Teleporters.withinRange(here, to, settings.teleporterRange())) {
            player.sendMessage(Component.text(
                    "That is too far away to teleport to.", NamedTextColor.RED));
            return;
        }

        Location landing = new Location(world, to.x(), to.y(), to.z());
        if (settings.teleporterRequireSign() && !hasTeleporterSign(landing)) {
            player.sendMessage(Component.text(
                    "There is no teleporter sign at the other end.", NamedTextColor.RED));
            return;
        }

        Optional<Location> safe = somewhereSafe(landing);
        if (safe.isEmpty()) {
            player.sendMessage(Component.text(
                    "There is nowhere safe to land there.", NamedTextColor.RED));
            return;
        }

        // Kept looking the way they were, since a teleporter's far end is somewhere a builder
        // arranged and turning somebody as they arrive undoes that arrangement.
        Location arriving = safe.get();
        arriving.setYaw(player.getLocation().getYaw());
        arriving.setPitch(player.getLocation().getPitch());
        player.teleportAsync(arriving, TeleportFlag.Relative.values());
    }

    /** Whether the far end is somewhere a builder has said people may arrive. */
    private static boolean hasTeleporterSign(Location landing) {
        for (int y = 0; y <= 1; y++) {
            Block block = landing.clone().add(0, y, 0).getBlock();
            if (block.getState() instanceof Sign sign
                    && Signs.read(sign).trimmedText(1)
                            .equalsIgnoreCase(Teleporters.SIGN_NAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Somewhere at the far end a player can stand.
     *
     * <p>Straight up from where the sign says, since a builder writing a block position means the
     * floor and a player needs the two blocks above it. Anything further afield would put somebody
     * somewhere the builder did not choose.
     */
    private static Optional<Location> somewhereSafe(Location landing) {
        for (int up = 0; up <= SAFE_SEARCH; up++) {
            Location candidate = landing.clone().add(0.5, up, 0.5);
            Block feet = candidate.getBlock();
            if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable()
                    && !Tag.FIRE.isTagged(feet.getType())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * The sign a click was meant for.
     *
     * <p>A click on the sign itself, or on a button that stands in for one two blocks back the way
     * the button faces.
     */
    private static Optional<Sign> signFor(Block clicked, MechanicSettings settings) {
        if (clicked.getState() instanceof Sign sign) {
            return Optional.of(sign);
        }
        if (!settings.teleporterButtons() || !Tag.BUTTONS.isTagged(clicked.getType())) {
            return Optional.empty();
        }
        if (!(clicked.getBlockData() instanceof Directional facing)) {
            return Optional.empty();
        }

        Block behind = clicked.getRelative(facing.getFacing().getOppositeFace(), 2);
        return behind.getState() instanceof Sign sign ? Optional.of(sign) : Optional.empty();
    }
}
