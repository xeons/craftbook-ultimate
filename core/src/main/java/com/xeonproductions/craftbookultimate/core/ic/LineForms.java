// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.entity.ItemCriteria;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import org.jspecify.annotations.NullMarked;

/**
 * The forms a sign line comes in.
 *
 * <p>Every one of these is the chip's own reader wrapped so it can be asked whether it would
 * succeed, rather than a second description of what the reader accepts. {@link #itemFilter} calls
 * {@code ItemCriteria.parse}, {@link #entity} calls {@code EntitySpec.parse}, and the forms each
 * prints come from the parser's own vocabulary. Nothing here can promise something the chip would
 * refuse.
 *
 * <p>{@link #read} is the way to add another: give it what the line is for, the parser, and the
 * shapes it takes. Where a shape is one the parser itself declares, take it from there rather than
 * writing it out again.
 */
@NullMarked
public final class LineForms {

    private LineForms() {
    }

    /**
     * A form built out of a parser that either reads a line or does not.
     *
     * @param what the line is for, as a noun phrase completing "is not"
     * @param reader the chip's own reader, answering empty for a line it cannot read
     * @param example something the reader accepts, which a test holds the reader to
     * @param accepted the shapes the line takes, for the page and for a refusal to quote
     */
    public static LineForm read(
            String what,
            BiFunction<String, LineContext, Optional<?>> reader,
            String example,
            String... accepted) {

        List<String> shapes = List.of(accepted);
        return new LineForm() {

            @Override
            public Optional<String> fault(String written, LineContext context) {
                return reader.apply(written, context).isPresent()
                        ? Optional.empty()
                        : Optional.of("\"" + written + "\" is not " + what);
            }

            @Override
            public List<String> accepted() {
                return shapes;
            }

            @Override
            public Optional<String> example() {
                return Optional.of(example);
            }
        };
    }

    /**
     * A line that takes any of several forms.
     *
     * <p>For the lines whose meaning depends on a flag after the model reference: an item sensor
     * reading from a book takes an offset on the line that otherwise carries a filter. Accepting
     * either is what keeps a flag the reader here cannot see from refusing a sign that works.
     */
    public static LineForm either(LineForm... forms) {
        List<LineForm> alternatives = List.of(forms);
        List<String> shapes = alternatives.stream()
                .flatMap(form -> form.accepted().stream())
                .distinct()
                .toList();

        return new LineForm() {

            @Override
            public Optional<String> fault(String written, LineContext context) {
                Optional<String> first = Optional.empty();
                for (LineForm form : alternatives) {
                    Optional<String> fault = form.fault(written, context);
                    if (fault.isEmpty()) {
                        return Optional.empty();
                    }
                    if (first.isEmpty()) {
                        first = fault;
                    }
                }
                return first;
            }

            @Override
            public List<String> accepted() {
                return shapes;
            }

            @Override
            public Optional<String> example() {
                return alternatives.isEmpty() ? Optional.empty() : alternatives.get(0).example();
            }
        };
    }

    /** A line that takes any text at all. */
    public static LineForm free() {
        return LineForm.free();
    }

    /**
     * A line that takes one of a fixed set of words, in any case.
     *
     * <p>The flags: {@code h} to hold a block, {@code Force} to replace what is there,
     * {@code INF} to keep counting.
     */
    public static LineForm oneOf(String... words) {
        List<String> allowed = List.of(words);
        return read(
                "one of " + joined(allowed),
                (written, context) -> allowed.stream()
                        .filter(word -> word.equalsIgnoreCase(written))
                        .findFirst(),
                allowed.isEmpty() ? "" : allowed.get(0),
                words);
    }

