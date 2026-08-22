// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.command;

import com.xeonproductions.craftbookultimate.core.command.BrokenChip;
import com.xeonproductions.craftbookultimate.core.command.Caller;
import com.xeonproductions.craftbookultimate.core.command.CatalogueActions;
import com.xeonproductions.craftbookultimate.core.command.CheckActions;
import com.xeonproductions.craftbookultimate.core.command.MusicActions;
import com.xeonproductions.craftbookultimate.core.command.SwitchActions;
import com.xeonproductions.craftbookultimate.core.command.VariableActions;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.music.Songs;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

/**
 * Every command this build has, in Sponge's own grammar.
 *
 * <p>What each one does is in {@code core/command/}, shared with the Paper build, so the two say
 * exactly the same things in exactly the same words. What is here is the shape a command may be
 * typed in, which is the one part that differs: SpongeAPI 20 exposes no Brigadier, so the tree is
 * built from {@link Command.Parameterized} rather than from literals and arguments.
 *
 * <p>The command names are frozen along with the model numbers they belong to. {@code /mcx120} and
 * {@code /mcx121} are read off signs, and an existing world's builders type them from memory.
 *
 * <p>The carts, the areas, the test bed and the debug stick are not bound on this platform yet, so
 * their commands are deliberately absent rather than registered and dead.
 */
@NullMarked
public final class CraftBookCommands {

    private final CatalogueActions catalogue;
    private final MusicActions music;
    private final SwitchActions switches;
    private final VariableActions variables;
    private final Supplier<Component> reload;
    private final Supplier<ChipCheck> check;

    /**
     * @param registry the chips a listing reads from
     * @param songs the music an operator has put in the folder
     * @param switches the boards the command controlled chips follow
     * @param variables the variables the VAR chips read
     * @param reload rereads the settings and answers with what happened
     * @param check reads the loaded chips for ones that cannot work
     */
    public CraftBookCommands(
            ICRegistry registry,
            Songs songs,
            SwitchActions switches,
            VariableActions variables,
            Supplier<Component> reload,
            Supplier<ChipCheck> check) {

        this.catalogue = new CatalogueActions(registry);
        this.music = new MusicActions(songs);
        this.switches = switches;
        this.variables = variables;
        this.reload = reload;
        this.check = check;
    }

    /** Registers everything against a plugin as the server asks for its commands. */
    public void registerOn(
            PluginContainer plugin, RegisterCommandEvent<Command.Parameterized> event) {

        event.register(plugin, craftbook(), "craftbook", "cb");
        event.register(plugin, variables(), "var", "variable");
        event.register(plugin, openSwitch(), "mcx120", "commandcontrolledic");
        event.register(plugin, openList(), "mcx120list", "commandcontrolledlist");
        event.register(plugin, guardedSwitch(), "mcx121", "passwordcontrolledic");
        event.register(plugin, guardedList(), "mcx121list", "passwordcontrolledlist");
        event.register(plugin, passwords(), "mcx121pass");
    }

    // The plugin's own command ------------------------------------------------------------------

    private Command.Parameterized craftbook() {
        Parameter.Value<Integer> page = Parameter.integerNumber().key("page").optional().build();
        Parameter.Value<String> chip = Parameter.string().key("chip")
                .completer(completing(catalogue::models))
                .build();

        Command.Parameterized list = Command.builder()
                .shortDescription(Component.text("Lists the chips this build knows."))
                .addParameter(page)
                .executor(context -> done(catalogue.list(
                        caller(context), context.one(page).orElse(1))))
                .build();

        Command.Parameterized info = Command.builder()
                .shortDescription(Component.text("Says everything about one chip."))
                .addParameter(chip)
                .executor(context -> done(catalogue.info(caller(context), context.requireOne(chip))))
                .build();

        Command.Parameterized ic = Command.builder()
                .shortDescription(Component.text("Reads the integrated circuit catalogue."))
                .addChild(list, "list")
                .addChild(info, "info")
                .build();

        return Command.builder()
                .shortDescription(Component.text("Reads the integrated circuit catalogue."))
                .permission(CatalogueActions.COMMAND)
                .addChild(ic, "ic")
                .addChild(reloadCommand(), "reload")
                .addChild(checkCommand(), "check")
                .addChild(musicCommand(), "music")
                .executor(context -> done(catalogue.summary(caller(context))))
                .build();
    }

