package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.radio.Band;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that carry a redstone signal without a wire between the two ends.
 *
 * <p>A transmitter drives a named band and a receiver anywhere on the server follows it. The pair
 * never touch each other: the transmitter writes the band's state and the receiver reads it on its
 * own tick, so the two ends may be any distance apart, in different worlds, and on threads that
 * know nothing of one another.
 *
 * <p>The band is written across two sign lines. Line 3 is the channel name; line 4 is an optional
 * namespace around it, so two builders can each have a channel called {@code door} without one
 * working the other's. A sign with no channel name is inert rather than joining a shared blank
 * channel.
 */
@NullMarked
public final class Wireless {

    /** The sign line carrying the channel name. */
    private static final int NARROW_BAND_LINE = 2;

    /** The sign line carrying the optional namespace. */
    public static final int WIDE_BAND_LINE = 3;

    /** Separates the analog transmitter's settings from each other. */
    private static final char SETTING_SEPARATOR = ':';

    /** The highest redstone power level, and so the highest band an analog transmitter drives. */
    private static final int FULL_POWER = 15;

    /**
     * The input an analog transmitter ignores when reading its level.
     *
     * <p>The pin below the sign, which is where a builder puts the clock driving the chip. Its own
     * power level is not part of the number being transmitted.
     */
    private static final int CLOCK_INPUT = 2;

    private Wireless() {}

    /**
     * Drives a band from its inputs.
     *
     * <p>The band carries a signal whenever anything is driving the chip, and stops when nothing
     * is. It keeps its last value when the transmitter is unloaded, so a receiver on the far side
     * of the world does not drop out because the transmitter's chunk went out of view.
     */
    public static ICLogic transmitter() {
        return state -> bandOn(state)
                .ifPresent(band -> state.radio().transmit(band, state.isAnyInputActive()));
    }

    /**
     * Follows a band with its output.
     *
     * <p>Ticking, it mirrors the band whatever its inputs are doing. Not ticking, it needs a clock
     * on its inputs and reads the band each time that clock fires.
     *
     * <p>A band nothing has ever transmitted on leaves the output alone rather than driving it
     * low, so a receiver holds what it was last showing until it hears something.
     */
    public static SelfTriggeringICLogic receiver() {
        return new Receiver();
    }

    /**
     * Drives one band per redstone power level.
     *
     * <p>Where a plain transmitter carries one bit, this carries a whole level: the channel name
     * has the level appended to it, so a power of 7 on channel {@code lift} drives {@code lift7}.
     * A row of receivers named {@code lift0} to {@code lift15} then reads the number back.
     *
     * <p>Line 3 carries the settings, separated by colons:
     *
     * <pre>
     *   lift          every level from 0 to 15
     *   lift:3:9      only levels 3 to 9
     *   lift:3:9:T    levels 3 to 9, and every band up to the current level is driven at once
     *   lift:T        every level, all driven at once
     * </pre>
     *
     * <p>With the levels driven one at a time, exactly one band is ever on, and it turns off as
     * the next comes on. Driving them all at once instead gives a thermometer, where a level of 7
     * has bands 0 to 7 on together, which is what a bar of lamps wants.
     */
    public static ICLogic analogTransmitter() {
        return new AnalogTransmitter();
    }

    /**
     * The band a chip's sign names.
     *
     * @return the band, or empty if the sign names no channel
     */
    static Optional<Band> bandOn(ChipState state) {
        return Band.parse(state.sign().trimmedText(WIDE_BAND_LINE), state.sign().trimmedText(NARROW_BAND_LINE));
    }

    /** Follows a band, whether by ticking or by being clocked. */
    private static final class Receiver implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                follow(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            follow(state);
        }

