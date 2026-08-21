// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.entity;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * A roster held in memory, so a test can put somebody online and see what a chip makes of it.
 */
@NullMarked
public final class SimpleRoster implements Roster {

    private final List<String> names = new ArrayList<>();

    /** A roster with nobody on it. */
    public static SimpleRoster empty() {
        return new SimpleRoster();
    }

    /** A roster with these people on it. */
    public static SimpleRoster of(String... names) {
        SimpleRoster roster = new SimpleRoster();
        for (String name : names) {
            roster.add(name);
        }
        return roster;
    }

    @Override
    public List<String> visibleNames() {
        return List.copyOf(names);
    }

    /** Puts somebody online. */
    public SimpleRoster add(String name) {
        names.add(name);
        return this;
    }

    /** Takes somebody offline. */
    public SimpleRoster remove(String name) {
        names.remove(name);
        return this;
    }
}
