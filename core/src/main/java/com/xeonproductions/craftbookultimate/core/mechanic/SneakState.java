// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/**
 * Whether somebody has to be crouching to work a mechanic.
 *
 * <p>Three answers rather than two, which is what the fork had and what a server needs: some
 * mechanics live on blocks that do something else when clicked, and crouching is how a builder says
 * which of the two they meant.
 */
@NullMarked
public enum SneakState {

    /** Only while crouching. */
    MUST,

    /** Only while not crouching, which is the usual way round. */
    MUST_NOT,

    /** Either way. */
    EITHER;

    /** Whether somebody may work the mechanic. */
    public boolean passes(boolean sneaking) {
        return switch (this) {
            case MUST -> sneaking;
            case MUST_NOT -> !sneaking;
            case EITHER -> true;
        };
    }

    /**
     * What an operator wrote in the settings, or the fallback where it says nothing usable.
     *
     * <p>{@code true} and {@code false} are accepted alongside the names, because that is what the
     * fork's own files hold and an operator moving one across should not have to translate it.
     */
    public static SneakState of(String written, SneakState fallback) {
        return switch (written.trim().toLowerCase(Locale.ROOT)) {
            case "must", "yes", "true" -> MUST;
            case "must-not", "must_not", "no", "false" -> MUST_NOT;
            case "either", "both", "ignore" -> EITHER;
            default -> fallback;
        };
    }

    /** How this is written in the settings file. */
    public String written() {
        return switch (this) {
            case MUST -> "must";
            case MUST_NOT -> "must-not";
            case EITHER -> "either";
        };
    }
}
