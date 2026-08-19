package com.xeonproductions.craftbookultimate.core.radio;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The channel a wireless transmitter shouts on and a receiver listens to.
 *
 * <p>A band has two parts, both written on the sign. The narrow band is the channel name and is
 * what a builder picks; the wide band is an optional namespace around it, so two people can each
 * have a channel called {@code door} without one working the other's door. Writing {@code uuid}
 * as the wide band substitutes the creating player's own unique id, which is how a builder gets a
 * namespace nobody else can transmit into by accident.
 *
 * <p>Bands are compared by both parts together and are shared across worlds, so a transmitter in
 * one world drives a receiver in another.
 *
 * @param wide the namespace, or empty for the shared one
 * @param narrow the channel name, which is never blank
 */
@NullMarked
public record Band(String wide, String narrow) {

    /** Separates the two halves when a band is written out for a person to read. */
    private static final char SEPARATOR = '/';

    public Band {
        wide = wide.trim();
        narrow = narrow.trim();
        if (narrow.isEmpty()) {
            throw new IllegalArgumentException("A band must have a channel name");
        }
    }

    /** A band in the shared namespace. */
    public static Band named(String narrow) {
        return new Band("", narrow);
    }

    /**
     * Reads a band off a sign.
     *
     * <p>A sign with no channel name names no band at all, rather than naming a blank one that
     * every other unnamed chip would also be on.
     *
     * @param wide the namespace line, which may be blank
     * @param narrow the channel name line
     * @return the band, or empty if no channel was named
     */
    public static Optional<Band> parse(String wide, String narrow) {
        if (narrow.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Band(wide, narrow));
    }

    /** This band with something appended to its channel name. */
    public Band withSuffix(String suffix) {
        return new Band(wide, narrow + suffix);
    }

    @Override
    public String toString() {
        return wide.isEmpty() ? narrow : wide + SEPARATOR + narrow;
    }
}
