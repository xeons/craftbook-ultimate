// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.xeonproductions.craftbookultimate.core.config.CartHabits;
import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("How every cart behaves")
class CartBehaviourTest {

    private static final Vec3i AHEAD = new Vec3i(4, 64, 8);
    private static final Key COAL = Blocks.key("coal");

    /** Nothing switched on, which is how a server that has never been configured runs. */
    private static final CartHabits NOTHING = CartHabits.DEFAULTS;

    @Nested
    @DisplayName("out of the box")
    class OutOfTheBox {

        @Test
        void leavesEveryHabitOff() {
            assertThat(NOTHING.decaysEmptyCarts()).isFalse();
            assertThat(NOTHING.removeOnExit()).isFalse();
            assertThat(NOTHING.pickUpItems()).isFalse();
            assertThat(NOTHING.blockMobs()).isFalse();
            assertThat(NOTHING.climbsWalls()).isFalse();
            assertThat(NOTHING.plateIntersections()).isFalse();
            assertThat(NOTHING.passesThroughAnyCart()).isFalse();
            assertThat(NOTHING.runDownEntities()).isFalse();
        }

        @Test
        void letsCreaturesRideAndLeavesStandingCartsAlone() {
            assertThat(CartBehaviour.mayRide(SimpleBystander.animal("pig"), NOTHING)).isTrue();
            assertThat(CartBehaviour.hasStoodEmpty(SimpleCart.rideable(), NOTHING)).isFalse();
        }
    }

    @Nested
    @DisplayName("keeping creatures out")
    class KeepingCreaturesOut {

        private final CartHabits blocking = NOTHING.withMobBlocking(true);

        @Test
        void turnsACreatureAway() {
            assertThat(CartBehaviour.mayRide(SimpleBystander.animal("pig"), blocking)).isFalse();
        }

        @Test
        void letsAPersonInRegardless() {
            assertThat(CartBehaviour.mayRide(SimpleBystander.player("Ada"), blocking)).isTrue();
        }
    }

    @Nested
    @DisplayName("taking an empty cart away")
    class TakingAnEmptyCartAway {

        private final CartHabits decaying = NOTHING.withDecay(40, true);

        @Test
        void takesOneNobodyHasClimbedBackInto() {
            assertThat(CartBehaviour.hasStoodEmpty(SimpleCart.rideable(), decaying)).isTrue();
        }

        @Test
        void leavesOneSomebodyIsSittingIn() {
            SimpleCart occupied = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            assertThat(CartBehaviour.hasStoodEmpty(occupied, decaying)).isFalse();
        }

        @Test
        void leavesOneThatHasAlreadyGone() {
            assertThat(CartBehaviour.hasStoodEmpty(SimpleCart.rideable().removed(), decaying))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("passing through another cart")
    class PassingThroughAnotherCart {

        @Test
        void passesThroughAnEmptyOneWhenAskedTo() {
            CartHabits habits = NOTHING.withPassThrough(true, false);

            assertThat(CartBehaviour.passesThrough(SimpleCart.rideable(), habits)).isTrue();
        }

        @Test
        void shuntsAnOccupiedOneAllTheSame() {
            CartHabits habits = NOTHING.withPassThrough(true, false);
            SimpleCart occupied = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            assertThat(CartBehaviour.passesThrough(occupied, habits)).isFalse();
        }

        @Test
        void countsALadenCartWithTheOccupiedOnes() {
            CartHabits habits = NOTHING.withPassThrough(false, true);

            assertThat(CartBehaviour.passesThrough(SimpleCart.storage(), habits)).isTrue();
            assertThat(CartBehaviour.passesThrough(SimpleCart.rideable(), habits)).isFalse();
        }
    }

    @Nested
    @DisplayName("running something down")
    class RunningSomethingDown {

        private final CartHabits running = NOTHING.withRunDown(true, false, false);

        private SimpleCart movingWithARider() {
            return SimpleCart.rideable()
                    .carrying(SimpleBystander.player("Ada"))
                    .moving(new Vec3d(0, 0, -0.4));
        }

        @Test
        void hurtsWhatAnOccupiedCartHits() {
            Optional<CartBehaviour.RunDown> what = CartBehaviour.runDown(
                    movingWithARider(), SimpleBystander.animal("pig"), running);

            assertThat(what).isPresent();
            assertThat(what.get().removes()).isFalse();
            assertThat(what.get().damage()).isEqualTo(10);
        }

        @Test
        void throwsItOnAndUpFromWhereTheCartIsGoing() {
            Optional<CartBehaviour.RunDown> what = CartBehaviour.runDown(
                    movingWithARider(), SimpleBystander.animal("pig"), running);

            Vec3d thrown = what.orElseThrow().thrownClear();
            assertThat(thrown.z()).isEqualTo(-1.6, within(1e-9));
            assertThat(thrown.y()).isEqualTo(0.3, within(1e-9));
        }

        @Test
        void leavesAStandingCartWithNothingToThrowThingsWith() {
            SimpleCart standing = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            Optional<CartBehaviour.RunDown> what =
                    CartBehaviour.runDown(standing, SimpleBystander.animal("pig"), running);

            assertThat(what.orElseThrow().thrownClear()).isEqualTo(Vec3d.ZERO);
        }

        @Test
        void doesNothingWhenNobodyIsAboard() {
            SimpleCart empty = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));

            assertThat(CartBehaviour.runDown(empty, SimpleBystander.animal("pig"), running))
                    .isEmpty();
        }

