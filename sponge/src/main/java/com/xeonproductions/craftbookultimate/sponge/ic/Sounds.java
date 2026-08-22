// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.registry.RegistryTypes;

/**
 * The shorthand a sound effect sign names a sound by.
 *
 * <p>Two letters of each word, so {@code entity.creeper.primed} is {@code ENCRPR}. Worked out from
 * the sounds the server actually has rather than held as a list, so a sound added by a later
 * version is nameable without anything here changing.
 */
@NullMarked
final class Sounds {

    private static final int LETTERS_PER_WORD = 2;

    private static final String WORD_SEPARATORS = "[._]";

    private static final Map<String, Key> BY_SHORTHAND = build();

    private Sounds() {}

    static Optional<Key> byShorthand(String written) {
        return Optional.ofNullable(BY_SHORTHAND.get(written.trim().toUpperCase(Locale.ROOT)));
    }

    static String shorthandFor(Key sound) {
        StringBuilder shorthand = new StringBuilder();
        for (String word : sound.value().split(WORD_SEPARATORS)) {
            if (!word.isEmpty()) {
                shorthand.append(word, 0, Math.min(LETTERS_PER_WORD, word.length()));
            }
        }
        return shorthand.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * Every sound, indexed by its shorthand.
     *
     * <p>First one wins where two sounds shorten to the same thing, which keeps a name meaning
     * what it meant rather than whichever sound the registry happened to list last.
     */
    private static Map<String, Key> build() {
        Map<String, Key> byShorthand = new LinkedHashMap<>();
        RegistryTypes.SOUND_TYPE.get().streamEntries().forEach(entry ->
                byShorthand.putIfAbsent(shorthandFor(entry.key()), entry.key()));
        return Map.copyOf(byShorthand);
    }
}
