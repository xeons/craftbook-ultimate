// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.snow;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.config.SnowSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.snow.Snowfall;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Snow that piles, slumps and melts.
 *
 * <p>What each of those means is in {@link Snowfall}; what is here is deciding when the game has
 * given us an occasion to do any of it.
 *
 * <p>Two occasions. The game forming snow of its own accord is where piling and slumping hang off,
 * which is deliberate: the game already decides where and how often weather falls, and borrowing
 * that decision means this changes how much snow gathers rather than where. And a snowball landing,
 * for the one part of this that a player does on purpose.
 *
 * <p>All of it is off out of the box, so on a server that has never been configured both handlers
 * return on their first line.
 */
@NullMarked
public final class SnowListener implements Listener {

    private final Configuration configuration;

    public SnowListener(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Deepens snow the game has just laid down.
     *
     * <p>The game's own layer is left where it is and then built on, rather than replaced, so a
     * server that only wants drifts to slump gets exactly that and nothing else.
     */
    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent event) {
        Block block = event.getBlock();
        SnowSettings snow = snowSettings(block);
        if (!snow.anythingAtAll() || !isSnow(event.getNewState().getType())) {
            return;
        }

        Snowfall snowfall = new Snowfall(new BukkitSnowWorld(block.getWorld()), snow);
        snowfall.tick(at(block));
    }

    /** Leaves snow where a snowball lands, for a server that has asked for it. */
    @EventHandler(ignoreCancelled = true)
    public void onSnowballLand(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball) || event.getHitBlock() == null) {
            return;
        }

        Block hit = event.getHitBlock();
        SnowSettings snow = snowSettings(hit);
        if (!snow.snowballsPile()) {
            return;
        }

        Block landing = event.getHitBlockFace() == null
                ? hit.getRelative(0, 1, 0)
                : hit.getRelative(event.getHitBlockFace());

        new Snowfall(new BukkitSnowWorld(hit.getWorld()), snow).pile(at(landing));
    }

    /**
     * How snow behaves where a block is, which is nothing at all where it has been switched off.
     *
     * <p>The defaults are what nothing switched on looks like, so a world snow does not run in
     * answers the same as a server that has never configured it.
     */
    private SnowSettings snowSettings(Block where) {
        Settings settings = configuration.settings();
        return settings.runsMechanicIn(Snowfall.NAME, where.getWorld().getName())
                ? settings.mechanics().snow()
                : SnowSettings.DEFAULTS;
    }

    private static boolean isSnow(Material material) {
        return material == Material.SNOW || material == Material.SNOW_BLOCK;
    }

    private static Vec3i at(Block block) {
        return new Vec3i(block.getX(), block.getY(), block.getZ());
    }
}
