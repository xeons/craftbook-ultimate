// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Lightning chips")
class LightningChipsTest {

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    private SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState.Builder chip(String model, String third, String fourth) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("BOLT", "[" + model + "]", third, fourth);
    }

    /** A generator that always gives the same number, so a chance can be tested exactly. */
    private static RandomGenerator always(int value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

    @Nested
    @DisplayName("striking one place")
    class StrikingOnePlace {

        @Test
        void strikesTheBlockTheSignHangsOn() {
            SimpleChipState state = chip("MCX255", "", "").inputs(true, false, false, false).build();

            LightningChips.lightning().trigger(state);

            assertThat(world.lightningStrikes()).containsExactly(BEHIND);
        }

        @Test
        void strikesAsFarAboveAsTheThirdLineSays() {
            SimpleChipState state = chip("MCX255", "20", "").inputs(true, false, false, false).build();

            LightningChips.lightning().trigger(state);

            assertThat(world.lightningStrikes()).containsExactly(BEHIND.add(0, 20, 0));
        }

        @Test
        void reportsTheStrikeOnItsOutput() {
            SimpleChipState state = chip("MCX255", "", "").inputs(true, false, false, false).build();

            LightningChips.lightning().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void goesLowAgainWhenNothingDrivesIt() {
            SimpleChipState state = chip("MCX255", "", "").build();

            LightningChips.lightning().trigger(state);

            assertThat(state.output(0)).isFalse();
            assertThat(world.lightningStrikes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("striking an area")
    class StrikingAnArea {

        @Test
        void strikesEveryBlockOfTheCubeAroundItsSupport() {
            // A blank third line means a reach of one, which is a cube three blocks on a side.
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        world.withBlock(BEHIND.add(x, y, z), "stone");
                    }
                }
            }
            SimpleChipState state = chip("MC1203", "", "").inputs(true, false, false, false).build();

            LightningChips.zeusBolt(always(0)).trigger(state);

            assertThat(world.lightningStrikes()).hasSize(27);
        }

        @Test
        void leavesTheAirAlone() {
            world.withBlock(BEHIND, "stone");
            SimpleChipState state = chip("MC1203", "", "").inputs(true, false, false, false).build();

            LightningChips.zeusBolt(always(0)).trigger(state);

            assertThat(world.lightningStrikes()).containsExactly(BEHIND);
        }

        @Test
        void strikesNothingWhenTheChanceIsZero() {
            world.withBlock(BEHIND, "stone");
            SimpleChipState state = chip("MC1203", "", "0").inputs(true, false, false, false).build();

            LightningChips.zeusBolt(always(0)).trigger(state);

            assertThat(world.lightningStrikes()).isEmpty();
        }

        @Test
        void movesItsMiddleWhereTheThirdLineSays() {
            world.withBlock(BEHIND.add(5, 0, 0), "stone");
            SimpleChipState state = chip("MC1203", "0=5:0:0", "").inputs(true, false, false, false).build();

            LightningChips.zeusBolt(always(0)).trigger(state);

            assertThat(world.lightningStrikes()).containsExactly(BEHIND.add(5, 0, 0));
        }

        @Test
        void takesADifferentReachOnEachAxis() {
            for (int x = -2; x <= 2; x++) {
                world.withBlock(BEHIND.add(x, 0, 0), "stone");
            }
            SimpleChipState state = chip("MC1203", "2,0,0", "").inputs(true, false, false, false).build();

            LightningChips.zeusBolt(always(0)).trigger(state);

            assertThat(world.lightningStrikes()).hasSize(5);
        }

        @Test
        void reachesNoFurtherThanTheSettingsAllow() {
            for (int x = -2; x <= 2; x++) {
                world.withBlock(BEHIND.add(x, 0, 0), "stone");
            }
            SimpleChipState state = chip("MC1203", "2,0,0", "")
                    .settings(Settings.builder().maxRadius(1).build())
                    .inputs(true, false, false, false)
                    .build();

            LightningChips.zeusBolt(always(0)).trigger(state);

            assertThat(world.lightningStrikes()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("smiting what is nearby")
    class SmitingWhatIsNearby {

        @Test
        void strikesEverythingInRange() {
            world.withBystander(SimpleBystander.monster("zombie").at(new Vec3d(2, 64, 0)));
            world.withBystander(SimpleBystander.player("Notch").at(new Vec3d(0, 64, 1)));
            SimpleChipState state = chip("MCX256", "", "").inputs(true, false, false, false).build();

            LightningChips.holySmite().trigger(state);

            assertThat(world.lightningStrikes()).hasSize(2);
        }

        @Test
        void leavesAnythingBeyondItsRange() {
            world.withBystander(SimpleBystander.monster("zombie").at(new Vec3d(30, 64, 0)));
            SimpleChipState state = chip("MCX256", "", "5").inputs(true, false, false, false).build();

            LightningChips.holySmite().trigger(state);

            assertThat(world.lightningStrikes()).isEmpty();
        }

        @Test
        void strikesOnEveryTickWhenItTicks() {
            world.withBystander(SimpleBystander.monster("zombie").at(new Vec3d(0, 64, 1)));
            SimpleChipState state = chip("MCX256", "", "").build();

            LightningChips.holySmite().tick(state);

            assertThat(world.lightningStrikes()).hasSize(1);
        }
    }
}
