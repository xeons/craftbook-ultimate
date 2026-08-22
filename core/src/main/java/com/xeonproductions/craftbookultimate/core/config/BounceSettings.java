// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the blocks that throw whoever jumps on them.
 *
 * <p>Two lists rather than one, because they are answered differently. A block in the first throws
 * somebody only when a sign under it says how hard; a block in the second carries its own throw
 * here and needs no sign at all.
 *
 * @param blocks what may throw somebody when a [Jump] sign is under it
 * @param automatic what throws somebody with no sign at all, and how hard, in the sign's own
 *     grammar
 * @param sensitivity how much of a jump counts as one
 */
@NullMarked
public record BounceSettings(Set<Key> blocks, Map<Key, String> automatic, double sensitivity) {

    /** What throws somebody when a sign under it says how hard. */
    public static final Key DEFAULT_BLOCK = Key.key("minecraft:diamond_block");

    /** What throws somebody with no sign at all. */
    public static final Key DEFAULT_AUTOMATIC_BLOCK = Key.key("minecraft:orange_terracotta");

    /** How hard it throws them, in the frozen grammar a sign would use. */
    public static final String DEFAULT_AUTOMATIC_THROW = "2,1,2";

    /** How much of a jump counts as one, as the fork had it. */
    public static final double DEFAULT_SENSITIVITY = 0.1;

    /** The bounce blocks as they have always thrown. */
    public static final BounceSettings DEFAULTS = new BounceSettings(
            Set.of(DEFAULT_BLOCK),
            Map.of(DEFAULT_AUTOMATIC_BLOCK, DEFAULT_AUTOMATIC_THROW),
            DEFAULT_SENSITIVITY);

    /** Copies both lists and holds the sensitivity to something a jump can pass. */
    public BounceSettings {
        blocks = Collections.unmodifiableSet(new LinkedHashSet<>(blocks));
        automatic = Collections.unmodifiableMap(new LinkedHashMap<>(automatic));
        sensitivity = Math.max(0, sensitivity);
    }

    /** These settings with a different set of blocks that need a sign. */
    public BounceSettings withBlocks(Set<Key> materials) {
        return new BounceSettings(materials, automatic, sensitivity);
    }

    /** These settings with a different set of blocks that need none. */
    public BounceSettings withAutomatic(Map<Key, String> throwing) {
        return new BounceSettings(blocks, throwing, sensitivity);
    }

    /** These settings noticing a bigger or smaller hop. */
    public BounceSettings withSensitivity(double rise) {
        return new BounceSettings(blocks, automatic, rise);
    }
}
