// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.physics;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Ladders that fall when what they were standing on goes away.
 *
 * <p>A ladder in the game hangs off the wall behind it and cares nothing for what is underneath,
 * so mining the bottom rung of a shaft leaves the rest of the ladder floating. This makes a ladder
 * behave the way a builder expects it to: take the bottom one out and the column above comes down.
 *
 * <p>It comes down one rung at a time rather than all at once. The rung above an empty space falls
 * as a block, which empties the space it was in, which is what makes the next rung fall — so the
 * whole column unzips itself downward and each falling block lands on whatever the last one
 * settled into.
 *
 * <p>The only part of {@code BetterPhysics} either source codebase has. Both carry exactly this
 * one behaviour behind a setting called {@code falling-ladders}, and the name of the mechanic is
 * kept even so, since a setting an operator already knows is worth more than a tidier name.
 */
@NullMarked
public final class FallingLadders {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.BETTER_PHYSICS;

    /** The block this is about. */
    public static final Key LADDER = Key.key("minecraft:ladder");

    private FallingLadders() {
    }

    /**
     * Whether a block is a ladder with nothing under it, and so should fall.
     *
     * @param block what is at the place being asked about
     * @param below what is directly under it
     */
    public static boolean falls(Key block, Key below) {
        return LADDER.equals(block) && Blocks.AIR.contains(below);
    }
}
