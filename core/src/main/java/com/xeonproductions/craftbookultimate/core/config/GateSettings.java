// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the gates.
 *
 * <p>A gate is the one mechanic made of something other than what its sign says, so what it may be
 * made of and how far it looks for that material are settings rather than sign grammar.
 *
 * @param blocks what a gate may be made of
 * @param radius how far around its sign a gate looks for its own material
 * @param clicking whether clicking a gate's material works the gate as well as its sign does
 */
@NullMarked
public record GateSettings(Set<Key> blocks, int radius, boolean clicking) {

    /** Every kind of fence, which is what most gates are made of. */
    private static final String[] FENCES = {
        "oak_fence", "spruce_fence", "birch_fence", "jungle_fence", "acacia_fence",
        "dark_oak_fence", "pale_oak_fence", "mangrove_fence", "cherry_fence", "bamboo_fence",
        "crimson_fence", "warped_fence", "nether_brick_fence"
    };

    /** Every kind of glass pane, which is what a gate somebody wants to see through is made of. */
    private static final String[] PANES = {
        "glass_pane", "white_stained_glass_pane", "orange_stained_glass_pane",
        "magenta_stained_glass_pane", "light_blue_stained_glass_pane",
        "yellow_stained_glass_pane", "lime_stained_glass_pane", "pink_stained_glass_pane",
        "gray_stained_glass_pane", "light_gray_stained_glass_pane", "cyan_stained_glass_pane",
        "purple_stained_glass_pane", "blue_stained_glass_pane", "brown_stained_glass_pane",
        "green_stained_glass_pane", "red_stained_glass_pane", "black_stained_glass_pane"
    };

    /** The most a gate may reach, however wide an operator makes it. */
    public static final int MAX_RADIUS = 16;

    /** The gates as they have always been built. */
    public static final GateSettings DEFAULTS = new GateSettings(defaultBlocks(), 5, true);

    /** Copies the list and holds the reach to something a gate can work with. */
    public GateSettings {
        blocks = Collections.unmodifiableSet(new LinkedHashSet<>(blocks));
        radius = Math.clamp(radius, 1, MAX_RADIUS);
    }

    /** Whether a block is one a gate may be made of. */
    public boolean allows(Key block) {
        return blocks.contains(block);
    }

    /** These settings with a different set of materials. */
    public GateSettings withBlocks(Set<Key> materials) {
        return new GateSettings(materials, radius, clicking);
    }

    /** These settings with gates reaching a different distance. */
    public GateSettings withRadius(int reach) {
        return new GateSettings(blocks, reach, clicking);
    }

    /** These settings with clicking a gate's material allowed or refused. */
    public GateSettings withClicking(boolean allowed) {
        return new GateSettings(blocks, radius, allowed);
    }

    /** What a gate may be made of when nobody has said otherwise. */
    private static Set<Key> defaultBlocks() {
        Set<Key> blocks = new LinkedHashSet<>();
        for (String[] group : new String[][] {FENCES, PANES}) {
            for (String name : group) {
                blocks.add(Blocks.key(name));
            }
        }
        blocks.add(Blocks.key("iron_bars"));
        return Collections.unmodifiableSet(blocks);
    }
}
