// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.meter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.IntFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The two instruments held up to a block")
class MetersTest {

    private static String plain(Component reading) {
        return PlainTextComponentSerializer.plainText().serialize(reading);
    }

    /** The bar between the brackets, which is the part that has to line up. */
    private static String bar(String reading) {
        return reading.substring(reading.indexOf('['), reading.indexOf(']') + 1);
    }

    @Nested
    @DisplayName("however they are read")
    class Both {

        private static void behavesLikeAMeter(IntFunction<Component> instrument, String label) {
            assertThat(bar(plain(instrument.apply(0))))
                    .hasSameSizeAs(bar(plain(instrument.apply(Meters.FULL))));
            assertThat(bar(plain(instrument.apply(7))))
                    .hasSameSizeAs(bar(plain(instrument.apply(Meters.FULL))));
            assertThat(plain(instrument.apply(7))).startsWith(label + ": [").endsWith(" 7");
        }

        @Test
        @DisplayName("draw a bar of the same width, so two readings line up by eye")
        void drawTheSameWidthBar() {
            behavesLikeAMeter(Meters::power, "Ammeter");
            behavesLikeAMeter(Meters::light, "LightStone");
        }

        @Test
        @DisplayName("hold a reading the world could not give to one it could")
        void holdAnImpossibleReading() {
            assertThat(plain(Meters.power(99))).endsWith(" 15");
            assertThat(plain(Meters.light(-1))).endsWith(" 0");
        }

        @Test
        @DisplayName("say the number as well as drawing it, since a bar cannot be counted")
        void sayTheNumber() {
            assertThat(plain(Meters.power(11))).endsWith(" 11");
            assertThat(plain(Meters.light(3))).endsWith(" 3");
        }
    }

    @Nested
    @DisplayName("read as power")
    class Power {

        @Test
        @DisplayName("says nothing is carried when nothing is")
        void showsAnEmptyBar() {
            assertThat(bar(plain(Meters.power(0)))).isEqualTo("[" + "|".repeat(Meters.FULL) + "]");
        }

        @Test
        @DisplayName("names itself, so two instruments cannot be confused in the chat log")
        void namesItself() {
            assertThat(plain(Meters.power(5))).startsWith("Ammeter: [");
        }
    }

    @Nested
    @DisplayName("read as light")
    class Light {

        @Test
        @DisplayName("splits where hostile mobs stop spawning, which is the question being asked")
        void splitsAtTheSafeLevel() {
            assertThat(Meters.LIT_ENOUGH).isEqualTo(9);
        }

        @Test
        @DisplayName("names itself")
        void namesItself() {
            assertThat(plain(Meters.light(5))).startsWith("LightStone: [");
        }
    }
}
