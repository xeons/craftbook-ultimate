package com.xeonproductions.craftbookultimate.core.effect;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * The firework displays the server has scripts for.
 *
 * <p>Scripts are files an operator puts in the plugin's folder, so this is a registry rather than
 * anything a chip writes to: a display chip names a show and finds it here.
 *
 * <p>Names are letters, digits and underscores only. That is not a matter of taste — a name goes
 * on to become a file name, and anything else would let a sign reach outside the folder scripts
 * are meant to live in.
 */
@NullMarked
public final class FireworkShows {

    /** What a show may be called. */
    private static final Pattern USABLE_NAME = Pattern.compile("[A-Za-z0-9_]{1,64}");

    private final Map<String, FireworkShow> byName = new ConcurrentHashMap<>();

    /** Whether a name is one a sign may ask for and a file may be called. */
    public static boolean isUsableName(String name) {
        return USABLE_NAME.matcher(name).matches();
    }

    /**
     * Records a show under a name, replacing any show already under it.
     *
     * @return true if the name was usable and the show was recorded
     */
    public boolean put(String name, FireworkShow show) {
        if (!isUsableName(name)) {
            return false;
        }
        byName.put(name, show);
        return true;
    }

    /** The show under a name, if there is one and the name is usable at all. */
    public Optional<FireworkShow> find(String name) {
        if (!isUsableName(name)) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name));
    }

    /** Every show that has been recorded. */
    public Set<String> names() {
        return Set.copyOf(byName.keySet());
    }

    /** How many shows there are. */
    public int size() {
        return byName.size();
    }

    /** Forgets every show, so a reload starts from the files as they are now. */
    public void clear() {
        byName.clear();
    }
}
