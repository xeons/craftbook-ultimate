// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.powerable;

import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A block that answers redstone without a sign, a mechanism or anything built.
 *
 * <p>Unlike a sign mechanic there is nothing to declare and nothing to look at: a glowstone is a
 * glowstone until somebody runs redstone to it, and then it is a lamp. What makes one is the block
 * itself, which is why these are found by looking at what changed rather than by an index.
 *
 * <p>Two shapes cover all of them. One turns into a different block and back; the other lights a
 * fire on top of itself. Everything else about them — which blocks, and whether they go out again
 * when their power source is mined away — is a setting.
 */
@NullMarked
public sealed interface Powerable {

    /** What this is called, for the setting that switches it off. */
    String name();

    /** Whether this is a block this mechanic works on at all. */
    boolean worksOn(Key block);

    /**
     * A block that becomes another block when it is powered.
     *
     * <p>Glowstone becoming soul sand, and a pumpkin becoming a jack o'lantern. Where the block has
     * a facing, the binding keeps it: a carved pumpkin that turned round when it lit would be a
     * worse lamp than one that did not light at all.
     *
     * @param unpowered what it is with no power reaching it
     * @param powered what it becomes when power arrives
     */
    record Swap(String name, Key unpowered, Key powered) implements Powerable {

        @Override
        public boolean worksOn(Key block) {
            return block.equals(unpowered) || block.equals(powered);
        }

        /** What the block should be, given whether power is reaching it. */
        public Key wanted(boolean powered) {
            return powered ? this.powered : unpowered;
        }
    }

    /**
     * A block that carries a fire on top of itself while it is powered.
     *
     * <p>Netherrack, and whatever else an operator names. The block itself never changes — what
     * changes is the air above it — so this is the one shape where what is read to tell whether it
     * is already on is a different block from the one that makes it.
     */
    record Fire(String name, Set<Key> blocks) implements Powerable {

        public Fire {
            blocks = Set.copyOf(blocks);
        }

        @Override
        public boolean worksOn(Key block) {
            return blocks.contains(block);
        }
    }
}
