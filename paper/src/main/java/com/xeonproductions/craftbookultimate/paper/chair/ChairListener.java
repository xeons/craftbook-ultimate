// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.chair;

import com.xeonproductions.craftbookultimate.core.chair.Chairs;
import com.xeonproductions.craftbookultimate.core.config.ChairSettings;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

/**
 * Sitting down, standing up, and clearing away the seats.
 *
 * <p>Sitting is a right-click with an empty hand on a block an operator allows. What happens
 * afterwards is mostly the game's: a rider follows their seat, dismounts when they press the key,
 * and is dropped when the seat goes. What is left for this is putting somebody down somewhere
 * sensible, and making sure a seat never outlives the chair it belongs to.
 *
 * <p>Three things take a seat away, and all three end in the same place: the block being broken,
 * the rider standing up, and the rider leaving the server. A fourth catches what a crash left
 * behind — a seat that comes back with its chunk and has nobody on it was never going to be sat on
 * again.
 */
@NullMarked
public final class ChairListener implements Listener {

    /** Long enough for the game to have finished putting somebody down before they are moved. */
    private static final long ONCE_THEY_ARE_OFF = 1;

    private final Configuration configuration;
    private final Seats seats;
    private final Sitting sitting;

    public ChairListener(Configuration configuration, Seats seats, Sitting sitting) {
        this.configuration = configuration;
        this.seats = seats;
        this.sitting = sitting;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Settings settings = configuration.settings();
        if (!settings.runsMechanicIn(Chairs.NAME, block.getWorld().getName())) {
            return;
        }

        // An empty hand, standing up, and not already riding something. Anything held is meant for
        // the block rather than for sitting on it, which is what keeps a chair out of the way of
        // building.
        if (!player.getInventory().getItemInMainHand().getType().isAir()
                || player.isSneaking()
                || player.getVehicle() != null) {
            return;
        }

        ChairSettings chairs = settings.mechanics().chair();
        if (!ChairBlocks.isChair(block, chairs)
                || !ChairBlocks.reachableFrom(block, event.getBlockFace())) {
            return;
        }

        Optional<SignLines> sign = ChairBlocks.signFor(block, chairs);
        if (chairs.requireSign() && sign.isEmpty()) {
            return;
        }

        if (!player.hasPermission(Chairs.CLICK_USE) || !sitting.clickingAllowed(player)) {
            return;
        }

        event.setCancelled(true);
        sitting.sitOn(player, block, sign.map(ChairBlocks::healing).orElse(false));
    }

    /**
     * Takes a seat away with the block it stood on.
     *
     * <p>The block is asked about before the world is, and deliberately: this runs on every block
     * anybody breaks anywhere, and looking for an entity each time would cost a search per pickaxe
     * swing. A seat only ever sits inside a block somebody could sit on, so a break somewhere else
     * costs one lookup in a set.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Settings settings = configuration.settings();
        if (!settings.runsMechanicIn(Chairs.NAME, broken.getWorld().getName())
                || !settings.mechanics().chair().allows(broken.getType().getKey())) {
            return;
        }
        sitting.clearAt(broken);
    }

    /**
     * Puts somebody down when they stand up.
     *
     * <p>The seat goes with them. A chair is not a vehicle anybody comes back to, so an empty one
     * is only ever litter.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player) || !seats.isSeat(event.getDismounted())) {
            return;
        }

        Entity seat = event.getDismounted();
        Optional<org.bukkit.Location> exit =
                seats.exitOf(seat, configuration.settings().mechanics().chair());

        player.sendMessage(Seats.stoodUp());
        sitting.noteStood(player);

        // A tick later, because the game is still moving them off the seat as this is raised and
        // anywhere they are put now is somewhere they are moved out of immediately afterwards.
        player.getScheduler().runDelayed(sitting.plugin(), task ->
                sitting.standUp(player, exit.orElse(null)), null, ONCE_THEY_ARE_OFF);

        seat.getScheduler().run(sitting.plugin(), task -> seats.clear(seat), null);
    }

    /** Takes away the seat of somebody who leaves, so a chair is free the moment they are gone. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        seats.under(event.getPlayer()).ifPresent(seats::clear);
        sitting.forget(event.getPlayer());
    }

    /**
     * Clears away a seat that came back with its chunk and has nobody on it.
     *
     * <p>A rider is never saved with their seat — a player who logs out is put back in the world
     * standing — so any seat found empty belongs to a session that ended. Without this a server
     * killed mid-session would leave one invisible armour stand per chair, for ever.
     */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (!configuration.settings()
                .runsMechanicIn(Chairs.NAME, event.getWorld().getName())) {
            return;
        }
        for (Entity entity : event.getEntities()) {
            if (seats.isSeat(entity) && entity.getPassengers().isEmpty()) {
                entity.remove();
            }
        }
    }

    /**
     * Refuses a healing sign to somebody who may not make one.
     *
     * <p>The only thing a chair's sign can say, so the only thing worth checking. Refusing it as
     * it is written rather than quietly ignoring it later is what tells a builder they cannot have
     * one, instead of leaving them to work out why their chair does nothing.
     */
    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!Chairs.isHealSign(nameOn(event))) {
            return;
        }
        if (!configuration.settings()
                .runsMechanicIn(Chairs.NAME, event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.getPlayer().hasPermission(Chairs.HEAL_BUILD)) {
            event.getPlayer().sendMessage(Component.text(
                    "That chair will heal whoever sits in it.", NamedTextColor.GREEN));
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text(
                "You may not make a chair that heals.", NamedTextColor.RED));
    }

    /** The second line of a sign being written, as plain text. */
    private static String nameOn(SignChangeEvent event) {
        Component line = event.line(1);
        return line == null
                ? ""
                : PlainTextComponentSerializer.plainText().serialize(line).trim();
    }

    /** Says that a chair is taken. */
    static Component occupied() {
        return Component.text("Somebody is already sitting there.", NamedTextColor.RED);
    }
}
