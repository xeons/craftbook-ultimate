package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("In-memory chip state")
class SimpleChipStateTest {

    @Nested
    @DisplayName("pins")
    class Pins {

        @Test
        void takesItsPinCountsFromALayout() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_3O).build();

            assertThat(state.inputCount()).isEqualTo(3);
            assertThat(state.outputCount()).isEqualTo(3);
        }

        @Test
        void startsWithEverythingOff() {
            SimpleChipState state = SimpleChipState.of(2, 2).build();

            assertThat(state.anyInput()).isFalse();
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void tracksInputsAndTheirPowerTogether() {
            SimpleChipState state = SimpleChipState.of(1, 1).build().withInput(0, true);

            assertThat(state.input(0)).isTrue();
            assertThat(state.inputPower(0)).isEqualTo(15);
        }

        @Test
        void derivesTheBooleanInputFromAnAnalogLevel() {
            SimpleChipState state = SimpleChipState.of(1, 1).build().withInputPower(0, 7);

            assertThat(state.input(0)).isTrue();
            assertThat(state.inputPower(0)).isEqualTo(7);

            state.withInputPower(0, 0);

            assertThat(state.input(0)).isFalse();
        }

        @Test
        void rejectsAPowerLevelOutsideTheRedstoneRange() {
            SimpleChipState state = SimpleChipState.of(1, 1).build();

            assertThatThrownBy(() -> state.withInputPower(0, 16))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsAPinThatDoesNotExist() {
            SimpleChipState state = SimpleChipState.of(1, 1).build();

            assertThatThrownBy(() -> state.input(1)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> state.output(1)).isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("convenience views")
    class ConvenienceViews {

        @Test
        void reportsWhetherAnyOrEveryInputIsPowered() {
            SimpleChipState state = SimpleChipState.of(3, 1).build().withInputs(true, false, false);

            assertThat(state.anyInput()).isTrue();
            assertThat(state.allInputs()).isFalse();
            assertThat(state.poweredInputCount()).isEqualTo(1);

            state.withInputs(true, true, true);

            assertThat(state.allInputs()).isTrue();
            assertThat(state.poweredInputCount()).isEqualTo(3);
        }

        @Test
        void treatsAChipWithNoInputsAsNotFullyPowered() {
            // "All inputs are on" must be false when there are none, or a gate with no inputs
            // would read as permanently satisfied.
            SimpleChipState state = SimpleChipState.of(0, 1).build();

            assertThat(state.allInputs()).isFalse();
            assertThat(state.anyInput()).isFalse();
        }

        @Test
        void namesTheFirstPinAsTheMainOne() {
            SimpleChipState state = SimpleChipState.of(1, 1).build().withInput(0, true);

            assertThat(state.mainInput()).isTrue();

            state.setMainOutput(true);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void drivesEveryOutputAtOnce() {
            SimpleChipState state = SimpleChipState.of(1, 3).build();

            state.setAllOutputs(true);

            assertThat(state.outputs()).containsExactly(true, true, true);
        }
    }

    @Nested
    @DisplayName("triggering")
    class Triggering {

        @Test
        void reportsWhichInputCausedTheRun() {
            SimpleChipState state = SimpleChipState.of(3, 1).triggeredInput(1).build();

            assertThat(state.triggeredInput()).isEqualTo(1);
            assertThat(state.isTriggered(1)).isTrue();
            assertThat(state.isTriggered(0)).isFalse();
            assertThat(state.isTriggeredByInput()).isTrue();
        }

        @Test
        void reportsNoTriggeringInputForATick() {
            SimpleChipState state = SimpleChipState.of(3, 1).build();

            assertThat(state.triggeredInput()).isEqualTo(-1);
            assertThat(state.isTriggeredByInput()).isFalse();
        }

        @Test
        void rejectsATriggeringInputThatDoesNotExist() {
            SimpleChipState state = SimpleChipState.of(1, 1).build();

            assertThatThrownBy(() -> state.withTriggeredInput(5))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("inverting mode")
    class InvertingMode {

        @Test
        void invertsWhatTheChipWrites() {
            SimpleChipState state =
                    SimpleChipState.of(1, 1).mode(ICMode.parse("!")).build();

            state.setOutput(0, true);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void leavesOutputsAloneWithoutTheInvertingMode() {
            SimpleChipState state = SimpleChipState.of(1, 1).build();

            state.setOutput(0, true);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void letsATestPresetAnOutputWithoutInverting() {
            // Flip-flops read their own previous output, so a test needs to seed it directly.
            SimpleChipState state =
                    SimpleChipState.of(1, 1).mode(ICMode.parse("!")).build().withRawOutput(0, true);

            assertThat(state.output(0)).isTrue();
        }
    }

    @Nested
    @DisplayName("configuration")
    class Configuration {

        @Test
        void exposesTheSignItWasBuiltWith() {
            SimpleChipState state =
                    SimpleChipState.of(1, 1).sign("REPEATER", "[MC1000]", "5", "").build();

            assertThat(state.sign().trimmedText(2)).isEqualTo("5");
        }

        @Test
        void defaultsToABlankSign() {
            assertThat(SimpleChipState.of(1, 1).build().sign()).isEqualTo(SignLines.EMPTY);
        }

        @Test
        void defaultsToNoMode() {
            assertThat(SimpleChipState.of(1, 1).build().mode()).isEqualTo(ICMode.NONE);
        }
    }

    @Test
    void doesNotShareItsBackingArrays() {
        SimpleChipState state = SimpleChipState.of(1, 1).build();

        state.outputs()[0] = true;
        state.inputs()[0] = true;

        assertThat(state.output(0)).isFalse();
        assertThat(state.input(0)).isFalse();
    }
}
