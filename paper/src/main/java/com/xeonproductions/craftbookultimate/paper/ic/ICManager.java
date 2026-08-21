package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLine;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.jspecify.annotations.NullMarked;

/**
 * Keeps track of every chip currently loaded in the world.
 *
 * <p>Two views are maintained: chips by the block their sign occupies, for lifecycle, and chips
 * by each block they read or drive, so a redstone change can find the chips that care about it
 * without searching.
 *
 * <p>The maps are safe to touch from several regions at once, but a chip itself only ever runs on
 * the region owning its sign.
 */
@NullMarked
public final class ICManager {

    /** The sign line carrying the chip's model reference. */
    public static final int IDENTIFIER_LINE = 1;

    private final ICRegistry registry;
    private final RegionSchedulers schedulers;
    private final ChipServices services;

    private final Map<BlockKey, ICInstance> bySign = new ConcurrentHashMap<>();
    private final Map<BlockKey, Set<ICInstance>> byPin = new ConcurrentHashMap<>();

    public ICManager(ICRegistry registry, RegionSchedulers schedulers, ChipServices services) {
        this.registry = registry;
        this.schedulers = schedulers;
        this.services = services;
    }

    /** The registries the chips here use to reach one another. */
    public ChipServices services() {
        return services;
    }

    /** The chip catalogue this manager builds from. */
    public ICRegistry registry() {
        return registry;
    }

    /** The number of chips currently loaded. */
    public int loadedCount() {
        return bySign.size();
    }

    /** Every chip currently loaded. */
    public Collection<ICInstance> loaded() {
        return List.copyOf(bySign.values());
    }

    /**
     * Reads a sign and starts the chip it describes, if it describes one.
     *
     * <p>Does nothing if the block is not a wall sign, does not name a known chip, or already has
     * a chip loaded.
     *
     * @param block the block the sign occupies
     * @return the chip that was started, or empty if there was nothing to start
     */
    public Optional<ICInstance> load(Block block) {
        BlockKey key = BlockKey.of(block);
        if (bySign.containsKey(key)) {
            return Optional.empty();
        }

        Optional<Sign> sign = Signs.at(block);
        if (sign.isEmpty()) {
            return Optional.empty();
        }

        SignLines lines = Signs.read(sign.get());
        Optional<ICInstance> created = describe(block, lines);
        created.ifPresent(instance -> {
            bySign.put(key, instance);
            for (BlockKey pin : instance.pinKeys()) {
                byPin.computeIfAbsent(pin, ignored -> ConcurrentHashMap.newKeySet()).add(instance);
            }
            instance.load(schedulers.at(block.getLocation()));
            markTitle(block, key, lines, instance.definition());
        });
        return created;
    }

    /**
     * Says on the sign itself whether the chip can work, where it does not already say so.
     *
     * <p>This is the only warning a chip built before its lines were spelled out ever gets. A sign
     * written now that leaves out a line its chip cannot work without is refused as it is written,
     * but one already standing in the world never comes past that check, and whoever built it is
     * long gone.
     *
     * <p>Nothing is written while the chunk is still arriving. The comparison is made here, on
     * lines already read for the chip itself, and only a sign that is actually wrong costs a task
     * and a write — so a world of working chips loads exactly as it did before.
     */
    private void markTitle(Block block, BlockKey key, SignLines lines, ICDefinition definition) {
        if (!ChipTitle.wouldChange(lines, definition)) {
            return;
        }

        schedulers.at(block.getLocation()).runLater(() -> {
            if (bySign.containsKey(key)) {
                Signs.at(block).ifPresent(current -> ChipTitle.mark(current, definition));
            }
        }, 1);
    }

    /**
     * Works out which chip a sign describes, without starting it.
     *
     * <p>Used both to load a chip and to check whether a sign would produce one.
     */
    public Optional<ICInstance> describe(Block block) {
        return Signs.at(block).flatMap(sign -> describe(block, Signs.read(sign)));
    }

