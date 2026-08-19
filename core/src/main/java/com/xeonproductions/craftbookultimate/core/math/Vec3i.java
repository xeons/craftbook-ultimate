package com.xeonproductions.craftbookultimate.core.math;

/**
 * An immutable integer position in world space.
 *
 * <p>This is deliberately independent of any server API so that mechanic and IC logic
 * can be exercised in plain unit tests.
 */
public record Vec3i(int x, int y, int z) {

    public static final Vec3i ZERO = new Vec3i(0, 0, 0);

    public Vec3i offset(BlockFace face) {
        return offset(face, 1);
    }

    public Vec3i offset(BlockFace face, int distance) {
        if (distance == 0) {
            return this;
        }
        return new Vec3i(
                x + face.deltaX() * distance,
                y + face.deltaY() * distance,
                z + face.deltaZ() * distance);
    }

    public Vec3i add(int dx, int dy, int dz) {
        return new Vec3i(x + dx, y + dy, z + dz);
    }

    public Vec3i add(Vec3i other) {
        return add(other.x, other.y, other.z);
    }

    public Vec3i subtract(Vec3i other) {
        return new Vec3i(x - other.x, y - other.y, z - other.z);
    }

    /**
     * The number of axis steps between the two positions, i.e. the Chebyshev distance.
     * Used by radius checks that treat an area as a cube rather than a sphere.
     */
    public int chebyshevDistance(Vec3i other) {
        return Math.max(Math.abs(x - other.x), Math.max(Math.abs(y - other.y), Math.abs(z - other.z)));
    }

    /** The squared euclidean distance, kept in integer space to avoid a square root. */
    public long distanceSquared(Vec3i other) {
        long dx = (long) x - other.x;
        long dy = (long) y - other.y;
        long dz = (long) z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