        private static void follow(ChipState state) {
            bandOn(state)
                    .flatMap(band -> state.radio().signal(band))
                    .ifPresent(state::setMainOutput);
        }
    }

    /**
     * The settings an analog transmitter reads off its sign.
     *
     * @param channel the channel name the level is appended to
     * @param lowest the lowest level that drives a band
     * @param highest the highest level that drives a band
     * @param cumulative whether every band up to the current level is driven, rather than one
     */
    record AnalogSettings(String channel, int lowest, int highest, boolean cumulative) {

        /**
         * Reads the settings line.
         *
         * <p>The bounds are only read when both are given. A line naming one number and nothing
         * else, such as {@code lift:3}, leaves the full range in place; that is how the format has
         * always behaved and signs in the world rely on it.
         *
         * @return the settings, or empty if the line names no channel or the bounds make no sense
         */
        static Optional<AnalogSettings> parse(String line) {
            String[] parts = line.trim().split(String.valueOf(SETTING_SEPARATOR), -1);
            String channel = parts[0].trim();
            if (channel.isEmpty() || parts.length > 4) {
                return Optional.empty();
            }

            int lowest = 0;
            int highest = FULL_POWER;
            boolean cumulative = false;

            if (parts.length == 2) {
                cumulative = isYes(parts[1]);
            } else if (parts.length >= 3) {
                Optional<Integer> low = asLevel(parts[1]);
                Optional<Integer> high = asLevel(parts[2]);
                if (low.isEmpty() || high.isEmpty()) {
                    return Optional.empty();
                }
                lowest = low.get();
                highest = high.get();
                cumulative = parts.length == 4 && isYes(parts[3]);
            }

            if (lowest > highest) {
                return Optional.empty();
            }
            return Optional.of(new AnalogSettings(channel, lowest, highest, cumulative));
        }

        /** Whether a level drives one of this transmitter's bands. */
        boolean covers(int level) {
            return level >= lowest && level <= highest;
        }

        private static Optional<Integer> asLevel(String text) {
            try {
                int level = Integer.parseInt(text.trim());
                return level >= 0 && level <= FULL_POWER ? Optional.of(level) : Optional.empty();
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        private static boolean isYes(String text) {
            String cleaned = text.trim().toLowerCase(Locale.ROOT);
            return cleaned.equals("t") || cleaned.equals("true");
        }
    }

    /** Turns a redstone level into a band per level. */
    private static final class AnalogTransmitter implements ICLogic {

        /** The level last transmitted, or -1 while the chip has never seen any power. */
        private int lastLevel = -1;

        @Override
        public boolean requiresAnalogRedstone() {
            return true;
        }

        @Override
        public void load(ChipState state) {
            trigger(state);
        }

        @Override
        public void trigger(ChipState state) {
            Optional<AnalogSettings> settings = AnalogSettings.parse(state.sign().trimmedText(NARROW_BAND_LINE));
            if (settings.isEmpty()) {
                return;
            }

            int level = highestInputLevel(state);

            // Until the chip has seen power for the first time it says nothing, so that loading a
            // world does not announce a level of zero on every band and switch off whatever the
            // last transmitter to speak had left on.
            if (lastLevel == -1 && level == 0) {
                return;
            }
            if (level == lastLevel) {
                return;
            }

            transmit(state, settings.get(), level);
            lastLevel = level;
        }

        private void transmit(ChipState state, AnalogSettings settings, int level) {
            Band base = new Band(state.sign().trimmedText(WIDE_BAND_LINE), settings.channel());

            if (settings.cumulative()) {
                for (int band = settings.lowest(); band <= settings.highest(); band++) {
                    state.radio().transmit(base.withSuffix(Integer.toString(band)), band <= level);
                }
                return;
            }

            if (settings.covers(lastLevel)) {
                state.radio().transmit(base.withSuffix(Integer.toString(lastLevel)), false);
            }
            if (settings.covers(level)) {
                state.radio().transmit(base.withSuffix(Integer.toString(level)), true);
            }
        }

        /** The strongest level on any input other than the clock. */
        private static int highestInputLevel(ChipState state) {
            int highest = 0;
            for (int input = 0; input < state.inputCount(); input++) {
                if (input == CLOCK_INPUT) {
                    continue;
                }
                highest = Math.max(highest, state.inputPower(input));
            }
            return highest;
        }
    }
}
