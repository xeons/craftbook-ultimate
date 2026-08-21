// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.ic.ICInstance;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The command that says which loaded chips cannot work.
 *
 * <p>The signs of broken chips are already marked red as they load, but that only helps somebody
 * standing in front of one. An operator asking what is wrong across a server needs the list, and
 * needs it without walking the map.
 *
 * <p>Nothing here writes a block. Every answer comes from sign text that is already in memory,
 * which is what makes this safe to run on a busy server and what makes it the right tool for the
 * question rather than a sweep that repaints as it goes.
 */
@NullMarked
public final class CheckCommands {

    /** The permission to ask. */
    public static final String CHECK = "craftbook.check";

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    /** How many broken chips are listed before the reply gives up and counts the rest. */
    private static final int LISTED = 12;

    private final ICManager manager;

    public CheckCommands(ICManager manager) {
        this.manager = manager;
    }

    /** The whole {@code /craftbook check} command. */
    public LiteralArgumentBuilder<CommandSourceStack> checkCommand() {
        return Commands.literal("check")
                .requires(source -> source.getSender().hasPermission(CHECK))
                .executes(this::check);
    }

    private int check(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        List<Broken> broken = brokenChips();
        int loaded = manager.loadedCount();

        if (broken.isEmpty()) {
            sender.sendMessage(Component.text(
                    "All " + loaded + " loaded chips have what they need.", NamedTextColor.GREEN));
            return SUCCESS;
        }

        if (sender instanceof Player player) {
            Vec3i here = Positions.toDomain(player.getLocation());
            broken.sort(Comparator.comparingLong(chip -> chip.distanceFrom(player.getWorld().getUID(), here)));
        }

        sender.sendMessage(Component.text(
                broken.size() + " of " + loaded + " loaded chips cannot work as written. "
                        + "Their signs are titled in red.",
                NamedTextColor.YELLOW));

        for (Broken chip : broken.subList(0, Math.min(LISTED, broken.size()))) {
            sender.sendMessage(Component.text("  " + chip.where(), NamedTextColor.AQUA)
                    .append(Component.text("  " + chip.model(), NamedTextColor.WHITE)));
            for (LineReview.Blank blank : chip.review().missing()) {
                sender.sendMessage(Component.text("    " + blank.said(), NamedTextColor.GRAY));
            }
        }

        if (broken.size() > LISTED) {
            sender.sendMessage(Component.text(
                    "  and " + (broken.size() - LISTED) + " more.", NamedTextColor.YELLOW));
        }
        return SUCCESS;
    }

    /**
     * Every loaded chip whose sign leaves out a line it cannot work without.
     *
     * <p>A chip whose sign has gone — broken between loading and being asked about — is skipped
     * rather than reported, since there is nothing left to go and fix.
     */
    private List<Broken> brokenChips() {
        List<Broken> broken = new ArrayList<>();
        for (ICInstance chip : manager.loaded()) {
            Optional<Sign> sign = Signs.at(Positions.toBlock(chip.world(), chip.signPosition()));
            if (sign.isEmpty()) {
                continue;
            }

            SignLines lines = Signs.read(sign.get());
            LineReview review = LineReview.of(chip.definition(), lines);
            if (review.broken()) {
                broken.add(new Broken(chip, review));
            }
        }
        return broken;
    }

    /** One chip that cannot work, and why. */
    private record Broken(ICInstance chip, LineReview review) {

        String model() {
            return chip.definition().model() + "  " + chip.definition().name();
        }

        String where() {
            Vec3i at = chip.signPosition();
            return at.x() + ", " + at.y() + ", " + at.z() + " in " + chip.world().getName();
        }

        /**
         * How far this chip is from somewhere, for putting the nearest first.
         *
         * <p>Squared, so no root is taken, and a chip in another world sorts after every chip in
         * this one however far away it is.
         */
        long distanceFrom(java.util.UUID world, Vec3i from) {
            if (!chip.world().getUID().equals(world)) {
                return Long.MAX_VALUE;
            }
            Vec3i at = chip.signPosition();
            long dx = (long) at.x() - from.x();
            long dy = (long) at.y() - from.y();
            long dz = (long) at.z() - from.z();
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
