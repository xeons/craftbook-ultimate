// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.boat;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * A boat, as the habits see one.
 *
 * <p>Deliberately smaller than {@link com.xeonproductions.craftbookultimate.core.cart.Cart}. A cart
 * runs on a mechanism and needs to be asked where it is going and what it is carrying; a boat has
 * no mechanisms at all in this plugin, so the only questions are the ones the habits ask.
 */
@NullMarked
public interface Boat {

    /** Where it is. */
    Vec3d position();

    /** Which way and how fast it is going. */
    Vec3d velocity();

    /** Whoever is aboard, which is empty for a boat nobody is in. */
    List<Bystander> riders();

    /** Whether it is still in the world rather than already gone. */
    boolean isPresent();

    /** Whether anybody is aboard. */
    default boolean isOccupied() {
        return !riders().isEmpty();
    }
}
