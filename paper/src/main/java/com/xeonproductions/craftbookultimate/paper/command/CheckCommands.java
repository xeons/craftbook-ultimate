// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.command.BrokenChip;
import com.xeonproductions.craftbookultimate.core.command.CheckActions;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.ic.ICInstance;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.block.Sign;
import org.jspecify.annotations.NullMarked;

/**
 * The grammar of the command that says which loaded chips cannot work.
 *
 * <p>What the answer looks like is in {@link CheckActions}. What is here is reading the signs of
 * the loaded chips, which is the one part that needs a world.
 */
@NullMarked
public final class CheckCommands {

    /** The permission to ask. */
    public static final String CHECK = CheckActions.CHECK;

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
        return Reply.done(CheckActions.report(
                Reply.caller(context), brokenChips(), manager.loadedCount()));
    }

    /**
     * Every loaded chip whose sign leaves out a line it cannot work without.
     *
     * <p>A chip whose sign has gone — broken between loading and being asked about — is skipped
     * rather than reported, since there is nothing left to go and fix.
     */
    private List<BrokenChip> brokenChips() {
        List<BrokenChip> broken = new ArrayList<>();
        for (ICInstance chip : manager.loaded()) {
            Optional<Sign> sign = Signs.at(Positions.toBlock(chip.world(), chip.signPosition()));
            if (sign.isEmpty()) {
                continue;
            }

            SignLines lines = Signs.read(sign.get());
            LineReview review = LineReview.of(chip.definition(), lines);
            if (review.broken()) {
                broken.add(new BrokenChip(
                        chip.definition(),
                        chip.world().getUID(),
                        chip.world().getName(),
                        chip.signPosition(),
                        review));
            }
        }
        return broken;
    }
}
