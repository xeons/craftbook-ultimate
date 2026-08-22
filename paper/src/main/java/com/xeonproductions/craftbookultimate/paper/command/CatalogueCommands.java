// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xeonproductions.craftbookultimate.core.command.CatalogueActions;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jspecify.annotations.NullMarked;

/**
 * The grammar of the commands for reading the chip catalogue.
 *
 * <p>What each one says is in {@link CatalogueActions}, which knows nothing about a server. What is
 * here is the shape a command may be typed in.
 */
@NullMarked
public final class CatalogueCommands {

    private final CatalogueActions actions;

    public CatalogueCommands(ICRegistry registry) {
        this.actions = new CatalogueActions(registry);
    }

    /** The plugin's own command, under which everything not tied to one chip lives. */
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("craftbook")
                .requires(source -> source.getSender().hasPermission(CatalogueActions.COMMAND))
                .executes(context -> Reply.done(actions.summary(Reply.caller(context))))
                .then(Commands.literal("ic")
                        .then(Commands.literal("list")
                                .executes(context -> Reply.done(actions.list(Reply.caller(context), 1)))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> Reply.done(actions.list(
                                                Reply.caller(context),
                                                IntegerArgumentType.getInteger(context, "page"))))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("chip", StringArgumentType.word())
                                        .suggests(models())
                                        .executes(context -> Reply.done(actions.info(
                                                Reply.caller(context),
                                                StringArgumentType.getString(context, "chip")))))));
    }

    /** Suggests the model numbers and shorthands the catalogue holds. */
    private SuggestionProvider<CommandSourceStack> models() {
        return (context, builder) -> {
            actions.models(builder.getRemaining()).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

}
