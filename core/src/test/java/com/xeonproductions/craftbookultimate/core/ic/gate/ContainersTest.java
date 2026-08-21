// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleDroppedItem;
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

@DisplayName("Container chips")
class ContainersTest {

    private static final Key STONE = Blocks.key("stone");
    private static final Key DIRT = Blocks.key("dirt");

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    private SimpleChipWorld world = new SimpleChipWorld();

    private static SimpleStockpile holding(Key item, int amount) {
        SimpleStockpile pile = SimpleStockpile.empty();
        pile.give(item, amount);
        return pile;
    }

    private SimpleChipState.Builder chip(String model, String third, String fourth) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("CHEST", "[" + model + "]", third, fourth);
    }

    @Nested
    @DisplayName("chest dispenser")
    class ChestDispenser {

        @Test
        void dropsWhatItsSignNamesOutOfANearbyContainer() {
            SimpleStockpile pile = holding(STONE, 10);
            SimpleChipState state = chip("MCX202", "stone", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, pile);

            Containers.chestDispenser().trigger(state);

            assertThat(world.droppedStacks()).hasSize(1);
            assertThat(world.droppedStacks().get(0).item()).isEqualTo(STONE);
            assertThat(world.droppedStacks().get(0).count()).isEqualTo(1);
            assertThat(pile.count(STONE)).isEqualTo(9);
        }

        @Test
        void dropsAsManyAsTheFourthLineAsksFor() {
            SimpleStockpile pile = holding(STONE, 100);
            SimpleChipState state = chip("MCX202", "stone", "12")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, pile);

            Containers.chestDispenser().trigger(state);

            assertThat(world.droppedStacks().get(0).count()).isEqualTo(12);
            assertThat(pile.count(STONE)).isEqualTo(88);
        }

        @Test
        void dropsWhatItFindsWhenItsSignNamesNoItem() {
            SimpleStockpile pile = holding(DIRT, 4);
            SimpleChipState state = chip("MCX202", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, pile);

            Containers.chestDispenser().trigger(state);

            assertThat(world.droppedStacks().get(0).item()).isEqualTo(DIRT);
        }

        @Test
        void dropsNothingWhenTheContainerHasNoneOfIt() {
            SimpleStockpile pile = holding(DIRT, 4);
            SimpleChipState state = chip("MCX202", "stone", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, pile);

            Containers.chestDispenser().trigger(state);

            assertThat(world.droppedStacks()).isEmpty();
        }

        @Test
        void dropsAboveWhateverIsStackedOnItsSupport() {
            world.withBlock(BEHIND.add(0, 1, 0), "stone");
            SimpleChipState state = chip("MCX202", "stone", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, holding(STONE, 4));

            Containers.chestDispenser().trigger(state);

            assertThat(world.droppedStacks().get(0).at().y()).isEqualTo(BEHIND.y() + 2);
        }

        @Test
        void takesFromTheContainerItsSignPointsAt() {
            Vec3i chestAt = SIGN.add(3, 0, 0);
            SimpleStockpile pile = holding(STONE, 10);
            SimpleChipState state = chip("MCX202", "stone", "1@3:0:0")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(chestAt, pile);

            Containers.chestDispenser().trigger(state);

            assertThat(pile.count(STONE)).isEqualTo(9);
        }

        @Test
        void dropsNothingWhileNothingDrivesIt() {
            SimpleChipState state = chip("MCX202", "stone", "")
                    .build()
                    .withStockpileAt(SIGN, holding(STONE, 10));

            Containers.chestDispenser().trigger(state);

            assertThat(world.droppedStacks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("chest collector")
    class ChestCollector {

        @Test
        void putsWhatIsLyingAroundIntoTheContainer() {
            SimpleDroppedItem lying = SimpleDroppedItem.of("stone", 5);
            world.withDroppedItem(SIGN, lying);
            SimpleStockpile pile = SimpleStockpile.empty();
            SimpleChipState state = chip("MCX203", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, pile);

            Containers.chestCollector().trigger(state);

            assertThat(pile.count(STONE)).isEqualTo(5);
            assertThat(lying.isPresent()).isFalse();
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void picksUpOnlyWhatItsSignNames() {
            SimpleDroppedItem wanted = SimpleDroppedItem.of("stone", 5);
            SimpleDroppedItem ignored = SimpleDroppedItem.of("dirt", 5);
            world.withDroppedItem(SIGN, wanted).withDroppedItem(SIGN, ignored);
            SimpleStockpile pile = SimpleStockpile.empty();
            SimpleChipState state = chip("MCX203", "stone", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, pile);

            Containers.chestCollector().trigger(state);

            assertThat(pile.count(STONE)).isEqualTo(5);
            assertThat(ignored.isPresent()).isTrue();
        }

        @Test
        void leavesOnTheGroundWhateverWouldNotFit() {
            SimpleDroppedItem lying = SimpleDroppedItem.of("stone", 10);
            world.withDroppedItem(SIGN, lying);
            SimpleStockpile pile = SimpleStockpile.withCapacity(4);
            SimpleChipState state = chip("MCX203", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, pile);

            Containers.chestCollector().trigger(state);

            assertThat(pile.count(STONE)).isEqualTo(4);
            assertThat(lying.count()).isEqualTo(6);
        }

        @Test
        void saysSoWhenThereWasNothingToPickUp() {
            SimpleChipState state = chip("MCX203", "", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(SIGN, SimpleStockpile.empty());

            Containers.chestCollector().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void picksUpOnEveryTickWhenItTicks() {
            SimpleDroppedItem lying = SimpleDroppedItem.of("stone", 5);
            world.withDroppedItem(SIGN, lying);
            SimpleStockpile pile = SimpleStockpile.empty();
            SimpleChipState state = chip("MCX203", "", "").build().withStockpileAt(SIGN, pile);

            Containers.chestCollector().tick(state);

            assertThat(pile.count(STONE)).isEqualTo(5);
        }
    }
}
