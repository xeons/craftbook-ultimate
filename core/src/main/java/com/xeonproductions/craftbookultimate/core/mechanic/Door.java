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
 * A wall that fills a doorway and clears out of it again.
 *
 * <p>A bridge stood on end. Two signs, one above the other, each with the door's material on the
 * inward side of it; the panel fills the space between those two rows and is as wide as both
 * frames are.
 *
 * <p>{@code [Door Up]} is written on the lower sign and looks upwards for its partner;
 * {@code [Door Down]} is written on the upper sign and looks down. {@code [Door]} is neither: it
 * marks an end that answers to the other sign and does nothing when worked itself.
 */
@NullMarked
public final class Door implements SignMechanic {

    /** The sign at the bottom of a door, which looks upwards for the other end. */
    public static final String UP = "[Door Up]";

    /** The sign at the top of a door, which looks downwards for the other end. */
    public static final String DOWN = "[Door Down]";

    /** The sign that only marks an end. */
    public static final String END = "[Door]";

    private static final List<String> NAMES = List.of(UP, DOWN, END);

    @Override
    public String name() {
        return "Door";
    }

    @Override
    public List<String> signNames() {
        return NAMES;
    }

    @Override
    public boolean act(MechanicVisit visit) {
        PostedSign sign = visit.sign();
        if (sign.isNamed(END)) {
            visit.complain("A door is worked from the sign that says which way it runs.");
            return false;
        }

        MechanicWorld world = visit.world();
        Settings settings = visit.settings();
        BlockFace towards = sign.isNamed(UP) ? BlockFace.UP : BlockFace.DOWN;

        Vec3i base = sign.position().offset(towards);
        Key material = world.blockAt(base);
        if (!Panels.isBuildable(material, settings)) {
            visit.complain("A door needs its material directly " + (towards == BlockFace.UP
                    ? "above" : "below") + " the sign.");
            return false;
        }

        Optional<PostedSign> far = world.nextSign(
                sign.position(), towards, settings.maxLength(), other -> claims(other.lines()));
        if (far.isEmpty()) {
            visit.complain("The other end of the door is missing.");
            return false;
        }

        Vec3i bottom = towards == BlockFace.UP ? base : far.get().position().offset(BlockFace.UP);
        Vec3i top = towards == BlockFace.UP ? far.get().position().offset(BlockFace.DOWN) : base;

        if (!world.blockAt(bottom).equals(world.blockAt(top))) {
            visit.complain("Both ends of the door must be made of the same thing.");
            return false;
        }
        if (top.y() - bottom.y() < 2) {
            visit.complain("There is no room inside the door.");
            return false;
        }

        int left = Panels.widthAlong(
                world, bottom, top, sign.left(), material, settings.maxWidth());
        int right = Panels.widthAlong(
                world, bottom, top, sign.right(), material, settings.maxWidth());

        Panel panel = Panel.between(
                bottom.offset(sign.left(), left).offset(BlockFace.UP),
                top.offset(sign.right(), right).offset(BlockFace.DOWN));

        if (visit.isByHand() && Panels.isObstructed(world, panel, material)) {
            visit.complain("Something is in the way of the door.");
            return false;
        }

        return Panels.toggle(visit, panel, material, "door");
    }
}
