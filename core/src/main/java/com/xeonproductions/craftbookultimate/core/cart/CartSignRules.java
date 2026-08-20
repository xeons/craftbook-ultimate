package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What a cart mechanic's sign is allowed to say.
 *
 * <p>Checked as the sign is written rather than when a cart first rolls over it, so a builder is
 * told about a mistyped filter while they are standing at the sign with the means to fix it,
 * instead of wondering later why their junction never sorts anything.
 */
@NullMarked
public final class CartSignRules {

    /** The name a cart mechanic's sign carries, on its second line, in brackets. */
    public static final int NAME_LINE = CartMechanism.MechanismSign.NAME_LINE;

    /** The line most mechanics read their first choice from. */
    private static final int THIRD_LINE = 2;

    /** The line most mechanics read their second choice from. */
    private static final int FOURTH_LINE = 3;

    /** The longest a delay may hold a cart, so a mistyped sign cannot strand one for a day. */
    private static final int MAX_DELAY_SECONDS = 3600;

    /** What a dispenser's sign may say to have the vehicle sent off. */
    private static final String PUSH = "push";

    /** Every name a cart mechanic's sign may carry. */
    private static final List<String> NAMES = List.of(
            "Station", "Sort", "CartLift", "Launch", "Delay", "Print",
            "Collect", "Deposit", "Craft", "Dispenser", "Eject", "Reverse");

    private CartSignRules() {}

    /** Every name a cart mechanic's sign may carry. */
    public static List<String> names() {
        return NAMES;
    }

    /**
     * The name a sign carries, if it carries one of a cart mechanic's.
     *
     * <p>The name is answered in its proper spelling however the builder typed it, so the rest of
     * the plugin has one form to compare against.
     */
    public static Optional<String> nameOn(SignLines lines) {
        String written = lines.trimmedText(NAME_LINE);
        for (String name : NAMES) {
            if (written.equalsIgnoreCase("[" + name + "]")) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
    }

    /**
     * What is wrong with a sign, if anything.
     *
     * @param name the mechanic's name, as {@link #nameOn} gives it
     * @param lines what the builder has written
     * @param world where item and recipe names are looked up
     * @return the complaint to make, or empty if the sign is fine
     */
    public static Optional<String> problemWith(String name, SignLines lines, CartWorld world) {
        return switch (name) {
            case "Delay" -> delayProblem(lines);
            case "Sort", "CartLift", "Launch" -> filterProblem(lines, world);
            case "Eject" -> ejectProblem(lines, world);
            case "Station" -> stationProblem(lines);
            case "Collect", "Deposit" -> itemProblem(lines, world);
            case "Craft" -> recipeProblem(lines, world);
            case "Dispenser" -> dispenserProblem(lines);
            default -> Optional.empty();
        };
    }

    /** A delay holds a cart for a number of seconds, and it has to be one. */
    private static Optional<String> delayProblem(SignLines lines) {
        String written = lines.trimmedText(THIRD_LINE);
        try {
            int seconds = Integer.parseInt(written);
            if (seconds < 1 || seconds > MAX_DELAY_SECONDS) {
                return Optional.of("A delay must be between 1 and " + MAX_DELAY_SECONDS + " seconds.");
            }
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.of("Line 3 must be how many seconds to wait.");
        }
    }

    /** The mechanics that choose between two courses take a cart filter on each of two lines. */
    private static Optional<String> filterProblem(SignLines lines, CartWorld world) {
        for (int line : new int[] {THIRD_LINE, FOURTH_LINE}) {
            String written = lines.trimmedText(line);
            if (!CartFilter.isWellFormed(written, world::resolveItem)) {
                return Optional.of("Line " + (line + 1) + " is not a cart filter: " + written);
            }
        }
        return Optional.empty();
    }

    /** An ejector may name the carts it empties, and empties everything if it does not. */
    private static Optional<String> ejectProblem(SignLines lines, CartWorld world) {
        String written = lines.trimmedText(THIRD_LINE);
        if (written.isEmpty() || CartFilter.isWellFormed(written, world::resolveItem)) {
            return Optional.empty();
        }
        return Optional.of("Line 3 is not a cart filter: " + written);
    }

    /** A station names itself on line 3 and says nothing anywhere else. */
    private static Optional<String> stationProblem(SignLines lines) {
        if (!lines.trimmedText(0).isEmpty() || !lines.trimmedText(FOURTH_LINE).isEmpty()) {
            return Optional.of("A station says nothing on lines 1 and 4.");
        }
        return Optional.empty();
    }

    /** The chest mechanics take one item and optionally how many of it. */
    private static Optional<String> itemProblem(SignLines lines, CartWorld world) {
        String written = lines.trimmedText(THIRD_LINE);
        if (written.isEmpty()) {
            return Optional.empty();
        }

        int separator = written.lastIndexOf(':');
        String named = separator < 0 ? written : written.substring(0, separator);
        if (separator >= 0) {
            try {
                Integer.parseInt(written.substring(separator + 1).trim());
            } catch (NumberFormatException e) {
                return Optional.of("What follows the colon must be how many to move.");
            }
        }
        if (!named.isBlank() && world.resolveItem(named.trim()).isEmpty()) {
            return Optional.of("There is no item called " + named.trim() + ".");
        }
        return Optional.empty();
    }

    /** A crafter names a recipe across its last two lines. */
    private static Optional<String> recipeProblem(SignLines lines, CartWorld world) {
        String written = lines.trimmedText(THIRD_LINE) + lines.trimmedText(FOURTH_LINE);
        if (written.isBlank()) {
            return Optional.of("Lines 3 and 4 must name a recipe.");
        }
        if (world.recipeNamed(CartRecipe.signNameOf(written)).isEmpty()) {
            return Optional.of("There is no recipe called " + written
                    + ". Recipe names have their underscores removed.");
        }
        return Optional.empty();
    }

    /** A dispenser may name what to hand out, and may say to push it. */
    private static Optional<String> dispenserProblem(SignLines lines) {
        String kind = lines.trimmedText(THIRD_LINE);
        if (!kind.isEmpty()
                && !kind.toLowerCase(Locale.ROOT).equals(PUSH)
                && VehicleKind.bySignName(kind).isEmpty()) {
            return Optional.of("Line 3 must name a vehicle, say push, or be blank.");
        }

        String push = lines.trimmedText(FOURTH_LINE);
        if (!push.isEmpty() && !push.toLowerCase(Locale.ROOT).equals(PUSH)) {
            return Optional.of("Line 4 must say push or be blank.");
        }
        return Optional.empty();
    }
}
