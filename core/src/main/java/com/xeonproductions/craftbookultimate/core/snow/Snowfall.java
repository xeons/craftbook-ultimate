// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.snow;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import com.xeonproductions.craftbookultimate.core.config.SnowSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Snow that piles, slumps and melts.
 *
 * <p>Not a mechanic anybody builds — it changes what snow does everywhere, which is why every part
 * of it is off until an operator says otherwise, and why what is here is decisions rather than a
 * sign.
 *
 * <p>All of it is arithmetic on depths through {@link SnowWorld}, so the whole mechanic is
 * exercised without a server. That matters more here than usual: snow that piles wrongly is the
 * kind of thing that ruins a landscape quietly and is noticed a week later.
 */
@NullMarked
public final class Snowfall {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.SNOW;

    /** The ways snow slumps: down first, then out. */
    private static final List<Vec3i> SLUMPS = List.of(
            new Vec3i(0, -1, 0),
            new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
            new Vec3i(0, 0, 1), new Vec3i(0, 0, -1));

    /** How deep the game itself leaves snow, and so how far a partial melt goes back to. */
    private static final int VANILLA_DEPTH = 1;

    private final SnowWorld world;
    private final SnowSettings settings;

    public Snowfall(SnowWorld world, SnowSettings settings) {
        this.world = world;
        this.settings = settings;
    }

    /**
     * What one snowy block does when the game gets round to it.
     *
     * <p>Three things it might be doing, in the order they take precedence: gathering more where it
     * is cold and the sky is open, going away where it is warm and the sky is open, and otherwise
     * slumping into whatever is lower beside it.
     */
    public void tick(Vec3i at) {
        if (world.depthAt(at) <= 0) {
            return;
        }

        if (world.isFreezing(at) && world.seesSky(at)) {
            pile(at);
            return;
        }
        if (settings.meltsInSunlight() && world.isWarm(at) && world.seesSky(at)) {
            melt(at);
            return;
        }
        slump(at);
    }

    /**
     * Adds a layer of snow wherever it would come to rest.
     *
     * <p>Snow falls before it settles: a layer landing over open air goes down to whatever is under
     * it rather than hanging where it arrived. The walk downward is bounded by the world's own
     * floor, which is what stops a column of air over the void from looping.
     */
    public void pile(Vec3i at) {
        Vec3i resting = restingPlace(at);
        int depth = world.depthAt(resting);

        if (depth >= SnowWorld.FULL) {
            // A full block already; anything more goes on top, and only where that is wanted.
            if (settings.piling() && resting.y() < world.ceiling()) {
                pile(resting.add(0, 1, 0));
            }
            return;
        }

        if (depth == 0 && !supports(resting.add(0, -1, 0))) {
            return;
        }

        int deeper = depth + 1;
        if (deeper >= SnowWorld.FULL && !settings.piling()) {
            deeper = SnowWorld.FULL - 1;
        }
        if (deeper == depth) {
            return;
        }

        world.setDepth(resting, deeper);

        if (settings.freezesWater()) {
            Vec3i below = resting.add(0, -1, 0);
            if (world.isWater(below)) {
                world.freeze(below);
            }
        }
        slump(resting);
    }

    /** Takes a layer away, and the last one clears the ground. */
    public void melt(Vec3i at) {
        int depth = world.depthAt(at);
        if (depth <= 0) {
            return;
        }
        if (settings.partialMeltOnly() && depth <= VANILLA_DEPTH) {
            return;
        }
        world.setDepth(at, depth - 1);
    }

    /**
     * Lets a pile slump into whatever is lower beside it.
     *
     * <p>Only into the lowest neighbour, and only where that is more than a layer shallower, so
     * snow settles into a slope rather than shuffling back and forth between two places that
     * differ by one. A single layer only ever falls downward — a dusting does not spread sideways.
     */
    public void slump(Vec3i at) {
        if (!settings.dispersion()) {
            return;
        }

        int depth = world.depthAt(at);
        if (depth <= 0) {
            return;
        }

        Optional<Vec3i> lowest = lowestBeside(at, depth);
        if (lowest.isEmpty()) {
            return;
        }

        Vec3i into = lowest.get();
        world.setDepth(at, depth - 1);
        world.setDepth(into, world.depthAt(into) + 1);
    }

    /** Where a layer landing here would come to rest, having fallen through whatever is clear. */
    private Vec3i restingPlace(Vec3i at) {
        Vec3i resting = at;
        while (resting.y() > world.floor()
                && world.depthAt(resting) == 0
                && world.isClear(resting)
                && !supports(resting.add(0, -1, 0))) {
            resting = resting.add(0, -1, 0);
        }
        return resting;
    }

    /**
     * Whether snow can rest on top of a place.
     *
     * <p>A full block of snow holds snow, which is what lets a drift grow past one block. Asked
     * here rather than left to each binding to remember, because forgetting it does not merely
     * misplace a layer: snow piled above a full block falls straight back into it and piles again,
     * for ever.
     */
    private boolean supports(Vec3i at) {
        return world.canRestOn(at) || world.depthAt(at) >= SnowWorld.FULL;
    }

    /** The neighbour a layer would slide into, if any is low enough to take one. */
    private Optional<Vec3i> lowestBeside(Vec3i at, int depth) {
        Vec3i found = null;
        int shallowest = depth;

        for (Vec3i step : SLUMPS) {
            // A single layer is a dusting, and a dusting only ever falls.
            if (depth == 1 && step.y() == 0) {
                continue;
            }

            Vec3i beside = at.add(step);
            if (!supports(beside.add(0, -1, 0)) && world.depthAt(beside) == 0) {
                continue;
            }

            int there = world.depthAt(beside);
            if (there < shallowest - 1) {
                shallowest = there;
                found = beside;
            }
        }
        return Optional.ofNullable(found);
    }
}
