package com.xeonproductions.craftbookultimate.core.entity;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Who is on the server.
 *
 * <p>The one thing a chip can ask about that is not somewhere in particular: whether a named player
 * is logged in at all, wherever they happen to be. Only names cross the seam, which are immutable
 * and safe to read from any region's thread.
 *
 * <p>Vanished players are left out, so a sensor cannot be used to find somebody who has taken
 * trouble not to be found.
 */
@NullMarked
public interface Roster {

    /** The account names of everyone online and not hiding. */
    List<String> visibleNames();

    /** Whether anybody online has a name containing a fragment. */
    default boolean anyNameContains(String fragment) {
        for (String name : visibleNames()) {
            if (name.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
