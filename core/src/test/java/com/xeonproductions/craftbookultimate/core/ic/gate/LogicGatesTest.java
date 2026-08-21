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

@DisplayName("Combinational gates")
class LogicGatesTest {

    private static SimpleChipState threeInput(boolean... inputs) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO).inputs(inputs).build();
    }

    private static boolean run(ICLogic gate, SimpleChipState state) {
        gate.trigger(state);
        return state.mainOutput();
    }

    @Nested
    @DisplayName("AND")
    class And {

        @ParameterizedTest(name = "{0},{1},{2} -> {3}")
        @CsvSource({
            "true,  true,  true,  true",
            "true,  true,  false, false",
            "true,  false, false, false",
            "false, false, false, false",
        })
        void followsItsTruthTable(boolean a, boolean b, boolean c, boolean expected) {
            assertThat(run(LogicGates.and(), threeInput(a, b, c))).isEqualTo(expected);
        }

        @Test
        void ignoresPinsThatAreNotWired() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, true, false)
                    .connected(true, true, false)
                    .build();

            assertThat(run(LogicGates.and(), state)).isTrue();
        }

        @Test
        void staysLowWithOnlyOneInputWired() {
            // An AND of one operand is not meaningful, so the gate refuses rather than
            // mirroring the single wire.
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, false, false)
                    .connected(true, false, false)
                    .build();

            assertThat(run(LogicGates.and(), state)).isFalse();
        }
    }

    @Nested
    @DisplayName("NAND")
    class Nand {

        @ParameterizedTest(name = "{0},{1},{2} -> {3}")
        @CsvSource({
            "true,  true,  true,  false",
            "true,  true,  false, true",
            "false, false, false, true",
        })
        void followsItsTruthTable(boolean a, boolean b, boolean c, boolean expected) {
            assertThat(run(LogicGates.nand(), threeInput(a, b, c))).isEqualTo(expected);
        }

        @Test
        void invertsASingleWiredInput() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, false, false)
                    .connected(true, false, false)
                    .build();

            assertThat(run(LogicGates.nand(), state)).isFalse();
        }

        @Test
        void staysLowWithNothingWired() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .connected(false, false, false)
                    .build();

            assertThat(run(LogicGates.nand(), state)).isFalse();
        }
    }

    @Nested
    @DisplayName("XOR and XNOR")
    class ExclusiveGates {

        @ParameterizedTest(name = "{0},{1} -> {2}")
        @CsvSource({
            "true,  true,  false",
            "true,  false, true",
            "false, true,  true",
            "false, false, false",
        })
        void xorFollowsItsTruthTable(boolean a, boolean b, boolean expected) {
            assertThat(run(LogicGates.xor(), threeInput(a, b, false))).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0},{1} -> {2}")
        @CsvSource({
            "true,  true,  true",
            "true,  false, false",
            "false, false, true",
        })
        void xnorFollowsItsTruthTable(boolean a, boolean b, boolean expected) {
            assertThat(run(LogicGates.xnor(), threeInput(a, b, false))).isEqualTo(expected);
        }

        @Test
        void usesTheFirstTwoWiredInputs() {
            // With pin 0 bare, the operands are pins 1 and 2.
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, true, false)
                    .connected(false, true, true)
                    .build();

            assertThat(run(LogicGates.xor(), state)).isTrue();
        }

        @Test
        void leavesTheOutputAloneWithFewerThanTwoWiredInputs() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, false, false)
                    .connected(true, false, false)
                    .build()
                    .withRawOutput(0, true);

            LogicGates.xor().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("repeater and inverter")
    class Buffers {

        @Test
        void theRepeaterMirrorsAnyWiredInput() {
            assertThat(run(LogicGates.repeater(), threeInput(false, true, false))).isTrue();
            assertThat(run(LogicGates.repeater(), threeInput(false, false, false))).isFalse();
        }

        @Test
        void theInverterOpposesTheRepeater() {
            assertThat(run(LogicGates.inverter(), threeInput(false, true, false))).isFalse();
            assertThat(run(LogicGates.inverter(), threeInput(false, false, false))).isTrue();
        }

        @Test
        void aPowerSourceBehindTheSignDrivesThem() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .powerSourceBehind(true)
                    .build();

            assertThat(run(LogicGates.repeater(), state)).isTrue();
        }

        @Test
        void anUnwiredPoweredPinDoesNotDriveThem() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, false, false)
                    .connected(false, false, false)
                    .build();

            assertThat(run(LogicGates.repeater(), state)).isFalse();
        }
    }

    @Test
    void theInvertingModeFlipsWhatAGateDrives() {
        SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .inputs(true, true, true)
                .mode(com.xeonproductions.craftbookultimate.core.ic.ICMode.parse("!"))
                .build();

        assertThat(run(LogicGates.and(), state)).isFalse();
    }
}
