// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.entity.Roster;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

/**
 * Who is online, as a chip that senses people sees it.
 *
 * <p>Anybody hidden is left out, because a chip that reported them would be a way of finding
 * somebody who has arranged not to be found.
 */
@NullMarked
public record SpongeRoster(Server server) implements Roster {

    @Override
    public List<String> visibleNames() {
        List<String> names = new ArrayList<>();
        for (ServerPlayer player : server.onlinePlayers()) {
            if (player.get(Keys.VANISH_STATE).map(VanishState::invisible).orElse(false)) {
                continue;
            }
            names.add(player.name());
        }
        return names;
    }
}
