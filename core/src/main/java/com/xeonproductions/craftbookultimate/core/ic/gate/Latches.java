// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import org.jspecify.annotations.NullMarked;

/**
 * The sequential chips: latches and flip-flops, whose output depends on what happened before.
 *
 * <p>These read their own current output to decide the next one, so they hold state in the world
 * rather than in the logic object. That keeps them correct across a server restart, since the
 * lever they drive is the memory.
 *
 * <p>Each of these leaves the output untouched in its hold condition. Writing nothing is what
 * makes a latch latch.
 */
@NullMarked
public final class Latches {

    /*
     * Where a latch keeps what it is holding: in its own output lever, read back through
     * ChipState#mainOutput. The lever is a block, so the state survives a chunk unloading and the
     * server stopping with nothing written down anywhere, and a builder can set a latch by hand by
     * flipping it. Nothing is kept on the sign; the counters below are the only chips here that
     * write one, and they write their running total. See finding 151.
     */

    private Latches() {}

    /**
     * An RS latch built from NOR gates.
     *
     * <p>Reset wins: while either reset input is powered the output is held low, and the set
     * input is ignored.
     *
     * <p>Pins: 0 set, 1 and 2 reset, output 0 is Q.
     */
    public static ICLogic rsNorLatch() {
        return state -> {
            if (state.input(1) || state.input(2)) {
                state.setMainOutput(false);
            } else if (state.input(0)) {
                state.setMainOutput(true);
            }
        };
    }

    /**
     * An RS latch built from NAND gates, driven by active-low inputs.
     *
     * <p>Set wins: powering input 0 raises the output whatever input 1 is doing, powering input 1
     * alone lowers it, and leaving both unpowered holds the current value.
     *
     * <p>Pins: 0 set, 1 reset, output 0 is Q.
     */
    public static ICLogic rsNandLatch() {
        return state -> {
            if (state.input(0)) {
                state.setMainOutput(true);
            } else if (state.input(1)) {
                state.setMainOutput(false);
            }
        };
    }

    /**
     * A JK flip-flop, clocked on the falling edge of input 0.
     *
     * <p>With both J and K powered the output toggles; with only J it sets, with only K it
     * resets, and with neither it holds.
     *
     * <p>Pins: 0 clock, 1 J, 2 K, output 0 is Q.
     */
    public static ICLogic jkFlipFlop() {
        return state -> {
            if (!state.isTriggered(0) || state.input(0)) {
                return;
            }

            boolean j = state.input(1);
            boolean k = state.input(2);
            if (j && k) {
                state.setMainOutput(!state.mainOutput());
            } else if (j) {
                state.setMainOutput(true);
            } else if (k) {
                state.setMainOutput(false);
            }
        };
    }

    /**
     * A D flip-flop that samples on the rising edge of its clock.
     *
     * <p>The reset input overrides everything. Otherwise the data input is copied to the output
     * only on the run where the clock itself changed, so a held clock does not keep sampling.
     *
     * <p>Pins: 0 data, 1 clock, 2 reset, output 0 is Q.
     */
    public static ICLogic edgeTriggeredDFlipFlop() {
        return state -> {
            if (state.input(2)) {
                state.setMainOutput(false);
            } else if (state.input(1) && state.isTriggered(1)) {
                state.setMainOutput(state.input(0));
            }
        };
    }

    /**
     * A D flip-flop that follows its data input for as long as the clock is held high.
     *
     * <p>The reset input is applied after the sample, so resetting while the clock is high still
     * drives the output low.
     *
     * <p>Pins: 0 clock, 1 data, 2 reset, output 0 is Q.
     */
    public static ICLogic levelTriggeredDFlipFlop() {
        return state -> {
            if (state.input(0)) {
                state.setMainOutput(state.input(1));
            }
            if (state.input(2)) {
                state.setMainOutput(false);
            }
        };
    }

    /**
     * A toggle flip-flop, which inverts its output each time it is driven.
     *
     * @param onRisingEdge true to toggle as the chip becomes active, false to toggle as it
     *     becomes inactive
     */
    public static ICLogic toggleFlipFlop(boolean onRisingEdge) {
        return state -> {
            if (state.isAnyInputActive() == onRisingEdge) {
                state.setMainOutput(!state.mainOutput());
            }
        };
    }

