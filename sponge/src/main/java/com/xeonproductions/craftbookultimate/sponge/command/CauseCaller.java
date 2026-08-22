// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.command;

import com.xeonproductions.craftbookultimate.core.command.Caller;
import com.xeonproductions.craftbookultimate.core.command.Standing;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

/** Whoever set off a Sponge command, as the thing carrying it out sees them. */
@NullMarked
public record CauseCaller(CommandCause cause) implements Caller {

    @Override
    public void send(Component message) {
        cause.audience().sendMessage(message);
    }

    @Override
    public boolean may(String permission) {
        return cause.subject().hasPermission(permission);
    }

    /**
     * What they are called, which is the namespace their own variables live in.
     *
     * <p>A player's name rather than the subject's identifier, because a variable namespace is
     * written on a sign by somebody reading it off a player list. Anything that is not a player —
     * the console, a command block — falls back to the subject identifier, which is what it is
     * known by and is not a name anybody would write.
     */
    @Override
    public String name() {
        return cause.first(ServerPlayer.class)
                .map(player -> player.name())
                .orElseGet(() -> cause.subject().friendlyIdentifier()
                        .orElseGet(() -> cause.subject().identifier()));
    }

    /**
     * Where they are, which only something in a world is.
     *
     * <p>A cause carries a location for a command block and a command run at a position as well as
     * for a player, so this answers for all of them and nothing sorts by distance from a console.
     */
    @Override
    public Optional<Standing> standing() {
        return cause.location().map(CauseCaller::standingAt);
    }

    private static Standing standingAt(ServerLocation location) {
        ServerWorld world = location.world();
        return new Standing(world.uniqueId(), Positions.toDomain(location));
    }
}
