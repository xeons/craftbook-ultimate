// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.copier.SignClipboard;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Changing a line of the sign somebody has copied, before they paste it.
 *
 * <p>The point of it is signs you cannot reach. A builder copies a sign, edits a line by command
 * and pastes it back, which is the only way to put text on a sign that is already in the world
 * without breaking it and writing a new one.
 *
 * <p>Lines are counted from one, as a builder counts them, not from zero as the code does.
 */
@NullMarked
public final class SignCommands {

    /** The permission to edit what has been copied. */
    public static final String EDIT = "craftbook.signcopier.edit";

    private final SignClipboard clipboard;

    public SignCommands(SignClipboard clipboard) {
        this.clipboard = clipboard;
    }

    /** The whole {@code /sign} command. */
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("sign")
                .then(Commands.literal("edit")
                        .requires(source -> source.getSender().hasPermission(EDIT))
                        .then(Commands.argument("line",
                                        IntegerArgumentType.integer(1, SignClipboard.LINES))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(context -> edit(
                                                context, StringArgumentType.getString(
                                                        context, "text")))))
                        .then(Commands.argument("blank",
                                        IntegerArgumentType.integer(1, SignClipboard.LINES))
                                .executes(context -> edit(context, ""))));
    }

    private int edit(CommandContext<CommandSourceStack> context, String text) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            context.getSource().getSender().sendMessage(Component.text(
                    "Only somebody holding a copied sign can edit one.", NamedTextColor.RED));
            return 0;
        }

        int line = IntegerArgumentType.getInteger(context, lineArgument(context));
        if (!clipboard.edit(player.getUniqueId(), line, text)) {
            player.sendMessage(Component.text(
                    "You have not copied a sign yet. Left-click one with black dye first.",
                    NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(Component.text(
                "Line " + line + " is now " + (text.isEmpty() ? "blank" : text) + ".",
                NamedTextColor.GREEN));
        return 1;
    }

    /** Which argument carried the line number, since blanking one takes a different branch. */
    private static String lineArgument(CommandContext<CommandSourceStack> context) {
        return context.getNodes().stream()
                .map(node -> node.getNode().getName())
                .anyMatch("blank"::equals) ? "blank" : "line";
    }
}
