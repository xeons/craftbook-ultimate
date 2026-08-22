// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What a sign line will actually accept.
 *
 * <p>A form is not a description of a grammar — it <em>is</em> the reader the chip uses. A form
 * that checks an item filter calls the same {@code ItemCriteria.parse} the sensor calls at run
 * time, and one that checks a number calls the same bounded read. That is the whole point of it:
 * a second copy of a grammar drifts from the first, and a document that promises what the code
 * refuses is worse than no document. Upstream's {@code getLineHelp} was not ported for exactly
 * that reason, and this is the version of it that cannot go stale.
 *
 * <p>Two things come out of one object. {@link #fault} answers whether what somebody has written
 * can be read at all, which is what refuses a sign as it is written and what marks a chip already
 * in the world as broken. {@link #accepted} says what the line would take, which is what the
 * catalogue page prints and what a refusal quotes back.
 *
 * <p>A blank line is never a fault here. Whether a chip can work without a line is
 * {@link LineSpec#required}, and asking both questions in one place would mean a chip with a
 * sensible default could never declare what its line accepts.
 */
@NullMarked
public interface LineForm {

    /**
     * Why a line cannot be read, or nothing where it can.
     *
     * <p>Only asked of a line somebody has written something on.
     *
     * @param written what is on the line, already trimmed
     * @param context what a name on the line means
     */
    Optional<String> fault(String written, LineContext context);

    /**
     * The forms this line takes, one per way of writing it.
     *
     * <p>Empty for a line that takes any text at all, which is not the same as a line nobody has
     * described: the meaning on {@link LineSpec} says what it is for either way.
     */
    List<String> accepted();

    /**
     * Something this form accepts, for a page to show and a test to check the form against.
     *
     * <p>Empty for a line that takes any text at all. Every other form has one, and a test asks
     * each form to read its own example: a form whose example it would refuse is one whose promise
     * and whose reader have come apart, which is the failure this whole seam exists to prevent.
     */
    default Optional<String> example() {
        return Optional.empty();
    }

    /** A line that takes any text at all, and so can never be written wrongly. */
    static LineForm free() {
        return Free.INSTANCE;
    }

    /** Whether this form checks anything, which is what decides if it is worth printing. */
    default boolean checksAnything() {
        return !accepted().isEmpty();
    }

    /** The one form that accepts everything. */
    final class Free implements LineForm {

        private static final Free INSTANCE = new Free();

        private Free() {
        }

        @Override
        public Optional<String> fault(String written, LineContext context) {
            return Optional.empty();
        }

        @Override
        public List<String> accepted() {
            return List.of();
        }
    }
}
