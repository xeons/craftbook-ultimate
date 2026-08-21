// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * A lift that carries somebody to the next sign above or below.
 *
 * <p>Every floor of a shaft has a sign in the same column, and a rider is taken to the next one
 * along rather than to a numbered floor, so a floor added between two others simply becomes the
 * next stop. Where the sign at the far end names the floor on its first line, the rider is told
 * where they have arrived.
 *
 * <p>{@code [Lift Up]} and {@code [Lift Down]} go one way; {@code [Lift]} is a floor that can be
 * arrived at but not left from; {@code [Lift UpDown]} goes either way, decided by which half of
 * the sign was touched, and is also the sign a rider works by jumping and crouching where the
 * settings allow it.
 */
@NullMarked
public final class Elevator implements SignMechanic {

    /** The sign that goes up. */
    public static final String UP = "[Lift Up]";

    /** The sign that goes down. */
    public static final String DOWN = "[Lift Down]";

    /** The sign that is only a floor to arrive at. */
    public static final String STOP = "[Lift]";

    /** The sign that goes both ways. */
    public static final String BOTH = "[Lift UpDown]";

    private static final List<String> NAMES = List.of(UP, DOWN, STOP, BOTH);

    /** The line a builder names the floor on. */
    private static final int FLOOR_LINE = 0;

    /** Where on a two-way sign the halves meet. */
    private static final double MIDDLE = 0.5;

    /** How much room somebody needs to stand in: their feet and their head. */
    private static final int ROOM_NEEDED = 2;

    @Override
    public String name() {
        return "Elevator";
    }

    @Override
    public List<String> signNames() {
        return NAMES;
    }

    @Override
    public boolean act(MechanicVisit visit) {
        Optional<BlockFace> way = wayFrom(visit);
        return way.isPresent() && ride(visit, way.get());
    }

    /**
     * Carries whoever set the lift off to the next floor.
     *
     * <p>Separate from {@link #act} because a jump lift says which way to go by how the rider
     * moved rather than by what is written on the sign.
     *
     * @param way up or down
     * @return true if they were carried
     */
    public boolean ride(MechanicVisit visit, BlockFace way) {
        Optional<Actor> rider = visit.actor();
        if (rider.isEmpty()) {
            return false;
        }
        if (!rider.get().mayUse(usePermission())) {
            rider.get().complain("You may not use lifts.");
            return false;
        }

        MechanicWorld world = visit.world();
        Optional<PostedSign> floor = nextFloor(world, visit.sign().position(), way);
        if (floor.isEmpty()) {
            rider.get().complain("This lift has nowhere to go.");
            return false;
        }

        Vec3i standing = rider.get().position().orElse(visit.sign().position());
        Vec3i arrival = new Vec3i(standing.x(), floor.get().position().y(), standing.z());

        Optional<Vec3i> feet = footingAt(visit, arrival);
        if (feet.isEmpty()) {
            return false;
        }
        if (!rider.get().moveTo(new Landing(world.id(), feet.get(), BlockFace.SELF))) {
            return false;
        }

        announce(rider.get(), floor.get(), way);
        return true;
    }

    /** Which way the sign says to go, or nothing when it says neither. */
    private Optional<BlockFace> wayFrom(MechanicVisit visit) {
        PostedSign sign = visit.sign();
        if (sign.isNamed(UP)) {
            return Optional.of(BlockFace.UP);
        }
        if (sign.isNamed(DOWN)) {
            return Optional.of(BlockFace.DOWN);
        }
        if (!sign.isNamed(BOTH) || visit.touchHeight().isEmpty()) {
            return Optional.empty();
        }
        // Exactly in the middle goes down. Nobody can click there, but the rule has to say.
        return Optional.of(
                visit.touchHeight().getAsDouble() <= MIDDLE ? BlockFace.DOWN : BlockFace.UP);
    }

    /** The next lift sign in the shaft, in the direction of travel. */
    private Optional<PostedSign> nextFloor(MechanicWorld world, Vec3i from, BlockFace way) {
        int step = way == BlockFace.UP ? 1 : -1;
        for (int y = from.y() + step; y >= world.minHeight() && y < world.maxHeight(); y += step) {
            Vec3i at = new Vec3i(from.x(), y, from.z());
            if (!world.isLoaded(at)) {
                continue;
            }
            Optional<PostedSign> sign = world.signAt(at).filter(found -> claims(found.lines()));
            if (sign.isPresent()) {
                return sign;
            }
        }
        return Optional.empty();
    }

    /**
     * Where a rider's feet go at the far end.
     *
     * <p>A floor is looked for below the arrival point rather than exactly at it, because a sign
     * is usually hung a little above the floor it serves. There has to be a floor within the
     * distance the settings allow, and room above it for a person to stand.
     *
     * @return where to stand, or nothing when the rider has been told why not
     */
    private static Optional<Vec3i> footingAt(MechanicVisit visit, Vec3i arrival) {
        MechanicWorld world = visit.world();
        int tolerance = visit.settings().mechanics().liftTolerance();

        int room = 0;
        Vec3i at = arrival;
        boolean floorFound = false;
        for (int dropped = 0; dropped < tolerance; dropped++) {
            if (at.y() <= world.minHeight()) {
                break;
            }
            if (!world.isPassable(at)) {
                floorFound = true;
                break;
            }
            room++;
            at = at.offset(BlockFace.DOWN);
        }

        if (!floorFound) {
            visit.complain("There is no floor there.");
            return Optional.empty();
        }

        // The sign's own height is solid but the block above it is clear, which is the usual
        // arrangement when a sign is hung at head height beside the floor it serves.
        if (room == 1 && world.isPassable(arrival.offset(BlockFace.UP))) {
            room++;
        }
        if (room < ROOM_NEEDED) {
            visit.complain("The way out of the lift is blocked.");
            return Optional.empty();
        }
        return Optional.of(at.offset(BlockFace.UP));
    }

    /** Tells the rider where they have arrived. */
    private static void announce(Actor rider, PostedSign floor, BlockFace way) {
        String named = floor.line(FLOOR_LINE);
        rider.inform(named.isEmpty()
                ? "You have gone " + way.name().toLowerCase(Locale.ROOT) + " a floor."
                : "Floor: " + named);
    }
}
