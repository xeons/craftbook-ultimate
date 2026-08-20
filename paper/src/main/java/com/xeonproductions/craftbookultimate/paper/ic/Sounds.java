package com.xeonproductions.craftbookultimate.paper.ic;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jspecify.annotations.NullMarked;

/**
 * The shorthand a sound effect chip's sign names a sound by.
 *
 * <p>The shorthand is the first two letters of each part of the sound's name run together and
 * upper-cased, so {@code entity.creeper.primed} is written {@code ENCRPR}. It is not unique — a
 * sound is only six or eight letters of its name — so where two sounds share one, the first in the
 * server's own order answers to it and the other has to be named in full.
 *
 * <p>Worked out from the sounds the server actually has, so a sound added by a later version can
 * be named the moment it exists.
 */
@NullMarked
final class Sounds {

    /** How many letters of each part of a name go into the shorthand. */
    private static final int LETTERS_PER_WORD = 2;

    /** What separates the parts of a sound's name. */
    private static final String WORD_SEPARATORS = "[._]";

    /** Built once, because walking the whole sound registry per sign would be wasteful. */
    private static final Map<String, Key> BY_SHORTHAND = build();

    private Sounds() {}

    /** The sound a shorthand names, or empty if nothing answers to it. */
    static Optional<Key> byShorthand(String written) {
        return Optional.ofNullable(BY_SHORTHAND.get(written.trim().toUpperCase(Locale.ROOT)));
    }

    /** The shorthand a sound is named by. */
    static String shorthandFor(Key sound) {
        StringBuilder shorthand = new StringBuilder();
        for (String word : sound.value().split(WORD_SEPARATORS)) {
            if (!word.isEmpty()) {
                shorthand.append(word, 0, Math.min(LETTERS_PER_WORD, word.length()));
            }
        }
        return shorthand.toString().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Key> build() {
        Map<String, Key> byShorthand = new LinkedHashMap<>();
        for (Sound sound : Registry.SOUND_EVENT) {
            Key key = Registry.SOUND_EVENT.getKey(sound);
            if (key != null) {
                byShorthand.putIfAbsent(shorthandFor(key), key);
            }
        }
        return Map.copyOf(byShorthand);
    }
}
