// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.entity;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What the item sensors are looking for.
 *
 * <p>Each line describing an item is one thing to check, written as {@code PARAM:VALUE}:
 *
 * <pre>
 *   ID:35@14      red wool
 *   STACK:64      a full stack
 *   NAME:Key      renamed exactly "Key"
 *   LORE:quest    with "quest" written somewhere on it
 * </pre>
 *
 * <p>A sign has room for one; a book has room for several, and a stack has to satisfy all of them.
 * Criteria that nothing was said about are not checked, so an empty set matches any item at all.
 *
 * @param item what the item must be
 * @param stackSize how many the stack must hold
 * @param displayName the name it must have been given, matched in full
 * @param lore a fragment that must appear somewhere in what is written on it
 */
@NullMarked
public record ItemCriteria(
        Optional<Key> item, OptionalInt stackSize, Optional<String> displayName, Optional<String> lore) {

    /** Separates the sort of check from what is being checked for. */
    private static final char FIELD_SEPARATOR = ':';

    /** The largest stack the game moves in one go. */
    private static final int MAX_STACK = 64;

    /** Nothing asked for, which matches any item. */
    public static final ItemCriteria ANY =
            new ItemCriteria(Optional.empty(), OptionalInt.empty(), Optional.empty(), Optional.empty());

    /** Whether this asks anything at all. */
    public boolean isAny() {
        return item.isEmpty() && stackSize.isEmpty() && displayName.isEmpty() && lore.isEmpty();
    }

    /**
     * Adds one more thing to check.
     *
     * <p>A second check of a sort already given replaces the first rather than being refused, so a
     * book with a stray repeated line still describes something.
     *
     * @param written one line, as {@code PARAM:VALUE}
     * @param items how to work out which item a name means
     * @return the widened criteria, or empty if the line says nothing usable
     */
    public Optional<ItemCriteria> and(String written, Function<String, Optional<Key>> items) {
        String trimmed = written.trim();
        int separator = trimmed.indexOf(FIELD_SEPARATOR);
        if (separator < 0) {
            return Optional.empty();
        }

        String sort = trimmed.substring(0, separator).trim().toUpperCase(Locale.ROOT);
        String value = trimmed.substring(separator + 1).trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }

        return switch (sort) {
            case "ID" -> items.apply(value)
                    .map(found -> new ItemCriteria(Optional.of(found), stackSize, displayName, lore));
            case "STACK" -> countOf(value)
                    .map(count -> new ItemCriteria(item, OptionalInt.of(count), displayName, lore));
            case "NAME" ->
                    Optional.of(new ItemCriteria(item, stackSize, Optional.of(value), lore));
            case "LORE" ->
                    Optional.of(new ItemCriteria(item, stackSize, displayName, Optional.of(value)));
            default -> Optional.empty();
        };
    }

    /** Reads one line into a fresh set of criteria. */
    public static Optional<ItemCriteria> parse(String written, Function<String, Optional<Key>> items) {
        return ANY.and(written, items);
    }

    /** Whether a stack is what these criteria describe. */
    public boolean matches(ItemView stack) {
        if (item.isPresent() && !item.get().equals(stack.type())) {
            return false;
        }
        if (stackSize.isPresent() && stackSize.getAsInt() != stack.count()) {
            return false;
        }
        if (displayName.isPresent() && !displayName.equals(stack.displayName())) {
            return false;
        }
        return lore.isEmpty() || stack.loreContains(lore.get());
    }

    private static Optional<Integer> countOf(String written) {
        try {
            int count = Integer.parseInt(written);
            return count < 0 || count > MAX_STACK ? Optional.empty() : Optional.of(count);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
