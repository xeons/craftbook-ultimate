package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.music.NoteInstrument;
import com.xeonproductions.craftbookultimate.core.music.Playlist;
import com.xeonproductions.craftbookultimate.core.music.Song;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The chip that plays a MIDI file")
class MelodyTest {

    /** Where the sign hangs. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    /** The block above the one it hangs on, where the note block goes. */
    private static final Vec3i NOTE_BLOCK = new Vec3i(0, 65, -1);

    /** A seeded source, so a shuffling playlist behaves the same on every run. */
    private static final long SEED = 1234;

    private final ChipServices services = ChipServices.create();
    private final SimpleChipWorld world = new SimpleChipWorld();

    /** A song of a few notes, one every other tick. */
    private static Song songOf(String name, int notes) {
        return new Song(
                name,
                java.util.stream.IntStream.range(0, notes)
                        .mapToObj(i -> new Song.Note(i * 2L, NoteInstrument.HARP, 1, 1))
                        .toList(),
                notes * 2L);
    }

    private SimpleChipState.Builder chip(String line3, String line4) {
        world.withBlock(NOTE_BLOCK, "note_block");
        return SimpleChipState.forLayout(PinLayout.UISO)
                .services(services)
                .world(world)
                .at(SIGN, BlockFace.SOUTH)
                .sign("", "[MCU700]", line3, line4);
    }

    private static ICLogic melody() {
        return Music.melody(new Random(SEED));
    }

    @Nested
    @DisplayName("playing one song")
    class PlayingOneSong {

        @Test
        void playsTheSongTheSignNames() {
            services.songs().putSong("fanfare", songOf("fanfare", 10));
            SimpleChipState state = chip("fanfare", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(6);

            assertThat(world.sounds()).hasSize(3);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void playsItThroughTheNoteBlock() {
            services.songs().putSong("fanfare", songOf("fanfare", 1));
            SimpleChipState state = chip("fanfare", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(2);

            assertThat(world.sounds().getFirst().at()).isEqualTo(Vec3d.middleOf(NOTE_BLOCK));
        }

        @Test
        void dropsItsOutputWhenTheSongRunsOut() {
            services.songs().putSong("fanfare", songOf("fanfare", 2));
            SimpleChipState state = chip("fanfare", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(20);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void stopsPartWayThroughWhenNothingDrivesItAnyMore() {
            services.songs().putSong("fanfare", songOf("fanfare", 20));
            SimpleChipState state = chip("fanfare", "").inputs(true, false, false, false).build();
            ICLogic chip = melody();

            chip.trigger(state);
            state.manualScheduler().advance(4);
            int soFar = world.sounds().size();

            chip.trigger(state.withInput(0, false));
            state.manualScheduler().advance(40);

            assertThat(world.sounds()).hasSize(soFar);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void stopsWhenItIsUnloaded() {
            services.songs().putSong("fanfare", songOf("fanfare", 20));
            SimpleChipState state = chip("fanfare", "").inputs(true, false, false, false).build();
            ICLogic chip = melody();

            chip.trigger(state);
            state.manualScheduler().advance(4);
            int soFar = world.sounds().size();

            chip.unload(state);
            state.manualScheduler().advance(40);

            assertThat(world.sounds()).hasSize(soFar);
        }

        @Test
        void doesNotStartAgainWhileItIsAlreadyPlaying() {
            services.songs().putSong("fanfare", songOf("fanfare", 10));
            SimpleChipState state = chip("fanfare", "").inputs(true, false, false, false).build();
            ICLogic chip = melody();

            chip.trigger(state);
            state.manualScheduler().advance(4);
            chip.trigger(state);
            state.manualScheduler().advance(16);

            assertThat(world.sounds()).hasSize(10);
        }

        @Test
        void startsAgainAtTheEndWhenAskedToLoop() {
            services.songs().putSong("fanfare", songOf("fanfare", 3));
            SimpleChipState state = chip("fanfare", "loop").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(30);

            assertThat(world.sounds()).hasSizeGreaterThan(3);
        }

        @Test
        void playsNothingForASongNobodyHas() {
            SimpleChipState state = chip("nosuchsong", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(20);

            assertThat(world.sounds()).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void playsNothingWithNoNoteBlockBesideIt() {
            services.songs().putSong("fanfare", songOf("fanfare", 3));
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.UISO)
                    .services(services)
                    .world(new SimpleChipWorld())
                    .at(SIGN, BlockFace.SOUTH)
                    .sign("", "[MCU700]", "fanfare", "")
                    .inputs(true, false, false, false)
                    .build();

            melody().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void playsNothingWhileNothingDrivesIt() {
            services.songs().putSong("fanfare", songOf("fanfare", 3));
            SimpleChipState state = chip("fanfare", "").inputs(false, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(20);

            assertThat(world.sounds()).isEmpty();
        }

        @Test
        void ignoresTheSpeedTheSignAsksFor() {
            services.songs().putSong("fanfare", songOf("fanfare", 3));
            SimpleChipState state = chip("fanfare:6", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(6);

            assertThat(world.sounds()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("working through a playlist")
    class WorkingThroughAPlaylist {

        private void twoSongPlaylist() {
            services.songs().putSong("first", songOf("first", 2));
            services.songs().putSong("second", songOf("second", 2));
            services.songs().putPlaylist("set", new Playlist("set", List.of("first", "second")));
        }

        @Test
        void playsTheFirstSongOnTheList() {
            twoSongPlaylist();
            SimpleChipState state = chip("set.p", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(3);

            assertThat(world.sounds()).hasSize(2);
        }

        @Test
        void movesOnToTheNextSongWhenOneEnds() {
            twoSongPlaylist();
            SimpleChipState state = chip("set.p", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(20);

            assertThat(world.sounds()).hasSize(4);
        }

        @Test
        void stopsAtTheEndOfTheListWhenItWasNotAskedToLoop() {
            twoSongPlaylist();
            SimpleChipState state = chip("set.p", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(200);

            assertThat(world.sounds()).hasSize(4);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void goesRoundAgainWhenAskedToLoop() {
            twoSongPlaylist();
            SimpleChipState state = chip("set.p", "loop").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(200);

            assertThat(world.sounds()).hasSizeGreaterThan(4);
        }

        @Test
        void keepsGoingWhenAskedToShuffle() {
            twoSongPlaylist();
            SimpleChipState state = chip("set.p", "random").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(200);

            assertThat(world.sounds()).hasSizeGreaterThan(4);
        }

        @Test
        void readsBothFlagsOffTheSameLine() {
            twoSongPlaylist();
            SimpleChipState state =
                    chip("set.p", "loop:random").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(60);

            assertThat(world.sounds()).hasSizeGreaterThan(4);
        }

        @Test
        void playsNothingForAPlaylistNobodyHas() {
            SimpleChipState state = chip("nosuchset.p", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(20);

            assertThat(world.sounds()).isEmpty();
        }

        @Test
        void skipsPastASongThePlaylistNamesButNobodyHas() {
            services.songs().putSong("second", songOf("second", 2));
            services.songs().putPlaylist("set", new Playlist("set", List.of("missing", "second")));
            SimpleChipState state = chip("set.p", "").inputs(true, false, false, false).build();

            melody().trigger(state);
            state.manualScheduler().advance(20);

            assertThat(world.sounds()).isEmpty();
        }
    }
}
