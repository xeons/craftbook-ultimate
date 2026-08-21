// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.entity;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * One potion effect, for how long and how strongly.
 *
 * @param effect which effect, named the way the game names it
 * @param durationTicks how long it lasts
 * @param amplifier the strength, where zero is the first level a potion bottle gives
 */
@NullMarked
public record PotionDose(Key effect, int durationTicks, int amplifier) {

    /** How long a dose written as lasting forever runs for. */
    public static final int FOREVER_TICKS = Integer.MAX_VALUE;

    public PotionDose {
        if (durationTicks < 1) {
            throw new IllegalArgumentException("A dose must last at least one tick");
        }
        if (amplifier < 0) {
            throw new IllegalArgumentException("A dose cannot be weaker than its first level");
        }
    }
}
