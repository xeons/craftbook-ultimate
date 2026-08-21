// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.variable;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * What a variable is called, and whose it is.
 *
 * <p>A name has two parts and a sign writes them as one word: {@code score} is the variable
 * {@code score} in the shared namespace, and {@code alice|score} is a different variable of the
 * same name belonging to {@code alice}. That is the whole of the grammar, and it is the same one
 * a command takes, so what a builder writes on a sign and what they type to make the variable
 * match exactly.
 *
 * <p>The namespace is what keeps two builders' variables apart without either having to invent a
 * unique name. It works the way a wireless band's does — see
 * {@link com.xeonproductions.craftbookultimate.core.radio.Band} — except that a variable always
 * has one, defaulting to {@link #SHARED} rather than to nothing, because a variable with no
 * namespace at all would read back from a file as a variable called nothing.
 *
 * <p>Names are letters, digits and underscores. Nothing else is allowed, which is deliberate: a
 * variable is looked up by exactly what is written, so a name with a stray space or a bracket in
 * it would be a variable nobody could ever refer to again.
 *
 * @param namespace whose variable it is, never blank
 * @param name what it is called, never blank
 */
@NullMarked
public record VariableName(String namespace, String name) {

    /** The namespace a variable is in when its name does not say otherwise. */
    public static final String SHARED = "global";

    /** Separates the namespace from the name, on a sign and in a command alike. */
    public static final char SEPARATOR = '|';

    /** What a namespace and a name may be made of. */
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_]+");

    public VariableName {
        namespace = namespace.trim().toLowerCase(Locale.ROOT);
        name = name.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED.matcher(namespace).matches()) {
            throw new IllegalArgumentException(
                    "A namespace is letters, digits and underscores, got \"" + namespace + "\"");
        }
        if (!ALLOWED.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "A variable name is letters, digits and underscores, got \"" + name + "\"");
        }
    }

    /** A variable in the shared namespace. */
    public static VariableName shared(String name) {
        return new VariableName(SHARED, name);
    }

    /**
     * Reads a name as it is written on a sign or typed into a command.
     *
     * <p>Anything that is not a name at all comes back empty rather than throwing, because both
     * places this is called from are reading something a person typed.
     *
     * @param written the whole name, with or without a namespace before it
     * @return the name, or empty if what was written is not one
     */
    public static Optional<VariableName> parse(String written) {
        String trimmed = written.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        int separator = trimmed.indexOf(SEPARATOR);
        String namespace = separator < 0 ? SHARED : trimmed.substring(0, separator);
        String name = separator < 0 ? trimmed : trimmed.substring(separator + 1);

        try {
            return Optional.of(new VariableName(namespace, name));
        } catch (IllegalArgumentException notAName) {
            return Optional.empty();
        }
    }

    /**
     * Reads a name and a namespace given separately, as a command with a namespace option does.
     *
     * <p>A namespace written into the name itself wins over the one given alongside it, so
     * {@code alice|score} means Alice's whether or not another namespace was named.
     *
     * @param written the name, with or without a namespace before it
     * @param fallback the namespace to use when the name does not carry one
     */
    public static Optional<VariableName> parse(String written, String fallback) {
        if (written.indexOf(SEPARATOR) >= 0) {
            return parse(written);
        }
        return parse(fallback.trim() + SEPARATOR + written.trim());
    }

    /** Whether this variable is in the shared namespace. */
    public boolean isShared() {
        return namespace.equals(SHARED);
    }

    /**
     * How this name is written on a sign and in a command.
     *
     * <p>A shared variable is written as its bare name, which is how somebody would have typed it,
     * rather than as {@code global|name}.
     */
    @Override
    public String toString() {
        return isShared() ? name : namespace + SEPARATOR + name;
    }

    /** This name written in full, with its namespace, however shared it is. */
    public String qualified() {
        return namespace + SEPARATOR + name;
    }
}
