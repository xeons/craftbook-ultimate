// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.area;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * Where a saved area came from, and where it goes back.
 *
 * <p>The game's own structure format records what a building is made of and not where it stood,
 * because a structure is meant to be placed anywhere. A toggled area is the opposite: it belongs
 * in exactly one place and going back to any other would be a bug. So the place is kept beside
 * the structure, in a file somebody can read and correct.
 *
 * @param world the world the area was saved from
 * @param origin its lowest corner
 * @param size how far it runs along each axis, at least one in each
 */
@NullMarked
public record AreaAnchor(UUID world, Vec3i origin, Vec3i size) {

    private static final String WORLD = "world";
    private static final String ORIGIN = "origin";
    private static final String SIZE = "size";

    /** Refuses a size that encloses nothing. */
    public AreaAnchor {
        if (size.x() < 1 || size.y() < 1 || size.z() < 1) {
            throw new IllegalArgumentException("An area is at least one block in each direction");
        }
    }

    /** An anchor covering the two corners somebody picked out, whichever way round they are. */
    public static AreaAnchor between(UUID world, Vec3i one, Vec3i other) {
        Vec3i low = new Vec3i(
                Math.min(one.x(), other.x()),
                Math.min(one.y(), other.y()),
                Math.min(one.z(), other.z()));
        Vec3i high = new Vec3i(
                Math.max(one.x(), other.x()),
                Math.max(one.y(), other.y()),
                Math.max(one.z(), other.z()));
        return new AreaAnchor(world, low, high.subtract(low).add(1, 1, 1));
    }

    /** How many blocks the area holds. */
    public int volume() {
        return size.x() * size.y() * size.z();
    }

    /** The corner opposite the origin, which is the last block the area covers. */
    public Vec3i far() {
        return origin.add(size.x() - 1, size.y() - 1, size.z() - 1);
    }

    /** The anchor as lines of a file. */
    public List<String> save() {
        return List.of(
                WORLD + " " + world,
                ORIGIN + " " + origin.x() + " " + origin.y() + " " + origin.z(),
                SIZE + " " + size.x() + " " + size.y() + " " + size.z());
    }

    /**
     * Reads an anchor back.
     *
     * @param lines the file's lines, with its notes and blank lines already taken out
     * @return the anchor, or empty if the file does not say all three things readably
     */
    public static Optional<AreaAnchor> read(List<String> lines) {
        UUID world = null;
        Vec3i origin = null;
        Vec3i size = null;

        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            try {
                switch (parts[0]) {
                    case WORLD -> {
                        if (parts.length == 2) {
                            world = UUID.fromString(parts[1]);
                        }
                    }
                    case ORIGIN -> {
                        if (parts.length == 4) {
                            origin = read(parts);
                        }
                    }
                    case SIZE -> {
                        if (parts.length == 4) {
                            size = read(parts);
                        }
                    }
                    default -> { }
                }
            } catch (IllegalArgumentException e) {
                // A line that does not read is left out, and the anchor is refused below for
                // want of whatever it was meant to say.
            }
        }

        if (world == null || origin == null || size == null
                || size.x() < 1 || size.y() < 1 || size.z() < 1) {
            return Optional.empty();
        }
        return Optional.of(new AreaAnchor(world, origin, size));
    }

    private static Vec3i read(String[] parts) {
        return new Vec3i(
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}
