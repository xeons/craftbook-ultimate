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
 * How a name written in the settings file becomes a block or an item.
 *
 * <p>Two things only the server can answer: what a name means, including the pre-flattening
 * spellings a file may carry, and what is currently in one of its block tags. Everything else about
 * reading the file is the same on any platform, which is why only this much is a seam.
 *
 * <p>Blocks and items are asked for separately because an id meant either one before the
 * flattening, and because half the things a setting names are not blocks at all: a light meter is
 * a pinch of glowstone dust and a tree lopper is worked with an axe. Asking for a block by a name
 * that means an item answers nothing, which reads as a misspelling.
 */
@NullMarked
public interface BlockNames {

    /** Marks a list entry that names a block tag rather than a single block. */
    char TAG_MARKER = '#';

    /** What a single written name means as a block, or nothing where it names no block. */
    Optional<Key> block(String written);

    /**
     * What a single written name means as an item, or nothing where it names no item.
     *
     * <p>Answers as {@link #block} does by default, which is right for a platform where the two
     * are the same lookup and wrong for one where they are not.
     */
    default Optional<Key> item(String written) {
        return block(written);
    }

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
        return named(written, report, this::block, "block");
    }

    /**
     * Reads a list of items, each named directly.
     *
     * <p>No tags: the tags a server publishes for these lists are block tags, and an item tag of
     * the same name is a different thing that may or may not exist. A list of tools is short
     * enough to write out.
     */
    default Set<Key> items(List<String> written, Consumer<String> report) {
        return named(written, report, this::item, "item");
    }

    private Set<Key> named(
            List<String> written,
            Consumer<String> report,
            java.util.function.Function<String, Optional<Key>> lookup,
            String what) {
        Set<Key> found = new LinkedHashSet<>();

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
                found.addAll(tagged);
                continue;
            }

            Optional<Key> one = lookup.apply(trimmed);
            if (one.isEmpty()) {
                report.accept("No " + what + " called " + trimmed + ", so it allows nothing");
                continue;
            }
            found.add(one.get());
        }

        return found;
    }
}
