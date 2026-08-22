// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Making and changing the variables the VAR chips read.
 *
 * <p>A variable has to exist before a sign can name one, so this is where every variable starts
 * life. That is deliberate rather than ceremonial: a chip naming a variable nobody has made would
 * be silently dead, so the chips refuse such a sign, and this is what a builder uses to put that
 * right.
 *
 * <p>Making and changing are separate verbs. Defining makes a variable and will not touch one that
 * already exists; setting changes one and will not make one. A command meaning to change a running
 * score cannot then quietly create a second one under a misspelling and leave the original where it
 * was.
 *
 * <p>Which variables somebody may touch follows the same rule here as it does on a sign: the shared
 * ones and their own always, anybody else's only with
 * {@link VariableChips#OTHER_NAMESPACE_PERMISSION}.
 */
@NullMarked
public final class VariableActions {

    /** The permission to make a variable. */
    public static final String DEFINE = "craftbook.variables.define";

    /** The permission to change one, by setting it or by doing a sum to it. */
    public static final String SET = "craftbook.variables.set";

    /** The permission to read one. */
    public static final String GET = "craftbook.variables.get";

    /** The permission to list them. */
    public static final String LIST = "craftbook.variables.list";

    /** The permission to remove one. */
    public static final String DELETE = "craftbook.variables.delete";

    /** How many variables a listing shows before it stops. */
    private static final int LISTING_LIMIT = 60;

    private final Variables variables;
    private final Runnable save;

    /**
     * @param variables the variables to work on
     * @param save writes them out after one has changed
     */
    public VariableActions(Variables variables, Runnable save) {
        this.variables = variables;
        this.save = save;
    }

    /** Makes a variable, refusing to touch one that is already there. */
    public boolean define(Caller caller, String written, String value) {
        Optional<VariableName> name = named(caller, written);
        if (name.isEmpty()) {
            return false;
        }

        if (!Variables.isStorable(value)) {
            caller.refuse("A value is letters, digits and . , : ; _ + - with no spaces.");
            return false;
        }
        if (!variables.define(name.get(), value)) {
            caller.refuse("There is already a variable called " + name.get()
                    + ". Change it with /var set.");
            return false;
        }

        save.run();
        caller.tell("Variable " + name.get() + " is now " + value + ".");
        return true;
    }

    /** Changes a variable, refusing to make one that is not there. */
    public boolean set(Caller caller, String written, String value) {
        Optional<VariableName> name = named(caller, written);
        if (name.isEmpty()) {
            return false;
        }

        if (!Variables.isStorable(value)) {
            caller.refuse("A value is letters, digits and . , : ; _ + - with no spaces.");
            return false;
        }
        if (!variables.set(name.get(), value)) {
            return missing(caller, name.get());
        }

        save.run();
        caller.tell("Variable " + name.get() + " is now " + value + ".");
        return true;
    }

    /** Reads a variable out. */
    public boolean get(Caller caller, String written) {
        Optional<VariableName> name = named(caller, written);
        if (name.isEmpty()) {
            return false;
        }

        Optional<String> value = variables.get(name.get());
        if (value.isEmpty()) {
            return missing(caller, name.get());
        }

        caller.tell(name.get() + " is " + value.get() + ".");
        return true;
    }

    /** Removes a variable, after which any chip naming it does nothing. */
    public boolean delete(Caller caller, String written) {
        Optional<VariableName> name = named(caller, written);
        if (name.isEmpty()) {
            return false;
        }

        if (!variables.remove(name.get())) {
            return missing(caller, name.get());
        }

        save.run();
        caller.tell("Variable " + name.get() + " is gone. Any chip naming it now does nothing.");
        return true;
    }

    /** Does a sum to a variable, which is what the four arithmetic commands all come down to. */
    public boolean apply(
            Caller caller, String written, VariableChips.Function function, double amount) {

        Optional<VariableName> name = named(caller, written);
        if (name.isEmpty()) {
            return false;
        }

        OptionalDouble held = variables.number(name.get());
        if (held.isEmpty()) {
            if (!variables.has(name.get())) {
                return missing(caller, name.get());
            }
            caller.refuse("Variable " + name.get() + " does not hold a number, so there is "
                    + "nothing to do a sum to.");
            return false;
        }

        variables.setNumber(name.get(), function.apply(held.getAsDouble(), amount));

        save.run();
        caller.tell("Variable " + name.get() + " is now "
                + variables.get(name.get()).orElse(Variables.DEFAULT_VALUE) + ".");
        return true;
    }

    /** Lists the variables there are, in one namespace or in all of them. */
    public boolean list(Caller caller, Optional<String> namespace) {
        List<VariableName> names = namespace
                .map(variables::namesIn)
                .orElseGet(variables::names);

        if (names.isEmpty()) {
            caller.tell(namespace
                    .map(where -> "There are no variables in " + where + ".")
                    .orElse("There are no variables yet. Make one with /var define."));
            return true;
        }

        caller.heading("Variables (" + names.size() + ")");
        for (VariableName name : names.stream().limit(LISTING_LIMIT).toList()) {
            caller.send(Component.text("  " + name + "  ", NamedTextColor.GRAY)
                    .append(Component.text(
                            variables.get(name).orElse(""), NamedTextColor.WHITE)));
        }
        if (names.size() > LISTING_LIMIT) {
            caller.tell("and " + (names.size() - LISTING_LIMIT)
                    + " more. Name a namespace to narrow it down.");
        }
        return true;
    }

    /** The variable names that carry on from what has been typed so far. */
    public List<String> known(String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> matching = new ArrayList<>();
        for (VariableName name : variables.names()) {
            String written = name.toString();
            if (written.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matching.add(written);
            }
        }
        return matching;
    }

    /**
     * The variable a command names, once it is known to be one this caller may touch.
     *
     * <p>A bare name means the shared variable, exactly as it does on a sign. Defaulting to the
     * caller's own namespace instead would make a command and a sign reading the same word name
     * different variables, which is the one thing a builder checking their work by command must be
     * able to rely on.
     */
    private Optional<VariableName> named(Caller caller, String written) {
        Optional<VariableName> name = VariableName.parse(written, VariableName.SHARED);

        if (name.isEmpty()) {
            caller.refuse("A variable name is letters, digits and underscores. Put a namespace "
                    + "and a " + VariableName.SEPARATOR + " before it to name somebody else's.");
            return Optional.empty();
        }

        if (!mayTouch(name.get(), caller)) {
            caller.refuse("The variable " + name.get() + " belongs to " + name.get().namespace()
                    + ", and you may only use your own and the shared ones.");
            return Optional.empty();
        }

        return name;
    }

    /** Whether somebody may touch a variable, by the same rule that governs building on one. */
    private static boolean mayTouch(VariableName name, Caller caller) {
        return name.isShared()
                || name.namespace().equalsIgnoreCase(caller.name())
                || caller.may(VariableChips.OTHER_NAMESPACE_PERMISSION);
    }

    private static boolean missing(Caller caller, VariableName name) {
        caller.refuse("There is no variable called " + name + ". Make one with /var define "
                + name + " 0.");
        return false;
    }
}
