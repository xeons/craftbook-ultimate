// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.chair;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Sitting down on a stair.
 *
 * <p>A chair is not built and carries no sign of its own. Any block an operator allows — every
 * stair out of the box — seats whoever right-clicks it empty-handed, and stays a stair for every
 * other purpose. What actually holds somebody up is an invisible marker put at the block and
 * ridden, so the game does the sitting and nothing has to be undone if the plugin stops.
 *
 * <p>What is here is the part that is arithmetic: where a seat sits inside its block, which sides
 * of a block may be clicked, where to put somebody who stands up, and how long they must wait
 * before sitting again. The marker itself, and the world it is put in, are a binding's job.
 */
@NullMarked
public final class Chairs {

    /** The name an operator switches it off by. */
    public static final String NAME = Mechanics.CHAIRS;

    /** The sign that makes a chair heal whoever is in it. */
    public static final String HEAL_SIGN = "[Sit Heal]";

    /** Sitting down by clicking a chair. */
    public static final String CLICK_USE = "craftbook.chairs.use";

    /** Sitting down with the command, wherever the player is standing. */
    public static final String COMMAND_USE = "craftbook.chairs.sit";

    /** Putting up a sign that makes a chair heal. */
    public static final String HEAL_BUILD = "craftbook.chairs.heal";

    /**
     * Where a seat sits inside the block it belongs to.
     *
     * <p>Middle of the block along both level sides, and low enough that a rider's legs end up on
     * the stair rather than hovering over it.
     */
    public static final Vec3d SEAT_OFFSET = new Vec3d(0.5, 0.3, 0.5);

    /** How long somebody must wait between sitting down and sitting down again, in seconds. */
    public static final long COOLDOWN_SECONDS = 3;

    /** How far below the seat a player standing up may be dropped to find them a floor. */
    private static final int[] NEARBY = {0, -1, 1};

    private Chairs() {
    }

    /** Whether a sign line is the one that makes a chair heal. */
    public static boolean isHealSign(String line) {
        return line.trim().equalsIgnoreCase(HEAL_SIGN);
    }

    /**
     * Where the seat of a chair is, given the block it is in.
     *
     * @param block the chair block
     */
    public static Vec3d seatIn(Vec3i block) {
        return new Vec3d(block.x(), block.y(), block.z()).add(SEAT_OFFSET);
    }

    /**
     * Where to try putting somebody who has stood up, nearest first.
     *
     * <p>The place they were sitting comes first, then one down, then one up, along each axis in
     * turn. Standing up ought to leave somebody where they were; the rest of the list is for a
     * chair that has been built into since they sat in it.
     *
     * @param from the block somebody is standing up out of
     */
    public static List<Vec3i> standingPlaces(Vec3i from) {
        List<Vec3i> places = new ArrayList<>(NEARBY.length * NEARBY.length * NEARBY.length);
        for (int x : NEARBY) {
            for (int y : NEARBY) {
                for (int z : NEARBY) {
                    places.add(new Vec3i(from.x() + x, from.y() + y, from.z() + z));
                }
            }
        }
        return places;
    }

    /**
     * Whether somebody may sit down yet.
     *
     * @param lastSatAt when they last sat down or stood up, in seconds since the epoch
     * @param now the time now, in the same units
     */
    public static boolean mayStandAgain(long lastSatAt, long now) {
        return now - lastSatAt > COOLDOWN_SECONDS;
    }

    /**
     * How much health somebody in a healing chair should have after one turn of healing.
     *
     * <p>Never past what they can hold, and never backwards, so a chair set to heal a negative
     * amount does nothing rather than becoming a trap.
     *
     * @param health what they have now
     * @param maximum what they can hold
     * @param amount what the chair heals by
     */
    public static double healed(double health, double maximum, double amount) {
        return Math.max(health, Math.min(maximum, health + Math.max(0, amount)));
    }
}
