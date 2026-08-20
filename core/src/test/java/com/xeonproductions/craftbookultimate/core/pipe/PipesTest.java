package com.xeonproductions.craftbookultimate.core.pipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Following a pipe")
class PipesTest {

    private static final PipeSettings SETTINGS = PipeSettings.DEFAULTS;
    private static final Key COAL = Blocks.key("coal");
    private static final Key IRON = Blocks.key("iron_ingot");

    /** Where a pipe starts, with its source to the west and its run to the east. */
    private static final Vec3i INPUT = new Vec3i(0, 64, 0);

    private static Vec3i east(int steps) {
        return new Vec3i(steps, 64, 0);
    }

    private static List<Vec3i> containersOf(PipeNetwork network) {
        return network.deliveries().stream().map(PipeNetwork.Delivery::container).toList();
    }

    @Nested
    @DisplayName("a glass pipe")
    class AGlassPipe {

        /**
         * A sticky piston facing west at the origin, glass running east, and a piston at the end
         * pointing at a chest.
         */
        private SimplePipeWorld straightRun(String pipeBlock) {
            return new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .runFrom(east(1), east(3), pipeBlock)
                    .withPiston(east(4), "piston", BlockFace.EAST)
                    .withContainer(east(5));
        }

        @Test
        void findsTheChestAPistonAtItsEndPointsAt() {
            PipeNetwork network = Pipes.trace(straightRun("glass"), INPUT, SETTINGS);

            assertThat(network.style()).isEqualTo(PipeStyle.GLASS);
            assertThat(containersOf(network)).containsExactly(east(5));
        }

        @Test
        void takesItemsFromWhateverTheStickyPistonFaces() {
            PipeNetwork network = Pipes.trace(straightRun("glass"), INPUT, SETTINGS);

            assertThat(network.source()).contains(east(-1));
        }

        @Test
        void neverPutsItemsBackWhereItTookThemFrom() {
            SimplePipeWorld world = straightRun("glass")
                    .withPiston(east(1), "piston", BlockFace.WEST);

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(containersOf(network)).doesNotContain(east(-1));
        }

        @Test
        void carriesOnThroughAPaneRatherThanSpreadingAtIt() {
            // A pane is a crossing: a run goes straight over it, which is what lets two pipes
            // share a block without either taking the other's items.
            SimplePipeWorld world = straightRun("glass")
                    .with(east(2), "glass_pane")
                    .withPiston(new Vec3i(2, 65, 0), "piston", BlockFace.UP)
                    .withContainer(new Vec3i(2, 66, 0));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(containersOf(network)).containsExactly(east(5));
        }

        @Test
        void branchesWhereGlassTouchesGlass() {
            SimplePipeWorld world = straightRun("glass")
                    .with(new Vec3i(2, 65, 0), "glass")
                    .withPiston(new Vec3i(2, 66, 0), "piston", BlockFace.UP)
                    .withContainer(new Vec3i(2, 67, 0));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(containersOf(network))
                    .containsExactlyInAnyOrder(east(5), new Vec3i(2, 67, 0));
        }

        @Test
        void reachesTheNearestWayOutFirst() {
            SimplePipeWorld world = straightRun("glass")
                    .with(new Vec3i(1, 65, 0), "glass")
                    .withPiston(new Vec3i(1, 66, 0), "piston", BlockFace.UP)
                    .withContainer(new Vec3i(1, 67, 0));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(containersOf(network)).startsWith(new Vec3i(1, 67, 0));
        }

        @Nested
        @DisplayName("coloured")
        class Coloured {

            @Test
            void passesFromOneColourToItsOwn() {
                PipeNetwork network =
                        Pipes.trace(straightRun("red_stained_glass"), INPUT, SETTINGS);

                assertThat(containersOf(network)).containsExactly(east(5));
            }

            @Test
            void refusesToPassFromOneColourToAnother() {
                SimplePipeWorld world = straightRun("red_stained_glass")
                        .with(east(2), "blue_stained_glass");

                PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

                assertThat(network.reachesAnywhere()).isFalse();
            }

