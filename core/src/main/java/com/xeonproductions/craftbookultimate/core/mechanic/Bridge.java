// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A deck that runs out across a gap and pulls back again.
 *
 * <p>Two signs facing each other across the gap, each with the bridge's material directly above
 * or below it. The deck runs between them at the material's own height, as wide as the landings
 * at both ends are: laying another row of blocks beside each sign widens the bridge without the
 * signs being touched.
 *
 * <p>Only the sign reading {@code [Bridge]} works it. The far sign reads {@code [Bridge End]} and
 * is there to be found, so that a bridge cannot be worked from the side somebody is standing on
 * when it retracts.
 */
@NullMarked
public final class Bridge implements SignMechanic {

    /** The sign that works the bridge. */
    public static final String START = "[Bridge]";

    /** The sign at the far end, which is there to be found rather than to be used. */
    public static final String END = "[Bridge End]";

    private static final List<String> NAMES = List.of(START, END);

    /** Where the deck sits relative to the sign, in the order the two are tried. */
    private static final BlockFace[] SIDES = {BlockFace.DOWN, BlockFace.UP};

    @Override
    public String name() {
        return Mechanics.BRIDGE;
    }

    @Override
    public List<String> signNames() {
        return NAMES;
    }

    @Override
    public boolean act(MechanicVisit visit) {
        PostedSign sign = visit.sign();
        if (!sign.isNamed(START)) {
            visit.complain("A bridge is worked from the sign at its other end.");
            return false;
        }

        MechanicWorld world = visit.world();
        Settings settings = visit.settings();

        Optional<BlockFace> side = deckSide(visit);
        if (side.isEmpty()) {
            visit.complain("A bridge needs its material directly above or below the sign.");
            return false;
        }

        Vec3i base = sign.position().offset(side.get());
        Key material = world.blockAt(base);

        Optional<PostedSign> far = world.nextSign(
                sign.position(), sign.back(), settings.maxLength(), other -> claims(other.lines()));
        if (far.isEmpty()) {
            visit.complain("The other end of the bridge is missing.");
            return false;
        }

        Vec3i farBase = far.get().position().offset(side.get());
        if (!world.blockAt(farBase).equals(material)) {
            visit.complain("The other end of the bridge is made of something else.");
            return false;
        }

        int length = sign.position().chebyshevDistance(far.get().position()) - 1;
        if (length < 1) {
            visit.complain("The two ends of the bridge are touching.");
            return false;
        }

        int left = Panels.widthAlong(
                world, base, farBase, sign.left(), material, settings.maxWidth());
        int right = Panels.widthAlong(
                world, base, farBase, sign.right(), material, settings.maxWidth());

        Panel deck = Panel.between(
                base.offset(sign.left(), left).offset(sign.back()),
                base.offset(sign.right(), right).offset(sign.back(), length));

        if (visit.isByHand() && Panels.isObstructed(world, deck, material)) {
            visit.complain("Something is in the way of the bridge.");
            return false;
        }

        return Panels.toggle(visit, deck, material, "bridge");
    }

    /**
     * Which side of the sign the deck is on.
     *
     * <p>Below is looked at first, because a bridge somebody walks over is the ordinary case and
     * one they walk under is not.
     */
    private static Optional<BlockFace> deckSide(MechanicVisit visit) {
        for (BlockFace side : SIDES) {
            Key block = visit.world().blockAt(visit.sign().position().offset(side));
            if (Panels.isBuildable(block, visit.settings())) {
                return Optional.of(side);
            }
        }
        return Optional.empty();
    }
}
