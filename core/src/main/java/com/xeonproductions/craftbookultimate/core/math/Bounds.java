package com.xeonproductions.craftbookultimate.core.math;

import org.jspecify.annotations.NullMarked;

/**
 * A box of blocks, given by two opposite corners.
 *
 * <p>Both corners are inside the box. A box of one block has the same corner twice, which is the
 * natural answer for a chip that reaches exactly one place, rather than something to special-case.
 *
 * <p>Corners are normalised as the box is built, so a caller may hand over whichever two it has
 * without working out which is the lower.
 *
 * @param from the corner with the smaller coordinate on every axis
 * @param to the corner with the larger
 */
@NullMarked
public record Bounds(Vec3i from, Vec3i to) {

    public Bounds {
        Vec3i lower = new Vec3i(
                Math.min(from.x(), to.x()), Math.min(from.y(), to.y()), Math.min(from.z(), to.z()));
        Vec3i upper = new Vec3i(
                Math.max(from.x(), to.x()), Math.max(from.y(), to.y()), Math.max(from.z(), to.z()));
        from = lower;
        to = upper;
    }

    /** A box around one position, reaching {@code radius} blocks on every axis. */
    public static Bounds around(Vec3i centre, int radius) {
        return around(centre, radius, radius, radius);
    }

    /** A box around one position, reaching a different distance across than it does up. */
    public static Bounds around(Vec3i centre, int radius, int below, int above) {
        return new Bounds(
                new Vec3i(centre.x() - radius, centre.y() - below, centre.z() - radius),
                new Vec3i(centre.x() + radius, centre.y() + above, centre.z() + radius));
    }

    /** A box of exactly one block. */
    public static Bounds of(Vec3i only) {
        return new Bounds(only, only);
    }

    /** How many blocks across the box runs east to west. */
    public int width() {
        return to.x() - from.x() + 1;
    }

    /** How many blocks tall the box is. */
    public int height() {
        return to.y() - from.y() + 1;
    }

    /** How many blocks across the box runs north to south. */
    public int length() {
        return to.z() - from.z() + 1;
    }

    /** How many blocks the box holds. */
    public long volume() {
        return (long) width() * height() * length();
    }

    /** Whether a position is inside the box. */
    public boolean contains(Vec3i position) {
        return position.x() >= from.x() && position.x() <= to.x()
                && position.y() >= from.y() && position.y() <= to.y()
                && position.z() >= from.z() && position.z() <= to.z();
    }

    /** How the box reads to somebody being told where it is. */
    public String describe() {
        return from.x() + "," + from.y() + "," + from.z()
                + " to " + to.x() + "," + to.y() + "," + to.z()
                + "  (" + width() + "x" + height() + "x" + length() + ")";
    }
}
