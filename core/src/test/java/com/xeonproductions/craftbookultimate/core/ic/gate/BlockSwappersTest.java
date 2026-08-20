package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Block swapping chips")
class BlockSwappersTest {

    private static final Key STONE = Blocks.key("stone");
    private static final Key GLASS = Blocks.key("glass");
    private static final Key DIRT = Blocks.key("dirt");

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    /** One above the block the sign hangs on, which is where {@code Y+1} points. */
    private static final Vec3i TARGET = BEHIND.offset(BlockFace.UP);

    @Nested
    @DisplayName("toggle block")
    class ToggleBlock {

        private SimpleChipState.Builder chip(SimpleChipWorld world, SimpleStockpile stockpile) {
            return SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(stockpile)
                    .sign("TOGGLE BLOCK", "[MCX211]", "stone|glass", "Y+1");
        }

        @Test
        void putsTheFirstBlockDownWhileDriven() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 4).with(GLASS, 4);
            SimpleChipState state = chip(world, stockpile).inputs(true, false, false, false).build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(STONE);
            assertThat(stockpile.count(STONE)).isEqualTo(3);
        }

        @Test
        void putsTheSecondBlockDownWhileIdle() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 4).with(GLASS, 4);
            SimpleChipState state = chip(world, stockpile).inputs(false, false, false, false).build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(GLASS);
        }

        @Test
        void swapsOneForTheOtherAndIsRefunded() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, GLASS);
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 1);
            SimpleChipState state = chip(world, stockpile).inputs(true, false, false, false).build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(STONE);
            assertThat(stockpile.count(STONE)).isZero();
            assertThat(stockpile.count(GLASS)).isEqualTo(1);
        }

        @Test
        void doesNothingWithoutTheBlockItWouldPlace() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, GLASS);
            SimpleChipState state = chip(world, SimpleStockpile.empty())
                    .inputs(true, false, false, false)
                    .build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(GLASS);
        }

        @Test
        void leavesSomethingElseInTheWayAlone() {
            // Swapping out a block that is not one of its two would be a way of taking it.
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, DIRT);
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 4);
            SimpleChipState state = chip(world, stockpile).inputs(true, false, false, false).build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(world.blockAt(TARGET)).isEqualTo(DIRT);
            assertThat(stockpile.count(STONE)).isEqualTo(4);
        }

        @Test
        void doesNothingWhenTheRightBlockIsAlreadyThere() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(TARGET, STONE);
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 4);
            SimpleChipState state = chip(world, stockpile).inputs(true, false, false, false).build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(stockpile.count(STONE)).isEqualTo(4);
        }

        @Test
        void readsItsOffsetOffTheSign() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(SimpleStockpile.empty().with(STONE, 4))
                    .sign("TOGGLE BLOCK", "[MCX211]", "stone|glass", "Z-3")
                    .inputs(true, false, false, false)
                    .build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(world.blockAt(BEHIND.add(0, 0, -3))).isEqualTo(STONE);
        }

        @Test
        void doesNothingWithoutAUsableSign() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .stockpile(SimpleStockpile.empty().with(STONE, 4))
                    .sign("TOGGLE BLOCK", "[MCX211]", "stone", "Y+1")
                    .inputs(true, false, false, false)
                    .build();

            BlockSwappers.toggleBlock().trigger(state);

            assertThat(world.placedBlockCount()).isZero();
        }
    }

    @Nested
    @DisplayName("block replacer")
    class BlockReplacer {

        private SimpleChipState.Builder chip(SimpleChipWorld world, String settings) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("BLOCK REPLACER", "[MC1249]", "glass|stone", settings);
        }

        @Test
        void swapsTheBlockItHangsOnWhenDriven() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BEHIND, STONE);
            SimpleChipState state = chip(world, "1:0:true").inputs(true, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);

            assertThat(world.blockAt(BEHIND)).isEqualTo(GLASS);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void swapsItBackWhenTheInputDrops() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BEHIND, GLASS);
            SimpleChipState state = chip(world, "1:0:true").inputs(false, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);

            assertThat(world.blockAt(BEHIND)).isEqualTo(STONE);
        }

        @Test
        void staysQuietWhenItHangsOnSomethingElse() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BEHIND, DIRT);
            SimpleChipState state = chip(world, "1:0:true").inputs(true, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);

            assertThat(world.blockAt(BEHIND)).isEqualTo(DIRT);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void spreadsThroughTouchingBlocksOneStepAtATime() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(BEHIND, STONE)
                    .withBlock(BEHIND.add(0, 0, -1), STONE)
                    .withBlock(BEHIND.add(0, 0, -2), STONE)
                    .withBlock(BEHIND.add(0, 0, -3), STONE);
            SimpleChipState state = chip(world, "1:0:true").inputs(true, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);

            // The block behind the sign and everything touching it change at once; each further
            // ring waits out the delay.
            assertThat(world.blockAt(BEHIND)).isEqualTo(GLASS);
            assertThat(world.blockAt(BEHIND.add(0, 0, -1))).isEqualTo(GLASS);
            assertThat(world.blockAt(BEHIND.add(0, 0, -2))).isEqualTo(STONE);

            state.manualScheduler().advance(1);
            assertThat(world.blockAt(BEHIND.add(0, 0, -2))).isEqualTo(GLASS);
            assertThat(world.blockAt(BEHIND.add(0, 0, -3))).isEqualTo(STONE);

            state.manualScheduler().advance(1);
            assertThat(world.blockAt(BEHIND.add(0, 0, -3))).isEqualTo(GLASS);
        }

        @Test
        void stopsAtAnythingThatIsNotOneOfItsTwoBlocks() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(BEHIND, STONE)
                    .withBlock(BEHIND.add(0, 0, -1), DIRT)
                    .withBlock(BEHIND.add(0, 0, -2), STONE);
            SimpleChipState state = chip(world, "1:0:true").inputs(true, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);
            state.manualScheduler().advance(10);

            assertThat(world.blockAt(BEHIND.add(0, 0, -1))).isEqualTo(DIRT);
            assertThat(world.blockAt(BEHIND.add(0, 0, -2))).isEqualTo(STONE);
        }

        @Test
        void keepsToOneBlockInAnyModeButZero() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(BEHIND, STONE)
                    .withBlock(BEHIND.add(0, 0, -1), STONE);
            SimpleChipState state = chip(world, "1:1:true").inputs(true, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);
            state.manualScheduler().advance(10);

            assertThat(world.blockAt(BEHIND)).isEqualTo(GLASS);
            assertThat(world.blockAt(BEHIND.add(0, 0, -1))).isEqualTo(STONE);
        }

        @Test
        void leavesTheNeighboursUntoldWhenAskedTo() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BEHIND, STONE);
            SimpleChipState state = chip(world, "1:1:false").inputs(true, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);

            assertThat(world.wasPlacedSilently(BEHIND)).isTrue();
        }

        @Test
        void fallsBackToItsDefaultsForAnUnreadableSettingsLine() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BEHIND, STONE);
            SimpleChipState state = chip(world, "nonsense").inputs(true, false, false).build();

            BlockSwappers.blockReplacer().trigger(state);

            assertThat(world.blockAt(BEHIND)).isEqualTo(GLASS);
            assertThat(world.wasPlacedSilently(BEHIND)).isFalse();
        }

        @Test
        void doesNothingWithoutAPairOfBlocks() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BEHIND, STONE);
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("BLOCK REPLACER", "[MC1249]", "glass", "1:0:true")
                    .inputs(true, false, false)
                    .build();

            ICLogic chip = BlockSwappers.blockReplacer();
            chip.trigger(state);

            assertThat(world.blockAt(BEHIND)).isEqualTo(STONE);
        }
    }
}
