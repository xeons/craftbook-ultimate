package com.xeonproductions.craftbookultimate.paper.store;

import com.xeonproductions.craftbookultimate.core.effect.FireworkShow;
import com.xeonproductions.craftbookultimate.core.effect.FireworkShows;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * Where the firework display scripts live.
 *
 * <p>An operator drops a script in the folder and a display chip names it. Two spellings are
 * understood and the extension says which: {@code .txt} for the plain one and {@code .fwk} for the
 * one that builds effects by name.
 *
 * <p>Scripts are read once, when the plugin starts, rather than every time a chunk loads. A show is
 * a file somebody wrote by hand and reading one on the ticking thread is not something a chunk load
 * should have to wait for.
 */
@NullMarked
public final class FireworkFiles {

    /** What the folder is called inside the plugin's own folder. */
    public static final String FOLDER_NAME = "fireworks";

    /** The extension of a script written one launch per line. */
    private static final String PLAIN_EXTENSION = ".txt";

    /** The extension of a script that builds effects by name. */
    private static final String NAMED_EXTENSION = ".fwk";

    /** The longest script that will be read, so one enormous file cannot fill memory. */
    private static final int MAX_LINES = 8192;

    private final Path folder;

    public FireworkFiles(Path directory) {
        this.folder = directory.resolve(FOLDER_NAME);
    }

    /** Where the scripts live. */
    public Path path() {
        return folder;
    }

    /**
     * Reads every script into a registry, replacing whatever was there.
     *
     * <p>A script that cannot be read is skipped rather than stopping the rest, since one bad file
     * should not cost an operator every other display they have written.
     *
     * @return how many shows were read
     * @throws IOException if the folder itself cannot be made or listed
     */
    public int load(FireworkShows shows) throws IOException {
        Files.createDirectories(folder);
        shows.clear();

        int read = 0;
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.toList()) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                String fileName = file.getFileName().toString();
                FireworkShow.Dialect dialect;
                if (fileName.endsWith(PLAIN_EXTENSION)) {
                    dialect = FireworkShow.Dialect.PLAIN;
                } else if (fileName.endsWith(NAMED_EXTENSION)) {
                    dialect = FireworkShow.Dialect.NAMED;
                } else {
                    continue;
                }

                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                if (!FireworkShows.isUsableName(name)) {
                    continue;
                }

                List<String> lines;
                try {
                    lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    continue;
                }
                if (lines.size() > MAX_LINES) {
                    lines = lines.subList(0, MAX_LINES);
                }

                FireworkShow show = FireworkShow.parse(lines, dialect);
                if (!show.isEmpty() && shows.put(name, show)) {
                    read++;
                }
            }
        }
        return read;
    }
}
