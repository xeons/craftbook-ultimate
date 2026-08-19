package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xeonproductions.craftbookultimate.core.control.PasswordStore;
import com.xeonproductions.craftbookultimate.core.control.Switchboard;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/**
 * The commands that throw the switches the command controlled chips follow.
 *
 * <p>Two families, deliberately kept apart. {@code /mcx120} throws a switch anybody may throw;
 * {@code /mcx121} throws one that takes a password, and {@code /mcx121pass} is where a password is
 * set or changed. A switch name on one is unrelated to the same name on the other.
 *
 * <p>Checking a password is deliberately slow, so it happens away from the thread that ticks the
 * world. That is why the reply to a {@code /mcx121} command arrives a moment after the command
 * rather than instantly, and why spamming it cannot hold the server up.
 */
@NullMarked
public final class SwitchCommands {

    /** What a switch may be told to do, beyond being toggled. */
    private static final String[] MODES = {"on", "off", "state"};

    private final Switchboard open;
    private final Switchboard guarded;
    private final PasswordStore passwords;
    private final Consumer<Runnable> offThread;
    private final Runnable savePasswords;

    /**
     * @param open the switches anyone may throw
     * @param guarded the switches that take a password
     * @param passwords the passwords guarding those switches
     * @param offThread runs work away from the thread that ticks the world
     * @param savePasswords writes the passwords out after one has changed
     */
    public SwitchCommands(
            Switchboard open,
            Switchboard guarded,
            PasswordStore passwords,
            Consumer<Runnable> offThread,
            Runnable savePasswords) {
        this.open = open;
        this.guarded = guarded;
        this.passwords = passwords;
        this.offThread = offThread;
        this.savePasswords = savePasswords;
    }

