// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.mechanic.SneakState;
import com.xeonproductions.craftbookultimate.core.mechanic.XpStorers;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the block that bottles experience.
 *
 * @param block what turns experience into bottles when it is clicked
 * @param perBottle how much experience one bottle costs
 * @param requiresBottle whether the player must be carrying empty bottles to fill
 * @param sneaking whether the player must be crouching to use one
 */
@NullMarked
public record XpSettings(
        Key block, int perBottle, boolean requiresBottle, SneakState sneaking) {

    /** What turns experience into bottles, as the fork had it. */
    public static final Key DEFAULT_BLOCK = Key.key("minecraft:spawner");

    /** The experience store as it has always worked. */
    public static final XpSettings DEFAULTS = new XpSettings(
            DEFAULT_BLOCK, XpStorers.DEFAULT_PER_BOTTLE, false, SneakState.MUST_NOT);

    /** Holds the price to something a bottle can be paid for with. */
    public XpSettings {
        perBottle = Math.max(1, perBottle);
    }

    /** These settings with a different block doing the bottling. */
    public XpSettings withBlock(Key doing) {
        return new XpSettings(doing, perBottle, requiresBottle, sneaking);
    }

    /** These settings with a bottle costing something else. */
    public XpSettings withPerBottle(int cost) {
        return new XpSettings(block, cost, requiresBottle, sneaking);
    }

    /** These settings with empty bottles needed or not. */
    public XpSettings withRequiresBottle(boolean required) {
        return new XpSettings(block, perBottle, required, sneaking);
    }

    /** These settings with a different rule about crouching. */
    public XpSettings withSneaking(SneakState state) {
        return new XpSettings(block, perBottle, requiresBottle, state);
    }
}
