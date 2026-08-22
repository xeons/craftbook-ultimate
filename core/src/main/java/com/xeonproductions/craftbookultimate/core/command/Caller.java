// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Whoever typed a command, as the thing carrying it out sees them.
 *
 * <p>Four questions cover every command this plugin has, and none of them needs a server: what to
 * say back, what the caller is allowed to do, what they are called — which is what decides whose
 * variables are whose — and where they are standing, which only the chip check uses and only to put
 * the nearest one first.
 *
 * <p>The console is a caller like any other. It has a name and every permission, and it is standing
 * nowhere, which is why {@link #standing()} answers nothing rather than a position in a world it is
 * not in.
 */
@NullMarked
public interface Caller {

    /** Says something back to whoever typed the command. */
    void send(Component message);

    /** Whether they are allowed to do a thing. */
    boolean may(String permission);

    /** What they are called, which is the namespace their own variables live in. */
    String name();

    /** Where they are, or nothing at all for a caller that is not in a world. */
    Optional<Standing> standing();

    /** Says that something happened. */
    default void tell(String message) {
        send(Component.text(message, NamedTextColor.GREEN));
    }

    /** Says that it did not, and why. */
    default void refuse(String message) {
        send(Component.text(message, NamedTextColor.RED));
    }

    /** Titles a listing. */
    default void heading(String message) {
        send(Component.text(message, NamedTextColor.YELLOW));
    }

    /** One line under a heading. */
    default void detail(String message) {
        send(Component.text(message, NamedTextColor.GRAY));
    }
}
