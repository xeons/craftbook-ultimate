// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SignArea;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that dig, flood, drain and water.
 *
 * <p>All of them work on the world rather than on a signal, and all of them pay into or out of a
 * container beside the chip. Where one takes blocks out of the world it puts what they dropped
 * away, and where one puts something into the world it takes the materials for it first, so none
 * of them makes or destroys anything a builder did not supply.
 *
 * <p>Each stops rather than half-finishes when the container is full or empty. A block is only
 * broken once there is somewhere for its drops to go, so nothing is destroyed for want of room.
 */
@NullMarked
public final class Terrain {

    /** The line naming a block to work on, or the reach of an area. */
    private static final int FIRST_LINE = 2;

    /** The line carrying whatever else a chip needs told. */
    private static final int SECOND_LINE = 3;

    /** What line 4 of a block breaker reads to make it keep the block whole. */
    private static final String INTACT = "true";

    /** Lava, spelt as a flooder's sign spells it. */
    private static final String LAVA = "lava";

    /** An empty bucket, which is what a pump fills and a spigot leaves behind. */
    private static final Key BUCKET = Key.key("bucket");

    /** A bucket of water. */
    private static final Key WATER_BUCKET = Key.key("water_bucket");

    /** A bucket of lava. */
    private static final Key LAVA_BUCKET = Key.key("lava_bucket");

    /** How far a flooder reaches when its sign does not say. */
    private static final int DEFAULT_FLOOD_RADIUS = 10;

    /** How far a spigot, terraformer or irrigator reaches when its sign does not say. */
    private static final int DEFAULT_AREA_RADIUS = 10;

    /** How far below itself a pump looks for something to lift. */
    private static final int PUMP_DEPTH = 10;

    /** How many blocks of liquid a pump will follow before giving up on finding a source. */
    private static final int PUMP_LIMIT = 128;

    /** How many blocks a spigot will walk over before giving up on finding somewhere to pour. */
    private static final int SPIGOT_LIMIT = 256;

    /** How wide a driller digs when its sign does not say. */
    private static final int DEFAULT_DRILL_WIDTH = 3;

    /** The widest a driller may dig. */
    private static final int MAX_DRILL_WIDTH = 16;

    /** How deep a driller digs when its sign does not say, and the deepest it may. */
    private static final int MAX_DRILL_DEPTH = 384;

    /** How many places a terraformer or irrigator tries in one pass over its area. */
    private static final int TRIES_PER_PASS = 10;

    private Terrain() {}

    /**
     * Breaks the block below the one the sign hangs on, putting what it drops in the container
     * above.
     *
     * <p>Line 3 may name a block, and then only that block is broken. Line 4 reading {@code true}
     * keeps the block whole rather than taking what it would drop, which is what a silk touch pick
     * would do to it.
     */
    public static SelfTriggeringICLogic blockBreakerBelow() {
        return new BlockBreaker(false);
    }

    /**
     * Breaks the block above the one the sign hangs on, putting what it drops in the container
     * below.
     *
     * <p>{@link #blockBreakerBelow()} turned over, and read from its sign in exactly the same way.
     */
    public static SelfTriggeringICLogic blockBreakerAbove() {
        return new BlockBreaker(true);
    }

    /**
     * Fills an area with a liquid while driven, and takes it away again when it is not.
     *
     * <p>Line 3 reads {@code water} or {@code lava} and line 4 is the area to cover. Driving it
     * fills every empty block in that area; letting it go drains every block of that liquid back
     * out. A lever on one therefore always agrees with what is in the ground.
     *
     * <p>Only the liquid it was told to place is ever removed, so draining a valley of water
     * leaves a lava flow that was already in it alone.
     */
    public static SelfTriggeringICLogic liquidFlooder() {
        return new LiquidFlooder();
    }

    /**
     * Lifts still liquid from below into buckets in the container above.
     *
     * <p>Looks straight down for a source block and, failing that, follows a run of liquid
     * sideways to find where it is coming from. Each source block taken costs an empty bucket and
     * gives back a full one, so a pump runs exactly as long as there are buckets to fill.
     */
    public static SelfTriggeringICLogic pump() {
        return new Pump();
    }

    /**
     * Pours liquid out of buckets in the container below into an area.
     *
     * <p>The reverse of the pump, and its area is read the same way a flooder's is. It fills the
     * nearest empty block first, so a spigot pouring into a channel fills it from the near end
     * rather than wherever a walk happened to reach.
     */
    public static SelfTriggeringICLogic spigot() {
        return new Spigot();
    }

