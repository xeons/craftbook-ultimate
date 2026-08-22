// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.platform.TimeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The chips whose behaviour depends on time.
 *
 * <p>Two different senses of time appear here, and they are not interchangeable. The sensors read
 * a clock and answer a question about now: whether it is day, whether the time is between two
 * values. The others measure a duration, and reach the scheduler to act once it has elapsed.
 *
 * <p>Durations written on signs are in milliseconds, which is how they have always been written,
 * but a server only acts on tick boundaries. A duration is therefore rounded up to whole ticks,
 * so a delay never comes out shorter than asked for.
 */
@NullMarked
public final class TimeChips {

    /** How long one server tick lasts, in milliseconds. */
    private static final long MILLIS_PER_TICK = 50L;

    /** The line a chip's first configuration value is written on. */
    private static final int FIRST_CONFIG_LINE = 2;

    /** The line a chip's second configuration value is written on. */
    private static final int SECOND_CONFIG_LINE = 3;

    private TimeChips() {}

    /**
     * Converts a duration in milliseconds to whole ticks, rounding up.
     *
     * <p>Rounding up rather than down means a delay is never shorter than the sign asked for, and
     * that any non-zero duration lasts at least one tick rather than vanishing.
     */
    public static long millisToTicks(long millis) {
        if (millis <= 0) {
            return 0;
        }
        return (millis + MILLIS_PER_TICK - 1) / MILLIS_PER_TICK;
    }

    /**
     * A clock that toggles its output every so many ticks.
     *
     * <p>Line 2 sets the period in ticks, clamped between 3 and 1000, defaulting to 20. The input
     * disables the ticking: while it is held the chip advances only when redstone pokes it, and
     * while it is released the chip advances on its own.
     */
    public static SelfTriggeringICLogic clock() {
        return new ClockLogic();
    }

    /**
     * Outputs high while the world time is within a window.
     *
     * <p>Line 2 sets the start of the window and line 3 the end, in ticks through the day,
     * defaulting to 0 and 13000. A window whose start is after its end wraps around midnight.
     */
    public static SelfTriggeringICLogic daySensor() {
        return new DaySensorLogic();
    }

    /**
     * Outputs high while the world time falls between two values.
     *
     * <p>Line 2 sets the start and line 3 the end, defaulting to the whole day. Unlike the
     * daylight sensor this window does not wrap: a start after an end simply never matches.
     */
    public static ICLogic betweenTime() {
        return state -> {
            long timeOfDay = state.time().timeOfDay();
            long start = configValue(state, FIRST_CONFIG_LINE, 0);
            long end = configValue(state, SECOND_CONFIG_LINE, TimeSource.TICKS_PER_DAY);

            state.setMainOutput(timeOfDay >= start && timeOfDay <= end);
        };
    }

    /**
     * Outputs high when the world time, divided by one value, leaves a remainder of at least
     * another.
     *
     * <p>Line 2 sets the divisor, defaulting to 2, and line 3 the threshold, defaulting to 0. The
     * defaults make the output follow whether the world time is odd.
     */
    public static ICLogic worldTimeModulus() {
        return modulus(state -> state.time().worldTicks());
    }

    /**
     * The same as {@link #worldTimeModulus()} but reading the wall clock in seconds, so it keeps
     * running at the same rate whatever the world is doing.
     */
    public static ICLogic unixTimeModulus() {
        return modulus(state -> state.time().unixSeconds());
    }

    /**
     * Sends a burst of pulses when triggered.
     *
     * <p>Line 2 sets how long each pulse lasts in milliseconds, from 100 to 1000 and defaulting
     * to 100. Line 3 sets how many pulses to send, from 1 to 10 and defaulting to 1. Triggering
     * again while a burst is running restarts it.
     */
    public static ICLogic pulse() {
        return new PulseLogic(false, false);
    }

