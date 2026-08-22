// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.sponge.adapter.Signs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.block.entity.Sign;

/**
 * Saying on the sign itself that a chip cannot work as written.
 *
 * <p>A chip built before its lines were written down is never refused, so nobody is ever told about
 * it, and a builder has no way to tell that from a wiring fault. A red title says broken now — and
 * the mark comes off again the moment the line is filled in, so it never says broken once.
 *
 * <p>Line one is the plugin's own, overwritten with the chip's shorthand as the sign is created, so
 * nothing a builder wrote is at stake and only its colour changes.
 */
@NullMarked
public final class ChipTitle {

    public static final int LINE = 0;

    private static final NamedTextColor BROKEN = NamedTextColor.RED;

    private ChipTitle() {}

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

    public static boolean wouldChange(SignLines lines, ICDefinition definition) {
        return broken(lines, definition) != BROKEN.equals(lines.line(LINE).color());
    }

    private static boolean broken(SignLines lines, ICDefinition definition) {
        return LineReview.of(definition, lines).broken();
    }
}