    /**
     * Feeds bonemeal from the container above to whatever is growing in an area.
     *
     * <p>Line 3 is the area. What bonemeal does to a plant is the game's own business, so this
     * fertilises exactly what a player standing there with a handful would, and needs teaching
     * nothing about a plant added to the game later.
     *
     * <p>The bonemeal is only spent where it took, so a pass over an area of full-grown wheat
     * costs nothing.
     */
    public static SelfTriggeringICLogic terraformer(RandomGenerator random) {
        return new Terraformer(random);
    }

    /**
     * Waters dry farmland in an area from the container above.
     *
     * <p>Line 3 is the area. Each patch watered costs a bucket of water and gives an empty one
     * back. A block of water standing on the container's own place is taken instead, which is how
     * a farm fed by a channel rather than by buckets is built.
     */
    public static SelfTriggeringICLogic irrigator(RandomGenerator random) {
        return new Irrigator(random);
    }

    /**
     * Digs a shaft downward, putting everything it takes into the container above.
     *
     * <p>Line 3 is how wide a patch to dig under and line 4 how deep to go. Each pulse takes the
     * topmost block of one column of that patch, so a driller left on a clock sinks a shaft the
     * width it was given.
     *
     * <p>Bedrock stops it, and so does running out of room to put what it digs.
     */
    public static SelfTriggeringICLogic driller(RandomGenerator random) {
        return new Driller(random);
    }

    /** Breaks the block on one side of the chip and puts it away on the other. */
    private record BlockBreaker(boolean above) implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(breakBlock(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            state.setMainOutput(breakBlock(state));
        }

        private boolean breakBlock(ChipState state) {
            ChipWorld world = state.world();
            Vec3i target = state.backPosition().add(0, above ? 1 : -1, 0);
            if (!world.isLoaded(target) || !world.isInBounds(target) || world.isAir(target)) {
                return false;
            }
            if (Blocks.UNBREAKABLE.contains(world.blockAt(target))) {
                return false;
            }

            Optional<Key> only = world.resolveBlock(state.sign().trimmedText(FIRST_LINE));
            if (only.isPresent() && !only.get().equals(world.blockAt(target))) {
                return false;
            }

            boolean intact = state.sign().trimmedText(SECOND_LINE).equalsIgnoreCase(INTACT);
            Map<Key, Integer> yield = intact ? world.intactDropsAt(target) : world.dropsAt(target);

            Stockpile stockpile =
                    state.stockpileNear(state.backPosition().add(0, above ? -1 : 1, 0), 0, Set.of());
            if (!hasRoomForAll(stockpile, yield)) {
                return false;
            }
            if (!world.setBlockAt(target, Blocks.AIR_KEY)) {
                return false;
            }

            yield.forEach(stockpile::give);
            return true;
        }
    }

