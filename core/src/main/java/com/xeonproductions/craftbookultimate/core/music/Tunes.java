// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.music;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * A tune written out on a sign.
 *
 * <p>The notation is frozen: signs in the world are written in it. A tune is a run of notes,
 * instrument changes and rests jammed together without separators, and what each looks like is
 * what tells them apart.
 *
 * <pre>
 *   c2 C2 d2    a note: the letter names it, a capital makes it sharp, the digit picks the octave
 *   0 5 11      an instrument, by number, from here to the end of the tune or the next number
 *   -4          a rest, in ticks, holding back everything after it
 * </pre>
 *
 * <p>The digit after a note picks the octave by whether it is odd or even rather than by what it
 * is, so {@code c2} and {@code c4} are the same note and {@code c3} is the octave below it. That
 * reads backwards against the note table it came from, and it is what every tune in the world was
 * written against, so it is what the notation means.
 *
 * <p>{@code F1} on its own is the one note below all of that, the bottom of a note block's range.
 *
 * <p>Instruments 0 to 10 are the ones the notation has always had, in the order it had them.
 * Everything from 11 up is a voice Minecraft has gained since, in the order the game lists them.
 */
@NullMarked
public final class Tunes {

    /** How many ticks are left between notes when the sign does not say. */
    public static final int DEFAULT_RATE = 3;

    /** The fastest a tune may be asked to play. */
    private static final int MIN_RATE = 1;

    /** The slowest. */
    private static final int MAX_RATE = 10;

    /** Separates the speed from the tune. */
    private static final char RATE_SEPARATOR = ':';

    /** A note, an instrument number, or a rest. */
    private static final Pattern TOKEN = Pattern.compile("(\\d{1,2})|([a-gA-G]\\d)|(-\\d)");

    /**
     * The note letters in the order the notation numbers them, lowest first.
     *
     * <p>Twelve semitones: a lower-case letter is the natural and an upper-case one the sharp
     * above it, which is why {@code b} and {@code e} have no upper-case partner.
     */
    private static final String SEMITONES = "gGaAbcCdDefF";

    /** The step the bottom note sits on. */
    private static final int BOTTOM_STEP = 0;

    /** What the bottom note is written as, and the only note whose digit means what it says. */
    private static final String BOTTOM_NOTE = "F1";

    /** How far up the scale the upper octave starts. */
    private static final int UPPER_OCTAVE = 12;

    /**
     * The instruments by the number a tune calls them.
     *
     * <p>The first eleven are frozen; a sign written years ago says {@code 4} for a bass drum and
     * has to go on meaning that. The rest are appended, so a tune may now name any voice a note
     * block has.
     */
    private static final List<NoteInstrument> BY_NUMBER = List.of(
            NoteInstrument.HARP,
            NoteInstrument.BASS,
            NoteInstrument.SNARE,
            NoteInstrument.HAT,
            NoteInstrument.BASEDRUM,
            NoteInstrument.GUITAR,
            NoteInstrument.BELL,
            NoteInstrument.CHIME,
            NoteInstrument.FLUTE,
            NoteInstrument.XYLOPHONE,
            NoteInstrument.PLING,
            NoteInstrument.IRON_XYLOPHONE,
            NoteInstrument.COW_BELL,
            NoteInstrument.DIDGERIDOO,
            NoteInstrument.BIT,
            NoteInstrument.BANJO,
            NoteInstrument.TRUMPET,
            NoteInstrument.ZOMBIE,
            NoteInstrument.SKELETON,
            NoteInstrument.CREEPER,
            NoteInstrument.DRAGON,
            NoteInstrument.WITHER_SKELETON,
            NoteInstrument.PIGLIN);

    private Tunes() {}

    /**
     * One note, and when it sounds.
     *
     * @param tick how long after the tune starts, in ticks
     * @param instrument the voice it is played in
     * @param step where it sits on that voice's scale, from 0 to 24
     */
    public record Beat(long tick, NoteInstrument instrument, int step) {}

    /**
     * A tune ready to play.
     *
     * @param beats the notes, in the order they sound
     * @param lengthInTicks how long the whole thing takes
     */
    public record Tune(List<Beat> beats, long lengthInTicks) {

        /** An empty tune, which is what unreadable notation comes to. */
        public static final Tune NOTHING = new Tune(List.of(), 0);

        public Tune {
            beats = List.copyOf(beats);
        }

        /** Whether there is anything to play. */
        public boolean isEmpty() {
            return beats.isEmpty();
        }
    }

    /**
     * Reads a tune, and the speed written in front of it.
     *
     * <p>A number and a colon at the start set how many ticks to leave between notes, from one to
     * ten; without one the tune plays at three. Anything in the notation that is not a note, an
     * instrument or a rest is skipped, so a tune with a stray character in it still plays.
     *
     * @param written the notation, as the sign carries it
     */
    public static Tune parse(String written) {
        int rate = DEFAULT_RATE;
        String notation = written;

        int separator = written.indexOf(RATE_SEPARATOR);
        if (separator > 0) {
            try {
                rate = Math.clamp(Integer.parseInt(written.substring(0, separator).trim()), MIN_RATE, MAX_RATE);
                notation = written.substring(separator + 1);
            } catch (NumberFormatException e) {
                notation = written;
            }
        }

        return read(notation, rate);
    }

    /** Reads notation at a known speed. */
    private static Tune read(String notation, int rate) {
        List<Beat> beats = new ArrayList<>();
        NoteInstrument voice = NoteInstrument.HARP;
        long tick = 0;
        long resting = 0;
        boolean started = false;

        Matcher token = TOKEN.matcher(notation);
        while (token.find()) {
            String text = token.group();

            if (text.charAt(0) == '-') {
                resting += Integer.parseInt(text.substring(1));
                continue;
            }
            if (Character.isDigit(text.charAt(0))) {
                int number = Integer.parseInt(text);
                if (number < BY_NUMBER.size()) {
                    voice = BY_NUMBER.get(number);
                }
                continue;
            }

            int step = stepOf(text);
            if (step < 0) {
                continue;
            }

            tick = started ? tick + rate + resting : resting;
            resting = 0;
            started = true;
            beats.add(new Beat(tick, voice, step));
        }

        return beats.isEmpty() ? Tune.NOTHING : new Tune(beats, tick + 1);
    }

    /**
     * Where a written note sits on a note block's scale, or -1 if it names none.
     *
     * <p>The octave comes from whether the digit is odd or even, not from the digit itself.
     */
    private static int stepOf(String written) {
        if (written.equals(BOTTOM_NOTE)) {
            return BOTTOM_STEP;
        }

        int semitone = SEMITONES.indexOf(written.charAt(0));
        if (semitone < 0) {
            return -1;
        }

        boolean upper = (written.charAt(1) - '0') % 2 == 0;
        return semitone + 1 + (upper ? UPPER_OCTAVE : 0);
    }
}
