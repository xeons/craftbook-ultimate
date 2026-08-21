// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.stock;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpiles;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Chest;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Finds the containers a mechanic can draw materials from.
 *
 * <p>A mechanic that builds looks for chests near itself and treats them collectively as one
 * {@link Stockpile}, nearest first, so the closest chest is emptied before a further one is
 * touched and refilled first when material comes back.
 */
@NullMarked
public final class NearbyStockpiles {

    /**
     * How far a mechanic looks for containers.
     *
     * <p>Five blocks in every direction from the block the mechanic acts from.
     */
    public static final int DEFAULT_RADIUS = 5;

    private NearbyStockpiles() {}

    /**
     * Collects the containers within the default radius of a position.
     *
     * @param world the world to look in
     * @param centre the position to look around
     * @return the containers as one stockpile, empty if there are none
     */
    public static Stockpile around(World world, Vec3i centre) {
        return around(world, centre, DEFAULT_RADIUS);
    }

    /**
     * Collects the containers within a radius of a position.
     *
     * <p>Only loaded blocks are searched, so a mechanic cannot pull chunks in by reaching past
     * the edge of what is already there.
     *
     * <p>The two halves of a double chest share one inventory, so finding either half claims
     * both and the pair contributes its contents once.
     *
     * @param world the world to look in
     * @param centre the position to look around
     * @param radius how far to look in each direction
     */
    public static Stockpile around(World world, Vec3i centre, int radius) {
        return around(world, centre, radius, Set.of());
    }

    /**
     * Collects the containers of particular kinds within a radius of a position.
     *
     * @param kinds the container blocks that count, or empty for any container at all
     */
    public static Stockpile around(World world, Vec3i centre, int radius, Set<Key> kinds) {
        List<Found> found = new ArrayList<>();
        Set<Vec3i> claimed = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Vec3i position = centre.add(dx, dy, dz);
                    if (claimed.contains(position)
                            || !world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) {
                        continue;
                    }

                    Block block = Positions.toBlock(world, position);
                    if (!kinds.isEmpty() && !kinds.contains(block.getType().getKey())) {
                        continue;
                    }
                    Inventory inventory = inventoryOf(block);
                    if (inventory == null) {
                        continue;
                    }

                    claimed.add(position);
                    partnerOf(block).ifPresent(claimed::add);

                    found.add(new Found(inventory, centre.distanceSquared(position)));
                }
            }
        }

        found.sort(Comparator.comparingLong(Found::distanceSquared));

        List<Stockpile> stockpiles = new ArrayList<>(found.size());
        for (Found container : found) {
            stockpiles.add(new ContainerStockpile(container.inventory()));
        }
        return Stockpiles.combined(stockpiles);
    }

    /**
     * The inventory of a container block, or null if the block is not a container.
     *
     * <p>For half of a double chest this is the joined inventory covering both halves, which is
     * why the other half has to be claimed rather than visited again.
     */
    private static @Nullable Inventory inventoryOf(Block block) {
        BlockState state = block.getState(false);
        return state instanceof Container container ? container.getInventory() : null;
    }

    /**
     * The position of the other half of a double chest.
     *
     * <p>Which side a half sits on is read from the block itself rather than from the inventory,
     * because the joined inventory a chest hands back is not guaranteed to be the same object for
     * both halves and so cannot be compared by identity.
     */
    private static java.util.Optional<Vec3i> partnerOf(Block block) {
        if (!(block.getBlockData() instanceof Chest chest) || chest.getType() == Chest.Type.SINGLE) {
            return java.util.Optional.empty();
        }

        // A chest's facing points out of its front; the pair runs along the perpendicular axis,
        // with the left half on the facing's left.
        BlockFace facing = chest.getFacing();
        BlockFace towardsPartner = chest.getType() == Chest.Type.LEFT
                ? rotateClockwise(facing)
                : rotateCounterClockwise(facing);

        Block partner = block.getRelative(towardsPartner);
        return java.util.Optional.of(Positions.toDomain(partner));
    }

    private static BlockFace rotateClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }

    private static BlockFace rotateCounterClockwise(BlockFace face) {
        return rotateClockwise(rotateClockwise(rotateClockwise(face)));
    }

    /** A container that was found, and how far away it is. */
    private record Found(Inventory inventory, long distanceSquared) {}
}
