// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What the tree lopper does beyond what a lopper run does.
 *
 * <p>Three things a vein miner has no use for: leaves, which are part of a tree and not part of a
 * seam; whether to take them; and whether to put a sapling back where the trunk stood. Everything
 * else the two share is in {@link LopperSettings}, so a change to how a run is followed reaches
 * both rather than one.
 *
 * @param lopper what the run itself follows and how far
 * @param leaves what counts as leaves, already expanded from whatever tags the file named
 * @param breakLeaves whether the leaves come down with the trunk
 * @param placeSaplings whether a sapling is put back where the trunk stood
 */
@NullMarked
public record TreeSettings(
        LopperSettings lopper, Set<Key> leaves, boolean breakLeaves, boolean placeSaplings) {

    /** The tree lopper as it comes: trunk only, and nothing replanted. */
    public static final TreeSettings DEFAULTS =
            new TreeSettings(LopperSettings.TREE_DEFAULTS, Set.of(), false, false);

    /** Copies the list of leaves. */
    public TreeSettings {
        leaves = Collections.unmodifiableSet(new LinkedHashSet<>(leaves));
    }

    /**
     * The blocks a run takes on top of its own kind.
     *
     * <p>The leaves, and only when it has been asked for them: a run told to leave them alone must
     * not follow them either, or a canopy would carry the run from one trunk to the next.
     */
    public Set<Key> alsoTaken() {
        return breakLeaves ? leaves : Set.of();
    }

    /** These settings with a different run under them. */
    public TreeSettings withLopper(LopperSettings run) {
        return new TreeSettings(run, leaves, breakLeaves, placeSaplings);
    }

    /** These settings with a different idea of what leaves are. */
    public TreeSettings withLeaves(Set<Key> canopy) {
        return new TreeSettings(lopper, canopy, breakLeaves, placeSaplings);
    }

    /** These settings taking the leaves down with the trunk, or leaving them. */
    public TreeSettings withBreakLeaves(boolean taking) {
        return new TreeSettings(lopper, leaves, taking, placeSaplings);
    }

    /** These settings replanting what they fell, or not. */
    public TreeSettings withPlaceSaplings(boolean replanting) {
        return new TreeSettings(lopper, leaves, breakLeaves, replanting);
    }
}
