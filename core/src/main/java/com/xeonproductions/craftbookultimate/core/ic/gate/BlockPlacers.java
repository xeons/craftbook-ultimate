package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that build and unbuild a run of blocks.
 *
 * <p>All of them work the same way: while driven they fill an area with a block, and while idle
 * they take it away again. Materials come from the chip's {@linkplain ChipState#stockpile()
 * stockpile}, so a bridge costs what it would cost to lay by hand and gives the blocks back when
 * it retracts.
 *
 * <h2>Where the area sits</h2>
 *
 * <p>The area runs away from the front of the sign, starting one block past the block the sign
 * hangs on so that the sign's own support is never part of it. It is centred on the sign's column
 * across its width, and rises from the sign's own level.
 *
 * <h2>Authorisation</h2>
 *
 * <p>A chip that does not force is created unauthorised, and refuses to act while its area still
 * contains the block it would place. That stops one being built over somebody's structure and
 * used to take it apart. Once the area is clear the chip authorises itself and works from then
 * on. A forcing chip skips the check, which is why it needs elevated permission.
 */
@NullMarked
public final class BlockPlacers {

    /** The line naming the block to place. */
    private static final int BLOCK_LINE = 2;

    /** The line giving the area's dimensions. */
    private static final int DIMENSIONS_LINE = 3;

    /** The sign line carrying the model reference and its markers. */
    private static final int IDENTIFIER_LINE = 1;

    /** Marks a chip that has yet to prove it is not being used to mine what is already there. */
    private static final char AUTHORISATION_FLAG = '*';

    /** How far past the sign the area begins. One clears the block the sign hangs on. */
    private static final int AREA_START_OFFSET = 2;

    private BlockPlacers() {}

    /**
     * A bridge: a flat run of blocks extending away from the sign.
     *
     * <p>Line 2 names the block. Line 3 reads {@code width:length} with an optional
     * {@code :verticalOffset}. The bridge is one block thick.
     *
     * @param forcing whether to skip the authorisation check
     */
    public static ICLogic bridge(boolean forcing) {
        return new AreaBuilder(Shape.BRIDGE, forcing);
    }

    /**
     * A door: an upright panel of blocks across the sign's facing.
     *
     * <p>Line 2 names the block. Line 3 reads {@code width:height} with an optional
     * {@code :verticalOffset}. The door is one block deep.
     *
     * @param forcing whether to skip the authorisation check
     */
    public static ICLogic door(boolean forcing) {
        return new AreaBuilder(Shape.DOOR, forcing);
    }

    /**
     * Sets a single block two above the block the sign hangs on.
     *
     * <p>Line 2 names the block, and line 3 reading {@code Force} lets it replace whatever is
     * already there rather than only filling air. The output reports whether the block was set.
     */
    public static ICLogic setBlockAbove() {
        return new SingleSetter(2);
    }

    /** Sets a single block two below the block the sign hangs on, as {@link #setBlockAbove()} does. */
    public static ICLogic setBlockBelow() {
        return new SingleSetter(-2);
    }

    /**
     * Sets a single block at an offset from the block the sign hangs on.
     *
     * <p>Line 2 reads {@code x:y:z:block}. Unlike the fixed setters this one pays for what it
     * places out of the chip's stockpile and refunds it when the chip goes idle.
     */
    public static ICLogic flexSet() {
        return new FlexSetter();
    }

    /** Which way a chip's two configured dimensions run. */
    private enum Shape {
        /** Width across, length away from the sign, one block thick. */
        BRIDGE,
        /** Width across, height upward, one block deep. */
        DOOR
    }

    /** The dimensions a chip read off its sign. */
    private record Dimensions(int width, int length, int height, int verticalOffset) {}

    /** Fills and empties an area, paying for what it places out of a stockpile. */
    private record AreaBuilder(Shape shape, boolean forcing) implements ICLogic {

        @Override
        public void trigger(ChipState state) {
            Optional<Key> block = state.world().resolveBlock(state.sign().trimmedText(BLOCK_LINE));
            Optional<Dimensions> dimensions = readDimensions(state);
            if (block.isEmpty() || dimensions.isEmpty()) {
                return;
            }

            List<Vec3i> area = areaOf(state, dimensions.get());
            if (!isEntirelyLoaded(state.world(), area)) {
                return;
            }

            if (!isAuthorised(state, area, block.get())) {
                return;
            }

            if (state.isAnyInputActive()) {
                place(state, area, block.get());
            } else {
                clear(state, area, block.get());
            }
        }

