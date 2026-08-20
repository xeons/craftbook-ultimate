package com.xeonproductions.craftbookultimate.core.pipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Remembering what a pipe reaches")
class PipeNetworksTest {

    private static final UUID WORLD = UUID.fromString("6b1d5d2a-0000-4000-8000-0000000000aa");
    private static final UUID ELSEWHERE = UUID.fromString("6b1d5d2a-0000-4000-8000-0000000000bb");
    private static final PipeSettings SETTINGS = PipeSettings.DEFAULTS;

    /** Where the pipe starts, in the chunk at the origin. */
    private static final Vec3i INPUT = new Vec3i(0, 64, 0);

    private PipeNetworks networks;
    private SimplePipeWorld world;

    /** A sticky piston, three glass, a piston and a chest, running east from the origin. */
    @BeforeEach
    void buildAPipe() {
        networks = new PipeNetworks();
        world = new SimplePipeWorld()
                .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                .withContainer(new Vec3i(-1, 64, 0))
                .runFrom(new Vec3i(1, 64, 0), new Vec3i(3, 64, 0), "glass")
                .withPiston(new Vec3i(4, 64, 0), "piston", BlockFace.EAST)
                .withContainer(new Vec3i(5, 64, 0));
    }

    private PipeNetwork trace() {
        return networks.from(world, WORLD, INPUT, SETTINGS);
    }

    @Nested
    @DisplayName("keeping the answer")
    class KeepingTheAnswer {

        @Test
        void followsAPipeOnlyOnce() {
            PipeNetwork first = trace();

            assertThat(trace()).isSameAs(first);
        }

        @Test
        void knowsNothingUntilItHasBeenAsked() {
            assertThat(networks.remembers(WORLD, INPUT)).isFalse();

            trace();

            assertThat(networks.remembers(WORLD, INPUT)).isTrue();
        }

        @Test
        void keepsTheTwoWorldsApart() {
            trace();

            assertThat(networks.remembers(ELSEWHERE, INPUT)).isFalse();
        }
    }

    @Nested
    @DisplayName("throwing it away")
    class ThrowingItAway {

        @Test
        void forgetsAPipeOneOfItsOwnBlocksHasChanged() {
            trace();

            networks.forgetAbout(WORLD, new Vec3i(2, 64, 0));

            assertThat(networks.remembers(WORLD, INPUT)).isFalse();
        }

        @Test
        void forgetsAPipeSomethingHasBeenPlacedAgainst() {
            // A block beside the run is how a pipe grows, and nothing inside it has changed to
            // say so, which is why the blocks just outside are watched too.
            trace();

            networks.forgetAbout(WORLD, new Vec3i(2, 65, 0));

            assertThat(networks.remembers(WORLD, INPUT)).isFalse();
        }

        @Test
        void forgetsAPipeWhoseChestHasGone() {
            trace();

            networks.forgetAbout(WORLD, new Vec3i(5, 64, 0));

            assertThat(networks.remembers(WORLD, INPUT)).isFalse();
        }

        @Test
        void leavesAPipeAloneWhenSomethingFarOffChanges() {
            trace();

            networks.forgetAbout(WORLD, new Vec3i(400, 64, 400));

            assertThat(networks.remembers(WORLD, INPUT)).isTrue();
        }

        @Test
        void leavesAPipeAloneWhenTheSameBlockChangesInAnotherWorld() {
            trace();

            networks.forgetAbout(ELSEWHERE, new Vec3i(2, 64, 0));

            assertThat(networks.remembers(WORLD, INPUT)).isTrue();
        }

        @Test
        void worksOutTheNewShapeTheNextTimeItIsAsked() {
            assertThat(trace().deliveries()).hasSize(1);

            world.with(new Vec3i(2, 64, 0), "stone");
            networks.forgetAbout(WORLD, new Vec3i(2, 64, 0));

            assertThat(trace().reachesAnywhere()).isFalse();
        }
    }

    @Nested
    @DisplayName("letting go of what is no longer there")
    class LettingGoOfWhatIsNoLongerThere {

        @Test
        void forgetsAPipeReachingIntoAChunkThatHasGone() {
            trace();

            networks.forgetChunk(WORLD, 0, 0);

            assertThat(networks.remembers(WORLD, INPUT)).isFalse();
        }

        @Test
        void leavesAPipeAloneWhenAChunkItDoesNotReachGoes() {
            trace();

            networks.forgetChunk(WORLD, 30, 30);

            assertThat(networks.remembers(WORLD, INPUT)).isTrue();
        }

        @Test
        void forgetsAPipeWhoseFarEndIsInTheChunkThatHasGone() {
            SimplePipeWorld across = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(new Vec3i(-1, 64, 0))
                    .runFrom(new Vec3i(1, 64, 0), new Vec3i(17, 64, 0), "glass")
                    .withPiston(new Vec3i(18, 64, 0), "piston", BlockFace.EAST)
                    .withContainer(new Vec3i(19, 64, 0));

            networks.from(across, WORLD, INPUT, SETTINGS);
            networks.forgetChunk(WORLD, 1, 0);

            assertThat(networks.remembers(WORLD, INPUT)).isFalse();
        }

        @Test
        void forgetsEveryPipeInAWorldThatHasGone() {
            trace();

            networks.forgetWorld(WORLD);

            assertThat(networks.remembers(WORLD, INPUT)).isFalse();
            assertThat(networks.size()).isZero();
        }

        @Test
        void keepsNothingBehindAfterAPipeIsForgotten() {
            trace();
            networks.forgetAbout(WORLD, new Vec3i(2, 64, 0));

            // Nothing is left pointing at the pipe that has gone, so a later change anywhere it
            // used to run finds an empty index rather than a stale entry.
            networks.forgetChunk(WORLD, 0, 0);

            assertThat(networks.size()).isZero();
        }

        @Test
        void forgetsEverythingWhenTheSettingsAreReread() {
            trace();

            networks.forgetEverything();

            assertThat(networks.size()).isZero();
        }
    }
}