    /** Fills an area with a liquid while driven and empties it again when it is not. */
    private static final class LiquidFlooder implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            flood(state, state.isAnyInputActive());
        }

        @Override
        public void tick(ChipState state) {
            flood(state, state.isAnyInputActive());
        }

        private static void flood(ChipState state, boolean filling) {
            ChipWorld world = state.world();
            Key liquid = state.sign().trimmedText(FIRST_LINE).equalsIgnoreCase(LAVA)
                    ? Blocks.LAVA_KEY
                    : Blocks.WATER_KEY;

            Bounds area =
                    SignArea.on(state, SECOND_LINE, DEFAULT_FLOOD_RADIUS).around(state.backPosition());

            boolean changed = false;
            for (Vec3i position : within(world, area)) {
                if (filling && world.isAir(position)) {
                    changed |= world.setBlockAt(position, liquid);
                } else if (!filling && world.blockAt(position).equals(liquid)) {
                    changed |= world.setBlockAt(position, Blocks.AIR_KEY);
                }
            }
            state.setMainOutput(changed);
        }
    }

    /** Lifts still liquid into buckets in the container above. */
    private static final class Pump implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(pump(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(pump(state));
            }
        }

        private static boolean pump(ChipState state) {
            ChipWorld world = state.world();
            Vec3i back = state.backPosition();
            Stockpile buckets = state.stockpileNear(back.add(0, 1, 0), 0, Set.of());

            Optional<Vec3i> source = sourceUnder(world, back);
            return source.isPresent() && fill(world, buckets, source.get());
        }

        /**
         * The nearest source block under the pump.
         *
         * <p>Straight down first, and then outward along whatever liquid is in the way, because a
         * column carrying a flow is standing under something rather than under nothing. Nearest
         * first, so a pump over a spring takes the spring and not a puddle it happens to feed.
         */
        private static Optional<Vec3i> sourceUnder(ChipWorld world, Vec3i back) {
            Set<Vec3i> seen = new HashSet<>();
            Deque<Vec3i> queue = new ArrayDeque<>();
            for (int depth = 1; depth <= PUMP_DEPTH; depth++) {
                queue.add(back.add(0, -depth, 0));
            }

            while (!queue.isEmpty() && seen.size() < PUMP_LIMIT) {
                Vec3i position = queue.poll();
                if (!seen.add(position) || !world.isLoaded(position) || !world.isInBounds(position)) {
                    continue;
                }
                if (!world.isWater(position) && !world.isLava(position)) {
                    continue;
                }
                if (world.isLiquidSource(position)) {
                    return Optional.of(position);
                }
                queue.add(position.add(1, 0, 0));
                queue.add(position.add(-1, 0, 0));
                queue.add(position.add(0, 0, 1));
                queue.add(position.add(0, 0, -1));
            }
            return Optional.empty();
        }

        /** Trades an empty bucket for a full one and takes the liquid out of the world. */
        private static boolean fill(ChipWorld world, Stockpile buckets, Vec3i source) {
            Key filled = world.isLava(source) ? LAVA_BUCKET : WATER_BUCKET;
            Key liquid = world.isLava(source) ? Blocks.LAVA_KEY : Blocks.WATER_KEY;

            if (!buckets.hasRoomFor(filled, 1) || !buckets.takeAll(BUCKET, 1)) {
                return false;
            }
            if (!world.setBlockAt(source, Blocks.AIR_KEY)) {
                buckets.give(BUCKET, 1);
                return false;
            }
            if (buckets.give(filled, 1) > 0) {
                buckets.give(BUCKET, 1);
                world.setBlockAt(source, liquid);
                return false;
            }
            return true;
        }
    }

    /** Pours liquid out of buckets in the container below into the nearest empty block. */
    private static final class Spigot implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(pour(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(pour(state));
            }
        }

        private static boolean pour(ChipState state) {
            ChipWorld world = state.world();
            Vec3i back = state.backPosition();
            Stockpile buckets = state.stockpileNear(back.add(0, -1, 0), 0, Set.of());

            SignArea asked = SignArea.on(state, FIRST_LINE, DEFAULT_AREA_RADIUS);
            Optional<Vec3i> spot =
                    nearestEmpty(world, asked.around(back), asked.centreFrom(back));
            if (spot.isEmpty()) {
                return false;
            }

            for (Key full : List.of(WATER_BUCKET, LAVA_BUCKET)) {
                if (!buckets.takeAll(full, 1)) {
                    continue;
                }
                Key liquid = full.equals(LAVA_BUCKET) ? Blocks.LAVA_KEY : Blocks.WATER_KEY;
                if (!world.setBlockAt(spot.get(), liquid)) {
                    buckets.give(full, 1);
                    return false;
                }
                buckets.give(BUCKET, 1);
                return true;
            }
            return false;
        }

        /** The empty block closest to the middle of the area, so a channel fills from its head. */
        private static Optional<Vec3i> nearestEmpty(ChipWorld world, Bounds area, Vec3i centre) {
            Set<Vec3i> seen = new HashSet<>();
            Deque<Vec3i> queue = new ArrayDeque<>();
            queue.add(centre);

            while (!queue.isEmpty() && seen.size() < SPIGOT_LIMIT) {
                Vec3i position = queue.poll();
                if (!area.contains(position) || !seen.add(position)) {
                    continue;
                }
                if (!world.isLoaded(position) || !world.isInBounds(position)) {
                    continue;
                }
                if (world.isAir(position)) {
                    return Optional.of(position);
                }
                if (!world.isWater(position) && !world.isLava(position)) {
                    continue;
                }
                queue.add(position.add(1, 0, 0));
                queue.add(position.add(-1, 0, 0));
                queue.add(position.add(0, 0, 1));
                queue.add(position.add(0, 0, -1));
                queue.add(position.add(0, 1, 0));
                queue.add(position.add(0, -1, 0));
            }
            return Optional.empty();
        }
    }

    /** Feeds bonemeal from above to whatever is growing in an area. */
    private record Terraformer(RandomGenerator random) implements SelfTriggeringICLogic {

        /** Bonemeal, which is the only thing this chip spends. */
        private static final Key BONEMEAL = Key.key("bone_meal");

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(fertilise(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(fertilise(state));
            }
        }

        private boolean fertilise(ChipState state) {
            ChipWorld world = state.world();
            Vec3i back = state.backPosition();
            Stockpile supply = state.stockpileNear(back.add(0, 1, 0), 0, Set.of());

            Bounds area = SignArea.on(state, FIRST_LINE, DEFAULT_AREA_RADIUS).around(back);

            for (int attempt = 0; attempt < TRIES_PER_PASS; attempt++) {
                if (!supply.has(BONEMEAL, 1)) {
                    return false;
                }
                Optional<Vec3i> spot = somewhereIn(area, random, world);
                if (spot.isEmpty() || !world.applyBonemeal(spot.get())) {
                    continue;
                }
                supply.takeAll(BONEMEAL, 1);
                return true;
            }
            return false;
        }
    }

    /** Waters dry farmland in an area from the container above. */
    private record Irrigator(RandomGenerator random) implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(irrigate(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(irrigate(state));
            }
        }

        private boolean irrigate(ChipState state) {
            ChipWorld world = state.world();
            Vec3i back = state.backPosition();
            Bounds area = SignArea.on(state, FIRST_LINE, DEFAULT_AREA_RADIUS).around(back);

            for (int attempt = 0; attempt < TRIES_PER_PASS; attempt++) {
                Optional<Vec3i> spot = somewhereIn(area, random, world);
                if (spot.isEmpty() || !world.isDryFarmland(spot.get())) {
                    continue;
                }
                if (!takeWater(state, back)) {
                    return false;
                }
                return world.waterFarmland(spot.get());
            }
            return false;
        }

        /**
         * Takes a bucket of water from the container above, or the block of water standing there.
         *
         * <p>The second is what makes a farm fed by a channel work: a run of water led to the
         * block above the chip is drawn on directly rather than having to be bucketed first.
         */
        private static boolean takeWater(ChipState state, Vec3i back) {
            Vec3i above = back.add(0, 1, 0);
            Stockpile supply = state.stockpileNear(above, 0, Set.of());

            if (supply.takeAll(WATER_BUCKET, 1)) {
                supply.give(BUCKET, 1);
                return true;
            }
            if (state.world().isWater(above)) {
                return state.world().setBlockAt(above, Blocks.AIR_KEY);
            }
            return false;
        }
    }

    /** Digs a shaft downward, putting what it takes into the container above. */
    private record Driller(RandomGenerator random) implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(drill(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                state.setMainOutput(drill(state));
            }
        }

        private boolean drill(ChipState state) {
            ChipWorld world = state.world();
            Vec3i back = state.backPosition();

            int width = boundedNumber(
                    state.sign().trimmedText(FIRST_LINE), 1, MAX_DRILL_WIDTH, DEFAULT_DRILL_WIDTH);
            int depth = boundedNumber(
                    state.sign().trimmedText(SECOND_LINE), 1, MAX_DRILL_DEPTH, MAX_DRILL_DEPTH);

            int offsetX = random.nextInt(width) - width / 2;
            int offsetZ = random.nextInt(width) - width / 2;

            Stockpile spoil = state.stockpileNear(back.add(0, 1, 0), 0, Set.of());
            return sink(world, spoil, back.add(offsetX, -1, offsetZ), depth);
        }

        /** Takes the topmost solid block of one column, down to the depth the sign allows. */
        private static boolean sink(ChipWorld world, Stockpile spoil, Vec3i top, int depth) {
            for (int step = 0; step < depth; step++) {
                Vec3i position = top.add(0, -step, 0);
                if (!world.isInBounds(position) || !world.isLoaded(position)) {
                    return false;
                }
                if (world.isAir(position)) {
                    continue;
                }
                if (Blocks.UNBREAKABLE.contains(world.blockAt(position))) {
                    return false;
                }

                Map<Key, Integer> yield = world.dropsAt(position);
                if (!hasRoomForAll(spoil, yield)) {
                    return false;
                }
                if (!world.setBlockAt(position, Blocks.AIR_KEY)) {
                    return false;
                }
                yield.forEach(spoil::give);
                return true;
            }
            return false;
        }
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

    /** Every block of an area this thread may read. */
    private static List<Vec3i> within(ChipWorld world, Bounds area) {
        List<Vec3i> found = new ArrayList<>();
        for (int x = area.from().x(); x <= area.to().x(); x++) {
            for (int y = area.from().y(); y <= area.to().y(); y++) {
                for (int z = area.from().z(); z <= area.to().z(); z++) {
                    Vec3i position = new Vec3i(x, y, z);
                    if (world.isLoaded(position) && world.isInBounds(position)) {
                        found.add(position);
                    }
                }
            }
        }
        return found;
    }

    /** Whether everything a broken block would drop has somewhere to go. */
    private static boolean hasRoomForAll(Stockpile stockpile, Map<Key, Integer> yield) {
        if (stockpile.isUnlimited()) {
            return true;
        }
        for (Map.Entry<Key, Integer> entry : yield.entrySet()) {
            if (!stockpile.hasRoomFor(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
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