    /** The command that throws an unguarded switch. */
    public LiteralArgumentBuilder<CommandSourceStack> openSwitchCommand() {
        return Commands.literal("mcx120")
                .then(Commands.argument("switch", StringArgumentType.string())
                        .suggests(namesOf(open))
                        .executes(context -> toggleOpen(context, Optional.empty()))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(modes())
                                .executes(context -> toggleOpen(
                                        context, Optional.of(StringArgumentType.getString(context, "mode"))))));
    }

    /** The command that lists the unguarded switches. */
    public LiteralArgumentBuilder<CommandSourceStack> openListCommand() {
        return Commands.literal("mcx120list")
                .requires(source -> source.getSender().hasPermission("craftbook.switch.list"))
                .executes(context -> {
                    list(context.getSource().getSender(), open, "Switches");
                    return SUCCESS;
                });
    }

    /** The command that throws a guarded switch. */
    public LiteralArgumentBuilder<CommandSourceStack> guardedSwitchCommand() {
        return Commands.literal("mcx121")
                .then(Commands.argument("switch", StringArgumentType.string())
                        .suggests(namesOf(guarded))
                        .then(Commands.argument("password", StringArgumentType.string())
                                .executes(context -> toggleGuarded(context, Optional.empty()))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(modes())
                                        .executes(context -> toggleGuarded(
                                                context,
                                                Optional.of(StringArgumentType.getString(context, "mode")))))));
    }

    /** The command that lists the guarded switches. */
    public LiteralArgumentBuilder<CommandSourceStack> guardedListCommand() {
        return Commands.literal("mcx121list")
                .requires(source -> source.getSender().hasPermission("craftbook.switch.list"))
                .executes(context -> {
                    list(context.getSource().getSender(), guarded, "Guarded switches");
                    return SUCCESS;
                });
    }

    /** The command that sets and changes passwords. */
    public LiteralArgumentBuilder<CommandSourceStack> passwordCommand() {
        return Commands.literal("mcx121pass")
                .then(Commands.literal("add")
                        .then(Commands.argument("switch", StringArgumentType.string())
                                .suggests(namesOf(guarded))
                                .then(Commands.argument("password", StringArgumentType.string())
                                        .executes(this::addPassword))))
                .then(Commands.literal("change")
                        .then(Commands.argument("switch", StringArgumentType.string())
                                .suggests(namesOf(guarded))
                                .then(Commands.argument("old", StringArgumentType.string())
                                        .then(Commands.argument("new", StringArgumentType.string())
                                                .executes(this::changePassword)))))
                .then(Commands.literal("has")
                        .then(Commands.argument("switch", StringArgumentType.string())
                                .suggests(namesOf(guarded))
                                .executes(this::hasPassword)));
    }

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    private int toggleOpen(CommandContext<CommandSourceStack> context, Optional<String> mode) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "switch");

        if (!open.isKnown(name)) {
            error(sender, "No chip is following a switch called " + name + ".");
            return 0;
        }
        return apply(sender, open, name, mode);
    }

    private int toggleGuarded(CommandContext<CommandSourceStack> context, Optional<String> mode) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "switch");
        String password = StringArgumentType.getString(context, "password");

        if (!guarded.isKnown(name)) {
            error(sender, "No chip is following a switch called " + name + ".");
            return 0;
        }
        if (!passwords.hasPassword(name)) {
            error(sender, "That switch has no password yet. Set one with /mcx121pass add.");
            return 0;
        }

        offThread.accept(() -> {
            if (!passwords.matches(name, password)) {
                error(sender, "Wrong password.");
                return;
            }
            apply(sender, guarded, name, mode);
        });
        return SUCCESS;
    }

    /** Throws a switch, or reports where it is standing. */
    private int apply(CommandSender sender, Switchboard board, String name, Optional<String> mode) {
        String wanted = mode.map(text -> text.toLowerCase(Locale.ROOT)).orElse("");

        switch (wanted) {
            case "" -> {
                Optional<Boolean> position = board.toggle(name);
                if (position.isEmpty()) {
                    error(sender, "That switch has never been thrown, so there is nothing to "
                            + "toggle. Use on or off.");
                    return 0;
                }
                tell(sender, "Switch " + name + " is now " + describe(position.get()) + ".");
            }
            case "on", "off" -> {
                board.set(name, wanted.equals("on"));
                tell(sender, "Switch " + name + " is now " + wanted + ".");
            }
            case "state" -> {
                Optional<Boolean> position = board.state(name);
                tell(sender, position
                        .map(on -> "Switch " + name + " is " + describe(on) + ".")
                        .orElse("Switch " + name + " has never been thrown."));
            }
            default -> {
                error(sender, "Say on, off or state, or nothing at all to toggle it.");
                return 0;
            }
        }
        return SUCCESS;
    }

    private int addPassword(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "switch");
        String password = StringArgumentType.getString(context, "password");

        if (!guarded.isKnown(name)) {
            error(sender, "No chip is following a switch called " + name + ".");
            return 0;
        }
        if (!PasswordStore.isSaveableName(name)) {
            error(sender, "A switch with a colon in its name cannot have a password saved.");
            return 0;
        }
        if (passwords.hasPassword(name)) {
            error(sender, "That switch already has a password. Use /mcx121pass change.");
            return 0;
        }

        offThread.accept(() -> {
            if (!passwords.setPassword(name, password)) {
                error(sender, "That password could not be set.");
                return;
            }
            savePasswords.run();
            tell(sender, "Password set for switch " + name + ".");
        });
        return SUCCESS;
    }

    private int changePassword(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "switch");
        String oldPassword = StringArgumentType.getString(context, "old");
        String newPassword = StringArgumentType.getString(context, "new");

        offThread.accept(() -> {
            if (!passwords.changePassword(name, oldPassword, newPassword)) {
                error(sender, "Wrong password, or that switch has none set.");
                return;
            }
            savePasswords.run();
            tell(sender, "Password changed for switch " + name + ".");
        });
        return SUCCESS;
    }

    private int hasPassword(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "switch");

        tell(sender, passwords.hasPassword(name)
                ? "Switch " + name + " has a password."
                : "Switch " + name + " has no password.");
        return SUCCESS;
    }

    private static void list(CommandSender sender, Switchboard board, String title) {
        if (board.size() == 0) {
            tell(sender, "No chip is following any switch right now.");
            return;
        }

        sender.sendMessage(Component.text(title + " (" + board.size() + ")", NamedTextColor.YELLOW));
        for (String name : board.names()) {
            Optional<Boolean> position = board.state(name);
            sender.sendMessage(Component.text("  " + name + "  ", NamedTextColor.GRAY)
                    .append(position
                            .map(on -> Component.text(
                                    describe(on), on ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .orElse(Component.text("never thrown", NamedTextColor.DARK_GRAY))));
        }
    }

    /** Suggests the switch names a board currently knows. */
    private static SuggestionProvider<CommandSourceStack> namesOf(Switchboard board) {
        return (context, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String name : board.names()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(typed)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> modes() {
        return (context, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String mode : MODES) {
                if (mode.startsWith(typed)) {
                    builder.suggest(mode);
                }
            }
            return CompletableFuture.completedFuture(builder.build());
        };
    }

    private static String describe(boolean on) {
        return on ? "on" : "off";
    }

    private static void tell(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
