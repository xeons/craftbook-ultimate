package com.xeonproductions.craftbookultimate.core.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Stockpiles")
class StockpileTest {

    private static final Key STONE = Blocks.key("stone");
    private static final Key DIRT = Blocks.key("dirt");

    @Nested
    @DisplayName("a simple stockpile")
    class Simple {

        @Test
        void startsEmpty() {
            SimpleStockpile stockpile = SimpleStockpile.empty();

            assertThat(stockpile.isEmpty()).isTrue();
            assertThat(stockpile.count(STONE)).isZero();
            assertThat(stockpile.has(STONE, 1)).isFalse();
        }

        @Test
        void holdsWhatItIsGiven() {
            SimpleStockpile stockpile = SimpleStockpile.empty();

            assertThat(stockpile.give(STONE, 10)).isZero();
            assertThat(stockpile.count(STONE)).isEqualTo(10);
        }

        @Test
        void keepsKindsApart() {
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 5).with(DIRT, 3);

            assertThat(stockpile.count(STONE)).isEqualTo(5);
            assertThat(stockpile.count(DIRT)).isEqualTo(3);
        }

        @Test
        void givesBackWhatWasTaken() {
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 10);

            assertThat(stockpile.take(STONE, 4)).isEqualTo(4);
            assertThat(stockpile.count(STONE)).isEqualTo(6);
        }

        @Test
        void takesOnlyWhatItHas() {
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 3);

            assertThat(stockpile.take(STONE, 10)).isEqualTo(3);
            assertThat(stockpile.count(STONE)).isZero();
        }

        @Test
        void refusesAPartialWithdrawalWhenAskedForAllOfIt() {
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 3);

            assertThat(stockpile.takeAll(STONE, 10)).isFalse();
            assertThat(stockpile.count(STONE))
                    .as("nothing should have been taken")
                    .isEqualTo(3);
        }

        @Test
        void honoursItsCapacity() {
            SimpleStockpile stockpile = SimpleStockpile.withCapacity(10);

            assertThat(stockpile.give(STONE, 15)).isEqualTo(5);
            assertThat(stockpile.count(STONE)).isEqualTo(10);
            assertThat(stockpile.hasRoomFor(STONE, 1)).isFalse();
        }

        @Test
        void rejectsANegativeCapacity() {
            assertThatThrownBy(() -> SimpleStockpile.withCapacity(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void ignoresMeaninglessAmounts() {
            SimpleStockpile stockpile = SimpleStockpile.empty().with(STONE, 5);

            assertThat(stockpile.take(STONE, 0)).isZero();
            assertThat(stockpile.take(STONE, -3)).isZero();
            assertThat(stockpile.give(STONE, -3)).isZero();
            assertThat(stockpile.count(STONE)).isEqualTo(5);
        }

        @Test
        void takingNothingAlwaysSucceeds() {
            assertThat(SimpleStockpile.empty().takeAll(STONE, 0)).isTrue();
        }
    }

    @Nested
    @DisplayName("an unlimited stockpile")
    class Unlimited {

        @Test
        void alwaysHasWhatIsAskedFor() {
            Stockpile stockpile = Stockpiles.unlimited();

            assertThat(stockpile.isUnlimited()).isTrue();
            assertThat(stockpile.has(STONE, 1_000_000)).isTrue();
            assertThat(stockpile.takeAll(STONE, 1_000_000)).isTrue();
        }

        @Test
        void swallowsEverythingGivenToIt() {
            assertThat(Stockpiles.unlimited().give(STONE, 1_000_000)).isZero();
        }

        @Test
        void reportsNoContents() {
            // It holds no total, so listing what is inside would be meaningless.
            assertThat(Stockpiles.unlimited().contents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("an empty stockpile")
    class EmptyStockpile {

        @Test
        void hasNothingAndTakesNothing() {
            Stockpile stockpile = Stockpiles.empty();

            assertThat(stockpile.has(STONE, 1)).isFalse();
            assertThat(stockpile.take(STONE, 1)).isZero();
        }

        @Test
        void refusesWhatItIsGiven() {
            assertThat(Stockpiles.empty().give(STONE, 5)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("several stockpiles combined")
    class Combined {

        @Test
        void addsUpWhatTheyHold() {
            Stockpile combined = Stockpiles.combined(List.of(
                    SimpleStockpile.empty().with(STONE, 3),
                    SimpleStockpile.empty().with(STONE, 4)));

            assertThat(combined.count(STONE)).isEqualTo(7);
            assertThat(combined.has(STONE, 7)).isTrue();
        }

        @Test
        void drawsFromTheFirstBeforeTheSecond() {
            SimpleStockpile near = SimpleStockpile.empty().with(STONE, 5);
            SimpleStockpile far = SimpleStockpile.empty().with(STONE, 5);

            Stockpiles.combined(List.of(near, far)).take(STONE, 3);

            assertThat(near.count(STONE)).isEqualTo(2);
            assertThat(far.count(STONE)).isEqualTo(5);
        }

        @Test
        void spillsIntoTheSecondWhenTheFirstRunsOut() {
            SimpleStockpile near = SimpleStockpile.empty().with(STONE, 5);
            SimpleStockpile far = SimpleStockpile.empty().with(STONE, 5);

            int taken = Stockpiles.combined(List.of(near, far)).take(STONE, 8);

            assertThat(taken).isEqualTo(8);
            assertThat(near.count(STONE)).isZero();
            assertThat(far.count(STONE)).isEqualTo(2);
        }

        @Test
        void fillsTheFirstBeforeTheSecond() {
            SimpleStockpile near = SimpleStockpile.withCapacity(4);
            SimpleStockpile far = SimpleStockpile.withCapacity(10);

            int leftOver = Stockpiles.combined(List.of(near, far)).give(STONE, 6);

            assertThat(leftOver).isZero();
            assertThat(near.count(STONE)).isEqualTo(4);
            assertThat(far.count(STONE)).isEqualTo(2);
        }

        @Test
        void putsBackWhatItTookWhenItCannotTakeEnough() {
            // Half-emptying several chests and then failing would leave the world short.
            SimpleStockpile near = SimpleStockpile.empty().with(STONE, 3);
            SimpleStockpile far = SimpleStockpile.empty().with(STONE, 2);

            boolean taken = Stockpiles.combined(List.of(near, far)).takeAll(STONE, 10);

            assertThat(taken).isFalse();
            assertThat(near.count(STONE)).isEqualTo(3);
            assertThat(far.count(STONE)).isEqualTo(2);
        }

        @Test
        void takesAcrossSeveralWhenItCan() {
            SimpleStockpile near = SimpleStockpile.empty().with(STONE, 3);
            SimpleStockpile far = SimpleStockpile.empty().with(STONE, 2);

            boolean taken = Stockpiles.combined(List.of(near, far)).takeAll(STONE, 5);

            assertThat(taken).isTrue();
            assertThat(near.count(STONE)).isZero();
            assertThat(far.count(STONE)).isZero();
        }

        @Test
        void isUnlimitedIfAnyPartIs() {
            Stockpile combined = Stockpiles.combined(List.of(
                    SimpleStockpile.empty(), Stockpiles.unlimited()));

            assertThat(combined.isUnlimited()).isTrue();
            assertThat(combined.has(STONE, 1_000)).isTrue();
        }

        @Test
        void mergesWhatEachPartHolds() {
            Stockpile combined = Stockpiles.combined(List.of(
                    SimpleStockpile.empty().with(STONE, 3),
                    SimpleStockpile.empty().with(STONE, 4).with(DIRT, 1)));

            assertThat(combined.contents()).containsEntry(STONE, 7).containsEntry(DIRT, 1);
        }

        @Test
        void collapsesTrivialCombinations() {
            SimpleStockpile only = SimpleStockpile.empty();

            assertThat(Stockpiles.combined(List.of())).isSameAs(Stockpiles.empty());
            assertThat(Stockpiles.combined(List.of(only))).isSameAs(only);
        }
    }
}
