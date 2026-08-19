package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that change the weather and the time of day.
 *
 * <p>These act on the whole world, not just the blocks near the sign, so a single chip affects
 * every player in it. That is the point of them, but it is also why they are worth restricting.
 */
@NullMarked
public final class WeatherChips {

    /** The line a duration is written on. */
    private static final int DURATION_LINE = 2;

    /** How long weather set by a chip lasts when the sign does not say. */
    private static final int DEFAULT_DURATION_TICKS = (int) ChipWorld.TICKS_PER_DAY;

    /** The time of day the world jumps to when asked for morning. */
    private static final long DAWN = 1_000L;

    /** The time of day the world jumps to when asked for night. */
    private static final long DUSK = 13_000L;

    private WeatherChips() {}

    /**
     * Turns the weather on while its input is held.
     *
     * <p>Line 2 gives how long the weather should last once started, defaulting to a full day.
     * The thunderstorm mode makes it start a storm rather than plain rain. The output mirrors
     * the input, so several of these can be chained.
     */
    public static ICLogic simpleWeatherControl() {
        return state -> {
            boolean active = state.isAnyInputActive();
            int duration = (int) configValue(state, DURATION_LINE, DEFAULT_DURATION_TICKS);

            if (active) {
                if (state.mode().behaviour() == ICMode.Behaviour.THUNDER_STORM) {
                    state.world().setThundering(true, duration);
                }
                state.world().setRaining(true, duration);
            } else {
                state.world().setRaining(false, duration);
                state.world().setThundering(false, duration);
            }

            state.setMainOutput(active);
        };
    }

    /**
     * Sets rain and thunder separately, on the rising edge of its clock.
     *
     * <p>Pins: 0 is the clock, 1 asks for rain and 2 asks for thunder. Nothing happens until the
     * clock rises, so the two selectors can be set up before being applied together.
     */
    public static ICLogic weatherControl() {
        return state -> {
            if (!state.isTriggered(0) || !state.input(0)) {
                return;
            }

            int duration = (int) configValue(state, DURATION_LINE, DEFAULT_DURATION_TICKS);
            boolean rain = state.input(1);
            boolean thunder = state.input(2);

            state.world().setRaining(rain, rain ? duration : 0);
            state.world().setThundering(thunder, thunder ? duration : 0);
        };
    }

    /**
     * Jumps the world to the next morning or the next night.
     *
     * <p>Pins: 0 is the clock, and 1 chooses morning when high or night when low. The world only
     * ever moves forwards: asking for a time that has already passed today lands on tomorrow, so
     * a chip can never rewind the day.
     */
    public static ICLogic timeControlAdvanced() {
        return state -> {
            if (!state.isTriggered(0) || !state.input(0)) {
                return;
            }

            long now = state.time().worldTicks();
            long today = now - Math.floorMod(now, ChipWorld.TICKS_PER_DAY);
            long timeOfDay = Math.floorMod(now, ChipWorld.TICKS_PER_DAY);

            long wanted = state.input(1) ? DAWN : DUSK;
            long offset = timeOfDay <= wanted ? wanted : wanted + ChipWorld.TICKS_PER_DAY;

            state.world().setWorldTicks(today + offset);
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
}
