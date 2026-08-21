// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.music;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * A piece of music worked out in advance, ready to be played a tick at a time.
 *
 * <p>Every decision has already been made by the time a song exists: which voice each note is
 * played in, how fast to play the sound to reach its pitch, and how loud. Playing one is a matter
 * of walking the notes in order and handing each to the world, which is what makes it cheap enough
 * to do on a ticking thread.
 *
 * <p>Notes are in the order they sound, and several may share a tick.
 */
@NullMarked
public record Song(String name, List<Note> notes, long lengthInTicks) {

    /** A song with nothing in it, which is what an unreadable file comes to. */
    public static final Song SILENCE = new Song("", List.of(), 0);

    public Song {
        notes = List.copyOf(notes);
    }

    /**
     * One note.
     *
     * @param tick how long after the song starts, in server ticks
     * @param instrument the voice it is played in
     * @param pitch how fast to play that voice's sound
     * @param volume how loud, from nothing to one
     */
    public record Note(long tick, NoteInstrument instrument, float pitch, float volume) {}

    /** Whether there is anything to play. */
    public boolean isEmpty() {
        return notes.isEmpty();
    }

    /** How many notes there are. */
    public int noteCount() {
        return notes.size();
    }
}
