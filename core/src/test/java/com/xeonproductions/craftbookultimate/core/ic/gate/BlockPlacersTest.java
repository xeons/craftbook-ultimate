package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpiles;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Block placing chips")
class BlockPlacersTest {

    private static final Key STONE = Blocks.key("stone");

    /** A south-facing sign, so the area runs north away from its front. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static SimpleChipState.Builder chip(
            SimpleChipWorld world, SimpleStockpile stockpile, String block, String dimensions) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .stockpile(stockpile)
                .sign("BRIDGE", "[MCX207]", block, dimensions);
    }

    /** A block in the area, measured north of the sign and across it. */
    private static Vec3i inArea(int stepsAway, int across, int up) {
        return new Vec3i(across, 64 + up, -stepsAway);
    }

    @Nested
    @DisplayName("bridge")
    class Bridge {

        @Test
        void fillsTheAreaWhileDriven() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 64);
            SimpleChipState state = chip(world, stockpile, "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(3, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(4, 0, 0))).isEqualTo(STONE);
        }

        @Test
        void startsPastTheBlockTheSignHangsOn() {
            // The sign's own support must never be built over.
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(1, 0, 0))).isEqualTo(Blocks.AIR_KEY);
            assertThat(world.blockAt(SIGN)).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void centresItsWidthOnTheSign() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "3:1")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, -1, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(2, 1, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(2, 2, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void appliesItsVerticalOffset() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "1:1:-1")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, -1))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void paysForWhatItPlaces() {
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 64);
            SimpleChipState state = chip(new SimpleChipWorld(), stockpile, "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(stockpile.count(STONE)).isEqualTo(61);
        }

        @Test
        void buildsOnlyWhatItCanAfford() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 2);
            SimpleChipState state = chip(world, stockpile, "stone", "1:5")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(stockpile.count(STONE)).isZero();
            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(3, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(4, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void buildsNothingWithNoMaterials() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty(), "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void buildsThroughWaterButNotThroughSolidBlocks() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(inArea(2, 0, 0), "water")
                    .withBlock(inArea(3, 0, 0), "obsidian");
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(3, 0, 0))).isEqualTo(Blocks.key("obsidian"));
            assertThat(world.blockAt(inArea(4, 0, 0))).isEqualTo(STONE);
        }
    }

    @Nested
    @DisplayName("retracting")
    class Retracting {

        private SimpleChipState built(SimpleChipWorld world, SimpleStockpile stockpile) {
            return chip(world, stockpile, "stone", "1:3")
                    .inputs(false, false, false, false)
                    .build();
        }

        @Test
        void takesItsOwnBlocksAwayWhenIdle() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(inArea(2, 0, 0), "stone")
                    .withBlock(inArea(3, 0, 0), "stone");
            SimpleChipState state = built(world, SimpleStockpile.empty());

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
            assertThat(world.blockAt(inArea(3, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void givesTheBlocksBack() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(inArea(2, 0, 0), "stone")
                    .withBlock(inArea(3, 0, 0), "stone");
            SimpleStockpile stockpile = SimpleStockpile.empty();

            BlockPlacers.bridge(false).trigger(built(world, stockpile));

            assertThat(stockpile.count(STONE)).isEqualTo(2);
        }

        @Test
        void leavesBlocksItDidNotPlace() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(inArea(2, 0, 0), "stone")
                    .withBlock(inArea(3, 0, 0), "obsidian");
            SimpleStockpile stockpile = SimpleStockpile.empty();

            BlockPlacers.bridge(false).trigger(built(world, stockpile));

            assertThat(world.blockAt(inArea(3, 0, 0))).isEqualTo(Blocks.key("obsidian"));
            assertThat(stockpile.count(STONE)).isEqualTo(1);
        }

        @Test
        void leavesBlocksInPlaceWhenThereIsNowhereToPutThem() {
            // Destroying materials because the chests are full would lose a player's blocks.
            SimpleChipWorld world = new SimpleChipWorld().withBlock(inArea(2, 0, 0), "stone");
            SimpleChipState state = chip(world, SimpleStockpile.empty(), "stone", "1:3")
                    .inputs(false, false, false, false)
                    .stockpile(Stockpiles.empty())
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
        }
    }

    @Nested
    @DisplayName("door")
    class Door {

        @Test
        void buildsUpwardRatherThanAway() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(SimpleStockpile.empty().with(STONE, 64))
                    .sign("DOOR", "[MCX208]", "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.door(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(2, 0, 1))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(2, 0, 2))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(3, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }
    }

    @Nested
    @DisplayName("authorisation")
    class Authorisation {

        private SimpleChipState unauthorised(SimpleChipWorld world) {
            return SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(SimpleStockpile.empty().with(STONE, 64))
                    .sign("BRIDGE", "[MCX207]*", "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();
        }

        @Test
        void refusesWhileItsAreaHoldsTheBlockItWouldPlace() {
            // Otherwise a chip could be dropped over someone's wall and used to mine it.
            SimpleChipWorld world = new SimpleChipWorld().withBlock(inArea(3, 0, 0), "stone");
            SimpleChipState state = unauthorised(world);

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
            assertThat(state.sign().trimmedText(1)).isEqualTo("[MCX207]*");
        }

        @Test
        void authorisesItselfOnceItsAreaIsClear() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = unauthorised(world);

            BlockPlacers.bridge(false).trigger(state);

            assertThat(state.sign().trimmedText(1)).isEqualTo("[MCX207]");
            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
        }

        @Test
        void aForcingChipSkipsTheCheckEntirely() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(inArea(3, 0, 0), "stone");
            SimpleChipState state = unauthorised(world);

            BlockPlacers.bridge(true).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(STONE);
        }
    }

    @Nested
    @DisplayName("refusing to act")
    class RefusingToAct {

        private void assertDoesNothing(String block, String dimensions) {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), block, dimensions)
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void withoutAUsableBlockName() {
            assertDoesNothing("", "1:3");
            assertDoesNothing("   ", "1:3");
        }

        @Test
        void withoutUsableDimensions() {
            assertDoesNothing("stone", "");
            assertDoesNothing("stone", "3");
            assertDoesNothing("stone", "banana:3");
        }

        @Test
        void withDimensionsThatAreNotPositive() {
            assertDoesNothing("stone", "0:3");
            assertDoesNothing("stone", "3:-1");
        }

        @Test
        void whenPartOfItsAreaIsNotLoaded() {
            SimpleChipWorld world = new SimpleChipWorld().withUnloaded(inArea(4, 0, 0));
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "1:3")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }
    }

    @Test
    void anUnlimitedStockpileBuildsWithoutMaterials() {
        SimpleChipWorld world = new SimpleChipWorld();
        SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .stockpile(Stockpiles.unlimited())
                .sign("BRIDGE", "[MC1207]", "stone", "1:3")
                .inputs(true, false, false, false)
                .build();

        ICLogic bridge = BlockPlacers.bridge(false);
        bridge.trigger(state);

        assertThat(world.blockAt(inArea(4, 0, 0))).isEqualTo(STONE);
    }

    @Nested
    @DisplayName("harvester")
    class Harvester {

        private static final Key WHEAT = Blocks.key("wheat");
        private static final Key SEEDS = Blocks.key("wheat_seeds");

        private SimpleChipState.Builder chip(
                SimpleChipWorld world, SimpleStockpile stockpile, String area) {
            return SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(stockpile)
                    .sign("HARVESTER", "[MCX213]", "wheat", area);
        }

        /** A row of three grown crops, which is where a 1:3:1 area sits. */
        private SimpleChipWorld rowOfWheat() {
            return new SimpleChipWorld()
                    .withBlock(inArea(2, 0, 1), WHEAT)
                    .withBlock(inArea(3, 0, 1), WHEAT)
                    .withBlock(inArea(4, 0, 1), WHEAT);
        }

        @Test
        void gathersTheCropWhenItsInputDrops() {
            SimpleChipWorld world = rowOfWheat();
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "1:3:1")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 1))).isEqualTo(Blocks.AIR_KEY);
            assertThat(world.blockAt(inArea(4, 0, 1))).isEqualTo(Blocks.AIR_KEY);
            assertThat(stockpile.count(WHEAT)).isEqualTo(3);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void gathersNothingWhileItIsDriven() {
            SimpleChipWorld world = rowOfWheat();
            SimpleChipState state = chip(world, SimpleStockpile.empty(), "1:3:1")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 1))).isEqualTo(WHEAT);
        }

        @Test
        void neverPutsAnythingBack() {
            // Unlike a bridge it only ever takes, so a cleared area stays cleared.
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile stockpile = SimpleStockpile.empty().with(WHEAT, 64);
            SimpleChipState state = chip(world, stockpile, "1:3:1")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.placedBlockCount()).isZero();
            assertThat(stockpile.count(WHEAT)).isEqualTo(64);
        }

        @Test
        void leavesWhatHasNotFinishedGrowing() {
            SimpleChipWorld world = rowOfWheat().withGrowing(inArea(3, 0, 1));
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "1:3:1")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(3, 0, 1))).isEqualTo(WHEAT);
            assertThat(stockpile.count(WHEAT)).isEqualTo(2);
        }

        @Test
        void putsAwayWhatTheCropActuallyDrops() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(inArea(2, 0, 1), WHEAT)
                    .withDrops(inArea(2, 0, 1), Map.of(WHEAT, 1, SEEDS, 2));
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "1:1:1")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(stockpile.count(WHEAT)).isEqualTo(1);
            assertThat(stockpile.count(SEEDS)).isEqualTo(2);
        }

        @Test
        void leavesACropStandingWithNowhereToPutIt() {
            SimpleChipWorld world = rowOfWheat();
            SimpleChipState state = chip(world, SimpleStockpile.withCapacity(0), "1:3:1")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 1))).isEqualTo(WHEAT);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresAnythingThatIsNotTheCropItWasToldAbout() {
            SimpleChipWorld world = rowOfWheat().withBlock(inArea(3, 0, 1), STONE);
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "1:3:1")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(3, 0, 1))).isEqualTo(STONE);
            assertThat(stockpile.count(WHEAT)).isEqualTo(2);
        }

        @Test
        void sitsJustAboveTheSignUnlessToldOtherwise() {
            // The area's default vertical offset is one, not zero.
            SimpleChipWorld world = new SimpleChipWorld().withBlock(inArea(2, 0, 0), WHEAT);
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "1:1:1")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(WHEAT);
        }

        @Test
        void takesItsVerticalOffsetAfterASlash() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(inArea(2, 0, 0), WHEAT);
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "1:1:1/0")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
            assertThat(stockpile.count(WHEAT)).isEqualTo(1);
        }

        @Test
        void refusesAnAreaItCannotRead() {
            SimpleChipWorld world = rowOfWheat();
            SimpleChipState state = chip(world, SimpleStockpile.empty(), "1:3")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 1))).isEqualTo(WHEAT);
        }

        @Test
        void waitsUntilItsAreaIsClearWhenItIsUnauthorised() {
            SimpleChipWorld world = rowOfWheat();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(SimpleStockpile.empty())
                    .sign("HARVESTER", "[MCX213]*", "wheat", "1:3:1")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 1))).isEqualTo(WHEAT);
        }
    }

    @Nested
    @DisplayName("flex set")
    class FlexSet {

        /** One above the block the sign hangs on, which is where {@code Y+1} points. */
        private static final Vec3i TARGET = new Vec3i(0, 65, -1);

        private SimpleChipState.Builder chip(
                SimpleChipWorld world, SimpleStockpile stockpile, String config, String hold) {
            return SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(stockpile)
                    .sign("FLEX SET", "[MCX206]", config, hold);
        }

        @Test
        void readsAnOffsetAndABlockFromOneLine() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 4);
            SimpleChipState state = chip(world, stockpile, "Y+1:stone", "")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(STONE);
            assertThat(stockpile.count(STONE)).isEqualTo(3);
        }

        @Test
        void leavesTheBlockWhereItIsWhenTheInputDrops() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, STONE);
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "Y+1:stone", "")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(STONE);
        }

        @Test
        void takesTheBlockBackWhenToldToHold() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, STONE);
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "Y+1:stone", "h")
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(Blocks.AIR_KEY);
            assertThat(stockpile.count(STONE)).isEqualTo(1);
        }

        @Test
        void onlyFillsAir() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, Blocks.key("dirt"));
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 4);
            SimpleChipState state = chip(world, stockpile, "Y+1:stone", "")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(Blocks.key("dirt"));
            assertThat(stockpile.count(STONE)).isEqualTo(4);
        }

        @Test
        void doesNothingWithoutTheBlockToPlace() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty(), "Y+1:stone", "")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void reachesAlongWhicheverAxisItWasGiven() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 4), "X-2:stone", "")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.blockAt(new Vec3i(-2, 64, -1))).isEqualTo(STONE);
        }

        @Test
        void refusesALineWithoutBothHalves() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 4), "stone", "")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.placedBlockCount()).isZero();
        }

        @Test
        void replacesWhatIsThereWithoutPayingWhenItIsTheAdminVariant() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, Blocks.key("dirt"));
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "Y+1:stone", "")
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.flexSetAdmin().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(STONE);
            assertThat(stockpile.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("respecting the settings")
    class RespectingTheSettings {

        private static final Key DIRT = Blocks.key("dirt");

        @Test
        void buildsNoFurtherThanTheSettingsAllow() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "1:9")
                    .settings(Settings.builder().maxLength(4).build())
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(5, 0, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(6, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void buildsNoWiderThanTheSettingsAllow() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "9:1")
                    .settings(Settings.builder().maxWidth(3).build())
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 1, 0))).isEqualTo(STONE);
            assertThat(world.blockAt(inArea(2, 2, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void placesNothingTheSettingsDoNotAllow() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, SimpleStockpile.empty().with(STONE, 64), "stone", "1:3")
                    .settings(Settings.builder().placeableBlocks(Set.of(DIRT)).build())
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void stillTakesAwayABlockTheSettingsNoLongerAllow() {
            // A block struck off the list leaves the bridges already made of it able to retract,
            // rather than stuck out with no way of bringing them in.
            SimpleChipWorld world = new SimpleChipWorld().withBlock(inArea(2, 0, 0), STONE);
            SimpleStockpile stockpile = SimpleStockpile.empty();
            SimpleChipState state = chip(world, stockpile, "stone", "1:3")
                    .settings(Settings.builder().placeableBlocks(Set.of(DIRT)).build())
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.bridge(false).trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 0))).isEqualTo(Blocks.AIR_KEY);
            assertThat(stockpile.count(STONE)).isEqualTo(1);
        }

        @Test
        void harvestsNoFurtherThanTheSettingsAllow() {
            Key wheat = Blocks.key("wheat");
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(inArea(2, 0, 1), wheat)
                    .withBlock(inArea(3, 0, 1), wheat)
                    .withBlock(inArea(4, 0, 1), wheat);
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(SimpleStockpile.empty())
                    .sign("HARVESTER", "[MCX213]", "wheat", "1:3:1")
                    .settings(Settings.builder().maxLength(2).build())
                    .inputs(false, false, false, false)
                    .build();

            BlockPlacers.harvester().trigger(state);

            assertThat(world.blockAt(inArea(2, 0, 1))).isEqualTo(Blocks.AIR_KEY);
            assertThat(world.blockAt(inArea(4, 0, 1))).isEqualTo(wheat);
        }

        @Test
        void aFlexSetPlacesNothingTheSettingsDoNotAllow() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(SimpleStockpile.empty().with(STONE, 4))
                    .sign("FLEX SET", "[MCX206]", "Y+1:stone", "")
                    .settings(Settings.builder().placeableBlocks(Set.of(DIRT)).build())
                    .inputs(true, false, false, false)
                    .build();

            BlockPlacers.flexSet().trigger(state);

            assertThat(world.blockAt(new Vec3i(0, 65, -1))).isEqualTo(Blocks.AIR_KEY);
        }
    }
}
