// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.entity;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.jspecify.annotations.NullMarked;

/**
 * The sixteen dye colours, in the order signs name them by number.
 *
 * <p>A sign asking for a green sheep says {@code sheep@13}. That number is the wool damage value
 * from before the flattening, and the game still lists its dyes in the same order, so the position
 * in this list is the number a sign uses.
 */
@NullMarked
public final class DyeColours {

    /** The colours in the order their numbers run, from white at zero to black at fifteen. */
    private static final List<String> NAMES = List.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black");

    private DyeColours() {}

    /** The colour a number names, or empty if it names none. */
    public static Optional<String> byNumber(int number) {
        if (number < 0 || number >= NAMES.size()) {
            return Optional.empty();
        }
        return Optional.of(NAMES.get(number));
    }

    /** The number a colour is named by, or empty if it is not one of the sixteen. */
    public static OptionalInt numberOf(String name) {
        int index = NAMES.indexOf(name.toLowerCase(java.util.Locale.ROOT));
        return index < 0 ? OptionalInt.empty() : OptionalInt.of(index);
    }

    /** How many there are. */
    public static int count() {
        return NAMES.size();
    }
}
