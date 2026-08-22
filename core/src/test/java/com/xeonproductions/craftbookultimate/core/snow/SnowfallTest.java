// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.snow;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.SnowSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Snow that piles, slumps and melts")
class SnowfallTest {

    private static final SnowSettings OFF = SnowSettings.DEFAULTS;

    private static final Vec3i GROUND = new Vec3i(0, 64, 0);

    private final Ground ground = new Ground();

    private Snowfall with(SnowSettings settings) {
        return new Snowfall(ground, settings);
    }

    @Nested
    @DisplayName("piling up")
    class Piling {

        @Test
        @DisplayName("settles a first layer on solid ground")
        void settlesOnSolidGround() {
            ground.solidAt(GROUND.add(0, -1, 0));

            with(OFF).pile(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(1);
        }

        @Test
        @DisplayName("falls to the ground rather than hanging where it landed")
        void fallsToTheGround() {
            ground.solidAt(new Vec3i(0, 60, 0));

            with(OFF).pile(new Vec3i(0, 64, 0));

            assertThat(ground.depthAt(new Vec3i(0, 61, 0))).isEqualTo(1);
            assertThat(ground.depthAt(new Vec3i(0, 64, 0))).isZero();
        }

        @Test
        @DisplayName("settles nowhere at all over a bottomless drop")
        void settlesNowhereOverAVoid() {
            with(OFF).pile(GROUND);

            assertThat(ground.written()).isEmpty();
        }

        @Test
        @DisplayName("stops where the game stops unless piling was asked for")
        void stopsWhereTheGameStops() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.depth(GROUND, SnowWorld.FULL - 1);

            with(OFF).pile(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(SnowWorld.FULL - 1);
        }

        @Test
        @DisplayName("piles into a full block when it was")
        void pilesIntoAFullBlock() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.depth(GROUND, SnowWorld.FULL - 1);

            with(new SnowSettings(true, false, false, false, false, false)).pile(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(SnowWorld.FULL);
        }

        @Test
        @DisplayName("starts the next block up once one is full, when piling was asked for")
        void carriesOnAboveAFullBlock() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.depth(GROUND, SnowWorld.FULL);

            with(new SnowSettings(true, false, false, false, false, false)).pile(GROUND);

            assertThat(ground.depthAt(GROUND.add(0, 1, 0))).isEqualTo(1);
        }

        @Test
        @DisplayName("freezes the water under it when that was asked for")
        void freezesWaterBeneath() {
            Vec3i below = GROUND.add(0, -1, 0);
            ground.solidAt(below);
            ground.waterAt(below);

            with(new SnowSettings(false, false, true, false, false, false)).pile(GROUND);

            assertThat(ground.frozen()).contains(below);
        }

        @Test
        @DisplayName("leaves the water alone when it was not")
        void leavesWaterAloneByDefault() {
            Vec3i below = GROUND.add(0, -1, 0);
            ground.solidAt(below);
            ground.waterAt(below);

            with(OFF).pile(GROUND);

            assertThat(ground.frozen()).isEmpty();
        }
    }

    @Nested
    @DisplayName("melting")
    class Melting {

