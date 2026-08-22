// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.snow;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.snow.SnowWorld;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.jspecify.annotations.NullMarked;

/**
 * A world as the snow sees it: a depth at every place rather than blocks.
 *
 * <p>Nought is bare ground, one to seven is a layer of that many, and eight is a full block of
 * snow. The game keeps those as two different blocks with different data, and turning them into one
 * number is the whole reason the piling can be arithmetic.
 */
@NullMarked
public record BukkitSnowWorld(World world) implements SnowWorld {

    @Override
    public int depthAt(Vec3i at) {
        Block block = blockAt(at);
        if (block.getType() == Material.SNOW_BLOCK) {
            return FULL;
        }
        if (block.getBlockData() instanceof Snow snow) {
            return snow.getLayers();
        }
        return 0;
    }

    @Override
    public void setDepth(Vec3i at, int depth) {
        Block block = blockAt(at);
        int held = Math.clamp(depth, 0, FULL);

        if (held == 0) {
            block.setType(Material.AIR, false);
            return;
        }
        if (held >= FULL) {
            block.setType(Material.SNOW_BLOCK, false);
            return;
        }

        if (block.getType() != Material.SNOW) {
            block.setType(Material.SNOW, false);
        }
        BlockData data = block.getBlockData();
        if (data instanceof Snow snow) {
            snow.setLayers(Math.clamp(held, snow.getMinimumLayers(), snow.getMaximumLayers()));
            block.setBlockData(snow, false);
        }
    }

    @Override
    public boolean isClear(Vec3i at) {
        Block block = blockAt(at);
        return block.getType() == Material.AIR || block.getType() == Material.SNOW;
    }

    /**
     * Whether snow could rest on top of a place.
     *
     * <p>A block with a full square top. Snow does not stay on a fence or a slab's lower half in
     * the game either, and this asking the same question is what keeps the piling looking like
     * weather rather than like a bug.
     */
    @Override
    public boolean canRestOn(Vec3i at) {
        Block block = blockAt(at);
        return block.getType().isSolid() && block.getType().isOccluding();
    }

    @Override
    public boolean isWater(Vec3i at) {
        return blockAt(at).getType() == Material.WATER;
    }

    @Override
    public void freeze(Vec3i at) {
        blockAt(at).setType(Material.ICE, false);
    }

    @Override
    public boolean isFreezing(Vec3i at) {
        return blockAt(at).getTemperature() < 0.15;
    }

    @Override
    public boolean isWarm(Vec3i at) {
        return blockAt(at).getTemperature() > 0.15;
    }

    @Override
    public boolean seesSky(Vec3i at) {
        Block block = blockAt(at);
        return block.getLightFromSky() > 0
                || world.getHighestBlockYAt(block.getX(), block.getZ()) <= block.getY();
    }

    @Override
    public int floor() {
        return world.getMinHeight();
    }

    @Override
    public int ceiling() {
        return world.getMaxHeight() - 1;
    }

    private Block blockAt(Vec3i at) {
        return world.getBlockAt(at.x(), at.y(), at.z());
    }
}
