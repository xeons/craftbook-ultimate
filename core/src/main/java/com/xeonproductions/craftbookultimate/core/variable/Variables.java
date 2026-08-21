// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.variable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * The named values chips and commands share.
 *
 * <p>A variable is a number somebody can read and change from anywhere: a command sets it, a chip
 * reads it, another chip on the far side of the world adds to it. Like a wireless band, the two
 * ends never see each other, which is what makes a scoreboard, a tally or a counter that survives
 * a restart possible without any of the chips involved being near one another.
 *
 * <p>Values are held as text and read as numbers where a chip wants one, so a variable can carry
 * something that is not a number without the store having to care. A chip asking for a number gets
 * {@link OptionalDouble} and decides for itself what to do when there is none, which is what keeps
 * a variable somebody has deleted or set to a word from breaking every chip that mentions it.
 *
 * <p>A variable has to be made before a sign can name one, which is why {@link #define} and
 * {@link #set} are separate. That is not ceremony: what a variable is called lives here rather
 * than in the blocks beside the sign, so a sign naming a variable nobody has made would otherwise
 * be silently dead, and a builder would have no way to tell that from a wiring fault.
 *
 * <p>Safe to use from any number of regions at once.
 */
@NullMarked
public final class Variables {

    /** Separates the fields of a variable when it is written out. */
    private static final char SEPARATOR = ' ';

    /**
     * What a value may be made of.
     *
     * <p>No spaces, so a saved line can be read back by splitting on them, and nothing that would
     * make a line ambiguous. Signs and numbers both fit inside this comfortably.
     */
    private static final Pattern ALLOWED_VALUE = Pattern.compile("[A-Za-z0-9.,:;_+-]+");

    /** The value a variable starts at when nobody says what it should be. */
    public static final String DEFAULT_VALUE = "0";

    private final Map<VariableName, String> values = new ConcurrentHashMap<>();

    /** Whether a variable of this name has been made. */
    public boolean has(VariableName name) {
        return values.containsKey(name);
    }

    /**
     * What a variable holds.
     *
     * @return its value, or empty if no such variable has been made
     */
    public Optional<String> get(VariableName name) {
        return Optional.ofNullable(values.get(name));
    }

    /**
     * What a variable holds, read as a number.
     *
     * <p>Empty covers both a variable that does not exist and one holding something that is not a
     * number. A chip cannot usefully tell those apart — neither gives it a number to work with —
     * and treating them the same is what stops a deleted variable from being different, to a chip,
     * from one somebody set to a word.
     */
    public OptionalDouble number(VariableName name) {
        String held = values.get(name);
        if (held == null) {
            return OptionalDouble.empty();
        }
        try {
            double parsed = Double.parseDouble(held);
            return Double.isFinite(parsed) ? OptionalDouble.of(parsed) : OptionalDouble.empty();
        } catch (NumberFormatException notANumber) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Makes a variable, if there is not one of that name already.
     *
     * @return true if it was made, false if one already existed and was left alone
     */
    public boolean define(VariableName name, String value) {
        if (!isStorable(value)) {
            return false;
        }
        return values.putIfAbsent(name, value) == null;
    }

    /**
     * Sets a variable that already exists.
     *
     * <p>Refuses to make one, so a command that means to change something cannot quietly create it
     * under a misspelling instead.
     *
     * @return true if there was such a variable and its value was changed
     */
    public boolean set(VariableName name, String value) {
        if (!isStorable(value) || !has(name)) {
            return false;
        }
        values.put(name, value);
        return true;
    }

    /**
     * Sets a variable that already exists to a number.
     *
     * <p>Whole numbers are written without a decimal part, so a counter reads {@code 7} rather
     * than {@code 7.0}, and nothing is ever written in scientific notation.
     *
     * @return true if there was such a variable and its value was changed
     */
    public boolean setNumber(VariableName name, double value) {
        if (!Double.isFinite(value)) {
            return false;
        }
        return set(name, format(value));
    }

    /**
     * Writes a number the way a variable holds one.
     *
     * <p>{@code 7.0} becomes {@code 7} and {@code 1e20} becomes its digits rather than
     * {@code 1.0E20}, so a value is always something a person could have typed.
     */
    public static String format(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_VALUE;
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /**
     * Removes a variable.
     *
     * @return true if there was one to remove
     */
    public boolean remove(VariableName name) {
        return values.remove(name) != null;
    }

    /** Every variable there is, in order, so a listing reads the same way twice. */
    public List<VariableName> names() {
        return values.keySet().stream()
                .sorted(Comparator.comparing(VariableName::namespace).thenComparing(VariableName::name))
                .toList();
    }

    /** Every variable in one namespace, in order. */
    public List<VariableName> namesIn(String namespace) {
        String wanted = new VariableName(namespace, "x").namespace();
        return names().stream().filter(name -> name.namespace().equals(wanted)).toList();
    }

    /** How many variables there are. */
    public int size() {
        return values.size();
    }

    /**
     * Whether a value is one a variable can hold.
     *
     * <p>Checked on the way in rather than on the way out, so a value that could not be saved is
     * never accepted in the first place and a variable a chip has been using cannot vanish at
     * shutdown.
     */
    public static boolean isStorable(String value) {
        return ALLOWED_VALUE.matcher(value).matches();
    }

    /**
     * Every variable and its value, written one to a line.
     *
     * <p>Three fields separated by spaces: namespace, name, then value. None of the three may
     * contain a space, so the line reads back by splitting on them.
     */
    public List<String> save() {
        List<String> lines = new ArrayList<>();
        for (VariableName name : names()) {
            String value = values.get(name);
            if (value != null && isStorable(value)) {
                lines.add(name.namespace() + SEPARATOR + name.name() + SEPARATOR + value);
            }
        }
        return lines;
    }

    /**
     * Reads variables back in, as {@link #save()} wrote them.
     *
     * <p>A line that is not a namespace, a name and a value is skipped, so somebody editing the
     * file by hand cannot cost themselves every other variable.
     *
     * @return how many variables were read
     */
    public int load(List<String> lines) {
        int read = 0;
        for (String line : lines) {
            String[] fields = line.trim().split(" +", 3);
            if (fields.length != 3) {
                continue;
            }

            Optional<VariableName> name = VariableName.parse(fields[0] + VariableName.SEPARATOR + fields[1]);
            if (name.isEmpty() || !isStorable(fields[2])) {
                continue;
            }

            values.put(name.get(), fields[2]);
            read++;
        }
        return read;
    }

    /** Forgets every variable. */
    public void clear() {
        values.clear();
    }
}
