// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.entity;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A stack of items as a chip sees it.
 *
 * <p>Enough to answer what the sensing chips ask: what it is, how many, and whether somebody has
 * renamed it or written on it. Anything else an item carries stays on the server side of the seam.
 *
 * @param type what the item is, named the way the game names it
 * @param count how many are in the stack
 * @param displayName the name it has been given, if it has one
 * @param lore the lines written on it, which is empty for most items
 */
@NullMarked
public record ItemView(Key type, int count, Optional<String> displayName, List<String> lore) {

    public ItemView {
        lore = List.copyOf(lore);
    }

    /** A plain stack that nobody has renamed or written on. */
    public static ItemView of(Key type, int count) {
        return new ItemView(type, count, Optional.empty(), List.of());
    }

    /** Whether any of the lines written on it contains a fragment. */
    public boolean loreContains(String fragment) {
        for (String line : lore) {
            if (line.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
