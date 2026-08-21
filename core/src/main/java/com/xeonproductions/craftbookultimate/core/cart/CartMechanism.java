// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The three blocks a cart mechanic is built from.
 *
 * <p>A piece of rail, the block holding it up, and usually a sign saying what to do. The block
 * underneath is what says which mechanic this is — brown wool clears a destination, obsidian is a
 * station — and the sign carries whatever that mechanic needs told.
 *
 * <p>The sign sits directly below the base block, two below it, or against one of its four sides,
 * which is the arrangement builders have always used and what a resolver looks for.
 *
 * @param rail where the cart runs
 * @param base the block under the rail, which says which mechanic this is
 * @param baseBlock what that block is
 * @param sign the sign, where there is one
 */
@NullMarked
public record CartMechanism(Vec3i rail, Vec3i base, Key baseBlock, Optional<MechanismSign> sign) {

    /** A mechanism with no sign, which is all several of them need. */
    public static CartMechanism unsigned(Vec3i rail, Vec3i base, Key baseBlock) {
        return new CartMechanism(rail, base, baseBlock, Optional.empty());
    }

    /** A mechanism with a sign. */
    public static CartMechanism signed(
            Vec3i rail, Vec3i base, Key baseBlock, MechanismSign sign) {
        return new CartMechanism(rail, base, baseBlock, Optional.of(sign));
    }

    /** Whether there is a sign to read. */
    public boolean hasSign() {
        return sign.isPresent();
    }

    /** A line of the sign, or blank where there is no sign. */
    public String line(int index) {
        return sign.map(s -> s.lines().trimmedText(index)).orElse("");
    }

    /**
     * Whether the sign names this mechanic.
     *
     * <p>The name goes on the second line, in brackets, the way every sign mechanic in the plugin
     * is named. Compared without regard to case, since a builder types it by hand.
     *
     * @param name the mechanic's name without its brackets, such as {@code Station}
     */
    public boolean isNamed(String name) {
        return line(MechanismSign.NAME_LINE).equalsIgnoreCase("[" + name + "]");
    }

    /** The three blocks of the mechanism, the sign included where there is one. */
    public List<Vec3i> blocks() {
        return sign.map(s -> List.of(rail, base, s.position()))
                .orElseGet(() -> List.of(rail, base));
    }

    /**
     * The sign on a cart mechanism.
     *
     * <p>A mechanic pushes a cart the way the sign is looking away from, so a sign facing the
     * platform sends the cart off down the track behind it.
     *
     * @param position where the sign is
     * @param lines what it says
     * @param facing the way the sign looks, which is out of whatever it is fixed to
     */
    public record MechanismSign(Vec3i position, SignLines lines, BlockFace facing) {

        /** The line carrying the mechanic's name in brackets. */
        public static final int NAME_LINE = 1;

        /** The way a cart leaves this mechanism, which is behind the sign. */
        public BlockFace outward() {
            return facing.opposite();
        }
    }
}