    /**
     * A three-bit combination lock.
     *
     * <p>The combination is written on line 2 of the sign as three characters, where {@code X}
     * means that input must be powered and anything else means it must not be. The characters
     * are read in the order middle, right, left, matching the order they appear on the sign when
     * facing it.
     *
     * <p>Pins: 0, 1 and 2 are the combination inputs, output 0 opens.
     */
    public static ICLogic combinationLock() {
        return state -> {
            String combination = state.sign().text(2);
            if (combination.length() < 3) {
                state.setMainOutput(false);
                return;
            }

            boolean open = state.input(0) == (combination.charAt(1) == 'X')
                    && state.input(1) == (combination.charAt(2) == 'X')
                    && state.input(2) == (combination.charAt(0) == 'X');

            state.setMainOutput(open);
        };
    }

    /**
     * A counter that counts up to a limit and reports when it arrives.
     *
     * <p>The running total lives on line 3 of the sign, where a player can read it. Pulsing
     * input 0 advances the count; pulsing input 1 returns it to zero. The output is high only on
     * the run where the count reaches the limit.
     *
     * @param limit the value the counter counts up to
     * @param repeating true to wrap back to zero and keep counting, false to stop at the limit
     */
    public static ICLogic counter(int limit, boolean repeating) {
        return new Counter(limit, repeating, true);
    }

    /**
     * A counter that counts down to zero and reports when it arrives.
     *
     * <p>Behaves as {@link #counter(int, boolean)} in reverse: it starts at the limit, input 0
     * decrements, and input 1 returns it to the limit.
     *
     * @param limit the value the counter starts from and resets to
     * @param repeating true to wrap back to the limit and keep counting, false to stop at zero
     */
    public static ICLogic downCounter(int limit, boolean repeating) {
        return new Counter(limit, repeating, false);
    }

    /**
     * A counter that takes its limit from the sign.
     *
     * <p>Line 2 reads either a limit on its own, or a limit followed by {@code :INF} to keep
     * counting past it. A line that does not start with a number falls back to a limit of five.
     *
     * @param countingUp true for an up counter, false for a down counter
     */
    public static ICLogic counterFromSign(boolean countingUp) {
        return state -> {
            CounterConfig config = CounterConfig.parse(state.sign().trimmedText(2));
            new Counter(config.limit(), config.repeating(), countingUp).trigger(state);
        };
    }

    /**
     * The limit and wrapping behaviour a counter reads off its sign.
     *
     * @param limit the value counted to
     * @param repeating whether to wrap and keep counting
     */
    private record CounterConfig(int limit, boolean repeating) {

        /** Used when the sign does not name a usable limit. */
        private static final int DEFAULT_LIMIT = 5;

        /** The marker that asks a counter to wrap rather than stop. */
        private static final String INFINITE = "INF";

        static CounterConfig parse(String line) {
            String[] parts = line.split(":");

            int limit;
            try {
                limit = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                return new CounterConfig(DEFAULT_LIMIT, false);
            }

            boolean repeating = parts.length > 1 && parts[1].trim().equalsIgnoreCase(INFINITE);
            return new CounterConfig(limit, repeating);
        }
    }

    /** The shared body of the up and down counters, which differ only in direction. */
    private record Counter(int limit, boolean repeating, boolean countingUp) implements ICLogic {

        /** The sign line the running total is kept on. */
        private static final int STATE_LINE = 3;

        @Override
        public void trigger(ChipState state) {
            int current = readCount(state);
            int updated = current;

            int target = countingUp ? limit : 0;
            int start = countingUp ? 0 : limit;

            if (state.input(0)) {
                if (current == target) {
                    if (repeating) {
                        updated = start;
                    }
                } else {
                    updated = countingUp ? current + 1 : current - 1;
                }
                state.setMainOutput(updated == target);
            } else if (state.input(1)) {
                updated = start;
                state.setMainOutput(false);
            }

            if (updated != current) {
                state.setSignLine(STATE_LINE, String.valueOf(updated));
            }
        }

        /** Reads the running total. A line that is not a number counts as zero. */
        private static int readCount(ChipState state) {
            try {
                return Integer.parseInt(state.sign().trimmedText(STATE_LINE));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
