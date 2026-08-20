package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Something a sign in the world makes happen.
 *
 * <p>Not an integrated circuit and not a cart mechanic: a sign that names one of these builds
 * something out of the blocks around it, or carries people between floors. It answers to a hand
 * on the sign and to redstone reaching it, and it keeps nothing between one and the next — the
 * state of a bridge is whether its blocks are there.
 *
 * <p>Everything a mechanic needs comes in on the {@link MechanicVisit}, which is what lets one be
 * exercised against a world built in a test with no server running.
 */
@NullMarked
public interface SignMechanic {

    /**
     * What this mechanic is called.
     *
     * <p>Used three ways, and they have to agree: it is the name an operator switches it off by,
     * the second half of the permissions to build and to use it, and how it is spoken of to a
     * builder.
     */
    String name();

    /**
     * Every name this mechanic's sign may carry, in brackets, in their proper spelling.
     *
     * <p>Several because most of these mechanics have more than one end, or more than one kind:
     * a bridge has a sign at each end and a gate comes in six materials.
     */
    List<String> signNames();

    /**
     * Does whatever the mechanic does.
     *
     * @return true if the mechanic acted, which is what stops the click doing anything else
     */
    boolean act(MechanicVisit visit);

    /** The permission to build one of these. */
    default String buildPermission() {
        return "craftbook." + name().toLowerCase(Locale.ROOT);
    }

    /** The permission to work one of these. */
    default String usePermission() {
        return buildPermission() + ".use";
    }

    /**
     * The name a sign carries, if it carries one of this mechanic's.
     *
     * <p>Answered in its proper spelling however the builder typed it, so the rest of the plugin
     * has one form to compare against and a sign is tidied up as it is written.
     */
    default Optional<String> nameOn(SignLines lines) {
        String written = lines.trimmedText(PostedSign.NAME_LINE);
        for (String candidate : signNames()) {
            if (written.equalsIgnoreCase(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Whether a sign is one of this mechanic's. */
    default boolean claims(SignLines lines) {
        return nameOn(lines).isPresent();
    }
}
