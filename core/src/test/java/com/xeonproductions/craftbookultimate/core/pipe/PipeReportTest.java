// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.pipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.debug.DebugMode;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Saying what the plugin believes about a pipe")
class PipeReportTest {

    private static final UUID WORLD = UUID.fromString("6b1d5d2a-0000-4000-8000-0000000000aa");
    private static final PipeSettings SETTINGS = PipeSettings.DEFAULTS;

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

    private PipeReport reportOn(Vec3i clicked, boolean remembered) {
        PipeNetwork network = networks.from(world, WORLD, INPUT, SETTINGS);
        boolean holdsItems = network.source().filter(world::holdsItemsAt).isPresent();
        return new PipeReport("world", clicked, INPUT, network, remembered, holdsItems);
    }

    /** Everything the report says, as one searchable piece of plain text. */
    private static String said(PipeReport report) {
        return report.describe().stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .reduce("", (all, line) -> all + line + "\n");
    }

    @Nested
    @DisplayName("about a working pipe")
    class AboutAWorkingPipe {

        @Test
        void saysWhereItIs() {
            assertThat(said(reportOn(INPUT, false))).contains("Pipe at 0, 64, 0");
        }

        @Test
        void saysWhichWorld() {
            assertThat(said(reportOn(INPUT, false))).contains("World: world");
        }

        @Test
        void saysWhichGrammarTheRunWasReadWith() {
            assertThat(said(reportOn(INPUT, false))).contains("glass");
        }

        @Test
        void saysWhereItTakesFrom() {
            assertThat(said(reportOn(INPUT, false))).contains("Takes from: -1, 64, 0");
        }

        @Test
        void saysWhereItReaches() {
            assertThat(said(reportOn(INPUT, false))).contains("5, 64, 0");
        }

        @Test
        void countsTheBlocksTheRunIsMadeOf() {
            assertThat(said(reportOn(INPUT, false))).contains("Blocks in the run");
        }

        @Test
        void saysWhetherTheAnswerWasAlreadyHeld() {
            assertThat(said(reportOn(INPUT, true))).contains("remembered");
            assertThat(said(reportOn(INPUT, false))).contains("worked out just now");
        }

        @Test
        void saysTheWaysOutAreTriedNearestFirst() {
            // The order is the whole reason a builder asks, so the report must say it is an order.
            assertThat(said(reportOn(INPUT, false))).contains("nearest first");
        }
    }

    @Nested
    @DisplayName("about a pipe that will not work")
    class AboutABrokenPipe {

        @Test
        void saysSoWhenTheRunReachesNothing() {
            SimplePipeWorld lonely = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .withContainer(new Vec3i(-1, 64, 0))
                    .runFrom(new Vec3i(1, 64, 0), new Vec3i(3, 64, 0), "glass");

            PipeNetwork network = new PipeNetworks().from(lonely, WORLD, INPUT, SETTINGS);
            String text = said(new PipeReport("world", INPUT, INPUT, network, false, true));

            assertThat(text).contains("Reaches: nowhere");
            assertThat(text).contains("no container it may fill");
        }

        @Test
        void saysSoWhenNothingHoldsItemsWhereItTakesFrom() {
            // The input faces a real place; there is simply no chest in it. That is the fault a
            // builder can actually fix, and it reads nothing like a pipe facing nowhere.
            SimplePipeWorld starved = new SimplePipeWorld()
                    .withPiston(INPUT, "sticky_piston", BlockFace.WEST)
                    .runFrom(new Vec3i(1, 64, 0), new Vec3i(3, 64, 0), "glass")
                    .withPiston(new Vec3i(4, 64, 0), "piston", BlockFace.EAST)
                    .withContainer(new Vec3i(5, 64, 0));

            PipeNetwork network = new PipeNetworks().from(starved, WORLD, INPUT, SETTINGS);
            String text = said(new PipeReport("world", INPUT, INPUT, network, false, false));

            assertThat(text).contains("nothing there holds items");
        }

