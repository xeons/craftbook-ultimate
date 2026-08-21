// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Chips configured from their sign")
class SignConfiguredChipsTest {

    @Nested
    @DisplayName("counter")
    class Counter {

        private SimpleChipState counting(String config, String current) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .inputs(true, false, false)
                    .sign("", "[MC3102]", config, current)
                    .build();
        }

        @Test
        void readsItsLimitFromTheSign() {
            SimpleChipState state = counting("2", "1");

            Latches.counterFromSign(true).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("2");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void stopsAtItsLimitWithoutTheInfiniteMarker() {
            SimpleChipState state = counting("2", "2");

            Latches.counterFromSign(true).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("2");
        }

        @Test
        void wrapsWithTheInfiniteMarker() {
            SimpleChipState state = counting("2:INF", "2");

            Latches.counterFromSign(true).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("0");
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "banana", "   ", ":INF"})
        void fallsBackToALimitOfFive(String config) {
            SimpleChipState state = counting(config, "4");

            Latches.counterFromSign(true).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("5");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void countsDownFromItsLimit() {
            SimpleChipState state = counting("3", "1");

            Latches.counterFromSign(false).trigger(state);

            assertThat(state.sign().trimmedText(3)).isEqualTo("0");
            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("random bits")
    class RandomBits {

        private int raisedFor(String config) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.SI5O)
                    .inputs(true)
                    .sign("", "[MC6020]", config, "")
                    .build();

            Routing.randomBitsFromSign(new Random(11)).trigger(state);

            int count = 0;
            for (boolean raised : state.outputs()) {
                if (raised) {
                    count++;
                }
            }
            return count;
        }

        @ParameterizedTest(name = "\"{0}\" raises {1}")
        @CsvSource({
            "2:2, 2",
            "4:4, 4",
            "0:0, 0",
        })
        void readsBothBoundsFromTheSign(String config, int expected) {
            assertThat(raisedFor(config)).isEqualTo(expected);
        }

        @Test
        void readsASingleValueAsTheUpperBound() {
            assertThat(raisedFor("3")).isBetween(0, 3);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "banana", "1:banana"})
        void fallsBackToTheFullRange(String config) {
            assertThat(raisedFor(config)).isBetween(0, 5);
        }
    }
}
