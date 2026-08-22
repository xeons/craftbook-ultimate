// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.xeonproductions.craftbookultimate.core.command.Caller;
import com.xeonproductions.craftbookultimate.core.command.Standing;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** Whoever typed a Bukkit command, as the thing carrying it out sees them. */
@NullMarked
public record SenderCaller(CommandSender sender) implements Caller {

    @Override
    public void send(Component message) {
        sender.sendMessage(message);
    }

    @Override
    public boolean may(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public String name() {
        return sender.getName();
    }

    /**
     * Where they are, which only a player is anywhere.
     *
     * <p>The console and a command block are standing nowhere as far as this is concerned, so
     * nothing that sorts by distance has anywhere to sort from and leaves the order alone.
     */
    @Override
    public Optional<Standing> standing() {
        if (!(sender instanceof Player player)) {
            return Optional.empty();
        }
        return Optional.of(new Standing(
                player.getWorld().getUID(), Positions.toDomain(player.getLocation())));
    }
}
