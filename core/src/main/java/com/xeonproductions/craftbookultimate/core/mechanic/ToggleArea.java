// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.area.AreaName;
import com.xeonproductions.craftbookultimate.core.area.AreaVault;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * A piece of the world put away and brought back.
 *
 * <p>The other sign mechanics build out of one material in a shape measured from their own sign.
 * This one has no shape of its own: somebody picks out a region, saves it under a name, and the
 * sign swaps that name in and out of the world. A hidden staircase, a shopfront that shuts for the
 * night, a bridge too irregular to describe as a box — all the same mechanic.
 *
 * <p>A sign names one area or two. With one, the area is placed and then cleared away again. With
 * two, the sign swaps between them, so a room can have a day form and a night form and never be
 * empty.
 *
 * <p>{@code [SaveArea]} differs from {@code [Area]} in one way: the half being put away is written
 * back to the store as it stands, rather than as it was first saved. Somebody rearranging the
 * furniture in a toggled room keeps the rearrangement.
 *
 * <p>Which half is standing is kept on the sign, in the dashes around one of the two names. That
 * is the whole of what this mechanic remembers, it is saved with the world as the sign is, and a
 * builder can see it and correct it.
 */
@NullMarked
public final class ToggleArea implements SignMechanic {

    /** The sign that swaps areas in and out. */
    public static final String TOGGLE = "[Area]";

    /** The sign that also writes down whatever it is putting away. */
    public static final String SAVING = "[SaveArea]";

    /** The line saying whose areas these are. */
    public static final int NAMESPACE_LINE = 0;

    /** The line naming the area the sign places. */
    public static final int ON_LINE = 2;

    /** The line naming the area it swaps to, or {@code --} for clearing the space instead. */
    public static final int OFF_LINE = 3;

    /** The permission to make a sign that writes back over what it put away. */
    public static final String SAVE_SIGN_PERMISSION = "craftbook.area.create.save";

    /** The permission to make a sign using the areas everybody shares. */
    public static final String GLOBAL_PERMISSION = "craftbook.area.create.global";

    /** The permission to make a sign using somebody else's areas. */
    public static final String OTHER_PERMISSION = "craftbook.area.create.other";

    private static final List<String> NAMES = List.of(TOGGLE, SAVING);

    /** How a name is written while the area it names is the one standing in the world. */
    private static final Pattern STANDING = Pattern.compile("^-[A-Za-z0-9_]*-$");

    @Override
    public String name() {
        return Mechanics.AREA;
    }

    @Override
    public List<String> signNames() {
        return NAMES;
    }

    @Override
    public boolean act(MechanicVisit visit) {
        PostedSign sign = visit.sign();
        AreaVault vault = visit.world().vault();

        String namespace = sign.line(NAMESPACE_LINE);
        Optional<AreaName> placed = AreaName.parse(namespace, sign.line(ON_LINE));
        if (placed.isEmpty()) {
            visit.complain("This sign does not name an area to place.");
            return false;
        }

        Optional<AreaName> other = otherOn(sign, namespace);
        boolean saving = sign.isNamed(SAVING);

        // Power arriving puts the area up, the same way it shuts a door, so what the signal says
        // is the opposite of whether the area is already standing.
        boolean standing = visit.askedToShut()
                .map(shut -> !shut)
                .orElseGet(() -> isStanding(sign));

        return standing
                ? putAway(visit, vault, placed.get(), other, saving)
                : bringOut(visit, vault, placed.get(), other, saving);
    }

