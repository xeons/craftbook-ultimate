// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the blocks that answer redstone.
 *
 * <p>Three mechanics share this record because two of them have one setting each and the third has
 * none: what a dark glowstone looks like belongs to GlowStone, what catches light belongs to
 * Netherrack, and a jack o'lantern is a carved pumpkin either way.
 *
 * @param glowstoneOffBlock what a glowstone is while it is dark
 * @param fireBlocks what catches light on top of itself while it is powered
 */
@NullMarked
public record PowerableSettings(Key glowstoneOffBlock, Set<Key> fireBlocks) {

    /** What a glowstone is while it is dark, as the fork had it. */
    public static final Key DEFAULT_GLOWSTONE_OFF = Key.key("minecraft:soul_sand");

    /** What catches light on top of itself while it is powered, as the fork had it. */
    public static final Key DEFAULT_FIRE_BLOCK = Key.key("minecraft:netherrack");

    /** The powerables as they have always behaved. */
    public static final PowerableSettings DEFAULTS =
            new PowerableSettings(DEFAULT_GLOWSTONE_OFF, Set.of(DEFAULT_FIRE_BLOCK));

    /** Copies the list of what burns. */
    public PowerableSettings {
        fireBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(fireBlocks));
    }

    /** These settings with a glowstone going dark into something else. */
    public PowerableSettings withGlowstoneOffBlock(Key block) {
        return new PowerableSettings(block, fireBlocks);
    }

    /** These settings with a different set of blocks that catch light. */
    public PowerableSettings withFireBlocks(Set<Key> blocks) {
        return new PowerableSettings(glowstoneOffBlock, blocks);
    }
}