        /**
         * Whether the chip may act, authorising it if it has become able to.
         *
         * <p>An unauthorised chip whose area still holds the block it places stays unauthorised
         * and does nothing.
         */
        private boolean isAuthorised(ChipState state, List<Vec3i> area, Key block) {
            String identifier = state.sign().trimmedText(IDENTIFIER_LINE);
            if (forcing || identifier.indexOf(AUTHORISATION_FLAG) < 0) {
                return true;
            }

            if (areaContains(state.world(), area, block)) {
                return false;
            }

            state.setSignLine(IDENTIFIER_LINE, identifier.replace(String.valueOf(AUTHORISATION_FLAG), ""));
            return true;
        }

        /**
         * Fills the area, taking each block from the stockpile as it goes.
         *
         * <p>Only positions that can be built through are filled, so the area may cost less than
         * its full size. Running out of materials part-way leaves what was already paid for.
         */
        private void place(ChipState state, List<Vec3i> area, Key block) {
            ChipWorld world = state.world();
            Stockpile stockpile = state.stockpile();

            List<Vec3i> fillable = new ArrayList<>();
            for (Vec3i position : area) {
                if (canBuildThrough(world, position)) {
                    fillable.add(position);
                }
            }
            if (fillable.isEmpty()) {
                return;
            }

            int affordable = stockpile.isUnlimited()
                    ? fillable.size()
                    : Math.min(fillable.size(), stockpile.count(block));
            if (affordable <= 0) {
                return;
            }

            int placed = 0;
            for (Vec3i position : fillable) {
                if (placed >= affordable || !stockpile.takeAll(block, 1)) {
                    break;
                }
                if (world.setBlockAt(position, block)) {
                    placed++;
                } else {
                    stockpile.give(block, 1);
                }
            }
        }

        /**
         * Empties the area, giving each block back to the stockpile.
         *
         * <p>Only the chip's own block is removed, so anything a player put in the way is left
         * alone. A block the stockpile cannot take is left in place rather than destroyed.
         */
        private void clear(ChipState state, List<Vec3i> area, Key block) {
            ChipWorld world = state.world();
            Stockpile stockpile = state.stockpile();

            for (Vec3i position : area) {
                if (!world.blockAt(position).equals(block)) {
                    continue;
                }
                if (!stockpile.isUnlimited() && !stockpile.hasRoomFor(block, 1)) {
                    break;
                }
                if (world.setBlockAt(position, Blocks.AIR_KEY)) {
                    stockpile.give(block, 1);
                }
            }
        }

        /**
         * The blocks this chip acts on, in the order it acts on them.
         *
         * <p>Ordered outward from the sign, so a chip that runs out of materials leaves a
         * structure that reaches part of the way rather than one that starts in mid-air. The
         * order does not depend on which way the sign happens to face.
         */
        private List<Vec3i> areaOf(ChipState state, Dimensions dimensions) {
            BlockFace away = state.facing().opposite();
            BlockFace across = away.rotateClockwise();

            Vec3i origin = state.signPosition()
                    .offset(away, AREA_START_OFFSET)
                    .offset(across, -(dimensions.width() / 2))
                    .add(0, dimensions.verticalOffset(), 0);

            List<Vec3i> positions = new ArrayList<>(
                    dimensions.width() * dimensions.length() * dimensions.height());

            for (int along = 0; along < dimensions.length(); along++) {
                for (int side = 0; side < dimensions.width(); side++) {
                    for (int up = 0; up < dimensions.height(); up++) {
                        positions.add(origin.offset(away, along).offset(across, side).add(0, up, 0));
                    }
                }
            }
            return positions;
        }

