// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.music.Songs;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Saying what music the server has.
 *
 * <p>A melody chip names a song, and a builder standing at a sign has no other way of finding out
 * what an operator has put in the folder. Both listings report what was read at start-up, so what
 * they say is exactly what a sign may ask for.
 */
@NullMarked
public final class MusicActions {

    /** The permission to ask what music there is. */
    public static final String LIST = "craftbook.music.list";

    /** How many names are listed at once. */
    private static final int SHOWN = 40;

    private final Songs songs;

    public MusicActions(Songs songs) {
        this.songs = songs;
    }

    /** The songs a melody chip may name. */
    public boolean songs(Caller caller) {
        return list(caller, "songs", songs.songNames());
    }

    /** The playlists a melody chip may name. */
    public boolean playlists(Caller caller) {
        return list(caller, "playlists", songs.playlistNames());
    }

    private static boolean list(Caller caller, String what, Set<String> names) {
        if (names.isEmpty()) {
            caller.refuse("There are no " + what + ".");
            return false;
        }

        List<String> sorted = names.stream().sorted().toList();
        caller.heading(sorted.size() + " " + what + ":");
        for (String name : sorted.subList(0, Math.min(SHOWN, sorted.size()))) {
            caller.detail("  " + name);
        }
        if (sorted.size() > SHOWN) {
            caller.send(Component.text(
                    "  and " + (sorted.size() - SHOWN) + " more.", NamedTextColor.DARK_GRAY));
        }
        return true;
    }
}
