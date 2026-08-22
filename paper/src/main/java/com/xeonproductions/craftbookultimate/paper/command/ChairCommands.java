// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.chair.Chairs;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.paper.chair.Seats;
import com.xeonproductions.craftbookultimate.paper.chair.Sitting;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Sitting down and standing up by command.
 *
 * <p>Three of them, and only the first does anything a chair could not. {@code /sit} seats
 * somebody wherever they are standing, on any block at all, which is what makes it useful on a
 * carpet or a step the settings do not list.
 *
 * <p>{@code /sittoggle} is a preference rather than a permission. The fork wrote a permission onto
 * the player through Sponge's permission service, which Bukkit has no equivalent of and which put
 * a player's own choice into an operator's permission tree; this keeps it in the player's own data
 * instead, where it belongs and where the game persists it for us.
 */
@NullMarked
public final class ChairCommands {

    /** Turning clicking to sit off for oneself. */
    public static final String TOGGLE = "craftbook.chairs.toggle";

    private final Configuration configuration;
    private final Sitting sitting;
    private final Seats seats;

    public ChairCommands(Configuration configuration, Sitting sitting, Seats seats) {
        this.configuration = configuration;
        this.sitting = sitting;
        this.seats = seats;
    }

    /** The {@code /sit} command. */
    public LiteralArgumentBuilder<CommandSourceStack> sit() {
        return Commands.literal("sit")
                .requires(source -> source.getSender().hasPermission(Chairs.COMMAND_USE))
                .executes(this::sitDown);
    }

    /** The {@code /stand} command. */
    public LiteralArgumentBuilder<CommandSourceStack> stand() {
        return Commands.literal("stand")
                .requires(source -> source.getSender().hasPermission(Chairs.COMMAND_USE))
                .executes(this::standUp);
    }

    /** The {@code /sittoggle} command. */
    public LiteralArgumentBuilder<CommandSourceStack> toggle() {
        return Commands.literal("sittoggle")
                .requires(source -> source.getSender().hasPermission(TOGGLE))
                .executes(this::toggleClicking);
    }

    private int sitDown(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = sitter(context);
        if (player.isEmpty() || !running(player.get())) {
            return 0;
        }
        return sitting.sitWhereTheyStand(player.get()) ? 1 : 0;
    }

    private int standUp(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = sitter(context);
        if (player.isEmpty()) {
            return 0;
        }

        Optional<Entity> seat = seats.under(player.get());
        if (seat.isEmpty()) {
            player.get().sendMessage(Component.text(
                    "You are not sitting on anything.", NamedTextColor.RED));
            return 0;
        }

        // Clearing the seat dismounts whoever is on it, which raises the same event a player
        // pressing the key does, so standing up by command and standing up by hand end the same
        // way and neither needs to know about the other.
        seats.clear(seat.get());
        return 1;
    }

    private int toggleClicking(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = sitter(context);
        if (player.isEmpty()) {
            return 0;
        }

        boolean clicking = sitting.toggleClicking(player.get());
        player.get().sendMessage(clicking
                ? Component.text("Clicking a chair will sit you down again.", NamedTextColor.GREEN)
                : Component.text("Clicking a chair will no longer sit you down. "
                        + "Use the command again to turn it back on.", NamedTextColor.GREEN));
        return 1;
    }

    /** Whoever ran the command, if a person ran it at all. */
    private static Optional<Player> sitter(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof Player player) {
            return Optional.of(player);
        }
        context.getSource().getSender().sendMessage(Component.text(
                "Only somebody standing in the world can sit down.", NamedTextColor.RED));
        return Optional.empty();
    }

    /** Whether the chairs run where somebody is standing. */
    private boolean running(Player player) {
        if (configuration.settings()
                .runsMechanicIn(Chairs.NAME, player.getWorld().getName())) {
            return true;
        }
        player.sendMessage(Component.text(
                "Chairs are switched off here.", NamedTextColor.RED));
        return false;
    }
}
