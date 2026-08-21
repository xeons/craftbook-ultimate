// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that read the world and report what they find.
 *
 * <p>All of them share a shape: they answer a yes-or-no question about somewhere near the sign,
 * and drive their output accordingly. They only look while something is driving them, so a
 * sensor with nothing wired to it costs nothing.
 *
 * <p>Positions are measured from the block the sign hangs on rather than from the sign, so the
 * sign itself is never part of what is being sensed.
 */
@NullMarked
public final class Sensors {

    /** The line a sensor's vertical offset is written on. */
    private static final int OFFSET_LINE = 2;

    /** How far below the backing block a sensor looks when the sign does not say. */
    private static final int DEFAULT_OFFSET = -1;

    private Sensors() {}

    /**
     * Outputs high while the block being watched is water.
     *
     * <p>Line 2 gives a vertical offset from the block the sign hangs on, defaulting to one
     * block below it.
     */
    public static ICLogic waterSensor() {
        return blockSensor(ChipWorld::isWater);
    }

    /** Outputs high while the block being watched is lava, as {@link #waterSensor()} does. */
    public static ICLogic lavaSensor() {
        return blockSensor(ChipWorld::isLava);
    }

    /**
     * Outputs high while the block being watched is at least as bright as a threshold.
     *
     * <p>Line 2 gives the threshold, defaulting to 8, which is the level below which hostile mobs
     * can spawn. Line 3 gives a vertical offset from the block the sign hangs on.
     */
    public static ICLogic lightSensor() {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            int threshold = (int) configValue(state, OFFSET_LINE, 8);
            int offset = (int) configValue(state, OFFSET_LINE + 1, 0);
            Vec3i target = state.backPosition().add(0, offset, 0);

            state.setMainOutput(state.world().lightLevel(target) >= threshold);
        };
    }

    /** Outputs high while it is raining or snowing. */
    public static ICLogic rainSensor() {
        return state -> {
            if (state.isAnyInputActive()) {
                state.setMainOutput(state.world().isRaining());
            }
        };
    }

    /** Outputs high while a thunderstorm is running. */
    public static ICLogic stormSensor() {
        return state -> {
            if (state.isAnyInputActive()) {
                state.setMainOutput(state.world().isThundering());
            }
        };
    }

    /**
     * Outputs high while a named block can be found in the column below the sign.
     *
     * <p>Line 2 names the block to look for. Line 3 gives how far down to search, defaulting to
     * the bottom of the world.
     *
     * <p>The column is searched from the block below the sign downwards, and the last depth a
     * block was found at is tried first on the next run, since a block usually has not moved.
     */
    public static SelfTriggeringICLogic blockDetector() {
        return new BlockDetectorLogic();
    }

    /**
     * Builds a sensor that asks a question about one block.
     *
     * @param test what makes the sensor output high
     */
    private static ICLogic blockSensor(BiPredicate<ChipWorld, Vec3i> test) {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            int offset = (int) configValue(state, OFFSET_LINE, DEFAULT_OFFSET);
            Vec3i target = state.backPosition().add(0, offset, 0);

            state.setMainOutput(test.test(state.world(), target));
        };
    }

    /** Reads a whole number from a sign line, falling back when it is missing or unreadable. */
    private static long configValue(ChipState state, int line, long fallback) {
        String text = state.sign().trimmedText(line);
        if (text.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Searches a column for a named block, remembering where it last found one. */
    private static final class BlockDetectorLogic implements SelfTriggeringICLogic {

        /** The line naming the block to look for. */
        private static final int BLOCK_LINE = 2;

        /** The line giving how far down to search. */
        private static final int DEPTH_LINE = 3;

        /** Means "nothing found", and is outside any world's vertical bounds. */
        private static final int NOT_FOUND = Integer.MIN_VALUE;

        private int lastFoundY = NOT_FOUND;

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                search(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            search(state);
        }

        private void search(ChipState state) {
            Optional<Key> wanted = Blocks.parse(state.sign().trimmedText(BLOCK_LINE));
            if (wanted.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            Predicate<Vec3i> matches = position -> state.world().blockAt(position).equals(wanted.get());
            Vec3i origin = state.backPosition();
            int top = origin.y() - 1;
            int bottom = bottomOf(state, top);

            // The block usually has not moved, so the depth it was last found at is worth trying
            // before walking the whole column again.
            if (lastFoundY != NOT_FOUND && lastFoundY <= top && lastFoundY >= bottom
                    && matches.test(new Vec3i(origin.x(), lastFoundY, origin.z()))) {
                state.setMainOutput(true);
                return;
            }

            for (int y = top; y >= bottom; y--) {
                if (matches.test(new Vec3i(origin.x(), y, origin.z()))) {
                    lastFoundY = y;
                    state.setMainOutput(true);
                    return;
                }
            }

            lastFoundY = NOT_FOUND;
            state.setMainOutput(false);
        }

        /** How far down to search, never past the bottom of the world. */
        private static int bottomOf(ChipState state, int top) {
            long depth = configValue(state, DEPTH_LINE, Long.MAX_VALUE);
            int worldBottom = state.world().minHeight();
            if (depth <= 0 || depth == Long.MAX_VALUE) {
                return worldBottom;
            }
            return (int) Math.max(worldBottom, top - depth + 1);
        }
    }
}
