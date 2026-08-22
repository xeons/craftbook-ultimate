// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

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
import com.xeonproductions.craftbookultimate.sponge.adapter.Directions;
import com.xeonproductions.craftbookultimate.sponge.adapter.LegacyBlocks;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import com.xeonproductions.craftbookultimate.sponge.game.GameInternals;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.carrier.CarrierBlockEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Humanoid;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.tag.BlockTypeTags;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.MinecraftDayTime;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.LightTypes;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.weather.WeatherTypes;
import org.spongepowered.math.vector.Vector3d;

/**
 * A {@link ChipWorld} backed by a real world.
 *
 * <p>Blocks are named by their namespaced key in both directions, which is how the game itself
 * names them, so no mapping table is needed and blocks added by later versions work without
 * anything here changing. Sponge's own {@code ResourceKey} is an Adventure key already, which is
 * what makes that crossing free.
 *
 * <p>Must only be used from the server thread, which is the thread every chip runs on.
 */
@NullMarked
public record SpongeChipWorld(ServerWorld world) implements ChipWorld {

    /** How wet farmland gets, which is what watering a patch sets it to. */
    private static final int WETTEST_FARMLAND = 7;

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
        return world.uniqueId();
    }

    @Override
    public Key blockAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Blocks.AIR_KEY;
        }
        return keyOf(world.block(position.x(), position.y(), position.z()).type());
    }

    @Override
    public boolean setBlockAt(Vec3i position, Key block, Placement how) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        Optional<BlockState> placing = stateFor(block, how);
        if (placing.isEmpty()) {
            return false;
        }

        BlockState state = placing.get();
        if (world.block(position.x(), position.y(), position.z()).equals(state)) {
            return false;
        }

        return world.setBlock(
                position.x(),
                position.y(),
                position.z(),
                state,
                how.notifyNeighbours() ? BlockChangeFlags.ALL : BlockChangeFlags.NOTIFY_CLIENTS);
    }

    /**
     * Whether a block could stand at a place.
     *
     * <p>The game is asked, because it is the only thing that knows a crop needs farmland under it
     * and the planter is what asks. Where it will not answer, what is left is the part that can be
     * known from the API alone — whether the place is clear — and a block needing support is put
     * down and falls off rather than being refused up front.
     */
    @Override
    public boolean canPlace(Vec3i position, Key block, Placement how) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        if (stateFor(block, how).isEmpty()) {
            return false;
        }

        Optional<Boolean> known = GameInternals.get().canSurvive(world, position, block);
        if (known.isPresent()) {
            return known.get();
        }

        BlockState standing = world.block(position.x(), position.y(), position.z());
        return standing.type().equals(BlockTypes.AIR.get())
                || standing.get(Keys.IS_PASSABLE).orElse(false);
    }

    /** The state a key and a placement describe, with a facing set where the block has one. */
    private Optional<BlockState> stateFor(Key block, Placement how) {
        Optional<BlockType> type = RegistryTypes.BLOCK_TYPE.get().findValue(ResourceKey.of(block));
        if (type.isEmpty()) {
            return Optional.empty();
        }

        BlockState state = type.get().defaultState();
        Optional<Direction> facing = how.facing().map(Directions::toServer);
        if (facing.isPresent()) {
            state = state.with(Keys.DIRECTION, facing.get()).orElse(state);
        }
        return Optional.of(state);
    }

    @Override
    public boolean isFullyGrown(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        BlockState state = world.block(position.x(), position.y(), position.z());
        Optional<Integer> age = state.get(Keys.GROWTH_STAGE);
        if (age.isEmpty()) {
            return true;
        }
        return age.get() >= state.get(Keys.MAX_GROWTH_STAGE).orElse(age.get());
    }

    /**
     * What breaking a block would give.
     *
     * <p>The game is asked, so a crop pays out what its loot table says rather than what anybody
     * assumed. Where it will not answer, the block's own item form stands in, which is right for
     * everything the harvester deals in except where a plant yields something other than itself —
     * an approximation that is visibly one, rather than a table of remembered yields that would
     * silently pay a builder the wrong amount.
     */
    @Override
    public Map<Key, Integer> dropsAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Map.of();
        }

        Key block = blockAt(position);
        Optional<Map<Key, Integer>> known = GameInternals.get().dropsAt(world, position, block);
        if (known.isPresent()) {
            return known.get();
        }

        BlockType type = world.block(position.x(), position.y(), position.z()).type();
        return type.item().<Map<Key, Integer>>map(item -> Map.of(keyOf(item), 1)).orElseGet(Map::of);
    }

    @Override
    public boolean isLiquidSource(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        return world.block(position.x(), position.y(), position.z())
                .get(Keys.FLUID_LEVEL)
                .map(level -> level == 0)
                .orElse(false);
    }

    @Override
    public boolean applyBonemeal(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        return GameInternals.get().applyBonemeal(world, position).orElse(false);
    }

    @Override
    public boolean isDryFarmland(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        return world.block(position.x(), position.y(), position.z())
                .get(Keys.MOISTURE)
                .map(moisture -> moisture < WETTEST_FARMLAND)
                .orElse(false);
    }

    @Override
    public boolean waterFarmland(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        BlockState state = world.block(position.x(), position.y(), position.z());
        if (state.get(Keys.MOISTURE).isEmpty()) {
            return false;
        }
        return state.with(Keys.MOISTURE, WETTEST_FARMLAND)
                .map(wet -> world.setBlock(position.x(), position.y(), position.z(), wet))
                .orElse(false);
    }

    @Override
    public List<DroppedItem> itemsNear(Vec3i centre, int radius) {
        if (!isLoaded(centre)) {
            return List.of();
        }

        return world
                .entities(org.spongepowered.api.entity.Item.class, boxAround(centreOf(centre), radius))
                .stream()
                .filter(item -> !item.isRemoved())
                .<DroppedItem>map(SpongeDroppedItem::new)
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
        return EntitySpawning.spawn(world, Positions.toServer(at), what, count);
    }

    @Override
    public boolean dropItem(Vec3d at, Key item, int count) {
        if (count < 1 || !isLoaded(at.toBlock())) {
            return false;
        }

        Optional<ItemType> type = RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.of(item));
        if (type.isEmpty()) {
            return false;
        }

        Entity dropped = world.createEntity(EntityTypes.ITEM.get(), Positions.toServer(at));
        dropped.offer(Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(type.get(), count).asImmutable());
        return world.spawnEntity(dropped);
    }

    @Override
    public boolean spawnExperience(Vec3d at, int amount) {
        if (amount < 1 || !isLoaded(at.toBlock()) || !isInBounds(at.toBlock())) {
            return false;
        }

        Entity orb = world.createEntity(EntityTypes.EXPERIENCE_ORB.get(), Positions.toServer(at));
        orb.offer(Keys.EXPERIENCE, amount);
        return world.spawnEntity(orb);
    }

    @Override
    public boolean launchProjectile(
            Vec3d from, Key projectile, Vec3d direction, double speed, double spread) {
        if (!isLoaded(from.toBlock())) {
            return false;
        }

        Optional<EntityType<?>> type =
                RegistryTypes.ENTITY_TYPE.get().findValue(ResourceKey.of(projectile));
        if (type.isEmpty()) {
            return false;
        }

        Entity thrown = world.createEntity(type.get(), Positions.toServer(from));
        Vec3d velocity = scatter(direction, spread).multiply(speed);
        thrown.offer(Keys.VELOCITY, new Vector3d(velocity.x(), velocity.y(), velocity.z()));
        return world.spawnEntity(thrown);
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

        Entity firework =
                world.createEntity(EntityTypes.FIREWORK_ROCKET.get(), Positions.toServer(at));
        firework.offer(Keys.FIREWORK_EFFECTS, List.of(Fireworks.toEffect(burst)));
        firework.offer(Keys.FIREWORK_FLIGHT_MODIFIER, Ticks.of(Math.max(1, fuseTicks)));
        return world.spawnEntity(firework);
    }

    @Override
    public boolean strikeLightning(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        Entity bolt = world.createEntity(EntityTypes.LIGHTNING_BOLT.get(), centreOf(position));
        return world.spawnEntity(bolt);
    }

    @Override
    public List<Bystander> bystandersNear(Vec3d centre, double radius) {
        if (!isLoaded(centre.toBlock())) {
            return List.of();
        }

        Vector3d middle = Positions.toServer(centre);
        return world
                .entities(
                        boxAround(middle, radius),
                        entity -> entity.position().distanceSquared(middle) <= radius * radius)
                .stream()
                .<Bystander>map(SpongeBystander::new)
                .toList();
    }

    @Override
    public List<Bystander> bystandersIn(Vec3d min, Vec3d max) {
        if (!isLoaded(min.toBlock()) || !isLoaded(max.toBlock())) {
            return List.of();
        }
        return world.entities(AABB.of(Positions.toServer(min), Positions.toServer(max))).stream()
                .<Bystander>map(SpongeBystander::new)
                .toList();
    }

    @Override
    public boolean showParticle(Vec3d at, Key particle, Optional<Key> block) {
        if (!isLoaded(at.toBlock())) {
            return false;
        }

        Optional<ParticleType> kind =
                RegistryTypes.PARTICLE_TYPE.get().findValue(ResourceKey.of(particle));
        if (kind.isEmpty()) {
            return false;
        }

        ParticleEffect.Builder builder = ParticleEffect.builder().type(kind.get()).quantity(1);
        block.flatMap(key -> stateFor(key, Placement.NORMAL))
                .ifPresent(state -> builder.option(ParticleOptions.BLOCK_STATE, state));

        world.spawnParticles(builder.build(), Positions.toServer(at));
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

    /**
     * Stopping a sound.
     *
     * <p>A world is not something Sponge lets you tell to stop a sound, only the people in it, so
     * each of them is told in turn. Nobody in the world means nothing to stop, which is the same
     * outcome by a shorter route.
     */
    @Override
    public boolean stopSound(Key sound) {
        world.players().forEach(player -> player.stopSound(SoundStop.named(sound)));
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
                .filter(key ->
                        RegistryTypes.SOUND_TYPE.get().findValue(ResourceKey.of(key)).isPresent());
        return named.isPresent() ? named : Sounds.byShorthand(written);
    }

    @Override
    public List<String> bookPagesAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return List.of();
        }

        Optional<CarrierBlockEntity> container = Positions.toLocation(world, position)
                .blockEntity()
                .filter(CarrierBlockEntity.class::isInstance)
                .map(CarrierBlockEntity.class::cast);
        if (container.isEmpty()) {
            return List.of();
        }

        for (Slot slot : container.get().inventory().slots()) {
            Optional<List<Component>> pages = slot.peek().get(Keys.PAGES);
            if (pages.isEmpty()) {
                continue;
            }
            List<String> plain = new ArrayList<>();
            for (Component page : pages.get()) {
                plain.add(PlainTextComponentSerializer.plainText().serialize(page));
            }
            return plain;
        }
        return List.of();
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return !world.chunkAtBlock(position.x(), position.y(), position.z()).isEmpty();
    }

    /**
     * Whether a place is carrying power.
     *
     * <p>Always an answer here, unlike on a server that splits a world across threads: every chip
     * runs on the one server thread, so there is no place this may not read.
     */
    @Override
    public Optional<Boolean> poweredAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Optional.empty();
        }
        return Optional.of(Redstone.isPowered(world, position));
    }

    @Override
    public boolean isPassable(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        return world.block(position.x(), position.y(), position.z())
                .get(Keys.IS_PASSABLE)
                .orElse(false);
    }

    @Override
    public List<Traveller> travellersIn(Vec3i position) {
        if (!isLoaded(position)) {
            return List.of();
        }

        // The server searches a box around a point, so it is asked for a little more than one
        // block and the results are then narrowed to the people actually standing in this one.
        return world
                .entities(Humanoid.class, boxAround(centreOf(position), ENTITY_SEARCH_RADIUS))
                .stream()
                .filter(entity -> Positions.toDomain(entity.position()).equals(position))
                .<Traveller>map(SpongeTraveller::new)
                .toList();
    }

    @Override
    public boolean releasePressurePlate(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        BlockState state = world.block(position.x(), position.y(), position.z());
        if (!state.type().is(BlockTypeTags.PRESSURE_PLATES)) {
            return false;
        }

        // The weighted plates carry a level rather than a flag, since they read how much is
        // standing on them.
        Optional<Integer> power = state.get(Keys.POWER);
        if (power.isPresent()) {
            if (power.get() == 0) {
                return false;
            }
            return replace(position, state.with(Keys.POWER, 0));
        }

        if (!state.get(Keys.IS_POWERED).orElse(false)) {
            return false;
        }
        return replace(position, state.with(Keys.IS_POWERED, false));
    }

    private boolean replace(Vec3i position, Optional<BlockState> state) {
        return state.map(cleared ->
                        world.setBlock(position.x(), position.y(), position.z(), cleared))
                .orElse(false);
    }

    @Override
    public int lightLevel(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return 0;
        }
        return world.light(LightTypes.BLOCK.get(), position.x(), position.y(), position.z());
    }

    @Override
    public boolean isRaining() {
        return !world.weather().type().equals(WeatherTypes.CLEAR.get());
    }

    @Override
    public boolean isThundering() {
        return world.weather().type().equals(WeatherTypes.THUNDER.get());
    }

    /**
     * Turning the rain on and off.
     *
     * <p>Sponge holds one weather where Bukkit holds a storm flag and a thunder flag, so raining
     * and thundering here are two ways of saying which of the three it is. Starting rain while it
     * is already thundering leaves the thunder alone, which is what setting a storm flag did.
     */
    @Override
    public void setRaining(boolean raining, int durationTicks) {
        if (!raining) {
            world.setWeather(WeatherTypes.CLEAR.get(), Ticks.of(durationTicks));
        } else if (!isThundering()) {
            world.setWeather(WeatherTypes.RAIN.get(), Ticks.of(durationTicks));
        }
    }

    @Override
    public void setThundering(boolean thundering, int durationTicks) {
        if (thundering) {
            world.setWeather(WeatherTypes.THUNDER.get(), Ticks.of(durationTicks));
        } else if (isThundering()) {
            world.setWeather(WeatherTypes.RAIN.get(), Ticks.of(durationTicks));
        }
    }

    @Override
    public void setWorldTicks(long worldTicks) {
        world.properties().setDayTime(MinecraftDayTime.of(world.engine(), Ticks.of(worldTicks)));
    }

    @Override
    public Optional<Key> resolveBlock(String written) {
        return LegacyBlocks.resolve(written);
    }

    @Override
    public int minHeight() {
        return world.min().y();
    }

    @Override
    public int maxHeight() {
        return world.max().y() + 1;
    }

    /** The middle of a block, which is where a search for what is standing there starts. */
    private Vector3d centreOf(Vec3i position) {
        return Positions.toCentre(world, position).position();
    }

    private static AABB boxAround(Vector3d middle, double radius) {
        return AABB.of(
                middle.sub(radius, radius, radius), middle.add(radius, radius, radius));
    }

    private static Key keyOf(BlockType type) {
        return RegistryTypes.BLOCK_TYPE.get().valueKey(type);
    }

    private static Key keyOf(ItemType type) {
        return RegistryTypes.ITEM_TYPE.get().valueKey(type);
    }
}
