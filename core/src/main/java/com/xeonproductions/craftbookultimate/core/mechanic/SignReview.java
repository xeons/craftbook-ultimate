package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import org.jspecify.annotations.NullMarked;

/**
 * What a mechanic makes of a sign somebody has just written.
 *
 * <p>Two answers and no third: keep it, in whatever spelling the mechanic wants it kept, or refuse
 * it with a reason the builder can act on. Checking here rather than the first time somebody uses
 * the mechanic means the builder is standing at the sign with the means to fix it.
 */
@NullMarked
public sealed interface SignReview {

    /** The sign is fine, and is to be kept as this. */
    record Accepted(SignLines lines) implements SignReview {}

    /** The sign is not fine, and this is what to tell whoever wrote it. */
    record Refused(String why) implements SignReview {}

    /** Keeps a sign exactly as it was written. */
    static SignReview keep(SignLines lines) {
        return new Accepted(lines);
    }

    /** Refuses a sign. */
    static SignReview refuse(String why) {
        return new Refused(why);
    }
}
