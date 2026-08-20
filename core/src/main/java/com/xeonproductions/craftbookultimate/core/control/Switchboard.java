package com.xeonproductions.craftbookultimate.core.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * <p>Two separate things are tracked, and keeping them apart is what makes a switch behave like
 * one. Which names are being followed says what a command may throw, and only a name some loaded
 * chip is following can be thrown, so a command cannot invent a switch. Where each switch is
 * thrown is remembered whether anything is following it or not, so a chunk going out of view and
 * coming back does not swing a door shut, and two chips on the same switch do not take it away
 * from one another when one of them unloads.
 *
 * <p>A switch nobody has ever thrown has no position at all, and a chip on it leaves its output
 * alone rather than driving it low.
 *
 * <p>Safe to use from any number of regions at once.
 */
@NullMarked
public final class Switchboard {

    /** Separates a switch's position from its name when written out. */
    private static final char SEPARATOR = ' ';

    /** How many chips are following each name. */
    private final Map<String, Integer> followers = new ConcurrentHashMap<>();

    /** Where each switch is thrown, for every switch that has ever been thrown. */
    private final Map<String, Boolean> positions = new ConcurrentHashMap<>();

    /**
     * Notes that a chip is following a name.
     *
     * <p>The switch keeps whatever position it was in, so a chip loading does not reset a build
     * that other chips are still following.
     */
    public void register(String name) {
        followers.merge(name, 1, Integer::sum);
    }

    /**
     * Notes that a chip has stopped following a name.
     *
     * <p>The position is kept. A chip unloading says nothing about where the switch is, only that
     * one fewer thing is watching it.
     */
    public void forget(String name) {
        followers.computeIfPresent(name, (ignored, count) -> count <= 1 ? null : count - 1);
    }

    /** Whether any chip is following a name. */
    public boolean isKnown(String name) {
        return followers.containsKey(name);
    }

    /**
     * Which way a switch is thrown.
     *
     * @return the position, or empty if it has never been thrown
     */
    public Optional<Boolean> state(String name) {
        return Optional.ofNullable(positions.get(name));
    }

    /**
     * Throws a switch one way or the other.
     *
     * @return true if there was a switch of that name to throw
     */
    public boolean set(String name, boolean on) {
        if (!isKnown(name)) {
            return false;
        }
        positions.put(name, on);
        return true;
    }

    /**
     * Throws a switch the other way from wherever it is.
     *
     * @return the position it ended up in, or empty if the name is unknown or has never been
     *     thrown and so has no other way to go
     */
    public Optional<Boolean> toggle(String name) {
        if (!isKnown(name)) {
            return Optional.empty();
        }
        Boolean flipped = positions.computeIfPresent(name, (ignored, on) -> !on);
        return Optional.ofNullable(flipped);
    }

    /** Every name being followed, in order, so a listing reads the same way twice. */
    public List<String> names() {
        return followers.keySet().stream().sorted().toList();
    }

    /** The number of switches being followed. */
    public int size() {
        return followers.size();
    }

    /**
     * Puts a switch back where it was, without anything following it yet.
     *
     * <p>Used when the positions saved from a previous run are read back, which happens before any
     * chip has loaded. A chip that later claims the name finds the switch where it left it.
     */
    public void restore(String name, boolean on) {
        if (!name.isEmpty()) {
            positions.put(name, on);
        }
    }

    /**
     * Every switch position, written one to a line.
     *
     * <p>The position comes first so that whatever follows the space is the name, which is a line
     * off a sign and may have spaces of its own.
     */
    public List<String> save() {
        List<String> lines = new ArrayList<>();
        positions.keySet().stream()
                .sorted()
                .forEach(name -> lines.add(positions.get(name) + String.valueOf(SEPARATOR) + name));
        return lines;
    }

    /**
     * Reads positions back in, as {@link #save()} wrote them.
     *
     * <p>A line that is not a position and a name is skipped, so somebody editing the file by hand
     * cannot cost themselves every other switch.
     *
     * @return how many switches were read
     */
    public int load(List<String> lines) {
        int read = 0;
        for (String line : lines) {
            int separator = line.indexOf(SEPARATOR);
            if (separator <= 0 || separator == line.length() - 1) {
                continue;
            }

            String position = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            if (!position.equals("true") && !position.equals("false")) {
                continue;
            }

            restore(line.substring(separator + 1), position.equals("true"));
            read++;
        }
        return read;
    }

    /** How many switches have a position, whether or not anything is following them. */
    public int rememberedCount() {
        return positions.size();
    }

    /** Forgets every switch and every position. */
    public void clear() {
        followers.clear();
        positions.clear();
    }
}
