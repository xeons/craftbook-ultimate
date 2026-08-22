// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.effect.FireworkBurst;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.core.world.Placement;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

/**
 * A {@link ChipWorld} backed by a real world.
 *
 * <p>Blocks are named by their namespaced key in both directions, which is how the game itself
 * names them, so no mapping table is needed and blocks added by later versions work without
 * anything here changing.
 *
 * <p>Belongs to one region and must only be used from the thread owning the blocks it touches.
 */
@NullMarked
public record BukkitChipWorld(World world) implements ChipWorld {

    /** How far around a block to ask the server for entities before checking exactly where they are. */
    private static final double ENTITY_SEARCH_RADIUS = 1.0;

    /**
     * How far a degree of spread may push a shot off its aim.
     *
     * <p>The game's own figure, used everywhere it scatters a projectile.
     */
    private static final double INACCURACY_PER_DEGREE = 0.0172275;

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
    public boolean setBlockAt(Vec3i position, Key block, Placement how) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        Material material = Registry.MATERIAL.get(block);
        if (material == null || !material.isBlock()) {
            return false;
        }

        Block target = Positions.toBlock(world, position);
        BlockData data = material.createBlockData();

        // A block with a front is placed pointing the way it was asked to. Everything else has no
        // facing to set, and asking for one is quietly ignored rather than refused.
        if (data instanceof Directional directional) {
            how.facing()
                    .map(Directions::toServer)
                    .filter(face -> directional.getFaces().contains(face))
                    .ifPresent(directional::setFacing);
        }

        if (target.getType() == material && target.getBlockData().matches(data)) {
            return false;
        }

