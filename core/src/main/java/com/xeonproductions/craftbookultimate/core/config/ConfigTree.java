// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/**
 * A settings file, as the thing reading it needs to see one.
 *
 * <p>Paths are dotted — {@code ics.max-radius} — which is how both YAML libraries in use here
 * already address a nested value, and is the whole of what {@link ConfigDocument} needs to know
 * about either of them.
 *
 * <p>Every getter takes what to answer when the file says nothing, so a file missing a setting and
 * a file that never had it behave the same way. That is what lets the same document be read from a
 * blank tree, which is how a fresh server gets a complete file written for it.
 */
@NullMarked
public interface ConfigTree {

    /** Whether the file already says something at a path. */
    boolean has(String path);

    /** Writes a value, replacing whatever was there. */
    void set(String path, Object value);

    /** Writes the lines explaining a setting, replacing whatever was there. */
    void comment(String path, List<String> lines);

    /** Writes the lines at the top of the whole file. */
    void header(List<String> lines);

    boolean bool(String path, boolean fallback);

    String text(String path, String fallback);

    int integer(String path, int fallback);

    long count(String path, long fallback);

    double number(String path, double fallback);

    /** A list of strings, empty where the file says nothing. */
    List<String> strings(String path);

    /**
     * The names directly under a path, in the order the file gives them.
     *
     * <p>Used where what an operator may write is not known in advance — which cart mechanic is
     * built from which block, and which blocks boost a cart — so the names are the data.
     */
    Set<String> childrenOf(String path);
}
