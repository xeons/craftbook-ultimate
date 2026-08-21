// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Sign;
import org.jspecify.annotations.NullMarked;

/**
 * The first line of a chip's sign, and the colour it is written in.
 *
 * <p>That line belongs to the plugin: it is filled in with the chip's shorthand as the sign is
 * created and is never something a builder wrote, so recolouring it puts nothing of theirs at
 * risk.
 *
 * <p>It is written red where the chip cannot do anything at all as its sign stands. A sign being
 * written now is refused outright instead, so the only chips this ever marks are ones that were
 * already in the world before the rule saying they needed more — and for those there is nobody
 * standing there to tell.
 *
 * <p>Red is not the whole of it: the mark comes off again once the sign is filled in, so a red
 * title always means a chip that is broken now rather than one that used to be. Both directions
 * are conditional on the colour actually differing, which is what makes loading a world of working
 * chips cost no writes at all.
 */
@NullMarked
public final class ChipTitle {

    /** The sign line a chip's shorthand is written on. */
    public static final int LINE = 0;

    /** The colour a chip that cannot work is titled in. */
    private static final NamedTextColor BROKEN = NamedTextColor.RED;

    private ChipTitle() {}

    /**
     * Recolours a chip's title to say whether it can work, if it is not already that colour.
     *
     * @param sign the chip's sign
     * @param definition the chip the sign names
     * @return true if the sign was written to
     */
    public static boolean mark(Sign sign, ICDefinition definition) {
        SignLines lines = Signs.read(sign);
        if (!wouldChange(lines, definition)) {
            return false;
        }

        // Only the colour changes, so a title carrying anything other than the plain shorthand
        // survives being marked and unmarked.
        Component title = lines.line(LINE);
        Signs.writeLine(sign, LINE, title.color(broken(lines, definition) ? BROKEN : null));
        return true;
    }

    /**
     * Whether marking this sign would write to it.
     *
     * <p>Asked before anything is scheduled, so a chip whose title is already right costs a
     * comparison and nothing else.
     */
    public static boolean wouldChange(SignLines lines, ICDefinition definition) {
        return broken(lines, definition) != BROKEN.equals(lines.line(LINE).color());
    }

    private static boolean broken(SignLines lines, ICDefinition definition) {
        return LineReview.of(definition, lines).broken();
    }
}
