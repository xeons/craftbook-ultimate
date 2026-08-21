// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.music;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The voices a note block can speak in.
 *
 * <p>Each one covers two octaves and no more, but they do not all cover the same two. A bass sits
 * two octaves below a harp and a bell sits two above, so between the lowest and the highest there
 * are six octaves of usable range — far more than any single instrument, and the reason the chips
 * that play written music choose an instrument partly by how high the note is.
 *
 * <p>Notes are counted the way MIDI counts them, where 60 is middle C. A harp covers 54 to 78,
 * which is the range a note block has always had; every other instrument is that window moved by
 * whole octaves.
 *
 * <p>The three percussion voices are not really pitched. They are given the harp's window so that
 * a written note still picks something, and they are left out of the automatic choosing, because
 * a drum standing in for a melody note is worse than a melody note in the wrong octave.
 */
@NullMarked
public enum NoteInstrument {

    /** The default, and what a note block plays standing on anything unremarkable. */
    HARP("harp", 54, true),

    /** Two octaves below the harp. */
    BASS("bass", 30, true),

    /** Unpitched. */
    SNARE("snare", 54, false),

    /** Unpitched. */
    HAT("hat", 54, false),

    /** Unpitched. */
    BASEDRUM("basedrum", 54, false),

    /** One octave below the harp. */
    GUITAR("guitar", 42, true),

    /** Two octaves above the harp. */
    BELL("bell", 78, true),

    /** Two octaves above the harp. */
    CHIME("chime", 78, true),

    /** One octave above the harp. */
    FLUTE("flute", 66, true),

    /** Two octaves above the harp. */
    XYLOPHONE("xylophone", 78, true),

    /** The harp's window, with a sharper attack. */
    PLING("pling", 54, true),

    /** The harp's window. */
    IRON_XYLOPHONE("iron_xylophone", 54, true),

    /** One octave above the harp. */
    COW_BELL("cow_bell", 66, true),

    /** Two octaves below the harp. */
    DIDGERIDOO("didgeridoo", 30, true),

    /** The harp's window. */
    BIT("bit", 54, true),

    /** The harp's window. */
    BANJO("banjo", 54, true),

    /** The harp's window. */
    TRUMPET("trumpet", 54, true),

    /** A zombie's voice, unpitched in practice. */
    ZOMBIE("imitate_zombie", 54, false),

    /** A skeleton's voice, unpitched in practice. */
    SKELETON("imitate_skeleton", 54, false),

    /** A creeper's voice, unpitched in practice. */
    CREEPER("imitate_creeper", 54, false),

    /** A dragon's voice, unpitched in practice. */
    DRAGON("imitate_ender_dragon", 54, false),

    /** A wither skeleton's voice, unpitched in practice. */
    WITHER_SKELETON("imitate_wither_skeleton", 54, false),

    /** A piglin's voice, unpitched in practice. */
    PIGLIN("imitate_piglin", 54, false);

    /** How many semitones a note block covers, which is two octaves and the note they meet on. */
    public static final int WINDOW = 25;

    /** Where in that window the sound plays at its own recorded pitch. */
    private static final int MIDDLE_OF_WINDOW = 12;

    /** Semitones in an octave. */
    private static final int OCTAVE = 12;

    /** What every note block sound is called, before the instrument's own name. */
    private static final String SOUND_PREFIX = "block.note_block.";

    /**
     * The pitched instruments, lowest window first.
     *
     * <p>The order is what the automatic choosing walks, so a note is given the lowest instrument
     * that can reach it and a melody keeps to one voice for as long as it can.
     */
    private static final List<NoteInstrument> BY_REGISTER = List.of(
            BASS, DIDGERIDOO, GUITAR, HARP, PLING, IRON_XYLOPHONE, BIT, BANJO, TRUMPET,
            FLUTE, COW_BELL, BELL, CHIME, XYLOPHONE);

    private final Key sound;
    private final int lowestNote;
    private final boolean pitched;

    NoteInstrument(String name, int lowestNote, boolean pitched) {
        this.sound = Blocks.key(SOUND_PREFIX + name);
        this.lowestNote = lowestNote;
        this.pitched = pitched;
    }

    /** The sound this instrument plays. */
    public Key sound() {
        return sound;
    }

    /** Whether it carries a tune, rather than being a drum or a shout. */
    public boolean isPitched() {
        return pitched;
    }

    /** The lowest note it can play, counted the way MIDI counts. */
    public int lowestNote() {
        return lowestNote;
    }

    /** The highest note it can play. */
    public int highestNote() {
        return lowestNote + WINDOW - 1;
    }

    /** Whether a note falls inside its window. */
    public boolean covers(int midiNote) {
        return midiNote >= lowestNote && midiNote <= highestNote();
    }

    /**
     * How fast to play the sound so it comes out at a note.
     *
     * <p>Notes outside the window are folded by whole octaves until they are inside, so something
     * far too high still lands on the right note of the scale rather than being dropped.
     */
    public float pitchFor(int midiNote) {
        int note = midiNote;
        while (note < lowestNote) {
            note += OCTAVE;
        }
        while (note > highestNote()) {
            note -= OCTAVE;
        }
        return (float) Math.pow(2, (note - lowestNote - MIDDLE_OF_WINDOW) / (double) OCTAVE);
    }

    /**
     * How fast to play the sound to reach a step of a note block's own scale.
     *
     * <p>The scale a note block is tuned through, from 0 to 24, measured from whatever the
     * instrument's own lowest note is.
     */
    public float pitchForStep(int step) {
        return (float) Math.pow(2, (Math.clamp(step, 0, WINDOW - 1) - MIDDLE_OF_WINDOW) / (double) OCTAVE);
    }

    /**
     * The instrument closest in voice to one already chosen that can reach a note.
     *
     * <p>Used when written music runs off the end of what the instrument it started on can play.
     * Rather than folding the note into the wrong octave, the note is given to whichever voice
     * covers it, so a bass line and a descant can both sound at the pitch they were written at.
     *
     * @param preferred the voice the music asked for
     * @param midiNote the note to reach
     * @return the voice to play it with, which is the preferred one wherever it can reach
     */
    public static NoteInstrument reaching(NoteInstrument preferred, int midiNote) {
        if (!preferred.isPitched() || preferred.covers(midiNote)) {
            return preferred;
        }
        for (NoteInstrument candidate : BY_REGISTER) {
            if (candidate.covers(midiNote)) {
                return candidate;
            }
        }
        return preferred;
    }

    /** The pitched instruments, lowest window first. */
    public static List<NoteInstrument> byRegister() {
        return BY_REGISTER;
    }

    /** The lowest note any instrument can reach. */
    public static int lowestReachable() {
        return BY_REGISTER.getFirst().lowestNote();
    }

    /** The highest note any instrument can reach. */
    public static int highestReachable() {
        int highest = 0;
        for (NoteInstrument instrument : BY_REGISTER) {
            highest = Math.max(highest, instrument.highestNote());
        }
        return highest;
    }

    /** An instrument by the name a sign or a playlist calls it, in any case. */
    public static Optional<NoteInstrument> named(String written) {
        String wanted = written.trim().toUpperCase(Locale.ROOT);
        for (NoteInstrument instrument : values()) {
            if (instrument.name().equals(wanted)) {
                return Optional.of(instrument);
            }
        }
        return Optional.empty();
    }
}