            @Test
            void passesThroughPlainGlassWhateverColourItCameFrom() {
                SimplePipeWorld world = straightRun("red_stained_glass").with(east(2), "glass");

                PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

                assertThat(containersOf(network)).containsExactly(east(5));
            }
        }
    }

    @Nested
    @DisplayName("a pane pipe")
    class APanePipe {

        /**
         * An extractor facing east at the origin, panes running east, and a chest beside the last
         * of them. The source sits behind the extractor.
         */
        private SimplePipeWorld straightRun() {
            return new SimplePipeWorld()
                    .withPiston(INPUT, "piston", BlockFace.EAST)
                    .withContainer(east(-1))
                    .runFrom(east(1), east(3), "glass_pane")
                    .withContainer(new Vec3i(3, 65, 0));
        }

        @Test
        void takesItemsFromBehindTheExtractorRatherThanInFrontOfIt() {
            PipeNetwork network = Pipes.trace(straightRun(), INPUT, SETTINGS);

            assertThat(network.style()).isEqualTo(PipeStyle.PANE);
            assertThat(network.source()).contains(east(-1));
        }

        @Test
        void fillsWhateverContainerTheRunTouches() {
            PipeNetwork network = Pipes.trace(straightRun(), INPUT, SETTINGS);

            assertThat(containersOf(network)).containsExactly(new Vec3i(3, 65, 0));
        }

        @Test
        void spreadsAtEveryPaneRatherThanGoingStraightOn() {
            SimplePipeWorld world = straightRun()
                    .with(new Vec3i(2, 65, 0), "glass_pane")
                    .withContainer(new Vec3i(2, 66, 0));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(containersOf(network))
                    .containsExactlyInAnyOrder(new Vec3i(3, 65, 0), new Vec3i(2, 66, 0));
        }

        @Test
        void doesNotRunThroughGlass() {
            SimplePipeWorld world = straightRun().with(east(2), "glass");

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(network.reachesAnywhere()).isFalse();
        }

