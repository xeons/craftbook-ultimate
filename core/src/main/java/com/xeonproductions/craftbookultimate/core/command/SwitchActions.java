// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.control.PasswordStore;
import com.xeonproductions.craftbookultimate.core.control.Switchboard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Throwing the switches the command controlled chips follow.
 *
 * <p>Two families, deliberately kept apart. One board holds the switches anybody may throw; the
 * other holds those that take a password. A switch name on one is unrelated to the same name on
 * the other.
 *
 * <p>Checking a password is deliberately slow, so it happens away from the thread that ticks the
 * world. That is why the reply to a guarded switch arrives a moment after the command rather than
 * instantly, and why spamming it cannot hold the server up.
 */
@NullMarked
public final class SwitchActions {

    /** The permission to see what switches there are. */
    public static final String LIST = "craftbook.switch.list";

    /** What a switch may be told to do, beyond being toggled. */
    public static final List<String> MODES = List.of("on", "off", "state");

    private final Switchboard open;
    private final Switchboard guarded;
    private final PasswordStore passwords;
    private final Consumer<Runnable> offThread;
    private final Runnable savePasswords;
    private final Runnable saveSwitches;

    /**
     * @param open the switches anyone may throw
     * @param guarded the switches that take a password
     * @param passwords the passwords guarding those switches
     * @param offThread runs work away from the thread that ticks the world
     * @param savePasswords writes the passwords out after one has changed
     * @param saveSwitches writes the switch positions out after one has been thrown
     */
    public SwitchActions(
            Switchboard open,
            Switchboard guarded,
            PasswordStore passwords,
            Consumer<Runnable> offThread,
            Runnable savePasswords,
            Runnable saveSwitches) {
        this.open = open;
        this.guarded = guarded;
        this.passwords = passwords;
        this.offThread = offThread;
        this.savePasswords = savePasswords;
        this.saveSwitches = saveSwitches;
    }

    /** Throws a switch anybody may throw. */
    public boolean toggleOpen(Caller caller, String name, Optional<String> mode) {
        if (!open.isKnown(name)) {
            caller.refuse("No chip is following a switch called " + name + ".");
            return false;
        }
        return apply(caller, open, name, mode);
    }

    /** Throws a switch that takes a password, once the password has been checked. */
    public boolean toggleGuarded(
            Caller caller, String name, String password, Optional<String> mode) {

        if (!guarded.isKnown(name)) {
            caller.refuse("No chip is following a switch called " + name + ".");
            return false;
        }
        if (!passwords.hasPassword(name)) {
            caller.refuse("That switch has no password yet. Set one with /mcx121pass add.");
            return false;
        }

        offThread.accept(() -> {
            if (!passwords.matches(name, password)) {
                caller.refuse("Wrong password.");
                return;
            }
            apply(caller, guarded, name, mode);
        });
        return true;
    }

    /** Sets a password on a switch that has none. */
    public boolean addPassword(Caller caller, String name, String password) {
        if (!guarded.isKnown(name)) {
            caller.refuse("No chip is following a switch called " + name + ".");
            return false;
        }
        if (!PasswordStore.isSaveableName(name)) {
            caller.refuse("A switch with a colon in its name cannot have a password saved.");
            return false;
        }
        if (passwords.hasPassword(name)) {
            caller.refuse("That switch already has a password. Use /mcx121pass change.");
            return false;
        }

        offThread.accept(() -> {
            if (!passwords.setPassword(name, password)) {
                caller.refuse("That password could not be set.");
                return;
            }
            savePasswords.run();
            caller.tell("Password set for switch " + name + ".");
        });
        return true;
    }

    /** Changes the password on a switch that has one. */
    public boolean changePassword(
            Caller caller, String name, String oldPassword, String newPassword) {

        offThread.accept(() -> {
            if (!passwords.changePassword(name, oldPassword, newPassword)) {
                caller.refuse("Wrong password, or that switch has none set.");
                return;
            }
            savePasswords.run();
            caller.tell("Password changed for switch " + name + ".");
        });
        return true;
    }

    /** Says whether a switch has a password, without saying what it is. */
    public boolean hasPassword(Caller caller, String name) {
        caller.tell(passwords.hasPassword(name)
                ? "Switch " + name + " has a password."
                : "Switch " + name + " has no password.");
        return true;
    }

    /** Lists the switches anybody may throw. */
    public boolean listOpen(Caller caller) {
        return list(caller, open, "Switches");
    }

    /** Lists the switches that take a password. */
    public boolean listGuarded(Caller caller) {
        return list(caller, guarded, "Guarded switches");
    }

    /** The names on a board that carry on from what has been typed so far. */
    public List<String> openNames(String typed) {
        return names(open, typed);
    }

    /** The guarded names that carry on from what has been typed so far. */
    public List<String> guardedNames(String typed) {
        return names(guarded, typed);
    }

    /** Throws a switch, or reports where it is standing. */
    private boolean apply(
            Caller caller, Switchboard board, String name, Optional<String> mode) {

        String wanted = mode.map(text -> text.toLowerCase(Locale.ROOT)).orElse("");

        switch (wanted) {
            case "" -> {
                Optional<Boolean> position = board.toggle(name);
                if (position.isEmpty()) {
                    caller.refuse("That switch has never been thrown, so there is nothing to "
                            + "toggle. Use on or off.");
                    return false;
                }
                caller.tell("Switch " + name + " is now " + describe(position.get()) + ".");
                saveSwitches.run();
            }
            case "on", "off" -> {
                board.set(name, wanted.equals("on"));
                caller.tell("Switch " + name + " is now " + wanted + ".");
                saveSwitches.run();
            }
            case "state" -> {
                Optional<Boolean> position = board.state(name);
                caller.tell(position
                        .map(on -> "Switch " + name + " is " + describe(on) + ".")
                        .orElse("Switch " + name + " has never been thrown."));
            }
            default -> {
                caller.refuse("Say on, off or state, or nothing at all to toggle it.");
                return false;
            }
        }
        return true;
    }

    private static boolean list(Caller caller, Switchboard board, String title) {
        if (board.size() == 0) {
            caller.tell("No chip is following any switch right now.");
            return true;
        }

        caller.heading(title + " (" + board.size() + ")");
        for (String name : board.names()) {
            Optional<Boolean> position = board.state(name);
            caller.send(Component.text("  " + name + "  ", NamedTextColor.GRAY)
                    .append(position
                            .map(on -> Component.text(
                                    describe(on), on ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .orElse(Component.text("never thrown", NamedTextColor.DARK_GRAY))));
        }
        return true;
    }

    private static List<String> names(Switchboard board, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> matching = new ArrayList<>();
        for (String name : board.names()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matching.add(name);
            }
        }
        return matching;
    }

    private static String describe(boolean on) {
        return on ? "on" : "off";
    }
}
