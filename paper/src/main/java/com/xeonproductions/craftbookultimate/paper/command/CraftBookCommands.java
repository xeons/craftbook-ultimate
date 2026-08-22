// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * Puts the plugin's commands into the server's command tree.
 *
 * <p>Registration goes through the lifecycle event rather than a descriptor file, so the commands
 * are real Brigadier nodes: the client knows their shape, completes switch names as they are typed
 * and colours an argument red before it is sent.
 *
 * <p>The command names are the ones players already know. A chip's command is named after the chip
 * it drives, which is how the old plugin named them and is what people have in their macros.
 */
@NullMarked
public final class CraftBookCommands {

    private final CatalogueCommands catalogue;
    private final SwitchCommands switches;
    private final ConfigCommands config;
    private final CartCommands carts;
    private final MusicCommands music;
    private final AreaCommands areas;
    private final VariableCommands variables;
    private final TestbedCommands testbed;
    private final CheckCommands check;
    private final DebugCommands debug;
    private final SignCommands signs;
    private final ChairCommands chairs;

    public CraftBookCommands(
            CatalogueCommands catalogue,
            SwitchCommands switches,
            ConfigCommands config,
            CartCommands carts,
            MusicCommands music,
            AreaCommands areas,
            VariableCommands variables,
            TestbedCommands testbed,
            CheckCommands check,
            DebugCommands debug,
            SignCommands signs,
            ChairCommands chairs) {
        this.catalogue = catalogue;
        this.switches = switches;
        this.config = config;
        this.carts = carts;
        this.music = music;
        this.areas = areas;
        this.variables = variables;
        this.testbed = testbed;
        this.check = check;
        this.debug = debug;
        this.signs = signs;
        this.chairs = chairs;
    }

    /** Registers everything against a plugin. */
    public void registerOn(Plugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    catalogue.command()
                            .then(config.reloadCommand())
                            .then(music.musicCommand())
                            .then(testbed.testbedCommand())
                            .then(check.checkCommand())
                            .then(debug.debugCommand())
                            .build(),
                    "Reads the integrated circuit catalogue.",
                    List.of("cb"));

            commands.register(
                    variables.command().build(),
                    "Makes and changes the variables the VAR chips read.",
                    List.of("variable"));

            commands.register(
                    switches.openSwitchCommand().build(),
                    "Throws a switch that a command controlled chip follows.",
                    List.of("commandcontrolledic"));

            commands.register(
                    switches.openListCommand().build(),
                    "Lists the switches chips are currently following.",
                    List.of("commandcontrolledlist"));

            commands.register(
                    switches.guardedSwitchCommand().build(),
                    "Throws a switch that takes a password.",
                    List.of("passwordcontrolledic"));

            commands.register(
                    switches.guardedListCommand().build(),
                    "Lists the switches that take a password.",
                    List.of("passwordcontrolledlist"));

            commands.register(
                    switches.passwordCommand().build(),
                    "Sets and changes the passwords on guarded switches.",
                    List.of());

            commands.register(
                    carts.destinationCommand().build(),
                    "Says where you are going, so the junctions route you there.",
                    List.of("st"));

            commands.register(
                    carts.goCommand().build(),
                    "Goes the way you are facing, from a junction that has stopped to ask.",
                    List.of());

            commands.register(
                    carts.recipesCommand().build(),
                    "Looks up what a recipe is called on a cart crafter's sign.",
                    List.of());

            commands.register(
                    signs.command().build(),
                    "Edits a line of the sign you have copied, before you paste it.",
                    List.of("signcopier"));

            commands.register(
                    areas.areaCommand().build(),
                    "Saves, deletes and lists the areas a toggled area sign can name.",
                    List.of("togglearea"));

            commands.register(
                    chairs.sit().build(),
                    "Sits you down where you are standing.",
                    List.of());

            commands.register(
                    chairs.stand().build(),
                    "Stands you up out of a chair.",
                    List.of());

            commands.register(
                    chairs.toggle().build(),
                    "Turns sitting down by clicking a chair on and off for yourself.",
                    List.of());
        });
    }
}
