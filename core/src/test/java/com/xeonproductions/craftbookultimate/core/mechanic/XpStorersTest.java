// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Bottling the experience somebody is carrying")
class XpStorersTest {

    private static final int PER_BOTTLE = XpStorers.DEFAULT_PER_BOTTLE;

    /** More bottles than anybody could hold, which is what "no bottle needed" means. */
    private static final int PLENTY = Integer.MAX_VALUE;

    @Nested
    @DisplayName("how many bottles it fills")
    class Counting {

        @Test
        @DisplayName("is what the experience pays for")
        void isWhatTheExperiencePaysFor() {
            assertThat(XpStorers.bottlesFor(PER_BOTTLE * 3, PER_BOTTLE, PLENTY)).isEqualTo(3);
        }

        @Test
        @DisplayName("is none at all when there is not enough for one")
        void isNoneWithoutEnough() {
            assertThat(XpStorers.bottlesFor(PER_BOTTLE - 1, PER_BOTTLE, PLENTY)).isZero();
            assertThat(XpStorers.bottlesFor(0, PER_BOTTLE, PLENTY)).isZero();
        }

        @Test
        @DisplayName("is held to the bottles they are actually carrying")
        void isHeldToTheBottlesCarried() {
            assertThat(XpStorers.bottlesFor(PER_BOTTLE * 10, PER_BOTTLE, 2)).isEqualTo(2);
        }

        @Test
        @DisplayName("is none when they are carrying no bottles at all")
        void isNoneWithoutBottles() {
            assertThat(XpStorers.bottlesFor(PER_BOTTLE * 10, PER_BOTTLE, 0)).isZero();
        }
    }

    @Nested
    @DisplayName("what is left afterwards")
    class Remainder {

        @Test
        @DisplayName("is the change, so nothing is lost to rounding")
        void isTheChange() {
            int carried = PER_BOTTLE * 2 + 3;
            int bottles = XpStorers.bottlesFor(carried, PER_BOTTLE, PLENTY);

            assertThat(bottles).isEqualTo(2);
            assertThat(XpStorers.remainderAfter(carried, PER_BOTTLE, bottles)).isEqualTo(3);
        }

        @Test
        @DisplayName("is everything they had when no bottle was filled")
        void isEverythingWhenNothingWasFilled() {
            assertThat(XpStorers.remainderAfter(PER_BOTTLE - 1, PER_BOTTLE, 0))
                    .isEqualTo(PER_BOTTLE - 1);
        }

        @Test
        @DisplayName("is nothing when the experience divided exactly")
        void isNothingWhenItDividedExactly() {
            assertThat(XpStorers.remainderAfter(PER_BOTTLE * 4, PER_BOTTLE, 4)).isZero();
        }
    }

    @Nested
    @DisplayName("whether somebody has to crouch")
    class Crouching {

        @Test
        @DisplayName("is the usual way round by default: not while crouching")
        void mustNotByDefault() {
            assertThat(SneakState.MUST_NOT.passes(false)).isTrue();
            assertThat(SneakState.MUST_NOT.passes(true)).isFalse();
        }

        @Test
        @DisplayName("can be turned round, or turned off")
        void canBeTurnedRoundOrOff() {
            assertThat(SneakState.MUST.passes(true)).isTrue();
            assertThat(SneakState.MUST.passes(false)).isFalse();
            assertThat(SneakState.EITHER.passes(true)).isTrue();
            assertThat(SneakState.EITHER.passes(false)).isTrue();
        }

        @Test
        @DisplayName("reads what the fork's own files hold, as well as its own names")
        void readsBothSpellings() {
            assertThat(SneakState.of("true", SneakState.EITHER)).isEqualTo(SneakState.MUST);
            assertThat(SneakState.of("false", SneakState.EITHER)).isEqualTo(SneakState.MUST_NOT);
            assertThat(SneakState.of("must-not", SneakState.EITHER)).isEqualTo(SneakState.MUST_NOT);
            assertThat(SneakState.of("either", SneakState.MUST)).isEqualTo(SneakState.EITHER);
        }

        @Test
        @DisplayName("falls back where the file says something it cannot read")
        void fallsBackOnRubbish() {
            assertThat(SneakState.of("sideways", SneakState.MUST)).isEqualTo(SneakState.MUST);
        }

        @Test
        @DisplayName("writes back what it reads, so the file survives a round trip")
        void survivesARoundTrip() {
            for (SneakState state : SneakState.values()) {
                assertThat(SneakState.of(state.written(), SneakState.EITHER)).isEqualTo(state);
            }
        }
    }
}
