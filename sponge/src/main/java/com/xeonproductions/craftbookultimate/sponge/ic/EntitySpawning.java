// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

/** Putting into the world whatever a sign described. */
@NullMarked
final class EntitySpawning {

    private EntitySpawning() {}

    static int spawn(ServerWorld world, Vector3d at, EntitySpec spec, int count) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnOne(world, at, spec).isPresent()) {
                spawned++;
            }
        }
        return spawned;
    }

    private static Optional<Entity> spawnOne(ServerWorld world, Vector3d at, EntitySpec spec) {
        return switch (spec) {
            case EntitySpec.OfType ofType -> spawnKind(world, at, ofType);
            case EntitySpec.Dropped dropped -> spawnDropped(world, at, dropped);
            case EntitySpec.WithData withData -> spawnWithData(world, at, withData);
            case EntitySpec.Mounted mounted -> spawnMounted(world, at, mounted);
            case EntitySpec.Category ignored -> Optional.empty();
            case EntitySpec.OfAnyType ignored -> Optional.empty();
            case EntitySpec.Person ignored -> Optional.empty();
        };
    }

    private static Optional<Entity> spawnKind(ServerWorld world, Vector3d at, EntitySpec.OfType spec) {
        Optional<EntityType<?>> type =
                RegistryTypes.ENTITY_TYPE.get().findValue(ResourceKey.of(spec.type()));
        if (type.isEmpty()) {
            return Optional.empty();
        }

        Entity entity = world.createEntity(type.get(), at);
        applyTraits(entity, spec.wanted());
        return world.spawnEntity(entity) ? Optional.of(entity) : Optional.empty();
    }

    private static void applyTraits(Entity entity, EntitySpec.Expectations wanted) {
        wanted.charged().ifPresent(charged -> entity.offer(Keys.IS_CHARGED, charged));
        wanted.saddled().ifPresent(saddled -> entity.offer(Keys.IS_SADDLED, saddled));
        wanted.dyeColour()
                .flatMap(EntitySpawning::dyeColour)
                .ifPresent(colour -> entity.offer(Keys.DYE_COLOR, colour));
    }

    private static Optional<DyeColor> dyeColour(String name) {
        return RegistryTypes.DYE_COLOR
                .get()
                .findValue(ResourceKey.minecraft(name.toLowerCase(Locale.ROOT)));
    }

    private static Optional<Entity> spawnDropped(
            ServerWorld world, Vector3d at, EntitySpec.Dropped spec) {
        if (spec.item().isEmpty()) {
            return Optional.empty();
        }

        Optional<ItemType> type =
                RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.of(spec.item().get()));
        if (type.isEmpty()) {
            return Optional.empty();
        }

        Entity dropped = world.createEntity(EntityTypes.ITEM.get(), at);
        dropped.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(type.get(), 1).asImmutable());
        return world.spawnEntity(dropped) ? Optional.of(dropped) : Optional.empty();
    }

    /**
     * Spawning something described with data as well as a name.
     *
     * <p>The braces a sign carries are the game's own SNBT, which Sponge can read as a data format,
     * so what is written is parsed into an archetype rather than being applied field by field. A
     * description the server cannot read spawns nothing, rather than spawning something that is
     * not what was asked for.
     */
    private static Optional<Entity> spawnWithData(
            ServerWorld world, Vector3d at, EntitySpec.WithData spec) {
        if (!(spec.base() instanceof EntitySpec.OfType ofType)) {
            return spawnOne(world, at, spec.base());
        }

        Optional<EntityType<?>> type =
                RegistryTypes.ENTITY_TYPE.get().findValue(ResourceKey.of(ofType.type()));
        if (type.isEmpty()) {
            return Optional.empty();
        }

        String inner = spec.data().trim();
        if (inner.length() < 2) {
            return spawnKind(world, at, ofType);
        }

        try {
            DataContainer data = DataFormats.SNBT.get().read(inner);
            EntityArchetype archetype =
                    EntityArchetype.builder().type(type.get()).entityData(data).build();
            Optional<Entity> spawned = archetype.apply(ServerLocation.of(world, at));
            spawned.ifPresent(entity -> applyTraits(entity, ofType.wanted()));
            return spawned;
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<Entity> spawnMounted(
            ServerWorld world, Vector3d at, EntitySpec.Mounted spec) {
        Optional<Entity> vehicle = spawnOne(world, at, spec.vehicle());
        if (vehicle.isEmpty()) {
            return Optional.empty();
        }

        List<Entity> riders = new ArrayList<>(vehicle.get().get(Keys.PASSENGERS).orElse(List.of()));
        for (EntitySpec rider : spec.riders()) {
            spawnOne(world, at, rider).ifPresent(riders::add);
        }
        vehicle.get().offer(Keys.PASSENGERS, riders);
        return vehicle;
    }
}