    /**
     * Rereading the settings file.
     *
     * <p>Rereading takes every chip down and picks it up again, because a chip a setting has just
     * switched off has to stop and one a setting has just switched on has to start. Signs are never
     * touched, so nothing is lost either way.
     */
    private Command.Parameterized reloadCommand() {
        return Command.builder()
                .shortDescription(Component.text("Rereads the settings file."))
                .permission("craftbook.reload")
                .executor(context -> {
                    context.cause().audience().sendMessage(reload.get());
                    return CommandResult.success();
                })
                .build();
    }

    private Command.Parameterized checkCommand() {
        return Command.builder()
                .shortDescription(Component.text("Says which loaded chips cannot work."))
                .permission(CheckActions.CHECK)
                .executor(context -> {
                    ChipCheck found = check.get();
                    return done(CheckActions.report(
                            caller(context), found.broken(), found.loaded()));
                })
                .build();
    }

    private Command.Parameterized musicCommand() {
        Command.Parameterized songs = Command.builder()
                .shortDescription(Component.text("Lists the songs a melody chip may name."))
                .executor(context -> done(music.songs(caller(context))))
                .build();

        Command.Parameterized playlists = Command.builder()
                .shortDescription(Component.text("Lists the playlists a melody chip may name."))
                .executor(context -> done(music.playlists(caller(context))))
                .build();

        return Command.builder()
                .shortDescription(Component.text("Says what music the server has."))
                .permission(MusicActions.LIST)
                .addChild(songs, "songs")
                .addChild(playlists, "playlists")
                .build();
    }

    // The variables -----------------------------------------------------------------------------

    private Command.Parameterized variables() {
        Parameter.Value<String> name = Parameter.string().key("variable")
                .completer(completing(variables::known))
                .build();
        Parameter.Value<String> value = Parameter.string().key("value").build();
        Parameter.Value<String> namespace = Parameter.string().key("namespace").optional().build();
        Parameter.Value<Double> amount = Parameter.doubleNumber().key("amount").build();

        Parameter.Value<String> definedValue = Parameter.string().key("value").optional().build();

        Command.Parameterized define = Command.builder()
                .shortDescription(Component.text("Makes a variable."))
                .permission(VariableActions.DEFINE)
                .addParameters(name, definedValue)
                .executor(context -> done(variables.define(
                        caller(context),
                        context.requireOne(name),
                        context.one(definedValue).orElse(Variables.DEFAULT_VALUE))))
                .build();

        Command.Parameterized set = Command.builder()
                .shortDescription(Component.text("Changes a variable."))
                .permission(VariableActions.SET)
                .addParameters(name, value)
                .executor(context -> done(variables.set(
                        caller(context), context.requireOne(name), context.requireOne(value))))
                .build();

        Command.Parameterized get = Command.builder()
                .shortDescription(Component.text("Reads a variable."))
                .permission(VariableActions.GET)
                .addParameter(name)
                .executor(context -> done(variables.get(caller(context), context.requireOne(name))))
                .build();

        Command.Parameterized list = Command.builder()
                .shortDescription(Component.text("Lists the variables."))
                .permission(VariableActions.LIST)
                .addParameter(namespace)
                .executor(context -> done(variables.list(caller(context), context.one(namespace))))
                .build();

        Command.Parameterized delete = Command.builder()
                .shortDescription(Component.text("Removes a variable."))
                .permission(VariableActions.DELETE)
                .addParameter(name)
                .executor(context -> done(
                        variables.delete(caller(context), context.requireOne(name))))
                .build();

        Command.Builder builder = Command.builder()
                .shortDescription(Component.text(
                        "Makes and changes the variables the VAR chips read."))
                .addChild(define, "define")
                .addChild(set, "set")
                .addChild(get, "get")
                .addChild(list, "list")
                .addChild(delete, "delete");

        builder.addChild(arithmetic(name, amount, VariableChips.Function.ADD), "add");
        builder.addChild(arithmetic(name, amount, VariableChips.Function.SUBTRACT), "subtract");
        builder.addChild(arithmetic(name, amount, VariableChips.Function.MULTIPLY), "multiply");
        builder.addChild(arithmetic(name, amount, VariableChips.Function.DIVIDE), "divide");
        return builder.build();
    }

    /** One of the four commands that does a sum to a variable. */
    private Command.Parameterized arithmetic(
            Parameter.Value<String> name,
            Parameter.Value<Double> amount,
            VariableChips.Function function) {

        return Command.builder()
                .shortDescription(Component.text("Does a sum to a variable."))
                .permission(VariableActions.SET)
                .addParameters(name, amount)
                .executor(context -> done(variables.apply(
                        caller(context),
                        context.requireOne(name),
                        function,
                        context.requireOne(amount))))
                .build();
    }

