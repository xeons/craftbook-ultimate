package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleDroppedItem;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Planting chips")
class FarmingTest {

    private static final Key WHEAT = Blocks.key("wheat");
    private static final Key SEEDS = Blocks.key("wheat_seeds");

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    /** The plot directly above the block the sign hangs on. */
    private static final Vec3i FIRST_PLOT = BEHIND.offset(BlockFace.UP);

    /** A plot further from the sign, along the field. */
    private static Vec3i plot(int stepsAway) {
        return new Vec3i(0, 65, -stepsAway);
    }

    @Nested
    @DisplayName("planter")
    class Planter {

        private SimpleChipState.Builder chip(SimpleChipWorld world, String item, String height) {
            return SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("PLANTER", "[MCX216]", item, height);
        }

        @Test
        void plantsWhatIsLyingNearby() {
            SimpleDroppedItem dropped = SimpleDroppedItem.of("wheat_seeds", 3);
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(FIRST_PLOT)
                    .withDroppedItem(FIRST_PLOT, dropped);
            SimpleChipState state = chip(world, "wheat_seeds", "").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(WHEAT);
            assertThat(dropped.count()).isEqualTo(2);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void needsItsInputWhenItIsNotTicking() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(FIRST_PLOT)
                    .withDroppedItem(FIRST_PLOT, SimpleDroppedItem.of("wheat_seeds", 3));
            SimpleChipState state = chip(world, "wheat_seeds", "")
                    .inputs(false, false, false, false)
                    .build();

            Farming.planter().trigger(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void plantsOnThePulseWhenItIsNotTicking() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(FIRST_PLOT)
                    .withDroppedItem(FIRST_PLOT, SimpleDroppedItem.of("wheat_seeds", 3));
            SimpleChipState state = chip(world, "wheat_seeds", "")
                    .inputs(true, false, false, false)
                    .build();

            Farming.planter().trigger(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(WHEAT);
        }

        @Test
        void ignoresItemsItWasNotToldToPlant() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(FIRST_PLOT)
                    .withDroppedItem(FIRST_PLOT, SimpleDroppedItem.of("carrot", 3));
            SimpleChipState state = chip(world, "wheat_seeds", "").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(Blocks.AIR_KEY);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void leavesTheSeedAloneWhereItWouldNotGrow() {
            SimpleDroppedItem dropped = SimpleDroppedItem.of("wheat_seeds", 3);
            SimpleChipWorld world = new SimpleChipWorld().withDroppedItem(FIRST_PLOT, dropped);
            SimpleChipState state = chip(world, "wheat_seeds", "").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(Blocks.AIR_KEY);
            assertThat(dropped.count()).isEqualTo(3);
        }

        @Test
        void willNotPlantThroughSomethingAlreadyThere() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(FIRST_PLOT, "stone")
                    .withPlantable(FIRST_PLOT)
                    .withDroppedItem(FIRST_PLOT, SimpleDroppedItem.of("wheat_seeds", 3));
            SimpleChipState state = chip(world, "wheat_seeds", "").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(Blocks.key("stone"));
        }

        @Test
        void readsHowFarAboveItsSupportToPlant() {
            Vec3i higher = BEHIND.add(0, 3, 0);
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(higher)
                    .withDroppedItem(higher, SimpleDroppedItem.of("wheat_seeds", 1));
            SimpleChipState state = chip(world, "wheat_seeds", "3").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(higher)).isEqualTo(WHEAT);
        }

        @Test
        void neverPlantsInsideItsOwnSupport() {
            // A height below one would put the plant in the block the sign hangs on.
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(FIRST_PLOT)
                    .withDroppedItem(FIRST_PLOT, SimpleDroppedItem.of("wheat_seeds", 1));
            SimpleChipState state = chip(world, "wheat_seeds", "0").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(WHEAT);
        }

        @Test
        void hangsCocoaOffTheSideOfWhateverWillTakeIt() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(FIRST_PLOT)
                    .withAcceptedFacing(BlockFace.EAST)
                    .withDroppedItem(FIRST_PLOT, SimpleDroppedItem.of("cocoa_beans", 1));
            SimpleChipState state = chip(world, "cocoa_beans", "").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(Blocks.key("cocoa"));
            assertThat(world.facingAt(FIRST_PLOT)).contains(BlockFace.EAST);
        }

        @Test
        void doesNothingForAnItemThatCannotBePlanted() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(FIRST_PLOT)
                    .withDroppedItem(FIRST_PLOT, SimpleDroppedItem.of("stone", 3));
            SimpleChipState state = chip(world, "stone", "").build();

