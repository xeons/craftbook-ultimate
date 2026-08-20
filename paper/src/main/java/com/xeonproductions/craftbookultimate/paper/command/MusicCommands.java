package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.music.Songs;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/**
 * The commands that say what music the server has.
 *
 * <p>A melody chip names a song, and a builder standing at a sign has no other way of finding out
 * what an operator has put in the folder. Both branches simply list what was read at startup, so
 * what they report is exactly what a sign can ask for.
 */
@NullMarked
public final class MusicCommands {

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    /** How many names are listed at once. */
    private static final int SHOWN = 40;

    private final Songs songs;

    public MusicCommands(Songs songs) {
        this.songs = songs;
    }

    /** The {@code music} branch of the plugin's own command. */
    public LiteralArgumentBuilder<CommandSourceStack> musicCommand() {
        return Commands.literal("music")
                .requires(source -> source.getSender().hasPermission("craftbook.music.list"))
                .then(Commands.literal("songs").executes(this::listSongs))
                .then(Commands.literal("playlists").executes(this::listPlaylists));
    }

    private int listSongs(CommandContext<CommandSourceStack> context) {
        return list(context.getSource().getSender(), "songs", songs.songNames());
    }

    private int listPlaylists(CommandContext<CommandSourceStack> context) {
        return list(context.getSource().getSender(), "playlists", songs.playlistNames());
    }

    private static int list(CommandSender sender, String what, Set<String> names) {
        if (names.isEmpty()) {
            sender.sendMessage(Component.text("There are no " + what + ".", NamedTextColor.RED));
            return 0;
        }

        List<String> sorted = names.stream().sorted().toList();
        sender.sendMessage(Component.text(sorted.size() + " " + what + ":", NamedTextColor.YELLOW));
        for (String name : sorted.subList(0, Math.min(SHOWN, sorted.size()))) {
            sender.sendMessage(Component.text("  " + name, NamedTextColor.GRAY));
        }
        if (sorted.size() > SHOWN) {
            sender.sendMessage(Component.text(
                    "  and " + (sorted.size() - SHOWN) + " more.", NamedTextColor.DARK_GRAY));
        }
        return SUCCESS;
    }
}
