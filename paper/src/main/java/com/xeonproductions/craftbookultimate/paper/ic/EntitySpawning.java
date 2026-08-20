package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Turns the description on a spawner's sign into things in the world.
 *
 * <p>Everything is spawned as a plugin's doing rather than as a natural spawn, so a mob farm built
 * out of these does not count against the world's own spawn limits and other plugins can tell where
 * it came from.
 */
@NullMarked
final class EntitySpawning {

    private EntitySpawning() {}

    /**
     * Puts a number of things in the world.
     *
     * @return how many were actually spawned
     */
    static int spawn(Location at, EntitySpec spec, int count) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnOne(at, spec).isPresent()) {
                spawned++;
            }
        }
        return spawned;
    }

    /** Puts one thing in the world, along with anything riding it. */
    private static Optional<Entity> spawnOne(Location at, EntitySpec spec) {
        return switch (spec) {
            case EntitySpec.OfType ofType -> spawnKind(at, ofType);
            case EntitySpec.Dropped dropped -> spawnDropped(at, dropped);
            case EntitySpec.WithData withData -> spawnWithData(at, withData);
            case EntitySpec.Mounted mounted -> spawnMounted(at, mounted);
            case EntitySpec.Category ignored -> Optional.empty();
            case EntitySpec.OfAnyType ignored -> Optional.empty();
            case EntitySpec.Person ignored -> Optional.empty();
        };
    }

    private static Optional<Entity> spawnKind(Location at, EntitySpec.OfType spec) {
        EntityType type = Registry.ENTITY_TYPE.get(spec.type());
        if (type == null || !type.isSpawnable()) {
            return Optional.empty();
        }

        Entity entity = at.getWorld()
                .spawnEntity(at, type, CreatureSpawnEvent.SpawnReason.CUSTOM, spawning ->
                        applyTraits(spawning, spec.wanted()));
        return Optional.of(entity);
    }

    /** Gives a newly made creature the properties its sign asked for. */
    private static void applyTraits(Entity entity, EntitySpec.Expectations wanted) {
        if (entity instanceof Creeper creeper) {
            wanted.charged().ifPresent(creeper::setPowered);
        }
        if (entity instanceof Pig pig) {
            wanted.saddled().ifPresent(pig::setSaddle);
        }
        if (entity instanceof Sheep sheep) {
            wanted.dyeColour()
                    .flatMap(EntitySpawning::dyeColour)
                    .ifPresent(sheep::setColor);
        }
    }

    private static Optional<org.bukkit.DyeColor> dyeColour(String name) {
        try {
            return Optional.of(org.bukkit.DyeColor.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<Entity> spawnDropped(Location at, EntitySpec.Dropped spec) {
        if (spec.item().isEmpty()) {
            return Optional.empty();
        }
        Material material = Registry.MATERIAL.get(spec.item().get());
        if (material == null || material.isAir()) {
            return Optional.empty();
        }
        return Optional.of(at.getWorld().dropItem(at, ItemStack.of(material)));
    }

    /**
     * Spawns something described with extra data.
     *
     * <p>The data is the game's own text format for an entity, so it is handed to the server with
     * the kind of thing spliced in front of it and the server builds whatever that describes. Data
     * on anything but a plain kind is ignored, since there is nothing to splice it into.
     */
    private static Optional<Entity> spawnWithData(Location at, EntitySpec.WithData spec) {
        if (!(spec.base() instanceof EntitySpec.OfType ofType)) {
            return spawnOne(at, spec.base());
        }

        String inner = spec.data().trim();
        if (inner.length() < 2) {
            return spawnOne(at, ofType);
        }
        inner = inner.substring(1, inner.length() - 1).trim();

        String described =
                "{id:\"" + ofType.type().asString() + "\"" + (inner.isEmpty() ? "" : "," + inner) + "}";
        try {
            Entity entity = Bukkit.getEntityFactory().createEntitySnapshot(described).createEntity(at);
            applyTraits(entity, ofType.wanted());
            return Optional.of(entity);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<Entity> spawnMounted(Location at, EntitySpec.Mounted spec) {
        Optional<Entity> vehicle = spawnOne(at, spec.vehicle());
        if (vehicle.isEmpty()) {
            return Optional.empty();
        }

        for (EntitySpec rider : spec.riders()) {
            spawnOne(at, rider).ifPresent(vehicle.get()::addPassenger);
        }
        return vehicle;
    }
}
