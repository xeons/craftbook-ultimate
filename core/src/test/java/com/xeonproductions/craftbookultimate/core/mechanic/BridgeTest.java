package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The bridge")
class BridgeTest {

    private static final Bridge BRIDGE = new Bridge();

    private static final Key COBBLESTONE = Blocks.key("cobblestone");

    /** The near sign, facing south, so the deck runs away to the north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    /** The far sign, five blocks along, leaving four blocks of gap between the landings. */
    private static final Vec3i FAR_SIGN = new Vec3i(0, 64, -5);

    private static final Vec3i BASE = new Vec3i(0, 63, 0);
    private static final Vec3i FAR_BASE = new Vec3i(0, 63, -5);

    private static final Vec3i DECK_NEAR = new Vec3i(0, 63, -1);
    private static final Vec3i DECK_FAR = new Vec3i(0, 63, -4);

    private final SimpleStockpile chest = SimpleStockpile.empty();
    private final SimpleMechanicWorld world = new SimpleMechanicWorld().withStockpile(chest);

    /** Both signs, both landings, and nothing in between. */
    private SimpleMechanicWorld withBridge() {
        return world.withSign(SIGN, BlockFace.SOUTH, "", Bridge.START, "", "")
                .withSign(FAR_SIGN, BlockFace.NORTH, "", Bridge.END, "", "")
                .with(BASE, COBBLESTONE)
                .with(FAR_BASE, COBBLESTONE);
    }

    private MechanicVisit byHand() {
        return MechanicVisit.byHand(
                world.signAt(SIGN).orElseThrow(),
                world,
                Settings.DEFAULTS,
                SimpleActor.at(new Vec3i(0, 64, 1)));
    }

    private MechanicVisit byRedstone(boolean powered) {
        return MechanicVisit.byRedstone(
                world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, powered);
    }

    private int deckBlocks() {
        return world.count(DECK_NEAR, DECK_FAR, "cobblestone");
    }

    @Nested
    @DisplayName("running out across the gap")
    class RunningOutAcrossTheGap {

        @Test
        void laysADeckBetweenTheTwoLandings() {
            withBridge();
            chest.with(COBBLESTONE, 4);

            assertThat(BRIDGE.act(byHand())).isTrue();

            assertThat(deckBlocks()).isEqualTo(4);
        }

        @Test
        void paysForTheDeckOutOfTheChest() {
            withBridge();
            chest.with(COBBLESTONE, 10);

            BRIDGE.act(byHand());

            assertThat(chest.count(COBBLESTONE)).isEqualTo(6);
        }

        @Test
        void widensToMatchTheLandingsAtBothEnds() {
            withBridge();
            // A second row of blocks either side of both landings.
            world.with(BASE.offset(BlockFace.EAST), COBBLESTONE)
                    .with(FAR_BASE.offset(BlockFace.EAST), COBBLESTONE)
                    .with(BASE.offset(BlockFace.WEST), COBBLESTONE)
                    .with(FAR_BASE.offset(BlockFace.WEST), COBBLESTONE);
            chest.with(COBBLESTONE, 64);

            BRIDGE.act(byHand());

            assertThat(world.count(new Vec3i(-1, 63, -1), new Vec3i(1, 63, -4), "cobblestone"))
                    .isEqualTo(12);
        }

        @Test
        void isOnlyAsWideAsItsNarrowerEnd() {
            withBridge();
            // The near landing is wider than the far one, so the deck matches the far one.
            world.with(BASE.offset(BlockFace.EAST), COBBLESTONE)
                    .with(BASE.offset(BlockFace.EAST, 2), COBBLESTONE)
                    .with(FAR_BASE.offset(BlockFace.EAST), COBBLESTONE);
            chest.with(COBBLESTONE, 64);

            BRIDGE.act(byHand());

            assertThat(world.count(new Vec3i(1, 63, -1), new Vec3i(1, 63, -4), "cobblestone"))
                    .isEqualTo(4);
            assertThat(world.count(new Vec3i(2, 63, -1), new Vec3i(2, 63, -4), "cobblestone"))
                    .isZero();
        }

        @Test
        void worksJustAsWellHangingBelowTheDeck() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Bridge.START, "", "")
                    .withSign(FAR_SIGN, BlockFace.NORTH, "", Bridge.END, "", "")
                    .with(SIGN.offset(BlockFace.UP), COBBLESTONE)
                    .with(FAR_SIGN.offset(BlockFace.UP), COBBLESTONE);
            chest.with(COBBLESTONE, 64);

            BRIDGE.act(byHand());

