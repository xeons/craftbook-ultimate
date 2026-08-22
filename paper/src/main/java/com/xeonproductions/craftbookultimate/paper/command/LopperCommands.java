// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.lopper.TreeLoppers;
import com.xeonproductions.craftbookultimate.core.lopper.VeinMiners;
import com.xeonproductions.craftbookultimate.paper.lopper.Lopping;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Turning the tree lopper and the vein miner off for oneself.
 *
 * <p>{@code /treelopper} and {@code /veinminer}, each with a {@code toggle}. Both mechanics change
 * what an ordinary swing of an ordinary tool does, so somebody building out of logs or digging out
 * a room in an ore-veined wall needs a way to stop them without an operator's help.
 *
 * <p>A preference rather than a permission, kept in the player's own data. The fork kept a list of
 * player identifiers in its settings file, which grew a line every time anybody changed their
 * mind, and which is not a thing an operator should have to look after.
 *
 * <p>{@code /timber} is an alias the fork carried and it is kept, since a builder who has typed it
 * for years should go on being able to.
 */
@NullMarked
public final class LopperCommands {

    private final Lopping lopping;

    public LopperCommands(Lopping lopping) {
        this.lopping = lopping;
    }

    /** The {@code /treelopper} command. */
    public LiteralArgumentBuilder<CommandSourceStack> treeLopper() {
        return command("treelopper", TreeLoppers.TOGGLE, lopping::fellsTrees, lopping::fellTrees,
                "Felling whole trees");
    }

    /** The {@code /veinminer} command. */
    public LiteralArgumentBuilder<CommandSourceStack> veinMiner() {
        return command("veinminer", VeinMiners.TOGGLE, lopping::minesSeams, lopping::mineSeams,
                "Mining whole seams");
    }

    /** One of the two commands, which differ only in what they are called and what they set. */
    private LiteralArgumentBuilder<CommandSourceStack> command(
            String name,
            String permission,
            Predicate<Player> reading,
            BiPredicate<Player, Boolean> writing,
            String what) {
        return Commands.literal(name)
                .requires(source -> source.getSender().hasPermission(permission))
                .then(Commands.literal("toggle")
                        .executes(context -> toggle(context, reading, writing, what)));
    }

    private static int toggle(
            CommandContext<CommandSourceStack> context,
            Predicate<Player> reading,
            BiPredicate<Player, Boolean> writing,
            String what) {
        Optional<Player> player = player(context);
        if (player.isEmpty()) {
            return 0;
        }

        boolean now = writing.test(player.get(), !reading.test(player.get()));
        player.get().sendMessage(Component.text(
                what + " is now " + (now ? "on" : "off") + " for you.",
                NamedTextColor.YELLOW));
        return 1;
    }

    private static Optional<Player> player(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof Player player) {
            return Optional.of(player);
        }
        context.getSource().getSender().sendMessage(Component.text(
                "Only somebody holding a tool has this to turn off.", NamedTextColor.RED));
        return Optional.empty();
    }
}