        /** Reads the two configured dimensions and the optional vertical offset. */
        private Optional<Dimensions> readDimensions(ChipState state) {
            String[] parts = state.sign().trimmedText(DIMENSIONS_LINE).split(":");
            if (parts.length < 2) {
                return Optional.empty();
            }

            try {
                int across = Integer.parseInt(parts[0].trim());
                int along = Integer.parseInt(parts[1].trim());
                int verticalOffset = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;

                if (across < 1 || along < 1) {
                    return Optional.empty();
                }

                return Optional.of(shape == Shape.BRIDGE
                        ? new Dimensions(across, along, 1, verticalOffset)
                        : new Dimensions(across, 1, along, verticalOffset));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        /** Whether a position can be built into, which means air or a liquid. */
        private static boolean canBuildThrough(ChipWorld world, Vec3i position) {
            return world.isAir(position) || world.isWater(position) || world.isLava(position);
        }

        /** Whether the area holds the block anywhere. */
        private static boolean areaContains(ChipWorld world, List<Vec3i> area, Key block) {
            for (Vec3i position : area) {
                if (world.blockAt(position).equals(block)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether every block of the area can be reached.
         *
         * <p>A chip that reached into unloaded chunks would either pull them in or build half a
         * structure, so it does nothing instead.
         */
        private static boolean isEntirelyLoaded(ChipWorld world, List<Vec3i> area) {
            for (Vec3i position : area) {
                if (!world.isLoaded(position) || !world.isInBounds(position)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Sets one block at a fixed vertical offset, without paying for it.
     *
     * <p>These chips predate the stockpile and have always conjured their block, which is why
     * they are restricted rather than free to build.
     */
    private record SingleSetter(int verticalOffset) implements ICLogic {

        /** The line that turns on replacing whatever is already there. */
        private static final int FORCE_LINE = 3;

        /** What line 3 has to read for the chip to replace a block rather than only fill air. */
        private static final String FORCE = "Force";

        @Override
        public void trigger(ChipState state) {
            if (!state.isAnyInputActive()) {
                return;
            }

            Optional<Key> block = state.world().resolveBlock(state.sign().trimmedText(BLOCK_LINE));
            if (block.isEmpty()) {
                return;
            }

            Vec3i target = state.backPosition().add(0, verticalOffset, 0);
            ChipWorld world = state.world();
            boolean forcing = state.sign().trimmedText(FORCE_LINE).equalsIgnoreCase(FORCE);

            if (!world.isLoaded(target) || !world.isInBounds(target)
                    || (!forcing && !world.isAir(target))) {
                state.setMainOutput(false);
                return;
            }

            state.setMainOutput(world.setBlockAt(target, block.get()));
        }
    }

    /** Sets one block at a configured offset, paying for it out of the stockpile. */
    private static final class FlexSetter implements ICLogic {

        /** How many parts line 2 has: three offsets and a block name. */
        private static final int CONFIG_PARTS = 4;

        /**
         * The block part of the line, which is everything after the three offsets.
         *
         * <p>Rejoined rather than taken as one field, because a block written the old way carries
         * its own colon, as in {@code 0:1:0:35:14}.
         */
        private static String legacyAwareBlockPart(String[] parts) {
            StringBuilder block = new StringBuilder(parts[3].trim());
            for (int i = CONFIG_PARTS; i < parts.length; i++) {
                block.append(':').append(parts[i].trim());
            }
            return block.toString();
        }

        @Override
        public void trigger(ChipState state) {
            String[] parts = state.sign().trimmedText(BLOCK_LINE).split(":");
            if (parts.length < CONFIG_PARTS) {
                return;
            }

            Optional<Key> block = state.world().resolveBlock(legacyAwareBlockPart(parts));
            if (block.isEmpty()) {
                return;
            }

            Vec3i target;
            try {
                target = state.backPosition().add(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException e) {
                return;
            }

            ChipWorld world = state.world();
            if (!world.isLoaded(target) || !world.isInBounds(target)) {
                return;
            }

            Stockpile stockpile = state.stockpile();

            if (state.isAnyInputActive()) {
                if (!world.isAir(target) || !stockpile.takeAll(block.get(), 1)) {
                    return;
                }
                if (!world.setBlockAt(target, block.get())) {
                    stockpile.give(block.get(), 1);
                }
            } else if (world.blockAt(target).equals(block.get())
                    && (stockpile.isUnlimited() || stockpile.hasRoomFor(block.get(), 1))
                    && world.setBlockAt(target, Blocks.AIR_KEY)) {
                stockpile.give(block.get(), 1);
            }
        }
    }
}