            assertThat(world.count(new Vec3i(0, 65, -1), new Vec3i(0, 65, -4), "cobblestone"))
                    .isEqualTo(4);
        }

        @Test
        void leavesTheDeckRetractedWhenTheChestIsOneShort() {
            withBridge();
            chest.with(COBBLESTONE, 3);

            assertThat(BRIDGE.act(byHand())).isFalse();

            assertThat(deckBlocks()).isZero();
            assertThat(chest.count(COBBLESTONE)).isEqualTo(3);
        }

        @Test
        void refusesToBuildOverSomethingInTheWay() {
            withBridge();
            world.with(new Vec3i(0, 63, -2), Blocks.key("dirt"));
            chest.with(COBBLESTONE, 64);

            assertThat(BRIDGE.act(byHand())).isFalse();

            assertThat(deckBlocks()).isZero();
        }
    }

    @Nested
    @DisplayName("pulling back")
    class PullingBack {

        @Test
        void takesTheDeckAwayAgain() {
            withBridge();
            world.filling(DECK_NEAR, DECK_FAR, "cobblestone");

            assertThat(BRIDGE.act(byHand())).isTrue();

            assertThat(deckBlocks()).isZero();
        }

        @Test
        void putsTheBlocksBackInTheChest() {
            withBridge();
            world.filling(DECK_NEAR, DECK_FAR, "cobblestone");

            BRIDGE.act(byHand());

            assertThat(chest.count(COBBLESTONE)).isEqualTo(4);
        }

        @Test
        void leavesTheDeckOutWhenThereIsNowhereToPutIt() {
            withBridge().withStockpile(SimpleStockpile.withCapacity(2));
            world.filling(DECK_NEAR, DECK_FAR, "cobblestone");

            assertThat(BRIDGE.act(byHand())).isFalse();

            assertThat(deckBlocks()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("what it refuses")
    class WhatItRefuses {

        @Test
        void cannotBeWorkedFromTheFarEnd() {
            withBridge();
            chest.with(COBBLESTONE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, -6));

            boolean acted = BRIDGE.act(MechanicVisit.byHand(
                    world.signAt(FAR_SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("other end")).isTrue();
        }

        @Test
        void saysSoWhenTheOtherEndIsMissing() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Bridge.START, "", "")
                    .with(BASE, COBBLESTONE);
            chest.with(COBBLESTONE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = BRIDGE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("missing")).isTrue();
        }

        @Test
        void saysSoWhenTheFarLandingIsMadeOfSomethingElse() {
            withBridge();
            world.with(FAR_BASE, Blocks.key("oak_planks"));
            chest.with(COBBLESTONE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = BRIDGE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("something else")).isTrue();
        }

        @Test
        void saysSoWhenThereIsNothingToMakeADeckOf() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Bridge.START, "", "")
                    .withSign(FAR_SIGN, BlockFace.NORTH, "", Bridge.END, "", "");
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = BRIDGE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("above or below")).isTrue();
        }

        @Test
        void willNotBeMadeOfSomethingTheSettingsForbid() {
            Settings strict = Settings.builder()
                    .placeableBlocks(java.util.Set.of(Blocks.key("oak_planks")))
                    .build();
            withBridge();
            chest.with(COBBLESTONE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = BRIDGE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, strict, who));

            assertThat(acted).isFalse();
            assertThat(deckBlocks()).isZero();
        }
    }

    @Nested
    @DisplayName("driven by redstone")
    class DrivenByRedstone {

        @Test
        void putsTheDeckOutWhenPowerArrives() {
            withBridge();
            chest.with(COBBLESTONE, 64);

            BRIDGE.act(byRedstone(true));

            assertThat(deckBlocks()).isEqualTo(4);
        }

        @Test
        void pullsTheDeckBackWhenPowerLeaves() {
            withBridge();
            world.filling(DECK_NEAR, DECK_FAR, "cobblestone");

            BRIDGE.act(byRedstone(false));

            assertThat(deckBlocks()).isZero();
        }

        @Test
        void staysOutWhenPowerArrivesAgain() {
            // A lever thrown twice must not take the bridge down the second time.
            withBridge();
            chest.with(COBBLESTONE, 64);

            BRIDGE.act(byRedstone(true));
            BRIDGE.act(byRedstone(true));

            assertThat(deckBlocks()).isEqualTo(4);
        }

        @Test
        void buildsAroundSomethingInTheWayRatherThanStopping() {
            // There is nobody to complain to, and a bridge held out by a signal should stay out.
            withBridge();
            world.with(new Vec3i(0, 63, -2), Blocks.key("dirt"));
            chest.with(COBBLESTONE, 64);

            BRIDGE.act(byRedstone(true));

            assertThat(deckBlocks()).isEqualTo(3);
            assertThat(world.blockAt(new Vec3i(0, 63, -2))).isEqualTo(Blocks.key("dirt"));
        }
    }

    @Test
    void buildsOutOfNothingWhenItsSignSaysAdmin() {
        world.withSign(SIGN, BlockFace.SOUTH, PostedSign.ADMIN, Bridge.START, "", "")
                .withSign(FAR_SIGN, BlockFace.NORTH, "", Bridge.END, "", "")
                .with(BASE, COBBLESTONE)
                .with(FAR_BASE, COBBLESTONE);

        boolean acted = BRIDGE.act(MechanicVisit.byHand(
                world.signAt(SIGN).orElseThrow(),
                world,
                Settings.DEFAULTS,
                SimpleActor.at(new Vec3i(0, 64, 1))));

        assertThat(acted).isTrue();
        assertThat(deckBlocks()).isEqualTo(4);
        assertThat(chest.count(COBBLESTONE)).isZero();
    }
}
