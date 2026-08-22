// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.lopper;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import org.jspecify.annotations.NullMarked;

/**
 * Mining a whole seam by breaking one ore of it.
 *
 * <p>The tree lopper pointed downward, and deliberately so: it is the same {@link Loppers} run
 * with a different list of blocks and a different list of tools, so the two cannot disagree about
 * what "connected" means or about which of them honours a limit.
 *
 * <p>New here rather than ported from either source codebase, which is why it has no frozen
 * anything: no sign, no model number, and settings free to be whatever reads best. What it does
 * have is the same shape as the tree lopper, since a builder who has learnt one has learnt both.
 */
@NullMarked
public final class VeinMiners {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.VEIN_MINER;

    /** The permission to mine a seam. */
    public static final String USE = "craftbook.veinminer.use";

    /** The permission to turn it off for oneself. */
    public static final String TOGGLE = "craftbook.veinminer.toggle";

    private VeinMiners() {
    }
}
