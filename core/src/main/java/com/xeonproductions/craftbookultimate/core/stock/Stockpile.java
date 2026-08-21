// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.stock;

import java.util.Map;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Somewhere a mechanic takes materials from and gives them back to.
 *
 * <p>A mechanic that builds something has to get the blocks from somewhere and has to put them
 * back when it takes the structure down again. A stockpile is that somewhere: usually the chests
 * a player left nearby, sometimes an unlimited supply for an admin build, sometimes a store held
 * on the mechanic itself.
 *
 * <p>Items are named by their namespaced key, so a stockpile deals in kinds and counts rather
 * than in stacks. How those counts are spread across slots is the implementation's business.
 *
 * <p>An implementation backed by containers in the world belongs to one region and must only be
 * used from the thread owning them.
 */
@NullMarked
public interface Stockpile {

    /** How many of an item this stockpile holds. */
    int count(Key item);

    /**
     * Takes items out.
     *
     * <p>Takes as many as it can and reports how many that was, which may be fewer than asked
     * for. A mechanic that must not half-consume its materials should use {@link #takeAll} instead.
     *
     * @param item what to take
     * @param amount how many to take
     * @return how many were actually taken
     */
    int take(Key item, int amount);

    /**
     * Puts items in.
     *
     * @param item what to give
     * @param amount how many to give
     * @return how many would not fit, which are the caller's problem to deal with
     */
    int give(Key item, int amount);

    /** Everything held, by kind. An unlimited stockpile reports nothing, since it holds no total. */
    Map<Key, Integer> contents();

    /**
     * Whether this stockpile is bottomless.
     *
     * <p>An unlimited stockpile always has what is asked for and always accepts what is given, so
     * a mechanic backed by one never fails for want of materials.
     */
    default boolean isUnlimited() {
        return false;
    }

    /** Whether this stockpile holds at least a number of an item. */
    default boolean has(Key item, int amount) {
        return isUnlimited() || count(item) >= amount;
    }

    /**
     * Takes items out, but only if all of them are available.
     *
     * <p>This is what a mechanic wants when a partial withdrawal would leave a half-built
     * structure and a hole in someone's chest.
     *
     * @return true if the items were taken
     */
    default boolean takeAll(Key item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!has(item, amount)) {
            return false;
        }
        return take(item, amount) == amount;
    }

    /** Whether there is room for a number of an item. */
    default boolean hasRoomFor(Key item, int amount) {
        return isUnlimited() || amount <= 0 || countRoomFor(item) >= amount;
    }

    /**
     * How many of an item could still be given before running out of room.
     *
     * <p>Defaults to reporting no room, so an implementation that cannot answer cheaply is
     * treated as full rather than pretending to have space it does not have.
     */
    default int countRoomFor(Key item) {
        return 0;
    }
}
