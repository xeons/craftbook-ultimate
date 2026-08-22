// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.dispenser;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.dispenser.DispenserRecipe;
import com.xeonproductions.craftbookultimate.core.dispenser.DispenserRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

/**
 * Dispensers loaded in a pattern that makes them do something other than dispense.
 *
 * <p>One event: the dispenser is about to fire, its nine slots are read, and if they spell a
 * machine that machine runs instead. Nothing is crafted — the pattern stays in the dispenser and
 * one of every stack is taken, so a machine goes on working until one of its stacks runs out.
 *
 * <p>Which pattern means what is in {@code core} and testable without a server; what is here is
 * reading the slots and making the thing appear.
 */
@NullMarked
public final class DispenserListener implements Listener {

    private final Configuration configuration;

    public DispenserListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        Settings settings = configuration.settings();
        if (!settings.runsMechanicIn(DispenserRecipes.NAME, block.getWorld().getName())
                || !settings.mechanics().dispensers().anythingAtAll()
                || !(block.getState() instanceof Dispenser dispenser)
                || !(block.getBlockData() instanceof Directional directional)) {
            return;
        }

        Optional<DispenserRecipe> machine = DispenserRecipes.matching(
                loaded(dispenser), settings.mechanics().dispensers());
        if (machine.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        fire(machine.get(), block, directional.getFacing());
        takeOneOfEach(dispenser);
    }

    /** What is in each of the nine slots, with air for an empty one. */
    private static List<Key> loaded(Dispenser dispenser) {
        List<Key> slots = new ArrayList<>(DispenserRecipe.SLOTS);
        ItemStack[] contents = dispenser.getInventory().getContents();
        for (int slot = 0; slot < DispenserRecipe.SLOTS; slot++) {
            ItemStack held = slot < contents.length ? contents[slot] : null;
            slots.add(held == null || held.getType().isAir()
                    ? DispenserRecipe.empty()
                    : held.getType().getKey());
        }
        return slots;
    }

    /** Runs one of the machines. */
    private static void fire(DispenserRecipe machine, Block block, BlockFace facing) {
        switch (machine) {
            case CANNON -> {
                TNTPrimed tnt = block.getWorld().spawn(muzzle(block, facing), TNTPrimed.class);
                tnt.setVelocity(shot(facing, DispenserRecipes.CANNON_SPEED));
            }
            case FIRE_ARROWS -> {
                Arrow arrow = block.getWorld().spawn(muzzle(block, facing), Arrow.class);
                arrow.setFireTicks(DispenserRecipes.ARROW_BURN);
                arrow.setVelocity(shot(facing, DispenserRecipes.SHOT_SPEED));
            }
            case SNOW_SHOOTER -> {
                Snowball snowball = block.getWorld().spawn(muzzle(block, facing), Snowball.class);
                snowball.setVelocity(shot(facing, DispenserRecipes.SHOT_SPEED));
            }
            case XP_SHOOTER -> {
                ThrownExpBottle bottle =
                        block.getWorld().spawn(muzzle(block, facing), ThrownExpBottle.class);
                bottle.setVelocity(shot(facing, DispenserRecipes.SHOT_SPEED));
            }
            case FAN -> draught(block, facing, false);
            case VACUUM -> draught(block, facing, true);
        }
    }

    /**
     * Blows or drags whatever stands in front of the dispenser.
     *
     * <p>Along the open air in front of it and no further: a wall stops the draught, which is what
     * lets a fan be built into one side of a corridor without reaching through it.
     */
    private static void draught(Block block, BlockFace facing, boolean pulling) {
        Block at = block.getRelative(facing);
        for (int away = 0; away < DispenserRecipes.DRAUGHT_REACH; away++) {
            if (!at.getType().isAir()) {
                return;
            }
            Vector push = direction(facing)
                    .multiply(DispenserRecipes.draught(away, pulling));
            for (Entity caught : at.getWorld().getNearbyEntities(at.getBoundingBox())) {
                caught.setVelocity(push);
            }
            at = at.getRelative(facing);
        }
    }

    /** Takes one of every stack, which is what keeps a machine's pattern intact. */
    private static void takeOneOfEach(Dispenser dispenser) {
        ItemStack[] contents = dispenser.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack held = contents[slot];
            if (held == null || held.getType() == Material.AIR) {
                continue;
            }
            held.setAmount(held.getAmount() - 1);
            dispenser.getInventory().setItem(slot, held.getAmount() <= 0 ? null : held);
        }
    }

    /** The middle of the block the dispenser points at. */
    private static Location muzzle(Block block, BlockFace facing) {
        return block.getRelative(facing).getLocation().add(0.5, 0.5, 0.5);
    }

    /** How fast and which way something leaves the dispenser, with a little lift on it. */
    private static Vector shot(BlockFace facing, double speed) {
        return direction(facing)
                .add(new Vector(0, DispenserRecipes.SHOT_RISE, 0))
                .normalize()
                .multiply(speed);
    }

    private static Vector direction(BlockFace facing) {
        return new Vector(facing.getModX(), facing.getModY(), facing.getModZ());
    }
}
