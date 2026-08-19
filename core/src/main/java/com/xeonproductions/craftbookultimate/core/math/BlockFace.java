package com.xeonproductions.craftbookultimate.core.math;

/**
 * The six axis-aligned directions, plus the four diagonals used by sign-relative
 * "left/right" resolution.
 *
 * <p>Mirrors the server's own direction enum without depending on it, so direction
 * arithmetic stays unit testable.
 */
public enum BlockFace {
    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    NORTH_EAST(1, 0, -1),
    SOUTH_EAST(1, 0, 1),
    SOUTH_WEST(-1, 0, 1),
    NORTH_WEST(-1, 0, -1),
    SELF(0, 0, 0);

    private static final BlockFace[] HORIZONTAL = {NORTH, EAST, SOUTH, WEST};

    private final int deltaX;
    private final int deltaY;
    private final int deltaZ;

    BlockFace(int deltaX, int deltaY, int deltaZ) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
    }

    public int deltaX() {
        return deltaX;
    }

    public int deltaY() {
        return deltaY;
    }

    public int deltaZ() {
        return deltaZ;
    }

    /** The four cardinal directions, in clockwise order starting at north. */
    public static BlockFace[] horizontals() {
        return HORIZONTAL.clone();
    }

    public boolean isHorizontal() {
        return deltaY == 0 && this != SELF;
    }

    public boolean isVertical() {
        return this == UP || this == DOWN;
    }

    /** True for the four cardinal directions only, excluding diagonals and the vertical axis. */
    public boolean isCardinal() {
        return this == NORTH || this == EAST || this == SOUTH || this == WEST;
    }

    public BlockFace opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
            case NORTH_EAST -> SOUTH_WEST;
            case SOUTH_EAST -> NORTH_WEST;
            case SOUTH_WEST -> NORTH_EAST;
            case NORTH_WEST -> SOUTH_EAST;
            case SELF -> SELF;
        };
    }

    /**
     * Rotates a cardinal direction a quarter turn clockwise when viewed from above.
     *
     * @throws IllegalStateException if this direction is not cardinal
     */
    public BlockFace rotateClockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new IllegalStateException("Cannot rotate non-cardinal direction " + this);
        };
    }

    /**
     * Rotates a cardinal direction a quarter turn counter-clockwise when viewed from above.
     *
     * @throws IllegalStateException if this direction is not cardinal
     */
    public BlockFace rotateCounterClockwise() {
        return rotateClockwise().opposite();
    }
}
