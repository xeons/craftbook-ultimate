// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What is wrong with the lines a chip says it reads.
 *
 * <p>The same question is asked in three places and has to be answered the same way in all of
 * them: as a sign is written, where a required line left blank refuses it; as a chip loads, where
 * one already in the world is marked instead; and when an operator asks what is broken across a
 * server. Keeping the reading here rather than in any one of those is what stops the three
 * disagreeing about whether a particular sign works.
 *
 * <p>Two kinds of wrong, and they are not the same. A line left <em>blank</em> is one somebody has
 * not filled in; a line the chip cannot <em>read</em> is one somebody filled in wrongly, and until
 * this existed the second was completely silent — a sensor whose filter would not parse set its
 * output low and returned, which looks exactly like a chip that is wired wrong.
 *
 * @param missing lines the chip cannot work without, left blank
 * @param defaulted lines the chip has a default for, left blank
 * @param unreadable lines carrying something the chip's own reader cannot make sense of
 */
@NullMarked
public record LineReview(List<Blank> missing, List<Blank> defaulted, List<Unreadable> unreadable) {

    public LineReview {
        missing = List.copyOf(missing);
        defaulted = List.copyOf(defaulted);
        unreadable = List.copyOf(unreadable);
    }

    /**
     * One line a chip reads that the sign has nothing on.
     *
     * @param index the sign line, counting from zero
     * @param spec what the chip says that line is for
     */
    public record Blank(int index, LineSpec spec) {

        /** How the line reads to whoever has to fill it in. */
        public String said() {
            return "Line " + (index + 1) + " is " + spec.meaning() + ".";
        }
    }

    /**
     * One line carrying something its chip cannot read.
     *
     * @param index the sign line, counting from zero
     * @param spec what the chip says that line is for
     * @param fault what the chip's own reader made of it
     */
    public record Unreadable(int index, LineSpec spec, String fault) {

        /** How the line reads to whoever has to put it right. */
        public String said() {
            String accepted = spec.accepted().isEmpty()
                    ? ""
                    : " Line " + (index + 1) + " takes " + String.join(", ", spec.accepted()) + ".";
            return "Line " + (index + 1) + ": " + fault + "." + accepted;
        }
    }

    /**
     * Reads a sign against what its chip says its lines are for.
     *
     * @param context what a name written on a line means
     */
    public static LineReview of(ICDefinition definition, SignLines lines, LineContext context) {
        List<Blank> missing = new ArrayList<>();
        List<Blank> defaulted = new ArrayList<>();
        List<Unreadable> unreadable = new ArrayList<>();

        for (int index : new int[] {ICDefinition.THIRD_LINE, ICDefinition.FOURTH_LINE}) {
            Optional<LineSpec> spec = definition.lineSpec(index);
            if (spec.isEmpty()) {
                continue;
            }
            if (lines.isBlank(index)) {
                Blank blank = new Blank(index, spec.get());
                (spec.get().required() ? missing : defaulted).add(blank);
                continue;
            }
            spec.get().fault(lines.trimmedText(index), context)
                    .ifPresent(fault -> unreadable.add(new Unreadable(index, spec.get(), fault)));
        }

        return new LineReview(missing, defaulted, unreadable);
    }

    /** Reads a sign with nothing but the names that need no server to resolve. */
    public static LineReview of(ICDefinition definition, SignLines lines) {
        return of(definition, lines, LineContext.lenient());
    }

    /**
     * Whether the chip does nothing at all as its sign stands.
     *
     * <p>A required line counts whether it is blank or unreadable — both leave a chip that will
     * never do anything. A line the chip has a default for does not, blank or not: it leaves a
     * chip that works, just not necessarily as its builder meant, and that is not something to
     * warn a whole server about.
     */
    public boolean broken() {
        return !missing.isEmpty() || unreadable.stream().anyMatch(bad -> bad.spec().required());
    }

    /** What is wrong that the chip cannot work around, in the order it should be said. */
    public List<String> refusals() {
        List<String> said = new ArrayList<>();
        for (Blank blank : missing) {
            said.add(blank.said());
        }
        for (Unreadable bad : unreadable) {
            if (bad.spec().required()) {
                said.add(bad.said());
            }
        }
        return List.copyOf(said);
    }

    /** What is wrong that the chip will work around, in the order it should be said. */
    public List<String> warnings() {
        List<String> said = new ArrayList<>();
        for (Blank blank : defaulted) {
            said.add(blank.said());
        }
        for (Unreadable bad : unreadable) {
            if (!bad.spec().required()) {
                said.add(bad.said() + " It will use its default instead.");
            }
        }
        return List.copyOf(said);
    }

    /** Whether anything at all is worth saying about this sign. */
    public boolean hasAnythingToSay() {
        return !missing.isEmpty() || !defaulted.isEmpty() || !unreadable.isEmpty();
    }

    /** Every blank line, whether the chip can work without it or not. */
    public List<Blank> all() {
        List<Blank> blanks = new ArrayList<>(missing);
        blanks.addAll(defaulted);
        return List.copyOf(blanks);
    }
}
