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

@DisplayName("The door")
class DoorTest {

    private static final Door DOOR = new Door();

    private static final Key COBBLESTONE = Blocks.key("cobblestone");

    /** The lower sign, facing south, so the frame runs east and west of somebody reading it. */
    private static final Vec3i LOWER_SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i UPPER_SIGN = new Vec3i(0, 70, 0);

    /** The frame blocks the panel hangs between. */
    private static final Vec3i BOTTOM = new Vec3i(0, 65, 0);

    private static final Vec3i TOP = new Vec3i(0, 69, 0);

    private static final Vec3i PANEL_BOTTOM = new Vec3i(0, 66, 0);
    private static final Vec3i PANEL_TOP = new Vec3i(0, 68, 0);

    private final SimpleStockpile chest = SimpleStockpile.empty();
    private final SimpleMechanicWorld world = new SimpleMechanicWorld().withStockpile(chest);

    /** A doorway worked from the bottom, three blocks of gap between its two frames. */
    private SimpleMechanicWorld withDoor() {
        return world.withSign(LOWER_SIGN, BlockFace.SOUTH, "", Door.UP, "", "")
                .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Door.END, "", "")
                .with(BOTTOM, COBBLESTONE)
                .with(TOP, COBBLESTONE);
    }

    private MechanicVisit at(Vec3i sign) {
        return MechanicVisit.byHand(
                world.signAt(sign).orElseThrow(),
                world,
                Settings.DEFAULTS,
                SimpleActor.at(new Vec3i(0, 64, 1)));
    }

    private int panelBlocks() {
        return world.count(PANEL_BOTTOM, PANEL_TOP, "cobblestone");
    }

    @Nested
    @DisplayName("filling the doorway")
    class FillingTheDoorway {

        @Test
        void putsAPanelBetweenTheTwoFrames() {
            withDoor();
            chest.with(COBBLESTONE, 3);

            assertThat(DOOR.act(at(LOWER_SIGN))).isTrue();

            assertThat(panelBlocks()).isEqualTo(3);
        }

        @Test
        void isWorkedFromTheTopJustAsWell() {
            world.withSign(LOWER_SIGN, BlockFace.SOUTH, "", Door.END, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Door.DOWN, "", "")
                    .with(BOTTOM, COBBLESTONE)
                    .with(TOP, COBBLESTONE);
            chest.with(COBBLESTONE, 64);

            assertThat(DOOR.act(at(UPPER_SIGN))).isTrue();

            assertThat(panelBlocks()).isEqualTo(3);
        }

        @Test
        void widensToMatchTheFramesAtBothEnds() {
            withDoor();
            world.with(BOTTOM.offset(BlockFace.EAST), COBBLESTONE)
                    .with(TOP.offset(BlockFace.EAST), COBBLESTONE)
                    .with(BOTTOM.offset(BlockFace.WEST), COBBLESTONE)
                    .with(TOP.offset(BlockFace.WEST), COBBLESTONE);
            chest.with(COBBLESTONE, 64);

            DOOR.act(at(LOWER_SIGN));

            assertThat(world.count(new Vec3i(-1, 66, 0), new Vec3i(1, 68, 0), "cobblestone"))
                    .isEqualTo(9);
        }

        @Test
        void paysForThePanelOutOfTheChest() {
            withDoor();
            chest.with(COBBLESTONE, 10);

            DOOR.act(at(LOWER_SIGN));

            assertThat(chest.count(COBBLESTONE)).isEqualTo(7);
        }

        @Test
        void leavesTheDoorwayOpenWhenTheChestIsOneShort() {
            withDoor();
            chest.with(COBBLESTONE, 2);

            assertThat(DOOR.act(at(LOWER_SIGN))).isFalse();

            assertThat(panelBlocks()).isZero();
        }
    }

    @Nested
    @DisplayName("clearing the doorway")
    class ClearingTheDoorway {

        @Test
        void takesThePanelAwayAgain() {
            withDoor();
            world.filling(PANEL_BOTTOM, PANEL_TOP, "cobblestone");

            assertThat(DOOR.act(at(LOWER_SIGN))).isTrue();

            assertThat(panelBlocks()).isZero();
            assertThat(chest.count(COBBLESTONE)).isEqualTo(3);
        }

        @Test
        void leavesTheFramesWhereTheyAre() {
            withDoor();
            world.filling(PANEL_BOTTOM, PANEL_TOP, "cobblestone");

            DOOR.act(at(LOWER_SIGN));

            assertThat(world.blockAt(BOTTOM)).isEqualTo(COBBLESTONE);
            assertThat(world.blockAt(TOP)).isEqualTo(COBBLESTONE);
        }
    }

    @Nested
    @DisplayName("what it refuses")
    class WhatItRefuses {

        @Test
        void doesNothingFromASignThatOnlyMarksAnEnd() {
            withDoor();
            chest.with(COBBLESTONE, 64);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 70, 1));

            boolean acted = DOOR.act(MechanicVisit.byHand(
                    world.signAt(UPPER_SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(panelBlocks()).isZero();
            assertThat(who.wasTold("which way it runs")).isTrue();
        }

        @Test
        void saysSoWhenTheOtherEndIsMissing() {
            world.withSign(LOWER_SIGN, BlockFace.SOUTH, "", Door.UP, "", "")
                    .with(BOTTOM, COBBLESTONE);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = DOOR.act(MechanicVisit.byHand(
                    world.signAt(LOWER_SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("missing")).isTrue();
        }

        @Test
        void saysSoWhenTheTwoFramesAreDifferentMaterials() {
            withDoor();
            world.with(TOP, Blocks.key("oak_planks"));
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = DOOR.act(MechanicVisit.byHand(
                    world.signAt(LOWER_SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("same thing")).isTrue();
        }

        @Test
        void saysSoWhenTheTwoSignsLeaveNoRoomBetweenThem() {
            world.withSign(LOWER_SIGN, BlockFace.SOUTH, "", Door.UP, "", "")
                    .withSign(new Vec3i(0, 66, 0), BlockFace.SOUTH, "", Door.END, "", "")
                    .with(BOTTOM, COBBLESTONE);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = DOOR.act(MechanicVisit.byHand(
                    world.signAt(LOWER_SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("no room")).isTrue();
        }

        @Test
        void refusesToFillOverSomethingInTheWay() {
            withDoor();
            world.with(new Vec3i(0, 67, 0), Blocks.key("dirt"));
            chest.with(COBBLESTONE, 64);

            assertThat(DOOR.act(at(LOWER_SIGN))).isFalse();

            assertThat(panelBlocks()).isZero();
        }
    }

    @Nested
    @DisplayName("driven by redstone")
    class DrivenByRedstone {

        @Test
        void shutsWhenPowerArrives() {
            withDoor();
            chest.with(COBBLESTONE, 64);

            DOOR.act(MechanicVisit.byRedstone(
                    world.signAt(LOWER_SIGN).orElseThrow(), world, Settings.DEFAULTS, true));

            assertThat(panelBlocks()).isEqualTo(3);
        }

        @Test
        void opensWhenPowerLeaves() {
            withDoor();
            world.filling(PANEL_BOTTOM, PANEL_TOP, "cobblestone");

            DOOR.act(MechanicVisit.byRedstone(
                    world.signAt(LOWER_SIGN).orElseThrow(), world, Settings.DEFAULTS, false));

            assertThat(panelBlocks()).isZero();
        }
    }

    @Test
    void isNoWiderThanTheSettingsAllow() {
        Settings narrow = Settings.builder().maxWidth(1).build();
        withDoor();
        world.with(BOTTOM.offset(BlockFace.EAST), COBBLESTONE)
                .with(TOP.offset(BlockFace.EAST), COBBLESTONE)
                .with(BOTTOM.offset(BlockFace.EAST, 2), COBBLESTONE)
                .with(TOP.offset(BlockFace.EAST, 2), COBBLESTONE);
        chest.with(COBBLESTONE, 64);

        DOOR.act(MechanicVisit.byHand(
                world.signAt(LOWER_SIGN).orElseThrow(),
                world,
                narrow,
                SimpleActor.at(new Vec3i(0, 64, 1))));

        assertThat(world.count(new Vec3i(1, 66, 0), new Vec3i(1, 68, 0), "cobblestone"))
                .isEqualTo(3);
        assertThat(world.count(new Vec3i(2, 66, 0), new Vec3i(2, 68, 0), "cobblestone"))
                .isZero();
    }
}
