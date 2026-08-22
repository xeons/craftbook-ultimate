// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the teleporters.
 *
 * @param buttons whether a button opposite a teleporter sign works it
 * @param requireSign whether the far end of a teleport needs a sign of its own
 * @param range how far a teleporter may send somebody, or a negative number for no limit
 */
@NullMarked
public record TeleporterSettings(boolean buttons, boolean requireSign, double range) {

    /** The teleporters as they have always sent people. */
    public static final TeleporterSettings DEFAULTS = new TeleporterSettings(true, true, -1);

    /** These settings with hidden-sign buttons allowed or refused. */
    public TeleporterSettings withButtons(boolean allowed) {
        return new TeleporterSettings(allowed, requireSign, range);
    }

    /** These settings with the far end needing a sign or not. */
    public TeleporterSettings withRequireSign(boolean required) {
        return new TeleporterSettings(buttons, required, range);
    }

    /** These settings with teleporters reaching a different distance. */
    public TeleporterSettings withRange(double reach) {
        return new TeleporterSettings(buttons, requireSign, reach);
    }
}
