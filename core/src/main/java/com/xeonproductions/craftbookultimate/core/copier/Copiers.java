// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.copier;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import org.jspecify.annotations.NullMarked;

/**
 * The three signs that hand out copies of something.
 *
 * <p>Each is a wall sign on a bookshelf with something to copy beside it: a banner in the blocks
 * above or below, a written book in an item frame, or a map number written on the sign itself. A
 * player holding a blank of the right sort right-clicks the sign or the bookshelf behind it and
 * gets a copy, one blank at a time.
 *
 * <p>Only the rules are here. What a banner's patterns are, what a book's pages say and how a map
 * is numbered are all things only a server can answer, so the copying itself is a binding's job.
 * What is worth stating once is the grammar — which names are claimed, and the two limits that
 * decide whether a copy may be made at all.
 */
@NullMarked
public final class Copiers {

    /** The sign that copies a banner. */
    public static final String BANNER_SIGN = "[BannerCopier]";

    /** The sign that copies a written book out of an item frame. */
    public static final String BOOK_SIGN = "[BookCopier]";

    /** The sign that hands out a numbered map. */
    public static final String MAP_SIGN = "[Map]";

    /** Every name these three claim. */
    public static final List<String> SIGN_NAMES = List.of(BANNER_SIGN, BOOK_SIGN, MAP_SIGN);

    /**
     * The mechanic a sign belongs to, which is what an operator switches off.
     *
     * <p>Not the same string as the sign name, and the map sign is not even a shortening of it:
     * a builder writes {@code [Map]} on a sign and an operator writes {@code MapCopier} in the
     * settings.
     */
    public static String mechanicOf(String signName) {
        return switch (signName) {
            case BANNER_SIGN -> Mechanics.BANNER_COPIER;
            case BOOK_SIGN -> Mechanics.BOOK_COPIER;
            default -> Mechanics.MAP_COPIER;
        };
    }

    /**
     * How many patterns a banner may carry and still be copied.
     *
     * <p>The game's own limit for a banner made at a loom, and the reason the check exists: a
     * banner built past it by other means cannot be handed out as an item that anybody could have
     * made, so it is refused rather than quietly truncated.
     */
    public static final int MAX_BANNER_PATTERNS = 6;

    /** The line a map sign carries its number on, which is the first. */
    public static final int MAP_NUMBER_LINE = 0;

    private Copiers() {
    }

    /** Which of the three a sign is, if it is one of them at all. */
    public static Optional<String> claimed(String nameLine) {
        String written = nameLine.trim();
        for (String name : SIGN_NAMES) {
            if (written.equalsIgnoreCase(name)) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
    }

    /**
     * The map a sign names, or nothing where its first line is not a map number.
     *
     * <p>Held to what a map may actually be numbered. The legacy fork wrote whatever was on the
     * sign straight into the item and let a number too large make a map that was not there; a sign
     * that could never work is better refused as it is written.
     */
    public static OptionalInt mapNumber(String line) {
        String written = line.trim();
        if (written.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            int number = Integer.parseInt(written);
            return number < 0 ? OptionalInt.empty() : OptionalInt.of(number);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /** The permission to build one, which is the same shape the sign mechanics use. */
    public static String buildPermission(String signName) {
        return "craftbook." + bareName(signName);
    }

    /** The permission to use one. */
    public static String usePermission(String signName) {
        return buildPermission(signName) + ".use";
    }

    /** A sign name with its brackets off and in lower case, for building a permission out of. */
    private static String bareName(String signName) {
        return signName.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
    }
}
