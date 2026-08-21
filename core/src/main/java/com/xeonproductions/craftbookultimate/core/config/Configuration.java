// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * The settings currently in force.
 *
 * <p>{@link Settings} is a value and never changes; this is the one place that says which value
 * is the current one. An operator rereading the configuration replaces it, and every chip picks
 * the new one up the next time it runs, without anything being reloaded or rebuilt.
 *
 * <p>Chips read this from any region's thread, so the current value is held where a write on one
 * thread is seen by a read on another.
 */
@NullMarked
public final class Configuration {

    private volatile Settings settings;

    /** Starts out with nothing configured. */
    public Configuration() {
        this(Settings.DEFAULTS);
    }

    /** Starts out with a particular set, which is how a test stands one up. */
    public Configuration(Settings settings) {
        this.settings = settings;
    }

    /** The settings in force. */
    public Settings settings() {
        return settings;
    }

    /** Puts a freshly read set of settings in force. */
    public void replaceWith(Settings settings) {
        this.settings = settings;
    }
}
