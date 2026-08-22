// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.message.Announcer;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

/**
 * How a chip addresses the server rather than a place.
 *
 * <p>Only a name and a piece of text ever cross, and both are values, so this is safe to call from
 * wherever a chip happens to be running.
 */
@NullMarked
public record SpongeAnnouncer(Server server, Logger logger) implements Announcer {

    @Override
    public void toEveryone(Component message) {
        for (ServerPlayer player : server.onlinePlayers()) {
            player.sendMessage(message);
        }
    }

    @Override
    public boolean toNamed(String name, Component message) {
        return server.player(name)
                .map(player -> {
                    player.sendMessage(message);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public void toLog(String line) {
        logger.info(line);
    }
}
