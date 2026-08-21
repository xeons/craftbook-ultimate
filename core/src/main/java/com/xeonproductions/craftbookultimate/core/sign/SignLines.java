// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.sign;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;

/**
 * The four lines of text on a sign.
 *
 * <p>Mechanics read their configuration from these lines, so this type is the boundary between
 * a sign in the world and everything that interprets one. Lines are held as Adventure components
 * because that is how the server presents them, but almost every mechanic wants the plain text,
 * so {@link #text(int)} is the usual accessor.
 *
 * <p>Instances are immutable. Use {@link #withLine(int, Component)} to derive an edited copy.
 */
@NullMarked
public final class SignLines {

    /** The number of lines every sign has. */
    public static final int LINE_COUNT = 4;

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** A sign with four empty lines. */
    public static final SignLines EMPTY = new SignLines(List.of(
            Component.empty(), Component.empty(), Component.empty(), Component.empty()));

    private final List<Component> lines;

    private SignLines(List<Component> lines) {
        this.lines = List.copyOf(lines);
    }

    /**
     * Wraps four components.
     *
     * @throws IllegalArgumentException if the list is not exactly four lines long
     */
    public static SignLines of(List<Component> lines) {
        if (lines.size() != LINE_COUNT) {
            throw new IllegalArgumentException(
                    "A sign has exactly " + LINE_COUNT + " lines, got " + lines.size());
        }
        return new SignLines(lines);
    }

    /**
     * Builds a sign from plain text, padding any missing trailing lines with blanks.
     *
     * @throws IllegalArgumentException if more than four lines are supplied
     */
    public static SignLines of(String... lines) {
        if (lines.length > LINE_COUNT) {
            throw new IllegalArgumentException(
                    "A sign has at most " + LINE_COUNT + " lines, got " + lines.length);
        }
        List<Component> components = new ArrayList<>(LINE_COUNT);
        for (String line : lines) {
            components.add(Component.text(line));
        }
        while (components.size() < LINE_COUNT) {
            components.add(Component.empty());
        }
        return new SignLines(components);
    }

    /**
     * The component on a line.
     *
     * @param index the line number, from zero to three
     * @throws IndexOutOfBoundsException if the line does not exist
     */
    public Component line(int index) {
        checkIndex(index);
        return lines.get(index);
    }

    /**
     * The plain text of a line, with any formatting removed.
     *
     * @param index the line number, from zero to three
     * @throws IndexOutOfBoundsException if the line does not exist
     */
    public String text(int index) {
        return PLAIN.serialize(line(index));
    }

    /**
     * The plain text of a line with surrounding whitespace removed.
     *
     * <p>Players are not careful about trailing spaces, so mechanics reading a value should
     * prefer this over {@link #text(int)}.
     */
    public String trimmedText(int index) {
        return text(index).trim();
    }

    /** True when a line has no visible text on it. */
    public boolean isBlank(int index) {
        return text(index).isBlank();
    }

    /** All four lines, as an immutable list. */
    public List<Component> lines() {
        return lines;
    }

    /** Returns a copy with one line replaced. */
    public SignLines withLine(int index, Component replacement) {
        checkIndex(index);
        List<Component> copy = new ArrayList<>(lines);
        copy.set(index, replacement);
        return new SignLines(copy);
    }

    /** Returns a copy with one line replaced by plain text. */
    public SignLines withLine(int index, String replacement) {
        return withLine(index, Component.text(replacement));
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= LINE_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Sign line " + index + " is outside the range 0 to " + (LINE_COUNT - 1));
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SignLines that && lines.equals(that.lines);
    }

    @Override
    public int hashCode() {
        return lines.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("SignLines[");
        for (int i = 0; i < LINE_COUNT; i++) {
            if (i > 0) {
                out.append(" | ");
            }
            out.append(text(i));
        }
        return out.append(']').toString();
    }
}