        @Test
        void saysSoWhenTheInputFacesNowhereAtAll() {
            PipeNetwork facingNowhere = new PipeNetwork(
                    PipeStyle.GLASS,
                    Optional.empty(),
                    List.of(),
                    Set.of(INPUT),
                    true);

            assertThat(said(new PipeReport("world", INPUT, INPUT, facingNowhere, false, false)))
                    .contains("Takes from: nothing");
        }

        @Test
        void saysWhenTheClickedBlockIsOnlyBesideTheRun() {
            // A builder pointing at the block next to a pipe gets told that is what they did.
            Vec3i beside = new Vec3i(2, 65, 0);

            assertThat(said(reportOn(beside, true)))
                    .contains("beside this pipe rather than part of it");
        }

        @Test
        void saysNothingAboutBeingBesideWhenTheBlockIsInTheRun() {
            assertThat(said(reportOn(new Vec3i(2, 64, 0), true)))
                    .doesNotContain("beside this pipe");
        }

        @Test
        void warnsWhenTheRunWasCutShortByItsLimit() {
            PipeNetwork cutShort = new PipeNetwork(
                    PipeStyle.GLASS,
                    Optional.of(new Vec3i(-1, 64, 0)),
                    List.of(),
                    Set.of(INPUT),
                    false);

            assertThat(said(new PipeReport("world", INPUT, INPUT, cutShort, false, true)))
                    .contains("cut short by its length limit");
        }

        @Test
        void saysNothingAboutTheLimitOnAWholePipe() {
            assertThat(said(reportOn(INPUT, false))).doesNotContain("cut short");
        }
    }

    @Nested
    @DisplayName("finding which pipe a block belongs to")
    class FindingThePipe {

        @Test
        void findsARememberedPipeFromABlockOfIt() {
            networks.from(world, WORLD, INPUT, SETTINGS);

            assertThat(networks.inputsTouching(WORLD, new Vec3i(2, 64, 0))).contains(INPUT);
        }

        @Test
        void findsNothingForABlockNoPipeHasEverTouched() {
            networks.from(world, WORLD, INPUT, SETTINGS);

            assertThat(networks.inputsTouching(WORLD, new Vec3i(500, 64, 500))).isEmpty();
        }

        @Test
        void findsNothingBeforeAnyPipeHasRun() {
            assertThat(networks.inputsTouching(WORLD, new Vec3i(2, 64, 0))).isEmpty();
        }

        @Test
        void handsBackTheAnswerItRemembers() {
            PipeNetwork traced = networks.from(world, WORLD, INPUT, SETTINGS);

            assertThat(networks.remembered(WORLD, INPUT)).contains(traced);
        }

        @Test
        void remembersNothingItHasNotTraced() {
            assertThat(networks.remembered(WORLD, INPUT)).isEmpty();
        }

        @Test
        void forgetsTheLookupWhenTheAnswerGoes() {
            networks.from(world, WORLD, INPUT, SETTINGS);

            networks.forgetAbout(WORLD, new Vec3i(2, 64, 0));

            assertThat(networks.remembered(WORLD, INPUT)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the mode that reads it")
    class TheMode {

        @Test
        void readsAnOrdinaryBlockRatherThanAChip() {
            assertThat(DebugMode.PIPE
                            .readsAnyBlock())
                    .isTrue();
        }

        @Test
        void isTheOnlyModeThatDoes() {
            assertThat(DebugMode.CYCLE.stream()
                            .filter(DebugMode
                                    ::readsAnyBlock)
                            .toList())
                    .containsExactly(
                            DebugMode.PIPE);
        }

        @Test
        void hasItsOwnPermission() {
            assertThat(DebugMode.PIPE.permission())
                    .isEqualTo("craftbook.debug.pipe");
        }
    }

    @Test
    void describingAPipeChangesNothingAboutIt() {
        PipeNetwork before = networks.from(world, WORLD, INPUT, SETTINGS);

        said(reportOn(INPUT, true));

        assertThat(networks.remembered(WORLD, INPUT)).contains(before);
    }
}
