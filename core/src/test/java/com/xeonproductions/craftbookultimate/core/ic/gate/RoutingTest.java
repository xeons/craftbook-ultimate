package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Routing chips")
class RoutingTest {

    @Nested
    @DisplayName("dispatcher")
    class Dispatcher {

        private SimpleChipState dispatch(boolean data, boolean selectB, boolean selectC) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_3O)
                    .inputs(data, selectB, selectC)
                    .build();
            Routing.dispatcher().trigger(state);
            return state;
        }

        @Test
        void copiesDataOntoASelectedOutput() {
            SimpleChipState state = dispatch(true, true, false);

            assertThat(state.output(1)).isTrue();
        }

        @Test
        void copiesDataOntoBothSelectedOutputs() {
            SimpleChipState state = dispatch(true, true, true);

            assertThat(state.output(1)).isTrue();
            assertThat(state.output(2)).isTrue();
        }

        @Test
        void leavesAnUnselectedOutputAtItsLastValue() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_3O)
                    .inputs(false, false, false)
                    .build()
                    .withRawOutput(1, true);

            Routing.dispatcher().trigger(state);

            assertThat(state.output(1)).isTrue();
        }
    }

    @Nested
    @DisplayName("multiplexer")
    class Multiplexer {

        private SimpleChipState select(boolean selector, boolean high, boolean low, int triggered) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(selector, high, low)
                    .triggeredInput(triggered)
                    .build();
            Routing.multiplexer().trigger(state);
            return state;
        }

        @ParameterizedTest(name = "selector {0} -> {3}")
        @CsvSource({
            "true,  true,  false, true",
            "true,  false, true,  false",
            "false, true,  false, false",
            "false, false, true,  true",
        })
        void passesTheSelectedSource(
                boolean selector, boolean high, boolean low, boolean expected) {
            assertThat(select(selector, high, low, 1).mainOutput()).isEqualTo(expected);
        }

        @Test
        void staysPutOnTheRunWhereTheSelectorItselfChanged() {
            SimpleChipState state = select(true, true, false, 0);

            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("demultiplexer")
    class Demultiplexer {

        private SimpleChipState address(boolean lowBit, boolean highBit) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_5O)
                    .inputs(false, lowBit, highBit)
                    .build();
            Routing.demultiplexer(1, 2).trigger(state);
            return state;
        }

        @ParameterizedTest(name = "{0},{1} -> output {2}")
        @CsvSource({
            "false, false, 0",
            "true,  false, 1",
            "false, true,  2",
            "true,  true,  3",
        })
        void raisesTheAddressedOutput(boolean lowBit, boolean highBit, int expected) {
            SimpleChipState state = address(lowBit, highBit);

            for (int output = 0; output < state.outputCount(); output++) {
                assertThat(state.output(output))
                        .as("output %d", output)
                        .isEqualTo(output == expected);
            }
        }

        @Test
        void raisesExactlyOneOutput() {
            SimpleChipState state = address(true, true);

            long raised = 0;
            for (int output = 0; output < state.outputCount(); output++) {
                if (state.output(output)) {
                    raised++;
                }
            }

            assertThat(raised).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("random bits")
    class RandomBits {

        @Test
        void raisesTheRequestedNumberOfOutputs() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.SI3O)
                    .inputs(true)
                    .build();

            Routing.randomBits(2, 2, new Random(42)).trigger(state);

            assertThat(countRaised(state)).isEqualTo(2);
        }

        @Test
        void staysWithinItsBounds() {
            for (int seed = 0; seed < 25; seed++) {
                SimpleChipState state = SimpleChipState.forLayout(PinLayout.SI5O)
                        .inputs(true)
                        .build();

                Routing.randomBits(1, 3, new Random(seed)).trigger(state);

                assertThat(countRaised(state)).isBetween(1, 3);
            }
        }

        @Test
        void isRepeatableForAGivenSeed() {
            assertThat(outputsForSeed(7)).isEqualTo(outputsForSeed(7));
        }

        @Test
        void variesAcrossSeeds() {
            boolean[] first = outputsForSeed(1);
            boolean differs = false;
            for (int seed = 2; seed < 30 && !differs; seed++) {
                differs = !java.util.Arrays.equals(first, outputsForSeed(seed));
            }

            assertThat(differs).isTrue();
        }

        @Test
        void pullsBoundsBackToTheNumberOfOutputs() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.SI3O)
                    .inputs(true)
                    .build();

            Routing.randomBits(10, 10, new Random(1)).trigger(state);

            assertThat(countRaised(state)).isEqualTo(3);
        }

        @Test
        void doesNothingWhileIdle() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.SI3O)
                    .inputs(false)
                    .build();

            Routing.randomBits(3, 3, new Random(1)).trigger(state);

            assertThat(countRaised(state)).isZero();
        }

        private boolean[] outputsForSeed(int seed) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.SI5O)
                    .inputs(true)
                    .build();
            Routing.randomBits(2, 2, new Random(seed)).trigger(state);
            return state.outputs();
        }

        private int countRaised(SimpleChipState state) {
            int count = 0;
            for (boolean raised : state.outputs()) {
                if (raised) {
                    count++;
                }
            }
            return count;
        }
    }
}
