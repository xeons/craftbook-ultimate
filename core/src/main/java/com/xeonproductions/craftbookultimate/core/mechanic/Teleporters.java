// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The sign that sends somebody somewhere else in the same world.
 *
 * <p>Coordinates on the third line, written {@code x:y:z}. A sign reading {@code ARRIVAL} instead
 * is somewhere to land rather than somewhere to leave from, which is how a pair is built: one sign
 * naming the other's place, and the other saying only that people arrive there.
 *
 * <p>A blank third line becomes {@code ARRIVAL} as the sign is written, so the simplest thing
 * somebody can build is a destination.
 */
@NullMarked
public final class Teleporters {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.TELEPORTER;

    /** The sign that makes one. */
    public static final String SIGN_NAME = "[Teleporter]";

    /** The permission to build one. */
    public static final String BUILD = "craftbook.teleporter";

    /** The permission to use one. */
    public static final String USE = "craftbook.teleporter.use";

    /** The line the destination is written on. */
    public static final int DESTINATION_LINE = 2;

    /** What a sign says when it is somewhere to arrive rather than somewhere to leave from. */
    public static final String ARRIVAL = "ARRIVAL";

    private Teleporters() {
    }

    /** Whether a sign is a place to land rather than a place to leave from. */
    public static boolean isArrival(String line) {
        return line.trim().equalsIgnoreCase(ARRIVAL);
    }

    /**
     * Where a sign sends somebody, or nothing where its line is not a place.
     *
     * <p>Held to whole-number-friendly reading rather than block positions, because the fork wrote
     * doubles and a builder who put half a block on a coordinate meant it.
     */
    public static Optional<Vec3d> destination(String line) {
        String written = line.trim();
        if (written.isEmpty() || isArrival(written)) {
            return Optional.empty();
        }

        String[] parts = written.split(":");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Vec3d(
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * What the third line should be kept as, once a builder has written it.
     *
     * <p>Blank becomes {@code ARRIVAL}, a spelling of arrival becomes the one spelling, and a set
     * of coordinates is kept as it was written so a builder's own numbers are still readable to
     * them.
     */
    public static String settled(String line) {
        String written = line.trim();
        if (written.isEmpty() || isArrival(written)) {
            return ARRIVAL;
        }
        return written;
    }

    /**
     * Whether somewhere is near enough to be teleported to.
     *
     * @param limit how far a teleporter may reach, or a negative number for no limit
     */
    public static boolean withinRange(Vec3d from, Vec3d to, double limit) {
        if (limit < 0) {
            return true;
        }
        return from.distanceSquared(to) <= limit * limit;
    }
}
