// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.boat;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.BoatHabits;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("What every boat does")
class BoatBehaviourTest {

    private static final BoatHabits OFF = BoatHabits.DEFAULTS;

    private static StubBoat boat() {
        return new StubBoat();
    }

    @Nested
    @DisplayName("when it has been left empty")
    class Decay {

        @Test
        @DisplayName("is left alone on a server that has not asked for decay")
        void leftAloneByDefault() {
            assertThat(BoatBehaviour.hasSatEmpty(boat(), OFF)).isFalse();
        }

        @Test
        @DisplayName("is taken away once the wait is over")
        void takenAwayWhenEmpty() {
            BoatHabits habits = OFF.withDecay(40, true);
            assertThat(BoatBehaviour.hasSatEmpty(boat(), habits)).isTrue();
        }

        @Test
        @DisplayName("is spared when somebody climbed back in while it was waiting")
        void sparedWhenSomebodyGotBackIn() {
            BoatHabits habits = OFF.withDecay(40, true);
            StubBoat occupied = boat().carrying(SimpleBystander.player("Ada"));

            assertThat(BoatBehaviour.hasSatEmpty(occupied, habits)).isFalse();
        }

        @Test
        @DisplayName("is spared when it has already gone")
        void sparedWhenAlreadyGone() {
            BoatHabits habits = OFF.withDecay(40, true);
            assertThat(BoatBehaviour.hasSatEmpty(boat().gone(), habits)).isFalse();
        }
    }

    @Nested
    @DisplayName("when it runs into something")
    class RunningDown {

        private final Bystander cow = SimpleBystander.animal("cow");

        @Test
        @DisplayName("does nothing on a server that has not asked for it")
        void doesNothingByDefault() {
            StubBoat rowed = boat().carrying(SimpleBystander.player("Ada"));
            assertThat(BoatBehaviour.runDown(rowed, cow, OFF)).isEmpty();
        }

        @Test
        @DisplayName("does nothing when nobody is aboard, since a drifting boat is not a weapon")
        void doesNothingWhenEmpty() {
            BoatHabits habits = OFF.withRunDown(true, false, false);
            assertThat(BoatBehaviour.runDown(boat(), cow, habits)).isEmpty();
        }

        @Test
        @DisplayName("hurts a creature it runs into")
        void hurtsACreature() {
            BoatHabits habits = OFF.withRunDown(true, false, false);
            StubBoat rowed = boat()
                    .carrying(SimpleBystander.player("Ada"))
                    .going(new Vec3d(1, 0, 0));

            Optional<BoatBehaviour.RunDown> hit = BoatBehaviour.runDown(rowed, cow, habits);

            assertThat(hit).isPresent();
            assertThat(hit.get().removes()).isFalse();
            assertThat(hit.get().thrownClear().y()).isGreaterThan(0);
        }

        @Test
        @DisplayName("never hurts its own rider")
        void neverHurtsItsOwnRider() {
            BoatHabits habits = OFF.withRunDown(true, false, false);
            Bystander rider = SimpleBystander.player("Ada");
            StubBoat rowed = boat().carrying(rider);

            assertThat(BoatBehaviour.runDown(rowed, rider, habits)).isEmpty();
        }

        @Test
        @DisplayName("leaves other boats alone unless it is told not to")
        void leavesOtherBoatsAlone() {
            BoatHabits habits = OFF.withRunDown(true, false, false);
            StubBoat rowed = boat().carrying(SimpleBystander.player("Ada"));

            assertThat(BoatBehaviour.runDown(
                    rowed, SimpleBystander.of("dark_oak_boat").asObject(), habits)).isEmpty();
            assertThat(BoatBehaviour.runDown(
                    rowed, SimpleBystander.of("bamboo_raft").asObject(), habits)).isEmpty();
        }

        @Test
        @DisplayName("takes away something that cannot be hurt, unless hurting is all it may do")
        void removesWhatCannotBeHurt() {
            StubBoat rowed = boat().carrying(SimpleBystander.player("Ada"));
            Bystander crate = SimpleBystander.of("item").asObject();

            assertThat(BoatBehaviour.runDown(rowed, crate, OFF.withRunDown(true, false, false)))
                    .get()
                    .extracting(BoatBehaviour.RunDown::removes)
                    .isEqualTo(true);

            assertThat(BoatBehaviour.runDown(rowed, crate, OFF.withRunDown(true, true, false)))
                    .isEmpty();
        }
    }

    /** A boat that is wherever it is told to be. */
    private static final class StubBoat implements Boat {

        private List<Bystander> riders = List.of();
        private Vec3d velocity = Vec3d.ZERO;
        private boolean present = true;

        StubBoat carrying(Bystander rider) {
            this.riders = List.of(rider);
            return this;
        }

        StubBoat going(Vec3d velocity) {
            this.velocity = velocity;
            return this;
        }

        StubBoat gone() {
            this.present = false;
            return this;
        }

        @Override
        public Vec3d position() {
            return Vec3d.ZERO;
        }

        @Override
        public Vec3d velocity() {
            return velocity;
        }

        @Override
        public List<Bystander> riders() {
            return riders;
        }

        @Override
        public boolean isPresent() {
            return present;
        }
    }
}