        @Test
        void doesNotHurtItsOwnRider() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            SimpleCart cart = SimpleCart.rideable().carrying(rider).moving(new Vec3d(0, 0, -0.4));

            assertThat(CartBehaviour.runDown(cart, rider, running)).isEmpty();
        }

        @Test
        void removesSomethingThatCannotBeHurt() {
            SimpleBystander crate = SimpleBystander.of("item").asObject();

            assertThat(CartBehaviour.runDown(movingWithARider(), crate, running))
                    .get()
                    .matches(CartBehaviour.RunDown::removes);
        }

        @Test
        void leavesWhatItCannotHurtAloneWhereHurtingIsAllItMayDo() {
            CartHabits onlyHurts = NOTHING.withRunDown(true, true, false);
            SimpleBystander crate = SimpleBystander.of("item").asObject();

            assertThat(CartBehaviour.runDown(movingWithARider(), crate, onlyHurts)).isEmpty();
        }

        @Test
        void sparesAnotherCartUnlessToldNotTo() {
            SimpleBystander cart = SimpleBystander.of("minecart").asObject();

            assertThat(CartBehaviour.runDown(movingWithARider(), cart, running)).isEmpty();
            assertThat(CartBehaviour.runDown(
                    movingWithARider(), cart, NOTHING.withRunDown(true, false, true)))
                    .isPresent();
        }

