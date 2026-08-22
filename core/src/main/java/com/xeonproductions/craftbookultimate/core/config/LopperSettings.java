// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.lopper.LopperRules;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What the tree lopper and the vein miner are allowed to take.
 *
 * <p>One record for both, because they are one mechanic pointed at two kinds of block. What
 * differs between them is entirely in the defaults, and those are held as written names rather
 * than as blocks: a tag is whatever the server currently has in it, so a list naming
 * {@code #minecraft:logs} gains a wood a later version of the game adds without anybody editing a
 * file.
 *
 * @param blocks what a run follows, already expanded from whatever tags the file named
 * @param tools what has to be in the hand for a run to happen at all
 * @param maxSize the most blocks one run takes, counting the one broken by hand. Zero switches the
 *     mechanic off without a second setting saying so
 * @param diagonals whether blocks touching at an edge or a corner count as connected
 * @param anyListedBlock whether a run follows anything on its list rather than only more of what
 *     was broken
 * @param singleUse whether the whole run costs the tool one point of wear rather than one a block
 */
@NullMarked
public record LopperSettings(
        Set<Key> blocks,
        Set<Key> tools,
        int maxSize,
        boolean diagonals,
        boolean anyListedBlock,
        boolean singleUse) {

    /** How many blocks a run takes when nobody has said. */
    public static final int DEFAULT_MAX_SIZE = 30;

    /** What a tree lopper follows: every log and every piece of wood the game has. */
    public static final List<String> DEFAULT_TREE_BLOCKS = List.of("#minecraft:logs");

    /** What a tree lopper is worked with. */
    public static final List<String> DEFAULT_AXES = List.of(
            "minecraft:wooden_axe",
            "minecraft:stone_axe",
            "minecraft:iron_axe",
            "minecraft:golden_axe",
            "minecraft:diamond_axe",
            "minecraft:netherite_axe");

    /** The leaves a tree lopper takes with the trunk, where it has been told to. */
    public static final List<String> DEFAULT_LEAVES = List.of("#minecraft:leaves");

    /** What a vein miner follows, which is every ore the game has and the two nether metals. */
    public static final List<String> DEFAULT_VEIN_BLOCKS = List.of(
            "#minecraft:coal_ores",
            "#minecraft:iron_ores",
            "#minecraft:copper_ores",
            "#minecraft:gold_ores",
            "#minecraft:redstone_ores",
            "#minecraft:lapis_ores",
            "#minecraft:diamond_ores",
            "#minecraft:emerald_ores",
            "minecraft:nether_quartz_ore",
            "minecraft:nether_gold_ore",
            "minecraft:ancient_debris");

    /** What a vein miner is worked with. */
    public static final List<String> DEFAULT_PICKAXES = List.of(
            "minecraft:wooden_pickaxe",
            "minecraft:stone_pickaxe",
            "minecraft:iron_pickaxe",
            "minecraft:golden_pickaxe",
            "minecraft:diamond_pickaxe",
            "minecraft:netherite_pickaxe");

    /**
     * The tree lopper as it comes.
     *
     * <p>The block and tool lists are left empty here and filled in from the names above as the
     * file is read, since only a server can say what is in a tag.
     */
    public static final LopperSettings TREE_DEFAULTS =
            new LopperSettings(Set.of(), Set.of(), DEFAULT_MAX_SIZE, false, false, false);

    /** The vein miner as it comes. */
    public static final LopperSettings VEIN_DEFAULTS =
            new LopperSettings(Set.of(), Set.of(), DEFAULT_MAX_SIZE, false, false, false);

    /** Copies both lists and holds the limit to something a run can reach. */
    public LopperSettings {
        blocks = Collections.unmodifiableSet(new LinkedHashSet<>(blocks));
        tools = Collections.unmodifiableSet(new LinkedHashSet<>(tools));
        maxSize = Math.max(0, maxSize);
    }

    /** These settings as the run itself reads them. */
    public LopperRules rules() {
        return new LopperRules(blocks, tools, maxSize, diagonals, anyListedBlock);
    }

    /** These settings following a different set of blocks. */
    public LopperSettings withBlocks(Set<Key> following) {
        return new LopperSettings(
                following, tools, maxSize, diagonals, anyListedBlock, singleUse);
    }

    /** These settings worked with a different set of tools. */
    public LopperSettings withTools(Set<Key> held) {
        return new LopperSettings(
                blocks, held, maxSize, diagonals, anyListedBlock, singleUse);
    }
}
