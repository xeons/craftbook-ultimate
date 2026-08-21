// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What a sign has left blank of the lines its chip says it reads.
 *
 * <p>The same question is asked in three places and has to be answered the same way in all of
 * them: as a sign is written, where a required line left blank refuses it; as a chip loads, where
 * one already in the world is marked instead; and when an operator asks what is broken across a
 * server. Keeping the reading here rather than in any one of those is what stops the three
 * disagreeing about whether a particular sign works.
 *
 * @param missing lines the chip cannot work without, left blank
 * @param defaulted lines the chip has a default for, left blank
 */
@NullMarked
public record LineReview(List<Blank> missing, List<Blank> defaulted) {

    public LineReview {
        missing = List.copyOf(missing);
        defaulted = List.copyOf(defaulted);
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

    /** Reads a sign against what its chip says its lines are for. */
    public static LineReview of(ICDefinition definition, SignLines lines) {
        List<Blank> missing = new ArrayList<>();
        List<Blank> defaulted = new ArrayList<>();

        for (int index : new int[] {ICDefinition.THIRD_LINE, ICDefinition.FOURTH_LINE}) {
            Optional<LineSpec> spec = definition.lineSpec(index);
            if (spec.isEmpty() || !lines.isBlank(index)) {
                continue;
            }
            Blank blank = new Blank(index, spec.get());
            (spec.get().required() ? missing : defaulted).add(blank);
        }

        return new LineReview(missing, defaulted);
    }

    /**
     * Whether the chip does nothing at all as its sign stands.
     *
     * <p>Only a required line counts. A blank line the chip has a default for leaves a chip that
     * works, just not necessarily as its builder meant, and that is not something to warn a whole
     * server about.
     */
    public boolean broken() {
        return !missing.isEmpty();
    }

    /** Every blank line, whether the chip can work without it or not. */
    public List<Blank> all() {
        List<Blank> blanks = new ArrayList<>(missing);
        blanks.addAll(defaulted);
        return List.copyOf(blanks);
    }
}
