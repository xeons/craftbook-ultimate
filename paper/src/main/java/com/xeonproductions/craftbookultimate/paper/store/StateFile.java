// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.store;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * A file of plain lines that some registry writes out and reads back.
 *
 * <p>The registries a chip shares with every other chip on the server hold things a player set
 * deliberately — a switch somebody threw, a band somebody is transmitting on — and losing them
 * because the server restarted is losing work. Each of them says how to write itself out as lines
 * and how to read those lines back, and this puts them somewhere.
 *
 * <p>Writing goes to a temporary file that is then moved into place, so a server killed part way
 * through a save leaves the previous file intact rather than half of a new one.
 */
@NullMarked
public final class StateFile {

    /** What a pending write is called before it is moved into place. */
    private static final String PENDING_SUFFIX = ".new";

    /** What marks a line as a note to whoever opens the file rather than an entry. */
    private static final String COMMENT = "#";

    private final Path file;
    private final List<String> header;

    /**
     * @param directory the plugin's own folder
     * @param fileName what the file is called inside it
     * @param header lines explaining what the file is, written above the entries
     */
    public StateFile(Path directory, String fileName, List<String> header) {
        this.file = directory.resolve(fileName);
        this.header = List.copyOf(header);
    }

    /** Where the file is. */
    public Path path() {
        return file;
    }

    /**
     * The entries in the file, with the notes and the blank lines taken out.
     *
     * @return the lines, or nothing at all if there is no file yet
     * @throws IOException if the file exists but cannot be read
     */
    public List<String> read() throws IOException {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank() && !line.stripLeading().startsWith(COMMENT))
                .toList();
    }

    /**
     * Writes entries out, replacing whatever was there.
     *
     * @throws IOException if the file cannot be written
     */
    public void write(List<String> entries) throws IOException {
        Files.createDirectories(file.getParent());

        List<String> lines = new ArrayList<>(header);
        lines.addAll(entries);

        Path pending = file.resolveSibling(file.getFileName() + PENDING_SUFFIX);
        Files.write(pending, lines, StandardCharsets.UTF_8);
        try {
            Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Some filesystems cannot promise an atomic move. A plain replace is still better than
            // writing over the file in place.
            Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
