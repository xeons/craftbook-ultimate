package com.xeonproductions.craftbookultimate.core.platform;

import org.jspecify.annotations.NullMarked;

/**
 * Where a chip reads the time from.
 *
 * <p>Three clocks matter and they are not interchangeable. World time drives anything that cares
 * about day and night. Wall clock time drives the few chips that key off real time regardless of
 * what the world is doing. Tick counting is the scheduler's business, not this interface's.
 *
 * <p>Going through here rather than reading a world or calling the system clock is what lets a
 * daylight sensor be tested at midnight without waiting.
 */
@NullMarked
public interface TimeSource {

    /** The number of ticks a world has existed for, which keeps counting past a day. */
    long worldTicks();

    /** Seconds since the unix epoch, from the wall clock. */
    long unixSeconds();

    /** The number of ticks a world has been in the current day, from 0 to 23999. */
    default long timeOfDay() {
        return Math.floorMod(worldTicks(), TICKS_PER_DAY);
    }

    /** The length of a Minecraft day in ticks. */
    long TICKS_PER_DAY = 24_000L;

    /**
     * A time source fixed at one moment, for tests and for anything that wants a stable reading.
     *
     * @param worldTicks the world age to report
     * @param unixSeconds the wall clock reading to report
     */
    static TimeSource fixed(long worldTicks, long unixSeconds) {
        return new TimeSource() {
            @Override
            public long worldTicks() {
                return worldTicks;
            }

            @Override
            public long unixSeconds() {
                return unixSeconds;
            }
        };
    }
}
