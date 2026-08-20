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

@DisplayName("The gate")
class GateTest {

    private static final Gate GATE = new Gate();

    private static final Key OAK_FENCE = Blocks.key("oak_fence");
    private static final Key IRON_BARS = Blocks.key("iron_bars");
    private static final Key GLASS_PANE = Blocks.key("glass_pane");
    private static final Key STONE = Blocks.key("stone");

    /** The sign, kept out of the gate's own column so the gate cannot bury it. */
    private static final Vec3i SIGN = new Vec3i(2, 64, 0);

    /** The lintel the gate hangs from, and the block it hangs by. */
    private static final Vec3i LINTEL = new Vec3i(0, 68, 0);

    private static final Vec3i HANGING = new Vec3i(0, 67, 0);

    /** The floor the gate reaches when it is closed. */
    private static final Vec3i GROUND = new Vec3i(0, 60, 0);

    /** The six blocks a closed gate fills between the hanging block and the floor. */
    private static final Vec3i CURTAIN_TOP = new Vec3i(0, 66, 0);

    private static final Vec3i CURTAIN_BOTTOM = new Vec3i(0, 61, 0);

    private final SimpleStockpile chest = SimpleStockpile.empty();
    private final SimpleMechanicWorld world = new SimpleMechanicWorld().withStockpile(chest);

    /** A gate of one column, hanging open from its lintel. */
    private SimpleMechanicWorld withGate(String signName, Key material) {
        return world.withSign(SIGN, BlockFace.SOUTH, "", signName, "", "")
                .with(LINTEL, STONE)
                .with(HANGING, material)
                .with(GROUND, STONE);
    }

    private MechanicVisit byHand() {
        return MechanicVisit.byHand(
                world.signAt(SIGN).orElseThrow(),
                world,
                Settings.DEFAULTS,
                SimpleActor.at(new Vec3i(2, 64, 1)));
    }

    private MechanicVisit byRedstone(boolean powered) {
        return MechanicVisit.byRedstone(
                world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, powered);
    }

    private int curtain(String material) {
        return world.count(CURTAIN_BOTTOM, CURTAIN_TOP, material);
    }

    @Nested
    @DisplayName("closing")
    class Closing {

        @Test
        void dropsTheColumnToTheFloor() {
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);

            assertThat(GATE.act(byHand())).isTrue();

            assertThat(curtain("oak_fence")).isEqualTo(6);
        }

        @Test
        void leavesTheBlockItHangsByWhereItIs() {
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);

            GATE.act(byHand());

