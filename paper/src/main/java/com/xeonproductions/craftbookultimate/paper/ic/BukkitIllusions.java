// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.illusion.Illusions;
import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Showing people something other than what is there, on a real server.
 *
 * <p>Nothing here reads or writes a block. A weather override is state the server keeps against
 * the player and sends down their own connection, so a chip may put one up from whichever region's
 * thread it happens to be running on, for somebody standing anywhere at all.
 */
@NullMarked
public record BukkitIllusions(Server server) implements Illusions {

    /** The permission node a player's group membership is read from. */
    private static final String GROUP_PREFIX = "group.";

    @Override
    public boolean showSkyToNamed(String nameFragment, Sky sky) {
        for (Player player : server.getOnlinePlayers()) {
            if (player.getName().contains(nameFragment)) {
                PlayerSkies.show(player, sky);
                return true;
            }
        }
        return false;
    }

    @Override
    public int showSkyToGroup(String group, Sky sky) {
        int count = 0;
        for (Player player : server.getOnlinePlayers()) {
            if (player.hasPermission(GROUP_PREFIX + group)) {
                PlayerSkies.show(player, sky);
                count++;
            }
        }
        return count;
    }

    @Override
    public int showSkyIn(UUID world, Sky sky) {
        int count = 0;
        for (Player player : server.getOnlinePlayers()) {
            if (player.getWorld().getUID().equals(world)) {
                PlayerSkies.show(player, sky);
                count++;
            }
        }
        return count;
    }
}
