package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Which way a piece of rail runs.
 *
 * <p>A rail always joins exactly two of its four sides, whether it climbs or lies flat, and that
 * pair is all a mechanic needs: a junction sets the pair to steer a cart, and a lift reads it to
 * decide which way the cart leaves the floor it arrives on.
 */
@NullMarked
public enum RailShape {

    /** Flat, running north to south. */
    NORTH_SOUTH(BlockFace.NORTH, BlockFace.SOUTH),

    /** Flat, running east to west. */
    EAST_WEST(BlockFace.EAST, BlockFace.WEST),

    /** Climbing towards the east, along the same line as {@link #EAST_WEST}. */
    ASCENDING_EAST(BlockFace.EAST, BlockFace.WEST),

    /** Climbing towards the west, along the same line as {@link #EAST_WEST}. */
    ASCENDING_WEST(BlockFace.EAST, BlockFace.WEST),

    /** Climbing towards the north, along the same line as {@link #NORTH_SOUTH}. */
    ASCENDING_NORTH(BlockFace.NORTH, BlockFace.SOUTH),

    /** Climbing towards the south, along the same line as {@link #NORTH_SOUTH}. */
    ASCENDING_SOUTH(BlockFace.NORTH, BlockFace.SOUTH),

    /** A curve joining south and east. */
    SOUTH_EAST(BlockFace.SOUTH, BlockFace.EAST),

    /** A curve joining south and west. */
    SOUTH_WEST(BlockFace.SOUTH, BlockFace.WEST),

    /** A curve joining north and west. */
    NORTH_WEST(BlockFace.NORTH, BlockFace.WEST),

    /** A curve joining north and east. */
    NORTH_EAST(BlockFace.NORTH, BlockFace.EAST);

    private final BlockFace first;
    private final BlockFace second;

    RailShape(BlockFace first, BlockFace second) {
        this.first = first;
        this.second = second;
    }

    /** The two sides this rail joins. */
    public List<BlockFace> directions() {
        return List.of(first, second);
    }

    /** Whether a cart on this rail can leave by a particular side. */
    public boolean runs(BlockFace direction) {
        return first == direction || second == direction;
    }

    /** Whether the rail climbs rather than lying flat. */
    public boolean ascends() {
        return this == ASCENDING_EAST
                || this == ASCENDING_WEST
                || this == ASCENDING_NORTH
                || this == ASCENDING_SOUTH;
    }

    /**
     * The flat rail joining two sides.
     *
     * <p>Empty when no rail joins them, which is the case for a side paired with itself or with
     * its opposite along the wrong axis. A junction that asks for an impossible pair is left as it
     * was rather than being bent into something else.
     */
    public static Optional<RailShape> joining(BlockFace one, BlockFace other) {
        if (!one.isCardinal() || !other.isCardinal() || one == other) {
            return Optional.empty();
        }
        for (RailShape shape : values()) {
            if (shape.ascends()) {
                continue;
            }
            if (shape.runs(one) && shape.runs(other)) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }
}