            assertThat(world.blockAt(HANGING)).isEqualTo(OAK_FENCE);
            assertThat(world.blockAt(LINTEL)).isEqualTo(STONE);
        }

        @Test
        void paysForEveryBlockOutOfTheChest() {
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 10);

            GATE.act(byHand());

            assertThat(chest.count(OAK_FENCE)).isEqualTo(4);
        }

        @Test
        void stopsWhereTheBlocksRunOut() {
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 2);
            SimpleActor who = SimpleActor.at(new Vec3i(2, 64, 1));

            GATE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(curtain("oak_fence")).isEqualTo(2);
            assertThat(who.wasTold("not enough blocks")).isTrue();
        }

        @Test
        void dropsEveryColumnOfAWideGate() {
            withGate("[Gate]", OAK_FENCE);
            world.with(new Vec3i(1, 68, 0), STONE)
                    .with(new Vec3i(1, 67, 0), OAK_FENCE)
                    .with(new Vec3i(1, 60, 0), STONE);
            chest.with(OAK_FENCE, 64);

            GATE.act(byHand());

            assertThat(world.count(new Vec3i(0, 61, 0), new Vec3i(1, 66, 0), "oak_fence"))
                    .isEqualTo(12);
        }

        @Test
        void followsAnArchThatStepsUpAndOverAGap() {
            // The two hanging blocks are diagonal neighbours, which is how an arched gate is
            // built and how it stays one gate rather than two.
            withGate("[Gate]", OAK_FENCE);
            world.with(new Vec3i(1, 69, 0), STONE)
                    .with(new Vec3i(1, 68, 0), OAK_FENCE)
                    .with(new Vec3i(1, 60, 0), STONE);
            chest.with(OAK_FENCE, 64);

            GATE.act(byHand());

            assertThat(world.count(new Vec3i(1, 61, 0), new Vec3i(1, 67, 0), "oak_fence"))
                    .isEqualTo(7);
        }

        @Test
        void fillsThroughWaterSoAGateCanHoldItBack() {
            withGate("[Gate]", OAK_FENCE);
            world.filling(CURTAIN_BOTTOM, CURTAIN_TOP, "water");
            chest.with(OAK_FENCE, 64);

            GATE.act(byHand());

            assertThat(curtain("oak_fence")).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("opening")
    class Opening {

        @Test
        void windsEveryBlockBackUpIntoTheLintel() {
            withGate("[Gate]", OAK_FENCE);
            world.filling(CURTAIN_BOTTOM, CURTAIN_TOP, "oak_fence");

            assertThat(GATE.act(byHand())).isTrue();

            assertThat(curtain("oak_fence")).isZero();
            assertThat(world.blockAt(HANGING)).isEqualTo(OAK_FENCE);
        }

        @Test
        void putsTheBlocksBackInTheChest() {
            withGate("[Gate]", OAK_FENCE);
            world.filling(CURTAIN_BOTTOM, CURTAIN_TOP, "oak_fence");

            GATE.act(byHand());

            assertThat(chest.count(OAK_FENCE)).isEqualTo(6);
        }

        @Test
        void stopsWhenThereIsNowhereToPutTheBlocks() {
            withGate("[Gate]", OAK_FENCE).withStockpile(SimpleStockpile.withCapacity(2));
            world.filling(CURTAIN_BOTTOM, CURTAIN_TOP, "oak_fence");
            SimpleActor who = SimpleActor.at(new Vec3i(2, 64, 1));

            GATE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(who.wasTold("nowhere to put")).isTrue();
        }
    }

    @Nested
    @DisplayName("the material each sign takes")
    class TheMaterialEachSignTakes {

        @Test
        void thePlainSignTakesAnythingAGateCanBeMadeOf() {
            withGate("[Gate]", IRON_BARS);
            chest.with(IRON_BARS, 64);

            GATE.act(byHand());

            assertThat(curtain("iron_bars")).isEqualTo(6);
        }

        @Test
        void theGlassSignIgnoresAFence() {
            withGate("[GlassGate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(2, 64, 1));

            boolean acted = GATE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("no gate")).isTrue();
        }

        @Test
        void theGlassSignTakesAPane() {
            withGate("[GlassGate]", GLASS_PANE);
            chest.with(GLASS_PANE, 64);

            GATE.act(byHand());

            assertThat(curtain("glass_pane")).isEqualTo(6);
        }

        @Test
        void theIronSignTakesBars() {
            withGate("[IronGate]", IRON_BARS);
            chest.with(IRON_BARS, 64);

            GATE.act(byHand());

            assertThat(curtain("iron_bars")).isEqualTo(6);
        }

        @Test
        void theNetherSignTakesNetherBrickFence() {
            Key netherFence = Blocks.key("nether_brick_fence");
            withGate("[NetherGate]", netherFence);
            chest.with(netherFence, 64);

            GATE.act(byHand());

            assertThat(curtain("nether_brick_fence")).isEqualTo(6);
        }

        @Test
        void willNotTakeSomethingTheSettingsDoNotAllowAsAGate() {
            Settings onlyIron = Settings.builder()
                    .mechanics(Settings.DEFAULTS.mechanics()
                            .withGateBlocks(java.util.Set.of(IRON_BARS)))
                    .build();
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);

            boolean acted = GATE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(),
                    world,
                    onlyIron,
                    SimpleActor.at(new Vec3i(2, 64, 1))));

            assertThat(acted).isFalse();
        }
    }

    @Nested
    @DisplayName("how far each sign looks")
    class HowFarEachSignLooks {

        @Test
        void theSmallSignIgnoresAGateThreeBlocksAway() {
            withGate("[DGate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(2, 64, 1));

            boolean acted = GATE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("no gate")).isTrue();
        }

        @Test
        void theSmallSignFindsTheGateBesideIt() {
            world.withSign(SIGN, BlockFace.SOUTH, "", "[DGate]", "", "")
                    .with(new Vec3i(1, 66, 0), STONE)
                    .with(new Vec3i(1, 65, 0), OAK_FENCE)
                    .with(new Vec3i(1, 60, 0), STONE);
            chest.with(OAK_FENCE, 64);

            assertThat(GATE.act(byHand())).isTrue();

            assertThat(world.count(new Vec3i(1, 61, 0), new Vec3i(1, 64, 0), "oak_fence"))
                    .isEqualTo(4);
        }

        @Test
        void doesNotReachFurtherThanTheSettingsAllow() {
            Settings close = Settings.builder()
                    .mechanics(Settings.DEFAULTS.mechanics().withGateRadius(1))
                    .build();
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);

            boolean acted = GATE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(),
                    world,
                    close,
                    SimpleActor.at(new Vec3i(2, 64, 1))));

            assertThat(acted).isFalse();
        }
    }

    @Nested
    @DisplayName("what it will not take for a gate")
    class WhatItWillNotTakeForAGate {

        @Test
        void aRunOfFenceWithOpenSkyAboveIt() {
            world.withSign(SIGN, BlockFace.SOUTH, "", "[Gate]", "", "")
                    .with(HANGING, OAK_FENCE)
                    .with(GROUND, STONE);
            chest.with(OAK_FENCE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(2, 64, 1));

            boolean acted = GATE.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("no gate")).isTrue();
        }
    }

    @Nested
    @DisplayName("driven by redstone")
    class DrivenByRedstone {

        @Test
        void closesWhenPowerArrives() {
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);

            GATE.act(byRedstone(true));

            assertThat(curtain("oak_fence")).isEqualTo(6);
        }

        @Test
        void opensWhenPowerLeaves() {
            withGate("[Gate]", OAK_FENCE);
            world.filling(CURTAIN_BOTTOM, CURTAIN_TOP, "oak_fence");

            GATE.act(byRedstone(false));

            assertThat(curtain("oak_fence")).isZero();
        }

        @Test
        void staysClosedWhenPowerArrivesAgain() {
            withGate("[Gate]", OAK_FENCE);
            chest.with(OAK_FENCE, 64);

            GATE.act(byRedstone(true));
            GATE.act(byRedstone(true));

            assertThat(curtain("oak_fence")).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("answering to a hand on its own fence")
    class AnsweringToAHandOnItsOwnFence {

        @Test
        void onlyWhenTheSignAsksForIt() {
            PostedSign plain = new PostedSign(
                    SIGN,
                    com.xeonproductions.craftbookultimate.core.sign.SignLines
                            .of("", "[Gate]", "", ""),
                    BlockFace.SOUTH);

            assertThat(GATE.answersToTouchOn(plain, OAK_FENCE, Settings.DEFAULTS)).isFalse();
        }

        @Test
        void whenTheSignEndsInTheClickableLetter() {
            PostedSign clickable = new PostedSign(
                    SIGN,
                    com.xeonproductions.craftbookultimate.core.sign.SignLines
                            .of("", "[Gate]C", "", ""),
                    BlockFace.SOUTH);

            assertThat(GATE.answersToTouchOn(clickable, OAK_FENCE, Settings.DEFAULTS)).isTrue();
        }

        @Test
        void andOnlyForItsOwnMaterial() {
            PostedSign clickable = new PostedSign(
                    SIGN,
                    com.xeonproductions.craftbookultimate.core.sign.SignLines
                            .of("", "[GlassGate]C", "", ""),
                    BlockFace.SOUTH);

            assertThat(GATE.answersToTouchOn(clickable, GLASS_PANE, Settings.DEFAULTS)).isTrue();
            assertThat(GATE.answersToTouchOn(clickable, OAK_FENCE, Settings.DEFAULTS)).isFalse();
        }

        @Test
        void notAtAllWhenTheSettingsForbidIt() {
            Settings noClicking = Settings.builder()
                    .mechanics(Settings.DEFAULTS.mechanics().withGateClicking(false))
                    .build();
            PostedSign clickable = new PostedSign(
                    SIGN,
                    com.xeonproductions.craftbookultimate.core.sign.SignLines
                            .of("", "[Gate]C", "", ""),
                    BlockFace.SOUTH);

            assertThat(GATE.answersToTouchOn(clickable, OAK_FENCE, noClicking)).isFalse();
        }
    }

    @Test
    void everyKindOfGateSignHasAClickableFormAsWell() {
        assertThat(GATE.signNames()).contains(
                "[Gate]", "[Gate]C",
                "[DGate]", "[DGate]C",
                "[GlassGate]", "[GlassGate]C",
                "[GlassDGate]", "[GlassDGate]C",
                "[IronGate]", "[IronGate]C",
                "[IronDGate]", "[IronDGate]C",
                "[NetherGate]", "[NetherGate]C",
                "[NetherDGate]", "[NetherDGate]C");
    }
}