        @Test
        @DisplayName("takes a layer off")
        void takesALayerOff() {
            ground.depth(GROUND, 4);

            with(OFF).melt(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(3);
        }

        @Test
        @DisplayName("clears the ground with the last layer")
        void clearsTheGround() {
            ground.depth(GROUND, 1);

            with(OFF).melt(GROUND);

            assertThat(ground.depthAt(GROUND)).isZero();
        }

        @Test
        @DisplayName("stops at what the game would have left when only a partial melt was asked")
        void stopsAtTheVanillaDepth() {
            ground.depth(GROUND, 1);

            with(new SnowSettings(false, false, false, true, true, false)).melt(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("slumping")
    class Slumping {

        @Test
        @DisplayName("stays in a column on a server that has not asked for it")
        void staysPutByDefault() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.solidAt(GROUND.add(1, -1, 0));
            ground.depth(GROUND, 6);

            with(OFF).slump(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(6);
        }

        @Test
        @DisplayName("slides a layer into the lower ground beside it")
        void slidesIntoTheLowerGround() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.solidAt(GROUND.add(1, -1, 0));
            ground.depth(GROUND, 6);

            with(dispersing()).slump(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(5);
            assertThat(ground.depthAt(GROUND.add(1, 0, 0))).isEqualTo(1);
        }

        @Test
        @DisplayName("leaves a slope alone rather than shuffling snow back and forth")
        void leavesASlopeAlone() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.solidAt(GROUND.add(1, -1, 0));
            ground.depth(GROUND, 4);
            ground.depth(GROUND.add(1, 0, 0), 3);

            with(dispersing()).slump(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(4);
        }

        @Test
        @DisplayName("never spreads a dusting sideways, only downward")
        void aDustingOnlyFalls() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.solidAt(GROUND.add(1, -1, 0));
            ground.depth(GROUND, 1);

            with(dispersing()).slump(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(1);
        }

        private static SnowSettings dispersing() {
            return new SnowSettings(false, true, false, false, false, false);
        }
    }

    @Nested
    @DisplayName("as the game ticks a block")
    class Ticking {

        @Test
        @DisplayName("does nothing where there is no snow")
        void doesNothingWithoutSnow() {
            with(OFF).tick(GROUND);

            assertThat(ground.written()).isEmpty();
        }

        @Test
        @DisplayName("gathers more where it is freezing under an open sky")
        void gathersWhereItIsFreezing() {
            ground.solidAt(GROUND.add(0, -1, 0));
            ground.depth(GROUND, 2);
            ground.freezing = true;

            with(OFF).tick(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(3);
        }

        @Test
        @DisplayName("goes away where it is warm under an open sky, when melting was asked for")
        void goesAwayWhereItIsWarm() {
            ground.depth(GROUND, 2);
            ground.warm = true;

            with(new SnowSettings(false, false, false, true, false, false)).tick(GROUND);

            assertThat(ground.depthAt(GROUND)).isEqualTo(1);
        }
    }

    /** A world made of whatever a test puts in it. */
    private static final class Ground implements SnowWorld {

        private final Map<Vec3i, Integer> depths = new HashMap<>();
        private final Set<Vec3i> solid = new HashSet<>();
        private final Set<Vec3i> water = new HashSet<>();
        private final Set<Vec3i> frozen = new HashSet<>();
        private final Set<Vec3i> written = new HashSet<>();

        private boolean freezing;
        private boolean warm;

        void solidAt(Vec3i at) {
            solid.add(at);
        }

        void waterAt(Vec3i at) {
            water.add(at);
        }

        void depth(Vec3i at, int depth) {
            depths.put(at, depth);
        }

        Set<Vec3i> frozen() {
            return frozen;
        }

        /** Everywhere a depth was actually written, so a test can say nothing happened at all. */
        Set<Vec3i> written() {
            return written;
        }

        @Override
        public int depthAt(Vec3i at) {
            return depths.getOrDefault(at, 0);
        }

        @Override
        public void setDepth(Vec3i at, int depth) {
            depths.put(at, depth);
            written.add(at);
        }

        @Override
        public boolean isClear(Vec3i at) {
            return !solid.contains(at);
        }

        @Override
        public boolean canRestOn(Vec3i at) {
            return solid.contains(at);
        }

        @Override
        public boolean isWater(Vec3i at) {
            return water.contains(at);
        }

        @Override
        public void freeze(Vec3i at) {
            frozen.add(at);
        }

        @Override
        public boolean isFreezing(Vec3i at) {
            return freezing;
        }

        @Override
        public boolean isWarm(Vec3i at) {
            return warm;
        }

        @Override
        public boolean seesSky(Vec3i at) {
            return true;
        }

        @Override
        public int floor() {
            return -64;
        }

        @Override
        public int ceiling() {
            return 320;
        }
    }
}