    /**
     * Holds its output high for a while after its input goes low.
     *
     * <p>Line 2 sets how long to hold, as a number of milliseconds optionally followed by a unit:
     * {@code T} or {@code TICKS}, {@code S} or {@code SECONDS}, {@code MS} or
     * {@code MILLISECONDS}. The output rises immediately when the input does.
     */
    public static ICLogic signalExtender() {
        return new SignalExtenderLogic();
    }

    /**
     * A repeater that waits before passing its input on.
     *
     * <p>Line 2 sets the delay in the same form as {@link #signalExtender()}. With no delay the
     * chip behaves exactly as the plain repeater. A change arriving while a delay is pending
     * replaces it, so the output always settles on the most recent input.
     */
    public static ICLogic delayedRepeater() {
        return new DelayedBuffer(false);
    }

    /** An inverter that waits before passing its input on, as {@link #delayedRepeater()} does. */
    public static ICLogic delayedInverter() {
        return new DelayedBuffer(true);
    }

    /**
     * Waits before turning on, and turns off the moment its input does.
     *
     * <p>One edge waits and the other passes straight through, which is what separates these from
     * {@link #delayedRepeater()}: that one delays both edges by the same amount, so it shifts a
     * signal without changing its shape. This one changes the shape — a flicker shorter than the
     * delay never reaches the output at all, which is what makes it a debounce.
     *
     * <p>Line 3 is how long to wait. Line 4 may read {@code hold}, which lets a wait already
     * running finish even if the input drops before it does.
     */
    public static ICLogic onDelay() {
        return new EdgeDelay(true, false);
    }

    /** {@link #onDelay()} with its answer turned over. */
    public static ICLogic invertedOnDelay() {
        return new EdgeDelay(true, true);
    }

    /**
     * Turns off with its input, and waits before turning back on.
     *
     * <p>Its non-inverted partner is not here: turning on at once and waiting before turning off
     * is exactly {@link #signalExtender()}, so that chip answers to both numbers rather than the
     * same behaviour being written twice.
     *
     * <p>Unlike the extender, a blank line 3 means no wait at all, which is what a blank delay
     * means on every other chip in this file. The extender keeps its own default of a second,
     * because that is what its signs have always done.
     */
    public static ICLogic invertedOffDelay() {
        return new EdgeDelay(false, true);
    }

    /**
     * A burst of pulses, fired when the input rises or when it falls, either way up.
     *
     * <p>Four chips from one, which is how upstream builds them too: what differs between them is
     * only which edge sets them off and which way round the output rests.
     *
     * @param firesOnLow whether the burst starts as the input drops rather than as it rises
     * @param inverting whether the output rests high and the pulses go low
     */
    public static ICLogic pulse(boolean firesOnLow, boolean inverting) {
        return new PulseLogic(firesOnLow, inverting);
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

    /**
     * Reads a duration written as a number with an optional unit.
     *
     * @return the duration in milliseconds, or the fallback when the line is missing or unreadable
     */
    private static long durationMillis(ChipState state, int line, long fallback) {
        return durationOf(fieldOn(state, line, 0), fallback);
    }

    /**
     * Reads one colon-separated field of a sign line.
     *
     * <p>A line may carry two things — how long a pulse lasts and how long to wait before the
     * first, say — and they are separated by a colon, which is how upstream writes the same pair.
     *
     * @param index which field, counting from zero
     */
    private static String fieldOn(ChipState state, int line, int index) {
        String[] fields = fieldsOf(state.sign().trimmedText(line));
        return index < fields.length ? fields[index] : "";
    }

    /**
     * Splits a line into its fields, keeping a unit that was written after a colon with its number.
     *
     * <p>A duration is written {@code 20T}, and that is what every page describing these chips
     * says. It was only ever read as {@code 20:T}, so the two disagreed and the documented
     * spelling did nothing — see finding 148. Both work now, which is what lets the colon mean a
     * second field without any sign that used the old spelling changing meaning: a colon followed
     * by a unit word belongs to the number in front of it, and a colon followed by anything else
     * starts a new field.
     */
    private static String[] fieldsOf(String text) {
        String[] parts = text.split(":", -1);
        List<String> fields = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (isUnit(trimmed) && !fields.isEmpty()) {
                fields.set(fields.size() - 1, fields.getLast() + trimmed);
            } else {
                fields.add(trimmed);
            }
        }
        return fields.toArray(new String[0]);
    }

