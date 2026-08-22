// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The box a sign asks a chip to work on.
 *
 * <p>One line says both how far to reach and where the middle of that reach sits. It is written
 * {@code radius}, or {@code x,y,z} for a box that is not the same size on every axis, and either
 * may be followed by {@code =x:y:z} to move the middle off the block the sign hangs on.
 *
 * <p>Every reach is held to what the settings allow, rather than a sign asking for too much being
 * refused: narrowing the limit shortens an existing build instead of breaking it.
 *
 * <p>A run of chips share this because they share the line. The lightning chips, the flooder, the
 * spigot, the terraformer and the irrigator all take the same one, and a builder who has learnt it
 * on any of them has learnt it on all of them.
 *
 * @param radiusX how far the box reaches east and west
 * @param radiusY how far it reaches up and down
 * @param radiusZ how far it reaches north and south
 * @param centreOffset how far the middle sits from the block the sign hangs on
 */
@NullMarked
public record SignArea(int radiusX, int radiusY, int radiusZ, Vec3i centreOffset) {

    /** What separates the reach from where the middle of it sits. */
    private static final char CENTRE_SEPARATOR = '=';

    /** What separates one axis from the next in a reach. */
    private static final String AXIS_SEPARATOR = ",";

    /** What separates one axis from the next in an offset. */
    private static final String OFFSET_SEPARATOR = ":";

    /**
     * Reads the area one line of a sign asks for.
     *
     * @param state the chip whose sign is being read
     * @param line which line of the sign carries the area
     * @param fallbackRadius how far to reach when the line does not say
     */
    public static SignArea on(ChipState state, int line, int fallbackRadius) {
        String written = state.sign().trimmedText(line);
        int separator = written.indexOf(CENTRE_SEPARATOR);

        String reach = separator < 0 ? written : written.substring(0, separator);
        Vec3i offset = separator < 0
                ? Vec3i.ZERO
                : parseOffset(written.substring(separator + 1)).orElse(Vec3i.ZERO);

        Settings settings = state.settings();
        String[] parts = reach.split(AXIS_SEPARATOR);
        if (parts.length >= 3) {
            return new SignArea(
                    radius(parts[0], settings, fallbackRadius),
                    radius(parts[1], settings, fallbackRadius),
                    radius(parts[2], settings, fallbackRadius),
                    offset);
        }

        int uniform = radius(reach, settings, fallbackRadius);
        return new SignArea(uniform, uniform, uniform, offset);
    }

    /** Whether a line would read as an area, which is what the sign's own form asks. */
    public static boolean isReadable(String written) {
        int separator = written.indexOf(CENTRE_SEPARATOR);
        String reach = (separator < 0 ? written : written.substring(0, separator)).trim();

        if (separator >= 0 && parseOffset(written.substring(separator + 1)).isEmpty()) {
            return false;
        }
        if (reach.isEmpty()) {
            return true;
        }

        String[] parts = reach.split(AXIS_SEPARATOR);
        if (parts.length != 1 && parts.length != 3) {
            return false;
        }
        for (String part : parts) {
            if (isNotAWholeNumber(part)) {
                return false;
            }
        }
        return true;
    }

    /** The box this area covers, given the block the sign hangs on. */
    public Bounds around(Vec3i backPosition) {
        Vec3i centre = backPosition.add(centreOffset);
        return new Bounds(
                new Vec3i(centre.x() - radiusX, centre.y() - radiusY, centre.z() - radiusZ),
                new Vec3i(centre.x() + radiusX, centre.y() + radiusY, centre.z() + radiusZ));
    }

    /** Where the middle of the box sits, given the block the sign hangs on. */
    public Vec3i centreFrom(Vec3i backPosition) {
        return backPosition.add(centreOffset);
    }

    /** A reach off a sign, held to what the settings allow a chip to cover. */
    private static int radius(String written, Settings settings, int fallback) {
        String trimmed = written.trim();
        if (trimmed.isEmpty()) {
            return Math.min(fallback, settings.maxRadius());
        }
        try {
            return Math.clamp(Integer.parseInt(trimmed), 0, settings.maxRadius());
        } catch (NumberFormatException e) {
            return Math.min(fallback, settings.maxRadius());
        }
    }

    private static Optional<Vec3i> parseOffset(String written) {
        String[] parts = written.split(OFFSET_SEPARATOR);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Vec3i(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean isNotAWholeNumber(String written) {
        try {
            Integer.parseInt(written.trim());
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
