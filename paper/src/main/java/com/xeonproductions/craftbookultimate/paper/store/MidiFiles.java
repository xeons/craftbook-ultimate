package com.xeonproductions.craftbookultimate.paper.store;

import com.xeonproductions.craftbookultimate.core.music.MidiSongs;
import com.xeonproductions.craftbookultimate.core.music.Playlist;
import com.xeonproductions.craftbookultimate.core.music.Song;
import com.xeonproductions.craftbookultimate.core.music.Songs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * Where the music lives.
 *
 * <p>Two folders inside the plugin's own: one of MIDI files and one of playlists naming them. An
 * operator drops a file in and a melody chip names it, so a sign never carries a path and cannot
 * reach anywhere the operator did not put something.
 *
 * <p>Files are read once, when the plugin starts, and turned into songs there and then. Reading
 * and converting a MIDI file is far too slow to do on a ticking thread, and a chunk load should
 * never have to wait for one.
 */
@NullMarked
public final class MidiFiles {

    /** What the folder of songs is called inside the plugin's own folder. */
    public static final String MIDI_FOLDER = "midi";

    /** What the folder of playlists is called. */
    public static final String PLAYLIST_FOLDER = "playlist";

    /** What a playlist file is called, after its name. */
    private static final String PLAYLIST_EXTENSION = ".playlist";

    /** The largest MIDI file that will be read, so one enormous file cannot fill memory. */
    private static final long MAX_MIDI_BYTES = 1024L * 1024L;

    /** The longest playlist that will be read. */
    private static final int MAX_PLAYLIST_LINES = 1024;

    private final Path midi;
    private final Path playlists;

    public MidiFiles(Path directory) {
        this.midi = directory.resolve(MIDI_FOLDER);
        this.playlists = directory.resolve(PLAYLIST_FOLDER);
    }

    /** Where the songs live. */
    public Path midiPath() {
        return midi;
    }

    /** Where the playlists live. */
    public Path playlistPath() {
        return playlists;
    }

    /**
     * Reads every song and playlist into a registry, replacing whatever was there.
     *
     * <p>A file that cannot be read is skipped rather than stopping the rest, since one bad file
     * should not cost an operator every other piece of music they have.
     *
     * @return how many songs and playlists were read between them
     * @throws IOException if a folder itself cannot be made or listed
     */
    public int load(Songs songs) throws IOException {
        Files.createDirectories(midi);
        Files.createDirectories(playlists);
        songs.clear();

        return readSongs(songs) + readPlaylists(songs);
    }

    private int readSongs(Songs songs) throws IOException {
        int read = 0;
        try (Stream<Path> files = Files.list(midi)) {
            for (Path file : files.toList()) {
                String name = nameOf(file, Playlist.MIDI_EXTENSION);
                if (name.isEmpty() || Files.size(file) > MAX_MIDI_BYTES) {
                    continue;
                }

                Song song;
                try {
                    song = MidiSongs.read(name, Files.readAllBytes(file));
                } catch (IOException e) {
                    continue;
                }
                if (!song.isEmpty() && songs.putSong(name, song)) {
                    read++;
                }
            }
        }
        return read;
    }

    private int readPlaylists(Songs songs) throws IOException {
        int read = 0;
        try (Stream<Path> files = Files.list(playlists)) {
            for (Path file : files.toList()) {
                String name = nameOf(file, PLAYLIST_EXTENSION);
                if (name.isEmpty()) {
                    continue;
                }

                List<String> lines;
                try {
                    lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    continue;
                }
                if (lines.size() > MAX_PLAYLIST_LINES) {
                    lines = lines.subList(0, MAX_PLAYLIST_LINES);
                }

                Playlist playlist = Playlist.read(name, lines);
                if (!playlist.isEmpty() && songs.putPlaylist(name, playlist)) {
                    read++;
                }
            }
        }
        return read;
    }

    /**
     * What a file is called, without its extension.
     *
     * <p>Empty for anything that is not a readable file of the right kind, or whose name is not
     * one a sign could ask for.
     */
    private static String nameOf(Path file, String extension) {
        if (!Files.isRegularFile(file)) {
            return "";
        }

        String fileName = file.getFileName().toString();
        if (!fileName.toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
            return "";
        }

        String name = fileName.substring(0, fileName.length() - extension.length());
        return Songs.isUsableName(name) ? name : "";
    }
}
