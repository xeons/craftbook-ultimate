// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xeonproductions.craftbookultimate.core.command.VariableActions;
import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The grammar of the commands that make and change the variables the VAR chips read.
 *
 * <p>What each one does is in {@link VariableActions}, including who may touch whose.
 */
@NullMarked
public final class VariableCommands {

    /** The permission to make a variable. */
    public static final String DEFINE = VariableActions.DEFINE;

    /** The permission to change one, by setting it or by doing a sum to it. */
    public static final String SET = VariableActions.SET;

    /** The permission to read one. */
    public static final String GET = VariableActions.GET;

    /** The permission to list them. */
    public static final String LIST = VariableActions.LIST;

    /** The permission to remove one. */
    public static final String DELETE = VariableActions.DELETE;

    private final VariableActions actions;

    /**
     * @param variables the variables to work on
     * @param save writes them out after one has changed
     */
    public VariableCommands(Variables variables, Runnable save) {
        this.actions = new VariableActions(variables, save);
    }

    /** The whole {@code /var} command. */
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("var")
                .then(Commands.literal("define")
                        .requires(source -> source.getSender().hasPermission(DEFINE))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .executes(context -> Reply.done(actions.define(
                                        Reply.caller(context),
                                        variable(context),
                                        Variables.DEFAULT_VALUE)))
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(context -> Reply.done(actions.define(
                                                Reply.caller(context),
                                                variable(context),
                                                value(context)))))))
                .then(Commands.literal("set")
                        .requires(source -> source.getSender().hasPermission(SET))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .suggests(known())
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(context -> Reply.done(actions.set(
                                                Reply.caller(context),
                                                variable(context),
                                                value(context)))))))
                .then(Commands.literal("get")
                        .requires(source -> source.getSender().hasPermission(GET))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .suggests(known())
                                .executes(context -> Reply.done(actions.get(
                                        Reply.caller(context), variable(context))))))
                .then(Commands.literal("list")
                        .requires(source -> source.getSender().hasPermission(LIST))
                        .executes(context -> Reply.done(actions.list(
                                Reply.caller(context), Optional.empty())))
                        .then(Commands.argument("namespace", StringArgumentType.word())
                                .executes(context -> Reply.done(actions.list(
                                        Reply.caller(context),
                                        Optional.of(StringArgumentType.getString(
                                                context, "namespace")))))))
                .then(Commands.literal("delete")
                        .requires(source -> source.getSender().hasPermission(DELETE))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .suggests(known())
                                .executes(context -> Reply.done(actions.delete(
                                        Reply.caller(context), variable(context))))))
                .then(arithmetic("add", VariableChips.Function.ADD))
                .then(arithmetic("subtract", VariableChips.Function.SUBTRACT))
                .then(arithmetic("multiply", VariableChips.Function.MULTIPLY))
                .then(arithmetic("divide", VariableChips.Function.DIVIDE));
    }

    /** One of the four commands that does a sum to a variable. */
    private LiteralArgumentBuilder<CommandSourceStack> arithmetic(
            String name, VariableChips.Function function) {

        return Commands.literal(name)
                .requires(source -> source.getSender().hasPermission(SET))
                .then(Commands.argument("variable", StringArgumentType.word())
                        .suggests(known())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                .executes(context -> Reply.done(actions.apply(
                                        Reply.caller(context),
                                        variable(context),
                                        function,
                                        DoubleArgumentType.getDouble(context, "amount"))))));
    }

    private static String variable(CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, "variable");
    }

    private static String value(CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, "value");
    }

    /** Suggests the variables there are, which is what somebody is usually reaching for. */
    private SuggestionProvider<CommandSourceStack> known() {
        return (context, builder) -> {
            actions.known(builder.getRemaining()).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
