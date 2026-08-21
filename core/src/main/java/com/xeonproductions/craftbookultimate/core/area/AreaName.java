// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.area;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * What a saved area is called.
 *
 * <p>Two parts: whose it is, and which of theirs it is. A player's areas live under their own
 * name and are theirs to save and delete; the areas everybody shares live under {@code GLOBAL}
 * and take a permission to make.
 *
 * <p>An identifier is written on a sign, so it is short and made of the characters that survive
 * being read off one. It is also folded to lower case, because the same area written two ways on
 * two signs has always been the same area.
 *
 * @param namespace whose area it is
 * @param id which of theirs
 */
@NullMarked
public record AreaName(String namespace, String id) {

    /** The namespace the areas everybody shares live under. */
    public static final String GLOBAL = "GLOBAL";

    /** What a sign writes in place of a second area to mean there is not one. */
    public static final String NONE = "--";

    /**
     * What an identifier may be made of.
     *
     * <p>Thirteen characters, which is what fits on a sign line with the dashes a toggled area
     * wears around it.
     */
    private static final Pattern USABLE_ID = Pattern.compile("[A-Za-z0-9_]{1,13}");

    /** What a namespace may be made of, which is what a player may be called. */
    private static final Pattern USABLE_NAMESPACE = Pattern.compile("[A-Za-z0-9_]{1,32}");

    /** Trims both halves and folds the identifier to lower case. */
    public AreaName {
        namespace = namespace.trim();
        id = id.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Reads an identifier as a sign writes it.
     *
     * <p>A toggled area wears dashes to say which of its two halves is standing, so the dashes
     * are not part of the name and are taken off before anything is looked up.
     *
     * @param namespace whose area it is
     * @param written the identifier as it appears on the sign
     * @return the name, or empty if the line names nothing usable
     */
    public static Optional<AreaName> parse(String namespace, String written) {
        String id = written.replace("-", "").trim();
        if (!isUsableId(id) || !isUsableNamespace(namespace)) {
            return Optional.empty();
        }
        return Optional.of(new AreaName(namespace, id));
    }

    /** Whether an identifier is one an area may be saved under. */
    public static boolean isUsableId(String id) {
        return USABLE_ID.matcher(id).matches();
    }

    /** Whether a namespace is one areas may be kept under. */
    public static boolean isUsableNamespace(String namespace) {
        return USABLE_NAMESPACE.matcher(namespace).matches();
    }

    /** Whether a sign line names a second area at all. */
    public static boolean namesSomething(String written) {
        String stripped = written.replace("-", "").trim();
        return !stripped.isEmpty();
    }

    /** Whether this is one of the areas everybody shares. */
    public boolean isGlobal() {
        return namespace.equalsIgnoreCase(GLOBAL);
    }

    @Override
    public String toString() {
        return namespace + "/" + id;
    }
}
