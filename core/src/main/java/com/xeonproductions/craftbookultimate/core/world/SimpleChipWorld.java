// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.world;

import com.xeonproductions.craftbookultimate.core.effect.FireworkBurst;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A world held entirely in memory.
 *
 * <p>Everything not explicitly placed is air, so a test describes only the blocks it cares about.
 * That makes a rule about the world assertable without a server: place a block, run the chip,
 * check what changed.
 *
 * <p>Instances are not thread safe.
 */
@NullMarked
public final class SimpleChipWorld implements ChipWorld {

    private static final int DEFAULT_MIN_HEIGHT = -64;
    private static final int DEFAULT_MAX_HEIGHT = 320;
    private static final int DEFAULT_LIGHT = 15;

    private final UUID id = UUID.randomUUID();
    private final Map<Vec3i, Key> blocks = new HashMap<>();
    private final Map<Vec3i, Integer> light = new HashMap<>();
    private final Set<Vec3i> unloaded = new HashSet<>();
    private final Map<Vec3i, List<Traveller>> travellers = new HashMap<>();
    private final Set<Vec3i> pressedPlates = new HashSet<>();
    private final Set<Vec3i> passable = new HashSet<>();
    private final Set<Vec3i> growing = new HashSet<>();
    private final Set<Vec3i> plantable = new HashSet<>();
    private final Set<Vec3i> powered = new HashSet<>();
    private final Set<Vec3i> fedPower = new HashSet<>();
    private final Set<Vec3i> unreadable = new HashSet<>();
    private final Map<Vec3i, Map<Key, Integer>> drops = new HashMap<>();
    private final Map<Vec3i, Map<Key, Integer>> intactDrops = new HashMap<>();
    private final Set<Vec3i> liquidSources = new HashSet<>();
    private final Set<Vec3i> dryFarmland = new HashSet<>();
    private final Set<Vec3i> bonemealed = new HashSet<>();
    private final List<Vec3i> wateredFarmland = new ArrayList<>();
    private final List<Vec3i> bonemealApplied = new ArrayList<>();
    private final Map<Vec3i, BlockFace> facings = new HashMap<>();
    private final Set<Vec3i> silentlyPlaced = new HashSet<>();
    private final List<PlacedItem> items = new ArrayList<>();
    private final List<Bystander> bystanders = new ArrayList<>();
    private final Map<Vec3i, List<String>> books = new HashMap<>();
    private final List<Spawn> spawns = new ArrayList<>();
    private final List<Drop> droppedStacks = new ArrayList<>();
    private final List<Shot> shots = new ArrayList<>();
    private final List<Firework> fireworks = new ArrayList<>();
    private final List<Vec3i> strikes = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Sound> sounds = new ArrayList<>();
    private final List<Key> stoppedSounds = new ArrayList<>();
    private final Set<String> knownSounds = new LinkedHashSet<>();

