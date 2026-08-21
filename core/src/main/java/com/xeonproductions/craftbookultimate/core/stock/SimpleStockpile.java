// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.stock;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A stockpile held as counts in memory.
 *
 * <p>Used two ways: as the store a mechanic keeps on itself, and as the thing a test asserts
 * against when checking that a mechanic paid for what it built.
 *
 * <p>Capacity is optional. Without one the pile is bottomless in the giving direction but still
 * has to actually hold something before it can be taken from, which is what a store attached to a
 * mechanic behaves like.
 *
 * <p>Instances are not thread safe.
 */
@NullMarked
public final class SimpleStockpile implements Stockpile {

    /** Means no limit on how much of one kind may be held. */
    private static final int UNLIMITED_CAPACITY = -1;

    private final Map<Key, Integer> counts = new HashMap<>();
    private final int capacityPerItem;

    private SimpleStockpile(int capacityPerItem) {
        this.capacityPerItem = capacityPerItem;
    }

    /** An empty stockpile that will hold as much as it is given. */
    public static SimpleStockpile empty() {
        return new SimpleStockpile(UNLIMITED_CAPACITY);
    }

    /**
     * An empty stockpile that will hold only so much of each kind.
     *
     * @param capacityPerItem the most of any one kind it will accept
     */
    public static SimpleStockpile withCapacity(int capacityPerItem) {
        if (capacityPerItem < 0) {
            throw new IllegalArgumentException("Capacity must not be negative, got " + capacityPerItem);
        }
        return new SimpleStockpile(capacityPerItem);
    }

    @Override
    public int count(Key item) {
        return counts.getOrDefault(item, 0);
    }

    @Override
    public int take(Key item, int amount) {
        if (amount <= 0) {
            return 0;
        }

        int held = count(item);
        int taken = Math.min(held, amount);
        if (taken == held) {
            counts.remove(item);
        } else {
            counts.put(item, held - taken);
        }
        return taken;
    }

    @Override
    public int give(Key item, int amount) {
        if (amount <= 0) {
            return 0;
        }

        int accepted = capacityPerItem == UNLIMITED_CAPACITY
                ? amount
                : Math.min(amount, countRoomFor(item));

        if (accepted > 0) {
            counts.merge(item, accepted, Integer::sum);
        }
        return amount - accepted;
    }

    @Override
    public int countRoomFor(Key item) {
        return capacityPerItem == UNLIMITED_CAPACITY
                ? Integer.MAX_VALUE
                : Math.max(0, capacityPerItem - count(item));
    }

    @Override
    public Map<Key, Integer> contents() {
        return Map.copyOf(counts);
    }

    /** Puts items in without regard for capacity, for setting up a starting state. */
    public SimpleStockpile with(Key item, int amount) {
        if (amount > 0) {
            counts.merge(item, amount, Integer::sum);
        }
        return this;
    }

    /** Whether nothing at all is held. */
    public boolean isEmpty() {
        return counts.isEmpty();
    }

    @Override
    public String toString() {
        return "SimpleStockpile" + counts;
    }
}
