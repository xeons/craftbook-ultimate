// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import org.jspecify.annotations.NullMarked;

/**
 * Whether a cart mechanic has been wired up, and if so whether it is switched on.
 *
 * <p>Three answers rather than two, because a mechanic with no redstone anywhere near it is not
 * the same as one somebody has deliberately switched off. Most cart mechanics work when nobody has
 * wired them at all and stop only when a wire that is there reads low, which is what lets a
 * builder add a switch to an existing mechanism without having to power it from then on.
 */
@NullMarked
public enum Wiring {

    /** Nothing is wired to it, so it works as it always has. */
    NONE,

    /** Something is wired to it and is off, so it is being held back. */
    OFF,

    /** Something is wired to it and is on. */
    ON;

    /**
     * Whether the mechanic should act.
     *
     * <p>True unless somebody has wired a switch and left it off.
     */
    public boolean allows() {
        return this != OFF;
    }
}
