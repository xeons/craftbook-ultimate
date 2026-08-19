package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.platform.TimeSource;
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
        return new PulseLogic();
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
        String text = state.sign().trimmedText(line);
        if (text.isEmpty()) {
            return fallback;
        }

        String[] parts = text.split(":");
        long value;
        try {
            value = Long.parseLong(parts[0].trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
        if (value < 0) {
            return fallback;
        }
        if (parts.length < 2) {
            return value;
        }

        return switch (parts[1].trim().toUpperCase(java.util.Locale.ROOT)) {
            case "T", "TICKS" -> value * MILLIS_PER_TICK;
            case "S", "SECONDS" -> value * 1000L;
            case "MS", "MILLISECONDS" -> value;
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

    /** Drives its output high and low a fixed number of times when poked. */
    private static final class PulseLogic implements ICLogic {

        private static final long MINIMUM_PULSE_MILLIS = 100L;
        private static final long MAXIMUM_PULSE_MILLIS = 1000L;
        private static final int MINIMUM_PULSES = 1;
        private static final int MAXIMUM_PULSES = 10;

        private Scheduler.@Nullable Task burst;

        @Override
        public void trigger(ChipState state) {
            if (!state.isAnyInputActive()) {
                return;
            }

            cancel();

            long periodTicks = Math.max(1, millisToTicks(pulseMillis(state)));
            int pulses = pulseCount(state);
            Burst body = new Burst(state, pulses);
            burst = state.scheduler().runRepeating(body, periodTicks, periodTicks);
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
            long configured = configValue(state, FIRST_CONFIG_LINE, MINIMUM_PULSE_MILLIS);
            return Math.max(MINIMUM_PULSE_MILLIS, Math.min(MAXIMUM_PULSE_MILLIS, configured));
        }

        private static int pulseCount(ChipState state) {
            long configured = configValue(state, SECOND_CONFIG_LINE, MINIMUM_PULSES);
            return (int) Math.max(MINIMUM_PULSES, Math.min(MAXIMUM_PULSES, configured));
        }

        /** Alternates the output, counting a completed pulse each time it falls. */
        private static final class Burst implements Runnable {

            private final ChipState state;
            private final int pulses;

            private Scheduler.@Nullable Task task;
            private boolean high;
            private int completed;

            Burst(ChipState state, int pulses) {
                this.state = state;
                this.pulses = pulses;
            }

            @Override
            public void run() {
                high = !high;
                state.setMainOutput(high);

                if (!high) {
                    completed++;
                    if (completed >= pulses && task != null) {
                        task.cancel();
                    }
                }
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
