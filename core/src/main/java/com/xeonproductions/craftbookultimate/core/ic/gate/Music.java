package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.music.NoteInstrument;
import com.xeonproductions.craftbookultimate.core.music.Tunes;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that make a noise.
 *
 * <p>Three of them, in rising order of how much they are told. One plays a single sound wherever
 * it is pointed. One plays a record through a jukebox and stops it again when it is switched off.
 * One plays a tune written out on the sign itself, note by note, through a note block.
 *
 * <p>The two that need a block to speak through look for it in the six blocks around the one the
 * sign hangs on, and do nothing at all if it is not there — so taking the note block away silences
 * the chip rather than leaving it playing into thin air.
 */
@NullMarked
public final class Music {

    /** The line naming what to play. */
    private static final int SUBJECT_LINE = 2;

    /** The line carrying where to play it, or the rest of what to play. */
    private static final int DETAIL_LINE = 3;

    /** How loud everything here plays. */
    private static final float FULL_VOLUME = 1;

    /** The pitch a sound plays at when nothing has asked for another. */
    private static final float NATURAL_PITCH = 1;

    /** Where a sound effect plays when its sign does not say. */
    private static final Vec3i DEFAULT_SOUND_OFFSET = new Vec3i(0, 1, 0);

    /** How far to either side a sound effect may be moved. */
    private static final int MAX_SIDEWAYS_OFFSET = 100;

    /** How far up or down it may be moved. */
    private static final int MAX_VERTICAL_OFFSET = 256;

    /** Separates the parts of an offset, and a tune's speed from the tune. */
    private static final String FIELD_SEPARATOR = ":";

    /** What every record's sound is called, before the record's own name. */
    private static final String RECORD_PREFIX = "music_disc.";

    /** The block a record is played through. */
    private static final Key JUKEBOX = Blocks.key("jukebox");

    /** The block a tune is played through. */
    private static final Key NOTE_BLOCK = Blocks.key("note_block");

