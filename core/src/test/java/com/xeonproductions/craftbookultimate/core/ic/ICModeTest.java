// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("IC mode string")
class ICModeTest {

    @Nested
    @DisplayName("behaviour selection")
    class BehaviourSelection {

        @ParameterizedTest(name = "\"{0}\" selects {1}")
        @CsvSource({
            "'!', INVERT",
            "'+', LOG_TO_NEARBY_PLAYERS",
            "'1', CYCLE_OFF",
            "'=', NO_Y_OFFSET",
            "'r', REVERSE",
            "'t', THUNDER_STORM",
            "'-', HUMANS_ONLY_EXTRA",
            "'p', TELEPORT_PAD",
            "'P', TELEPORT_PAD_FORCED_PRESSURE_PLATE",
        })
        void recognisesEveryModeCharacter(String raw, ICMode.Behaviour expected) {
            assertThat(ICMode.parse(raw).behaviour()).isEqualTo(expected);
        }

        @Test
        void distinguishesTheTwoTeleportPadModesByCase() {
            assertThat(ICMode.parse("p").behaviour()).isEqualTo(ICMode.Behaviour.TELEPORT_PAD);
            assertThat(ICMode.parse("P").behaviour())
                    .isEqualTo(ICMode.Behaviour.TELEPORT_PAD_FORCED_PRESSURE_PLATE);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "   ", "zz", "?"})
        void fallsBackToNoBehaviour(String raw) {
            assertThat(ICMode.parse(raw).behaviour()).isEqualTo(ICMode.Behaviour.NONE);
        }

        @Test
        void reportsInversionOnlyForTheInvertMode() {
            assertThat(ICMode.parse("!").invertsOutputs()).isTrue();
            assertThat(ICMode.parse("r").invertsOutputs()).isFalse();
            assertThat(ICMode.NONE.invertsOutputs()).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = ICMode.Behaviour.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
        void everyBehaviourIsReachableFromItsOwnSymbol(ICMode.Behaviour behaviour) {
            assertThat(ICMode.parse(String.valueOf(behaviour.symbol())).behaviour())
                    .isEqualTo(behaviour);
        }
    }

    @Nested
    @DisplayName("pin permutation")
    class PinPermutation {

        @Test
        void readsAFullPermutation() {
            ICMode mode = ICMode.parse("badcfe");

            ICMode.PinPermutation permutation = mode.permutation().orElseThrow();
            assertThat(permutation.size()).isEqualTo(6);
            assertThat(permutation.slotFor(0)).isEqualTo(1);
            assertThat(permutation.slotFor(1)).isEqualTo(0);
            assertThat(permutation.slotFor(2)).isEqualTo(3);
        }

        @Test
        void acceptsAReversedPermutation() {
            // Any ordering of the letters is a permutation, including a plain reversal.
            ICMode mode = ICMode.parse("fedcba");

            assertThat(mode.permutation()).isPresent();
            assertThat(mode.slotFor(0)).isEqualTo(5);
            assertThat(mode.slotFor(5)).isEqualTo(0);
        }

        @Test
        void combinesABehaviourWithAPermutation() {
            ICMode mode = ICMode.parse("!badcfe");

            assertThat(mode.behaviour()).isEqualTo(ICMode.Behaviour.INVERT);
            assertThat(mode.slotFor(0)).isEqualTo(1);
        }

        @Test
        void acceptsAShortPermutationAndLeavesTheRestAlone() {
            ICMode mode = ICMode.parse("ba");

            assertThat(mode.slotFor(0)).isEqualTo(1);
            assertThat(mode.slotFor(1)).isEqualTo(0);
            assertThat(mode.slotFor(2)).isEqualTo(2);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"aa", "abcdefg", "az", "abz"})
        void rejectsAnythingThatIsNotAPermutation(String raw) {
            assertThat(ICMode.parse(raw).permutation()).isEmpty();
        }

        @Test
        void leavesPinsUntouchedWithoutAPermutation() {
            assertThat(ICMode.NONE.slotFor(3)).isEqualTo(3);
        }

        @Test
        void rendersBackToItsOwnLetters() {
            assertThat(ICMode.parse("badcfe").permutation().orElseThrow()).hasToString("badcfe");
        }

        @Test
        void doesNotShareItsBackingArray() {
            ICMode.PinPermutation permutation = ICMode.parse("ba").permutation().orElseThrow();

            permutation.slots()[0] = 99;

            assertThat(permutation.slotFor(0)).isEqualTo(1);
        }