    /**
     * A whole number within bounds.
     *
     * <p>Note what this does <em>not</em> do: a chip whose number is out of bounds is held to the
     * bounds rather than refused, so this is only a fault for something that is not a number at
     * all. That is deliberate and matches the settings — asking for more than is allowed gets as
     * much as is allowed.
     */
    public static LineForm wholeNumber(int lowest, int highest) {
        return read(
                "a whole number",
                (written, context) -> {
                    try {
                        return Optional.of(Integer.parseInt(written.trim()));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                },
                String.valueOf(lowest),
                lowest + " to " + highest);
    }

    /** A number, whole or not, within bounds. */
    public static LineForm number(double lowest, double highest) {
        return read(
                "a number",
                (written, context) -> {
                    try {
                        return Optional.of(Double.parseDouble(written.trim()));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                },
                trimmed(lowest),
                trimmed(lowest) + " to " + trimmed(highest));
    }

    /**
     * Numbers separated by colons, of which the first is required and the rest are not.
     *
     * <p>Covers {@code speed:spread}, {@code width:length:height}, {@code radius:height:up} and
     * the rest of the measured lines, which are all the same shape with different names.
     *
     * @param parts what each number is, in order, the first being the one that must be there
     */
    public static LineForm measurements(String... parts) {
        List<String> named = List.of(parts);
        if (named.isEmpty()) {
            throw new IllegalArgumentException("A measurement takes at least one number");
        }

        return read(
                "a measurement",
                (written, context) -> {
                    String[] written_parts = written.trim().split(":", -1);
                    if (written_parts.length == 0 || written_parts.length > named.size()) {
                        return Optional.empty();
                    }
                    for (String part : written_parts) {
                        try {
                            Double.parseDouble(part.trim());
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    }
                    return Optional.of(written_parts.length);
                },
                "1",
                shapesOf(named));
    }

    /** One thing to check about an item, as the item sensors read it. */
    public static LineForm itemFilter() {
        return read(
                "a check this chip understands",
                (written, context) -> ItemCriteria.parse(written, context::item),
                ItemCriteria.ITEM_CHECK + ":stone",
                ItemCriteria.ACCEPTED.toArray(new String[0]));
    }

    /** Something to match, riders and all, as the sensors and the spawners read it. */
    public static LineForm entity() {
        return read(
                "something this chip can look for",
                (written, context) -> EntitySpec.parse(written, context::item),
                "pig",
                EntitySpec.ACCEPTED.toArray(new String[0]));
    }

    /** One thing to match, without the riders a whole description may carry. */
    public static LineForm oneEntity() {
        return read(
                "something this chip can look for",
                (written, context) -> EntitySpec.parseOne(written, context::item),
                "pig",
                EntitySpec.ACCEPTED.toArray(new String[0]));
    }

    /** A block, by modern name or by the number a sign written before the flattening uses. */
    public static LineForm block() {
        return read(
                "a block this server has",
                (written, context) -> context.item(written),
                "stone",
                "<block>", "<id>:<data>");
    }

    /** A variable, as the three variable chips name one. */
    public static LineForm variable() {
        return read(
                "a variable name",
                (written, context) -> VariableName.parse(written),
                "count",
                "<name>", "<namespace>|<name>");
    }

    /**
     * A wireless channel, with or without a namespace around it.
     *
     * <p>Only the shape is checked. Whether anybody is transmitting on the channel is not
     * something a sign can be wrong about — a receiver built before its transmitter is an ordinary
     * way to build a pair.
     */
    public static LineForm band() {
        return read(
                "a channel name",
                (written, context) -> written.isBlank() ? Optional.empty() : Optional.of(written),
                "channel",
                "<channel>");
    }

    /** A block a chip swaps to, given as the two it swaps between. */
    public static LineForm blockPair(char separator) {
        return read(
                "a pair of blocks",
                (written, context) -> {
                    int at = written.indexOf(separator);
                    if (at < 0) {
                        return context.item(written).map(List::of);
                    }
                    Optional<?> driven = context.item(written.substring(0, at).trim());
                    Optional<?> idle = context.item(written.substring(at + 1).trim());
                    return driven.isPresent() && idle.isPresent()
                            ? Optional.of(written)
                            : Optional.empty();
                },
                "stone" + separator + "air",
                "<block>", "<driven>" + separator + "<idle>");
    }

    /**
     * A step from the sign, as three numbers, optionally marked as measured from the world.
     *
     * <p>Used by the chips that act somewhere other than where they are.
     */
    public static LineForm offset() {
        return read(
                "a step from the sign",
                (written, context) -> {
                    String trimmed = written.startsWith("!") ? written.substring(1) : written;
                    String[] parts = trimmed.split(":", -1);
                    if (parts.length != 3) {
                        return Optional.empty();
                    }
                    for (String part : parts) {
                        try {
                            Integer.parseInt(part.trim());
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    }
                    return Optional.of(trimmed);
                },
                "0:1:0",
                "<x>:<y>:<z>", "!<x>:<y>:<z>");
    }

    /**
     * The box a chip works on, as one reach and an optional middle to measure it from.
     *
     * <p>The reader is {@link SignArea} itself, so what this promises and what the chips accept
     * cannot come apart.
     */
    public static LineForm searchArea() {
        return read(
                "an area",
                (written, context) ->
                        SignArea.isReadable(written) ? Optional.of(written) : Optional.empty(),
                "10",
                "<radius>", "<x>,<y>,<z>", "<radius>=<x>:<y>:<z>");
    }

    /** A block name with an offset in front of it, as the flex set reads one. */
    public static LineForm offsetAndBlock() {
        return read(
                "an offset and a block",
                (written, context) -> {
                    int at = written.lastIndexOf(':');
                    if (at < 0) {
                        return Optional.empty();
                    }
                    return BlockReference.parse(written.substring(at + 1).trim()).isPresent()
                            ? Optional.of(written)
                            : Optional.empty();
                },
                "Y+1:stone",
                "<offset>:<block>");
    }

    /** The shapes a list of named measurements takes, shortest first. */
    private static String[] shapesOf(List<String> named) {
        String[] shapes = new String[named.size()];
        for (int count = 1; count <= named.size(); count++) {
            shapes[count - 1] = "<" + String.join(">:<", named.subList(0, count)) + ">";
        }
        return shapes;
    }

    private static String joined(List<String> words) {
        return String.join(", ", words);
    }

    /** A bound as it should read on a page, without a trailing nought nobody needs. */
    private static String trimmed(double bound) {
        return bound == Math.rint(bound) && !Double.isInfinite(bound)
                ? String.valueOf((long) bound)
                : String.valueOf(bound).toLowerCase(Locale.ROOT);
    }
}