    /** The six blocks touching the one the sign hangs on. */
    private static final List<Vec3i> AROUND = List.of(
            new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, 0),
            new Vec3i(0, 1, 0),
            new Vec3i(0, -1, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, -1));

    private Music() {}

    /**
     * Plays one sound.
     *
     * <p>Line 3 names it, either in full as {@code entity.creeper.primed} or by the shorthand of
     * the first two letters of each part of that name, {@code ENCRPR}. Line 4 is an
     * {@code x:y:z} offset from the sign to where it should come from, and defaults to the block
     * above.
     */
    public static ICLogic soundEffect() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            Optional<Key> sound = state.world().resolveSound(state.sign().trimmedText(SUBJECT_LINE));
            if (sound.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            Vec3i where = state.signPosition().add(soundOffsetOn(state));
            state.setMainOutput(
                    state.world().playSound(Vec3d.middleOf(where), sound.get(), FULL_VOLUME, NATURAL_PITCH));
        };
    }

    /**
     * Plays a record through a jukebox.
     *
     * <p>Line 3 is the record's name as the game calls it, {@code 13}, {@code mellohi},
     * {@code pigstep} and so on. A jukebox has to be touching the block the sign hangs on, and the
     * music comes from the jukebox.
     *
     * <p>Switching the chip off stops the record, which is what a jukebox does when its disc is
     * taken out, so a record is never left playing with nothing driving it.
     */
    public static ICLogic jukebox() {
        return state -> {
            Optional<Vec3i> jukebox = blockAround(state, JUKEBOX);
            Optional<Key> record = recordOn(state);
            if (jukebox.isEmpty() || record.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            if (state.isAnyInputActive()) {
                state.world()
                        .playSound(
                                Vec3d.middleOf(jukebox.get()), record.get(), FULL_VOLUME, NATURAL_PITCH);
                state.setMainOutput(true);
            } else {
                state.world().stopSound(record.get());
                state.setMainOutput(false);
            }
        };
    }

    /**
     * Plays a tune written on the sign, through a note block.
     *
     * <p>Line 3 is the tune, optionally with how many ticks to leave between notes in front of it
     * and a colon after that, so {@code 3:0c2e2g2} plays three notes three ticks apart. Line 4 is
     * more of the same tune, run on from line 3.
     *
     * <p>A tune is a run of notes, instrument changes and rests, written without separators:
     *
     * <pre>
     *   c2 C2 d2    a note; the letter is the note and a capital makes it sharp
     *   0 5 11      an instrument, by number, applying to everything after it
     *   -4          a rest, in ticks
     * </pre>
     *
     * <p>The digit after a note picks the octave by whether it is odd or even rather than by what
     * it is, so {@code c2} and {@code c4} are the same note and {@code c3} is the octave below.
     * That is how tunes in the world are written and how they sound.
     *
     * <p>A note block has to be touching the block the sign hangs on, and the tune comes from the
     * note block. The output stays high until the last note has sounded.
     */
    public static ICLogic tune() {
        return new TuneChip();
    }

    /** Whichever of the blocks touching the sign's support is of a kind, if any is. */
    private static Optional<Vec3i> blockAround(ChipState state, Key kind) {
        for (Vec3i offset : AROUND) {
            Vec3i candidate = state.backPosition().add(offset);
            if (state.world().blockAt(candidate).equals(kind)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** The record a jukebox sign names. */
    private static Optional<Key> recordOn(ChipState state) {
        String written = state.sign().trimmedText(SUBJECT_LINE).toLowerCase(Locale.ROOT);
        return written.isEmpty()
                ? Optional.empty()
                : state.world().resolveSound(RECORD_PREFIX + written);
    }

    /** Where a sound effect should come from, relative to the sign. */
    private static Vec3i soundOffsetOn(ChipState state) {
        String[] parts = state.sign().trimmedText(DETAIL_LINE).split(FIELD_SEPARATOR);
        if (parts.length != 3) {
            return DEFAULT_SOUND_OFFSET;
        }
        try {
            return new Vec3i(
                    Math.clamp(Integer.parseInt(parts[0].trim()), -MAX_SIDEWAYS_OFFSET, MAX_SIDEWAYS_OFFSET),
                    Math.clamp(Integer.parseInt(parts[1].trim()), -MAX_VERTICAL_OFFSET, MAX_VERTICAL_OFFSET),
                    Math.clamp(Integer.parseInt(parts[2].trim()), -MAX_SIDEWAYS_OFFSET, MAX_SIDEWAYS_OFFSET));
        } catch (NumberFormatException e) {
            return DEFAULT_SOUND_OFFSET;
        }
    }

    /** Plays a written tune one note at a time, and holds its output high until it is over. */
    private static final class TuneChip implements ICLogic {

        /** The tick the tune currently playing finishes on, or -1 when nothing is playing. */
        private long endsAt = -1;

        @Override
        public void trigger(ChipState state) {
            if (!state.isAnyInputActive()) {
                return;
            }

            Optional<Vec3i> noteBlock = blockAround(state, NOTE_BLOCK);
            if (noteBlock.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            Tunes.Tune tune = Tunes.parse(
                    state.sign().trimmedText(SUBJECT_LINE) + state.sign().trimmedText(DETAIL_LINE));
            if (tune.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            Vec3d at = Vec3d.middleOf(noteBlock.get());
            for (Tunes.Beat beat : tune.beats()) {
                NoteInstrument voice = beat.instrument();
                state.scheduler()
                        .runLater(
                                () -> state.world()
                                        .playSound(at, voice.sound(), FULL_VOLUME, voice.pitchForStep(beat.step())),
                                beat.tick());
            }

            state.setMainOutput(true);
            endsAt = tune.lengthInTicks();
            state.scheduler().runLater(() -> state.setMainOutput(false), endsAt);
        }
    }
}
