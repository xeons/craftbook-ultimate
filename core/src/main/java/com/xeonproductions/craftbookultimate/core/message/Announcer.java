// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.message;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * Everybody a chip can speak to who is not standing in front of it.
 *
 * <p>A chip speaking to somebody nearby already has them: it asks the world what is around and
 * tells each one in turn. This is the other kind of speaking, where the audience is the server
 * rather than a place — everyone online, one player wherever they happen to be, or the operator
 * reading the log.
 *
 * <p>That distinction is what makes this safe on a regionised server. Nothing here reaches into
 * anybody's blocks; a name and a piece of text are all that cross, and both are immutable, so a
 * chip in one region may speak to a player in another without either thread touching the other's
 * world.
 */
@NullMarked
public interface Announcer {

    /** Says something to everybody online. */
    void toEveryone(Component message);

    /**
     * Says something to one player, wherever on the server they are.
     *
     * @param name the account name, matched exactly
     * @return whether they were online to hear it
     */
    boolean toNamed(String name, Component message);

    /**
     * Writes a line to the server's log.
     *
     * <p>Plain text rather than a component, because a log file has no colours and an operator
     * grepping one wants the characters they see on the sign.
     */
    void toLog(String line);
}
