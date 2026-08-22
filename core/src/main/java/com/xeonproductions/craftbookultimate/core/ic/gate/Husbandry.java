// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SignArea;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that work a farm and the animals on it.
 *
 * <p>All four take from and give to the container above the block their sign hangs on, which is
 * where the farming chips have always looked for one. None of them makes anything out of nothing:
 * a hoe is worn out, a bucket is spent, food is eaten.
 *
 * <p>Line 3 is the area to work on, in the grammar {@link SignArea} defines, except on the
 * experience spawner, which works where it stands.
 *
 * <p>What may be sheared and what may be bred are the game's own answers rather than lists kept
 * here. An animal the game learns to breed, or a creature it learns to shear, works with nothing
 * in this file changed — the same reason the terraformer asks the game what bonemeal does.
 */
@NullMarked
public final class Husbandry {

    /** The line carrying the area to work on. */
    private static final int AREA_LINE = 2;

    /** The line carrying whatever else a chip needs told. */
    private static final int EXTRA_LINE = 3;

    /** How far these chips reach when their sign does not say. */
    private static final int DEFAULT_AREA_RADIUS = 10;

    /** How many places a cultivator tries in one pass over its area. */
    private static final int TRIES_PER_PASS = 10;

    /** How much experience one orb carries when the sign does not say. */
    private static final int DEFAULT_EXPERIENCE = 1;

    /** The most experience one orb may be asked to carry. */
    private static final int MAX_EXPERIENCE = 1000;

    /** How many orbs a spawner makes when the sign does not say. */
    private static final int DEFAULT_ORBS = 1;

    /** The most orbs one pulse may make, so a sign cannot ask for a thousand of them. */
    private static final int MAX_ORBS = 64;

    /** How far above the sign's support an orb appears. */
    private static final double ORB_HEIGHT = 1.5;

    /** An empty bucket, which is what milking a cow costs. */
    private static final Key BUCKET = Key.key("bucket");

    /** A bucket of milk, which is what it gives back. */
    private static final Key MILK_BUCKET = Key.key("milk_bucket");

    /** Shears, which is what taking a coat costs. */
    private static final Key SHEARS = Key.key("shears");

    /** How much food breeding a pair costs, which is one each. */
    private static final int FOOD_PER_PAIR = 2;

    /** Every hoe the game has, which is what a cultivator will wear out. */
    private static final Set<Key> HOES = Set.of(
            Blocks.key("wooden_hoe"),
            Blocks.key("stone_hoe"),
            Blocks.key("iron_hoe"),
            Blocks.key("golden_hoe"),
            Blocks.key("diamond_hoe"),
            Blocks.key("netherite_hoe"));

    /** What a cultivator will till. */
    private static final Set<Key> TILLABLE = Set.of(
            Blocks.key("dirt"), Blocks.key("grass_block"), Blocks.key("dirt_path"),
            Blocks.key("coarse_dirt"), Blocks.key("rooted_dirt"));

    /** A cow, which is the one creature this file names for itself. */
    private static final Key COW = Key.key("minecraft", "cow");

    /** What tilling gives. */
    private static final Key FARMLAND = Blocks.key("farmland");

    private Husbandry() {}

    /**
     * Tills earth in an area into farmland, wearing out a hoe from the container above.
     *
     * <p>Line 3 is the area. A patch is only turned where there is nothing standing on it, so a
     * cultivator does not till the ground out from under a crop.
     *
     * <p>Every hoe is accepted, netherite included. The fork's list stopped at diamond, so the best
     * hoe in the game sat in the chest doing nothing — see finding 144.
     */
    public static SelfTriggeringICLogic cultivator(RandomGenerator random) {
        return new Cultivator(random);
    }

