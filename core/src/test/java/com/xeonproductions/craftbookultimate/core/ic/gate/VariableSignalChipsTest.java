// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.mechanic.SimpleActor;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("The chips that carry a number between redstone and a variable")
class VariableSignalChipsTest {

    private final ChipServices services = ChipServices.create();

    private final VariableName score = VariableName.shared("score");

    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    /** The pin the readout uses to say the variable was there at all. */
    private static final int READABLE_PIN = 4;

    private Variables variables() {
        return services.variables();
    }

    private SimpleChipState.Builder readoutChip(String variable) {
        return SimpleChipState.forLayout(PinLayout.SI5O)
                .services(services)
                .at(SIGN, BlockFace.SOUTH)
                .sign("VAR READOUT", "[MCN100]", variable, "");
    }

    private SimpleChipState.Builder recorderChip(String variable) {
        return SimpleChipState.forLayout(PinLayout.SISO)
                .services(services)
                .at(SIGN, BlockFace.SOUTH)
                .sign("SIGNAL VAR", "[MCN101]", variable, "");
    }

    /** The number a chip's first four pins are showing. */
    private static int shownBy(SimpleChipState state) {
        int value = 0;
        for (int bit = 0; bit < 4; bit++) {
            if (state.output(bit)) {
                value |= 1 << bit;
            }
        }
        return value;
    }

    @Nested
    @DisplayName("the readout")
    class Readout {

        @ParameterizedTest
        @DisplayName("shows a variable across its pins")
        @CsvSource({"0,0", "1,1", "2,2", "5,5", "10,10", "11,11", "15,15"})
        void showsTheValue(double held, int shown) {
            variables().define(score, String.valueOf((int) held));
            SimpleChipState state = readoutChip("score").build();

            VariableChips.readout().tick(state);

            assertThat(shownBy(state)).isEqualTo(shown);
        }

        @Test
        void putsTheOnesOnTheFirstPin() {
            // Least significant first, which is the order the adders in this catalogue read.
            variables().define(score, "1");
            SimpleChipState state = readoutChip("score").build();

            VariableChips.readout().tick(state);

            assertThat(state.output(0)).isTrue();
            assertThat(state.output(1)).isFalse();
            assertThat(state.output(2)).isFalse();
            assertThat(state.output(3)).isFalse();
        }

        @Test
        void showsTheLargestItCanRatherThanWrappingRound() {
            variables().define(score, "9000");
            SimpleChipState state = readoutChip("score").build();

            VariableChips.readout().tick(state);

            assertThat(shownBy(state)).isEqualTo(15);
        }

        @Test
        void showsNothingBelowZero() {
            variables().define(score, "-4");
            SimpleChipState state = readoutChip("score").build();

            VariableChips.readout().tick(state);

            assertThat(shownBy(state)).isZero();
        }

        @Test
        void roundsAFractionToTheNearestWholeNumber() {
            variables().define(score, "6.7");
            SimpleChipState state = readoutChip("score").build();

            VariableChips.readout().tick(state);

            assertThat(shownBy(state)).isEqualTo(7);
        }

        @Test
        void saysTheVariableWasThereToRead() {
            variables().define(score, "3");
            SimpleChipState state = readoutChip("score").build();

            VariableChips.readout().tick(state);

            assertThat(state.output(READABLE_PIN)).isTrue();
        }

        @Test
        void saysWhenThereIsNoSuchVariable() {
            // The whole point of the fifth pin: a variable holding zero and one that has gone look
            // identical on the other four.
            SimpleChipState state = readoutChip("missing").build();

            VariableChips.readout().tick(state);

            assertThat(shownBy(state)).isZero();
            assertThat(state.output(READABLE_PIN)).isFalse();
        }

        @Test
        void saysWhenTheVariableHoldsSomethingThatIsNotANumber() {
            variables().define(score, "hello");
            SimpleChipState state = readoutChip("score").build();

            VariableChips.readout().tick(state);

            assertThat(state.output(READABLE_PIN)).isFalse();
        }

        @Test
        void needsItsInputWhenItIsNotTicking() {
            variables().define(score, "7");
            SimpleChipState state = readoutChip("score").inputs(false).build();

            VariableChips.readout().trigger(state);

            assertThat(shownBy(state)).isZero();
        }

        @Test
        void isRefusedWhenTheVariableDoesNotExist() {
            SignLines lines = SignLines.of("VAR READOUT", "[MCN100]", "nothing", "");

            assertThat(VariableChips.readout()
                            .reviewSign(lines, services, SimpleActor.named("Alice")))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("the signal recorder")
    class SignalRecorder {

        @ParameterizedTest
        @DisplayName("writes the level arriving at its input into the variable")
        @CsvSource({"0", "1", "7", "15"})
        void writesTheLevel(int arriving) {
            variables().define(score, "99");
            SimpleChipState state = recorderChip("score").build().withInputPower(0, arriving);

            VariableChips.signalRecorder().tick(state);

            assertThat(variables().number(score)).hasValue(arriving);
        }

        @Test
        void saysSoOnItsOutput() {
            variables().define(score, "0");
            SimpleChipState state = recorderChip("score").build().withInputPower(0, 9);

            VariableChips.signalRecorder().tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void readsLowWhenTheVariableHasGoneSinceTheSignWasWritten() {
            SimpleChipState state = recorderChip("score").build().withInputPower(0, 9);

            VariableChips.signalRecorder().tick(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void leavesTheVariableAloneWhenTheLevelHasNotChanged() {
            // A chip ticking against a steady lever must not rewrite the same number every tick.
            variables().define(score, "9");
            SimpleChipState state = recorderChip("score").build().withInputPower(0, 9);

            VariableChips.signalRecorder().tick(state);

            assertThat(variables().number(score)).hasValue(9);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void isRefusedWhenTheVariableDoesNotExist() {
            SignLines lines = SignLines.of("SIGNAL VAR", "[MCN101]", "nothing", "");

            assertThat(VariableChips.signalRecorder()
                            .reviewSign(lines, services, SimpleActor.named("Alice")))
                    .isPresent();
        }

        @Test
        void carriesALevelBackOutThroughTheReadout() {
            // The pair are each other's opposite, which is the reason both exist.
            variables().define(score, "0");
            SimpleChipState recorder = recorderChip("score").build().withInputPower(0, 11);

            VariableChips.signalRecorder().tick(recorder);

            SimpleChipState readout = readoutChip("score").build();
            VariableChips.readout().tick(readout);

            assertThat(shownBy(readout)).isEqualTo(11);
        }
    }
}
