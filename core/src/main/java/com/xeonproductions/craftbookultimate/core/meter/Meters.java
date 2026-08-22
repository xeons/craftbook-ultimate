// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.meter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Holding something up to a block to read a number off it.
 *
 * <p>Two of them, and they are the same instrument with a different dial. An ammeter reads how much
 * redstone power a block carries; a light meter reads how much light falls on the face that was
 * clicked. The fork drew both as a fifteen-mark bar and wrote the two drawings out separately, so
 * they are one here and only the colours differ.
 *
 * <p>Fifteen marks whichever it is, always, so the bar is the same width every time and two
 * readings can be compared by eye without reading the number. The colour is the reading: what a
 * builder wants at a glance is whether the answer is good, and only they know what good means.
 */
@NullMarked
public final class Meters {

    /** The largest either instrument can read. */
    public static final int FULL = 15;

    /** The light level at and above which nothing hostile will spawn. */
    public static final int LIT_ENOUGH = 9;

    /** What is held up to a block to read its power, unless an operator says otherwise. */
    public static final String NAME_AMMETER = "Ammeter";

    /** What is held up to a block to read its light, unless an operator says otherwise. */
    public static final String NAME_LIGHT_STONE = "LightStone";

    /** The permission to read a power level. */
    public static final String AMMETER_USE = "craftbook.ammeter.use";

    /** The permission to read a light level. */
    public static final String LIGHT_STONE_USE = "craftbook.lightstone.use";

    private Meters() {
    }

    /**
     * How much power a block is carrying.
     *
     * <p>Four bands rather than two, because a redstone builder is not asking a yes-or-no question:
     * they want to know how far a signal has left to run before it dies, and the colour says that
     * at a glance.
     */
    public static Component power(int level) {
        return reading("Ammeter", level, band -> {
            if (band > 10) {
                return NamedTextColor.DARK_GREEN;
            }
            if (band > 5) {
                return NamedTextColor.GOLD;
            }
            return band > 0 ? NamedTextColor.DARK_RED : NamedTextColor.BLACK;
        });
    }

    /**
     * How much light falls on a block.
     *
     * <p>Two bands, split where hostile mobs stop spawning, since that is the question anybody
     * holding a light meter is actually asking.
     */
    public static Component light(int level) {
        return reading("LightStone", level,
                band -> band >= LIT_ENOUGH ? NamedTextColor.GREEN : NamedTextColor.DARK_RED);
    }

    /** The bar, its label and its number. */
    private static Component reading(String label, int level, Dial dial) {
        int shown = Math.clamp(level, 0, FULL);
        TextColor lit = dial.colourAt(shown);

        Component bar = Component.text(label + ": [", NamedTextColor.YELLOW);
        if (shown > 0) {
            bar = bar.append(Component.text("|".repeat(shown), lit));
        }
        if (shown < FULL) {
            bar = bar.append(Component.text("|".repeat(FULL - shown), NamedTextColor.BLACK));
        }
        return bar.append(Component.text("]", NamedTextColor.YELLOW))
                .append(Component.text(" " + shown, NamedTextColor.WHITE));
    }

    /** What colour a reading is drawn in, which is the only thing the two instruments disagree on. */
    @FunctionalInterface
    private interface Dial {
        TextColor colourAt(int level);
    }
}
