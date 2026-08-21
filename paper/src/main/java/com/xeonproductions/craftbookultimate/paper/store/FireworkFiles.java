// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.store;

import com.xeonproductions.craftbookultimate.core.effect.FireworkShow;
import com.xeonproductions.craftbookultimate.core.effect.FireworkShows;
import java.io.IOException;
import java.io.InputStream;
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
 *
 * <p>A few shows ship with the plugin and are written out when the folder is first made, so that a
 * new server has something to wire a display chip to and something to read before writing one. They
 * are written only at that moment: an operator who edits one keeps their edit, and one who deletes
 * one has it stay deleted. Deleting the whole folder brings them all back.
 */
@NullMarked
public final class FireworkFiles {

    /** What the folder is called inside the plugin's own folder. */
    public static final String FOLDER_NAME = "fireworks";

    /**
     * The shows that ship with the plugin, written out when the folder is first made.
     *
     * <p>Named rather than discovered, because a jar cannot be listed reliably from inside itself,
     * and because a list somebody has to add to is a list somebody has to think about.
     */
    public static final List<String> BUNDLED =
            List.of("finale.fwk", "aurora.fwk", "victory.fwk", "heartbeat.txt");

    /** Where inside the jar the bundled shows live. */
    private static final String BUNDLED_PATH = "/" + FOLDER_NAME + "/";

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
        unpackIfFolderIsNew();
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

    /**
     * Writes the shipped shows out, the once, as the folder is made.
     *
     * <p>Gated on the folder not being there rather than on each file, so that a show an operator
     * has deleted stays deleted. The cost of that is a show added in a later version not reaching a
     * server that already has the folder, which is the right way round: an unexpected file
     * reappearing is worse than an example nobody asked for going missing.
     *
     * <p>A bundled show that cannot be written is skipped. They are examples, and losing one is
     * not a reason to stop the plugin starting.
     */
    private void unpackIfFolderIsNew() throws IOException {
        if (Files.isDirectory(folder)) {
            return;
        }

        Files.createDirectories(folder);
        for (String name : BUNDLED) {
            try (InputStream bundled = FireworkFiles.class.getResourceAsStream(BUNDLED_PATH + name)) {
                if (bundled == null) {
                    continue;
                }
                Files.copy(bundled, folder.resolve(name));
            } catch (IOException e) {
                // An example that will not write is not worth failing a start-up over.
            }
        }
    }
}
