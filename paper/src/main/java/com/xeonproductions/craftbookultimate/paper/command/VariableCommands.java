// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/**
 * The commands that make and change the variables the VAR chips read.
 *
 * <p>A variable has to exist before a sign can name one, so this is where every variable starts
 * life. That is deliberate rather than ceremonial: a chip naming a variable nobody has made would
 * be silently dead, so the chips refuse such a sign, and this is what a builder uses to put that
 * right.
 *
 * <p>Making and changing are separate verbs. {@code define} makes a variable and will not touch one
 * that already exists; {@code set} changes one and will not make one. A command meaning to change a
 * running score cannot then quietly create a second one under a misspelling and leave the original
 * where it was.
 *
 * <p>Which variables somebody may touch follows the same rule here as it does on a sign: the shared
 * ones and their own always, anybody else's only with
 * {@link VariableChips#OTHER_NAMESPACE_PERMISSION}.
 */
@NullMarked
public final class VariableCommands {

    /** The permission to make a variable. */
    public static final String DEFINE = "craftbook.variables.define";

    /** The permission to change one, by setting it or by doing a sum to it. */
    public static final String SET = "craftbook.variables.set";

    /** The permission to read one. */
    public static final String GET = "craftbook.variables.get";

    /** The permission to list them. */
    public static final String LIST = "craftbook.variables.list";

    /** The permission to remove one. */
    public static final String DELETE = "craftbook.variables.delete";

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    /** How many variables a listing shows before it stops. */
    private static final int LISTING_LIMIT = 60;

    private final Variables variables;
    private final Runnable save;

    /**
     * @param variables the variables to work on
     * @param save writes them out after one has changed
     */
    public VariableCommands(Variables variables, Runnable save) {
        this.variables = variables;
        this.save = save;
    }

    /** The whole {@code /var} command. */
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("var")
                .then(Commands.literal("define")
                        .requires(source -> source.getSender().hasPermission(DEFINE))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .executes(context -> define(context, Variables.DEFAULT_VALUE))
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(context -> define(
                                                context, StringArgumentType.getString(context, "value"))))))
                .then(Commands.literal("set")
                        .requires(source -> source.getSender().hasPermission(SET))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .suggests(known())
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(this::set))))
                .then(Commands.literal("get")
                        .requires(source -> source.getSender().hasPermission(GET))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .suggests(known())
                                .executes(this::get)))
                .then(Commands.literal("list")
                        .requires(source -> source.getSender().hasPermission(LIST))
                        .executes(context -> list(context, Optional.empty()))
                        .then(Commands.argument("namespace", StringArgumentType.word())
                                .executes(context -> list(
                                        context,
                                        Optional.of(StringArgumentType.getString(context, "namespace"))))))
                .then(Commands.literal("delete")
                        .requires(source -> source.getSender().hasPermission(DELETE))
                        .then(Commands.argument("variable", StringArgumentType.word())
                                .suggests(known())
                                .executes(this::delete)))
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
                                .executes(context -> apply(context, function))));
    }

    private int define(CommandContext<CommandSourceStack> context, String value) {
        CommandSender sender = context.getSource().getSender();
        Optional<VariableName> name = named(context, sender);
        if (name.isEmpty()) {
            return 0;
        }

        if (!Variables.isStorable(value)) {
            error(sender, "A value is letters, digits and . , : ; _ + - with no spaces.");
            return 0;
        }
        if (!variables.define(name.get(), value)) {
            error(sender, "There is already a variable called " + name.get()
                    + ". Change it with /var set.");
            return 0;
        }

        save.run();
        tell(sender, "Variable " + name.get() + " is now " + value + ".");
        return SUCCESS;
    }

    private int set(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Optional<VariableName> name = named(context, sender);
        if (name.isEmpty()) {
            return 0;
        }

        String value = StringArgumentType.getString(context, "value");
        if (!Variables.isStorable(value)) {
            error(sender, "A value is letters, digits and . , : ; _ + - with no spaces.");
            return 0;
        }
        if (!variables.set(name.get(), value)) {
            return missing(sender, name.get());
        }

        save.run();
        tell(sender, "Variable " + name.get() + " is now " + value + ".");
        return SUCCESS;
    }

    private int get(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Optional<VariableName> name = named(context, sender);
        if (name.isEmpty()) {
            return 0;
        }

        Optional<String> value = variables.get(name.get());
        if (value.isEmpty()) {
            return missing(sender, name.get());
        }

        tell(sender, name.get() + " is " + value.get() + ".");
        return SUCCESS;
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Optional<VariableName> name = named(context, sender);
        if (name.isEmpty()) {
            return 0;
        }

        if (!variables.remove(name.get())) {
            return missing(sender, name.get());
        }

        save.run();
        tell(sender, "Variable " + name.get() + " is gone. Any chip naming it now does nothing.");
        return SUCCESS;
    }

    /** Does a sum to a variable, which is what the four arithmetic commands all come down to. */
    private int apply(CommandContext<CommandSourceStack> context, VariableChips.Function function) {
        CommandSender sender = context.getSource().getSender();
        Optional<VariableName> name = named(context, sender);
        if (name.isEmpty()) {
            return 0;
        }

        OptionalDouble held = variables.number(name.get());
        if (held.isEmpty()) {
            if (!variables.has(name.get())) {
                return missing(sender, name.get());
            }
            error(sender, "Variable " + name.get() + " does not hold a number, so there is "
                    + "nothing to do a sum to.");
            return 0;
        }

        double amount = DoubleArgumentType.getDouble(context, "amount");
        variables.setNumber(name.get(), function.apply(held.getAsDouble(), amount));

        save.run();
        tell(sender, "Variable " + name.get() + " is now "
                + variables.get(name.get()).orElse(Variables.DEFAULT_VALUE) + ".");
        return SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> context, Optional<String> namespace) {
        CommandSender sender = context.getSource().getSender();

        List<VariableName> names = namespace
                .map(variables::namesIn)
                .orElseGet(variables::names);

        if (names.isEmpty()) {
            tell(sender, namespace
                    .map(where -> "There are no variables in " + where + ".")
                    .orElse("There are no variables yet. Make one with /var define."));
            return SUCCESS;
        }

        sender.sendMessage(Component.text(
                "Variables (" + names.size() + ")", NamedTextColor.YELLOW));
        for (VariableName name : names.stream().limit(LISTING_LIMIT).toList()) {
            sender.sendMessage(Component.text("  " + name + "  ", NamedTextColor.GRAY)
                    .append(Component.text(
                            variables.get(name).orElse(""), NamedTextColor.WHITE)));
        }
        if (names.size() > LISTING_LIMIT) {
            tell(sender, "and " + (names.size() - LISTING_LIMIT)
                    + " more. Name a namespace to narrow it down.");
        }
        return SUCCESS;
    }

    /**
     * The variable a command names, once it is known to be one this sender may touch.
     *
     * <p>A bare name means the shared variable, exactly as it does on a sign. Defaulting to the
     * sender's own namespace instead would make {@code /var get score} and a sign reading
     * {@code score} name different variables, which is the one thing a builder checking their work
     * by command must be able to rely on.
     */
    private Optional<VariableName> named(
            CommandContext<CommandSourceStack> context, CommandSender sender) {

        String written = StringArgumentType.getString(context, "variable");
        Optional<VariableName> name = VariableName.parse(written, VariableName.SHARED);

        if (name.isEmpty()) {
            error(sender, "A variable name is letters, digits and underscores. Put a namespace "
                    + "and a " + VariableName.SEPARATOR + " before it to name somebody else's.");
            return Optional.empty();
        }

        if (!mayTouch(name.get(), sender)) {
            error(sender, "The variable " + name.get() + " belongs to " + name.get().namespace()
                    + ", and you may only use your own and the shared ones.");
            return Optional.empty();
        }

        return name;
    }

    /** Whether somebody may touch a variable, by the same rule that governs building on one. */
    private static boolean mayTouch(VariableName name, CommandSender sender) {
        return name.isShared()
                || name.namespace().equalsIgnoreCase(sender.getName())
                || sender.hasPermission(VariableChips.OTHER_NAMESPACE_PERMISSION);
    }

    /** Suggests the variables there are, which is what somebody is usually reaching for. */
    private SuggestionProvider<CommandSourceStack> known() {
        return (context, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (VariableName name : variables.names()) {
                String written = name.toString();
                if (written.toLowerCase(Locale.ROOT).startsWith(typed)) {
                    builder.suggest(written);
                }
            }
            return builder.buildFuture();
        };
    }

    private static int missing(CommandSender sender, VariableName name) {
        error(sender, "There is no variable called " + name + ". Make one with /var define "
                + name + " 0.");
        return 0;
    }

    private static void tell(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
