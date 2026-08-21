// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.OptionalDouble;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The lift")
class ElevatorTest {

    private static final Elevator LIFT = new Elevator();

    /** The sign the rider works, and the one above it they arrive at. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i UPPER_SIGN = new Vec3i(0, 70, 0);

    private static final Vec3i LOWER_SIGN = new Vec3i(0, 58, 0);

    /** Where the rider stands: beside the sign rather than inside it. */
    private static final Vec3i STANDING = new Vec3i(0, 64, 1);

    private final SimpleMechanicWorld world = new SimpleMechanicWorld();

    /** A landing with a floor two blocks below the sign's height and room to stand on it. */
    private SimpleMechanicWorld withFloorAt(int y) {
        return world.with(new Vec3i(0, y - 2, 1), "stone");
    }

    private MechanicVisit at(Vec3i sign, SimpleActor rider) {
        return MechanicVisit.byHand(
                world.signAt(sign).orElseThrow(), world, Settings.DEFAULTS, rider);
    }

    private MechanicVisit at(Vec3i sign, SimpleActor rider, double touchHeight) {
        return MechanicVisit.byHand(
                world.signAt(sign).orElseThrow(),
                OptionalDouble.of(touchHeight),
                world,
                Settings.DEFAULTS,
                rider);
    }

    @Nested
    @DisplayName("carrying somebody")
    class CarryingSomebody {

        @Test
        void takesThemToTheNextSignAbove() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            assertThat(LIFT.act(at(SIGN, rider))).isTrue();

            assertThat(rider.sentTo()).isPresent();
            assertThat(rider.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(0, 69, 1));
        }

        @Test
        void takesThemToTheNextSignBelow() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.DOWN, "", "")
                    .withSign(LOWER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(58);
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider));

            assertThat(rider.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(0, 57, 1));
        }

        @Test
        void leavesThemLookingTheWayTheyWere() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider));

            assertThat(rider.sentTo().orElseThrow().facing()).isEqualTo(BlockFace.SELF);
        }

        @Test
        void passesOverASignThatIsNotALift() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(new Vec3i(0, 67, 0), BlockFace.SOUTH, "", "[Bridge]", "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider));

            assertThat(rider.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(0, 69, 1));
        }

        @Test
        void putsThemOnTheFloorWhereTheSignIsHungAtHeadHeight() {
            // The block level with the sign is solid and the one above it is clear, which is how
            // a sign beside a doorway usually sits.
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "")
                    .with(new Vec3i(0, 69, 1), "stone");
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider));

            assertThat(rider.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(0, 70, 1));
        }
    }

    @Nested
    @DisplayName("saying where they have arrived")
    class SayingWhereTheyHaveArrived {

        @Test
        void namesTheFloorWhenTheFarSignDoes() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "Roof Garden", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider));

            assertThat(rider.wasTold("Floor: Roof Garden")).isTrue();
        }

        @Test
        void saysWhichWayTheyWentWhenItDoesNot() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider));

            assertThat(rider.wasTold("gone up a floor")).isTrue();
        }
    }

    @Nested
    @DisplayName("the two-way sign")
    class TheTwoWaySign {

        @Test
        void goesUpWhenTheTopHalfIsTouched() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.BOTH, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider, 0.8));

            assertThat(rider.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(0, 69, 1));
        }

        @Test
        void goesDownWhenTheBottomHalfIsTouched() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.BOTH, "", "")
                    .withSign(LOWER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(58);
            SimpleActor rider = SimpleActor.at(STANDING);

            LIFT.act(at(SIGN, rider, 0.2));

            assertThat(rider.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(0, 57, 1));
        }

        @Test
        void doesNothingWhenNobodySaysWhereOnItTheyTouched() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.BOTH, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            assertThat(LIFT.act(at(SIGN, rider))).isFalse();

            assertThat(rider.wasMoved()).isFalse();
        }

        @Test
        void carriesSomebodyWhoJumpedWithoutTouchingItAtAll() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.BOTH, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            assertThat(LIFT.ride(at(SIGN, rider), BlockFace.UP)).isTrue();

            assertThat(rider.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(0, 69, 1));
        }
    }

    @Nested
    @DisplayName("what it refuses")
    class WhatItRefuses {

        @Test
        void aSignThatIsOnlySomewhereToArriveAt() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING);

            assertThat(LIFT.act(at(SIGN, rider))).isFalse();

            assertThat(rider.wasMoved()).isFalse();
        }

        @Test
        void aShaftWithNoOtherFloorInIt() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "");
            SimpleActor rider = SimpleActor.at(STANDING);

            assertThat(LIFT.act(at(SIGN, rider))).isFalse();

            assertThat(rider.wasTold("nowhere to go")).isTrue();
        }

        @Test
        void aLandingWithNoFloorUnderIt() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            SimpleActor rider = SimpleActor.at(STANDING);

            assertThat(LIFT.act(at(SIGN, rider))).isFalse();

            assertThat(rider.wasTold("no floor")).isTrue();
        }

        @Test
        void aLandingWithNoRoomToStandOnIt() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "")
                    .with(new Vec3i(0, 70, 1), "stone");
            SimpleActor rider = SimpleActor.at(STANDING);

            assertThat(LIFT.act(at(SIGN, rider))).isFalse();

            assertThat(rider.wasTold("blocked")).isTrue();
        }

        @Test
        void somebodyWithoutThePermissionToRide() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);
            SimpleActor rider = SimpleActor.at(STANDING).allowedOnly();

            assertThat(LIFT.act(at(SIGN, rider))).isFalse();

            assertThat(rider.wasTold("may not use lifts")).isTrue();
            assertThat(rider.wasMoved()).isFalse();
        }

        @Test
        void redstone() {
            world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                    .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "");
            withFloorAt(70);

            boolean acted = LIFT.act(MechanicVisit.byRedstone(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, true));

            assertThat(acted).isFalse();
        }
    }

    @Test
    void willNotDropSomebodyFurtherThanTheSettingsAllow() {
        Settings tight = Settings.builder()
                .mechanics(Settings.DEFAULTS.mechanics().withLiftTolerance(2))
                .build();
        world.withSign(SIGN, BlockFace.SOUTH, "", Elevator.UP, "", "")
                .withSign(UPPER_SIGN, BlockFace.SOUTH, "", Elevator.STOP, "", "")
                // The floor is four blocks below the sign, which is further than allowed.
                .with(new Vec3i(0, 66, 1), "stone");
        SimpleActor rider = SimpleActor.at(STANDING);

        boolean acted = LIFT.act(MechanicVisit.byHand(
                world.signAt(SIGN).orElseThrow(), world, tight, rider));

        assertThat(acted).isFalse();
        assertThat(rider.wasTold("no floor")).isTrue();
    }
}
