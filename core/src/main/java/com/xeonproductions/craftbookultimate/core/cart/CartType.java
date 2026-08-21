// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The kinds of minecart a mechanic can tell apart.
 *
 * <p>The names here are the ones a sign uses, which is why {@code storage} rather than chest and
 * {@code powered} rather than furnace: those are what builders have written on rails for years.
 */
@NullMarked
public enum CartType {

    /** The plain minecart, which is the one people ride. */
    RIDEABLE("minecart"),

    /** The chest minecart, written as {@code storage}. */
    CHEST("storage"),

    /** The furnace minecart, written as {@code powered}. */
    FURNACE("powered"),

    /** The hopper minecart. */
    HOPPER("hopper"),

    /** The TNT minecart. */
    TNT("tnt"),

    /** The spawner minecart, which no sign names but which still rolls. */
    SPAWNER("spawner"),

    /** The command block minecart, which no sign names but which still rolls. */
    COMMAND_BLOCK("command");

    private final String signName;

    CartType(String signName) {
        this.signName = signName;
    }

    /** What a sign calls this kind of cart. */
    public String signName() {
        return signName;
    }

    /** Whether this kind of cart holds items of its own. */
    public boolean holdsItems() {
        return this == CHEST || this == HOPPER;
    }

    /** The kind of cart a name on a sign refers to, if it refers to one. */
    public static Optional<CartType> bySignName(String written) {
        String name = written.trim().toLowerCase(Locale.ROOT);
        for (CartType type : values()) {
            if (type.signName.equals(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
