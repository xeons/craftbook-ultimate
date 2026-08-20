package com.xeonproductions.craftbookultimate.core.music;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("A tune written out on a sign")
class TunesTest {

    @Nested
    @DisplayName("reading notes")
    class ReadingNotes {

        @ParameterizedTest(name = "\"{0}\" is step {1}")
        @CsvSource({
            "F1, 0",
            "g3, 1",
            "G3, 2",
            "a3, 3",
            "A3, 4",
            "b3, 5",
            "c3, 6",
            "C3, 7",
            "d3, 8",
            "D3, 9",
            "e3, 10",
            "f3, 11",
            "F3, 12",
            "g2, 13",
            "c2, 18",
            "F4, 24"
        })
        void putsEachNoteOnItsOwnStepOfTheScale(String written, int step) {
            assertThat(Tunes.parse(written).beats().getFirst().step()).isEqualTo(step);
        }

        @Test
        void picksTheOctaveByWhetherTheDigitIsOddOrEven() {
            // The notation has always read this way round, and tunes are written against it.
            assertThat(Tunes.parse("c2").beats().getFirst().step())
                    .isEqualTo(Tunes.parse("c4").beats().getFirst().step());
            assertThat(Tunes.parse("c3").beats().getFirst().step())
                    .isNotEqualTo(Tunes.parse("c2").beats().getFirst().step());
        }

        @Test
        void readsARunOfNotesWithNothingBetweenThem() {
            assertThat(Tunes.parse("c2e2g2").beats()).hasSize(3);
        }

        @Test
        void skipsAnythingItDoesNotUnderstand() {
            assertThat(Tunes.parse("c2!!e2").beats()).hasSize(2);
        }

        @Test
        void comesToNothingWhenThereIsNoTuneInIt() {
            assertThat(Tunes.parse("hello").isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("choosing instruments")
    class ChoosingInstruments {

        @Test
        void startsOnAHarpWhenTheTuneNamesNoInstrument() {
            assertThat(Tunes.parse("c2").beats().getFirst().instrument())
                    .isEqualTo(NoteInstrument.HARP);
        }

        @ParameterizedTest(name = "instrument {0}")
        @CsvSource({
            "0, HARP",
            "1, BASS",
            "2, SNARE",
            "3, HAT",
            "4, BASEDRUM",
            "5, GUITAR",
            "6, BELL",
            "7, CHIME",
            "8, FLUTE",
            "9, XYLOPHONE",
            "10, PLING"
        })
        void keepsTheNumberingTheNotationHasAlwaysHad(String number, NoteInstrument expected) {
            assertThat(Tunes.parse(number + "c2").beats().getFirst().instrument()).isEqualTo(expected);
        }

        @Test
        void namesTheVoicesMinecraftHasGainedSince() {
            assertThat(Tunes.parse("11c2").beats().getFirst().instrument())
                    .isEqualTo(NoteInstrument.IRON_XYLOPHONE);
            assertThat(Tunes.parse("16c2").beats().getFirst().instrument())
                    .isEqualTo(NoteInstrument.TRUMPET);
        }

        @Test
        void keepsAnInstrumentUntilAnotherIsNamed() {
            var beats = Tunes.parse("1c2e2" + "6g2").beats();

            assertThat(beats.get(0).instrument()).isEqualTo(NoteInstrument.BASS);
            assertThat(beats.get(1).instrument()).isEqualTo(NoteInstrument.BASS);
            assertThat(beats.get(2).instrument()).isEqualTo(NoteInstrument.BELL);
        }

        @Test
        void ignoresANumberNoInstrumentAnswersTo() {
            assertThat(Tunes.parse("1c2" + "97e2").beats().get(1).instrument())
                    .isEqualTo(NoteInstrument.BASS);
        }
    }

    @Nested
    @DisplayName("keeping time")
    class KeepingTime {

        @Test
        void soundsTheFirstNoteStraightAway() {
            assertThat(Tunes.parse("c2").beats().getFirst().tick()).isZero();
        }

        @Test
        void leavesThreeTicksBetweenNotesWhenTheSignDoesNotSay() {
            assertThat(Tunes.parse("c2e2g2").beats().stream().map(Tunes.Beat::tick))
                    .containsExactly(0L, 3L, 6L);
        }

        @Test
        void playsAtTheSpeedTheSignAsksFor() {
            assertThat(Tunes.parse("5:c2e2g2").beats().stream().map(Tunes.Beat::tick))
                    .containsExactly(0L, 5L, 10L);
        }

        @Test
        void pullsAnImpossibleSpeedBackIntoRange() {
            assertThat(Tunes.parse("99:c2e2").beats().get(1).tick()).isEqualTo(10);
            assertThat(Tunes.parse("0:c2e2").beats().get(1).tick()).isEqualTo(1);
        }

        @Test
        void holdsBackEverythingAfterARest() {
            assertThat(Tunes.parse("c2-5e2g2").beats().stream().map(Tunes.Beat::tick))
                    .containsExactly(0L, 8L, 11L);
        }

        @Test
        void waitsBeforeTheFirstNoteWhenTheRestComesFirst() {
            assertThat(Tunes.parse("-4c2").beats().getFirst().tick()).isEqualTo(4);
        }

        @Test
        void runsUntilTheLastNoteHasSounded() {
            assertThat(Tunes.parse("c2e2g2").lengthInTicks()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("turning a step into a pitch")
    class TurningAStepIntoAPitch {

        @Test
        void playsTheMiddleOfTheScaleAtTheSoundsOwnPitch() {
            assertThat(NoteInstrument.HARP.pitchForStep(12)).isEqualTo(1);
        }

        @Test
        void playsAnOctaveUpAtTwiceTheSpeed() {
            assertThat(NoteInstrument.HARP.pitchForStep(24)).isCloseTo(2, within(0.0001f));
        }

        @Test
        void playsTheBottomOfTheScaleAtHalfTheSpeed() {
            assertThat(NoteInstrument.HARP.pitchForStep(0)).isCloseTo(0.5f, within(0.0001f));
        }
    }
}
