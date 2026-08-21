// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.sign.SignOffset;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.core.world.Placement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that swap one block for another rather than building anything.
 *
 * <p>Both work from a pair of blocks written on the sign as {@code first|second}. One swaps a
 * single block back and forth as its input changes; the other does the same and then lets the
 * change spread outward through every touching block of the same pair.
 */
@NullMarked
public final class BlockSwappers {

    /** The line carrying the pair of blocks. */
    private static final int PAIR_LINE = 2;

    /** What separates the two blocks of the pair. */
    private static final char PAIR_SEPARATOR = '|';

    private BlockSwappers() {}

    /**
     * Swaps one block between two kinds as its input changes.
     *
     * <p>Line 3 names the pair as {@code driven|idle}, and line 4 gives one axis step from the
     * block the sign hangs on, such as {@code Y+1}.
     *
     * <p>The chip pays for what it puts down and is refunded what it takes up, so switching back
     * and forth costs nothing over time but the two blocks have to be somewhere it can reach.
     * Anything else standing in the target position is left alone, since a chip that overwrote it
     * would be a way of quietly taking somebody's blocks apart.
     */
    public static ICLogic toggleBlock() {
        return new ToggleBlock();
    }

    /**
     * Swaps a block between two kinds and lets the change spread to its neighbours.
     *
     * <p>Line 3 names the pair as {@code driven|idle}. Line 4 reads {@code delay:mode:physics}:
     * the delay is how many ticks pass between one block changing and the next, so the change
     * travels as a visible wave; a mode other than zero keeps the change to the one block behind
     * the sign; and physics decides whether the surrounding blocks are told the world changed.
     *
     * <p>The wave follows blocks of either kind, so a structure built from the two of them
     * changes over entirely, and it stops at anything else.
     */
    public static ICLogic blockReplacer() {
        return new BlockReplacer();
    }

    /** The two blocks a swapping chip works between. */
    private record Pair(Key driven, Key idle) {

        /**
         * Reads the pair off the sign.
         *
         * @return the pair, or empty if the line does not name two blocks that exist
         */
        static Optional<Pair> on(ChipState state) {
            String line = state.sign().trimmedText(PAIR_LINE);
            int separator = line.indexOf(PAIR_SEPARATOR);
            if (separator < 0) {
                return Optional.empty();
            }

            Optional<Key> first = state.world().resolveBlock(line.substring(0, separator));
            Optional<Key> second = state.world().resolveBlock(line.substring(separator + 1));
            if (first.isEmpty() || second.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Pair(first.get(), second.get()));
        }

        /** Which of the two the chip should be showing. */
        Key wanted(boolean driven) {
            return driven ? this.driven : idle;
        }

        /** The other one, which is what should be there now if the chip is to swap it. */
        Key other(boolean driven) {
            return driven ? idle : this.driven;
        }

        /** Whether a block is one of the two. */
        boolean contains(Key block) {
            return driven.equals(block) || idle.equals(block);
        }
    }

    /** Swaps a single block, paying for it. */
    private static final class ToggleBlock implements ICLogic {

        /** The line giving the step from the block the sign hangs on. */
        private static final int OFFSET_LINE = 3;

        @Override
        public void trigger(ChipState state) {
            Optional<Pair> pair = Pair.on(state);
            Optional<Vec3i> offset = SignOffset.parse(state.sign().trimmedText(OFFSET_LINE));
            if (pair.isEmpty() || offset.isEmpty()) {
                return;
            }

            ChipWorld world = state.world();
            Vec3i target = state.backPosition().add(offset.get());
            if (!world.isLoaded(target) || !world.isInBounds(target)) {
                return;
            }

            boolean driven = state.isAnyInputActive();
            Key wanted = pair.get().wanted(driven);
            Key expected = pair.get().other(driven);
            Key present = world.blockAt(target);

            if (wanted.equals(present)) {
                return;
            }

            Stockpile stockpile = state.stockpile();
            if (!stockpile.has(wanted, 1)) {
                return;
            }

            // Nothing there yet, so there is nothing to take back and nothing in the way.
            if (world.isAir(target)) {
                if (world.setBlockAt(target, wanted) && !stockpile.isUnlimited()) {
                    stockpile.take(wanted, 1);
                }
                return;
            }

            // Something else has been put in the way. Swapping it out would be a way of taking
            // it, so the chip stops instead.
            if (!expected.equals(present)) {
                return;
            }

            if (!stockpile.isUnlimited() && !stockpile.hasRoomFor(present, 1)) {
                return;
            }
            if (!world.setBlockAt(target, wanted)) {
                return;
            }
            if (!stockpile.isUnlimited()) {
                stockpile.give(present, 1);
                stockpile.take(wanted, 1);
            }
        }
    }

