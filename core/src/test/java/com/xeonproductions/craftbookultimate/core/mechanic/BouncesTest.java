// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The block that throws whoever jumps on it")
class BouncesTest {

    @Nested
    @DisplayName("reading what its sign asks for")
    class Reading {

        @Test
        @DisplayName("throws you straight up when the sign names one number")
        void oneNumberIsStraightUp() {
            Bounces.Bounce bounce = Bounces.parse("5").orElseThrow();

            assertThat(bounce.speed()).isEqualTo(new Vec3d(0, 5, 0));
        }

        @Test
        @DisplayName("takes a push along the ground when the sign names three")
        void threeNumbersPushAlongTheGround() {
            Bounces.Bounce bounce = Bounces.parse("2,1,2").orElseThrow();

            assertThat(bounce.speed()).isEqualTo(new Vec3d(2, 1, 2));
        }

        @Test
        @DisplayName("hops when the sign says nothing")
        void aBlankSignHops() {
            Bounces.Bounce bounce = Bounces.parse("").orElseThrow();

            assertThat(bounce.speed()).isEqualTo(new Vec3d(0, Bounces.DEFAULT_UP, 0));
        }

        @Test
        @DisplayName("does nothing at all when the numbers are wrong, rather than quietly hopping")
        void rubbishThrowsNobody() {
            assertThat(Bounces.parse("up")).isEmpty();
            assertThat(Bounces.parse("1,2")).isEmpty();
            assertThat(Bounces.parse("1,2,3,4")).isEmpty();
        }

        @Test
        @DisplayName("does not mind the spaces a builder leaves around the numbers")
        void ignoresSpaces() {
            assertThat(Bounces.parse(" 2 , 1 , 2 ").orElseThrow().speed())
                    .isEqualTo(new Vec3d(2, 1, 2));
        }
    }

    @Nested
    @DisplayName("throwing somebody")
    class Throwing {

        @Test
        @DisplayName("follows where they are looking by default")
        void followsTheirFacing() {
            Bounces.Bounce bounce = Bounces.parse("2,1,2").orElseThrow();

            assertThat(bounce.alongFacing()).isTrue();
            assertThat(bounce.forFacing(0)).isNotEqualTo(bounce.forFacing(90));
        }

        @Test
        @DisplayName("ignores where they are looking when the sign is marked with a !")
        void ignoresFacingWhenMarked() {
            Bounces.Bounce bounce = Bounces.parse("!2,1,2").orElseThrow();

            assertThat(bounce.alongFacing()).isFalse();
            assertThat(bounce.forFacing(0)).isEqualTo(bounce.forFacing(90))
                    .isEqualTo(new Vec3d(2, 1, 2));
        }

        @Test
        @DisplayName("never turns the upward part, since up is up wherever you stand")
        void neverTurnsTheUpwardPart() {
            Bounces.Bounce bounce = Bounces.parse("2,7,2").orElseThrow();

            assertThat(bounce.forFacing(0).y()).isEqualTo(7);
            assertThat(bounce.forFacing(123).y()).isEqualTo(7);
        }

        @Test
        @DisplayName("takes a marked sign's numbers with no facing at all")
        void aMarkedSignIsAlsoAllowedOneNumber() {
            assertThat(Bounces.parse("!5").orElseThrow().speed()).isEqualTo(new Vec3d(0, 5, 0));
        }

        @Test
        @DisplayName("throws somebody looking north along the way they are looking")
        void throwsAlongTheWayTheyLook() {
            Bounces.Bounce bounce = Bounces.parse("1,0,1").orElseThrow();

            // Minecraft's yaw of -90 is due east, which the turn puts wholly on x.
            Vec3d east = bounce.forFacing(-90);
            assertThat(east.x()).isCloseTo(1, within(1e-9));
            assertThat(east.z()).isCloseTo(0, within(1e-9));
        }
    }
}
