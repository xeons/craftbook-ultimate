// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.PotionDose;
import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Effect chips")
class EffectsTest {

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    private SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState.Builder chip(String model, String third, String fourth) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("EFFECT", "[" + model + "]", third, fourth);
    }

    /** A generator that always gives the same number, so a random choice can be tested exactly. */
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
    @DisplayName("potion area")
    class PotionArea {

        @Test
        void dosesPlayersInRange() {
            SimpleBystander walker = SimpleBystander.player("Notch").at(Vec3d.centreOf(BEHIND));
            world.withBystander(walker);
            SimpleChipState state = chip("MCX146", "SP:30:1", "").inputs(true, false, false, false).build();

            Effects.potionArea().trigger(state);

            assertThat(walker.doses())
                    .containsExactly(new PotionDose(Blocks.key("speed"), 600, 1));
        }

        @Test
        void leavesAnythingThatIsNotAPlayerAlone() {
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(Vec3d.centreOf(BEHIND));
            world.withBystander(zombie);
            SimpleChipState state = chip("MCX146", "SP:30:1", "").inputs(true, false, false, false).build();

            Effects.potionArea().trigger(state);

            assertThat(zombie.doses()).isEmpty();
        }

        @Test
        void dosesWhicheverSortItsFourthLineNames() {
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(Vec3d.centreOf(BEHIND));
            world.withBystander(zombie);
            SimpleChipState state = chip("MCX146", "PO:10:0", "5@M").inputs(true, false, false, false).build();

            Effects.potionArea().trigger(state);

            assertThat(zombie.doses()).hasSize(1);
        }

        @Test
        void treatsInfiniteAsADoseThatNeverWearsOff() {
            SimpleBystander walker = SimpleBystander.player("Notch").at(Vec3d.centreOf(BEHIND));
            world.withBystander(walker);
            SimpleChipState state = chip("MCX146", "SP:INF:0", "").inputs(true, false, false, false).build();

            Effects.potionArea().trigger(state);

            assertThat(walker.doses().get(0).durationTicks()).isEqualTo(PotionDose.FOREVER_TICKS);
        }

        @Test
        void movesItsMiddleWhereTheFourthLineSays() {
            SimpleBystander walker =
                    SimpleBystander.player("Notch").at(Vec3d.centreOf(BEHIND.add(20, 0, 0)));
            world.withBystander(walker);
            SimpleChipState state =
                    chip("MCX146", "SP:30:1", "2:20:0:0").inputs(true, false, false, false).build();

            Effects.potionArea().trigger(state);

            assertThat(walker.doses()).hasSize(1);
        }

        @Test
        void dosesNobodyWhenItsSignNamesNoEffect() {
            SimpleBystander walker = SimpleBystander.player("Notch").at(Vec3d.centreOf(BEHIND));
            world.withBystander(walker);
            SimpleChipState state = chip("MCX146", "", "").inputs(true, false, false, false).build();

            Effects.potionArea().trigger(state);

            assertThat(walker.doses()).isEmpty();
        }

        @Test
        void readsItsSettingsOutOfABookWhenAskedTo() {
            world.withBook(BEHIND.add(0, 1, 0), List.of("SP:30:1\nPO:10:0\n8@E"));
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(Vec3d.centreOf(BEHIND));
            world.withBystander(zombie);
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("EFFECT", "[MCX146]B", "", "")
                    .inputs(true, false, false, false)
                    .build();

            Effects.potionArea().trigger(state);

            assertThat(zombie.doses()).hasSize(2);
        }

        @Test
        void dosesNobodyWhileNothingDrivesIt() {
            SimpleBystander walker = SimpleBystander.player("Notch").at(Vec3d.centreOf(BEHIND));
            world.withBystander(walker);
            SimpleChipState state = chip("MCX146", "SP:30:1", "").build();

            Effects.potionArea().trigger(state);

            assertThat(walker.doses()).isEmpty();
        }
    }

    @Nested
    @DisplayName("particle emitter")
    class ParticleEmitter {

        @Test
        void showsWhatItsSignNames() {
            SimpleChipState state = chip("MCX250", "flame", "").inputs(true, false, false, false).build();

            Effects.particleEmitter().trigger(state);

            assertThat(world.particles()).hasSize(1);
            assertThat(world.particles().get(0).particle()).isEqualTo(Blocks.key("flame"));
            assertThat(world.particles().get(0).at()).isEqualTo(Vec3d.centreOf(BEHIND));
        }

        @Test
        void takesTheBlockAParticleNeedsFromAfterTheColon() {
            SimpleChipState state =
                    chip("MCX250", "block:redstone_block", "").inputs(true, false, false, false).build();

            Effects.particleEmitter().trigger(state);

            assertThat(world.particles().get(0).particle()).isEqualTo(Blocks.key("block"));
            assertThat(world.particles().get(0).block())
                    .isEqualTo(Optional.of(Blocks.key("redstone_block")));
        }

        @Test
        void movesTheParticleAlongTheAxisTheFourthLineNames() {
            SimpleChipState state = chip("MCX250", "flame", "Y3").inputs(true, false, false, false).build();

            Effects.particleEmitter().trigger(state);

            assertThat(world.particles().get(0).at()).isEqualTo(Vec3d.centreOf(BEHIND).add(0, 3, 0));
        }

        @Test
        void showsNothingWhileNothingDrivesIt() {
            SimpleChipState state = chip("MCX250", "flame", "").build();

            Effects.particleEmitter().trigger(state);

            assertThat(world.particles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("fireworks")
    class Fireworks {

        @Test
        void setsOneOffAboveItsSupport() {
            SimpleChipState state = chip("MC1250", "", "").inputs(true, false, false, false).build();

            Effects.fireworks(always(0)).trigger(state);

            assertThat(world.fireworks()).hasSize(1);
            assertThat(world.fireworks().get(0).at()).isEqualTo(Vec3d.middleOf(BEHIND));
        }

        @Test
        void setsNoneOffWhileNothingDrivesIt() {
            SimpleChipState state = chip("MC1250", "", "").build();

            Effects.fireworks(always(0)).trigger(state);

            assertThat(world.fireworks()).isEmpty();
        }
    }
}
