// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NullMarked;

/** The two things every command here does to get between Brigadier and what the command means. */
@NullMarked
final class Reply {

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    /** What it takes as a command having done nothing. */
    private static final int NOTHING = 0;

    private Reply() {
    }

    /** Whoever typed it. */
    static SenderCaller caller(CommandContext<CommandSourceStack> context) {
        return new SenderCaller(context.getSource().getSender());
    }

    /** What Brigadier is told about how it went. */
    static int done(boolean acted) {
        return acted ? SUCCESS : NOTHING;
    }
}
