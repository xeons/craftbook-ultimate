// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.boat;

import com.xeonproductions.craftbookultimate.core.config.BoatHabits;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What every boat in the world does, with nothing built and no sign anywhere.
 *
 * <p>The counterpart of {@link com.xeonproductions.craftbookultimate.core.cart.CartBehaviour}, and
 * together for the same reason: the habits overlap. Two of them answer the same dismount, and the
 * decisions belong beside one another rather than scattered across a listener.
 *
 * <p>Every decision here is a pure function of a boat and the settings. Nothing reaches into a
 * world, so all of it is exercised in plain JUnit and the binding is left with only the doing.
 */
@NullMarked
public final class BoatBehaviour {

    /** How much a boat hurts what it runs into, in half hearts. */
    public static final double RUN_DOWN_DAMAGE = 10;

    /** How hard something run down is thrown clear. */
    private static final double THROWN_CLEAR = 1.6;

    /** How far up, so it is knocked off the water rather than dragged along under the hull. */
    private static final double THROWN_UP = 0.3;

    private BoatBehaviour() {
    }

    /**
     * Whether a boat left sitting empty is ready to be taken away.
     *
     * <p>Asked when the wait is already over, so all that is left is whether it is still there and
     * still empty — somebody may have climbed back in, and that should call the whole thing off.
     */
    public static boolean hasSatEmpty(Boat boat, BoatHabits habits) {
        return habits.decaysEmptyBoats() && boat.isPresent() && !boat.isOccupied();
    }

    /**
     * What a moving boat does to something it runs into.
     *
     * <p>Only a boat with somebody aboard does anything at all, which is the rule the carts follow
     * and for the same reason: a boat drifting on a current is not a weapon.
     *
     * @param boat the boat doing the running down
     * @param hit whatever is in its way
     */
    public static Optional<RunDown> runDown(Boat boat, Bystander hit, BoatHabits habits) {
        if (!habits.runDownEntities() || !boat.isOccupied() || isAboard(boat, hit)) {
            return Optional.empty();
        }

        if (isBoat(hit) && !habits.runDownOtherBoats()) {
            return Optional.empty();
        }
        if (hit.isLiving()) {
            return Optional.of(new RunDown(RUN_DOWN_DAMAGE, thrownClear(boat)));
        }
        // Nothing that is not alive can be hurt, so where hurting is the most that is allowed
        // there is nothing left to do to it.
        return habits.runDownOnlyHurts() ? Optional.empty() : Optional.of(RunDown.REMOVED);
    }

    /** Whether something is riding the boat that is running it down. */
    private static boolean isAboard(Boat boat, Bystander who) {
        for (Bystander rider : boat.riders()) {
            if (rider.equals(who)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether something hit is itself a boat.
     *
     * <p>Read off the entity's own name rather than by asking the platform, since every boat and
     * raft the game has — and any the game adds later — ends its name the same way.
     */
    private static boolean isBoat(Bystander hit) {
        String name = hit.type().value();
        return name.endsWith("boat") || name.endsWith("raft");
    }

    /** Which way something run down is thrown, which is on and up from where the boat is going. */
    private static Vec3d thrownClear(Boat boat) {
        Vec3d going = boat.velocity();
        if (going.length() <= 0) {
            return Vec3d.ZERO;
        }
        return going.normalise().multiply(THROWN_CLEAR).add(new Vec3d(0, THROWN_UP, 0));
    }

    /** What running something down comes to. */
    public record RunDown(double damage, Vec3d thrownClear) {

        /** Taken out of the world rather than hurt, which is all that can be done to a thing. */
        public static final RunDown REMOVED = new RunDown(0, Vec3d.ZERO);

        /** Whether this is a removal rather than an injury. */
        public boolean removes() {
            return damage <= 0;
        }
    }
}
