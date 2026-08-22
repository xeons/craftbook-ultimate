// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the two meters.
 *
 * <p>Each is an item held up to a block, and the only setting either has is which item. They share
 * a record because they share a dial: what a reading looks like is the same for both.
 *
 * @param ammeterItem what is held to read a redstone power level off a block
 * @param lightStoneItem what is held to read a light level off a block
 */
@NullMarked
public record MeterSettings(Key ammeterItem, Key lightStoneItem) {

    /** What is held up to a block to read its redstone power. */
    public static final Key DEFAULT_AMMETER_ITEM = Key.key("minecraft:charcoal");

    /** What is held up to a block to read its light level. */
    public static final Key DEFAULT_LIGHT_STONE_ITEM = Key.key("minecraft:glowstone_dust");

    /** The meters as they have always been read. */
    public static final MeterSettings DEFAULTS =
            new MeterSettings(DEFAULT_AMMETER_ITEM, DEFAULT_LIGHT_STONE_ITEM);

    /** These settings with a different item reading redstone. */
    public MeterSettings withAmmeterItem(Key item) {
        return new MeterSettings(item, lightStoneItem);
    }

    /** These settings with a different item reading light. */
    public MeterSettings withLightStoneItem(Key item) {
        return new MeterSettings(ammeterItem, item);
    }
}
