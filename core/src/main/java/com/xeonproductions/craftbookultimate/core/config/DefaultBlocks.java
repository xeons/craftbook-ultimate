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
 * The blocks a building chip may place until an operator says otherwise.
 *
 * <p>Structural building materials, and nothing that does anything on its own. A bridge made of
 * planks is a bridge; one made of TNT, sand or a spawner is a way of getting a block somewhere it
 * could not otherwise be put.
 *
 * <p>Kept in order so the file written on a server's first run reads as a list somebody grouped
 * rather than a jumble.
 */
@NullMarked
public final class DefaultBlocks {

    /** Every kind of plank. */
    private static final String[] PLANKS = {
        "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks",
        "dark_oak_planks", "pale_oak_planks", "mangrove_planks", "cherry_planks", "bamboo_planks",
        "crimson_planks", "warped_planks"
    };

    /** Every kind of log, and the two nether stems that stand in for them. */
    private static final String[] LOGS = {
        "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
        "pale_oak_log", "mangrove_log", "cherry_log", "crimson_stem", "warped_stem"
    };

    /** Stone, the rocks cut from it, and the blocks a double stone slab used to become. */
    private static final String[] STONE = {
        "stone", "granite", "cobblestone", "smooth_stone", "stone_bricks", "mossy_stone_bricks",
        "cracked_stone_bricks", "chiseled_stone_bricks", "sandstone", "bricks", "nether_bricks",
        "quartz_block"
    };

    /** What a building chip may place when nothing has been configured. */
    public static final Set<Key> PLACEABLE = assemble();

    private DefaultBlocks() {}

    private static Set<Key> assemble() {
        Set<Key> blocks = new LinkedHashSet<>();
        for (String[] group : new String[][] {PLANKS, LOGS, STONE}) {
            for (String name : group) {
                blocks.add(Blocks.key(name));
            }
        }
        return Collections.unmodifiableSet(blocks);
    }
}
