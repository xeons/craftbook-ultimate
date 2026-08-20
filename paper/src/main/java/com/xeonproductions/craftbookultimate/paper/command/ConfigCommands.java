package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * The command that rereads the settings file.
 *
 * <p>Rereading takes every chip down and picks it up again, because a chip a setting has just
 * switched off has to stop and one a setting has just switched on has to start. Signs are never
 * touched, so nothing is lost either way.
 */
@NullMarked
public final class ConfigCommands {

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    private final Supplier<Component> reload;

    /**
     * @param reload rereads the settings and answers with what happened
     */
    public ConfigCommands(Supplier<Component> reload) {
        this.reload = reload;
    }

    /** The {@code reload} branch of the plugin's own command. */
    public LiteralArgumentBuilder<CommandSourceStack> reloadCommand() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("craftbook.reload"))
                .executes(context -> {
                    context.getSource().getSender().sendMessage(reload.get());
                    return SUCCESS;
                });
    }
}
