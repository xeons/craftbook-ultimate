// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.illusion;

import org.jspecify.annotations.NullMarked;

/**
 * What somebody is shown of the weather, whatever the weather is really doing.
 *
 * <p>Only two things can be shown, because only two things are sent: whether it is coming down or
 * not. Snow rather than rain, and how hard, are the client's own business, worked out from where
 * the player is standing.
 */
@NullMarked
public enum Sky {

    /** Rain, or snow where it is cold enough for snow. */
    DOWNFALL,

    /** Nothing falling. */
    CLEAR,

    /** Whatever the world is really doing, which is what everybody sees by default. */
    REAL
}
