// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the pipes.
 *
 * <p>A pipe is followed from its input every time that input is powered, and the answer is kept
 * until one of its blocks changes, so the limit here is on how large a pipe may be rather than on
 * how often it may run. A pipe past its limit carries items as far as the limit reaches instead of
 * refusing to work, which is the same bargain every other limit in the plugin makes.
 *
 * @param enabled whether pipes run at all
 * @param maxLength how many blocks of pipe are followed before the search gives up
 * @param stackPerPull whether one pulse moves a single stack or empties what it is pointed at
 */
@NullMarked
public record PipeSettings(boolean enabled, int maxLength, boolean stackPerPull) {

    /** The fewest blocks a pipe may be allowed, which is enough for an input and one way out. */
    private static final int MIN_LENGTH = 2;

    /** How far the fork followed a pipe, counting the blocks it searched through. */
    public static final int CUSTOMARY_LENGTH = 150;

    /** Pipes as the fork ran them. */
    public static final PipeSettings DEFAULTS = new PipeSettings(true, CUSTOMARY_LENGTH, true);

    /** Holds the limit to something a pipe can be built within. */
    public PipeSettings {
        maxLength = Math.max(MIN_LENGTH, maxLength);
    }

    /** These settings with pipes running, or not. */
    public PipeSettings withEnabled(boolean enabled) {
        return new PipeSettings(enabled, maxLength, stackPerPull);
    }

    /** These settings with a different limit on how large a pipe may be. */
    public PipeSettings withMaxLength(int blocks) {
        return new PipeSettings(enabled, blocks, stackPerPull);
    }

    /** These settings with a pulse moving one stack, or everything it can reach. */
    public PipeSettings withStackPerPull(boolean stackPerPull) {
        return new PipeSettings(enabled, maxLength, stackPerPull);
    }
}
