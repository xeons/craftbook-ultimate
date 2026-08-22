// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Saying which loaded chips cannot work.
 *
 * <p>The signs of broken chips are already marked red as they load, but that only helps somebody
 * standing in front of one. An operator asking what is wrong across a server needs the list, and
 * needs it without walking the map.
 *
 * <p>Nothing here touches a block. Every answer is built from what the caller was already handed,
 * which is what makes this safe to run on a busy server and what makes it the right tool for the
 * question rather than a sweep that repaints as it goes.
 */
@NullMarked
public final class CheckActions {

    /** The permission to ask. */
    public static final String CHECK = "craftbook.check";

    /** How many broken chips are listed before the reply gives up and counts the rest. */
    private static final int LISTED = 12;

    private CheckActions() {
    }

    /**
     * Reports the broken chips among however many are loaded.
     *
     * <p>Nearest first where the caller is standing somewhere, because the one they can walk to is
     * the one they are most likely asking about. A caller standing nowhere — the console — gets
     * them in whatever order they were found, since there is no nearest.
     */
    public static boolean report(Caller caller, List<BrokenChip> broken, int loaded) {
        if (broken.isEmpty()) {
            caller.send(Component.text(
                    "All " + loaded + " loaded chips have what they need.", NamedTextColor.GREEN));
            return true;
        }

        List<BrokenChip> ordered = new ArrayList<>(broken);
        caller.standing().ifPresent(standing -> ordered.sort(Comparator.comparingLong(
                chip -> standing.distanceTo(chip.world(), chip.at()))));

        caller.heading(broken.size() + " of " + loaded + " loaded chips cannot work as written. "
                + "Their signs are titled in red.");

        for (BrokenChip chip : ordered.subList(0, Math.min(LISTED, ordered.size()))) {
            caller.send(Component.text("  " + chip.where(), NamedTextColor.AQUA)
                    .append(Component.text("  " + chip.model(), NamedTextColor.WHITE)));
            for (LineReview.Blank blank : chip.review().missing()) {
                caller.detail("    " + blank.said());
            }
        }

        if (ordered.size() > LISTED) {
            caller.heading("  and " + (ordered.size() - LISTED) + " more.");
        }
        return true;
    }
}