        @Test
        void neverPutsItemsBackWhereItTookThemFrom() {
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "piston", BlockFace.EAST)
                    .withContainer(east(-1))
                    .with(east(1), "glass_pane")
                    .with(new Vec3i(-1, 64, 0), "chest");

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(containersOf(network)).doesNotContain(east(-1));
        }
    }

    @Nested
    @DisplayName("the two kinds together")
    class TheTwoKindsTogether {

        @Test
        void readsAPaneAsWhicheverKindOfPipeReachedIt() {
            // The same block, and it means different things in the two runs: the glass pipe goes
            // straight over it, the pane pipe spreads from it.
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .with(east(1), "glass_pane")
                    .with(east(2), "glass_pane")
                    .withContainer(new Vec3i(1, 65, 0));

            PipeNetwork asGlass = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(asGlass.reachesAnywhere()).isFalse();

            SimplePipeWorld asPipe = new SimplePipeWorld()
                    .withPiston(INPUT, "piston", BlockFace.EAST)
                    .withContainer(east(-1))
                    .with(east(1), "glass_pane")
                    .with(east(2), "glass_pane")
                    .withContainer(new Vec3i(1, 65, 0));

            assertThat(containersOf(Pipes.trace(asPipe, INPUT, SETTINGS)))
                    .containsExactly(new Vec3i(1, 65, 0));
        }

        @Test
        void findsNoPipeAtAllAtAnOrdinaryBlock() {
            SimplePipeWorld world = new SimplePipeWorld().with(INPUT, "stone");

            assertThat(Pipes.trace(world, INPUT, SETTINGS).reachesAnywhere()).isFalse();
        }
    }

    @Nested
    @DisplayName("knowing when to stop")
    class KnowingWhenToStop {

        @Test
        void followsOnlyAsFarAsItIsAllowed() {
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .runFrom(east(1), east(40), "glass")
                    .withPiston(east(41), "piston", BlockFace.EAST)
                    .withContainer(east(42));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS.withMaxLength(6));

            assertThat(network.whole()).isFalse();
            assertThat(network.reachesAnywhere()).isFalse();
        }

        @Test
        void saysSoWhenPartOfItCannotBeRead() {
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .runFrom(east(1), east(3), "glass")
                    .unloadedAt(east(2));

            assertThat(Pipes.trace(world, INPUT, SETTINGS).whole()).isFalse();
        }

        @Test
        void remembersEveryBlockItIsMadeOfSoTheAnswerCanBeThrownAway() {
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .runFrom(east(1), east(3), "glass")
                    .withPiston(east(4), "piston", BlockFace.EAST)
                    .withContainer(east(5));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(network.touches(east(2))).isTrue();
            assertThat(network.touches(east(5))).isTrue();
            assertThat(network.touches(east(-1))).isTrue();
            assertThat(network.touches(new Vec3i(9, 9, 9))).isFalse();
        }
    }

    @Nested
    @DisplayName("what a way out will take")
    class WhatAWayOutWillTake {

        @Test
        void takesAnythingWhereNoSignSaysOtherwise() {
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .with(east(1), "glass")
                    .withPiston(east(2), "piston", BlockFace.EAST)
                    .withContainer(east(3));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(network.deliveries().getFirst().filter().isAnything()).isTrue();
        }

        @Test
        void readsAFilterOffASignOnThePistonThatFillsIt() {
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .with(east(1), "glass")
                    .withPiston(east(2), "piston", BlockFace.EAST)
                    .withSignOn(east(2), "", "[Pipe]", "coal", "")
                    .withContainer(east(3));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(network.deliveriesFor(COAL)).hasSize(1);
            assertThat(network.deliveriesFor(IRON)).isEmpty();
        }

        @Test
        void ignoresASignThatNamesSomethingElse() {
            SimplePipeWorld world = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(east(-1))
                    .with(east(1), "glass")
                    .withPiston(east(2), "piston", BlockFace.EAST)
                    .withSignOn(east(2), "", "[Extractor]", "coal", "")
                    .withContainer(east(3));

            PipeNetwork network = Pipes.trace(world, INPUT, SETTINGS);

            assertThat(network.deliveriesFor(IRON)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("a filter on a sign")
    class AFilterOnASign {

        private PipeFilter filter(String wanted, String refused) {
            return PipeFilter.on(
                    SignLines.of("", "[Pipe]", wanted, refused), new SimplePipeWorld()::resolveItem);
        }

        @Test
        void carriesEverythingWhenItNamesNothing() {
            assertThat(filter("", "").carries(COAL)).isTrue();
            assertThat(filter("", "").isAnything()).isTrue();
        }

        @Test
        void carriesOnlyWhatTheThirdLineNames() {
            PipeFilter only = filter("coal, iron_ingot", "");

            assertThat(only.carries(COAL)).isTrue();
            assertThat(only.carries(IRON)).isTrue();
            assertThat(only.carries(Blocks.key("stone"))).isFalse();
        }

        @Test
        void refusesWhatTheFourthLineNames() {
            PipeFilter except = filter("", "coal");

            assertThat(except.carries(COAL)).isFalse();
            assertThat(except.carries(IRON)).isTrue();
        }

        @Test
        void letsTheFourthLineCarveAnExceptionOutOfTheThird() {
            PipeFilter both = filter("coal, iron_ingot", "coal");

            assertThat(both.carries(COAL)).isFalse();
            assertThat(both.carries(IRON)).isTrue();
        }

        @Test
        void readsThePreFlatteningSpellingsAWorldIsFullOf() {
            assertThat(filter("35:14", "").carries(Blocks.key("red_wool"))).isTrue();
        }

        @Test
        void complainsAboutANameThatMeansNothing() {
            SimplePipeWorld world = new SimplePipeWorld().knowingOnly("coal");

            assertThat(PipeFilter.problemWith(
                    SignLines.of("", "[Pipe]", "coal, aeroplane", ""), world::resolveItem))
                    .isPresent();
        }

        @Test
        void acceptsASignThatNamesNothingAtAll() {
            SimplePipeWorld world = new SimplePipeWorld().knowingOnly("coal");

            assertThat(PipeFilter.problemWith(
                    SignLines.of("", "[Pipe]", "", ""), world::resolveItem))
                    .isEmpty();
        }
    }
}
