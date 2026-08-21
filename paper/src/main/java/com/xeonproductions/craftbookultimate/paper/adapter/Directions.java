// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.adapter;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Converts between the server's directions and the platform-independent ones the domain uses.
 *
 * <p>The server's enum carries sixteen compass points for entity facing, while chip and mechanic
 * geometry only ever deals with the six axes and the four diagonals. Anything outside that set
 * has no equivalent and is reported as absent rather than guessed at.
 */
@NullMarked
public final class Directions {

    private static final Map<org.bukkit.block.BlockFace, BlockFace> TO_DOMAIN =
            new EnumMap<>(org.bukkit.block.BlockFace.class);
    private static final Map<BlockFace, org.bukkit.block.BlockFace> TO_SERVER =
            new EnumMap<>(BlockFace.class);

    static {
        link(org.bukkit.block.BlockFace.NORTH, BlockFace.NORTH);
        link(org.bukkit.block.BlockFace.EAST, BlockFace.EAST);
        link(org.bukkit.block.BlockFace.SOUTH, BlockFace.SOUTH);
        link(org.bukkit.block.BlockFace.WEST, BlockFace.WEST);
        link(org.bukkit.block.BlockFace.UP, BlockFace.UP);
        link(org.bukkit.block.BlockFace.DOWN, BlockFace.DOWN);
        link(org.bukkit.block.BlockFace.NORTH_EAST, BlockFace.NORTH_EAST);
        link(org.bukkit.block.BlockFace.SOUTH_EAST, BlockFace.SOUTH_EAST);
        link(org.bukkit.block.BlockFace.SOUTH_WEST, BlockFace.SOUTH_WEST);
        link(org.bukkit.block.BlockFace.NORTH_WEST, BlockFace.NORTH_WEST);
        link(org.bukkit.block.BlockFace.SELF, BlockFace.SELF);
    }

    private Directions() {}

    private static void link(org.bukkit.block.BlockFace server, BlockFace domain) {
        TO_DOMAIN.put(server, domain);
        TO_SERVER.put(domain, server);
    }

    /**
     * Converts a server direction.
     *
     * @return the equivalent domain direction, or empty for the intermediate compass points
     */
    public static Optional<BlockFace> toDomain(org.bukkit.block.BlockFace face) {
        return Optional.ofNullable(TO_DOMAIN.get(face));
    }

    /**
     * The yaw an entity looking along a direction has.
     *
     * <p>Minecraft measures yaw from south and turns towards west, so south is zero, west is
     * ninety, and so on. Anything without a horizontal component leaves an entity looking south,
     * since yaw alone cannot express looking straight up or down.
     */
    public static float yawOf(BlockFace face) {
        double angle = Math.atan2(-face.deltaX(), face.deltaZ());
        if (face.deltaX() == 0 && face.deltaZ() == 0) {
            return 0f;
        }
        return (float) Math.toDegrees((angle + 2 * Math.PI) % (2 * Math.PI));
    }

    /** Converts a domain direction, which always has a server equivalent. */
    public static org.bukkit.block.BlockFace toServer(BlockFace face) {
        org.bukkit.block.BlockFace server = TO_SERVER.get(face);
        if (server == null) {
            throw new IllegalStateException("No server direction for " + face);
        }
        return server;
    }
}
