package com.xeonproductions.craftbookultimate.core.ic;

import org.jspecify.annotations.NullMarked;

/**
 * What one of a chip's configurable sign lines means.
 *
 * <p>A chip is declared on line 2 and configured on lines 3 and 4. What those two lines mean is
 * particular to the chip and is written nowhere the plugin can read — until this. It serves two
 * purposes at once, which is why it exists rather than being prose in a document: the catalogue
 * page is generated from it, and a sign missing a line it needs is caught as it is written.
 *
 * <p>The distinction that matters is {@link #required}. Many chips default sensibly when a line is
 * blank — a mob zapper with nothing on line 3 removes hostile mobs, a rain sensor needs nothing at
 * all — and refusing those signs would be wrong. Others do nothing whatever without it, and are
 * silent about it: a melody with no file named returns before it plays a note. Those are the signs
 * worth refusing, because the alternative is a builder staring at a chip that looks broken.
 *
 * @param meaning what the line is for, as a phrase that reads after "Line 3 is" or "Line 3 gives"
 * @param required whether the chip does nothing at all when the line is blank
 */
@NullMarked
public record LineSpec(String meaning, boolean required) {

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
        return new LineSpec(meaning, true);
    }

    /**
     * A line the chip has a sensible default for.
     *
     * <p>A sign leaving this blank is created, and its builder is told what they have defaulted to.
     */
    public static LineSpec optional(String meaning) {
        return new LineSpec(meaning, false);
    }
}
