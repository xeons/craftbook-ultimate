// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.Map;
import java.util.random.RandomGenerator;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Terrain and liquid chips")
class TerrainTest {

    private static final Key STONE = Blocks.key("stone");
    private static final Key BUCKET = Key.key("bucket");
    private static final Key WATER_BUCKET = Key.key("water_bucket");
    private static final Key LAVA_BUCKET = Key.key("lava_bucket");
    private static final Key BONEMEAL = Key.key("bone_meal");

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BACK = new Vec3i(0, 64, -1);
    private static final Vec3i ABOVE = new Vec3i(0, 65, -1);
    private static final Vec3i BELOW = new Vec3i(0, 63, -1);

    /** A generator that always draws zero, so a chip picking a spot picks a known one. */
    private static final RandomGenerator ALWAYS_FIRST = new RandomGenerator() {
        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    };

    private static SimpleChipState.Builder chip(
            SimpleChipWorld world, String model, String third, String fourth) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("TERRAIN", "[" + model + "]", third, fourth);
    }

    @Nested
    @DisplayName("block breaker")
    class BlockBreaker {

        @Test
        void takesTheBlockBelowAndPutsItInTheContainerAbove() {
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(BELOW, STONE).withDrops(BELOW, Map.of(STONE, 1));
            SimpleStockpile chest = SimpleStockpile.empty();

            SimpleChipState state = chip(world, "MC1220", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.blockBreakerBelow().trigger(state);

            assertThat(world.blockAt(BELOW)).isEqualTo(Blocks.AIR_KEY);
            assertThat(chest.count(STONE)).isEqualTo(1);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void takesTheBlockAboveAndPutsItInTheContainerBelow() {
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(ABOVE, STONE).withDrops(ABOVE, Map.of(STONE, 1));
            SimpleStockpile chest = SimpleStockpile.empty();

            SimpleChipState state = chip(world, "MC1221", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(BELOW, chest);

            Terrain.blockBreakerAbove().trigger(state);

            assertThat(world.blockAt(ABOVE)).isEqualTo(Blocks.AIR_KEY);
            assertThat(chest.count(STONE)).isEqualTo(1);
        }

        @Test
        void leavesABlockThatIsNotTheOneNamedOnLineThree() {
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(BELOW, STONE).withDrops(BELOW, Map.of(STONE, 1));

            SimpleChipState state = chip(world, "MC1220", "dirt", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            Terrain.blockBreakerBelow().trigger(state);

            assertThat(world.blockAt(BELOW)).isEqualTo(STONE);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void keepsTheBlockWholeWhenLineFourSaysTrue() {
            SimpleChipWorld world = new SimpleChipWorld();
            Key ore = Blocks.key("deepslate_diamond_ore");
            world.withBlock(BELOW, ore)
                    .withDrops(BELOW, Map.of(Key.key("diamond"), 1))
                    .withIntactDrops(BELOW, Map.of(ore, 1));
            SimpleStockpile chest = SimpleStockpile.empty();

            SimpleChipState state = chip(world, "MC1220", "", "true")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.blockBreakerBelow().trigger(state);

            assertThat(chest.count(ore)).isEqualTo(1);
            assertThat(chest.count(Key.key("diamond"))).isZero();
        }

        @Test
        void leavesTheBlockStandingWhenThereIsNowhereToPutIt() {
            // Nothing is destroyed for want of room.
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(BELOW, STONE).withDrops(BELOW, Map.of(STONE, 1));

            SimpleChipState state = chip(world, "MC1220", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.withCapacity(0));

            Terrain.blockBreakerBelow().trigger(state);

            assertThat(world.blockAt(BELOW)).isEqualTo(STONE);
        }

        @Test
        void refusesToTakeBedrock() {
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(BELOW, Blocks.key("bedrock"));

            SimpleChipState state = chip(world, "MC1220", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            Terrain.blockBreakerBelow().trigger(state);

            assertThat(world.blockAt(BELOW)).isEqualTo(Blocks.key("bedrock"));
        }
    }

    @Nested
    @DisplayName("liquid flooder")
    class LiquidFlooder {

        @Test
        void fillsTheAreaWhileItIsDriven() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1222", "water", "1")
                    .inputs(true, false, false, false)
                    .build();

            Terrain.liquidFlooder().trigger(state);

            assertThat(world.blockAt(BACK.add(1, 0, 0))).isEqualTo(Blocks.WATER_KEY);
        }

        @Test
        void drainsItAgainWhenTheInputGoesAway() {
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(BACK.add(1, 0, 0), Blocks.WATER_KEY);

            SimpleChipState state = chip(world, "MC1222", "water", "1")
                    .inputs(false, false, false, false)
                    .build();

            Terrain.liquidFlooder().trigger(state);

            assertThat(world.blockAt(BACK.add(1, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void leavesALiquidItWasNotToldToPlace() {
            // Draining a valley of water must not take a lava flow with it.
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(BACK.add(1, 0, 0), Blocks.LAVA_KEY);

            SimpleChipState state = chip(world, "MC1222", "water", "1")
                    .inputs(false, false, false, false)
                    .build();

            Terrain.liquidFlooder().trigger(state);

            assertThat(world.blockAt(BACK.add(1, 0, 0))).isEqualTo(Blocks.LAVA_KEY);
        }

        @Test
        void placesLavaWhenTheSignSaysLava() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1222", "lava", "1")
                    .inputs(true, false, false, false)
                    .build();

            Terrain.liquidFlooder().trigger(state);

            assertThat(world.blockAt(BACK.add(1, 0, 0))).isEqualTo(Blocks.LAVA_KEY);
        }

        @Test
        void reachesOnlyAsFarAsTheAreaSaysTo() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1222", "water", "1")
                    .inputs(true, false, false, false)
                    .build();

            Terrain.liquidFlooder().trigger(state);

            assertThat(world.blockAt(BACK.add(3, 0, 0))).isEqualTo(Blocks.AIR_KEY);
        }
    }

    @Nested
    @DisplayName("pump")
    class Pump {

        @Test
        void tradesAnEmptyBucketForTheLiquidBelowIt() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i spring = BACK.add(0, -3, 0);
            world.withLiquidSource(spring, Blocks.WATER_KEY);
            SimpleStockpile chest = SimpleStockpile.empty().with(BUCKET, 1);

            SimpleChipState state = chip(world, "MC1225", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.pump().trigger(state);

            assertThat(world.blockAt(spring)).isEqualTo(Blocks.AIR_KEY);
            assertThat(chest.count(WATER_BUCKET)).isEqualTo(1);
            assertThat(chest.count(BUCKET)).isZero();
        }

        @Test
        void leavesTheLiquidWhereItIsWithNoBucketToPutItIn() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i spring = BACK.add(0, -1, 0);
            world.withLiquidSource(spring, Blocks.WATER_KEY);

            SimpleChipState state = chip(world, "MC1225", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            Terrain.pump().trigger(state);

            assertThat(world.blockAt(spring)).isEqualTo(Blocks.WATER_KEY);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void picksUpLavaAsLava() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i spring = BACK.add(0, -1, 0);
            world.withLiquidSource(spring, Blocks.LAVA_KEY);
            SimpleStockpile chest = SimpleStockpile.empty().with(BUCKET, 1);

            SimpleChipState state = chip(world, "MC1225", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.pump().trigger(state);

            assertThat(chest.count(LAVA_BUCKET)).isEqualTo(1);
        }

        @Test
        void followsARunOfLiquidSidewaysToFindWhereItComesFrom() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i flowing = BACK.add(0, -1, 0);
            Vec3i spring = BACK.add(2, -1, 0);
            world.withBlock(flowing, Blocks.WATER_KEY)
                    .withBlock(BACK.add(1, -1, 0), Blocks.WATER_KEY)
                    .withLiquidSource(spring, Blocks.WATER_KEY);
            SimpleStockpile chest = SimpleStockpile.empty().with(BUCKET, 1);

            SimpleChipState state = chip(world, "MC1225", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.pump().trigger(state);

            assertThat(world.blockAt(spring)).isEqualTo(Blocks.AIR_KEY);
            assertThat(world.blockAt(flowing)).isEqualTo(Blocks.WATER_KEY);
        }
    }

    @Nested
    @DisplayName("spigot")
    class Spigot {

        @Test
        void poursABucketOutIntoTheNearestEmptyBlock() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile chest = SimpleStockpile.empty().with(WATER_BUCKET, 1);

            SimpleChipState state = chip(world, "MC1226", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(BELOW, chest);

            Terrain.spigot().trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(Blocks.WATER_KEY);
            assertThat(chest.count(WATER_BUCKET)).isZero();
            assertThat(chest.count(BUCKET)).isEqualTo(1);
        }

        @Test
        void doesNothingWithNoFullBucketToPourFrom() {
            SimpleChipWorld world = new SimpleChipWorld();

            SimpleChipState state = chip(world, "MC1226", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(BELOW, SimpleStockpile.empty().with(BUCKET, 4));

            Terrain.spigot().trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(Blocks.AIR_KEY);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void poursLavaWhenThatIsWhatIsInTheChest() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile chest = SimpleStockpile.empty().with(LAVA_BUCKET, 1);

            SimpleChipState state = chip(world, "MC1226", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(BELOW, chest);

            Terrain.spigot().trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(Blocks.LAVA_KEY);
        }
    }

    @Nested
    @DisplayName("terraformer")
    class Terraformer {

        @Test
        void spendsBonemealOnSomethingThatWillTakeIt() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile chest = SimpleStockpile.empty().with(BONEMEAL, 4);
            SimpleChipState state = chip(world, "MC1223", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            world.withSomethingToFertilise(state.backPosition());

            Terrain.terraformer(ALWAYS_FIRST).trigger(state);

            assertThat(world.bonemealApplied()).contains(state.backPosition());
            assertThat(chest.count(BONEMEAL)).isEqualTo(3);
        }

        @Test
        void spendsNothingWhereBonemealWouldNotTake() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile chest = SimpleStockpile.empty().with(BONEMEAL, 4);

            SimpleChipState state = chip(world, "MC1223", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.terraformer(ALWAYS_FIRST).trigger(state);

            assertThat(chest.count(BONEMEAL)).isEqualTo(4);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void doesNothingWithNoBonemealToSpend() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1223", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            world.withSomethingToFertilise(state.backPosition());

            Terrain.terraformer(ALWAYS_FIRST).trigger(state);

            assertThat(world.bonemealApplied()).isEmpty();
        }
    }

    @Nested
    @DisplayName("irrigator")
    class Irrigator {

        @Test
        void watersDryFarmlandAndSpendsABucket() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile chest = SimpleStockpile.empty().with(WATER_BUCKET, 2);

            SimpleChipState state = chip(world, "MC1238", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            world.withDryFarmland(state.backPosition());

            Terrain.irrigator(ALWAYS_FIRST).trigger(state);

            assertThat(world.wateredFarmland()).contains(state.backPosition());
            assertThat(chest.count(WATER_BUCKET)).isEqualTo(1);
            assertThat(chest.count(BUCKET)).isEqualTo(1);
        }

        @Test
        void leavesFarmlandThatIsAlreadyWet() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile chest = SimpleStockpile.empty().with(WATER_BUCKET, 2);

            SimpleChipState state = chip(world, "MC1238", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.irrigator(ALWAYS_FIRST).trigger(state);

            assertThat(world.wateredFarmland()).isEmpty();
            assertThat(chest.count(WATER_BUCKET)).isEqualTo(2);
        }

        @Test
        void drinksFromABlockOfWaterStandingOnTheChest() {
            // A farm fed by a channel rather than by buckets.
            SimpleChipWorld world = new SimpleChipWorld();
            world.withBlock(ABOVE, Blocks.WATER_KEY);

            SimpleChipState state = chip(world, "MC1238", "0", "")
                    .inputs(true, false, false, false)
                    .build();

            world.withDryFarmland(state.backPosition());

            Terrain.irrigator(ALWAYS_FIRST).trigger(state);

            assertThat(world.wateredFarmland()).contains(state.backPosition());
            assertThat(world.blockAt(ABOVE)).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void doesNothingWithNoWaterAnywhere() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1238", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            world.withDryFarmland(state.backPosition());

            Terrain.irrigator(ALWAYS_FIRST).trigger(state);

            assertThat(world.wateredFarmland()).isEmpty();
        }
    }

    @Nested
    @DisplayName("driller")
    class Driller {

        @Test
        void takesTheTopmostBlockUnderItself() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i seam = BACK.add(-1, -1, -1);
            world.withBlock(seam, STONE).withDrops(seam, Map.of(STONE, 1));
            SimpleStockpile chest = SimpleStockpile.empty();

            SimpleChipState state = chip(world, "MC1248", "3", "8")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.driller(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(seam)).isEqualTo(Blocks.AIR_KEY);
            assertThat(chest.count(STONE)).isEqualTo(1);
        }

        @Test
        void digsPastEmptySpaceToReachTheFirstSolidBlock() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i seam = BACK.add(-1, -5, -1);
            world.withBlock(seam, STONE).withDrops(seam, Map.of(STONE, 1));
            SimpleStockpile chest = SimpleStockpile.empty();

            SimpleChipState state = chip(world, "MC1248", "3", "16")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, chest);

            Terrain.driller(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(seam)).isEqualTo(Blocks.AIR_KEY);
        }

        @Test
        void stopsAtTheDepthTheSignAllows() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i seam = BACK.add(-1, -9, -1);
            world.withBlock(seam, STONE).withDrops(seam, Map.of(STONE, 1));

            SimpleChipState state = chip(world, "MC1248", "3", "4")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            Terrain.driller(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(seam)).isEqualTo(STONE);
        }

        @Test
        void stopsAtBedrock() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i floor = BACK.add(-1, -1, -1);
            world.withBlock(floor, Blocks.key("bedrock"));

            SimpleChipState state = chip(world, "MC1248", "3", "16")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            Terrain.driller(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(floor)).isEqualTo(Blocks.key("bedrock"));
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void leavesTheSeamWhenThereIsNowhereToPutIt() {
            SimpleChipWorld world = new SimpleChipWorld();
            Vec3i seam = BACK.add(-1, -1, -1);
            world.withBlock(seam, STONE).withDrops(seam, Map.of(STONE, 1));

            SimpleChipState state = chip(world, "MC1248", "3", "8")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.withCapacity(0));

            Terrain.driller(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(seam)).isEqualTo(STONE);
        }
    }
}
