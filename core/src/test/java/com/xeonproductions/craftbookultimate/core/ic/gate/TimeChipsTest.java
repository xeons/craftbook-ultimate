package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Time-based chips")
class TimeChipsTest {

    private static SimpleChipState.Builder chip(String config, String secondConfig) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .sign("", "[MC1420]", config, secondConfig);
    }

    @Nested
    @DisplayName("duration rounding")
    class DurationRounding {

        @ParameterizedTest(name = "{0}ms is {1} ticks")
        @CsvSource({"0, 0", "1, 1", "50, 1", "51, 2", "100, 2", "1000, 20"})
        void roundsUpToWholeTicks(long millis, long ticks) {
            assertThat(TimeChips.millisToTicks(millis)).isEqualTo(ticks);
        }

        @Test
        void neverLosesASubTickDuration() {
            // Rounding down would make a short delay vanish entirely.
            assertThat(TimeChips.millisToTicks(1)).isPositive();
        }

        @Test
        void treatsANegativeDurationAsNone() {
            assertThat(TimeChips.millisToTicks(-100)).isZero();
        }
    }

    @Nested
    @DisplayName("clock")
    class Clock {

        @Test
        void togglesAfterItsConfiguredPeriod() {
            SelfTriggeringICLogic clock = TimeChips.clock();
            SimpleChipState state = chip("3", "").build();

            clock.tick(state);
            clock.tick(state);
            assertThat(state.mainOutput()).isFalse();

            clock.tick(state);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void keepsToggling() {
            SelfTriggeringICLogic clock = TimeChips.clock();
            SimpleChipState state = chip("3", "").build();

            for (int i = 0; i < 3; i++) {
                clock.tick(state);
            }
            assertThat(state.mainOutput()).isTrue();

            for (int i = 0; i < 3; i++) {
                clock.tick(state);
            }
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void stopsTickingWhileItsInputIsHeld() {
            SelfTriggeringICLogic clock = TimeChips.clock();
            SimpleChipState state = chip("3", "").build().withInput(0, true);

            clock.trigger(state);

            for (int i = 0; i < 10; i++) {
                clock.tick(state);
            }

            assertThat(state.mainOutput()).isFalse();
        }

        @ParameterizedTest(name = "\"{0}\" gives a period of {1}")
        @CsvSource({"'', 20", "1, 3", "5000, 1000", "banana, 20"})
        void clampsItsPeriodToAUsableRange(String config, int expectedPeriod) {
            SelfTriggeringICLogic clock = TimeChips.clock();
            SimpleChipState state = chip(config, "").build();

            for (int i = 0; i < expectedPeriod - 1; i++) {
                clock.tick(state);
            }
            assertThat(state.mainOutput()).isFalse();

            clock.tick(state);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void ticksWhetherOrNotTheSignAsksItTo() {
            assertThat(TimeChips.clock().alwaysSelfTriggering()).isTrue();
        }
    }

    @Nested
    @DisplayName("daylight sensor")
    class DaylightSensor {

        private boolean isDayAt(long worldTicks, String dawn, String dusk) {
            SelfTriggeringICLogic sensor = TimeChips.daySensor();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MC1230]", dawn, dusk)
                    .worldTicks(worldTicks)
                    .build();

            sensor.tick(state);
            return state.mainOutput();
        }

        @ParameterizedTest(name = "tick {0} is day: {1}")
        @CsvSource({"0, true", "6000, true", "13000, true", "13001, false", "18000, false", "23999, false"})
        void followsTheDefaultDayWindow(long worldTicks, boolean expected) {
            assertThat(isDayAt(worldTicks, "", "")).isEqualTo(expected);
        }

        @Test
        void readsItsWindowFromTheSign() {
            assertThat(isDayAt(5000, "4000", "6000")).isTrue();
            assertThat(isDayAt(7000, "4000", "6000")).isFalse();
        }

        @Test
        void treatsAWindowInOrderAsTheTimesBetweenThem() {
            assertThat(isDayAt(12000, "6000", "18000")).isTrue();
            assertThat(isDayAt(23000, "6000", "18000")).isFalse();
            assertThat(isDayAt(1000, "6000", "18000")).isFalse();
        }

        @Test
        void wrapsAWindowThatStartsAfterItEnds() {
            // Dawn after dusk describes a window running through midnight.
            assertThat(isDayAt(23000, "18000", "6000")).isTrue();
            assertThat(isDayAt(1000, "18000", "6000")).isTrue();
            assertThat(isDayAt(12000, "18000", "6000")).isFalse();
        }

        @Test
        void countsPastMidnightOnTheWorldClock() {
            // A world several days old still reports the right time of day.
            assertThat(isDayAt(24_000 * 5 + 6000, "", "")).isTrue();
            assertThat(isDayAt(24_000 * 5 + 18000, "", "")).isFalse();
        }
    }

    @Nested
    @DisplayName("between time")
    class BetweenTime {

        private boolean activeAt(long worldTicks, String start, String end) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MCX027]", start, end)
                    .worldTicks(worldTicks)
                    .build();

            TimeChips.betweenTime().trigger(state);
            return state.mainOutput();
        }

        @Test
        void outputsInsideItsWindow() {
            assertThat(activeAt(5000, "4000", "6000")).isTrue();
            assertThat(activeAt(3000, "4000", "6000")).isFalse();
            assertThat(activeAt(7000, "4000", "6000")).isFalse();
        }

        @Test
        void includesBothEnds() {
            assertThat(activeAt(4000, "4000", "6000")).isTrue();
            assertThat(activeAt(6000, "4000", "6000")).isTrue();
        }

        @Test
        void coversTheWholeDayByDefault() {
            assertThat(activeAt(0, "", "")).isTrue();
            assertThat(activeAt(23999, "", "")).isTrue();
        }

        @Test
        void neverMatchesAWindowThatStartsAfterItEnds() {
            assertThat(activeAt(12000, "18000", "6000")).isFalse();
        }
    }

    @Nested
    @DisplayName("time modulus")
    class TimeModulus {

        @ParameterizedTest(name = "tick {0} mod 2 >= 0 is {1}")
        @CsvSource({"0, true", "1, true"})
        void defaultsToFollowingWhetherTheTimeIsOdd(long worldTicks, boolean expected) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MC1025]", "", "")
                    .inputs(true, false, false)
                    .worldTicks(worldTicks)
                    .build();

            TimeChips.worldTimeModulus().trigger(state);

            assertThat(state.mainOutput()).isEqualTo(expected);
        }

        @Test
        void comparesTheRemainderAgainstItsThreshold() {
            SimpleChipState low = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MC1025]", "10", "5")
                    .inputs(true, false, false)
                    .worldTicks(103)
                    .build();
            SimpleChipState high = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MC1025]", "10", "5")
                    .inputs(true, false, false)
                    .worldTicks(107)
                    .build();

            TimeChips.worldTimeModulus().trigger(low);
            TimeChips.worldTimeModulus().trigger(high);

            assertThat(low.mainOutput()).isFalse();
            assertThat(high.mainOutput()).isTrue();
        }

        @Test
        void doesNothingWhileIdle() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MC1025]", "1", "0")
                    .worldTicks(5)
                    .build();

            TimeChips.worldTimeModulus().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void ignoresADivisorOfZero() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MC1025]", "0", "0")
                    .inputs(true, false, false)
                    .worldTicks(5)
                    .build();

            TimeChips.worldTimeModulus().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void theUnixVariantReadsTheWallClock() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MC1026]", "10", "5")
                    .inputs(true, false, false)
                    .time(com.xeonproductions.craftbookultimate.core.platform.TimeSource.fixed(0, 107))
                    .build();

            TimeChips.unixTimeModulus().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("pulse")
    class Pulse {

        private SimpleChipState pulsing(String length, String count) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MCX010]", length, count)
                    .inputs(true, false, false)
                    .build();
        }

        @Test
        void raisesAndLowersItsOutputOnce() {
            ICLogic pulse = TimeChips.pulse();
            SimpleChipState state = pulsing("100", "1");

            pulse.trigger(state);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(2);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(2);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void stopsAfterItsConfiguredNumberOfPulses() {
            ICLogic pulse = TimeChips.pulse();
            SimpleChipState state = pulsing("100", "2");

            pulse.trigger(state);
            state.manualScheduler().advance(100);

            assertThat(state.manualScheduler().pendingCount()).isZero();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void sendsAsManyPulsesAsAsked() {
            ICLogic pulse = TimeChips.pulse();
            SimpleChipState state = pulsing("100", "3");

            pulse.trigger(state);

            int rises = 0;
            boolean previous = false;
            for (int i = 0; i < 100; i++) {
                state.manualScheduler().advance(1);
                if (state.mainOutput() && !previous) {
                    rises++;
                }
                previous = state.mainOutput();
            }

            assertThat(rises).isEqualTo(3);
        }

        @Test
        void doesNothingWhileIdle() {
            ICLogic pulse = TimeChips.pulse();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MCX010]", "100", "1")
                    .build();

            pulse.trigger(state);

            assertThat(state.manualScheduler().pendingCount()).isZero();
        }

        @Test
        void stopsWhenTheChipIsUnloaded() {
            ICLogic pulse = TimeChips.pulse();
            SimpleChipState state = pulsing("100", "5");

            pulse.trigger(state);
            pulse.unload(state);

            assertThat(state.manualScheduler().pendingCount()).isZero();
        }
    }

    @Nested
    @DisplayName("signal extender")
    class SignalExtender {

        private SimpleChipState extending(String config) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("", "[MCX011]", config, "")
                    .build();
        }

        @Test
        void risesImmediatelyWithItsInput() {
            ICLogic extender = TimeChips.signalExtender();
            SimpleChipState state = extending("500").withInput(0, true);

            extender.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void holdsItsOutputAfterTheInputStops() {
            ICLogic extender = TimeChips.signalExtender();
            SimpleChipState state = extending("500").withInput(0, true);

            extender.trigger(state);
            state.withInput(0, false);
            extender.trigger(state);

            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(9);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void cancelsAPendingDropWhenTheInputReturns() {
            ICLogic extender = TimeChips.signalExtender();
            SimpleChipState state = extending("500").withInput(0, true);

            extender.trigger(state);
            state.withInput(0, false);
            extender.trigger(state);
            state.withInput(0, true);
            extender.trigger(state);

            state.manualScheduler().advance(100);

            assertThat(state.mainOutput()).isTrue();
        }

        @ParameterizedTest(name = "\"{0}\" holds for {1} ticks")
        @CsvSource({"500, 10", "'10:T', 10", "'1:S', 20", "'500:MS', 10"})
        void readsItsUnitFromTheSign(String config, int expectedTicks) {
            ICLogic extender = TimeChips.signalExtender();
            SimpleChipState state = extending(config).withInput(0, true);

            extender.trigger(state);
            state.withInput(0, false);
            extender.trigger(state);

            state.manualScheduler().advance(expectedTicks - 1);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("delayed repeater and inverter")
    class DelayedBuffers {

        @Test
        void passesStraightThroughWithNoDelay() {
            ICLogic repeater = TimeChips.delayedRepeater();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .sign("", "[MC1000]", "", "")
                    .inputs(true, false, false, false)
                    .build();

            repeater.trigger(state);

            assertThat(state.mainOutput()).isTrue();
            assertThat(state.manualScheduler().pendingCount()).isZero();
        }

        @Test
        void waitsBeforePassingItsInputOn() {
            ICLogic repeater = TimeChips.delayedRepeater();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .sign("", "[MC1000]", "500", "")
                    .inputs(true, false, false, false)
                    .build();

            repeater.trigger(state);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(10);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void settlesOnTheInputAsItIsWhenTheDelayElapses() {
            // A pulse shorter than the delay leaves the output low, because the chip reads the
            // input again when it finally acts rather than replaying what it saw earlier.
            ICLogic repeater = TimeChips.delayedRepeater();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .sign("", "[MC1000]", "500", "")
                    .inputs(true, false, false, false)
                    .build();

            repeater.trigger(state);
            state.withInput(0, false);
            state.manualScheduler().advance(10);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void replacesAPendingDelayWithTheLatestChange() {
            ICLogic repeater = TimeChips.delayedRepeater();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .sign("", "[MC1000]", "500", "")
                    .inputs(true, false, false, false)
                    .build();

            repeater.trigger(state);
            repeater.trigger(state);

            assertThat(state.manualScheduler().pendingCount()).isEqualTo(1);
        }

        @Test
        void theInverterIsTheOppositeOfTheRepeater() {
            ICLogic inverter = TimeChips.delayedInverter();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .sign("", "[MC1001]", "", "")
                    .build();

            inverter.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }
}
