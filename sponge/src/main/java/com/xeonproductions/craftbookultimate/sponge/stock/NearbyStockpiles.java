// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.stock;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpiles;
import com.xeonproductions.craftbookultimate.sponge.adapter.Directions;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.block.entity.carrier.CarrierBlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.ChestAttachmentType;
import org.spongepowered.api.data.type.ChestAttachmentTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * The containers near a chip, as one place to take materials from.
 *
 * <p>Nearest first, so a mechanic pays out of the chest beside it before the one across the room.
 * A double chest is counted once: both halves share an inventory, and finding it twice would make a
 * chip believe it had twice the materials it has.
 */
@NullMarked
public final class NearbyStockpiles {

    public static final int DEFAULT_RADIUS = 5;

    private NearbyStockpiles() {}

    public static Stockpile around(ServerWorld world, Vec3i centre) {
        return around(world, centre, DEFAULT_RADIUS);
    }

    public static Stockpile around(ServerWorld world, Vec3i centre, int radius) {
        return around(world, centre, radius, Set.of());
    }

    public static Stockpile around(ServerWorld world, Vec3i centre, int radius, Set<Key> kinds) {
        List<Found> found = new ArrayList<>();
        Set<Vec3i> claimed = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Vec3i position = centre.add(dx, dy, dz);
                    if (claimed.contains(position) || !isLoaded(world, position)) {
                        continue;
                    }

                    if (!kinds.isEmpty() && !kinds.contains(keyAt(world, position))) {
                        continue;
                    }

                    Optional<Inventory> inventory = inventoryAt(world, position);
                    if (inventory.isEmpty()) {
                        continue;
                    }

                    claimed.add(position);
                    partnerOf(world, position).ifPresent(claimed::add);

                    found.add(new Found(inventory.get(), centre.distanceSquared(position)));
                }
            }
        }

        found.sort(Comparator.comparingLong(Found::distanceSquared));

        List<Stockpile> stockpiles = new ArrayList<>(found.size());
        for (Found container : found) {
            stockpiles.add(new InventoryStockpile(container.inventory()));
        }
        return Stockpiles.combined(stockpiles);
    }

    private static boolean isLoaded(ServerWorld world, Vec3i position) {
        return !world.chunkAtBlock(position.x(), position.y(), position.z()).isEmpty();
    }

    private static Key keyAt(ServerWorld world, Vec3i position) {
        return RegistryTypes.BLOCK_TYPE
                .get()
                .valueKey(world.block(position.x(), position.y(), position.z()).type());
    }

    private static Optional<Inventory> inventoryAt(ServerWorld world, Vec3i position) {
        return Positions.toLocation(world, position)
                .blockEntity()
                .filter(CarrierBlockEntity.class::isInstance)
                .map(entity -> ((CarrierBlockEntity) entity).inventory());
    }

    /**
     * The other half of a double chest, where there is one.
     *
     * <p>A chest's facing points out of its front and the pair runs along the perpendicular axis,
     * with the left half on the facing's left.
     */
    private static Optional<Vec3i> partnerOf(ServerWorld world, Vec3i position) {
        var state = world.block(position.x(), position.y(), position.z());

        Optional<ChestAttachmentType> attachment = state.get(Keys.CHEST_ATTACHMENT_TYPE);
        if (attachment.isEmpty() || attachment.get().equals(ChestAttachmentTypes.SINGLE.get())) {
            return Optional.empty();
        }

        Optional<Direction> facing = state.get(Keys.DIRECTION);
        if (facing.isEmpty()) {
            return Optional.empty();
        }

        Direction towardsPartner = attachment.get().equals(ChestAttachmentTypes.LEFT.get())
                ? rotateClockwise(facing.get())
                : rotateCounterClockwise(facing.get());

        return Directions.toDomain(towardsPartner)
                .map(face -> position.add(face.deltaX(), face.deltaY(), face.deltaZ()));
    }

    private static Direction rotateClockwise(Direction face) {
        return switch (face) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> face;
        };
    }

    private static Direction rotateCounterClockwise(Direction face) {
        return rotateClockwise(rotateClockwise(rotateClockwise(face)));
    }

    private record Found(Inventory inventory, long distanceSquared) {}
}