            Farming.planter().tick(state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(Blocks.AIR_KEY);
        }
    }

    @Nested
    @DisplayName("area planter")
    class AreaPlanter {

        private SimpleChipState.Builder chip(SimpleChipWorld world, String item, String field) {
            return SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("AREA PLANTER", "[MCX215]", item, field);
        }

        private SimpleChipWorld fieldOfThree() {
            return new SimpleChipWorld()
                    .withPlantable(plot(1))
                    .withPlantable(plot(2))
                    .withPlantable(plot(3));
        }

        /** Steps a ticking chip far enough for it to make a pass over its field. */
        private void tickUntilItSows(SelfTriggeringICLogic chip, SimpleChipState state) {
            for (int i = 0; i < 20; i++) {
                chip.tick(state);
            }
        }

        @Test
        void sowsEveryPlotItCanReach() {
            SimpleChipWorld world = fieldOfThree()
                    .withDroppedItem(SIGN, SimpleDroppedItem.of("wheat_seeds", 8));
            SimpleChipState state = chip(world, "wheat_seeds", "1:3")
                    .inputs(false, false, false, false)
                    .build();

            tickUntilItSows(Farming.areaPlanter(), state);

            assertThat(world.blockAt(plot(1))).isEqualTo(WHEAT);
            assertThat(world.blockAt(plot(2))).isEqualTo(WHEAT);
            assertThat(world.blockAt(plot(3))).isEqualTo(WHEAT);
        }

        @Test
        void startsDirectlyAboveTheBlockTheSignHangsOn() {
            SimpleChipWorld world = fieldOfThree()
                    .withDroppedItem(SIGN, SimpleDroppedItem.of("wheat_seeds", 8));
            SimpleChipState state = chip(world, "wheat_seeds", "1:3")
                    .inputs(false, false, false, false)
                    .build();

            tickUntilItSows(Farming.areaPlanter(), state);

            assertThat(world.blockAt(FIRST_PLOT)).isEqualTo(WHEAT);
        }

        @Test
        void sowsNoMoreThanItHasSeedFor() {
            SimpleDroppedItem dropped = SimpleDroppedItem.of("wheat_seeds", 2);
            SimpleChipWorld world = fieldOfThree().withDroppedItem(SIGN, dropped);
            SimpleChipState state = chip(world, "wheat_seeds", "1:3")
                    .inputs(false, false, false, false)
                    .build();

            tickUntilItSows(Farming.areaPlanter(), state);

            assertThat(world.blockAt(plot(1))).isEqualTo(WHEAT);
            assertThat(world.blockAt(plot(2))).isEqualTo(WHEAT);
            assertThat(world.blockAt(plot(3))).isEqualTo(Blocks.AIR_KEY);
            assertThat(dropped.count()).isZero();
        }

        @Test
        void takesItsTimeBetweenPasses() {
            SimpleChipWorld world = fieldOfThree()
                    .withDroppedItem(SIGN, SimpleDroppedItem.of("wheat_seeds", 8));
            SimpleChipState state = chip(world, "wheat_seeds", "1:3")
                    .inputs(false, false, false, false)
                    .build();

            SelfTriggeringICLogic chip = Farming.areaPlanter();
            for (int i = 0; i < 19; i++) {
                chip.tick(state);
            }

            assertThat(world.blockAt(plot(1))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void stopsSowingWhileSomethingDrivesIt() {
            // The input is a brake on a ticking area planter, not a trigger.
            SimpleChipWorld world = fieldOfThree()
                    .withDroppedItem(SIGN, SimpleDroppedItem.of("wheat_seeds", 8));
            SimpleChipState state = chip(world, "wheat_seeds", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            tickUntilItSows(Farming.areaPlanter(), state);

            assertThat(world.blockAt(plot(1))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void sowsOnThePulseWhenItIsNotTicking() {
            SimpleChipWorld world = fieldOfThree()
                    .withDroppedItem(SIGN, SimpleDroppedItem.of("wheat_seeds", 8));
            SimpleChipState state = chip(world, "wheat_seeds", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            Farming.areaPlanter().trigger(state);

            assertThat(world.blockAt(plot(1))).isEqualTo(WHEAT);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void skipsPlotsThatAreAlreadyPlanted() {
            SimpleDroppedItem dropped = SimpleDroppedItem.of("wheat_seeds", 8);
            SimpleChipWorld world = fieldOfThree()
                    .withBlock(plot(2), WHEAT)
                    .withDroppedItem(SIGN, dropped);
            SimpleChipState state = chip(world, "wheat_seeds", "1:3")
                    .inputs(false, false, false, false)
                    .build();

            tickUntilItSows(Farming.areaPlanter(), state);

            assertThat(dropped.count()).isEqualTo(6);
        }

        @Test
        void doesNothingWithoutAFieldOnItsSign() {
            SimpleChipWorld world = fieldOfThree()
                    .withDroppedItem(SIGN, SimpleDroppedItem.of("wheat_seeds", 8));
            SimpleChipState state = chip(world, "wheat_seeds", "3")
                    .inputs(false, false, false, false)
                    .build();

            tickUntilItSows(Farming.areaPlanter(), state);

            assertThat(world.blockAt(plot(1))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void widensAcrossTheSignsColumn() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withPlantable(new Vec3i(-1, 65, -1))
                    .withPlantable(new Vec3i(0, 65, -1))
                    .withPlantable(new Vec3i(1, 65, -1))
                    .withDroppedItem(SIGN, SimpleDroppedItem.of("wheat_seeds", 8));
            SimpleChipState state = chip(world, "wheat_seeds", "3:1")
                    .inputs(false, false, false, false)
                    .build();

            tickUntilItSows(Farming.areaPlanter(), state);

            assertThat(world.blockAt(new Vec3i(-1, 65, -1))).isEqualTo(WHEAT);
            assertThat(world.blockAt(new Vec3i(0, 65, -1))).isEqualTo(WHEAT);
            assertThat(world.blockAt(new Vec3i(1, 65, -1))).isEqualTo(WHEAT);
        }
    }

    @Test
    void knowsWhatEachSeedBecomes() {
        assertThat(com.xeonproductions.craftbookultimate.core.farm.Plantables.plantedForm(SEEDS))
                .contains(WHEAT);
        assertThat(com.xeonproductions.craftbookultimate.core.farm.Plantables.plantedForm(
                        Blocks.key("oak_sapling")))
                .contains(Blocks.key("oak_sapling"));
        assertThat(com.xeonproductions.craftbookultimate.core.farm.Plantables.plantedForm(
                        Blocks.key("stone")))
                .isEmpty();
    }
}
