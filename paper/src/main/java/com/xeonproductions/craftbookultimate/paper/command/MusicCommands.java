// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.xeonproductions.craftbookultimate.core.command.MusicActions;
import com.xeonproductions.craftbookultimate.core.music.Songs;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jspecify.annotations.NullMarked;

/**
 * The grammar of the commands that say what music the server has.
 *
 * <p>What each one says is in {@link MusicActions}.
 */
@NullMarked
public final class MusicCommands {

    private final MusicActions actions;

    public MusicCommands(Songs songs) {
        this.actions = new MusicActions(songs);
    }

    /** The {@code music} branch of the plugin's own command. */
    public LiteralArgumentBuilder<CommandSourceStack> musicCommand() {
        return Commands.literal("music")
                .requires(source -> source.getSender().hasPermission(MusicActions.LIST))
                .then(Commands.literal("songs").executes(context ->
                        Reply.done(actions.songs(Reply.caller(context)))))
                .then(Commands.literal("playlists").executes(context ->
                        Reply.done(
                                actions.playlists(Reply.caller(context)))));
    }
}