    /** Whether a word is one of the units a duration may carry. */
    private static boolean isUnit(String written) {
        return switch (written.toUpperCase(Locale.ROOT)) {
            case "T", "TICKS", "S", "SECONDS", "MS", "MILLISECONDS" -> true;
            default -> false;
        };
    }

    /**
     * Reads a duration written as a number with an optional unit.
     *
     * <p>A bare number is milliseconds, which is what these chips have always taken. {@code T} or
     * {@code TICKS} after it makes it ticks, {@code S} or {@code SECONDS} seconds.
     *
     * @return the duration in milliseconds, or the fallback when the text is not one
     */
    private static long durationOf(String written, long fallback) {
        String text = written.trim();
        if (text.isEmpty()) {
            return fallback;
        }

        int digits = 0;
        while (digits < text.length() && Character.isDigit(text.charAt(digits))) {
            digits++;
        }
        if (digits == 0) {
            return fallback;
        }

        long value;
        try {
            value = Long.parseLong(text.substring(0, digits));
        } catch (NumberFormatException e) {
            return fallback;
        }

        return switch (text.substring(digits).trim().toUpperCase(Locale.ROOT)) {
            case "", "MS", "MILLISECONDS" -> value;
            case "T", "TICKS" -> value * MILLIS_PER_TICK;
            case "S", "SECONDS" -> value * 1000L;
            default -> fallback;
        };
    }

