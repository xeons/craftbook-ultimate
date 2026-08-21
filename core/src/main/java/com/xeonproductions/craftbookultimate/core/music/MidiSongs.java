// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.music;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jspecify.annotations.NullMarked;

/**
 * Turning a MIDI file into something note blocks can play.
 *
 * <p>Only the file is read; nothing here asks the machine to make a sound, so this works the same
 * on a server with no sound hardware as on a desktop.
 *
 * <p>Three things have to be reconciled. A MIDI file counts time in its own ticks against a tempo
 * that may change part way through, and the server counts in twentieths of a second. A MIDI file
 * names instruments from a list of a hundred and twenty-eight, and a note block has a couple of
 * dozen voices. And a MIDI file may put a note anywhere across ten octaves, where one note block
 * voice covers two.
 *
 * <p>The last of those is the one worth solving properly. The voices do not all sit in the same
 * two octaves — a bass is two octaves below a harp and a bell two above — so a note too high for
 * the voice the music asked for is given to whichever voice can reach it rather than being folded
 * into the wrong octave. Between the lowest and the highest that is about six octaves, which is
 * enough for most music to come out at the pitch it was written at.
 *
 * <p>Drums are read from the channel MIDI reserves for them and played on the note block's own
 * drums. The voices that imitate mobs are never chosen here: they are a novelty rather than an
 * instrument, and a skeleton rattling through a melody line is worse than no melody line.
 */
@NullMarked
public final class MidiSongs {

    /** Microseconds in a server tick. */
    private static final long MICROSECONDS_PER_TICK = 50_000;

    /** Microseconds in a second. */
    private static final long MICROSECONDS_PER_SECOND = 1_000_000;

    /** What a MIDI file means by a tempo before it says otherwise: a hundred and twenty a minute. */
    private static final long DEFAULT_TEMPO = 500_000;

    /** The channel MIDI reserves for drums, counted from zero. */
    private static final int DRUM_CHANNEL = 9;

    /** How many channels a MIDI file has. */
    private static final int CHANNELS = 16;

    /** The meta message that changes the tempo. */
    private static final int SET_TEMPO = 0x51;

    /** How many bytes that message carries. */
    private static final int TEMPO_BYTES = 3;

    /** The loudest a note can be asked for. */
    private static final float FULL_VOLUME = 1;

    /** The highest velocity MIDI can express. */
    private static final float MAX_VELOCITY = 127;

    /** How many notes a song may hold, so one enormous file cannot fill memory. */
    private static final int MAX_NOTES = 30_000;

    /**
     * How many notes may sound on the same tick.
     *
     * <p>A dense chord in a MIDI file can be a couple of dozen notes at once, which is more sounds
     * than anybody can hear apart and more work than a tick should do. The ones over the limit are
     * dropped rather than played.
     */
    private static final int MAX_NOTES_PER_TICK = 8;

    /**
     * The voice each family of the General MIDI instrument list is played in.
     *
     * <p>One entry per family of eight, which is how that list is arranged: pianos, then tuned
     * percussion, organs, guitars, basses, strings, and so on to the sound effects at the end.
     */
    private static final NoteInstrument[] BY_FAMILY = {
        NoteInstrument.HARP,            // Piano
        NoteInstrument.IRON_XYLOPHONE,  // Chromatic percussion
        NoteInstrument.HARP,            // Organ
        NoteInstrument.GUITAR,          // Guitar
        NoteInstrument.BASS,            // Bass
        NoteInstrument.HARP,            // Strings
        NoteInstrument.HARP,            // Ensemble
        NoteInstrument.TRUMPET,         // Brass
        NoteInstrument.FLUTE,           // Reed
        NoteInstrument.FLUTE,           // Pipe
        NoteInstrument.BIT,             // Synth lead
        NoteInstrument.HARP,            // Synth pad
        NoteInstrument.BIT,             // Synth effects
        NoteInstrument.BANJO,           // Ethnic
        NoteInstrument.IRON_XYLOPHONE,  // Percussive
        NoteInstrument.HARP             // Sound effects
    };

    /** How many programs are in each of those families. */
    private static final int FAMILY_SIZE = 8;

    private MidiSongs() {}

    /**
     * Reads a MIDI file.
     *
     * @param name what to call the song
     * @param midi the file's bytes
     * @return the song, or {@link Song#SILENCE} if the bytes are not a MIDI file or hold no notes
     */
    public static Song read(String name, byte[] midi) {
        Sequence sequence;
        try {
            sequence = MidiSystem.getSequence(new ByteArrayInputStream(midi));
        } catch (InvalidMidiDataException | IOException e) {
            return Song.SILENCE;
        }
        return read(name, sequence);
    }

