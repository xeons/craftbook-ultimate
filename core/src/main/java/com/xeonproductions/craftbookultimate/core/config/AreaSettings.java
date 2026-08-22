// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the toggled areas.
 *
 * <p>Both limits are on what may be put on disk rather than on what a sign may ask for, because a
 * toggled area is the one mechanic that keeps its blocks somewhere other than the world.
 *
 * @param maxBlocks the most blocks one saved area may hold, or zero for no limit
 * @param maxPerNamespace the most areas one name may have saved, or zero for no limit
 */
@NullMarked
public record AreaSettings(int maxBlocks, int maxPerNamespace) {

    /** The areas as they have always been saved. */
    public static final AreaSettings DEFAULTS = new AreaSettings(5000, 30);

    /** Holds both limits to something that can be counted against. */
    public AreaSettings {
        maxBlocks = Math.max(0, maxBlocks);
        maxPerNamespace = Math.max(0, maxPerNamespace);
    }

    /**
     * Whether an area of a size may be saved.
     *
     * <p>A limit of zero is no limit rather than none allowed, which is what the setting has
     * always been documented to mean.
     */
    public boolean allowsAreaOf(int blocks) {
        return maxBlocks == 0 || blocks <= maxBlocks;
    }

    /** Whether a namespace already holding this many areas may have another. */
    public boolean allowsAnother(int held) {
        return maxPerNamespace == 0 || held < maxPerNamespace;
    }

    /** These settings with a different limit on how big one area may be. */
    public AreaSettings withMaxBlocks(int blocks) {
        return new AreaSettings(blocks, maxPerNamespace);
    }

    /** These settings with a different limit on how many areas one name may have. */
    public AreaSettings withMaxPerNamespace(int areas) {
        return new AreaSettings(maxBlocks, areas);
    }
}
