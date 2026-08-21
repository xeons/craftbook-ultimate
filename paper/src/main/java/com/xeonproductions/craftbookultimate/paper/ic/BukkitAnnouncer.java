// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.message.Announcer;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * How a chip on a real server speaks to the server rather than to a place.
 *
 * <p>Nothing here reads or writes a block, so a chip may use it from whichever region's thread it
 * happens to be running on. Who is online is a server-wide list rather than one region's, and
 * sending somebody a message puts it on their connection rather than touching where they stand.
 */
@NullMarked
public record BukkitAnnouncer(Server server, Logger logger) implements Announcer {

    @Override
    public void toEveryone(Component message) {
        for (Player player : server.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    @Override
    public boolean toNamed(String name, Component message) {
        Player player = server.getPlayerExact(name);
        if (player == null) {
            return false;
        }
        player.sendMessage(message);
        return true;
    }

    @Override
    public void toLog(String line) {
        logger.info(line);
    }
}
