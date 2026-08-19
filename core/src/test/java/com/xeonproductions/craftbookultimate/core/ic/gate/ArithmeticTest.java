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

@DisplayName("Arithmetic chips")
class ArithmeticTest {

    private static SimpleChipState run(ICLogic chip, boolean a, boolean b, boolean c) {
        SimpleChipState state =
                SimpleChipState.forLayout(PinLayout.THREE_I_3O).inputs(a, b, c).build();
        chip.trigger(state);
        return state;
    }

    @Nested
    @DisplayName("half adder")
    class HalfAdder {

        @ParameterizedTest(name = "{0}+{1} -> sum {2} carry {3}")
        @CsvSource({
            "false, false, false, false",
            "true,  false, true,  false",
            "false, true,  true,  false",
            "true,  true,  false, true",
        })
        void followsItsTruthTable(boolean a, boolean b, boolean sum, boolean carry) {
            SimpleChipState state = run(Arithmetic.halfAdder(), false, a, b);

            assertThat(state.output(0)).isEqualTo(sum);
            assertThat(state.output(1)).isEqualTo(carry);
        }

        @Test
        void ignoresTheCarryInputPin() {
            SimpleChipState withCarry = run(Arithmetic.halfAdder(), true, true, true);
            SimpleChipState withoutCarry = run(Arithmetic.halfAdder(), false, true, true);

            assertThat(withCarry.outputs()).isEqualTo(withoutCarry.outputs());
        }
    }

    @Nested
    @DisplayName("full adder")
    class FullAdder {

        @ParameterizedTest(name = "{0}+{1}+{2} -> sum {3} carry {4}")
        @CsvSource({
            "false, false, false, false, false",
            "false, true,  false, true,  false",
            "false, true,  true,  false, true",
            "true,  true,  true,  true,  true",
            "true,  false, false, true,  false",
            "true,  true,  false, false, true",
        })
        void followsItsTruthTable(
                boolean carryIn, boolean a, boolean b, boolean sum, boolean carryOut) {
            SimpleChipState state = run(Arithmetic.fullAdder(), carryIn, a, b);

            assertThat(state.output(0)).isEqualTo(sum);
            assertThat(state.output(1)).isEqualTo(carryOut);
        }

        @Test
        void matchesIntegerAdditionAcrossEveryInput() {
            for (int bits = 0; bits < 8; bits++) {
                boolean carryIn = (bits & 1) != 0;
                boolean a = (bits & 2) != 0;
                boolean b = (bits & 4) != 0;
                int expected = (carryIn ? 1 : 0) + (a ? 1 : 0) + (b ? 1 : 0);

                SimpleChipState state = run(Arithmetic.fullAdder(), carryIn, a, b);
                int actual = (state.output(0) ? 1 : 0) + (state.output(1) ? 2 : 0);

                assertThat(actual).as("%d + %d + %d", carryIn ? 1 : 0, a ? 1 : 0, b ? 1 : 0)
                        .isEqualTo(expected);
            }
        }
    }

    @Nested
    @DisplayName("subtractors")
    class Subtractors {

        @ParameterizedTest(name = "{0}-{1} -> difference {2} borrow {3}")
        @CsvSource({
            "false, false, false, false",
            "true,  false, true,  false",
            "false, true,  true,  true",
            "true,  true,  false, false",
        })
        void theHalfSubtractorFollowsItsTruthTable(
                boolean minuend, boolean subtrahend, boolean difference, boolean borrow) {
            SimpleChipState state = run(Arithmetic.halfSubtractor(), false, minuend, subtrahend);

            assertThat(state.output(0)).isEqualTo(difference);
            assertThat(state.output(1)).isEqualTo(borrow);
        }

        @Test
        void theFullSubtractorMatchesIntegerSubtractionAcrossEveryInput() {
            for (int bits = 0; bits < 8; bits++) {
                boolean minuend = (bits & 1) != 0;
                boolean subtrahend = (bits & 2) != 0;
                boolean borrowIn = (bits & 4) != 0;
                int result = (minuend ? 1 : 0) - (subtrahend ? 1 : 0) - (borrowIn ? 1 : 0);

                SimpleChipState state =
                        run(Arithmetic.fullSubtractor(), minuend, subtrahend, borrowIn);

                assertThat(state.output(0))
                        .as("difference bit of %d - %d - %d",
                                minuend ? 1 : 0, subtrahend ? 1 : 0, borrowIn ? 1 : 0)
                        .isEqualTo((result & 1) != 0);
                assertThat(state.output(1))
                        .as("borrow of %d - %d - %d",
                                minuend ? 1 : 0, subtrahend ? 1 : 0, borrowIn ? 1 : 0)
                        .isEqualTo(result < 0);
            }
        }
    }

    @Test
    void everyChipDuplicatesItsCarryOntoBothCarryPins() {
        for (ICLogic chip : new ICLogic[] {
            Arithmetic.halfAdder(), Arithmetic.fullAdder(),
            Arithmetic.halfSubtractor(), Arithmetic.fullSubtractor()
        }) {
            SimpleChipState state = run(chip, true, false, true);

            assertThat(state.output(1)).isEqualTo(state.output(2));
        }
    }
}