    /** Builds a chip comparing a clock reading against a divisor and a threshold. */
    private static ICLogic modulus(java.util.function.ToLongFunction<ChipState> reading) {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            long divisor = configValue(state, FIRST_CONFIG_LINE, 2);
            if (divisor <= 0) {
                return;
            }
            long threshold = configValue(state, SECOND_CONFIG_LINE, 0);

            state.setMainOutput(Math.floorMod(reading.applyAsLong(state), divisor) >= threshold);
        };
    }

    /** Counts ticks and toggles its output each time it has counted enough of them. */
    private static final class ClockLogic implements SelfTriggeringICLogic {

        private static final int MINIMUM_PERIOD = 3;
        private static final int MAXIMUM_PERIOD = 1000;
        private static final int DEFAULT_PERIOD = 20;

        private int ticks;
        private boolean inputActive;

        @Override
        public void trigger(ChipState state) {
            inputActive = state.isAnyInputActive();
            if (inputActive) {
                advance(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            if (!inputActive) {
                advance(state);
            }
        }

        @Override
        public boolean alwaysSelfTriggering() {
            return true;
        }

        private void advance(ChipState state) {
            ticks++;
            if (ticks >= period(state)) {
                ticks = 0;
                state.setMainOutput(!state.mainOutput());
            }
        }

        private int period(ChipState state) {
            long configured = configValue(state, FIRST_CONFIG_LINE, DEFAULT_PERIOD);
            return (int) Math.max(MINIMUM_PERIOD, Math.min(MAXIMUM_PERIOD, configured));
        }
    }

    /** Outputs high while the world time is inside a window that may wrap around midnight. */
    private static final class DaySensorLogic implements SelfTriggeringICLogic {

        private static final long DEFAULT_DAWN = 0L;
        private static final long DEFAULT_DUSK = 13_000L;

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(isDaytime(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            state.setMainOutput(isDaytime(state));
        }

        @Override
        public boolean alwaysSelfTriggering() {
            return true;
        }

        private static boolean isDaytime(ChipState state) {
            long time = state.time().timeOfDay();
            long dawn = configValue(state, FIRST_CONFIG_LINE, DEFAULT_DAWN);
            long dusk = configValue(state, SECOND_CONFIG_LINE, DEFAULT_DUSK);

            if (dawn < dusk) {
                return time >= dawn && time <= dusk;
            }
            if (dawn > dusk) {
                // The window runs through midnight, so it is everything from dawn to the end of
                // the day plus everything from the start of the day to dusk.
                return time >= dawn || time <= dusk;
            }
            return time < dusk;
        }
    }

    /**
     * A burst of pulses, on one edge or the other, either way up.
     *
     * <p>Line 3 is {@code length[:startDelay]} and line 4 is {@code count[:pause]}. The two
     * second fields are additions: a sign that gives neither behaves exactly as it always did,
     * pulsing at once and running its pulses back to back.
     */
    private static final class PulseLogic implements ICLogic {

        private static final long MINIMUM_PULSE_MILLIS = 100L;
        private static final long MAXIMUM_PULSE_MILLIS = 1000L;
        private static final int MINIMUM_PULSES = 1;
        private static final int MAXIMUM_PULSES = 10;

        /** The longest a burst may be made to wait before it starts, or between its pulses. */
        private static final long MAXIMUM_GAP_MILLIS = 60_000L;

        private final boolean firesOnLow;
        private final boolean inverting;

        private Scheduler.@Nullable Task burst;

        PulseLogic(boolean firesOnLow, boolean inverting) {
            this.firesOnLow = firesOnLow;
            this.inverting = inverting;
        }

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive() == firesOnLow) {
                return;
            }

            cancel();

            long periodTicks = Math.max(1, millisToTicks(pulseMillis(state)));
            long startTicks = Math.max(1, millisToTicks(startDelayMillis(state)));
            long pauseTicks = Math.max(0, millisToTicks(pauseMillis(state)));

            Burst body = new Burst(state, pulseCount(state), periodTicks, pauseTicks, inverting);
            burst = state.scheduler().runRepeating(body, startTicks, 1);
            body.task = burst;
        }

        @Override
        public void unload(ChipState state) {
            cancel();
        }

        private void cancel() {
            if (burst != null) {
                burst.cancel();
                burst = null;
            }
        }

        private static long pulseMillis(ChipState state) {
            long configured = durationOf(
                    fieldOn(state, FIRST_CONFIG_LINE, 0), MINIMUM_PULSE_MILLIS);
            return Math.clamp(configured, MINIMUM_PULSE_MILLIS, MAXIMUM_PULSE_MILLIS);
        }

        /** How long to wait before the first pulse, which a sign need not give at all. */
        private static long startDelayMillis(ChipState state) {
            return Math.clamp(
                    durationOf(fieldOn(state, FIRST_CONFIG_LINE, 1), 0), 0, MAXIMUM_GAP_MILLIS);
        }

        private static int pulseCount(ChipState state) {
            long configured = durationOf(fieldOn(state, SECOND_CONFIG_LINE, 0), MINIMUM_PULSES);
            return Math.clamp(configured, MINIMUM_PULSES, MAXIMUM_PULSES);
        }

        /** How long the output rests between one pulse and the next. */
        private static long pauseMillis(ChipState state) {
            return Math.clamp(
                    durationOf(fieldOn(state, SECOND_CONFIG_LINE, 1), 0), 0, MAXIMUM_GAP_MILLIS);
        }

        /**
         * Runs the burst a tick at a time, holding the output up and then down for as long as each
         * half is meant to last.
         *
         * <p>A tick at a time rather than one task per edge, so the whole burst is one thing to
         * cancel when the chip goes away.
         */
        private static final class Burst implements Runnable {

            private final ChipState state;
            private final int pulses;
            private final long highTicks;
            private final long lowTicks;
            private final boolean inverting;

            private Scheduler.@Nullable Task task;
            private boolean high;
            private long remaining;
            private int completed;

            Burst(ChipState state, int pulses, long highTicks, long lowTicks, boolean inverting) {
                this.state = state;
                this.pulses = pulses;
                this.highTicks = highTicks;
                this.lowTicks = lowTicks;
                this.inverting = inverting;
            }

            @Override
            public void run() {
                if (remaining > 0) {
                    remaining--;
                    return;
                }

                high = !high;
                state.setMainOutput(inverting != high);

                if (high) {
                    remaining = highTicks - 1;
                    return;
                }

                completed++;
                if (completed >= pulses && task != null) {
                    task.cancel();
                    return;
                }
                remaining = lowTicks;
            }
        }
    }

    /**
     * Delays one edge and lets the other through at once.
     *
     * @param delaysRising whether it is the input going high that waits
     * @param inverting whether the output is the opposite of the input
     */
    private static final class EdgeDelay implements ICLogic {

        /** What line 4 reads to let a wait finish after the input has gone. */
        private static final String HOLD = "hold";

        private final boolean delaysRising;
        private final boolean inverting;

        private Scheduler.@Nullable Task pending;

        EdgeDelay(boolean delaysRising, boolean inverting) {
            this.delaysRising = delaysRising;
            this.inverting = inverting;
        }

        @Override
        public void trigger(ChipState state) {
            boolean active = state.isAnyInputActive();
            boolean waiting = active == delaysRising;

            if (!waiting && holds(state) && pending != null) {
                // Told to let the wait finish, so the edge that would normally cut it short is
                // ignored and only the pending write is left to speak.
                return;
            }

            cancel();
            if (!waiting) {
                write(state);
                return;
            }

            long delay = millisToTicks(durationMillis(state, FIRST_CONFIG_LINE, 0));
            if (delay <= 0) {
                write(state);
                return;
            }
            pending = state.scheduler().runLater(() -> write(state), delay);
        }

        @Override
        public void unload(ChipState state) {
            cancel();
        }

        /** Reads the input afresh, so a signal that came back leaves no edge behind it. */
        private void write(ChipState state) {
            state.setMainOutput(inverting != state.isAnyInputActive());
        }

        private static boolean holds(ChipState state) {
            return state.sign().trimmedText(SECOND_CONFIG_LINE).equalsIgnoreCase(HOLD);
        }

        private void cancel() {
            if (pending != null) {
                pending.cancel();
                pending = null;
            }
        }
    }

    /** Keeps its output high for a while after its input stops. */
    private static final class SignalExtenderLogic implements ICLogic {

        private static final long DEFAULT_EXTENSION_MILLIS = 1000L;

        private Scheduler.@Nullable Task pending;
        private boolean inputActive;

        @Override
        public void trigger(ChipState state) {
            boolean active = state.isAnyInputActive();

            if (active && !inputActive) {
                cancel();
                state.setMainOutput(true);
            } else if (!active && inputActive) {
                cancel();
                long delay = Math.max(1, millisToTicks(
                        durationMillis(state, FIRST_CONFIG_LINE, DEFAULT_EXTENSION_MILLIS)));
                pending = state.scheduler().runLater(() -> state.setMainOutput(false), delay);
            }

            inputActive = active;
        }

        @Override
        public void unload(ChipState state) {
            cancel();
        }

        private void cancel() {
            if (pending != null) {
                pending.cancel();
                pending = null;
            }
        }
    }

    /** A repeater or inverter that may wait before passing its input on. */
    private static final class DelayedBuffer implements ICLogic {

        private final boolean inverting;

        private Scheduler.@Nullable Task pending;

        DelayedBuffer(boolean inverting) {
            this.inverting = inverting;
        }

        @Override
        public void trigger(ChipState state) {
            cancel();

            long delay = millisToTicks(durationMillis(state, FIRST_CONFIG_LINE, 0));
            if (delay <= 0) {
                write(state);
                return;
            }

            pending = state.scheduler().runLater(() -> write(state), delay);
        }

        @Override
        public void unload(ChipState state) {
            cancel();
        }

        /** Reads the input afresh, so a delayed chip settles on the input as it is when it acts. */
        private void write(ChipState state) {
            state.setMainOutput(inverting != state.isAnyInputActive());
        }

        private void cancel() {
            if (pending != null) {
                pending.cancel();
                pending = null;
            }
        }
    }
}
