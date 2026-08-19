package com.xeonproductions.craftbookultimate.core.control;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

/**
 * The named switches a command can throw, and the chips that follow them.
 *
 * <p>A chip names a switch on its sign and drives its output from that switch's position. Nothing
 * in the world moves the switch: it is thrown by somebody typing a command, which is how a build
 * is opened from anywhere rather than from a lever next to it.
 *
 * <p>A switch exists only while a chip claiming it is loaded, so a command cannot invent one and
 * a name is not a lasting thing. A switch nobody has ever thrown has no position yet, and a chip
 * on it leaves its output alone rather than driving it low.
 *
 * <p>Safe to use from any number of regions at once.
 */
@NullMarked
public final class Switchboard {

    /** Present with a position once thrown; present and empty until then. */
    private final Map<String, Optional<Boolean>> switches = new ConcurrentHashMap<>();

    /**
     * Notes that a chip is following a name, creating the switch if this is the first.
     *
     * <p>A switch that already has a position keeps it, so a chip reloading does not reset a
     * build that other chips are still following.
     */
    public void register(String name) {
        switches.putIfAbsent(name, Optional.empty());
    }

    /** Forgets a name, once no chip is following it any more. */
    public void forget(String name) {
        switches.remove(name);
    }

    /** Whether any chip is following a name. */
    public boolean isKnown(String name) {
        return switches.containsKey(name);
    }

    /**
     * Which way a switch is thrown.
     *
     * @return the position, or empty if the name is unknown or has never been thrown
     */
    public Optional<Boolean> state(String name) {
        Optional<Boolean> position = switches.get(name);
        return position == null ? Optional.empty() : position;
    }

    /**
     * Throws a switch one way or the other.
     *
     * @return true if there was a switch of that name to throw
     */
    public boolean set(String name, boolean on) {
        return switches.computeIfPresent(name, (ignored, position) -> Optional.of(on)) != null;
    }

    /**
     * Throws a switch the other way from wherever it is.
     *
     * @return the position it ended up in, or empty if the name is unknown or has never been
     *     thrown and so has no other way to go
     */
    public Optional<Boolean> toggle(String name) {
        Optional<Boolean> flipped = switches.computeIfPresent(
                name, (ignored, position) -> position.map(on -> !on));
        return flipped == null ? Optional.empty() : flipped;
    }

    /** Every name being followed, in order, so a listing reads the same way twice. */
    public List<String> names() {
        return switches.keySet().stream().sorted().toList();
    }

    /** The number of switches. */
    public int size() {
        return switches.size();
    }

    /** Forgets every switch. */
    public void clear() {
        switches.clear();
    }
}
