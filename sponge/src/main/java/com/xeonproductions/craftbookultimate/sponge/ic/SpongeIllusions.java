// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.illusion.Illusions;
import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.sponge.game.GameInternals;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

/**
 * Showing people weather the world is not having.
 *
 * <p>SpongeAPI has no per-player weather, so this goes through {@link GameInternals} to the game
 * itself. Where that will not answer, nobody is shown anything and the count says so rather than
 * claiming a display nobody saw.
 */
@NullMarked
public record SpongeIllusions(Server server) implements Illusions {

    private static final String GROUP_PREFIX = "group.";

    @Override
    public boolean showSkyToNamed(String nameFragment, Sky sky) {
        for (ServerPlayer player : server.onlinePlayers()) {
            if (player.name().contains(nameFragment)) {
                return GameInternals.get().showSky(player, sky);
            }
        }
        return false;
    }

    @Override
    public int showSkyToGroup(String group, Sky sky) {
        int count = 0;
        for (ServerPlayer player : server.onlinePlayers()) {
            if (player.hasPermission(GROUP_PREFIX + group) && GameInternals.get().showSky(player, sky)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int showSkyIn(UUID world, Sky sky) {
        int count = 0;
        for (ServerPlayer player : server.onlinePlayers()) {
            if (player.world().uniqueId().equals(world) && GameInternals.get().showSky(player, sky)) {
                count++;
            }
        }
        return count;
    }
}