        target.setBlockData(data, how.notifyNeighbours());
        return true;
    }

    @Override
    public boolean canPlace(Vec3i position, Key block, Placement how) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        Material material = Registry.MATERIAL.get(block);
        if (material == null || !material.isBlock()) {
            return false;
        }

        BlockData data = material.createBlockData();
        if (data instanceof Directional directional) {
            Optional<org.bukkit.block.BlockFace> face = how.facing()
                    .map(Directions::toServer)
                    .filter(candidate -> directional.getFaces().contains(candidate));
            if (how.facing().isPresent() && face.isEmpty()) {
                return false;
            }
            face.ifPresent(directional::setFacing);
        }

        return Positions.toBlock(world, position).canPlace(data);
    }

    @Override
    public boolean isFullyGrown(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        BlockData data = Positions.toBlock(world, position).getBlockData();
        return !(data instanceof Ageable ageable) || ageable.getAge() >= ageable.getMaximumAge();
    }

    @Override
    public Map<Key, Integer> dropsAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Map.of();
        }

        Map<Key, Integer> totals = new HashMap<>();
        for (ItemStack stack : Positions.toBlock(world, position).getDrops()) {
            totals.merge(stack.getType().getKey(), stack.getAmount(), Integer::sum);
        }
        return totals;
    }

    @Override
    public List<DroppedItem> itemsNear(Vec3i centre, int radius) {
        if (!isLoaded(centre)) {
            return List.of();
        }
        return world
                .getNearbyEntitiesByType(Item.class, Positions.toCentre(world, centre), radius)
                .stream()
                .filter(Item::isValid)
                .<DroppedItem>map(BukkitDroppedItem::new)
                .toList();
    }

    @Override
    public Optional<Key> resolveItem(String written) {
        return LegacyBlocks.resolveItem(written);
    }

    @Override
    public int spawn(Vec3d at, EntitySpec what, int count) {
        if (count < 1 || !isLoaded(at.toBlock())) {
            return 0;
        }
        return EntitySpawning.spawn(toLocation(at), what, count);
    }

    @Override
    public boolean dropItem(Vec3d at, Key item, int count) {
        if (count < 1 || !isLoaded(at.toBlock())) {
            return false;
        }
        Material material = Registry.MATERIAL.get(item);
        if (material == null || material.isAir()) {
            return false;
        }
        world.dropItem(toLocation(at), ItemStack.of(material, count));
        return true;
    }

    @Override
    public boolean launchProjectile(
            Vec3d from, Key projectile, Vec3d direction, double speed, double spread) {
        if (!isLoaded(from.toBlock())) {
            return false;
        }

        EntityType type = Registry.ENTITY_TYPE.get(projectile);
        if (type == null || !type.isSpawnable()) {
            return false;
        }

        Entity thrown = world.spawnEntity(
                toLocation(from), type, CreatureSpawnEvent.SpawnReason.CUSTOM, null);
        Vec3d velocity = scatter(direction, spread).multiply(speed);
        thrown.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z()));
        return true;
    }

    /**
     * Nudges an aim off true by the game's own amount of inaccuracy.
     *
     * <p>The same measure the game uses for a dispenser, so a shooter left on its defaults
     * scatters the way a dispenser does rather than by some number of this plugin's own.
     */
    private static Vec3d scatter(Vec3d direction, double spread) {
        Vec3d aim = direction.normalise();
        if (spread <= 0) {
            return aim;
        }
        double deviation = INACCURACY_PER_DEGREE * spread;
        return aim.add(triangle(deviation), triangle(deviation), triangle(deviation));
    }

    /** A number around zero, most likely to be near it and never further than the deviation. */
    private static double triangle(double deviation) {
        return deviation
                * (ThreadLocalRandom.current().nextDouble() - ThreadLocalRandom.current().nextDouble());
    }

    @Override
    public boolean launchFirework(Vec3d at, FireworkBurst burst, int fuseTicks) {
        if (!isLoaded(at.toBlock())) {
            return false;
        }

        Firework firework = world.spawn(toLocation(at), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(toEffect(burst));
        firework.setFireworkMeta(meta);
        firework.setTicksToDetonate(Math.max(1, fuseTicks));
        return true;
    }

    /** Turns a described burst into the game's own idea of one. */
    private static FireworkEffect toEffect(FireworkBurst burst) {
        FireworkEffect.Builder builder = FireworkEffect.builder()
                .with(FireworkEffect.Type.valueOf(burst.shape().name()))
                .flicker(burst.flicker())
                .trail(burst.trail());

        for (int colour : burst.colours()) {
            builder.withColor(Color.fromRGB(colour));
        }
        for (int fade : burst.fades()) {
            builder.withFade(Color.fromRGB(fade));
        }
        return builder.build();
    }

    @Override
    public boolean strikeLightning(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        world.strikeLightning(Positions.toCentre(world, position));
        return true;
    }

    @Override
    public List<Bystander> bystandersNear(Vec3d centre, double radius) {
        if (!isLoaded(centre.toBlock())) {
            return List.of();
        }
        return world.getNearbyEntities(toLocation(centre), radius, radius, radius).stream()
                .filter(entity -> entity.getLocation().distanceSquared(toLocation(centre))
                        <= radius * radius)
                .<Bystander>map(BukkitBystander::new)
                .toList();
    }

    @Override
    public List<Bystander> bystandersIn(Vec3d min, Vec3d max) {
        BoundingBox box = BoundingBox.of(
                new Vector(min.x(), min.y(), min.z()), new Vector(max.x(), max.y(), max.z()));
        if (!isLoaded(min.toBlock()) || !isLoaded(max.toBlock())) {
            return List.of();
        }
        return world.getNearbyEntities(box).stream().<Bystander>map(BukkitBystander::new).toList();
    }

    @Override
    public boolean showParticle(Vec3d at, Key particle, Optional<Key> block) {
        if (!isLoaded(at.toBlock())) {
            return false;
        }

        Particle kind = Registry.PARTICLE_TYPE.get(particle);
        if (kind == null) {
            return false;
        }

        Object data = null;
        if (BlockData.class.isAssignableFrom(kind.getDataType())) {
            Material material = block.map(Registry.MATERIAL::get).orElse(null);
            if (material == null || !material.isBlock()) {
                return false;
            }
            data = material.createBlockData();
        } else if (kind.getDataType() != Void.class) {
            // Particles wanting anything else have no way of being described on a sign.
            return false;
        }

        world.spawnParticle(kind, toLocation(at), 1, data);
        return true;
    }

    @Override
    public boolean playSound(Vec3d at, Key sound, float volume, float pitch) {
        if (!isLoaded(at.toBlock())) {
            return false;
        }
        world.playSound(
                Sound.sound(sound, Sound.Source.MASTER, volume, pitch), at.x(), at.y(), at.z());
        return true;
    }

    @Override
    public boolean stopSound(Key sound) {
        world.stopSound(SoundStop.named(sound));
        return true;
    }

    /**
     * Works out which sound a sign means.
     *
     * <p>A name written out in full is looked up as it stands. Anything else is taken for the
     * shorthand the sound effect chip has always used, and matched against the same shorthand
     * worked out from every sound the server has.
     */
    @Override
    public Optional<Key> resolveSound(String written) {
        Optional<Key> named = ChipWorld.super.resolveSound(written)
                .filter(key -> Registry.SOUND_EVENT.get(key) != null);
        return named.isPresent() ? named : Sounds.byShorthand(written);
    }

    @Override
    public List<String> bookPagesAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return List.of();
        }

        BlockState state = Positions.toBlock(world, position).getState(false);
        if (!(state instanceof Container container)) {
            return List.of();
        }

        for (ItemStack stack : container.getInventory()) {
            if (stack == null || !(stack.getItemMeta() instanceof BookMeta book)) {
                continue;
            }
            List<String> pages = new java.util.ArrayList<>();
            for (Component page : book.pages()) {
                pages.add(PlainTextComponentSerializer.plainText().serialize(page));
            }
            return pages;
        }
        return List.of();
    }

    /** The place in the world a point names. */
    private Location toLocation(Vec3d point) {
        return new Location(world, point.x(), point.y(), point.z());
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return world.isChunkLoaded(position.x() >> 4, position.z() >> 4);
    }

    @Override
    public Optional<Boolean> poweredAt(Vec3i position) {
        return readingAt(position, Redstone::isPowered);
    }

    @Override
    public Optional<Boolean> receivingPowerAt(Vec3i position) {
        return readingAt(
                position,
                block -> block.isBlockPowered() || block.isBlockIndirectlyPowered());
    }

    /**
     * Asks a question of a block that may be a long way off, or answers nothing.
     *
     * <p>The place may not be loaded, and on a server that splits regions across threads a distant
     * place can belong to one this is not. Reading it anyway would be a race, so the answer is
     * that there is no answer and the asking chip leaves its output alone.
     */
    private Optional<Boolean> readingAt(Vec3i position, Predicate<Block> question) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Optional.empty();
        }
        if (!Bukkit.isOwnedByCurrentRegion(world, position.x() >> 4, position.z() >> 4)) {
            return Optional.empty();
        }
        return Optional.of(question.test(Positions.toBlock(world, position)));
    }

    @Override
    public boolean isPassable(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        return Positions.toBlock(world, position).isPassable();
    }

    @Override
    public List<Traveller> travellersIn(Vec3i position) {
        if (!isLoaded(position)) {
            return List.of();
        }

        // The server searches a box around a point, so it is asked for a little more than one
        // block and the results are then narrowed to the people actually standing in this one.
        return world
                .getNearbyEntitiesByType(
                        HumanEntity.class, Positions.toCentre(world, position), ENTITY_SEARCH_RADIUS)
                .stream()
                .filter(entity -> Positions.toDomain(entity.getLocation()).equals(position))
                .<Traveller>map(BukkitTraveller::new)
                .toList();
    }

    @Override
    public boolean releasePressurePlate(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        Block block = Positions.toBlock(world, position);
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) {
            return false;
        }

        BlockData data = block.getBlockData();
        if (data instanceof Powerable plate) {
            if (!plate.isPowered()) {
                return false;
            }
            plate.setPowered(false);
            block.setBlockData(plate, true);
            return true;
        }

        // The weighted plates carry a level rather than a flag, since they read how much is
        // standing on them.
        if (data instanceof AnaloguePowerable plate) {
            if (plate.getPower() == 0) {
                return false;
            }
            plate.setPower(0);
            block.setBlockData(plate, true);
            return true;
        }

        return false;
    }

    @Override
    public int lightLevel(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return 0;
        }
        return Positions.toBlock(world, position).getLightLevel();
    }

    @Override
    public boolean isRaining() {
        return world.hasStorm();
    }

    @Override
    public boolean isThundering() {
        return world.isThundering();
    }

    @Override
    public void setRaining(boolean raining, int durationTicks) {
        world.setStorm(raining);
        world.setWeatherDuration(durationTicks);
    }

    @Override
    public void setThundering(boolean thundering, int durationTicks) {
        world.setThundering(thundering);
        world.setThunderDuration(durationTicks);
    }

    @Override
    public void setWorldTicks(long worldTicks) {
        world.setFullTime(worldTicks);
    }

    @Override
    public Optional<Key> resolveBlock(String written) {
        return LegacyBlocks.resolve(written);
    }

    @Override
    public int minHeight() {
        return world.getMinHeight();
    }

    @Override
    public int maxHeight() {
        return world.getMaxHeight();
    }
}
