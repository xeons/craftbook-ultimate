package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Shooting chips")
class ProjectilesTest {

    /** A south-facing sign, so shots leave northwards, out of the back. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    private SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState chip(String speedLine, String verticalLine) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("SHOOTER", "[MC1240]", speedLine, verticalLine)
                .inputs(true, false, false, false)
                .build();
    }

    @Nested
    @DisplayName("a shooter")
    class AShooter {

        @Test
        void firesOneProjectileOfItsOwnKind() {
            Projectiles.snowShooter().trigger(chip("", ""));

            assertThat(world.shots()).hasSize(1);
            assertThat(world.shots().get(0).projectile()).isEqualTo(Blocks.key("snowball"));
        }

        @Test
        void firesAwayFromTheBlockTheSignHangsOn() {
            Projectiles.arrowShooter().trigger(chip("", ""));

            assertThat(world.shots().get(0).direction().z()).isEqualTo(-1);
            assertThat(world.shots().get(0).direction().x()).isEqualTo(0);
        }

        @Test
        void startsJustOutsideThatBlockRatherThanInsideIt() {
            Projectiles.arrowShooter().trigger(chip("", ""));

            assertThat(world.shots().get(0).from().z())
                    .isEqualTo(BEHIND.z() + 0.5 - 0.55, within(1e-9));
        }

        @Test
        void usesTheDefaultSpeedAndSpreadWhenItsSignIsBlank() {
            Projectiles.arrowShooter().trigger(chip("", ""));

            assertThat(world.shots().get(0).speed()).isEqualTo(0.5);
            assertThat(world.shots().get(0).spread()).isEqualTo(12.0);
        }

        @Test
        void takesTheSpeedAndSpreadFromItsSign() {
            Projectiles.arrowShooter().trigger(chip("1.5:0", ""));

            assertThat(world.shots().get(0).speed()).isEqualTo(1.5);
            assertThat(world.shots().get(0).spread()).isEqualTo(0.0);
        }

        @Test
        void aimsUpAndDownFromTheFourthLine() {
            Projectiles.arrowShooter().trigger(chip("", "0.4"));

            assertThat(world.shots().get(0).direction().y()).isEqualTo(0.4);
        }

        @Test
        void holdsASignEditedToAskForTooMuchWithinItsLimits() {
            // The old chip only checked its numbers when the sign was made, so an edit afterwards
            // went straight through.
            Projectiles.arrowShooter().trigger(chip("500:900", "50"));

            assertThat(world.shots().get(0).speed()).isEqualTo(2.0);
            assertThat(world.shots().get(0).spread()).isEqualTo(50.0);
            assertThat(world.shots().get(0).direction().y()).isEqualTo(1.0);
        }

        @Test
        void firesNothingWhileNothingDrivesIt() {
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("SHOOTER", "[MC1240]", "", "")
                    .build();

            Projectiles.arrowShooter().trigger(state);

            assertThat(world.shots()).isEmpty();
        }
    }

    @Nested
    @DisplayName("a barrage")
    class ABarrage {

        @Test
        void firesFiveAtOnce() {
            Projectiles.eggBarrage().trigger(chip("", ""));

            assertThat(world.shots()).hasSize(5);
            assertThat(world.shots()).allMatch(shot -> shot.projectile().equals(Blocks.key("egg")));
        }
    }

    @Nested
    @DisplayName("the fireball shooter")
    class TheFireballShooter {

        @Test
        void launchesStraightOutTheBackWhenItsSignIsBlank() {
            Projectiles.fireballShooter().trigger(chip("", ""));

            assertThat(world.shots()).hasSize(1);
            assertThat(world.shots().get(0).projectile()).isEqualTo(Blocks.key("fireball"));
            assertThat(world.shots().get(0).direction().z()).isEqualTo(-1, within(1e-9));
            assertThat(world.shots().get(0).direction().x()).isEqualTo(0, within(1e-9));
        }

        @Test
        void turnsAsFarAsTheFourthLineSays() {
            Projectiles.fireballShooter().trigger(chip("", "90"));

            assertThat(world.shots().get(0).direction().x()).isEqualTo(-1, within(1e-9));
            assertThat(world.shots().get(0).direction().z()).isEqualTo(0, within(1e-9));
        }

        @Test
        void tiltsAsFarAsTheFourthLineSays() {
            Projectiles.fireballShooter().trigger(chip("", "0:0.5"));

            assertThat(world.shots().get(0).direction().y()).isEqualTo(0.5, within(1e-9));
        }

        @Test
        void neverScatters() {
            // A fireball is aimed, not thrown, so an inaccuracy figure would only make it useless.
            Projectiles.fireballShooter().trigger(chip("", ""));

            assertThat(world.shots().get(0).spread()).isEqualTo(0.0);
        }
    }
}
