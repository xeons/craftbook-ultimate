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

@DisplayName("Delaying one edge, and pulsing on either")
class EdgeTimingTest {

    private static SimpleChipState chip(String model, String third, String fourth) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .sign("", "[" + model + "]", third, fourth)
                .build();
    }

    @Nested
    @DisplayName("the delayer")
    class Delayer {

        @Test
        void waitsBeforeTurningOn() {
            ICLogic delayer = TimeChips.onDelay();
            SimpleChipState state = chip("MC2100", "500", "").withInput(0, true);

            delayer.trigger(state);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(9);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void turnsOffTheMomentItsInputDoes() {
            // The whole of what separates it from the delayed repeater, which waits on both edges.
            ICLogic delayer = TimeChips.onDelay();
            SimpleChipState state = chip("MC2100", "500", "").withInput(0, true);

            delayer.trigger(state);
            state.manualScheduler().advance(10);
            assertThat(state.mainOutput()).isTrue();

            state.withInput(0, false);
            delayer.trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void swallowsAFlickerShorterThanItsDelay() {
            // What makes it a debounce: the output never sees the pulse at all.
            ICLogic delayer = TimeChips.onDelay();
            SimpleChipState state = chip("MC2100", "500", "").withInput(0, true);

            delayer.trigger(state);
            state.manualScheduler().advance(3);
            state.withInput(0, false);
            delayer.trigger(state);
            state.manualScheduler().advance(20);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void letsAWaitFinishWhenToldToHold() {
            ICLogic delayer = TimeChips.onDelay();
            SimpleChipState state = chip("MC2100", "500", "hold").withInput(0, true);

            delayer.trigger(state);
            state.manualScheduler().advance(3);
            state.withInput(0, false);
            delayer.trigger(state);

            // The wait survives the input going away, but reads the input when it fires.
            assertThat(state.manualScheduler().pendingCount()).isNotZero();
        }

        @Test
        void passesStraightThroughWithNoDelay() {
            ICLogic delayer = TimeChips.onDelay();
            SimpleChipState state = chip("MC2100", "", "").withInput(0, true);

            delayer.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void differsFromTheDelayedRepeaterOnTheFallingEdge() {
            SimpleChipState delayed = chip("MC2100", "500", "").withInput(0, true);
            SimpleChipState repeated = chip("MC1000", "500", "").withInput(0, true);

            ICLogic delayer = TimeChips.onDelay();
            ICLogic repeater = TimeChips.delayedRepeater();

            delayer.trigger(delayed);
            repeater.trigger(repeated);
            delayed.manualScheduler().advance(10);
            repeated.manualScheduler().advance(10);

            delayed.withInput(0, false);
            repeated.withInput(0, false);
            delayer.trigger(delayed);
            repeater.trigger(repeated);

            assertThat(delayed.mainOutput()).isFalse();
            assertThat(repeated.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("the inverted delayers")
    class InvertedDelayers {

        @Test
        void waitBeforeTurningOff() {
            ICLogic delayer = TimeChips.invertedOnDelay();
            SimpleChipState state = chip("MC2101", "500", "").withInput(0, true);

            delayer.trigger(state);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(10);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void turnOnTheMomentTheInputDrops() {
            ICLogic delayer = TimeChips.invertedOnDelay();
            SimpleChipState state = chip("MC2101", "500", "").withInput(0, false);

            delayer.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theFallingEdgeOneWaitsBeforeComingBackOn() {
            ICLogic delayer = TimeChips.invertedOffDelay();
            SimpleChipState state = chip("MC2111", "500", "").withInput(0, true);

            delayer.trigger(state);
            assertThat(state.mainOutput()).isFalse();

            state.withInput(0, false);
            delayer.trigger(state);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(10);
            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("the pulsers")
    class Pulsers {

        @Test
        void theOrdinaryOneFiresAsItsInputRises() {
            ICLogic pulser = TimeChips.pulse(false, false);
            SimpleChipState state = chip("MCX010", "100", "1").withInput(0, true);

            pulser.trigger(state);
            state.manualScheduler().advance(1);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theFallingEdgeOneDoesNotFireAsItsInputRises() {
            ICLogic pulser = TimeChips.pulse(true, false);
            SimpleChipState state = chip("MC2510", "100", "1").withInput(0, true);

            pulser.trigger(state);
            state.manualScheduler().advance(5);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void theFallingEdgeOneFiresAsItsInputDrops() {
            ICLogic pulser = TimeChips.pulse(true, false);
            SimpleChipState state = chip("MC2510", "100", "1").withInput(0, false);

            pulser.trigger(state);
            state.manualScheduler().advance(1);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theInvertedOneRestsHighAndPulsesLow() {
            ICLogic pulser = TimeChips.pulse(false, true);
            SimpleChipState state = chip("MC2501", "100", "1").withInput(0, true);

            pulser.trigger(state);
            state.manualScheduler().advance(1);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void waitsTheStartDelayBeforeTheFirstPulse() {
            ICLogic pulser = TimeChips.pulse(false, false);
            SimpleChipState state = chip("MCX010", "100:500", "1").withInput(0, true);

            pulser.trigger(state);
            state.manualScheduler().advance(5);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(5);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void restsBetweenPulsesWhenToldTo() {
            ICLogic pulser = TimeChips.pulse(false, false);
            SimpleChipState state = chip("MCX010", "100", "2:500").withInput(0, true);

            pulser.trigger(state);
            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(2);
            assertThat(state.mainOutput()).isFalse();

            // Still resting where a back-to-back burst would already have risen again.
            state.manualScheduler().advance(3);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(8);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void runsBackToBackWhenNoRestIsGiven() {
            // The frozen behaviour: a sign with no second field pulses exactly as it always did.
            ICLogic pulser = TimeChips.pulse(false, false);
            SimpleChipState state = chip("MCX010", "100", "2").withInput(0, true);

            pulser.trigger(state);
            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(2);
            assertThat(state.mainOutput()).isFalse();

            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void stopsAfterTheNumberOfPulsesAskedFor() {
            ICLogic pulser = TimeChips.pulse(false, false);
            SimpleChipState state = chip("MCX010", "100", "2").withInput(0, true);

            pulser.trigger(state);
            state.manualScheduler().advance(100);

            assertThat(state.manualScheduler().pendingCount()).isZero();
            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("the duration grammar")
    class Durations {

        @Test
        void readsTheSuffixSpellingEveryPageDocuments() {
            // 5T is what the docs have always said; only 5:T was ever read. See finding 148.
            // Five ticks, chosen because the extender's own default is a second — twenty ticks —
            // so a chip that failed to read the line would still be holding here.
            ICLogic extender = TimeChips.signalExtender();
            SimpleChipState state = chip("MCX011", "5T", "").withInput(0, true);

            extender.trigger(state);
            state.withInput(0, false);
            extender.trigger(state);

            state.manualScheduler().advance(4);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void stillReadsTheColonSpellingItUsedTo() {
            ICLogic extender = TimeChips.signalExtender();
            SimpleChipState state = chip("MCX011", "5:T", "").withInput(0, true);

            extender.trigger(state);
            state.withInput(0, false);
            extender.trigger(state);

            state.manualScheduler().advance(5);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void stillReadsABareNumberAsMilliseconds() {
            ICLogic extender = TimeChips.signalExtender();
            SimpleChipState state = chip("MCX011", "500", "").withInput(0, true);

            extender.trigger(state);
            state.withInput(0, false);
            extender.trigger(state);

            state.manualScheduler().advance(10);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void tellsASecondFieldApartFromAUnit() {
            // 100:500 is two fields; 100:S is one number with its unit.
            ICLogic pulser = TimeChips.pulse(false, false);
            SimpleChipState twoFields = chip("MCX010", "100:500", "1").withInput(0, true);

            pulser.trigger(twoFields);
            twoFields.manualScheduler().advance(5);

            assertThat(twoFields.mainOutput()).isFalse();
        }
    }
}
