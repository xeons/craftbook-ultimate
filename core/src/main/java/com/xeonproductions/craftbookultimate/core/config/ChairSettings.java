// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the chairs.
 *
 * @param blocks what may be sat on
 * @param requireSign whether a chair only works with a sign beside it
 * @param maxSignDistance how far from the clicked block that sign may be
 * @param faceCorrectDirection whether sitting down turns somebody to face the way the block does
 * @param exitAtEntry whether standing up puts somebody back where they sat down from
 * @param healAmount how much a healing chair heals by each turn
 * @param healRate how many ticks apart those turns are
 */
@NullMarked
public record ChairSettings(
        Set<Key> blocks,
        boolean requireSign,
        int maxSignDistance,
        boolean faceCorrectDirection,
        boolean exitAtEntry,
        double healAmount,
        int healRate) {

    /**
     * What is written into the file when nobody has said otherwise.
     *
     * <p>The tag rather than the sixty-odd blocks in it, because that is one line an operator can
     * read and it gains every stair a later version of the game adds. The list below is what the
     * tag comes to on a server that has it; a server whose tag is empty falls back to it, which is
     * what keeps a settings file readable without the meaning depending on the file.
     */
    public static final List<String> DEFAULT_BLOCK_NAMES = List.of("#minecraft:stairs");

    /** Every stair, which is what a chair is out of the box. */
    private static final String[] STAIRS = {
        "oak_stairs", "spruce_stairs", "birch_stairs", "jungle_stairs", "acacia_stairs",
        "dark_oak_stairs", "pale_oak_stairs", "mangrove_stairs", "cherry_stairs", "bamboo_stairs",
        "bamboo_mosaic_stairs", "crimson_stairs", "warped_stairs",
        "stone_stairs", "cobblestone_stairs", "mossy_cobblestone_stairs", "stone_brick_stairs",
        "mossy_stone_brick_stairs", "granite_stairs", "polished_granite_stairs", "diorite_stairs",
        "polished_diorite_stairs", "andesite_stairs", "polished_andesite_stairs",
        "cobbled_deepslate_stairs", "polished_deepslate_stairs", "deepslate_brick_stairs",
        "deepslate_tile_stairs", "tuff_stairs", "polished_tuff_stairs", "tuff_brick_stairs",
        "sandstone_stairs", "smooth_sandstone_stairs", "red_sandstone_stairs",
        "smooth_red_sandstone_stairs", "brick_stairs", "mud_brick_stairs", "nether_brick_stairs",
        "red_nether_brick_stairs", "quartz_stairs", "smooth_quartz_stairs", "purpur_stairs",
        "prismarine_stairs", "prismarine_brick_stairs", "dark_prismarine_stairs",
        "blackstone_stairs", "polished_blackstone_stairs", "polished_blackstone_brick_stairs",
        "end_stone_brick_stairs", "resin_brick_stairs",
        "cut_copper_stairs", "exposed_cut_copper_stairs", "weathered_cut_copper_stairs",
        "oxidized_cut_copper_stairs", "waxed_cut_copper_stairs",
        "waxed_exposed_cut_copper_stairs", "waxed_weathered_cut_copper_stairs",
        "waxed_oxidized_cut_copper_stairs"
    };

    /** How much a healing chair heals by, as the fork had it: half a heart. */
    public static final double DEFAULT_HEAL_AMOUNT = 1.0;

    /** How many ticks apart it does it, as the fork had it: half a second. */
    public static final int DEFAULT_HEAL_RATE = 10;

    /** The chairs as they have always been sat in. */
    public static final ChairSettings DEFAULTS = new ChairSettings(
            defaultBlocks(), false, 3, true, false, DEFAULT_HEAL_AMOUNT, DEFAULT_HEAL_RATE);

    /** Copies the list and holds both numbers to something a chair can work with. */
    public ChairSettings {
        blocks = Collections.unmodifiableSet(new LinkedHashSet<>(blocks));
        maxSignDistance = Math.max(0, maxSignDistance);
        healAmount = Math.max(0, healAmount);
        healRate = Math.max(1, healRate);
    }

    /** Whether a block is one somebody may sit on. */
    public boolean allows(Key block) {
        return blocks.contains(block);
    }

    /** Whether a healing chair does anything at all, which it does not at no rate or no amount. */
    public boolean heals() {
        return healAmount > 0;
    }

    /** These settings with a different set of blocks to sit on. */
    public ChairSettings withBlocks(Set<Key> seats) {
        return new ChairSettings(seats, requireSign, maxSignDistance, faceCorrectDirection,
                exitAtEntry, healAmount, healRate);
    }

    /** These settings with a sign needed, or not. */
    public ChairSettings withRequireSign(boolean required) {
        return new ChairSettings(blocks, required, maxSignDistance, faceCorrectDirection,
                exitAtEntry, healAmount, healRate);
    }

    /** These settings with a sign allowed to be further off, or nearer. */
    public ChairSettings withMaxSignDistance(int distance) {
        return new ChairSettings(blocks, requireSign, distance, faceCorrectDirection,
                exitAtEntry, healAmount, healRate);
    }

    /** These settings with sitting down turning somebody, or leaving them as they were. */
    public ChairSettings withFaceCorrectDirection(boolean turning) {
        return new ChairSettings(blocks, requireSign, maxSignDistance, turning,
                exitAtEntry, healAmount, healRate);
    }

    /** These settings with standing up returning somebody to where they sat down from. */
    public ChairSettings withExitAtEntry(boolean returning) {
        return new ChairSettings(blocks, requireSign, maxSignDistance, faceCorrectDirection,
                returning, healAmount, healRate);
    }

    /** These settings with a healing chair healing by something else. */
    public ChairSettings withHealAmount(double amount) {
        return new ChairSettings(blocks, requireSign, maxSignDistance, faceCorrectDirection,
                exitAtEntry, amount, healRate);
    }

    /** These settings with a healing chair healing at a different pace. */
    public ChairSettings withHealRate(int ticks) {
        return new ChairSettings(blocks, requireSign, maxSignDistance, faceCorrectDirection,
                exitAtEntry, healAmount, ticks);
    }

    private static Set<Key> defaultBlocks() {
        Set<Key> blocks = new LinkedHashSet<>();
        for (String name : STAIRS) {
            blocks.add(Blocks.key(name));
        }
        return Collections.unmodifiableSet(blocks);
    }
}
