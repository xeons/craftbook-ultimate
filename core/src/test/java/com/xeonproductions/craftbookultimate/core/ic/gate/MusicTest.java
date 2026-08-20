package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.music.NoteInstrument;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The chips that make a noise")
class MusicTest {

    /** Where every chip in these tests hangs its sign. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    /** The block above the one the sign hangs on, where a note block or jukebox is put. */
    private static final Vec3i ABOVE_SUPPORT = new Vec3i(0, 65, -1);

    private final SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState.Builder chip(PinLayout layout, String... lines) {
        return SimpleChipState.forLayout(layout)
                .world(world)
                .at(SIGN, BlockFace.SOUTH)
                .sign(lines);
    }

    @Nested
    @DisplayName("playing one sound")
    class PlayingOneSound {

        @Test
        void playsWhatTheSignNames() {
            SimpleChipState state = chip(PinLayout.THREE_I_SO, "", "[MCX251]", "entity.creeper.primed", "")
                    .inputs(true, false, false)
                    .build();

            Music.soundEffect().trigger(state);

            assertThat(world.sounds()).hasSize(1);
            assertThat(world.sounds().getFirst().sound()).isEqualTo(Blocks.key("entity.creeper.primed"));
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void playsItAboveTheSignWhenNoOffsetIsGiven() {
            SimpleChipState state = chip(PinLayout.THREE_I_SO, "", "[MCX251]", "entity.creeper.primed", "")
                    .inputs(true, false, false)
                    .build();

            Music.soundEffect().trigger(state);

            assertThat(world.sounds().getFirst().at()).isEqualTo(Vec3d.middleOf(SIGN.add(0, 1, 0)));
        }

        @Test
        void playsItWhereTheSignSays() {
            SimpleChipState state = chip(PinLayout.THREE_I_SO, "", "[MCX251]", "entity.creeper.primed", "3:0:-2")
                    .inputs(true, false, false)
                    .build();

            Music.soundEffect().trigger(state);

            assertThat(world.sounds().getFirst().at()).isEqualTo(Vec3d.middleOf(SIGN.add(3, 0, -2)));
        }

        @Test
        void playsNothingForASoundThatDoesNotExist() {
            world.knowingOnlySounds("entity.creeper.primed");
            SimpleChipState state = chip(PinLayout.THREE_I_SO, "", "[MCX251]", "notasound", "")
                    .inputs(true, false, false)
                    .build();

            Music.soundEffect().trigger(state);

            assertThat(world.sounds()).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void playsNothingWhileNothingDrivesIt() {
            SimpleChipState state = chip(PinLayout.THREE_I_SO, "", "[MCX251]", "entity.creeper.primed", "")
                    .inputs(false, false, false)
                    .build();

            Music.soundEffect().trigger(state);

            assertThat(world.sounds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("playing a record")
    class PlayingARecord {

        private SimpleChipState.Builder jukeboxChip(String record) {
            world.withBlock(ABOVE_SUPPORT, "jukebox");
            return chip(PinLayout.AISO, "", "[MCU706]", record, "");
        }

        @Test
        void playsTheRecordThroughTheJukebox() {
            SimpleChipState state = jukeboxChip("mellohi").inputs(true, false, false, false).build();

            Music.jukebox().trigger(state);

            assertThat(world.sounds()).hasSize(1);
            assertThat(world.sounds().getFirst().sound()).isEqualTo(Blocks.key("music_disc.mellohi"));
            assertThat(world.sounds().getFirst().at()).isEqualTo(Vec3d.middleOf(ABOVE_SUPPORT));
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void stopsTheRecordWhenNothingDrivesItAnyMore() {
            SimpleChipState state = jukeboxChip("mellohi").inputs(false, false, false, false).build();

            Music.jukebox().trigger(state);

            assertThat(world.stoppedSounds()).containsExactly(Blocks.key("music_disc.mellohi"));
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void playsNothingWithNoJukeboxBesideIt() {
            SimpleChipState state =
                    chip(PinLayout.AISO, "", "[MCU706]", "mellohi", "").inputs(true, false, false, false).build();

            Music.jukebox().trigger(state);

            assertThat(world.sounds()).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void playsNothingForARecordThatDoesNotExist() {
            world.knowingOnlySounds("music_disc.mellohi");
            SimpleChipState state = jukeboxChip("notarecord").inputs(true, false, false, false).build();

            Music.jukebox().trigger(state);

            assertThat(world.sounds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("playing a written tune")
    class PlayingAWrittenTune {

        private SimpleChipState.Builder tuneChip(String line3, String line4) {
            world.withBlock(ABOVE_SUPPORT, "note_block");
            return chip(PinLayout.AISO, "", "[MCU705]", line3, line4);
        }

        @Test
        void soundsTheFirstNoteStraightAway() {
            SimpleChipState state = tuneChip("0c2e2g2", "").inputs(true, false, false, false).build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(1);

            assertThat(world.sounds()).hasSize(1);
            assertThat(world.sounds().getFirst().sound()).isEqualTo(NoteInstrument.HARP.sound());
        }

        @Test
        void soundsTheRestOfTheTuneInTime() {
            SimpleChipState state = tuneChip("0c2e2g2", "").inputs(true, false, false, false).build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(6);

            assertThat(world.sounds()).hasSize(3);
        }

        @Test
        void playsItThroughTheNoteBlock() {
            SimpleChipState state = tuneChip("0c2", "").inputs(true, false, false, false).build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(1);

            assertThat(world.sounds().getFirst().at()).isEqualTo(Vec3d.middleOf(ABOVE_SUPPORT));
        }

        @Test
        void runsOnFromTheThirdLineToTheFourth() {
            SimpleChipState state = tuneChip("0c2e2", "g2b2").inputs(true, false, false, false).build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(9);

            assertThat(world.sounds()).hasSize(4);
        }

        @Test
        void changesVoiceWhereTheTuneSaysTo() {
            SimpleChipState state = tuneChip("1c2" + "6e2", "").inputs(true, false, false, false).build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(3);

            assertThat(world.sounds().get(0).sound()).isEqualTo(NoteInstrument.BASS.sound());
            assertThat(world.sounds().get(1).sound()).isEqualTo(NoteInstrument.BELL.sound());
        }

        @Test
        void holdsItsOutputUpUntilTheTuneIsOver() {
            SimpleChipState state = tuneChip("0c2e2g2", "").inputs(true, false, false, false).build();
            ICLogic chip = Music.tune();

            chip.trigger(state);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(6);
            assertThat(state.mainOutput()).isTrue();

            state.manualScheduler().advance(1);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void playsNothingWithNoNoteBlockBesideIt() {
            SimpleChipState state = chip(PinLayout.AISO, "", "[MCU705]", "0c2e2", "")
                    .inputs(true, false, false, false)
                    .build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(10);

            assertThat(world.sounds()).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void playsNothingForATuneItCannotRead() {
            SimpleChipState state = tuneChip("hello", "").inputs(true, false, false, false).build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(10);

            assertThat(world.sounds()).isEmpty();
        }

        @Test
        void playsNothingWhileNothingDrivesIt() {
            SimpleChipState state = tuneChip("0c2e2", "").inputs(false, false, false, false).build();

            Music.tune().trigger(state);
            state.manualScheduler().advance(10);

            assertThat(world.sounds()).isEmpty();
        }
    }
}
