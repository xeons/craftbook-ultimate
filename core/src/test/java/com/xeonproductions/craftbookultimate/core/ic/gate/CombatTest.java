package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Chips that hurt what is near them")
class CombatTest {

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    /** The first place above the chip's support that somebody could stand. */
    private static final Vec3d ABOVE = Vec3d.centreOf(BEHIND.add(0, 1, 0));

    private SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState.Builder chip(PinLayout layout, String model, String third, String fourth) {
        return SimpleChipState.forLayout(layout)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("TRAP", "[" + model + "]", third, fourth);
    }

    @Nested
    @DisplayName("mob zapper")
    class MobZapper {

        private SimpleChipState.Builder zapper(String third, String fourth) {
            return chip(PinLayout.SISO, "MCX130", third, fourth);
        }

        @Test
        void removesHostileMobsByDefault() {
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(new Vec3d(1, 64, 0));
            SimpleBystander cow = SimpleBystander.animal("cow").at(new Vec3d(1, 64, 0));
            world.withBystander(zombie).withBystander(cow);
            SimpleChipState state = zapper("", "").inputs(true).build();

            Combat.mobZapper().trigger(state);

            assertThat(zombie.isPresent()).isFalse();
            assertThat(cow.isPresent()).isTrue();
        }

        @Test
        void removesOnlyWhatItsSignNames() {
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(new Vec3d(1, 64, 0));
            SimpleBystander creeper = SimpleBystander.monster("creeper").at(new Vec3d(1, 64, 0));
            world.withBystander(zombie).withBystander(creeper);
            SimpleChipState state = zapper("creeper", "").inputs(true).build();

            Combat.mobZapper().trigger(state);

            assertThat(zombie.isPresent()).isTrue();
            assertThat(creeper.isPresent()).isFalse();
        }

        @Test
        void reportsWhetherItRemovedAnything() {
            world.withBystander(SimpleBystander.monster("zombie").at(new Vec3d(1, 64, 0)));
            SimpleChipState state = zapper("", "").inputs(true).build();

            Combat.mobZapper().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void saysSoWhenThereWasNothingToRemove() {
            SimpleChipState state = zapper("", "").inputs(true).build();

            Combat.mobZapper().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void leavesAnythingBeyondItsRange() {
            SimpleBystander far = SimpleBystander.monster("zombie").at(new Vec3d(30, 64, 0));
            world.withBystander(far);
            SimpleChipState state = zapper("", "5").inputs(true).build();

            Combat.mobZapper().trigger(state);

            assertThat(far.isPresent()).isTrue();
        }

        @Test
        void refusesARangeBeyondWhatItIsAllowed() {
            SimpleBystander far = SimpleBystander.monster("zombie").at(new Vec3d(80, 64, 0));
            world.withBystander(far);
            SimpleChipState state = zapper("", "5000").inputs(true).build();

            Combat.mobZapper().trigger(state);

            assertThat(far.isPresent()).isTrue();
        }
    }

    @Nested
    @DisplayName("hit player above")
    class HitPlayerAbove {

        private SimpleChipState.Builder trap(String third, String fourth) {
            return chip(PinLayout.UISO, "MCX131", third, fourth);
        }

        @Test
        void hurtsWhoeverIsStandingOverIt() {
            SimpleBystander walker = SimpleBystander.player("Notch").at(ABOVE);
            world.withBystander(walker);
            SimpleChipState state = trap("", "").inputs(true, false, false, false).build();

            Combat.hitPlayerAbove().trigger(state);

            assertThat(walker.damageTaken()).isEqualTo(1);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void hitsAsHardAsTheFourthLineSays() {
            SimpleBystander walker = SimpleBystander.player("Notch").at(ABOVE);
            world.withBystander(walker);
            SimpleChipState state = trap("", "6").inputs(true, false, false, false).build();

            Combat.hitPlayerAbove().trigger(state);

            assertThat(walker.damageTaken()).isEqualTo(6);
        }

        @Test
        void picksOneNamedPlayerOut() {
            SimpleBystander target = SimpleBystander.player("Notch").at(ABOVE);
            SimpleBystander bystander = SimpleBystander.player("Herobrine").at(ABOVE);
            world.withBystander(target).withBystander(bystander);
            SimpleChipState state = trap("p:Notch", "").inputs(true, false, false, false).build();

            Combat.hitPlayerAbove().trigger(state);

            assertThat(target.damageTaken()).isEqualTo(1);
            assertThat(bystander.damageTaken()).isZero();
        }

        @Test
        void leavesCreaturesThatAreNotPlayersAlone() {
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(ABOVE);
            world.withBystander(zombie);
            SimpleChipState state = trap("", "").inputs(true, false, false, false).build();

            Combat.hitPlayerAbove().trigger(state);

            assertThat(zombie.damageTaken()).isZero();
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void looksAboveWhateverIsStackedOnItsSupport() {
            world.withBlock(BEHIND.add(0, 1, 0), "stone");
            SimpleBystander walker = SimpleBystander.player("Notch").at(Vec3d.centreOf(BEHIND.add(0, 2, 0)));
            world.withBystander(walker);
            SimpleChipState state = trap("", "").inputs(true, false, false, false).build();

            Combat.hitPlayerAbove().trigger(state);

            assertThat(walker.damageTaken()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("hit mob above")
    class HitMobAbove {

        private SimpleChipState.Builder trap(String third, String fourth) {
            return chip(PinLayout.UISO, "MCX132", third, fourth);
        }

        @Test
        void hurtsHostileMobsStandingOverIt() {
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(ABOVE);
            world.withBystander(zombie);
            SimpleChipState state = trap("", "").inputs(true, false, false, false).build();

            Combat.hitMobAbove().trigger(state);

            assertThat(zombie.damageTaken()).isEqualTo(1);
        }

        @Test
        void leavesPlayersAlone() {
            SimpleBystander walker = SimpleBystander.player("Notch").at(ABOVE);
            world.withBystander(walker);
            SimpleChipState state = trap("", "").inputs(true, false, false, false).build();

            Combat.hitMobAbove().trigger(state);

            assertThat(walker.damageTaken()).isZero();
        }

        @Test
        void hurtsWhateverKindItsSignNames() {
            SimpleBystander cow = SimpleBystander.animal("cow").at(ABOVE);
            world.withBystander(cow);
            SimpleChipState state = trap("cow", "3").inputs(true, false, false, false).build();

            Combat.hitMobAbove().trigger(state);

            assertThat(cow.damageTaken()).isEqualTo(3);
        }

        @Test
        void hurtsOnEveryTickWhenItTicks() {
            SimpleBystander zombie = SimpleBystander.monster("zombie").at(ABOVE);
            world.withBystander(zombie);
            SimpleChipState state = trap("", "").build();

            Combat.hitMobAbove().tick(state);
            Combat.hitMobAbove().tick(state);

            assertThat(zombie.damageTaken()).isEqualTo(2);
        }
    }
}
