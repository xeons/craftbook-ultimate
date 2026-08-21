// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A world with a pipe built in it by hand.
 *
 * <p>A test says which blocks are where and which of them hold things, and the tracer walks it the
 * same way it walks a real one. Everything is air until something is put in it.
 */
@NullMarked
public final class SimplePipeWorld implements PipeWorld {

    private final Map<Vec3i, Key> blocks = new LinkedHashMap<>();
    private final Map<Vec3i, BlockFace> facings = new HashMap<>();
    private final Map<Vec3i, SignLines> signs = new HashMap<>();
    private final Set<Vec3i> containers = new HashSet<>();
    private final Set<Vec3i> unloaded = new HashSet<>();
    private Optional<Set<String>> knownItems = Optional.empty();

    @Override
    public Key blockAt(Vec3i position) {
        return blocks.getOrDefault(position, Blocks.AIR_KEY);
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return !unloaded.contains(position);
    }

    @Override
    public Optional<BlockFace> facingAt(Vec3i position) {
        return Optional.ofNullable(facings.get(position));
    }

    @Override
    public boolean holdsItemsAt(Vec3i position) {
        return containers.contains(position);
    }

    @Override
    public Optional<SignLines> signOn(Vec3i position) {
        return Optional.ofNullable(signs.get(position));
    }

    @Override
    public Optional<Key> resolveItem(String written) {
        Optional<Key> resolved = PipeWorld.super.resolveItem(written);
        return knownItems
                .map(known -> resolved.filter(item -> known.contains(item.value())))
                .orElse(resolved);
    }

    /** Puts a block somewhere. */
    public SimplePipeWorld with(Vec3i position, String block) {
        blocks.put(position, Blocks.key(block));
        return this;
    }

    /** Puts a run of the same block between two corners, which is how a pipe is laid. */
    public SimplePipeWorld runFrom(Vec3i from, Vec3i to, String block) {
        Vec3i step = new Vec3i(
                Integer.signum(to.x() - from.x()),
                Integer.signum(to.y() - from.y()),
                Integer.signum(to.z() - from.z()));
        Vec3i at = from;
        with(at, block);
        while (!at.equals(to)) {
            at = at.add(step.x(), step.y(), step.z());
            with(at, block);
        }
        return this;
    }

    /** Puts a piston somewhere, pointing whichever way. */
    public SimplePipeWorld withPiston(Vec3i position, String block, BlockFace facing) {
        with(position, block);
        facings.put(position, facing);
        return this;
    }

    /** Puts something that holds items somewhere. */
    public SimplePipeWorld withContainer(Vec3i position) {
        containers.add(position);
        return with(position, "chest");
    }

    /** Hangs a sign on a block. */
    public SimplePipeWorld withSignOn(Vec3i position, String... lines) {
        signs.put(position, SignLines.of(lines));
        return this;
    }

    /** Makes a position unreadable, as an unloaded chunk would be. */
    public SimplePipeWorld unloadedAt(Vec3i position) {
        unloaded.add(position);
        return this;
    }

    /**
     * Says which items exist, so a name that is not one of them resolves to nothing.
     *
     * <p>Without this any well-formed name is an item, which is what a world with no server behind
     * it can tell. A test checking that a mistyped filter is refused has to say otherwise.
     */
    public SimplePipeWorld knowingOnly(String... items) {
        this.knownItems = Optional.of(Set.of(items));
        return this;
    }
}
