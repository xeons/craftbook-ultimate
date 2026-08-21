package com.xeonproductions.craftbookultimate.core.ic;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * The parsed contents of an IC sign's identifier line, which is the second line of the sign.
 *
 * <p>This is the one piece of the plugin whose syntax is frozen: existing worlds are full of
 * signs written this way, so the grammar accepted here must keep working. Two spellings are
 * recognised.
 *
 * <p>A model reference names the chip by its catalogue number and may carry a suffix:
 *
 * <pre>
 *   [MC1000]        a plain repeater
 *   [MC1000]S       the self-triggering variant
 *   [MCX131]*       a awaitingAuthorisation chip, marked as vetted when it was created
 *   [MC1000]!       inverted outputs, via the mode string
 *   [MC1000]S!abcdef  self-triggering, inverted, with the pins remapped
 * </pre>
 *
 * <p>A shorthand reference names the chip by its readable alias instead, optionally asking for
 * the self-triggering variant:
 *
 * <pre>
 *   =REPEATER
 *   =REPEATER ST
 *   =RE T FLIP ST
 * </pre>
 *
 * <p>Everything in the suffix that is not the {@code S} or {@code *} marker is the mode string,
 * which is handed to {@link ICMode} to interpret. Case is preserved there, because {@code p}
 * and {@code P} select different modes, whereas the model id and shorthand are matched without
 * regard to case because players type them by hand.
 *
 * @param kind which of the two spellings was used
 * @param identifier the model id or shorthand, trimmed and upper-cased
 * @param selfTriggering whether the self-triggering variant was requested
 * @param awaitingAuthorisation whether the awaitingAuthorisation marker was present
 * @param mode the remaining suffix, with its case intact; empty when there is none
 */
@NullMarked
public record ICLine(
        Kind kind, String identifier, boolean selfTriggering, boolean awaitingAuthorisation, String mode) {

    /** Which spelling an {@link ICLine} was written in. */
    public enum Kind {
        /** A bracketed model id, such as {@code [MC1000]}. */
        MODEL,
        /** An {@code =}-prefixed shorthand, such as {@code =REPEATER}. */
        SHORTHAND
    }

    /** Which line of a sign carries the identifier, counted from zero. */
    public static final int LINE_INDEX = 1;

    /** Marks the self-triggering variant in a model suffix. */
    private static final char SELF_TRIGGER_FLAG = 'S';

    /** The same flag written in lower case, which a builder means just as often. */
    private static final char LOWER_SELF_TRIGGER_FLAG = 's';

    /** Marks a awaitingAuthorisation chip whose creation was already permission checked. */
    private static final char AUTHORISATION_FLAG = '*';

    /** The suffix that selects the self-triggering variant of a shorthand. */
    private static final String ST_MARKER = " ST";

    /** A model id in brackets, followed by a free-form suffix. */
    private static final Pattern MODEL_PATTERN = Pattern.compile("^\\[([A-Za-z0-9]{2,16})](.*)$");

    /**
     * A shorthand alias. Shorthands are drawn from a awaitingAuthorisation alphabet that happens to include
     * spaces, so they cannot simply run to the end of the line without care.
     */
    private static final Pattern SHORTHAND_PATTERN =
            Pattern.compile("^=([A-Za-z0-9 +?^-]*[A-Za-z0-9+?^-])$");

    public ICLine {
        identifier = identifier.trim().toUpperCase(Locale.ROOT);
        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("IC identifier must not be blank");
        }
    }

    /**
     * Parses an identifier line.
     *
     * @param raw the raw second line of the sign, in any case and with any surrounding whitespace
     * @return the parsed reference, or empty if the line is not an IC identifier at all
     */
    public static Optional<ICLine> parse(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        Matcher model = MODEL_PATTERN.matcher(trimmed);
        if (model.matches()) {
            return Optional.of(fromSuffix(model.group(1), model.group(2)));
        }

        Matcher shorthand = SHORTHAND_PATTERN.matcher(trimmed);
        if (shorthand.matches()) {
            // A shorthand may contain spaces, so the ST marker cannot be split off by the pattern
            // without it also swallowing the last word of a multi-word alias. Strip it here
            // instead. No registered shorthand ends in ST, which keeps this unambiguous.
            String text = shorthand.group(1);
            boolean selfTriggering = false;
            int markerStart = text.length() - ST_MARKER.length();
            if (markerStart > 0 && text.toUpperCase(Locale.ROOT).endsWith(ST_MARKER)) {
                text = text.substring(0, markerStart).trim();
                selfTriggering = true;
            }
            if (text.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ICLine(Kind.SHORTHAND, text, selfTriggering, false, ""));
        }

        return Optional.empty();
    }

    /**
     * Splits a model suffix into its flags and its mode string.
     *
     * <p>Flags are recognised wherever they appear in the suffix, so {@code [MCX207]S*} and
     * {@code [MCX207]*S} mean the same thing. Everything left over is the mode string.
     *
     * <p>A lower case {@code s} asks for the ticking chip as surely as an upper case one. No mode
     * character is an {@code s} and no pin permutation can contain one, so nothing is given up by
     * reading it that way — where a sign written {@code [MCX120]s} would otherwise have produced a
     * chip that quietly never ticks and no way for its builder to see why.
     */
    private static ICLine fromSuffix(String modelId, String suffix) {
        boolean selfTriggering = false;
        boolean awaitingAuthorisation = false;
        StringBuilder mode = new StringBuilder(suffix.length());

        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (c == SELF_TRIGGER_FLAG || c == LOWER_SELF_TRIGGER_FLAG) {
                selfTriggering = true;
            } else if (c == AUTHORISATION_FLAG) {
                awaitingAuthorisation = true;
            } else {
                mode.append(c);
            }
        }

        return new ICLine(Kind.MODEL, modelId, selfTriggering, awaitingAuthorisation, mode.toString());
    }

    /** Returns this reference with the awaitingAuthorisation marker applied. */
    public ICLine withAwaitingAuthorisation() {
        return awaitingAuthorisation ? this : new ICLine(kind, identifier, selfTriggering, true, mode);
    }

    /** Returns this reference with the self-triggering marker applied. */
    public ICLine withSelfTriggering() {
        return selfTriggering ? this : new ICLine(kind, identifier, true, awaitingAuthorisation, mode);
    }

    /** True when a mode string is present for {@link ICMode} to interpret. */
    public boolean hasMode() {
        return !mode.isBlank();
    }

    /**
     * Renders this reference back to sign text.
     *
     * <p>Model references round-trip exactly, with the flags written before the mode string so
     * that the result parses back to an equal value. Shorthand references are rendered in their
     * own spelling; a caller that wants the sign to carry the canonical model id should resolve
     * through {@link ICRegistry} and render the definition's model reference instead.
     */
    public String render() {
        StringBuilder out = new StringBuilder();
        if (kind == Kind.MODEL) {
            out.append('[').append(identifier).append(']');
            if (selfTriggering) {
                out.append(SELF_TRIGGER_FLAG);
            }
            if (awaitingAuthorisation) {
                out.append(AUTHORISATION_FLAG);
            }
            out.append(mode);
        } else {
            out.append('=').append(identifier);
            if (selfTriggering) {
                out.append(ST_MARKER);
            }
        }
        return out.toString();
    }

    @Override
    public String toString() {
        return render();
    }
}
