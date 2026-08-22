// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.random.RandomGenerator;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Farming and animal chips")
class HusbandryTest {

    private static final Vec3i SIGN = new Vec3i(0, 64, 0);
    private static final Vec3i BACK = new Vec3i(0, 64, -1);
    private static final Vec3i ABOVE = new Vec3i(0, 65, -1);

    private static final Key IRON_HOE = Blocks.key("iron_hoe");
    private static final Key NETHERITE_HOE = Blocks.key("netherite_hoe");
    private static final Key FARMLAND = Blocks.key("farmland");
    private static final Key DIRT = Blocks.key("dirt");
    private static final Key BUCKET = Key.key("bucket");
    private static final Key MILK = Key.key("milk_bucket");
    private static final Key SHEARS = Key.key("shears");
    private static final Key WHITE_WOOL = Key.key("minecraft", "white_wool");
    private static final Key WHEAT = Key.key("minecraft", "wheat");

    /** Always draws zero, so a chip picking a spot picks a known one. */
    private static final RandomGenerator ALWAYS_FIRST = new RandomGenerator() {
        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    };

    private static SimpleChipState.Builder chip(
            SimpleChipWorld world, String model, String third, String fourth) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("FARM", "[" + model + "]", third, fourth);
    }

    @Nested
    @DisplayName("the cultivator")
    class Cultivator {

        @Test
        void tillsEarthIntoFarmland() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACK, DIRT);
            SimpleStockpile shed = SimpleStockpile.empty().with(IRON_HOE, 1);

            SimpleChipState state = chip(world, "MC1235", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.cultivator(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(FARMLAND);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void acceptsANetheriteHoe() {
            // The fork's list stopped at diamond, so the best hoe in the game did nothing.
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACK, DIRT);
            SimpleStockpile shed = SimpleStockpile.empty().with(NETHERITE_HOE, 1);

            SimpleChipState state = chip(world, "MC1235", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.cultivator(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(FARMLAND);
        }

        @Test
        void doesNothingWithNoHoeToWear() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACK, DIRT);

            SimpleChipState state = chip(world, "MC1235", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.withCapacity(0));

            Husbandry.cultivator(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(DIRT);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void leavesEarthWithSomethingStandingOnIt() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBlock(BACK, DIRT)
                    .withBlock(BACK.add(0, 1, 0), Blocks.key("wheat"));

            SimpleChipState state = chip(world, "MC1235", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty().with(IRON_HOE, 1));

            Husbandry.cultivator(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(DIRT);
        }

        @Test
        void leavesABlockItCannotTill() {
            SimpleChipWorld world = new SimpleChipWorld().withBlock(BACK, Blocks.key("stone"));

            SimpleChipState state = chip(world, "MC1235", "0", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty().with(IRON_HOE, 1));

            Husbandry.cultivator(ALWAYS_FIRST).trigger(state);

            assertThat(world.blockAt(BACK)).isEqualTo(Blocks.key("stone"));
        }
    }

    @Nested
    @DisplayName("the animal harvester")
    class AnimalHarvester {

        private SimpleBystander cowAt(Vec3i position) {
            return SimpleBystander.animal("cow").at(Vec3d.centreOf(position));
        }

        private SimpleBystander sheepAt(Vec3i position) {
            return SimpleBystander.animal("sheep").at(Vec3d.centreOf(position)).woolly("white");
        }

        @Test
        void milksACowIntoTheContainerAbove() {
            SimpleChipWorld world = new SimpleChipWorld().withBystander(cowAt(BACK));
            SimpleStockpile shed = SimpleStockpile.empty().with(BUCKET, 1);

            SimpleChipState state = chip(world, "MC1244", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.animalHarvester().trigger(state);

            assertThat(shed.count(MILK)).isEqualTo(1);
            assertThat(shed.count(BUCKET)).isZero();
        }

        @Test
        void leavesACowAloneWithNoBucketToFill() {
            SimpleChipWorld world = new SimpleChipWorld().withBystander(cowAt(BACK));
            SimpleStockpile shed = SimpleStockpile.empty();

            SimpleChipState state = chip(world, "MC1244", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.animalHarvester().trigger(state);

            assertThat(shed.count(MILK)).isZero();
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void shearsASheepIntoTheContainerAbove() {
            SimpleBystander sheep = sheepAt(BACK);
            SimpleChipWorld world = new SimpleChipWorld().withBystander(sheep);
            SimpleStockpile shed = SimpleStockpile.empty().with(SHEARS, 1);

            SimpleChipState state = chip(world, "MC1244", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.animalHarvester().trigger(state);

            assertThat(shed.count(WHITE_WOOL)).isEqualTo(1);
            assertThat(sheep.isShearable()).isFalse();
        }

        @Test
        void leavesASheepThatHasAlreadyBeenShorn() {
            SimpleBystander sheep = sheepAt(BACK);
            sheep.shear();

            SimpleChipWorld world = new SimpleChipWorld().withBystander(sheep);
            SimpleStockpile shed = SimpleStockpile.empty().with(SHEARS, 1);

            SimpleChipState state = chip(world, "MC1244", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.animalHarvester().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void leavesAYoungAnimalAlone() {
            SimpleChipWorld world = new SimpleChipWorld().withBystander(cowAt(BACK).young());
            SimpleStockpile shed = SimpleStockpile.empty().with(BUCKET, 1);

            SimpleChipState state = chip(world, "MC1244", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.animalHarvester().trigger(state);

            assertThat(shed.count(MILK)).isZero();
        }

        @Test
        void ignoresAnAnimalOutsideItsArea() {
            SimpleChipWorld world =
                    new SimpleChipWorld().withBystander(cowAt(BACK.add(20, 0, 0)));
            SimpleStockpile shed = SimpleStockpile.empty().with(BUCKET, 1);

            SimpleChipState state = chip(world, "MC1244", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, shed);

            Husbandry.animalHarvester().trigger(state);

            assertThat(shed.count(MILK)).isZero();
        }
    }

    @Nested
    @DisplayName("the animal breeder")
    class AnimalBreeder {

        private SimpleBystander readyCow() {
            return SimpleBystander.animal("cow")
                    .at(Vec3d.centreOf(BACK))
                    .readyToBreed("wheat");
        }

        @Test
        void putsAPairInLoveAndSpendsTheirFood() {
            SimpleBystander one = readyCow();
            SimpleBystander other = readyCow();
            SimpleChipWorld world = new SimpleChipWorld().withBystander(one).withBystander(other);
            SimpleStockpile trough = SimpleStockpile.empty().with(WHEAT, 8);

            SimpleChipState state = chip(world, "MC1280", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, trough);

            Husbandry.animalBreeder().trigger(state);

            assertThat(one.wasEncouraged()).isTrue();
            assertThat(other.wasEncouraged()).isTrue();
            assertThat(trough.count(WHEAT)).isEqualTo(6);
        }

        @Test
        void makesNoBabyItself() {
            // The game does the breeding, so nothing is spawned here.
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBystander(readyCow())
                    .withBystander(readyCow());

            SimpleChipState state = chip(world, "MC1280", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty().with(WHEAT, 8));

            Husbandry.animalBreeder().trigger(state);

            assertThat(world.spawns()).isEmpty();
        }

        @Test
        void needsTwoOfAKind() {
            SimpleBystander cow = readyCow();
            SimpleBystander pig = SimpleBystander.animal("pig")
                    .at(Vec3d.centreOf(BACK))
                    .readyToBreed("wheat");

            SimpleChipWorld world = new SimpleChipWorld().withBystander(cow).withBystander(pig);

            SimpleChipState state = chip(world, "MC1280", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty().with(WHEAT, 8));

            Husbandry.animalBreeder().trigger(state);

            assertThat(cow.wasEncouraged()).isFalse();
            assertThat(pig.wasEncouraged()).isFalse();
        }

        @Test
        void spendsNothingOnFoodTheAnimalWillNotEat() {
            SimpleBystander one = readyCow();
            SimpleChipWorld world =
                    new SimpleChipWorld().withBystander(one).withBystander(readyCow());
            SimpleStockpile trough = SimpleStockpile.empty().with(Key.key("minecraft", "stone"), 8);

            SimpleChipState state = chip(world, "MC1280", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, trough);

            Husbandry.animalBreeder().trigger(state);

            assertThat(one.wasEncouraged()).isFalse();
            assertThat(trough.count(Key.key("minecraft", "stone"))).isEqualTo(8);
        }

        @Test
        void leavesAPairThatIsNotReady() {
            SimpleBystander one = SimpleBystander.animal("cow").at(Vec3d.centreOf(BACK));
            SimpleBystander other = SimpleBystander.animal("cow").at(Vec3d.centreOf(BACK));
            SimpleChipWorld world = new SimpleChipWorld().withBystander(one).withBystander(other);
            SimpleStockpile trough = SimpleStockpile.empty().with(WHEAT, 8);

            SimpleChipState state = chip(world, "MC1280", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, trough);

            Husbandry.animalBreeder().trigger(state);

            assertThat(trough.count(WHEAT)).isEqualTo(8);
        }

        @Test
        void doesNothingWithAnEmptyTrough() {
            SimpleChipWorld world = new SimpleChipWorld()
                    .withBystander(readyCow())
                    .withBystander(readyCow());

            SimpleChipState state = chip(world, "MC1280", "2", "")
                    .inputs(true, false, false, false)
                    .build()
                    .withStockpileAt(ABOVE, SimpleStockpile.empty());

            Husbandry.animalBreeder().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("the experience spawner")
    class ExperienceSpawner {

        @Test
        void dropsOneOrbByDefault() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1246", "", "")
                    .inputs(true, false, false, false)
                    .build();

            Husbandry.experienceSpawner().trigger(state);

            assertThat(world.experience()).hasSize(1);
            assertThat(world.experience().getFirst().amount()).isEqualTo(1);
        }

        @Test
        void takesWhatEachOrbIsWorthOffLineThree() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1246", "7", "")
                    .inputs(true, false, false, false)
                    .build();

            Husbandry.experienceSpawner().trigger(state);

            assertThat(world.experience().getFirst().amount()).isEqualTo(7);
        }

        @Test
        void takesHowManyOrbsOffLineFour() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1246", "3", "5")
                    .inputs(true, false, false, false)
                    .build();

            Husbandry.experienceSpawner().trigger(state);

            assertThat(world.experience()).hasSize(5);
        }

        @Test
        void refusesToMakeMoreOrbsThanASignMayAskFor() {
            // A chip on a clock asking for a thousand orbs is a way to stop a server.
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1246", "1", "100000")
                    .inputs(true, false, false, false)
                    .build();

            Husbandry.experienceSpawner().trigger(state);

            assertThat(world.experience()).hasSize(64);
        }

        @Test
        void dropsThemAboveItsOwnSupport() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1246", "", "")
                    .inputs(true, false, false, false)
                    .build();

            Husbandry.experienceSpawner().trigger(state);

            assertThat(world.experience().getFirst().at().y()).isGreaterThan(BACK.y());
        }

        @Test
        void dropsNothingUntilItIsDriven() {
            SimpleChipWorld world = new SimpleChipWorld();
            SimpleChipState state = chip(world, "MC1246", "", "")
                    .inputs(false, false, false, false)
                    .build();

            Husbandry.experienceSpawner().trigger(state);

            assertThat(world.experience()).isEmpty();
        }
    }
}