    /** Swaps a block and lets the change spread outward from it. */
    private static final class BlockReplacer implements ICLogic {

        /** The line carrying the delay, the mode and whether physics apply. */
        private static final int SETTINGS_LINE = 3;

        /** How many blocks a single wave may change before it is called off. */
        private static final int MAX_SPREAD = 15_000;

        /** The mode in which the change spreads rather than staying where it started. */
        private static final int SPREADING_MODE = 0;

        @Override
        public void trigger(ChipState state) {
            Optional<Pair> pair = Pair.on(state);
            if (pair.isEmpty()) {
                return;
            }

            Settings settings = Settings.on(state);
            ChipWorld world = state.world();
            Vec3i origin = state.backPosition();
            boolean driven = state.isAnyInputActive();

            if (!world.isLoaded(origin) || !pair.get().contains(world.blockAt(origin))) {
                state.setMainOutput(false);
                return;
            }

            Placement how = Placement.NORMAL.withNotifications(settings.physics());
            swap(world, origin, pair.get(), driven, how);
            state.setMainOutput(true);

            if (settings.mode() == SPREADING_MODE) {
                spread(state, origin, pair.get(), driven, settings, how);
            }
        }

        /** Puts the wanted block down, if the other one is what is there. */
        private static void swap(ChipWorld world, Vec3i position, Pair pair, boolean driven, Placement how) {
            if (pair.other(driven).equals(world.blockAt(position))) {
                world.setBlockAt(position, pair.wanted(driven), how);
            }
        }

        /**
         * Walks outward from the first block, one step of neighbours per delay.
         *
         * <p>Each round changes everything it reached and queues what those blocks touch, so the
         * change travels out as a front rather than jumping about. Blocks already visited are
         * never revisited, which is what makes the walk finish.
         */
        private static void spread(
                ChipState state, Vec3i origin, Pair pair, boolean driven, Settings settings, Placement how) {

            Set<Vec3i> visited = new HashSet<>();
            visited.add(origin);

            Deque<List<Vec3i>> pending = new ArrayDeque<>();
            pending.add(neighboursOf(origin));

            step(state, pair, driven, settings, how, visited, pending);
        }

        private static void step(
                ChipState state,
                Pair pair,
                boolean driven,
                Settings settings,
                Placement how,
                Set<Vec3i> visited,
                Deque<List<Vec3i>> pending) {

            List<Vec3i> front = pending.poll();
            if (front == null || visited.size() >= MAX_SPREAD) {
                return;
            }

            ChipWorld world = state.world();
            List<Vec3i> next = new ArrayList<>();

            for (Vec3i position : front) {
                if (!visited.add(position) || visited.size() > MAX_SPREAD) {
                    continue;
                }
                if (!world.isLoaded(position) || !world.isInBounds(position)) {
                    continue;
                }
                if (!pair.contains(world.blockAt(position))) {
                    continue;
                }

                swap(world, position, pair, driven, how);
                next.addAll(neighboursOf(position));
            }

            if (next.isEmpty()) {
                return;
            }

            pending.add(next);
            Scheduler scheduler = state.scheduler();
            scheduler.runLater(
                    () -> step(state, pair, driven, settings, how, visited, pending),
                    settings.delayTicks());
        }

        /** The six blocks touching a face of this one. */
        private static List<Vec3i> neighboursOf(Vec3i position) {
            List<Vec3i> neighbours = new ArrayList<>(6);
            for (BlockFace face : new BlockFace[] {
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
                BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
            }) {
                neighbours.add(position.offset(face));
            }
            return neighbours;
        }

        /**
         * How the wave behaves.
         *
         * @param delayTicks how long between one ring of blocks changing and the next
         * @param mode zero to let the change spread, anything else to keep it to one block
         * @param physics whether the neighbours are told the world changed
         */
        private record Settings(long delayTicks, int mode, boolean physics) {

            private static final Settings DEFAULT = new Settings(20, 0, true);

            /** Reads the settings line, falling back to the defaults for anything unreadable. */
            static Settings on(ChipState state) {
                String[] parts = state.sign().trimmedText(SETTINGS_LINE).split(":");
                if (parts[0].isBlank()) {
                    return DEFAULT;
                }

                try {
                    long delay = Math.max(0, Long.parseLong(parts[0].trim()));
                    int mode = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                    boolean physics = parts.length <= 2 || isYes(parts[2]);
                    return new Settings(delay, mode, physics);
                } catch (NumberFormatException e) {
                    return DEFAULT;
                }
            }

            private static boolean isYes(String text) {
                String cleaned = text.trim().toLowerCase(Locale.ROOT);
                return cleaned.equals("1") || cleaned.equals("true");
            }
        }
    }
}
