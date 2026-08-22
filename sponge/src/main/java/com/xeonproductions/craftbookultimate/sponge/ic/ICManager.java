// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLine;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.BlockKey;
import com.xeonproductions.craftbookultimate.sponge.adapter.Signs;
import com.xeonproductions.craftbookultimate.sponge.platform.ServerSchedulers;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * Every chip the server currently knows about.
 *
 * <p>Two indexes, and the second is what makes redstone cheap: one from a sign's block to the chip
 * it is, and one from each pin block to the chips reading it. A block changing anywhere is then a
 * lookup rather than a walk over every chip in the world.
 */
@NullMarked
public final class ICManager {

    public static final int IDENTIFIER_LINE = 1;

    private final ICRegistry registry;
    private final ServerSchedulers schedulers;
    private final ChipServices services;

    private final Map<BlockKey, ICInstance> bySign = new ConcurrentHashMap<>();
    private final Map<BlockKey, Set<ICInstance>> byPin = new ConcurrentHashMap<>();

    public ICManager(ICRegistry registry, ServerSchedulers schedulers, ChipServices services) {
        this.registry = registry;
        this.schedulers = schedulers;
        this.services = services;
    }

    public ChipServices services() {
        return services;
    }

    public ICRegistry registry() {
        return registry;
    }

    public int loadedCount() {
        return bySign.size();
    }

    public Collection<ICInstance> loaded() {
        return List.copyOf(bySign.values());
    }

    public Optional<ICInstance> load(ServerWorld world, Vec3i position) {
        BlockKey key = BlockKey.of(world.uniqueId(), position);
        if (bySign.containsKey(key)) {
            return Optional.empty();
        }

        Optional<Sign> sign = Signs.at(world, position);
        if (sign.isEmpty()) {
            return Optional.empty();
        }

        SignLines lines = Signs.read(sign.get());
        Optional<ICInstance> created = describe(world, position, lines);
        created.ifPresent(instance -> {
            bySign.put(key, instance);
            for (BlockKey pin : instance.pinKeys()) {
                byPin.computeIfAbsent(pin, ignored -> ConcurrentHashMap.newKeySet()).add(instance);
            }
            instance.load(schedulers.at(world, position));
            markTitle(world, position, key, lines, instance.definition());
        });
        return created;
    }

    /**
     * Marks a loaded chip's title where it says the wrong thing.
     *
     * <p>Nothing is written unless the colour actually differs, so a world of working chips writes
     * no blocks at all. What does get written is put off a tick and dropped if the chip has gone,
     * so no block is touched while its chunk is still arriving.
     */
    private void markTitle(
            ServerWorld world, Vec3i position, BlockKey key, SignLines lines, ICDefinition definition) {
        if (!ChipTitle.wouldChange(lines, definition)) {
            return;
        }

        schedulers.at(world, position).runLater(() -> {
            if (bySign.containsKey(key)) {
                Signs.at(world, position).ifPresent(current -> ChipTitle.mark(current, definition));
            }
        }, 1);
    }

    public Optional<ICInstance> describe(ServerWorld world, Vec3i position) {
        return Signs.at(world, position)
                .flatMap(sign -> describe(world, position, Signs.read(sign)));
    }

    private Optional<ICInstance> describe(ServerWorld world, Vec3i position, SignLines lines) {
        Optional<BlockFace> facing = Signs.facing(world, position);
        if (facing.isEmpty()) {
            return Optional.empty();
        }

        Settings settings = services.configuration().settings();
        if (!settings.allowsWorld(world.key().value())) {
            return Optional.empty();
        }

        String identifier = lines.text(IDENTIFIER_LINE);
        return resolve(identifier)
                .filter(resolution -> settings.allowsChip(resolution.definition().allModels()))
                .map(resolution -> new ICInstance(
                        world,
                        position,
                        facing.get(),
                        resolution.definition(),
                        modeOf(identifier),
                        resolution.selfTriggering(),
                        services));
    }

    public Optional<ICRegistry.Resolution> resolve(String identifierLine) {
        return registry.resolve(identifierLine);
    }

    public static ICMode modeOf(String identifierLine) {
        return ICLine.parse(identifierLine)
                .map(line -> ICMode.parse(line.mode()))
                .orElse(ICMode.NONE);
    }

    public Optional<ICInstance> unload(ServerWorld world, Vec3i position) {
        return unload(BlockKey.of(world.uniqueId(), position));
    }

    public Optional<ICInstance> reload(ServerWorld world, Vec3i position) {
        unload(world, position);
        return load(world, position);
    }

    public int unloadWithin(UUID world, Bounds box) {
        int unloaded = 0;
        for (BlockKey key : List.copyOf(bySign.keySet())) {
            if (key.world().equals(world) && box.contains(key.position())) {
                unload(key);
                unloaded++;
            }
        }
        return unloaded;
    }

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

    public Optional<ICInstance> at(ServerWorld world, Vec3i position) {
        return Optional.ofNullable(bySign.get(BlockKey.of(world.uniqueId(), position)));
    }

    /** Sets off every chip reading a block that has just changed. */
    public void triggerAt(ServerWorld world, Vec3i position) {
        Set<ICInstance> chips = byPin.get(BlockKey.of(world.uniqueId(), position));
        if (chips == null || chips.isEmpty()) {
            return;
        }

        for (ICInstance chip : chips) {
            if (chip.isUnloaded()) {
                continue;
            }
            int input = chip.inputAt(position);
            if (input < 0) {
                continue;
            }

            // Always the same thread here, since the server ticks every world on one. The hand-over
            // is kept because what it says — this work belongs where the chip's sign is — stays
            // true, and a server that ever splits worlds across threads would want it.
            schedulers.executeAt(chip.world(), chip.signPosition(), () -> {
                if (!chip.isUnloaded()) {
                    chip.trigger(input);
                }
            });
        }
    }

    public int unloadChunk(UUID world, int chunkX, int chunkZ) {
        int unloaded = 0;
        for (BlockKey key : List.copyOf(bySign.keySet())) {
            if (key.world().equals(world) && key.x() >> 4 == chunkX && key.z() >> 4 == chunkZ) {
                unload(key);
                unloaded++;
            }
        }
        return unloaded;
    }

    public void unloadAll() {
        for (BlockKey key : List.copyOf(bySign.keySet())) {
            unload(key);
        }
        byPin.clear();
    }
}