        @Test
        void doesNothingAtAllUnlessAskedFor() {
            assertThat(CartBehaviour.runDown(
                    movingWithARider(), SimpleBystander.animal("pig"), NOTHING))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("being pushed along by a block")
    class BeingPushedAlongByABlock {

        private final CartHabits climbing = NOTHING.withClimbing(0.15, true);

        @Test
        void sendsACartStraightOnAcrossAPressurePlate() {
            SimpleCartWorld world = new SimpleCartWorld().withBlock(AHEAD, "stone_pressure_plate");

            Optional<Vec3d> push =
                    CartBehaviour.pushFrom(world, AHEAD, new Vec3d(0, 0, -0.4), climbing);

            assertThat(push.orElseThrow().z()).isEqualTo(-CartHabits.CROSSING_SPEED, within(1e-9));
            assertThat(push.orElseThrow().x()).isEqualTo(0, within(1e-9));
        }

        @Test
        void leavesACartStandingOnAPlateWhereItIs() {
            // It has no direction to be sent in, and asking for one would be asking which way
            // nothing is pointing.
            SimpleCartWorld world = new SimpleCartWorld().withBlock(AHEAD, "stone_pressure_plate");

            assertThat(CartBehaviour.pushFrom(world, AHEAD, Vec3d.ZERO, climbing)).isEmpty();
        }

        @Test
        void sendsACartUpALadderAndIntoTheWallItHangsOn() {
            SimpleCartWorld world = new SimpleCartWorld()
                    .withBlock(AHEAD, "ladder")
                    .withClimbable(AHEAD, BlockFace.NORTH);

            Vec3d push = CartBehaviour.pushFrom(world, AHEAD, Vec3d.ZERO, climbing).orElseThrow();

            assertThat(push.y()).isEqualTo(0.15, within(1e-9));
            assertThat(push.z()).isEqualTo(-1, within(1e-9));
        }

        @Test
        void keepsWhateverSpeedTheCartCameInWith() {
            SimpleCartWorld world = new SimpleCartWorld()
                    .withBlock(AHEAD, "ladder")
                    .withClimbable(AHEAD, BlockFace.NORTH);

            Vec3d push = CartBehaviour.pushFrom(world, AHEAD, new Vec3d(0.2, 0, 0), climbing)
                    .orElseThrow();

            assertThat(push.x()).isEqualTo(0.2, within(1e-9));
        }

        @Test
        void takesTheAverageOfTheSidesAVineHasGrownAcross() {
            SimpleCartWorld world = new SimpleCartWorld()
                    .withBlock(AHEAD, "vine")
                    .withClimbable(AHEAD, BlockFace.NORTH, BlockFace.EAST);

            Vec3d push = CartBehaviour.pushFrom(world, AHEAD, Vec3d.ZERO, climbing).orElseThrow();

            assertThat(push.x()).isEqualTo(Math.sqrt(0.5), within(1e-9));
            assertThat(push.z()).isEqualTo(-Math.sqrt(0.5), within(1e-9));
        }

        @Test
        void leavesACartAloneOnAVineClingingToNothing() {
            SimpleCartWorld world = new SimpleCartWorld().withBlock(AHEAD, "vine");

            assertThat(CartBehaviour.pushFrom(world, AHEAD, Vec3d.ZERO, climbing)).isEmpty();
        }

        @Test
        void leavesACartAloneOnAnOrdinaryBlock() {
            SimpleCartWorld world = new SimpleCartWorld().withBlock(AHEAD, "stone");

            assertThat(CartBehaviour.pushFrom(world, AHEAD, new Vec3d(0, 0, -0.4), climbing))
                    .isEmpty();
        }

        @Test
        void doesNothingAtAllUnlessAskedFor() {
            SimpleCartWorld world = new SimpleCartWorld()
                    .withBlock(AHEAD, "ladder")
                    .withClimbable(AHEAD, BlockFace.NORTH);

            assertThat(CartBehaviour.pushFrom(world, AHEAD, Vec3d.ZERO, NOTHING)).isEmpty();
        }
    }

    @Nested
    @DisplayName("gathering up what it passes")
    class GatheringUpWhatItPasses {

        private final CartHabits gathering = NOTHING.withItemPickup(true);

        @Test
        void takesUpAStackAStorageCartRunsOver() {
            SimpleCartWorld world = new SimpleCartWorld().withItemLying(Vec3d.ZERO, COAL, 12);
            SimpleCart cart = SimpleCart.storage();

            assertThat(CartBehaviour.gatherItems(cart, world, gathering)).isEqualTo(12);
            assertThat(cart.contents().orElseThrow().count(COAL)).isEqualTo(12);
            assertThat(world.itemsLying()).isEmpty();
        }

        @Test
        void leavesWhatIsLyingOutOfReach() {
            SimpleCartWorld world = new SimpleCartWorld()
                    .withItemLying(new Vec3d(0, 0, 6), COAL, 12);

            assertThat(CartBehaviour.gatherItems(SimpleCart.storage(), world, gathering)).isZero();
            assertThat(world.itemsLying()).hasSize(1);
        }

        @Test
        void leavesAStackWholeWhereItWillNotAllFit() {
            SimpleCartWorld world = new SimpleCartWorld().withItemLying(Vec3d.ZERO, COAL, 12);
            SimpleCart cart = SimpleCart.storage()
                    .withContents(SimpleStockpile.withCapacity(4));

            assertThat(CartBehaviour.gatherItems(cart, world, gathering)).isZero();
            assertThat(world.itemsLying()).hasSize(1);
        }

        @Test
        void leavesItForACartWithNowhereToPutIt() {
            SimpleCartWorld world = new SimpleCartWorld().withItemLying(Vec3d.ZERO, COAL, 12);

            assertThat(CartBehaviour.gatherItems(SimpleCart.rideable(), world, gathering)).isZero();
            assertThat(world.itemsLying()).hasSize(1);
        }

        @Test
        void doesNothingAtAllUnlessAskedFor() {
            SimpleCartWorld world = new SimpleCartWorld().withItemLying(Vec3d.ZERO, COAL, 12);

            assertThat(CartBehaviour.gatherItems(SimpleCart.storage(), world, NOTHING)).isZero();
            assertThat(world.itemsLying()).hasSize(1);
        }
    }
}
