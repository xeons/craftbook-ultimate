// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * How a block written in the settings file becomes a block.
 *
 * <p>Two things only the server can answer: what a name means, including the pre-flattening
 * spellings a file may carry, and what is currently in one of its block tags. Everything else about
 * reading the file is the same on any platform, which is why only this much is a seam.
 */
@NullMarked
public interface BlockNames {

    /** Marks a list entry that names a block tag rather than a single block. */
    char TAG_MARKER = '#';

    /** What a single written name means, or nothing where it names nothing. */
    Optional<Key> block(String written);

    /** Every block currently in one of the server's tags, empty where there is no such tag. */
    Set<Key> tagged(String tag);

    /**
     * Reads a list of blocks, each named directly or by a tag.
     *
     * <p>A tag is expanded to whatever the server currently has in it, so a list naming
     * {@code #minecraft:planks} gains any plank a later version of the game adds without the file
     * being touched.
     *
     * <p>An entry that means nothing is reported and skipped rather than taking the rest of the
     * list down with it — one misspelling in a long list should cost that one block.
     */
    default Set<Key> blocks(List<String> written, Consumer<String> report) {
        Set<Key> blocks = new LinkedHashSet<>();

        for (String entry : written) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.charAt(0) == TAG_MARKER) {
                Set<Key> tagged = tagged(trimmed.substring(1));
                if (tagged.isEmpty()) {
                    report.accept("No block tag called " + trimmed + ", so it allows nothing");
                }
                blocks.addAll(tagged);
                continue;
            }

            Optional<Key> block = block(trimmed);
            if (block.isEmpty()) {
                report.accept("No block called " + trimmed + ", so it allows nothing");
                continue;
            }
            blocks.add(block.get());
        }

        return blocks;
    }
}
