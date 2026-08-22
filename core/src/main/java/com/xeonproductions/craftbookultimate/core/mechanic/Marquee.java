// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * A sign that tells whoever clicks it what a variable says.
 *
 * <p>A readout a builder puts in the world for other people: a score beside a scoreboard, a stock
 * count on a shop wall, a countdown at a gate. {@code /var get} answers the same question, but only
 * for somebody who already knows the variable is there and how it is spelt, which is the difference
 * between a command and a sign.
 *
 * <p>The name is a false friend and worth saying so once: {@code MC2999} and {@code MC3456} are
 * also called marquees, and they are chasing lights. This is the other sense of the word — a board
 * with something written on it — and shares no code with either.
 *
 * <p>From <b>upstream</b> rather than the fork, along with the variables themselves, so its sign
 * name, its lines and its namespace grammar are upstream's. The permission is not: upstream's
 * {@code craftbook.mech.marquee} becomes {@code craftbook.marquee}, which is what every mechanic
 * here is called.
 *
 * <p>Reads and never writes. There is nothing to switch, so redstone arriving at one does nothing
 * at all — a sign that told the empty air what a variable said would be a way of announcing it to
 * nobody once a tick.
 */
@NullMarked
public final class Marquee implements SignMechanic {

    /** The sign that makes one. */
    public static final String SIGN = "[Marquee]";

    /** The line the variable is named on. */
    public static final int VARIABLE_LINE = 2;

    /** The line the namespace is named on, blank meaning the shared one. */
    public static final int NAMESPACE_LINE = 3;

    private static final Marquee INSTANCE = new Marquee();

    private Marquee() {
    }

    /** The one of these there is. */
    public static Marquee instance() {
        return INSTANCE;
    }

    @Override
    public String name() {
        return Mechanics.MARQUEE;
    }

    @Override
    public List<String> signNames() {
        return List.of(SIGN);
    }

    @Override
    public boolean act(MechanicVisit visit) {
        Optional<Actor> who = visit.actor();
        if (who.isEmpty()) {
            return false;
        }

        Optional<VariableName> variable = variableOn(visit.sign().lines());
        if (variable.isEmpty()) {
            who.get().complain("This sign does not name a variable.");
            return true;
        }

        // A variable can be deleted long after its sign was written, and refusing a sign
        // retrospectively is not a thing that can happen, so the reading says so instead.
        Optional<String> value = visit.world().variables().get(variable.get());
        if (value.isEmpty()) {
            who.get().complain("There is no variable called " + variable.get() + " any more.");
            return true;
        }

        who.get().inform(value.get());
        return true;
    }

    /**
     * Checks the sign as it is written.
     *
     * <p>The same check the three variable chips make, and for the same reason: what a variable is
     * called lives in the store rather than in the blocks beside the sign, so a sign naming one
     * nobody has made would be silently dead and its builder would have nothing to tell that from
     * a wiring fault.
     */
    @Override
    public SignReview review(SignLines lines, Actor builder, MechanicWorld world) {
        Optional<VariableName> variable = variableOn(lines);
        if (variable.isEmpty()) {
            return SignReview.refuse(
                    "Line 3 names the variable, in letters, digits and underscores. Put a "
                            + "namespace and a " + VariableName.SEPARATOR + " before it, or write "
                            + "the namespace on line 4, to name somebody else's.");
        }

        VariableName name = variable.get();
        if (!world.variables().has(name)) {
            return SignReview.refuse("There is no variable called " + name
                    + ". Make one with /var define " + name + " 0.");
        }
        if (!VariableChips.mayUse(name, builder)) {
            return SignReview.refuse("The variable " + name + " belongs to " + name.namespace()
                    + ", and you may only read your own and the shared ones.");
        }

        return SignReview.keep(lines);
    }

    /**
     * The variable a sign names.
     *
     * <p>Two spellings, both upstream's and both kept: {@code alice|score} on line 3 alone, or
     * {@code score} on line 3 with {@code alice} on line 4. A namespace written on line 3 wins,
     * since it is the more specific of the two.
     */
    public static Optional<VariableName> variableOn(SignLines lines) {
        String namespace = lines.trimmedText(NAMESPACE_LINE);
        return VariableName.parse(
                lines.trimmedText(VARIABLE_LINE),
                namespace.isEmpty() ? VariableName.SHARED : namespace);
    }
}
