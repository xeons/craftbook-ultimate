// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What one of a chip's configurable sign lines means, and what it will take.
 *
 * <p>A chip is declared on line 2 and configured on lines 3 and 4. What those two lines mean is
 * particular to the chip and is written nowhere the plugin can read — until this. It serves three
 * purposes at once, which is why it exists rather than being prose in a document: the catalogue
 * page is generated from it, a sign missing a line it needs is caught as it is written, and so is
 * a sign whose line the chip could never read.
 *
 * <p>Two questions, deliberately separate. {@link #required} is whether the chip does anything at
 * all with the line blank — many default sensibly, and refusing those signs would be wrong, while
 * a melody with no file named returns before it plays a note. {@link #form} is whether what
 * somebody wrote can be read at all, and it is the chip's own reader rather than a description of
 * one, so it can never promise a spelling the chip refuses.
 *
 * <p>The two combine the obvious way. A required line that cannot be read refuses the sign, since
 * the chip demonstrably will not work. An optional one warns instead, since the chip falls back to
 * its default and still does something.
 *
 * @param meaning what the line is for, as a phrase that reads after "Line 3 is" or "Line 3 gives"
 * @param required whether the chip does nothing at all when the line is blank
 * @param form what the line will actually take
 */
@NullMarked
public record LineSpec(String meaning, boolean required, LineForm form) {

    public LineSpec {
        meaning = meaning.trim();
        if (meaning.isEmpty()) {
            throw new IllegalArgumentException("A line's meaning must say something");
        }
    }

    /**
     * A line the chip cannot work without.
     *
     * <p>A sign leaving this blank is refused as it is written.
     */
    public static LineSpec required(String meaning) {
        return new LineSpec(meaning, true, LineForm.free());
    }

    /** A line the chip cannot work without, in a form it can read. */
    public static LineSpec required(String meaning, LineForm form) {
        return new LineSpec(meaning, true, form);
    }

    /**
     * A line the chip has a sensible default for.
     *
     * <p>A sign leaving this blank is created, and its builder is told what they have defaulted to.
     */
    public static LineSpec optional(String meaning) {
        return new LineSpec(meaning, false, LineForm.free());
    }

    /** A line the chip has a default for, in a form it can read. */
    public static LineSpec optional(String meaning, LineForm form) {
        return new LineSpec(meaning, false, form);
    }

    /** These same lines in a different form, for a chip whose reader was written later. */
    public LineSpec taking(LineForm reading) {
        return new LineSpec(meaning, required, reading);
    }

    /** Why what somebody has written cannot be read, or nothing where it can. */
    public Optional<String> fault(String written, LineContext context) {
        return written.isBlank() ? Optional.empty() : form.fault(written, context);
    }

    /** The shapes this line takes, empty for a line that takes any text at all. */
    public List<String> accepted() {
        return form.accepted();
    }
}
