// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("World sensors")
class SensorsTest {

    /** A south-facing sign at the origin, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(10, 64, 10);
    private static final Vec3i BACKING = new Vec3i(10, 64, 9);

    private static SimpleChipState.Builder sensor(SimpleChipWorld world, String... config) {
        String[] lines = new String[] {"", "[MCX205]", config.length > 0 ? config[0] : "",
            config.length > 1 ? config[1] : ""};
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign(lines)
                .inputs(true, false, false);
    }

    @Nested
    @DisplayName("liquid sensors")
    class LiquidSensors {

        @Test
        void looksOneBlockBelowTheBackingBlockByDefault() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(BACKING.add(0, -1, 0), "water");
            SimpleChipState state = sensor(world).build();

            Sensors.waterSensor().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void ignoresWaterSomewhereElse() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(SIGN.add(5, 0, 5), "water");
            SimpleChipState state = sensor(world).build();

            Sensors.waterSensor().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void readsItsOffsetFromTheSign() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -5, 0), "water");
            SimpleChipState state = sensor(world, "-5").build();

            Sensors.waterSensor().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void countsABubbleColumnAsWater() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(BACKING.add(0, -1, 0), "bubble_column");
            SimpleChipState state = sensor(world).build();

            Sensors.waterSensor().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theLavaSensorAnswersForLava() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -1, 0), "lava");
            SimpleChipState state = sensor(world).build();

            Sensors.lavaSensor().trigger(state);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void theLavaSensorIgnoresWater() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -1, 0), "water");
            SimpleChipState state = sensor(world).build();

            Sensors.lavaSensor().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void doesNothingWhileIdle() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -1, 0), "water");
            SimpleChipState state = sensor(world).inputs(false, false, false).build();

            Sensors.waterSensor().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("light sensor")
    class LightSensor {

        @ParameterizedTest(name = "light {0} against a threshold of 8 is {1}")
        @CsvSource({"0, false", "7, false", "8, true", "15, true"})
        void comparesAgainstItsThreshold(int light, boolean expected) {
            SimpleChipWorld world = new SimpleChipWorld().withAmbientLight(light);
            SimpleChipState state = sensor(world).build();

            Sensors.lightSensor().trigger(state);

            assertThat(state.mainOutput()).isEqualTo(expected);
        }

        @Test
        void readsItsThresholdFromTheSign() {
            SimpleChipWorld world = new SimpleChipWorld().withAmbientLight(5);
            SimpleChipState state = sensor(world, "4").build();

            Sensors.lightSensor().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void looksWhereItsOffsetSaysTo() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withAmbientLight(0)
                    .withLight(BACKING.add(0, 3, 0), 15);
            SimpleChipState state = sensor(world, "10", "3").build();

            Sensors.lightSensor().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("weather sensors")
    class WeatherSensors {

        @Test
        void theRainSensorFollowsThePrecipitation() {
            SimpleChipState dry = sensor(new SimpleChipWorld()).build();
            SimpleChipState wet = sensor(new SimpleChipWorld().withWeather(true, false)).build();

            Sensors.rainSensor().trigger(dry);
            Sensors.rainSensor().trigger(wet);

            assertThat(dry.mainOutput()).isFalse();
            assertThat(wet.mainOutput()).isTrue();
        }

        @Test
        void theStormSensorNeedsThunderNotJustRain() {
            SimpleChipState raining = sensor(new SimpleChipWorld().withWeather(true, false)).build();
            SimpleChipState storming = sensor(new SimpleChipWorld().withWeather(true, true)).build();

            Sensors.stormSensor().trigger(raining);
            Sensors.stormSensor().trigger(storming);

            assertThat(raining.mainOutput()).isFalse();
            assertThat(storming.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("block detector")
    class BlockDetector {

        private SimpleChipState detecting(SimpleChipWorld world, String block, String depth) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("", "[MCX205]", block, depth)
                    .inputs(true, false, false)
                    .build();
        }

        @Test
        void findsTheNamedBlockBelowTheSign() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -5, 0), "diamond_ore");
            SimpleChipState state = detecting(world, "diamond_ore", "");

            Sensors.blockDetector().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void reportsNothingWhenTheBlockIsAbsent() {
            SimpleChipState state = detecting(new SimpleChipWorld(), "diamond_ore", "");

            Sensors.blockDetector().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void staysWithinItsSearchDepth() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -10, 0), "diamond_ore");

            SimpleChipState shallow = detecting(world, "diamond_ore", "3");
            Sensors.blockDetector().trigger(shallow);
            assertThat(shallow.mainOutput()).isFalse();

            SimpleChipState deep = detecting(world, "diamond_ore", "20");
            Sensors.blockDetector().trigger(deep);
            assertThat(deep.mainOutput()).isTrue();
        }

        @Test
        void doesNotLookAtTheBackingBlockItself() {
            // The search starts one below the block the sign hangs on, so a chip cannot detect
            // the very block it is mounted against.
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING, "diamond_ore");
            SimpleChipState state = detecting(world, "diamond_ore", "1");

            Sensors.blockDetector().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void noticesTheBlockBeingRemoved() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -3, 0), "diamond_ore");
            SelfTriggeringICLogic detector = Sensors.blockDetector();
            SimpleChipState found = detecting(world, "diamond_ore", "");

            detector.trigger(found);
            assertThat(found.mainOutput()).isTrue();

            world.withBlock(BACKING.add(0, -3, 0), "air");
            SimpleChipState gone = detecting(world, "diamond_ore", "");
            detector.trigger(gone);

            assertThat(gone.mainOutput()).isFalse();
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "   "})
        void reportsNothingWithoutABlockToLookFor(String block) {
            SimpleChipState state = detecting(new SimpleChipWorld(), block, "");

            Sensors.blockDetector().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void acceptsAFullyQualifiedBlockName() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -2, 0), "diamond_ore");
            SimpleChipState state = detecting(world, "minecraft:diamond_ore", "");

            Sensors.blockDetector().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void acceptsANameWrittenWithSpaces() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACKING.add(0, -2, 0), "iron_block");
            SimpleChipState state = detecting(world, "Iron Block", "");

            Sensors.blockDetector().trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Test
    void anUnloadedPositionReadsAsAir() {
        Vec3i target = BACKING.add(0, -1, 0);
        SimpleChipWorld world = new SimpleChipWorld().withBlock(target, "water").withUnloaded(target);
        SimpleChipState state = sensor(world).build();

        Sensors.waterSensor().trigger(state);

        assertThat(state.mainOutput()).isFalse();
    }
}