    /**
     * Milks the cows and shears the sheep in an area into the container above.
     *
     * <p>Line 3 is the area. Milking costs an empty bucket and gives a full one; shearing costs a
     * point off the shears and gives the wool, in the colour the sheep was wearing.
     *
     * <p>Only grown animals, and only ones with something to give: a lamb, a sheep already shorn
     * and a cow with nowhere to put the milk are all left alone.
     */
    public static SelfTriggeringICLogic animalHarvester() {
        return new AnimalHarvester();
    }

    /**
     * Feeds pairs of animals in an area from the container above so that they breed.
     *
     * <p>Line 3 is the area. Two of a kind that are grown and off their cooldown are found, a food
     * both of them will take is looked for in the container, and two of it are spent putting the
     * pair in love.
     *
     * <p>Nothing here makes a baby: the game does the breeding, so the cooldowns, the experience
     * and every rule about what may breed with what stay where they belong. That also means every
     * animal the game will breed works here, rather than the five the fork listed.
     */
    public static SelfTriggeringICLogic animalBreeder() {
        return new AnimalBreeder();
    }

    /**
     * Drops experience above itself.
     *
     * <p>Line 3 is how much each orb is worth and line 4 is how many orbs to drop. Both are held
     * to what a sign may ask for, since a chip on a clock asking for a thousand orbs is a way to
     * stop a server rather than a way to reward somebody.
     */
    public static SelfTriggeringICLogic experienceSpawner() {
        return new ExperienceSpawner();
    }

    /** Turns earth into farmland, at the cost of a hoe. */
    private record Cultivator(RandomGenerator random) implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(cultivate(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(cultivate(state));
            }
        }

