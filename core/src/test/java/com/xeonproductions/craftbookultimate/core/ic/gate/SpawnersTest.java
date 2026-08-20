package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Spawning chips")
class SpawnersTest {

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    private static SimpleChipState.Builder chip(SimpleChipWorld world, String model, String... lines) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("SPAWNER", "[" + model + "]", lines[0], lines[1]);
    }

    @Nested
    @DisplayName("entity spawner")
    class EntitySpawner {

        @Test
        void spawnsWhatItsSignNames() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX200", "zombie", "").inputs(true, false, false, false).build();

            Spawners.entitySpawner().trigger(state);

            assertThat(world.spawns()).hasSize(1);
            assertThat(world.spawns().get(0).count()).isEqualTo(1);
            assertThat(world.spawns().get(0).what())
                    .isEqualTo(EntitySpec.OfType.of(Blocks.key("zombie")));
        }

        @Test
        void spawnsAsManyAsTheAsteriskAsksFor() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX200", "zombie*7", "").inputs(true, false, false, false).build();

            Spawners.entitySpawner().trigger(state);

            assertThat(world.spawns().get(0).count()).isEqualTo(7);
        }

        @Test
        void readsADescriptionRunningOntoTheFourthLine() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX200", "pig+zom", "bie").inputs(true, false, false, false).build();

            Spawners.entitySpawner().trigger(state);

            assertThat(world.spawns().get(0).what()).isInstanceOf(EntitySpec.Mounted.class);
        }

        @Test
        void refusesToSpawnMoreThanAHundredAtOnce() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX200", "zombie*5000", "").inputs(true, false, false, false).build();

            Spawners.entitySpawner().trigger(state);

            assertThat(world.spawns().get(0).count()).isEqualTo(100);
        }

        @Test
        void spawnsAboveWhateverIsStackedOnItsSupport() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(BEHIND, "stone")
                    .withBlock(BEHIND.add(0, 1, 0), "stone");
            SimpleChipState state = chip(world, "MCX200", "zombie", "").inputs(true, false, false, false).build();

            Spawners.entitySpawner().trigger(state);

            assertThat(world.spawns().get(0).at()).isEqualTo(Vec3d.centreOf(BEHIND.add(0, 2, 0)));
        }

        @Test
        void spawnsNothingWhileNothingDrivesIt() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX200", "zombie", "").build();

            Spawners.entitySpawner().trigger(state);

            assertThat(world.spawns()).isEmpty();
        }

        @Test
        void spawnsNothingForADescriptionNothingCouldBeMadeFrom() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX200", "mobs", "").inputs(true, false, false, false).build();

            Spawners.entitySpawner().trigger(state);

            assertThat(world.spawns()).isEmpty();
        }
    }

    @Nested
    @DisplayName("item spawner")
    class ItemSpawner {

        @Test
        void dropsWhatItsSignNames() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX201", "stone", "").inputs(true, false, false, false).build();

            Spawners.itemSpawner().trigger(state);

            assertThat(world.droppedStacks()).hasSize(1);
            assertThat(world.droppedStacks().get(0).item()).isEqualTo(Blocks.key("stone"));
            assertThat(world.droppedStacks().get(0).count()).isEqualTo(1);
        }

        @Test
        void dropsAsManyAsTheFourthLineAsksFor() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX201", "stone", "16").inputs(true, false, false, false).build();

            Spawners.itemSpawner().trigger(state);

            assertThat(world.droppedStacks().get(0).count()).isEqualTo(16);
        }

        @Test
        void refusesToDropMoreThanAStack() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX201", "stone", "999").inputs(true, false, false, false).build();

            Spawners.itemSpawner().trigger(state);

            assertThat(world.droppedStacks().get(0).count()).isEqualTo(64);
        }

        @Test
        void dropsOneWhenTheAmountIsNotANumber() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX201", "stone", "lots").inputs(true, false, false, false).build();

            Spawners.itemSpawner().trigger(state);

            assertThat(world.droppedStacks().get(0).count()).isEqualTo(1);
        }

        @Test
        void dropsNothingForAnItemThatDoesNotExist() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MCX201", "", "").inputs(true, false, false, false).build();

            Spawners.itemSpawner().trigger(state);

            assertThat(world.droppedStacks()).isEmpty();
        }
    }
}
