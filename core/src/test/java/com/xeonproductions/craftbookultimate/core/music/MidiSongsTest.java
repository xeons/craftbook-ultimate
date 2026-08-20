package com.xeonproductions.craftbookultimate.core.music;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Turning a MIDI file into something note blocks can play")
class MidiSongsTest {

    /** Ticks per quarter note in every sequence built here. */
    private static final int RESOLUTION = 24;

    /** A quarter note at a hundred and twenty a minute, in microseconds. */
    private static final int QUARTER_NOTE = 500_000;

    /** Middle C, as MIDI counts notes. */
    private static final int MIDDLE_C = 60;

    /** A comfortable velocity. */
    private static final int MEZZO = 100;

    /** Builds a sequence with one track, for a test to add events to. */
    private static Sequence sequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, RESOLUTION);
        sequence.createTrack();
        return sequence;
    }

    /** Adds a note starting at a MIDI tick. */
    private static void note(Sequence sequence, long tick, int channel, int midiNote, int velocity)
            throws InvalidMidiDataException {
        Track track = sequence.getTracks()[0];
        track.add(new MidiEvent(
                new ShortMessage(ShortMessage.NOTE_ON, channel, midiNote, velocity), tick));
    }

    /** Points a channel at a General MIDI program. */
    private static void program(Sequence sequence, long tick, int channel, int program)
            throws InvalidMidiDataException {
        Track track = sequence.getTracks()[0];
        track.add(new MidiEvent(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0), tick));
    }

    /** Sets the tempo, in microseconds to the quarter note. */
    private static void tempo(Sequence sequence, long tick, int microsPerQuarter)
            throws InvalidMidiDataException {
        byte[] data = {
            (byte) (microsPerQuarter >> 16), (byte) (microsPerQuarter >> 8), (byte) microsPerQuarter
        };
        sequence.getTracks()[0].add(new MidiEvent(new MetaMessage(0x51, data, data.length), tick));
    }

    private static Song read(Sequence sequence) {
        return MidiSongs.read("test", sequence);
    }

    @Nested
    @DisplayName("reading notes")
    class ReadingNotes {

        @Test
        void readsANoteOutOfTheFile() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, MEZZO);

            assertThat(read(sequence).notes()).hasSize(1);
        }

        @Test
        void leavesOutTheEndOfANote() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, MEZZO);
            note(sequence, RESOLUTION, 0, MIDDLE_C, 0);

            assertThat(read(sequence).notes()).hasSize(1);
        }

        @Test
        void takesItsVolumeFromHowHardTheNoteWasStruck() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, 127);

            assertThat(read(sequence).notes().getFirst().volume()).isEqualTo(1);
        }

        @Test
        void comesToNothingForBytesThatAreNotAMidiFile() {
            assertThat(MidiSongs.read("test", new byte[] {1, 2, 3}).isEmpty()).isTrue();
        }

        @Test
        void comesToNothingForAFileWithNoNotesInIt() throws Exception {
            assertThat(read(sequence()).isEmpty()).isTrue();
        }

        @Test
        void readsAFileWrittenOutAndBackIn() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, MEZZO);

            assertThat(MidiSongs.read("test", written(sequence)).notes()).hasSize(1);
        }

        private byte[] written(Sequence sequence) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MidiSystem.write(sequence, MidiSystem.getMidiFileTypes(sequence)[0], out);
            return out.toByteArray();
        }
    }

    @Nested
    @DisplayName("keeping time")
    class KeepingTime {

        @Test
        void startsTheFirstNoteAtTheBeginning() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, MEZZO);

            assertThat(read(sequence).notes().getFirst().tick()).isZero();
        }

        @Test
        void turnsAQuarterNoteIntoTenServerTicks() throws Exception {
            // A quarter note at 120 a minute is half a second, which is ten ticks.
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, MEZZO);
            note(sequence, RESOLUTION, 0, MIDDLE_C + 2, MEZZO);

            assertThat(read(sequence).notes().get(1).tick()).isEqualTo(10);
        }

        @Test
        void followsATempoChangePartWayThrough() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, MEZZO);
            tempo(sequence, 0, QUARTER_NOTE / 2);
            note(sequence, RESOLUTION, 0, MIDDLE_C + 2, MEZZO);

            assertThat(read(sequence).notes().get(1).tick()).isEqualTo(5);
        }

        @Test
        void runsUntilTheLastNoteHasSounded() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 0, MIDDLE_C, MEZZO);
            note(sequence, RESOLUTION, 0, MIDDLE_C, MEZZO);

            assertThat(read(sequence).lengthInTicks()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("choosing a voice")
    class ChoosingAVoice {

        @Test
        void playsAPianoOnAHarp() throws Exception {
            Sequence sequence = sequence();
            program(sequence, 0, 0, 0);
            note(sequence, 0, 0, MIDDLE_C, MEZZO);

            assertThat(read(sequence).notes().getFirst().instrument()).isEqualTo(NoteInstrument.HARP);
        }

        @Test
        void playsBrassOnATrumpet() throws Exception {
            Sequence sequence = sequence();
            program(sequence, 0, 0, 56);
            note(sequence, 0, 0, MIDDLE_C, MEZZO);

            assertThat(read(sequence).notes().getFirst().instrument())
                    .isEqualTo(NoteInstrument.TRUMPET);
        }

        @Test
        void movesToAVoiceThatCanReachAHighNote() throws Exception {
            Sequence sequence = sequence();
            program(sequence, 0, 0, 0);
            note(sequence, 0, 0, 96, MEZZO);

            NoteInstrument chosen = read(sequence).notes().getFirst().instrument();

            assertThat(chosen).isNotEqualTo(NoteInstrument.HARP);
            assertThat(chosen.covers(96)).isTrue();
        }

        @Test
        void movesToAVoiceThatCanReachALowNote() throws Exception {
            Sequence sequence = sequence();
            program(sequence, 0, 0, 0);
            note(sequence, 0, 0, 36, MEZZO);

            NoteInstrument chosen = read(sequence).notes().getFirst().instrument();

            assertThat(chosen.covers(36)).isTrue();
        }

        @Test
        void keepsTheVoiceItWasAskedForWhereItCanReach() throws Exception {
            Sequence sequence = sequence();
            program(sequence, 0, 0, 32);
            note(sequence, 0, 0, 40, MEZZO);

            assertThat(read(sequence).notes().getFirst().instrument()).isEqualTo(NoteInstrument.BASS);
        }

        @Test
        void playsTheSameNoteAtTheSamePitchWhicheverVoiceReachesIt() throws Exception {
            Sequence sequence = sequence();
            program(sequence, 0, 0, 0);
            note(sequence, 0, 0, 78, MEZZO);
            note(sequence, 0, 0, 79, MEZZO);

            Song song = read(sequence);
            NoteInstrument low = song.notes().get(0).instrument();
            NoteInstrument high = song.notes().get(1).instrument();

            // 78 is the top of a harp and 79 is one semitone above it, so a different voice takes
            // it. A semitone up should still sound a semitone up.
            double step = Math.pow(2, 1 / 12.0);
            assertThat(high.pitchFor(79) * ratioBetween(high, low))
                    .isCloseTo((float) (low.pitchFor(78) * step), within(0.01));
        }

        /** How the two voices' windows relate, as a multiplier on pitch. */
        private double ratioBetween(NoteInstrument higher, NoteInstrument lower) {
            return Math.pow(2, (higher.lowestNote() - lower.lowestNote()) / 12.0);
        }

        @Test
        void neverUsesAMobsVoiceForMusic() throws Exception {
            Sequence sequence = sequence();
            for (int program = 0; program < 128; program += 8) {
                program(sequence, 0, 0, program);
                note(sequence, program, 0, MIDDLE_C, MEZZO);
            }

            assertThat(read(sequence).notes())
                    .allSatisfy(note -> assertThat(note.instrument().isPitched()).isTrue());
        }

        @Test
        void playsDrumsOnTheNoteBlocksOwnDrums() throws Exception {
            Sequence sequence = sequence();
            note(sequence, 0, 9, 36, MEZZO);
            note(sequence, 1, 9, 38, MEZZO);
            note(sequence, 2, 9, 42, MEZZO);

            assertThat(read(sequence).notes().stream().map(Song.Note::instrument))
                    .containsExactly(
                            NoteInstrument.BASEDRUM, NoteInstrument.SNARE, NoteInstrument.HAT);
        }
    }

    @Nested
    @DisplayName("keeping a file from running away with the server")
    class KeepingAFileFromRunningAway {

        @Test
        void dropsTheNotesOverTheLimitOnACrowdedTick() throws Exception {
            Sequence sequence = sequence();
            for (int i = 0; i < 40; i++) {
                note(sequence, 0, 0, MIDDLE_C + (i % 12), MEZZO);
            }

            assertThat(read(sequence).notes()).hasSizeLessThanOrEqualTo(8);
        }
    }
}