        private boolean cultivate(ChipState state) {
            ChipWorld world = state.world();
            Vec3i back = state.backPosition();
            Bounds area = SignArea.on(state, AREA_LINE, DEFAULT_AREA_RADIUS).around(back);
            Stockpile shed = state.stockpileNear(back.add(0, 1, 0), 0, Set.of());

            for (int attempt = 0; attempt < TRIES_PER_PASS; attempt++) {
                Optional<Vec3i> spot = somewhereIn(area, random, world);
                if (spot.isEmpty()) {
                    continue;
                }
                Vec3i earth = spot.get();
                if (!TILLABLE.contains(world.blockAt(earth)) || !world.isAir(earth.add(0, 1, 0))) {
                    continue;
                }
                if (!shed.wearOne(HOES)) {
                    return false;
                }
                return world.setBlockAt(earth, FARMLAND);
            }
            return false;
        }
    }

    /** Milks and shears whatever is standing in the area. */
    private static final class AnimalHarvester implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(harvest(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(harvest(state));
            }
        }

        private static boolean harvest(ChipState state) {
            Vec3i back = state.backPosition();
            Stockpile shed = state.stockpileNear(back.add(0, 1, 0), 0, Set.of());

            for (Bystander animal : animalsIn(state)) {
                if (!animal.isAdult()) {
                    continue;
                }
                if (milk(animal, shed) || shear(animal, shed)) {
                    return true;
                }
            }
            return false;
        }

        /** Trades an empty bucket for a full one. The cow is not changed by being milked. */
        private static boolean milk(Bystander animal, Stockpile shed) {
            if (!animal.type().equals(COW) || !shed.hasRoomFor(MILK_BUCKET, 1)) {
                return false;
            }
            if (!shed.takeAll(BUCKET, 1)) {
                return false;
            }
            if (shed.give(MILK_BUCKET, 1) > 0) {
                shed.give(BUCKET, 1);
                return false;
            }
            return true;
        }

        /**
         * Takes a coat, at the cost of a point off the shears.
         *
         * <p>The shears are worn, which the fork never did — one pair sheared a flock for ever.
         * See finding 145.
         */
        private static boolean shear(Bystander animal, Stockpile shed) {
            if (!animal.isShearable() || !shed.has(SHEARS, 1)) {
                return false;
            }
            Optional<Key> coat = animal.shear();
            if (coat.isEmpty()) {
                return false;
            }
            shed.wearOne(Set.of(SHEARS));
            shed.give(coat.get(), 1);
            return true;
        }
    }

    /** Puts pairs of animals in love, and lets the game do the rest. */
    private static final class AnimalBreeder implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(breed(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(breed(state));
            }
        }

        private static boolean breed(ChipState state) {
            Vec3i back = state.backPosition();
            Stockpile trough = state.stockpileNear(back.add(0, 1, 0), 0, Set.of());
            // Every food it might spend has to be named, and a stockpile's contents are the only
            // place to learn what it holds; the container above a chip is always a real one.
            Map<Key, Integer> held = trough.contents();
            if (held.isEmpty()) {
                return false;
            }

            List<Bystander> ready = new ArrayList<>();
            for (Bystander animal : animalsIn(state)) {
                if (animal.isReadyToBreed()) {
                    ready.add(animal);
                }
            }

            for (int first = 0; first < ready.size(); first++) {
                for (int second = first + 1; second < ready.size(); second++) {
                    if (pairUp(ready.get(first), ready.get(second), trough, held)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** Feeds two of a kind, if the container holds something both of them want. */
        private static boolean pairUp(
                Bystander one, Bystander other, Stockpile trough, Map<Key, Integer> held) {
            if (!one.type().equals(other.type())) {
                return false;
            }

            for (Key food : held.keySet()) {
                if (!one.isBredBy(food) || !trough.takeAll(food, FOOD_PER_PAIR)) {
                    continue;
                }
                if (one.encourageBreeding() && other.encourageBreeding()) {
                    return true;
                }
                trough.give(food, FOOD_PER_PAIR);
                return false;
            }
            return false;
        }
    }

    /** Drops orbs of experience above itself. */
    private static final class ExperienceSpawner implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(spawn(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(spawn(state));
            }
        }

        private static boolean spawn(ChipState state) {
            int worth = boundedNumber(
                    state.sign().trimmedText(AREA_LINE), 1, MAX_EXPERIENCE, DEFAULT_EXPERIENCE);
            int orbs = boundedNumber(
                    state.sign().trimmedText(EXTRA_LINE), 1, MAX_ORBS, DEFAULT_ORBS);

            Vec3d at = Vec3d.centreOf(state.backPosition()).add(0, ORB_HEIGHT, 0);

            boolean dropped = false;
            for (int orb = 0; orb < orbs; orb++) {
                dropped |= state.world().spawnExperience(at, worth);
            }
            return dropped;
        }
    }

    /** Every living thing standing in the area a chip's sign describes. */
    private static List<Bystander> animalsIn(ChipState state) {
        Bounds area = SignArea.on(state, AREA_LINE, DEFAULT_AREA_RADIUS)
                .around(state.backPosition());

        List<Bystander> found = new ArrayList<>();
        for (Bystander bystander : state.world().bystandersIn(
                Vec3d.of(area.from()), Vec3d.of(area.to()).add(1, 1, 1))) {
            if (bystander.isPresent() && bystander.isAnimal()) {
                found.add(bystander);
            }
        }
        return found;
    }

    /** Somewhere inside an area that this thread may actually read. */
    private static Optional<Vec3i> somewhereIn(
            Bounds area, RandomGenerator random, ChipWorld world) {
        Vec3i position = new Vec3i(
                area.from().x() + random.nextInt(area.width()),
                area.from().y() + random.nextInt(area.height()),
                area.from().z() + random.nextInt(area.length()));

        return world.isLoaded(position) && world.isInBounds(position)
                ? Optional.of(position)
                : Optional.empty();
    }

    /** A number from a sign, held within bounds, falling back when the text is not one. */
    private static int boundedNumber(String written, int lowest, int highest, int fallback) {
        String trimmed = written.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        try {
            return Math.clamp(Integer.parseInt(trimmed), lowest, highest);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
