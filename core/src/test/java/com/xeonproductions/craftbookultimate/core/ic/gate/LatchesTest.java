// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Latches and flip-flops")
class LatchesTest {

    private static SimpleChipState chip(boolean... inputs) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO).inputs(inputs).build();
    }

    @Nested
    @DisplayName("RS-NOR latch")
    class RsNor {

        @Test
        void setsOnTheSetInput() {
            SimpleChipState state = chip(true, false, false);

            Latches.rsNorLatch().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @ParameterizedTest(name = "reset on pin {0}")
        @CsvSource({"1", "2"})
        void resetsOnEitherResetInput(int resetPin) {
            SimpleChipState state = chip(false, false, false).withRawOutput(0, true);
            state.withInput(resetPin, true);

            Latches.rsNorLatch().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void letsResetWinOverSet() {
            SimpleChipState state = chip(true, true, false);

            Latches.rsNorLatch().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void holdsWithNoInput() {
            SimpleChipState state = chip(false, false, false).withRawOutput(0, true);

            Latches.rsNorLatch().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("RS-NAND latch")
    class RsNand {

        @ParameterizedTest(name = "set={0} reset={1} -> {2}")
        @CsvSource({
            "true,  true,  true",
            "true,  false, true",
            "false, true,  false",
        })
        void followsItsTruthTable(boolean set, boolean reset, boolean expected) {
            SimpleChipState state = chip(set, reset, false);

            Latches.rsNandLatch().trigger(state);

            assertThat(state.mainOutput()).isEqualTo(expected);
        }

        @Test
        void holdsWithNeitherInput() {
            SimpleChipState state = chip(false, false, false).withRawOutput(0, true);

            Latches.rsNandLatch().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("JK flip-flop")
    class JkFlipFlop {

        /** Clocked on the falling edge, so the clock pin must be the trigger and read low. */
        private SimpleChipState clocked(boolean j, boolean k) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(false, j, k)
                    .triggeredInput(0)
                    .build();
        }

        @Test
        void togglesWithBothInputsHigh() {
            SimpleChipState state = clocked(true, true).withRawOutput(0, false);

            Latches.jkFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isTrue();

            Latches.jkFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void setsWithJAlone() {
            SimpleChipState state = clocked(true, false);

            Latches.jkFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void resetsWithKAlone() {
            SimpleChipState state = clocked(false, true).withRawOutput(0, true);

            Latches.jkFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void ignoresARisingClock() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, true, false)
                    .triggeredInput(0)
                    .build();

            Latches.jkFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void ignoresARunTriggeredByAnotherPin() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(false, true, false)
                    .triggeredInput(1)
                    .build();

            Latches.jkFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("D flip-flops")
    class DFlipFlops {

        @Test
        void theEdgeTriggeredOneSamplesOnlyWhenTheClockChanges() {
            SimpleChipState clocking = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, true, false)
                    .triggeredInput(1)
                    .build();

            Latches.edgeTriggeredDFlipFlop().trigger(clocking);

            assertThat(clocking.mainOutput()).isTrue();

            SimpleChipState held = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(false, true, false)
                    .triggeredInput(0)
                    .build()
                    .withRawOutput(0, true);

            Latches.edgeTriggeredDFlipFlop().trigger(held);

            assertThat(held.mainOutput()).isTrue();
        }

        @Test
        void theEdgeTriggeredOneResetsAboveAll() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, true, true)
                    .triggeredInput(1)
                    .build()
                    .withRawOutput(0, true);

            Latches.edgeTriggeredDFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void theLevelTriggeredOneFollowsDataWhileTheClockIsHigh() {
            SimpleChipState state = chip(true, true, false);

            Latches.levelTriggeredDFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isTrue();

            state.withInput(1, false);
            Latches.levelTriggeredDFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void theLevelTriggeredOneHoldsWhileTheClockIsLow() {
            SimpleChipState state = chip(false, false, false).withRawOutput(0, true);

            Latches.levelTriggeredDFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theLevelTriggeredOneResetsEvenWhileClocked() {
            SimpleChipState state = chip(true, true, true);

            Latches.levelTriggeredDFlipFlop().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("toggle flip-flop")
    class ToggleFlipFlop {

        @Test
        void theRisingEdgeVariantTogglesAsItBecomesActive() {
            ICLogic gate = Latches.toggleFlipFlop(true);
            SimpleChipState active = chip(true, false, false);

            gate.trigger(active);

            assertThat(active.mainOutput()).isTrue();
        }

        @Test
        void theRisingEdgeVariantIgnoresBecomingInactive() {
            ICLogic gate = Latches.toggleFlipFlop(true);
            SimpleChipState idle = chip(false, false, false);

            gate.trigger(idle);

            assertThat(idle.mainOutput()).isFalse();
        }

        @Test
        void theFallingEdgeVariantTogglesAsItBecomesInactive() {
            ICLogic gate = Latches.toggleFlipFlop(false);
            SimpleChipState idle = chip(false, false, false);

            gate.trigger(idle);

            assertThat(idle.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("combination lock")
    class CombinationLock {

        private SimpleChipState withCombination(String combination, boolean... inputs) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(inputs)
                    .sign("", "[MC3050]", combination, "")
                    .build();
        }

        @Test
        void opensOnTheMatchingCombination() {
            // Characters are read middle, right, left against pins 0, 1 and 2.
            SimpleChipState state = withCombination("-XX", true, true, false);

            Latches.combinationLock().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void staysShutOnAWrongCombination() {
            SimpleChipState state = withCombination("-XX", true, false, false);

            Latches.combinationLock().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void staysShutOnATooShortCombination() {
            SimpleChipState state = withCombination("X", true, true, true);

            Latches.combinationLock().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("counters")
    class Counters {

        private SimpleChipState counting(String current, boolean advance, boolean reset) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(advance, reset, false)
                    .sign("", "[MC3102]", "", current)
                    .build();
        }

        @Test
        void theUpCounterAdvancesAndRecordsItsTotal() {
            SimpleChipState state = counting("0", true, false);

            Latches.counter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("1");
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void theUpCounterFiresOnReachingItsLimit() {
            SimpleChipState state = counting("2", true, false);

            Latches.counter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("3");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theUpCounterStopsAtItsLimitWhenNotRepeating() {
            SimpleChipState state = counting("3", true, false);

            Latches.counter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("3");
        }

        @Test
        void theUpCounterWrapsWhenRepeating() {
            SimpleChipState state = counting("3", true, false);

            Latches.counter(3, true).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("0");
        }

        @Test
        void theUpCounterReturnsToZeroOnReset() {
            SimpleChipState state = counting("2", false, true);

            Latches.counter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("0");
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void theDownCounterDecrementsAndFiresAtZero() {
            SimpleChipState state = counting("1", true, false);

            Latches.downCounter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("0");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theDownCounterReturnsToItsLimitOnReset() {
            SimpleChipState state = counting("1", false, true);

            Latches.downCounter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("3");
        }

        @Test
        void treatsAnUnreadableTotalAsZero() {
            SimpleChipState state = counting("banana", true, false);

            Latches.counter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("1");
        }

        @Test
        void leavesTheSignAloneWhenNothingChanged() {
            SimpleChipState state = counting("3", true, false);

            Latches.counter(3, false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("3");
        }
    }
}