    /** Reads a sequence that has already been parsed. */
    static Song read(String name, Sequence sequence) {
        List<MidiEvent> events = inTimeOrder(sequence);
        if (events.isEmpty()) {
            return Song.SILENCE;
        }

        int[] program = new int[CHANNELS];
        List<Song.Note> notes = new ArrayList<>();

        // A file counting in divisions of a beat measures against the tempo, which may change as
        // it goes; one counting in divisions of a second measures against the second itself.
        long perTick = sequence.getDivisionType() == Sequence.PPQ ? DEFAULT_TEMPO : MICROSECONDS_PER_SECOND;
        long ticksPer = ticksPerBeat(sequence);
        long micros = 0;
        long lastMidiTick = 0;
        long crowdedTick = -1;
        int onThatTick = 0;

        for (MidiEvent event : events) {
            // Multiplied out before dividing. A tick is rarely a whole number of microseconds, and
            // rounding each one on its own loses a fraction per tick that adds up to whole
            // seconds over a song.
            micros += (event.getTick() - lastMidiTick) * perTick / ticksPer;
            lastMidiTick = event.getTick();
            MidiMessage message = event.getMessage();

            if (message instanceof MetaMessage meta && meta.getType() == SET_TEMPO) {
                if (sequence.getDivisionType() == Sequence.PPQ) {
                    perTick = tempoOf(meta);
                }
                continue;
            }
            if (!(message instanceof ShortMessage note)) {
                continue;
            }

            if (note.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                program[note.getChannel()] = note.getData1();
                continue;
            }
            if (note.getCommand() != ShortMessage.NOTE_ON || note.getData2() == 0) {
                continue;
            }

            long tick = micros / MICROSECONDS_PER_TICK;
            if (tick != crowdedTick) {
                crowdedTick = tick;
                onThatTick = 0;
            }
            if (onThatTick >= MAX_NOTES_PER_TICK) {
                continue;
            }
            onThatTick++;

            NoteInstrument voice = voiceFor(note.getChannel(), program[note.getChannel()], note.getData1());
            notes.add(new Song.Note(
                    tick,
                    voice,
                    voice.pitchFor(note.getData1()),
                    Math.min(FULL_VOLUME, note.getData2() / MAX_VELOCITY)));

            if (notes.size() >= MAX_NOTES) {
                break;
            }
        }

        if (notes.isEmpty()) {
            return Song.SILENCE;
        }
        return new Song(name, notes, notes.getLast().tick() + 1);
    }

    /** Every event in every track, in the order they happen. */
    private static List<MidiEvent> inTimeOrder(Sequence sequence) {
        List<MidiEvent> events = new ArrayList<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                events.add(track.get(i));
            }
        }
        events.sort(Comparator.comparingLong(MidiEvent::getTick));
        return events;
    }

    /**
     * How many of the file's own ticks go into whatever it measures time against.
     *
     * <p>A file counts either in divisions of a beat, where the tempo says how long a beat is, or
     * in divisions of a second, where it says so itself and the tempo does not come into it. Either
     * way this is the number the tempo is divided by.
     */
    private static long ticksPerBeat(Sequence sequence) {
        int resolution = Math.max(1, sequence.getResolution());
        if (sequence.getDivisionType() == Sequence.PPQ) {
            return resolution;
        }
        return Math.max(1, (long) (sequence.getDivisionType() * resolution));
    }

    /** The tempo a message carries, in microseconds to the quarter note. */
    private static long tempoOf(MetaMessage meta) {
        byte[] data = meta.getData();
        if (data.length < TEMPO_BYTES) {
            return DEFAULT_TEMPO;
        }
        return ((long) (data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
    }

    /**
     * The voice a note is played in.
     *
     * <p>Drums come off the channel MIDI keeps for them and are chosen by which drum they are.
     * Everything else takes the voice its instrument family maps to, moved to whichever voice can
     * actually reach the note.
     */
    private static NoteInstrument voiceFor(int channel, int program, int midiNote) {
        if (channel == DRUM_CHANNEL) {
            return Drums.forNote(midiNote);
        }
        NoteInstrument family = BY_FAMILY[Math.clamp(program / FAMILY_SIZE, 0, BY_FAMILY.length - 1)];
        return NoteInstrument.reaching(family, midiNote);
    }

    /** Which of a note block's three drums stands in for each of MIDI's. */
    private static final class Drums {

        private Drums() {}

        static NoteInstrument forNote(int midiNote) {
            return switch (midiNote) {
                case 35, 36, 41, 43, 45, 47, 48, 50 -> NoteInstrument.BASEDRUM;
                case 37, 38, 39, 40 -> NoteInstrument.SNARE;
                default -> NoteInstrument.HAT;
            };
        }
    }
}
