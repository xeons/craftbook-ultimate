// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.music;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * The music the server has files for.
 *
 * <p>Songs and playlists are files an operator puts in the plugin's folder, so this is a registry
 * rather than anything a chip writes to: a melody chip names one and finds it here.
 *
 * <p>Names are letters, digits and underscores only, for the same reason they are on a firework
 * display: a name goes on to become a file name, and anything else would let a sign reach outside
 * the folder the music is meant to live in.
 */
@NullMarked
public final class Songs {

    /** What a song or a playlist may be called. */
    private static final Pattern USABLE_NAME = Pattern.compile("[A-Za-z0-9_]{1,64}");

    private final Map<String, Song> songs = new ConcurrentHashMap<>();
    private final Map<String, Playlist> playlists = new ConcurrentHashMap<>();

    /** Whether a name is one a sign may ask for and a file may be called. */
    public static boolean isUsableName(String name) {
        return USABLE_NAME.matcher(name).matches();
    }

    /**
     * Records a song under a name, replacing any song already under it.
     *
     * @return true if the name was usable and the song was recorded
     */
    public boolean putSong(String name, Song song) {
        if (!isUsableName(name)) {
            return false;
        }
        songs.put(name, song);
        return true;
    }

    /**
     * Records a playlist under a name, replacing any playlist already under it.
     *
     * @return true if the name was usable and the playlist was recorded
     */
    public boolean putPlaylist(String name, Playlist playlist) {
        if (!isUsableName(name)) {
            return false;
        }
        playlists.put(name, playlist);
        return true;
    }

    /** The song under a name, if there is one and the name is usable at all. */
    public Optional<Song> findSong(String name) {
        return isUsableName(name) ? Optional.ofNullable(songs.get(name)) : Optional.empty();
    }

    /** The playlist under a name, if there is one and the name is usable at all. */
    public Optional<Playlist> findPlaylist(String name) {
        return isUsableName(name) ? Optional.ofNullable(playlists.get(name)) : Optional.empty();
    }

    /** Every song that has been read. */
    public Set<String> songNames() {
        return Set.copyOf(songs.keySet());
    }

    /** Every playlist that has been read. */
    public Set<String> playlistNames() {
        return Set.copyOf(playlists.keySet());
    }

    /** How many songs there are. */
    public int songCount() {
        return songs.size();
    }

    /** How many playlists there are. */
    public int playlistCount() {
        return playlists.size();
    }

    /** Forgets everything, which is what rereading the folder starts with. */
    public void clear() {
        songs.clear();
        playlists.clear();
    }
}
