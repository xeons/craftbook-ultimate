// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.chair;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Sitting down")
class ChairsTest {

    @Nested
    @DisplayName("the seat inside a block")
    class TheSeat {

        @Test
        @DisplayName("is in the middle of it, along both level sides")
        void isInTheMiddle() {
            Vec3d seat = Chairs.seatIn(new Vec3i(10, 64, -3));

            assertThat(seat.x()).isEqualTo(10.5);
            assertThat(seat.z()).isEqualTo(-2.5);
        }

        @Test
        @DisplayName("sits low enough that a rider's legs are on the block, not over it")
        void sitsLow() {
            assertThat(Chairs.seatIn(new Vec3i(0, 64, 0)).y()).isEqualTo(64.3);
        }
    }

    @Nested
    @DisplayName("where somebody standing up is put")
    class StandingUp {

        @Test
        @DisplayName("is where they were sitting, before anywhere else")
        void isWhereTheyWere() {
            Vec3i chair = new Vec3i(4, 70, 8);

            assertThat(Chairs.standingPlaces(chair)).first().isEqualTo(chair);
        }

        @Test
        @DisplayName("is never further than one block away")
        void isNeverFurtherThanOneBlock() {
            Vec3i chair = new Vec3i(0, 0, 0);

            assertThat(Chairs.standingPlaces(chair)).allSatisfy(place -> {
                assertThat(Math.abs(place.x())).isLessThanOrEqualTo(1);
                assertThat(Math.abs(place.y())).isLessThanOrEqualTo(1);
                assertThat(Math.abs(place.z())).isLessThanOrEqualTo(1);
            });
        }

        @Test
        @DisplayName("covers every block around the chair, each once")
        void coversEveryBlockAround() {
            List<Vec3i> places = Chairs.standingPlaces(new Vec3i(0, 0, 0));

            assertThat(places).hasSize(27).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("tries one down before one up, so nobody is stood on the chair's back")
        void triesDownBeforeUp() {
            List<Vec3i> places = Chairs.standingPlaces(new Vec3i(0, 0, 0));

            assertThat(places.indexOf(new Vec3i(0, -1, 0)))
                    .isLessThan(places.indexOf(new Vec3i(0, 1, 0)));
        }
    }

    @Nested
    @DisplayName("the wait between sitting down twice")
    class TheWait {

        @Test
        @DisplayName("holds somebody who has only just stood up")
        void holdsSomebodyWhoJustStoodUp() {
            assertThat(Chairs.mayStandAgain(1000, 1001)).isFalse();
        }

        @Test
        @DisplayName("is over once it has been waited out")
        void isOverOnceWaited() {
            assertThat(Chairs.mayStandAgain(1000, 1000 + Chairs.COOLDOWN_SECONDS + 1)).isTrue();
        }

        @Test
        @DisplayName("is not over at the very second it runs out")
        void isNotOverOnTheSecond() {
            assertThat(Chairs.mayStandAgain(1000, 1000 + Chairs.COOLDOWN_SECONDS)).isFalse();
        }
    }

    @Nested
    @DisplayName("a healing chair")
    class Healing {

        @Test
        @DisplayName("heals by what it is set to")
        void healsByWhatItIsSetTo() {
            assertThat(Chairs.healed(10, 20, 1)).isEqualTo(11);
        }

        @Test
        @DisplayName("never heals past what somebody can hold")
        void neverHealsPastTheMaximum() {
            assertThat(Chairs.healed(19.5, 20, 5)).isEqualTo(20);
        }

        @Test
        @DisplayName("does nothing rather than hurting, where it is set to heal backwards")
        void neverHurts() {
            assertThat(Chairs.healed(10, 20, -5)).isEqualTo(10);
        }

        @Test
        @DisplayName("leaves somebody already over their maximum where they are")
        void leavesSomebodyOverTheirMaximum() {
            assertThat(Chairs.healed(24, 20, 1)).isEqualTo(24);
        }
    }

    @Nested
    @DisplayName("the sign that makes a chair heal")
    class TheSign {

        @Test
        @DisplayName("is recognised however it is capitalised")
        void isRecognisedHoweverCapitalised() {
            assertThat(Chairs.isHealSign("[sit heal]")).isTrue();
            assertThat(Chairs.isHealSign("[SIT HEAL]")).isTrue();
        }

        @Test
        @DisplayName("is recognised with space around it, as a sign is often written")
        void isRecognisedWithSpaceAround() {
            assertThat(Chairs.isHealSign("  [Sit Heal] ")).isTrue();
        }

        @Test
        @DisplayName("is not any other sign")
        void isNotAnyOtherSign() {
            assertThat(Chairs.isHealSign("[Sit]")).isFalse();
            assertThat(Chairs.isHealSign("")).isFalse();
        }
    }
}
