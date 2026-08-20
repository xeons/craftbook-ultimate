package com.xeonproductions.craftbookultimate.core.music;

import java.util.List;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NullMarked;

/**
 * An ordered list of songs to work through.
 *
 * <p>A playlist holds names rather than songs, so a file replaced while the server is running is
 * picked up the next time the playlist reaches it, and a playlist naming a song that has since
 * been deleted skips it rather than breaking.
 *
 * @param name what the playlist is called
 * @param songs the song names, in order
 */
@NullMarked
public record Playlist(String name, List<String> songs) {

    public Playlist {
        songs = List.copyOf(songs);
    }

    /** Whether there is anything to play. */
    public boolean isEmpty() {
        return songs.isEmpty();
    }

    /** How many songs are on it. */
    public int size() {
        return songs.size();
    }

    /**
     * The song at a place in the list, wrapping round so any number is answerable.
     *
     * @param index how far down the list, counted from zero
     */
    public String at(int index) {
        return songs.get(Math.floorMod(index, songs.size()));
    }

    /** A place in the list picked at random. */
    public int anyIndex(RandomGenerator random) {
        return random.nextInt(songs.size());
    }

    /**
     * Reads a playlist file.
     *
     * <p>One song a line. Blank lines and lines opening with a {@code #} are ignored, so a
     * playlist can carry notes about itself, and a trailing {@code .mid} is dropped so a line may
     * name either the song or the file it lives in.
     */
    public static Playlist read(String name, List<String> lines) {
        List<String> songs = new java.util.ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            songs.add(trimmed.endsWith(MIDI_EXTENSION)
                    ? trimmed.substring(0, trimmed.length() - MIDI_EXTENSION.length())
                    : trimmed);
        }
        return new Playlist(name, songs);
    }

    /** What a song's file is called, after its name. */
    public static final String MIDI_EXTENSION = ".mid";
}
