// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

/**
 * Where each passenger has said they are going.
 *
 * <p>A rider sets a destination by name, and every station and junction between here and there
 * reads it: a sort sign sends the cart the way its destination lies, and a station holds the cart
 * only if the name is its own. That is the whole of the routing, and it is one string per player.
 *
 * <p>Held here rather than looked up from the rails because the two ends are nowhere near each
 * other. A junction reads a name a player set at the other end of a railway, on another thread,
 * without either reaching into the other's blocks.
 *
 * <p>A destination lasts as long as the session. Nothing is written to disk, which is why a
 * station tells a rider they will have to set it again after a restart.
 */
@NullMarked
public final class Stations {

    /** What a glob stands in for: any run of characters, including none. */
    private static final char ANY = '*';

    private final Map<UUID, String> destinations = new ConcurrentHashMap<>();

    /**
     * Sets where somebody is going.
     *
     * @param traveller the rider's unique id
     * @param station the destination's name, which is compared without regard to case
     */
    public void setDestination(UUID traveller, String station) {
        destinations.put(traveller, station.toLowerCase(Locale.ROOT));
    }

    /** Where somebody is going, if they have said. */
    public Optional<String> destination(UUID traveller) {
        return Optional.ofNullable(destinations.get(traveller));
    }

    /**
     * Forgets where somebody was going.
     *
     * @return whether they had said in the first place
     */
    public boolean clearDestination(UUID traveller) {
        return destinations.remove(traveller) != null;
    }

    /** Whether anybody has said where they are going. */
    public boolean isEmpty() {
        return destinations.isEmpty();
    }

    /** Forgets every destination. */
    public void clear() {
        destinations.clear();
    }

    /**
     * Whether somebody's destination is one a sign is asking about.
     *
     * @param traveller the rider's unique id
     * @param pattern the name on the sign, where {@code *} stands for any run of characters
     */
    public boolean isHeadingFor(UUID traveller, String pattern) {
        return destination(traveller).filter(name -> matches(name, pattern)).isPresent();
    }

    /**
     * Whether a destination matches a pattern.
     *
     * <p>{@code *} stands for any run of characters, including none, so {@code north*} matches
     * every destination whose name begins that way and {@code *junction} every one that ends
     * there. Everything else has to match exactly, ignoring case.
     */
    public static boolean matches(String station, String pattern) {
        String name = station.toLowerCase(Locale.ROOT);
        String glob = pattern.toLowerCase(Locale.ROOT);

        int nameAt = 0;
        int globAt = 0;
        int lastWildcard = -1;
        int resumeAt = 0;

        while (nameAt < name.length()) {
            if (globAt < glob.length() && glob.charAt(globAt) == ANY) {
                // Remember the wildcard so a later mismatch can come back and let it swallow one
                // more character, rather than giving up on a match that is still possible.
                lastWildcard = globAt++;
                resumeAt = nameAt;
            } else if (globAt < glob.length() && glob.charAt(globAt) == name.charAt(nameAt)) {
                globAt++;
                nameAt++;
            } else if (lastWildcard >= 0) {
                globAt = lastWildcard + 1;
                nameAt = ++resumeAt;
            } else {
                return false;
            }
        }

        while (globAt < glob.length() && glob.charAt(globAt) == ANY) {
            globAt++;
        }
        return globAt == glob.length();
    }
}
