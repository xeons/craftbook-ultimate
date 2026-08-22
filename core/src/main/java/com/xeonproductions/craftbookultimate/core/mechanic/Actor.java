// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Whoever set a mechanic off.
 *
 * <p>A mechanic worked by hand has one and a mechanic worked by redstone has none, which is the
 * whole of the difference between the two as far as the mechanics are concerned: an actor is
 * somebody to refuse, somebody to explain the refusal to, and in the lift's case somebody to
 * carry.
 */
@NullMarked
public interface Actor {

    /** What they are called. */
    String name();

    /** Tells them something. */
    void tell(Component message);

    /** Whether they are allowed to do something. */
    boolean mayUse(String permission);

    /**
     * Whether they are crouching.
     *
     * <p>Crouching is how somebody says they want to place a block against a gate rather than
     * work the gate, so the mechanics that answer to a click on their own material leave a
     * crouching player alone.
     */
    boolean isSneaking();

    /** Where they are standing, or nothing if they are not in the world. */
    Optional<Vec3i> position();

    /**
     * What they are holding, in either hand.
     *
     * <p>Both hands together rather than one at a time, because every mechanic that asks this asks
     * whether somebody has a thing on them, not which hand it is in. Only what sort of item it is,
     * since that is all a hidden switch's key is.
     */
    default Set<Key> held() {
        return Set.of();
    }

    /**
     * Sends them somewhere.
     *
     * <p>A landing facing {@link com.xeonproductions.craftbookultimate.core.math.BlockFace#SELF}
     * leaves them looking the way they already were, which is what a lift wants.
     *
     * @return true if the journey started
     */
    boolean moveTo(Landing landing);

    /** Tells them something has gone wrong. */
    default void complain(String message) {
        tell(Component.text(message, NamedTextColor.RED));
    }

    /** Tells them how something went. */
    default void inform(String message) {
        tell(Component.text(message, NamedTextColor.YELLOW));
    }
}
