// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * How everything a player rides behaves, with nothing built and no sign anywhere.
 *
 * <p>This is what the rest of the settings are not. Everything else in the file is about a
 * mechanism — a block with a sign on it saying what to do, which does nothing at all until somebody
 * builds one. Everything under here changes what an ordinary cart or an ordinary boat does anywhere
 * in the world, which is why **all of it is off out of the box**: a server that has never been
 * configured runs both exactly as the game does.
 *
 * <p>Carts and boats are kept apart rather than merged, though three habits are word for word the
 * same on both. They were separate in the fork this is ported from, and they should stay separate:
 * an operator clearing abandoned carts off their stations has said nothing whatever about the boats
 * on their lake, and one setting for both would put words in their mouth.
 *
 * <p>What they share is a shape rather than a value. Both decay when left empty, both can be taken
 * away when their rider steps out, and both can hurt what they run into — so the two records read
 * alike and the decisions in {@code core/cart/} and {@code core/boat/} are written alike.
 *
 * @param carts how every minecart behaves, whatever it is standing on
 * @param boats how every boat behaves, wherever it is floating
 */
@NullMarked
public record VehicleHabits(CartHabits carts, BoatHabits boats) {

    /** Nothing switched on at all: both behave as the game runs them. */
    public static final VehicleHabits DEFAULTS =
            new VehicleHabits(CartHabits.DEFAULTS, BoatHabits.DEFAULTS);

    /** These habits with the carts changed. */
    public VehicleHabits withCarts(CartHabits changed) {
        return new VehicleHabits(changed, boats);
    }

    /** These habits with the boats changed. */
    public VehicleHabits withBoats(BoatHabits changed) {
        return new VehicleHabits(carts, changed);
    }
}
