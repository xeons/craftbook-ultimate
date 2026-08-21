// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.farm;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What each plantable item becomes when it is put in the ground.
 *
 * <p>Only the item to block half is recorded. What a given plant will grow on is the game's own
 * business and is asked of the world rather than listed here, so a plant added by a later version
 * needs one line rather than a table of soils.
 *
 * <p>Cocoa beans are the odd one out: they grow on the side of a jungle log rather than on top of
 * anything, so the block they become has to face the log it is attached to.
 */
@NullMarked
public final class Plantables {

    /** Cocoa, which attaches to the side of a log rather than standing on the ground. */
    public static final Key COCOA = Blocks.key("cocoa");

    private static final Map<Key, Key> PLANTED = Map.ofEntries(
            Map.entry(Blocks.key("wheat_seeds"), Blocks.key("wheat")),
            Map.entry(Blocks.key("carrot"), Blocks.key("carrots")),
            Map.entry(Blocks.key("potato"), Blocks.key("potatoes")),
            Map.entry(Blocks.key("beetroot_seeds"), Blocks.key("beetroots")),
            Map.entry(Blocks.key("melon_seeds"), Blocks.key("melon_stem")),
            Map.entry(Blocks.key("pumpkin_seeds"), Blocks.key("pumpkin_stem")),
            Map.entry(Blocks.key("nether_wart"), Blocks.key("nether_wart")),
            Map.entry(Blocks.key("torchflower_seeds"), Blocks.key("torchflower_crop")),
            Map.entry(Blocks.key("pitcher_pod"), Blocks.key("pitcher_crop")),
            Map.entry(Blocks.key("sweet_berries"), Blocks.key("sweet_berry_bush")),
            Map.entry(Blocks.key("cocoa_beans"), COCOA),
            Map.entry(Blocks.key("oak_sapling"), Blocks.key("oak_sapling")),
            Map.entry(Blocks.key("spruce_sapling"), Blocks.key("spruce_sapling")),
            Map.entry(Blocks.key("birch_sapling"), Blocks.key("birch_sapling")),
            Map.entry(Blocks.key("jungle_sapling"), Blocks.key("jungle_sapling")),
            Map.entry(Blocks.key("acacia_sapling"), Blocks.key("acacia_sapling")),
            Map.entry(Blocks.key("dark_oak_sapling"), Blocks.key("dark_oak_sapling")),
            Map.entry(Blocks.key("cherry_sapling"), Blocks.key("cherry_sapling")),
            Map.entry(Blocks.key("pale_oak_sapling"), Blocks.key("pale_oak_sapling")),
            Map.entry(Blocks.key("mangrove_propagule"), Blocks.key("mangrove_propagule")));

    private Plantables() {}

    /**
     * The block an item becomes once planted.
     *
     * @return the block, or empty if the item is not something that can be planted
     */
    public static Optional<Key> plantedForm(Key item) {
        return Optional.ofNullable(PLANTED.get(item));
    }

    /** Whether an item can be planted at all. */
    public static boolean isPlantable(Key item) {
        return PLANTED.containsKey(item);
    }

    /** Whether a planted block attaches to the side of something rather than standing on it. */
    public static boolean attachesSideways(Key planted) {
        return COCOA.equals(planted);
    }

    /** Every item that can be planted, and what it becomes. */
    public static Map<Key, Key> all() {
        return PLANTED;
    }
}