    /**
     * Works out which chip a sign already read describes.
     *
     * <p>A sign in a world the settings exclude, or naming a chip they switch off, describes
     * nothing. The sign itself is untouched, so putting the setting back brings the chip straight
     * back to life.
     */
    private Optional<ICInstance> describe(Block block, SignLines lines) {
        Optional<BlockFace> facing = Signs.facing(block);
        if (facing.isEmpty()) {
            return Optional.empty();
        }

        Settings settings = services.configuration().settings();
        if (!settings.allowsWorld(block.getWorld().getName())) {
            return Optional.empty();
        }

        String identifier = lines.text(IDENTIFIER_LINE);
        return resolve(identifier)
                .filter(resolution -> settings.allowsChip(resolution.definition().allModels()))
                .map(resolution -> new ICInstance(
                        block.getWorld(),
                        Positions.toDomain(block),
                        facing.get(),
                        resolution.definition(),
                        modeOf(identifier),
                        resolution.selfTriggering(),
                        services));
    }

    /** Resolves an identifier line to a chip. */
    public Optional<ICRegistry.Resolution> resolve(String identifierLine) {
        return registry.resolve(identifierLine);
    }

    /** The mode written after the model reference on an identifier line. */
    public static ICMode modeOf(String identifierLine) {
        return ICLine.parse(identifierLine).map(line -> ICMode.parse(line.mode())).orElse(ICMode.NONE);
    }

    /**
     * Stops the chip whose sign is at a block, if there is one.
     *
     * @return the chip that was stopped, or empty if there was none
     */
    public Optional<ICInstance> unload(Block block) {
        return unload(BlockKey.of(block));
    }

    /** Stops the chip whose sign is at a key, if there is one. */
    public Optional<ICInstance> unload(BlockKey key) {
        ICInstance instance = bySign.remove(key);
        if (instance == null) {
            return Optional.empty();
        }

        for (BlockKey pin : instance.pinKeys()) {
            byPin.computeIfPresent(pin, (ignored, chips) -> {
                chips.remove(instance);
                return chips.isEmpty() ? null : chips;
            });
        }
        instance.unload();
        return Optional.of(instance);
    }

    /** The chip whose sign is at a block, if one is loaded. */
    public Optional<ICInstance> at(Block block) {
        return Optional.ofNullable(bySign.get(BlockKey.of(block)));
    }

    /**
     * Runs every chip that reads the block whose redstone state just changed.
     *
     * <p>A block may be an input to more than one chip, so all of them run. A block that is only
     * an output is ignored, since a chip driving its own output must not trigger itself.
     *
     * @param block the block whose redstone state changed
     */
    public void triggerAt(Block block) {
        Set<ICInstance> chips = byPin.get(BlockKey.of(block));
        if (chips == null || chips.isEmpty()) {
            return;
        }

        Vec3i position = Positions.toDomain(block);
        for (ICInstance chip : chips) {
            if (chip.isUnloaded()) {
                continue;
            }
            int input = chip.inputAt(position);
            if (input < 0) {
                continue;
            }

            // A chip is normally in the same region as the pin that changed, and runs straight
            // away. Should it not be, it runs on the region owning its sign instead of this
            // thread reaching into blocks it does not own.
            schedulers.executeAt(chip.world(), chip.signPosition(), () -> {
                if (!chip.isUnloaded()) {
                    chip.trigger(input);
                }
            });
        }
    }

    /** Starts every chip on a sign in a chunk's captured tile entities. */
    public int loadAll(Collection<Block> candidates) {
        int loaded = 0;
        for (Block block : candidates) {
            if (load(block).isPresent()) {
                loaded++;
            }
        }
        return loaded;
    }

    /** Stops every chip whose sign lies in a chunk. */
    public int unloadChunk(java.util.UUID world, int chunkX, int chunkZ) {
        int unloaded = 0;
        for (BlockKey key : List.copyOf(bySign.keySet())) {
            if (key.world().equals(world) && key.x() >> 4 == chunkX && key.z() >> 4 == chunkZ) {
                unload(key);
                unloaded++;
            }
        }
        return unloaded;
    }

    /** Stops every chip. */
    public void unloadAll() {
        for (BlockKey key : List.copyOf(bySign.keySet())) {
            unload(key);
        }
        byPin.clear();
    }
}