    // The switches ------------------------------------------------------------------------------

    private Command.Parameterized openSwitch() {
        Parameter.Value<String> name = Parameter.string().key("switch")
                .completer(completing(switches::openNames))
                .build();
        Parameter.Value<String> mode = modeParameter();

        return Command.builder()
                .shortDescription(Component.text(
                        "Throws a switch that a command controlled chip follows."))
                .addParameters(name, mode)
                .executor(context -> done(switches.toggleOpen(
                        caller(context), context.requireOne(name), context.one(mode))))
                .build();
    }

    private Command.Parameterized openList() {
        return Command.builder()
                .shortDescription(Component.text("Lists the switches chips are following."))
                .permission(SwitchActions.LIST)
                .executor(context -> done(switches.listOpen(caller(context))))
                .build();
    }

    private Command.Parameterized guardedSwitch() {
        Parameter.Value<String> name = guardedName();
        Parameter.Value<String> password = Parameter.string().key("password").build();
        Parameter.Value<String> mode = modeParameter();

        return Command.builder()
                .shortDescription(Component.text("Throws a switch that takes a password."))
                .addParameters(name, password, mode)
                .executor(context -> done(switches.toggleGuarded(
                        caller(context),
                        context.requireOne(name),
                        context.requireOne(password),
                        context.one(mode))))
                .build();
    }

    private Command.Parameterized guardedList() {
        return Command.builder()
                .shortDescription(Component.text("Lists the switches that take a password."))
                .permission(SwitchActions.LIST)
                .executor(context -> done(switches.listGuarded(caller(context))))
                .build();
    }

    private Command.Parameterized passwords() {
        Parameter.Value<String> name = guardedName();
        Parameter.Value<String> password = Parameter.string().key("password").build();
        Parameter.Value<String> oldPassword = Parameter.string().key("old").build();
        Parameter.Value<String> newPassword = Parameter.string().key("new").build();

        Command.Parameterized add = Command.builder()
                .shortDescription(Component.text("Sets a password on a switch that has none."))
                .addParameters(name, password)
                .executor(context -> done(switches.addPassword(
                        caller(context), context.requireOne(name), context.requireOne(password))))
                .build();

        Command.Parameterized change = Command.builder()
                .shortDescription(Component.text("Changes the password on a switch."))
                .addParameters(name, oldPassword, newPassword)
                .executor(context -> done(switches.changePassword(
                        caller(context),
                        context.requireOne(name),
                        context.requireOne(oldPassword),
                        context.requireOne(newPassword))))
                .build();

        Command.Parameterized has = Command.builder()
                .shortDescription(Component.text("Says whether a switch has a password."))
                .addParameters(name)
                .executor(context -> done(
                        switches.hasPassword(caller(context), context.requireOne(name))))
                .build();

        return Command.builder()
                .shortDescription(Component.text("Sets and changes the passwords on switches."))
                .addChild(add, "add")
                .addChild(change, "change")
                .addChild(has, "has")
                .build();
    }

    private Parameter.Value<String> guardedName() {
        return Parameter.string().key("switch")
                .completer(completing(switches::guardedNames))
                .build();
    }

    private static Parameter.Value<String> modeParameter() {
        return Parameter.string().key("mode").optional()
                .completer(completing(typed -> {
                    String prefix = typed.toLowerCase(Locale.ROOT);
                    List<String> matching = new ArrayList<>();
                    for (String mode : SwitchActions.MODES) {
                        if (mode.startsWith(prefix)) {
                            matching.add(mode);
                        }
                    }
                    return matching;
                }))
                .build();
    }

    // Getting between Sponge and what a command means --------------------------------------------

    private static Caller caller(CommandContext context) {
        return new CauseCaller(context.cause());
    }

    /**
     * What Sponge is told about how it went.
     *
     * <p>A command that refused is still a command that ran: it has already said why in its own
     * words, and answering with an error as well would have Sponge say something less useful over
     * the top of it.
     */
    private static CommandResult done(boolean acted) {
        return acted ? CommandResult.success() : CommandResult.builder().result(0).build();
    }

    private static ValueCompleter completing(Function<String, List<String>> names) {
        return (context, currentInput) -> names.apply(currentInput).stream()
                .map(CommandCompletion::of)
                .toList();
    }

    /** What the chip check found, read off the world before the answer is built. */
    public record ChipCheck(List<BrokenChip> broken, int loaded) {

        public ChipCheck {
            broken = List.copyOf(broken);
        }
    }
}