        @Test
        void comparesByValue() {
            assertThat(ICMode.parse("ba")).isEqualTo(ICMode.parse("ba"));
            assertThat(ICMode.parse("ba")).isNotEqualTo(ICMode.parse("ab"));
        }
    }

    @Nested
    @DisplayName("fitting a permutation to a chip")
    class Fitting {

        @Test
        void keepsAPermutationTheChipCanHonour() {
            // 3I3O has six pins: three inputs then three outputs. This swaps the first two of
            // each, which leaves every pin on its own side.
            ICMode mode = ICMode.parse("bacedf");

            assertThat(mode.fittedTo(PinLayout.THREE_I_3O)).isEqualTo(mode);
        }

        @Test
        void dropsAPermutationNamingAPinTheChipDoesNotHave() {
            // SISO has two pins; f is the sixth. Before this the chip threw as its chunk loaded.
            ICMode mode = ICMode.parse("f");

            assertThat(mode.fittedTo(PinLayout.SISO).permutation()).isEmpty();
        }

        @Test
        void dropsAPermutationLongerThanTheChip() {
            assertThat(ICMode.parse("bacedf").fittedTo(PinLayout.AISO).permutation()).isEmpty();
        }

        @Test
        void dropsAPermutationThatWouldPutTwoPinsOnOneBlock() {
            // ac moves pin 0 to slot 0 and pin 1 to slot 2, and pin 2 stays at slot 2 because the
            // permutation says nothing about it. Two pins, one block.
            assertThat(ICMode.parse("ac").fittedTo(PinLayout.THREE_I_SO).permutation()).isEmpty();
        }

        @Test
        void keepsAShortPermutationThatRearrangesOnlyItsOwnPins() {
            ICMode fitted = ICMode.parse("ba").fittedTo(PinLayout.THREE_I_SO);

            assertThat(fitted.slotFor(0)).isEqualTo(1);
            assertThat(fitted.slotFor(1)).isEqualTo(0);
            assertThat(fitted.slotFor(2)).isEqualTo(2);
        }

        @Test
        void keepsTheBehaviourWhenItDropsThePermutation() {
            ICMode fitted = ICMode.parse("!f").fittedTo(PinLayout.SISO);

            assertThat(fitted.behaviour()).isEqualTo(ICMode.Behaviour.INVERT);
            assertThat(fitted.permutation()).isEmpty();
        }

        @Test
        void leavesAModeWithNoPermutationAlone() {
            assertThat(ICMode.parse("!").fittedTo(PinLayout.SISO))
                    .isEqualTo(ICMode.parse("!"));
        }

        @Test
        void dropsAPermutationSendingAnInputToAnOutput() {
            // SISO is one input then one output, so ba asks the chip to read the block it drives
            // and drive the block it reads. An input stays an input. See finding 153.
            assertThat(ICMode.parse("ba").fittedTo(PinLayout.SISO).permutation()).isEmpty();
        }

        @Test
        void dropsAPermutationCrossingInTheMiddleOfALongerRun() {
            // badcfe reads as a pairwise swap and is the example the fork's own source gives, but
            // c and d are the last input and the first output, so it swaps one for the other.
            assertThat(ICMode.parse("badcfe").fittedTo(PinLayout.THREE_I_3O).permutation()).isEmpty();
        }

        @Test
        void movesTheOutputsAmongThemselves() {
            ICMode fitted = ICMode.parse("abcedf").fittedTo(PinLayout.THREE_I_3O);

            assertThat(fitted.slotFor(3)).isEqualTo(4);
            assertThat(fitted.slotFor(4)).isEqualTo(3);
            assertThat(fitted.slotFor(0)).isZero();
        }

        @Test
        void countsSidesByTheChipsOwnWiringRatherThanTheLetters() {
            // AISO is four inputs and one output, so d is an input there and e is the output --
            // not the three-and-three split the letters suggest.
            assertThat(ICMode.parse("badc").fittedTo(PinLayout.AISO).permutation()).isPresent();
            assertThat(ICMode.parse("badc").fittedTo(PinLayout.THREE_I_SO).permutation()).isEmpty();
        }
    }

    @Nested
    @DisplayName("no mode")
    class NoMode {

        @Test
        void anEmptyStringIsNoModeAtAll() {
            assertThat(ICMode.parse("")).isEqualTo(ICMode.NONE);
            assertThat(ICMode.parse("").isNone()).isTrue();
        }

        @Test
        void aBehaviourIsNotNoMode() {
            assertThat(ICMode.parse("!").isNone()).isFalse();
        }

        @Test
        void parsingNeverFails() {
            // A typo in a mode string should leave a working chip, not a broken one.
            assertThat(ICMode.parse("!!!nonsense???")).isNotNull();
        }
    }
}
