// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.lopper;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What one run of the lopper is allowed to take.
 *
 * <p>The whole of what separates felling a tree from mining out a vein, which is why the two
 * mechanics share one engine: both start from a block somebody has just broken, follow the blocks
 * touching it that are the same sort of thing, and stop at a limit. A tree lopper is these rules
 * pointed at logs and held in an axe, and a vein miner is these rules pointed at ores and held in
 * a pickaxe.
 *
 * @param blocks what may be followed, already expanded from whatever tags the file named
 * @param tools what has to be in the hand for any of it to happen
 * @param maxSize the most blocks one run may take, counting the one that was broken by hand
 * @param diagonals whether blocks touching only at an edge or a corner count as connected
 * @param anyListedBlock whether a run follows anything on its list rather than only more of the
 *     block that was broken. Off, because felling an oak should not take the spruce against it;
 *     on is worth having for an ore seam that crosses from stone into deepslate and changes its
 *     name halfway
 */
@NullMarked
public record LopperRules(
        Set<Key> blocks,
        Set<Key> tools,
        int maxSize,
        boolean diagonals,
        boolean anyListedBlock) {

    /** Copies both lists and holds the limit to something a run can actually reach. */
    public LopperRules {
        blocks = Collections.unmodifiableSet(new LinkedHashSet<>(blocks));
        tools = Collections.unmodifiableSet(new LinkedHashSet<>(tools));
        maxSize = Math.max(0, maxSize);
    }

    /** Whether a block is one of the kinds this run follows. */
    public boolean follows(Key block) {
        return blocks.contains(block);
    }

    /** Whether what somebody is holding works this mechanic. */
    public boolean worksWith(Key tool) {
        return tools.contains(tool);
    }

    /**
     * Whether a run may happen at all.
     *
     * <p>A limit of zero switches the mechanic off without a second setting to say so, in the same
     * way a cart habit's number does, and an empty list of either kind means there is nothing to
     * follow or nothing to follow it with.
     */
    public boolean runsAtAll() {
        return maxSize > 0 && !blocks.isEmpty() && !tools.isEmpty();
    }
}
