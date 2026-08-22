// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The sign that sends somebody somewhere else")
class TeleportersTest {

    @Nested
    @DisplayName("reading where it goes")
    class Destination {

        @Test
        @DisplayName("is the coordinates on its third line")
        void isTheCoordinates() {
            assertThat(Teleporters.destination("10:64:-20"))
                    .contains(new Vec3d(10, 64, -20));
        }

        @Test
        @DisplayName("keeps a fraction a builder meant")
        void keepsAFraction() {
            assertThat(Teleporters.destination("10.5:64:-20.5"))
                    .contains(new Vec3d(10.5, 64, -20.5));
        }

        @Test
        @DisplayName("is nowhere for a sign that only says people arrive there")
        void arrivalGoesNowhere() {
            assertThat(Teleporters.destination("ARRIVAL")).isEmpty();
            assertThat(Teleporters.isArrival("arrival")).isTrue();
        }

        @Test
        @DisplayName("is nowhere when the line is not three numbers")
        void rubbishGoesNowhere() {
            assertThat(Teleporters.destination("")).isEmpty();
            assertThat(Teleporters.destination("10:64")).isEmpty();
            assertThat(Teleporters.destination("10:64:20:30")).isEmpty();
            assertThat(Teleporters.destination("over:there:somewhere")).isEmpty();
        }
    }

    @Nested
    @DisplayName("as it is written")
    class Settling {

        @Test
        @DisplayName("makes a blank line into somewhere to arrive")
        void aBlankLineBecomesArrival() {
            assertThat(Teleporters.settled("")).isEqualTo(Teleporters.ARRIVAL);
            assertThat(Teleporters.settled("   ")).isEqualTo(Teleporters.ARRIVAL);
        }

        @Test
        @DisplayName("settles however a builder spelled arrival on the one spelling")
        void arrivalIsSettled() {
            assertThat(Teleporters.settled("arrival")).isEqualTo(Teleporters.ARRIVAL);
            assertThat(Teleporters.settled("Arrival")).isEqualTo(Teleporters.ARRIVAL);
        }

        @Test
        @DisplayName("leaves coordinates as the builder wrote them, so they can still read them")
        void coordinatesAreLeftAlone() {
            assertThat(Teleporters.settled("10:64:-20")).isEqualTo("10:64:-20");
        }
    }

    @Nested
    @DisplayName("how far it may reach")
    class Range {

        private static final Vec3d HERE = new Vec3d(0, 64, 0);

        @Test
        @DisplayName("is anywhere at all when no limit is set")
        void reachesAnywhereWithoutALimit() {
            assertThat(Teleporters.withinRange(HERE, new Vec3d(9999, 64, 9999), -1)).isTrue();
        }

        @Test
        @DisplayName("stops at the limit when one is")
        void stopsAtTheLimit() {
            assertThat(Teleporters.withinRange(HERE, new Vec3d(0, 64, 10), 20)).isTrue();
            assertThat(Teleporters.withinRange(HERE, new Vec3d(0, 64, 30), 20)).isFalse();
        }

        @Test
        @DisplayName("counts somewhere exactly at the limit as near enough")
        void theLimitItselfIsNearEnough() {
            assertThat(Teleporters.withinRange(HERE, new Vec3d(0, 64, 20), 20)).isTrue();
        }
    }
}
