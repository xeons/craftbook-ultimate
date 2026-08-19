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

    public CraftBookCommands(CatalogueCommands catalogue, SwitchCommands switches) {
        this.catalogue = catalogue;
        this.switches = switches;
    }

    /** Registers everything against a plugin. */
    public void registerOn(Plugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    catalogue.command().build(),
                    "Reads the integrated circuit catalogue.",
                    List.of("cb"));

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
        });
    }
}
