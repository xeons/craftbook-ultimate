// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.store;

import com.xeonproductions.craftbookultimate.core.control.PasswordStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Where the switch passwords are kept between restarts.
 *
 * <p>A password on a switch is worth nothing if it is forgotten every time the server stops, so
 * this is the one thing the plugin persists so far. What is written is a salted hash and never a
 * password, so the file is not worth stealing.
 *
 * <p>Writing goes to a temporary file that is then moved into place, so a server killed part way
 * through a save leaves the previous file intact rather than half of a new one.
 */
@NullMarked
public final class PasswordFile {

    /** What the file is called inside the plugin's own folder. */
    public static final String FILE_NAME = "switch-passwords.txt";

    /** The header written above the entries, explaining what the file is to anyone who opens it. */
    private static final List<String> HEADER = List.of(
            "# Passwords for the switches driven by the MCX121 chip.",
            "# Each line is a switch name, a salt and a hash, separated by colons.",
            "# No password is stored here and none can be recovered from this file.",
            "# Deleting a line clears that switch's password.");

    private final Path file;

    public PasswordFile(Path directory) {
        this.file = directory.resolve(FILE_NAME);
    }

    /** Where the passwords are kept. */
    public Path path() {
        return file;
    }

    /**
     * Reads the passwords into a store.
     *
     * @return how many were read, or zero if there is nothing to read yet
     * @throws IOException if the file exists but cannot be read
     */
    public int load(PasswordStore store) throws IOException {
        if (!Files.isRegularFile(file)) {
            return 0;
        }

        List<String> entries = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        return store.load(entries);
    }

    /**
     * Writes a store's passwords out, replacing whatever was there.
     *
     * @throws IOException if the file cannot be written
     */
    public void save(PasswordStore store) throws IOException {
        Files.createDirectories(file.getParent());

        Path pending = file.resolveSibling(FILE_NAME + ".new");
        List<String> lines = new java.util.ArrayList<>(HEADER);
        lines.addAll(store.save());

        Files.write(pending, lines, StandardCharsets.UTF_8);
        try {
            Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            // Some filesystems cannot promise an atomic move. A plain replace is still better than
            // writing over the file in place.
            Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
