// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Weather and time chips")
class WeatherChipsTest {

    private static SimpleChipState.Builder chip(SimpleChipWorld world) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .world(world)
                .sign("", "[MCX233]", "", "");
    }

    @Nested
    @DisplayName("simple weather control")
    class SimpleWeatherControl {

        @Test
        void startsRainWhileItsInputIsHeld() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world).inputs(true, false, false).build();

            WeatherChips.simpleWeatherControl().trigger(state);

            assertThat(world.isRaining()).isTrue();
            assertThat(world.isThundering()).isFalse();
        }

        @Test
        void stopsTheWeatherWhenItsInputDrops() {
            SimpleChipWorld world = new SimpleChipWorld().withWeather(true, true);
            SimpleChipState state = chip(world).inputs(false, false, false).build();

            WeatherChips.simpleWeatherControl().trigger(state);

            assertThat(world.isRaining()).isFalse();
            assertThat(world.isThundering()).isFalse();
        }

        @Test
        void startsAStormInThunderMode() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world)
                    .inputs(true, false, false)
                    .mode(ICMode.parse("t"))
                    .build();

            WeatherChips.simpleWeatherControl().trigger(state);

            assertThat(world.isThundering()).isTrue();
            assertThat(world.isRaining()).isTrue();
        }

        @Test
        void readsItsDurationFromTheSign() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .world(world)
                    .sign("", "[MCX233]", "600", "")
                    .inputs(true, false, false)
                    .build();

            WeatherChips.simpleWeatherControl().trigger(state);

            assertThat(world.rainDuration()).isEqualTo(600);
        }

        @Test
        void mirrorsItsInputOnItsOutput() {
            SimpleChipState state = chip(new SimpleChipWorld()).inputs(true, false, false).build();

            WeatherChips.simpleWeatherControl().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("weather control")
    class WeatherControl {

        private SimpleChipState clocked(SimpleChipWorld world, boolean rain, boolean thunder) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .world(world)
                    .sign("", "[MCT233]", "", "")
                    .inputs(true, rain, thunder)
                    .triggeredInput(0)
                    .build();
        }

        @ParameterizedTest(name = "rain {0} thunder {1}")
        @CsvSource({"true, false", "false, true", "true, true", "false, false"})
        void setsBothKindsOfWeatherIndependently(boolean rain, boolean thunder) {
            SimpleChipWorld world = new SimpleChipWorld();

            WeatherChips.weatherControl().trigger(clocked(world, rain, thunder));

            assertThat(world.isRaining()).isEqualTo(rain);
            assertThat(world.isThundering()).isEqualTo(thunder);
        }

        @Test
        void doesNothingUntilItsClockRises() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState idle = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .world(world)
                    .sign("", "[MCT233]", "", "")
                    .inputs(false, true, true)
                    .triggeredInput(0)
                    .build();

            WeatherChips.weatherControl().trigger(idle);

            assertThat(world.isRaining()).isFalse();
        }

        @Test
        void ignoresARunTriggeredByAnotherPin() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .world(world)
                    .sign("", "[MCT233]", "", "")
                    .inputs(true, true, false)
                    .triggeredInput(1)
                    .build();

            WeatherChips.weatherControl().trigger(state);

            assertThat(world.isRaining()).isFalse();
        }

        @Test
        void clearingWeatherLetsItChangeAgainImmediately() {
            SimpleChipWorld world = new SimpleChipWorld().withWeather(true, true);

            WeatherChips.weatherControl().trigger(clocked(world, false, false));

            assertThat(world.rainDuration()).isZero();
            assertThat(world.thunderDuration()).isZero();
        }
    }

    @Nested
    @DisplayName("time control")
    class TimeControl {

        private void jump(SimpleChipWorld world, long from, boolean toMorning) {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .world(world)
                    .sign("", "[MC3231]", "", "")
                    .inputs(true, toMorning, false)
                    .triggeredInput(0)
                    .worldTicks(from)
                    .build();

            WeatherChips.timeControlAdvanced().trigger(state);
        }

        @Test
        void jumpsForwardToMorning() {
            SimpleChipWorld world = new SimpleChipWorld();

            jump(world, 500, true);

            assertThat(world.worldTicks()).isEqualTo(1_000);
        }

        @Test
        void jumpsForwardToNight() {
            SimpleChipWorld world = new SimpleChipWorld();

            jump(world, 500, false);

            assertThat(world.worldTicks()).isEqualTo(13_000);
        }

        @Test
        void neverRewindsTheDay() {
            // Morning has already passed, so the world moves on to tomorrow rather than back.
            SimpleChipWorld world = new SimpleChipWorld();

            jump(world, 5_000, true);

            assertThat(world.worldTicks()).isEqualTo(25_000);
        }

        @Test
        void keepsTheDaysAlreadyElapsed() {
            SimpleChipWorld world = new SimpleChipWorld();

            jump(world, 24_000 * 7 + 500, true);

            assertThat(world.worldTicks()).isEqualTo(24_000 * 7 + 1_000);
        }

        @Test
        void doesNothingUntilItsClockRises() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState idle = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .world(world)
                    .sign("", "[MC3231]", "", "")
                    .inputs(false, true, false)
                    .triggeredInput(0)
                    .worldTicks(500)
                    .build();

            WeatherChips.timeControlAdvanced().trigger(idle);

            assertThat(world.worldTicks()).isZero();
        }
    }
}
