package com.xeonproductions.craftbookultimate.core.math;

/**
 * An immutable position or direction in world space, measured in blocks but not whole ones.
 *
 * <p>Blocks are named by {@link Vec3i}; this is for the things that live between them. A
 * projectile leaves from a point on the face of a block, a particle appears in the middle of one,
 * and a velocity is a direction with a length. None of those land on integers.
 */
public record Vec3d(double x, double y, double z) {

    public static final Vec3d ZERO = new Vec3d(0, 0, 0);

    /** The corner of a block, which is the position the game gives it. */
    public static Vec3d of(Vec3i block) {
        return new Vec3d(block.x(), block.y(), block.z());
    }

    /** The middle of a block, horizontally, at its floor. */
    public static Vec3d centreOf(Vec3i block) {
        return new Vec3d(block.x() + 0.5, block.y(), block.z() + 0.5);
    }

    /** The exact middle of a block, in all three directions. */
    public static Vec3d middleOf(Vec3i block) {
        return new Vec3d(block.x() + 0.5, block.y() + 0.5, block.z() + 0.5);
    }

    /** A unit vector pointing the way a face points. */
    public static Vec3d of(BlockFace face) {
        return new Vec3d(face.deltaX(), face.deltaY(), face.deltaZ());
    }

    public Vec3d add(double dx, double dy, double dz) {
        return new Vec3d(x + dx, y + dy, z + dz);
    }

    public Vec3d add(Vec3d other) {
        return add(other.x, other.y, other.z);
    }

    public Vec3d subtract(Vec3d other) {
        return new Vec3d(x - other.x, y - other.y, z - other.z);
    }

    public Vec3d multiply(double factor) {
        return new Vec3d(x * factor, y * factor, z * factor);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** The squared distance to another point, which avoids a square root when comparing. */
    public double distanceSquared(Vec3d other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** The same direction with a length of one, or zero if there is no direction to keep. */
    public Vec3d normalise() {
        double length = length();
        return length == 0 ? ZERO : multiply(1 / length);
    }

    /** Rotates around the vertical axis, clockwise when viewed from above. */
    public Vec3d rotateAroundY(double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(x * cos + z * sin, y, z * cos - x * sin);
    }

    /** The block containing this point. */
    public Vec3i toBlock() {
        return new Vec3i(
                (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
