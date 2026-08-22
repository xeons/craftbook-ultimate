// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.dispenser.DispenserRecipe;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/**
 * Which of the dispenser machines an operator allows.
 *
 * <p>One switch each rather than one for the lot, because they are six unrelated things that
 * happen to be built the same way: a server that wants a fan does not necessarily want a cannon
 * throwing lit dynamite about.
 *
 * @param allowed the recipes that work
 */
@NullMarked
public record DispenserSettings(Set<DispenserRecipe> allowed) {

    /** Every recipe allowed, which is what the mechanic does once it is switched on at all. */
    public static final DispenserSettings DEFAULTS =
            new DispenserSettings(EnumSet.allOf(DispenserRecipe.class));

    /** Copies the set. */
    public DispenserSettings {
        allowed = Collections.unmodifiableSet(allowed.isEmpty()
                ? EnumSet.noneOf(DispenserRecipe.class)
                : EnumSet.copyOf(allowed));
    }

    /** Whether one of the machines works. */
    public boolean allows(DispenserRecipe recipe) {
        return allowed.contains(recipe);
    }

    /** Whether any of them do, which decides whether a dispenser is looked at twice. */
    public boolean anythingAtAll() {
        return !allowed.isEmpty();
    }

    /** These settings with one of the machines allowed or refused. */
    public DispenserSettings with(DispenserRecipe recipe, boolean allowing) {
        Set<DispenserRecipe> changed = EnumSet.noneOf(DispenserRecipe.class);
        changed.addAll(allowed);
        if (allowing) {
            changed.add(recipe);
        } else {
            changed.remove(recipe);
        }
        return new DispenserSettings(changed);
    }
}
