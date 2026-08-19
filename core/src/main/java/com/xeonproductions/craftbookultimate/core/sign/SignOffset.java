package com.xeonproductions.craftbookultimate.core.sign;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * A step along one axis, as the single-block chips write it on a sign.
 *
 * <p>Written as an axis, a sign and a distance: {@code X+5}, {@code Y-2}, {@code Z+3}. Only one
 * axis at a time, which is why these chips reach a nearby block rather than an arbitrary one.
 *
 * <p>The distance is capped, so a chip cannot be pointed at something a long way from the sign
 * that put it there.
 */
@NullMarked
public final class SignOffset {

    /** The furthest a chip may reach along its axis. */
    public static final int MAX_DISTANCE = 9;

    private SignOffset() {}

    /**
     * Reads an offset as written on a sign.
     *
     * @param text the offset, such as {@code Y+1}; case does not matter
     * @return the offset, or empty if the text is not one or reaches too far
     */
    public static Optional<Vec3i> parse(String text) {
        String cleaned = text.trim();
        if (cleaned.length() < 3) {
            return Optional.empty();
        }

        char sign = cleaned.charAt(1);
        if (sign != '+' && sign != '-') {
            return Optional.empty();
        }

        int distance;
        try {
            // The sign character belongs to the number, so the axis alone is dropped.
            distance = Integer.parseInt(cleaned.substring(1));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        if (Math.abs(distance) > MAX_DISTANCE) {
            return Optional.empty();
        }

        return switch (Character.toUpperCase(cleaned.charAt(0))) {
            case 'X' -> Optional.of(new Vec3i(distance, 0, 0));
            case 'Y' -> Optional.of(new Vec3i(0, distance, 0));
            case 'Z' -> Optional.of(new Vec3i(0, 0, distance));
            default -> Optional.empty();
        };
    }
}
