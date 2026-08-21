// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Where each rider has said they are going")
class StationsTest {

    private static final UUID RIDER = UUID.nameUUIDFromBytes("rider".getBytes());

    @Nested
    @DisplayName("remembering a destination")
    class RememberingADestination {

        @Test
        void hasNoneUntilSomebodySays() {
            assertThat(new Stations().destination(RIDER)).isEmpty();
        }

        @Test
        void keepsWhatWasSaid() {
            Stations stations = new Stations();
            stations.setDestination(RIDER, "Central");

            assertThat(stations.destination(RIDER)).contains("central");
        }

        @Test
        void replacesAnEarlierDestination() {
            Stations stations = new Stations();
            stations.setDestination(RIDER, "Central");
            stations.setDestination(RIDER, "North");

            assertThat(stations.destination(RIDER)).contains("north");
        }

        @Test
        void forgetsOnAsking() {
            Stations stations = new Stations();
            stations.setDestination(RIDER, "Central");

            assertThat(stations.clearDestination(RIDER)).isTrue();
            assertThat(stations.destination(RIDER)).isEmpty();
        }

        @Test
        void reportsHavingNothingToForget() {
            assertThat(new Stations().clearDestination(RIDER)).isFalse();
        }
    }

    @Nested
    @DisplayName("matching a name against a pattern")
    class MatchingANameAgainstAPattern {

        @Test
        void matchesTheSameNameWhicheverWayItWasWritten() {
            assertThat(Stations.matches("Central", "central")).isTrue();
            assertThat(Stations.matches("central", "CENTRAL")).isTrue();
        }

        @Test
        void refusesADifferentName() {
            assertThat(Stations.matches("central", "north")).isFalse();
        }

        @Test
        void matchesEverythingBeginningTheSameWay() {
            assertThat(Stations.matches("northgate", "north*")).isTrue();
            assertThat(Stations.matches("north", "north*")).isTrue();
            assertThat(Stations.matches("southgate", "north*")).isFalse();
        }

        @Test
        void matchesEverythingEndingTheSameWay() {
            assertThat(Stations.matches("northjunction", "*junction")).isTrue();
            assertThat(Stations.matches("junction", "*junction")).isTrue();
            assertThat(Stations.matches("northplatform", "*junction")).isFalse();
        }

        @Test
        void matchesAcrossTheMiddle() {
            assertThat(Stations.matches("northeastgate", "north*gate")).isTrue();
            assertThat(Stations.matches("northgate", "north*gate")).isTrue();
        }

        @Test
        void refusesWhenWhatFollowsTheWildcardIsMissing() {
            // The old matcher gave up here and said yes, so a junction asking for one branch
            // claimed every cart that reached it.
            assertThat(Stations.matches("northeastpier", "north*gate")).isFalse();
        }

        @Test
        void refusesANameShorterThanThePattern() {
            assertThat(Stations.matches("nor", "north")).isFalse();
        }

        @Test
        void matchesAnythingAtAllAgainstABareWildcard() {
            assertThat(Stations.matches("anywhere", "*")).isTrue();
            assertThat(Stations.matches("", "*")).isTrue();
        }

        @Test
        void treatsSeveralWildcardsAsOne() {
            assertThat(Stations.matches("northgate", "north***gate")).isTrue();
        }
    }

    @Test
    void answersWhetherARiderIsHeadingSomewhere() {
        Stations stations = new Stations();
        stations.setDestination(RIDER, "northgate");

        assertThat(stations.isHeadingFor(RIDER, "north*")).isTrue();
        assertThat(stations.isHeadingFor(RIDER, "south*")).isFalse();
        assertThat(stations.isHeadingFor(UUID.randomUUID(), "north*")).isFalse();
    }
}
