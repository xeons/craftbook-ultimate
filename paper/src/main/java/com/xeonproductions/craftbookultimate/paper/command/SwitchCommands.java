// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xeonproductions.craftbookultimate.core.command.SwitchActions;
import com.xeonproductions.craftbookultimate.core.control.PasswordStore;
import com.xeonproductions.craftbookultimate.core.control.Switchboard;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * The grammar of the commands that throw the switches the command controlled chips follow.
 *
 * <p>What each one does is in {@link SwitchActions}. The command names are frozen along with the
 * model numbers they belong to: {@code /mcx120} throws a switch anybody may throw, {@code /mcx121}
 * throws one that takes a password, and {@code /mcx121pass} is where a password is set or changed.
 */
@NullMarked
public final class SwitchCommands {

    private final SwitchActions actions;

    /**
     * @param open the switches anyone may throw
     * @param guarded the switches that take a password
     * @param passwords the passwords guarding those switches
     * @param offThread runs work away from the thread that ticks the world
     * @param savePasswords writes the passwords out after one has changed
     * @param saveSwitches writes the switch positions out after one has been thrown
     */
    public SwitchCommands(
            Switchboard open,
            Switchboard guarded,
            PasswordStore passwords,
            Consumer<Runnable> offThread,
            Runnable savePasswords,
            Runnable saveSwitches) {
        this.actions = new SwitchActions(
                open, guarded, passwords, offThread, savePasswords, saveSwitches);
    }

    /** The command that throws an unguarded switch. */
    public LiteralArgumentBuilder<CommandSourceStack> openSwitchCommand() {
        return Commands.literal("mcx120")
                .then(Commands.argument("switch", StringArgumentType.string())
                        .suggests(suggesting(actions::openNames))
                        .executes(context -> Reply.done(actions.toggleOpen(
                                Reply.caller(context), switchName(context), Optional.empty())))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(modes())
                                .executes(context -> Reply.done(actions.toggleOpen(
                                        Reply.caller(context), switchName(context), mode(context))))));
    }

    /** The command that lists the unguarded switches. */
    public LiteralArgumentBuilder<CommandSourceStack> openListCommand() {
        return Commands.literal("mcx120list")
                .requires(source -> source.getSender().hasPermission(SwitchActions.LIST))
                .executes(context -> Reply.done(actions.listOpen(Reply.caller(context))));
    }

    /** The command that throws a guarded switch. */
    public LiteralArgumentBuilder<CommandSourceStack> guardedSwitchCommand() {
        return Commands.literal("mcx121")
                .then(Commands.argument("switch", StringArgumentType.string())
                        .suggests(suggesting(actions::guardedNames))
                        .then(Commands.argument("password", StringArgumentType.string())
                                .executes(context -> Reply.done(actions.toggleGuarded(
                                        Reply.caller(context),
                                        switchName(context),
                                        password(context),
                                        Optional.empty())))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(modes())
                                        .executes(context -> Reply.done(actions.toggleGuarded(
                                                Reply.caller(context),
                                                switchName(context),
                                                password(context),
                                                mode(context)))))));
    }

    /** The command that lists the guarded switches. */
    public LiteralArgumentBuilder<CommandSourceStack> guardedListCommand() {
        return Commands.literal("mcx121list")
                .requires(source -> source.getSender().hasPermission(SwitchActions.LIST))
                .executes(context -> Reply.done(actions.listGuarded(Reply.caller(context))));
    }

    /** The command that sets and changes passwords. */
    public LiteralArgumentBuilder<CommandSourceStack> passwordCommand() {
        return Commands.literal("mcx121pass")
                .then(Commands.literal("add")
                        .then(Commands.argument("switch", StringArgumentType.string())
                                .suggests(suggesting(actions::guardedNames))
                                .then(Commands.argument("password", StringArgumentType.string())
                                        .executes(context -> Reply.done(actions.addPassword(
                                                Reply.caller(context),
                                                switchName(context),
                                                password(context)))))))
                .then(Commands.literal("change")
                        .then(Commands.argument("switch", StringArgumentType.string())
                                .suggests(suggesting(actions::guardedNames))
                                .then(Commands.argument("old", StringArgumentType.string())
                                        .then(Commands.argument("new", StringArgumentType.string())
                                                .executes(context -> Reply.done(
                                                        actions.changePassword(
                                                                Reply.caller(context),
                                                                switchName(context),
                                                                StringArgumentType.getString(
                                                                        context, "old"),
                                                                StringArgumentType.getString(
                                                                        context, "new"))))))))
                .then(Commands.literal("has")
                        .then(Commands.argument("switch", StringArgumentType.string())
                                .suggests(suggesting(actions::guardedNames))
                                .executes(context -> Reply.done(actions.hasPassword(
                                        Reply.caller(context), switchName(context))))));
    }

    private static String switchName(CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, "switch");
    }

    private static String password(CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, "password");
    }

    private static Optional<String> mode(CommandContext<CommandSourceStack> context) {
        return Optional.of(StringArgumentType.getString(context, "mode"));
    }

    /** Suggests whatever a board currently knows. */
    private static SuggestionProvider<CommandSourceStack> suggesting(
            Function<String, List<String>> names) {

        return (context, builder) -> {
            names.apply(builder.getRemaining()).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> modes() {
        return (context, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String mode : SwitchActions.MODES) {
                if (mode.startsWith(typed)) {
                    builder.suggest(mode);
                }
            }
            return builder.buildFuture();
        };
    }
}
