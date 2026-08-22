// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.adapter;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.util.Direction;

/**
 * Which way a block faces, said in both vocabularies.
 *
 * <p>Sponge names its diagonals as one word and its nowhere as {@code NONE}, so the two sets are
 * paired by hand rather than by matching names. Sponge's sixteen-point directions have no domain
 * counterpart and are deliberately absent: a chip's geometry is built out of the six faces and the
 * four corners, and answering a secondary ordinal with a guess would put pins in the wrong place.
 */
@NullMarked
public final class Directions {

    private static final Map<Direction, BlockFace> TO_DOMAIN = new EnumMap<>(Direction.class);

    private static final Map<BlockFace, Direction> TO_SERVER = new EnumMap<>(BlockFace.class);

    static {
        link(Direction.NORTH, BlockFace.NORTH);
        link(Direction.EAST, BlockFace.EAST);
        link(Direction.SOUTH, BlockFace.SOUTH);
        link(Direction.WEST, BlockFace.WEST);
        link(Direction.UP, BlockFace.UP);
        link(Direction.DOWN, BlockFace.DOWN);
        link(Direction.NORTHEAST, BlockFace.NORTH_EAST);
        link(Direction.SOUTHEAST, BlockFace.SOUTH_EAST);
        link(Direction.SOUTHWEST, BlockFace.SOUTH_WEST);
        link(Direction.NORTHWEST, BlockFace.NORTH_WEST);
        link(Direction.NONE, BlockFace.SELF);
    }

    private Directions() {}

    private static void link(Direction server, BlockFace domain) {
        TO_DOMAIN.put(server, domain);
        TO_SERVER.put(domain, server);
    }

    public static Optional<BlockFace> toDomain(Direction face) {
        return Optional.ofNullable(TO_DOMAIN.get(face));
    }

    public static Direction toServer(BlockFace face) {
        Direction server = TO_SERVER.get(face);
        if (server == null) {
            throw new IllegalStateException("No server direction for " + face);
        }
        return server;
    }

    /** Which way somebody put down at a block would be looking to face it. */
    public static double yawOf(BlockFace face) {
        if (face.deltaX() == 0 && face.deltaZ() == 0) {
            return 0d;
        }
        double angle = Math.atan2(-face.deltaX(), face.deltaZ());
        return Math.toDegrees((angle + 2 * Math.PI) % (2 * Math.PI));
    }
}
