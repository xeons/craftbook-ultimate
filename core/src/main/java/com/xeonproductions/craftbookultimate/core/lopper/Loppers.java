// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.lopper;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Following one broken block outward to everything of its kind that touches it.
 *
 * <p>The whole of both the tree lopper and the vein miner. Breaking a log with an axe takes the
 * rest of the trunk; breaking an ore with a pickaxe takes the rest of the seam. They differ only
 * in {@link LopperRules}, so there is one of these rather than two that would drift apart.
 *
 * <p><b>Nearest first.</b> The run spreads a step at a time rather than following one branch to
 * its end, so a limit reached partway through takes the blocks closest to the hand rather than
 * whichever direction happened to be looked at first. That matters at the limit and nowhere else,
 * which is exactly when a builder is watching.
 *
 * <p><b>The exact block, not the list.</b> The list of blocks says whether the mechanic engages at
 * all; what the run then follows is the block that was actually broken. Felling an oak does not
 * take the spruce growing against it, and mining iron does not take the gold beside it.
 * {@link LopperRules#anyListedBlock()} is how an operator asks for the other behaviour, which is
 * chiefly worth it for a seam that crosses from stone into deepslate and so changes its name
 * halfway.
 */
@NullMarked
public final class Loppers {

    /** The six blocks sharing a face. */
    private static final Vec3i[] TOUCHING = {
        new Vec3i(0, 1, 0),
        new Vec3i(0, -1, 0),
        new Vec3i(1, 0, 0),
        new Vec3i(-1, 0, 0),
        new Vec3i(0, 0, 1),
        new Vec3i(0, 0, -1),
    };

    /** Every block sharing a face, an edge or a corner. */
    private static final Vec3i[] TOUCHING_AT_ALL = corners();

    private Loppers() {
    }

    /**
     * Everything one run takes, beginning with the block that was broken by hand.
     *
     * <p>The first entry is always the starting block, so a caller that has already broken it can
     * skip past it and one that has not can break the lot.
     *
     * @param start where the hand landed
     * @param rules what may be followed and how far
     * @param alsoTake blocks that count as part of the run whatever the start was, which is how a
     *     tree lopper takes leaves; empty for a run that follows only its own kind
     * @param sight the blocks, as the run may read them
     * @return the blocks to take, nearest first, or nothing at all where the run never began
     */
    public static List<Vec3i> reach(
            Vec3i start, LopperRules rules, Set<Key> alsoTake, LopperSight sight) {
        if (!rules.runsAtAll() || !sight.isReadable(start)) {
            return List.of();
        }

        Key kind = sight.blockAt(start);
        if (!rules.follows(kind)) {
            return List.of();
        }

        Vec3i[] neighbours = rules.diagonals() ? TOUCHING_AT_ALL : TOUCHING;
        List<Vec3i> taken = new ArrayList<>();
        Set<Vec3i> seen = new LinkedHashSet<>();
        Deque<Vec3i> queue = new ArrayDeque<>();

        seen.add(start);
        queue.add(start);

        while (!queue.isEmpty() && taken.size() < rules.maxSize()) {
            Vec3i at = queue.removeFirst();
            taken.add(at);

            for (Vec3i step : neighbours) {
                Vec3i next = at.add(step);
                if (!seen.add(next) || !sight.isReadable(next)) {
                    continue;
                }
                if (belongs(sight.blockAt(next), kind, rules, alsoTake)) {
                    queue.addLast(next);
                }
            }
        }

        return List.copyOf(taken);
    }

    /** Whether a block found beside the run is part of it. */
    private static boolean belongs(
            Key found, Key kind, LopperRules rules, Set<Key> alsoTake) {
        if (alsoTake.contains(found)) {
            return true;
        }
        return rules.anyListedBlock() ? rules.follows(found) : found.equals(kind);
    }

    /** The twenty-six blocks touching one, in any way at all. */
    private static Vec3i[] corners() {
        List<Vec3i> around = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        around.add(new Vec3i(x, y, z));
                    }
                }
            }
        }
        return around.toArray(new Vec3i[0]);
    }
}
