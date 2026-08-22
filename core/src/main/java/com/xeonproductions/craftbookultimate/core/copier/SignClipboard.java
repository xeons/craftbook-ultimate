// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.copier;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * What each player has copied off a sign, waiting to be put on another.
 *
 * <p>The sign copier is not a mechanism. Nothing is built, no sign says what it is, and what makes
 * it work is an item in somebody's hand — so what it needs remembering is per player rather than
 * per place, and it lives here rather than in the world.
 *
 * <p>Held only while the server is up. A clipboard is a thing somebody is in the middle of doing,
 * and one restored a week later would paste text its owner had long forgotten copying.
 */
@NullMarked
public final class SignClipboard {

    /** The lines a sign has, and so the largest line number one may be told to edit. */
    public static final int LINES = SignLines.LINE_COUNT;

    private final Map<UUID, SignLines> copied = new HashMap<>();

    /** Remembers what a player has just copied. */
    public void put(UUID player, SignLines lines) {
        copied.put(player, lines);
    }

    /** What a player has copied, or nothing where they have copied nothing yet. */
    public Optional<SignLines> get(UUID player) {
        return Optional.ofNullable(copied.get(player));
    }

    /** Forgets what a player copied, which is what leaving the server does. */
    public void forget(UUID player) {
        copied.remove(player);
    }

    /** How many players are holding something, which is only ever asked by a test. */
    public int size() {
        return copied.size();
    }

    /**
     * Changes one line of what a player has copied.
     *
     * <p>Answers whether it could be done: a line number outside the sign, or a player with
     * nothing copied, both mean there is nothing to change.
     *
     * @param line the line as a builder counts them, from 1
     */
    public boolean edit(UUID player, int line, String text) {
        if (line < 1 || line > LINES) {
            return false;
        }
        SignLines held = copied.get(player);
        if (held == null) {
            return false;
        }
        copied.put(player, held.withLine(line - 1, text));
        return true;
    }
}