    @Override
    public SignReview review(SignLines lines, Actor builder, MechanicWorld world) {
        String written = lines.trimmedText(NAMESPACE_LINE);
        String namespace;

        if (written.equalsIgnoreCase(AreaName.GLOBAL)) {
            if (!builder.mayUse(GLOBAL_PERMISSION)) {
                return SignReview.refuse("You may not make an area everybody shares.");
            }
            namespace = AreaName.GLOBAL;
        } else if (written.isEmpty() || written.equalsIgnoreCase(builder.name())) {
            namespace = builder.name();
        } else {
            if (!builder.mayUse(OTHER_PERMISSION)) {
                return SignReview.refuse("You may not make an area in somebody else's name.");
            }
            namespace = written;
        }

        if (!AreaName.isUsableNamespace(namespace)) {
            return SignReview.refuse("The first line is not a name areas can be kept under.");
        }
        if (lines.trimmedText(PostedSign.NAME_LINE).equalsIgnoreCase(SAVING)
                && !builder.mayUse(SAVE_SIGN_PERMISSION)) {
            return SignReview.refuse("You may not make an area that writes back over itself.");
        }

        Optional<AreaName> placed = AreaName.parse(namespace, lines.trimmedText(ON_LINE));
        if (placed.isEmpty()) {
            return SignReview.refuse(
                    "The third line names the area to place: letters, digits and underscores, "
                            + "up to thirteen of them.");
        }
        if (!world.vault().has(placed.get())) {
            return SignReview.refuse("There is no area saved as " + placed.get() + ".");
        }

        String otherWritten = lines.trimmedText(OFF_LINE);
        if (AreaName.namesSomething(otherWritten)) {
            Optional<AreaName> other = AreaName.parse(namespace, otherWritten);
            if (other.isEmpty()) {
                return SignReview.refuse(
                        "The fourth line names the area to swap to, or " + AreaName.NONE
                                + " to clear the space instead.");
            }
            if (!world.vault().has(other.get())) {
                return SignReview.refuse("There is no area saved as " + other.get() + ".");
            }
        }

        return SignReview.keep(lines.withLine(NAMESPACE_LINE, namespace));
    }

    /** Takes the standing area out of the world, putting the other one up in its place. */
    private boolean putAway(
            MechanicVisit visit,
            AreaVault vault,
            AreaName placed,
            Optional<AreaName> other,
            boolean saving) {
        if (saving && !vault.capture(placed)) {
            visit.complain("There is no area saved as " + placed + ".");
            return false;
        }

        boolean done = other.isPresent() ? vault.restore(other.get()) : vault.clear(placed);
        if (!done) {
            visit.complain("That area could not be taken down.");
            return false;
        }

        mark(visit, false);
        visit.inform("Toggled.");
        return true;
    }

    /** Puts the standing area back, writing down whatever it replaces. */
    private boolean bringOut(
            MechanicVisit visit,
            AreaVault vault,
            AreaName placed,
            Optional<AreaName> other,
            boolean saving) {
        if (saving && other.isPresent() && !vault.capture(other.get())) {
            visit.complain("There is no area saved as " + other.get() + ".");
            return false;
        }
        if (!vault.restore(placed)) {
            visit.complain("There is no area saved as " + placed + ".");
            return false;
        }

        mark(visit, true);
        visit.inform("Toggled.");
        return true;
    }

    /**
     * The area on the fourth line, where the sign names one at all.
     *
     * <p>A sign with nothing there, or with the two dashes that stand for nothing, clears the
     * space rather than swapping to something else.
     */
    private static Optional<AreaName> otherOn(PostedSign sign, String namespace) {
        String written = sign.line(OFF_LINE);
        return AreaName.namesSomething(written)
                ? AreaName.parse(namespace, written)
                : Optional.empty();
    }

    /**
     * Whether the area the sign places is the one currently in the world.
     *
     * <p>Written in the dashes around whichever name is standing. A sign whose names are both
     * bare has never been used, and reads as standing so that the first use takes it down — which
     * is what a builder wants after saving the area from the world it is already in.
     */
    static boolean isStanding(PostedSign sign) {
        String placed = sign.line(ON_LINE);
        String other = sign.line(OFF_LINE);
        return STANDING.matcher(placed).matches()
                || !(AreaName.NONE.equals(other) || STANDING.matcher(other).matches());
    }

    /** Writes which half is now standing back onto the sign. */
    private static void mark(MechanicVisit visit, boolean standing) {
        int dashed = standing ? ON_LINE : OFF_LINE;
        int bare = standing ? OFF_LINE : ON_LINE;

        SignLines lines = visit.sign().lines();
        lines = lines.withLine(bare, stripped(lines.trimmedText(bare)));
        lines = lines.withLine(dashed, "-" + stripped(lines.trimmedText(dashed)) + "-");
        visit.world().writeSign(visit.sign().position(), lines);
    }

    private static String stripped(String written) {
        return written.replace("-", "");
    }
}
