// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.mechanic;

import com.xeonproductions.craftbookultimate.core.area.AreaVault;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.mechanic.MechanicWorld;
import com.xeonproductions.craftbookultimate.core.mechanic.PostedSign;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.ic.LegacyBlocks;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import com.xeonproductions.craftbookultimate.paper.stock.NearbyStockpiles;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Switch;
import org.jspecify.annotations.NullMarked;

/**
 * A {@link MechanicWorld} backed by a real world.
 *
 * <p>Belongs to one region and must only be used from the thread owning the blocks it touches.
 * Everything a bridge, a door or a gate reaches is within a few blocks of its own sign, so that
 * is never a constraint in practice.
 */
@NullMarked
public record BukkitMechanicWorld(
        World world, AreaVault vault, RegionSchedulers schedulers, Variables variables)
        implements MechanicWorld {

    @Override
    public Variables variables() {
        return variables;
    }

    /** How long a wooden button stays pressed, which is longer than a stone one. */
    private static final long WOODEN_BUTTON_TICKS = 30;

    /** How long every other button stays pressed. */
    private static final long BUTTON_TICKS = 20;

    @Override
    public boolean workSwitchAt(Vec3i position) {
        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        if (!(block.getBlockData() instanceof Switch switchData)) {
            return false;
        }

        boolean button = Tag.BUTTONS.isTagged(block.getType());
        switchData.setPowered(button || !switchData.isPowered());
        block.setBlockData(switchData);

        if (button) {
            // A button springs back on its own, exactly as it would under a hand. The block is
            // read again first, since anything may have happened to it in the meantime.
            long ticks = Tag.WOODEN_BUTTONS.isTagged(block.getType())
                    ? WOODEN_BUTTON_TICKS
                    : BUTTON_TICKS;
            schedulers.at(world, position).runLater(() -> release(block), ticks);
        }
        return true;
    }

    /** Lets a button back out again, if it is still a pressed button. */
    private static void release(Block button) {
        if (button.getBlockData() instanceof Switch pressed && pressed.isPowered()) {
            pressed.setPowered(false);
            button.setBlockData(pressed);
        }
    }

    @Override
    public UUID id() {
        return world.getUID();
    }

    @Override
    public Key blockAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Blocks.AIR_KEY;
        }
        return Positions.toBlock(world, position).getType().getKey();
    }

    @Override
    public boolean setBlockAt(Vec3i position, Key block) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        Material material = Registry.MATERIAL.get(block);
        if (material == null || !material.isBlock()) {
            return false;
        }

        Block target = Positions.toBlock(world, position);
        if (target.getType() == material) {
            return false;
        }
        target.setType(material, true);
        return true;
    }

    @Override
    public Optional<PostedSign> signAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Optional.empty();
        }
        Block block = Positions.toBlock(world, position);
        return Signs.at(block).map(sign ->
                new PostedSign(position, Signs.read(sign), facingOf(block)));
    }

    /**
     * Which way a sign looks.
     *
     * <p>A wall sign looks out of whatever it hangs on. A standing sign can face any of sixteen
     * ways, and a bridge or a door running at an angle is not a thing, so it is rounded to the
     * quarter it is nearest.
     */
    private static BlockFace facingOf(Block block) {
        return Signs.facing(block).orElseGet(() -> {
            if (block.getBlockData() instanceof Rotatable rotatable) {
                return nearestQuarter(rotatable.getRotation());
            }
            return BlockFace.NORTH;
        });
    }

    /** The compass quarter a rotation is nearest. */
    private static BlockFace nearestQuarter(
            org.bukkit.block.BlockFace rotation) {
        int east = rotation.getModX();
        int south = rotation.getModZ();
        if (Math.abs(east) > Math.abs(south)) {
            return east > 0
                    ? BlockFace.EAST
                    : BlockFace.WEST;
        }
        if (south != 0) {
            return south > 0
                    ? BlockFace.SOUTH
                    : BlockFace.NORTH;
        }
        return BlockFace.NORTH;
    }

    @Override
    public boolean writeSign(Vec3i position, SignLines lines) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        Block block = Positions.toBlock(world, position);
        return Signs.at(block).map(sign -> {
            Signs.write(sign, lines);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return world.isChunkLoaded(position.x() >> 4, position.z() >> 4);
    }

    @Override
    public boolean isPassable(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        return Positions.toBlock(world, position).isPassable();
    }

    @Override
    public Stockpile stockpileAround(Vec3i position) {
        return NearbyStockpiles.around(world, position);
    }

    @Override
    public int minHeight() {
        return world.getMinHeight();
    }

    @Override
    public int maxHeight() {
        return world.getMaxHeight();
    }

    @Override
    public Optional<Key> resolveBlock(String written) {
        return LegacyBlocks.resolve(written);
    }

    @Override
    public Optional<Key> resolveItem(String written) {
        return LegacyBlocks.resolveItem(written);
    }
}