    private BlockFace acceptedFacing = BlockFace.NORTH;
    private int minHeight = DEFAULT_MIN_HEIGHT;
    private int maxHeight = DEFAULT_MAX_HEIGHT;
    private int ambientLight = DEFAULT_LIGHT;
    private boolean raining;
    private boolean thundering;
    private int rainDuration;
    private int thunderDuration;
    private long worldTicks;

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Key blockAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Blocks.AIR_KEY;
        }
        return blocks.getOrDefault(position, Blocks.AIR_KEY);
    }

    @Override
    public boolean setBlockAt(Vec3i position, Key block, Placement how) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        Key previous = blocks.put(position, block);
        how.facing().ifPresent(face -> facings.put(position, face));
        if (how.notifyNeighbours()) {
            silentlyPlaced.remove(position);
        } else {
            silentlyPlaced.add(position);
        }
        return !block.equals(previous);
    }

    @Override
    public boolean canPlace(Vec3i position, Key block, Placement how) {
        // Nothing here knows the game's placement rules, so a test says which positions accept a
        // block and everywhere else refuses.
        return isLoaded(position)
                && isInBounds(position)
                && plantable.contains(position)
                && how.facing().map(face -> face == acceptedFacing).orElse(true);
    }

    @Override
    public boolean isFullyGrown(Vec3i position) {
        return !growing.contains(position);
    }

    @Override
    public Map<Key, Integer> dropsAt(Vec3i position) {
        Map<Key, Integer> yielded = drops.get(position);
        if (yielded != null) {
            return Map.copyOf(yielded);
        }
        // With nothing said about it, a block yields itself, which is what most of them do.
        return isAir(position) ? Map.of() : Map.of(blockAt(position), 1);
    }

    @Override
    public Map<Key, Integer> intactDropsAt(Vec3i position) {
        Map<Key, Integer> whole = intactDrops.get(position);
        return whole == null ? dropsAt(position) : Map.copyOf(whole);
    }

    @Override
    public boolean isLiquidSource(Vec3i position) {
        return liquidSources.contains(position);
    }

    @Override
    public boolean applyBonemeal(Vec3i position) {
        if (!bonemealed.contains(position)) {
            return false;
        }
        bonemealApplied.add(position);
        return true;
    }

    @Override
    public boolean isDryFarmland(Vec3i position) {
        return dryFarmland.contains(position);
    }

    @Override
    public boolean waterFarmland(Vec3i position) {
        if (!dryFarmland.remove(position)) {
            return false;
        }
        wateredFarmland.add(position);
        return true;
    }

    /** Makes a position hold a liquid that can be picked up in a bucket. */
    public SimpleChipWorld withLiquidSource(Vec3i position, Key liquid) {
        blocks.put(position, liquid);
        liquidSources.add(position);
        return this;
    }

    /** Makes a position hold farmland that wants watering. */
    public SimpleChipWorld withDryFarmland(Vec3i position) {
        dryFarmland.add(position);
        return this;
    }

    /** Makes a position hold something bonemeal would work on. */
    public SimpleChipWorld withSomethingToFertilise(Vec3i position) {
        bonemealed.add(position);
        return this;
    }

    /** Makes breaking a position with a tool that keeps blocks whole give something else. */
    public SimpleChipWorld withIntactDrops(Vec3i position, Map<Key, Integer> whole) {
        intactDrops.put(position, Map.copyOf(whole));
        return this;
    }

    /** Every patch of farmland this world has been asked to water, in order. */
    public List<Vec3i> wateredFarmland() {
        return List.copyOf(wateredFarmland);
    }

    /** Every position this world has been asked to fertilise, in order. */
    public List<Vec3i> bonemealApplied() {
        return List.copyOf(bonemealApplied);
    }

    @Override
    public List<DroppedItem> itemsNear(Vec3i centre, int radius) {
        List<DroppedItem> found = new ArrayList<>();
        for (PlacedItem placed : items) {
            if (placed.item().isPresent() && placed.position().chebyshevDistance(centre) <= radius) {
                found.add(placed.item());
            }
        }
        return found;
    }

    @Override
    public int spawn(Vec3d at, EntitySpec what, int count) {
        if (!what.isSpawnable() || count < 1 || !isLoaded(at.toBlock())) {
            return 0;
        }
        spawns.add(new Spawn(at, what, count));
        return count;
    }

    @Override
    public boolean dropItem(Vec3d at, Key item, int count) {
        if (count < 1 || !isLoaded(at.toBlock())) {
            return false;
        }
        droppedStacks.add(new Drop(at, item, count));
        return true;
    }

    @Override
    public boolean launchProjectile(
            Vec3d from, Key projectile, Vec3d direction, double speed, double spread) {
        if (!isLoaded(from.toBlock())) {
            return false;
        }
        shots.add(new Shot(from, projectile, direction, speed, spread));
        return true;
    }

    @Override
    public boolean launchFirework(Vec3d at, FireworkBurst burst, int flightTicks) {
        if (!isLoaded(at.toBlock())) {
            return false;
        }
        fireworks.add(new Firework(at, burst, flightTicks));
        return true;
    }

    @Override
    public boolean strikeLightning(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        strikes.add(position);
        return true;
    }

    @Override
    public List<Bystander> bystandersNear(Vec3d centre, double radius) {
        List<Bystander> found = new ArrayList<>();
        for (Bystander bystander : bystanders) {
            if (bystander.isPresent() && bystander.position().distanceSquared(centre) <= radius * radius) {
                found.add(bystander);
            }
        }
        return found;
    }

    @Override
    public List<Bystander> bystandersIn(Vec3d min, Vec3d max) {
        List<Bystander> found = new ArrayList<>();
        for (Bystander bystander : bystanders) {
            Vec3d at = bystander.position();
            if (bystander.isPresent()
                    && at.x() >= min.x() && at.x() <= max.x()
                    && at.y() >= min.y() && at.y() <= max.y()
                    && at.z() >= min.z() && at.z() <= max.z()) {
                found.add(bystander);
            }
        }
        return found;
    }

    @Override
    public boolean showParticle(Vec3d at, Key particle, Optional<Key> block) {
        particles.add(new Particle(at, particle, block));
        return true;
    }

    @Override
    public boolean playSound(Vec3d at, Key sound, float volume, float pitch) {
        sounds.add(new Sound(at, sound, volume, pitch));
        return true;
    }

    @Override
    public boolean stopSound(Key sound) {
        stoppedSounds.add(sound);
        return true;
    }

    @Override
    public List<String> bookPagesAt(Vec3i position) {
        return List.copyOf(books.getOrDefault(position, List.of()));
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return !unloaded.contains(position);
    }

    @Override
    public Optional<Boolean> poweredAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position) || unreadable.contains(position)) {
            return Optional.empty();
        }
        return Optional.of(powered.contains(position));
    }

    @Override
    public Optional<Boolean> receivingPowerAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position) || unreadable.contains(position)) {
            return Optional.empty();
        }
        return Optional.of(powered.contains(position) || fedPower.contains(position));
    }

    @Override
    public boolean isPassable(Vec3i position) {
        // Nothing here knows which blocks a player can stand in, so only the placed ones are
        // solid. A test that needs a passable block placed says so with withPassable.
        return isAir(position) || passable.contains(position);
    }

    @Override
    public List<Traveller> travellersIn(Vec3i position) {
        return List.copyOf(travellers.getOrDefault(position, List.of()));
    }

    @Override
    public boolean releasePressurePlate(Vec3i position) {
        return pressedPlates.remove(position);
    }

    @Override
    public int lightLevel(Vec3i position) {
        return light.getOrDefault(position, ambientLight);
    }

    @Override
    public boolean isRaining() {
        return raining;
    }

    @Override
    public boolean isThundering() {
        return thundering;
    }

    @Override
    public void setRaining(boolean raining, int durationTicks) {
        this.raining = raining;
        this.rainDuration = durationTicks;
    }

    @Override
    public void setThundering(boolean thundering, int durationTicks) {
        this.thundering = thundering;
        this.thunderDuration = durationTicks;
    }

    @Override
    public void setWorldTicks(long worldTicks) {
        this.worldTicks = worldTicks;
    }

    /** The world age this world was last set to. */
    public long worldTicks() {
        return worldTicks;
    }

    /** How long the current precipitation was set to last. */
    public int rainDuration() {
        return rainDuration;
    }

    /** How long the current thunderstorm was set to last. */
    public int thunderDuration() {
        return thunderDuration;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public int maxHeight() {
        return maxHeight;
    }

    /** Places a block. */
    public SimpleChipWorld withBlock(Vec3i position, Key block) {
        blocks.put(position, block);
        return this;
    }

    /** Places a vanilla block by its bare name. */
    public SimpleChipWorld withBlock(Vec3i position, String blockName) {
        return withBlock(position, Blocks.key(blockName));
    }

    /** Sets the light level at one position, overriding the ambient level. */
    public SimpleChipWorld withLight(Vec3i position, int level) {
        light.put(position, level);
        return this;
    }

    /** Sets the light level everywhere that has not been given one of its own. */
    public SimpleChipWorld withAmbientLight(int level) {
        this.ambientLight = level;
        return this;
    }

    /** Marks a position as somewhere the game would allow a block to be placed. */
    public SimpleChipWorld withPlantable(Vec3i position) {
        plantable.add(position);
        return this;
    }

    /** Sets which facing a block that has one is allowed to be placed with. */
    public SimpleChipWorld withAcceptedFacing(BlockFace face) {
        this.acceptedFacing = face;
        return this;
    }

    /** Marks a position as receiving redstone power. */
    public SimpleChipWorld withPowered(Vec3i position) {
        powered.add(position);
        return this;
    }

    /**
     * Marks a position as having power pushed at it without carrying any itself.
     *
     * <p>What a plain block with a lever on its side reads as: nothing to the trigger reader and
     * something to the power sensor.
     */
    public SimpleChipWorld withPowerArriving(Vec3i position) {
        fedPower.add(position);
        return this;
    }

    /**
     * Marks a position as somewhere this thread cannot read from.
     *
     * <p>Stands in for a place that belongs to another region on a server that splits them.
     */
    public SimpleChipWorld withUnreadable(Vec3i position) {
        unreadable.add(position);
        return this;
    }

    /** Marks whatever is at a position as still growing. */
    public SimpleChipWorld withGrowing(Vec3i position) {
        growing.add(position);
        return this;
    }

    /** Says what breaking the block at a position yields. */
    public SimpleChipWorld withDrops(Vec3i position, Map<Key, Integer> yielded) {
        drops.put(position, Map.copyOf(yielded));
        return this;
    }

    /** Drops a stack of items on the ground. */
    public SimpleChipWorld withDroppedItem(Vec3i position, DroppedItem item) {
        items.add(new PlacedItem(position, item));
        return this;
    }

    /** Which way the block at a position was placed, if a chip gave it a facing. */
    public Optional<BlockFace> facingAt(Vec3i position) {
        return Optional.ofNullable(facings.get(position));
    }

    /** Whether the block at a position was placed without telling its neighbours. */
    public boolean wasPlacedSilently(Vec3i position) {
        return silentlyPlaced.contains(position);
    }

    /** Puts someone in a block, where a chip that moves people will find them. */
    public SimpleChipWorld withTraveller(Vec3i position, Traveller traveller) {
        travellers.computeIfAbsent(position, ignored -> new ArrayList<>()).add(traveller);
        return this;
    }

    /** Marks a position as somewhere a player could stand despite a block being placed there. */
    public SimpleChipWorld withPassable(Vec3i position) {
        passable.add(position);
        return this;
    }

    /** Puts a pressed pressure plate at a position. */
    public SimpleChipWorld withPressedPlate(Vec3i position) {
        pressedPlates.add(position);
        return this;
    }

    /** Whether a pressure plate at a position is still pressed. */
    public boolean isPlatePressed(Vec3i position) {
        return pressedPlates.contains(position);
    }

    /** Marks a position as being in an unloaded chunk. */
    public SimpleChipWorld withUnloaded(Vec3i position) {
        unloaded.add(position);
        return this;
    }

    /** Sets the weather. */
    public SimpleChipWorld withWeather(boolean raining, boolean thundering) {
        setWeather(raining, thundering);
        return this;
    }

    /** Sets the world's vertical bounds. */
    public SimpleChipWorld withHeights(int minHeight, int maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        return this;
    }

    /** Puts a book in the container at a position, for the chips that read their settings from one. */
    public SimpleChipWorld withBook(Vec3i position, List<String> pages) {
        books.put(position, List.copyOf(pages));
        return this;
    }

    /** Puts something in front of a chip, where a chip looking around will find it. */
    public SimpleChipWorld withBystander(Bystander bystander) {
        bystanders.add(bystander);
        return this;
    }

    /** Everything a chip has spawned, in the order it did. */
    public List<Spawn> spawns() {
        return List.copyOf(spawns);
    }

    /** Every stack a chip has dropped, in the order it did. */
    public List<Drop> droppedStacks() {
        return List.copyOf(droppedStacks);
    }

    /** Every projectile a chip has thrown, in the order it did. */
    public List<Shot> shots() {
        return List.copyOf(shots);
    }

    /** Every firework a chip has set off, in the order it did. */
    public List<Firework> fireworks() {
        return List.copyOf(fireworks);
    }

    /** Everywhere a chip has called lightning down on, in the order it did. */
    public List<Vec3i> lightningStrikes() {
        return List.copyOf(strikes);
    }

    /** Every particle a chip has shown, in the order it did. */
    public List<Particle> particles() {
        return List.copyOf(particles);
    }

    /** Every sound a chip has played, in the order it did. */
    public List<Sound> sounds() {
        return List.copyOf(sounds);
    }

    /** Every sound something has asked to stop, in order. */
    public List<Key> stoppedSounds() {
        return List.copyOf(stoppedSounds);
    }

    /**
     * Restricts which sounds this world knows about.
     *
     * <p>A world told nothing recognises any well-formed name, which is what most tests want. One
     * told a list recognises only those, so a test can prove that a mistyped sound is refused.
     */
    public SimpleChipWorld knowingOnlySounds(String... names) {
        knownSounds.addAll(List.of(names));
        return this;
    }

    @Override
    public Optional<Key> resolveSound(String written) {
        Optional<Key> found = ChipWorld.super.resolveSound(written);
        if (knownSounds.isEmpty()) {
            return found;
        }
        return found.filter(key -> knownSounds.contains(key.value()));
    }

    /** A stack of items and where it is lying. */
    private record PlacedItem(Vec3i position, DroppedItem item) {}

    /** Something a chip put in the world. */
    public record Spawn(Vec3d at, EntitySpec what, int count) {}

    /** A stack a chip dropped. */
    public record Drop(Vec3d at, Key item, int count) {}

    /** A projectile a chip threw. */
    public record Shot(Vec3d from, Key projectile, Vec3d direction, double speed, double spread) {}

    /** A firework a chip set off. */
    public record Firework(Vec3d at, FireworkBurst burst, int flightTicks) {}

    /** A particle a chip showed. */
    public record Particle(Vec3d at, Key particle, Optional<Key> block) {}

    /** A sound a chip played. */
    public record Sound(Vec3d at, Key sound, float volume, float pitch) {}

    /** The number of blocks explicitly placed, which is what a test asserts changes against. */
    public int placedBlockCount() {
        return blocks.size();
    }
}
