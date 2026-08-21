// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Destinations;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The pair of chips that move people from one place to another.
 *
 * <p>A destination takes a name and says where people sent to that name should arrive. A
 * transporter names a destination and sends whoever is standing on it there. The two are matched
 * by the name on line 3 of each sign and nothing else, so they may be any distance apart and in
 * different worlds.
 *
 * <p>Neither end ever reads the other's blocks. A destination works out its own arrival point from
 * its own surroundings and publishes it; a transporter reads that and hands the traveller to the
 * server to move. That is what makes the pair safe when the two ends are being ticked by different
 * threads.
 */
@NullMarked
public final class Transport {

    /** The sign line carrying the name the two ends are matched on. */
    private static final int NAME_LINE = 2;

    /** How far above the block in front of the sign a teleport pad stands. */
    private static final int PAD_HEIGHT = 2;

    private Transport() {}

    /**
     * Sends whoever is standing on it to a named destination.
     *
     * <p>Where people are picked up from depends on the mode. By default they are picked up from
     * the first clear block above the one the sign hangs on, which is a doorway you walk into. The
     * {@code p} mode instead picks them up two blocks above the block in front of the sign, which
     * is a pad you stand on, with the sign underneath out of the way. {@code P} does the same and
     * additionally releases the pressure plate they were standing on, so a pad built from a plate
     * is ready to fire again rather than staying pressed by someone who is no longer there.
     *
     * <p>The output goes high when somebody was sent.
     */
    public static ICLogic transporter() {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            String name = nameOn(state);
            Optional<Landing> landing = name.isEmpty()
                    ? Optional.<Landing>empty()
                    : state.destinations().find(name);
            if (landing.isEmpty()) {
                return;
            }

            Optional<Vec3i> origin = pickupPoint(state);
            if (origin.isEmpty()) {
                return;
            }

            boolean releasePlates = state.mode().behaviour() == ICMode.Behaviour.TELEPORT_PAD_FORCED_PRESSURE_PLATE;
            boolean sentAnyone = false;

            for (Traveller traveller : state.world().travellersIn(origin.get())) {
                // Read where they were standing before moving them, since afterwards they are
                // somewhere else entirely and the plate to release is not under them any more.
                Vec3i feet = traveller.position();
                if (!traveller.moveTo(landing.get())) {
                    continue;
                }
                sentAnyone = true;
                if (releasePlates) {
                    state.world().releasePressurePlate(feet);
                }
            }

            state.setMainOutput(sentAnyone);
        };
    }

    /**
     * Answers to a name, and receives whoever a transporter sends to it.
     *
     * <p>People arrive in the first clear block above the one the sign hangs on, looking the way
     * the sign's back points, which is out into the room the pad is built into.
     *
     * <p>A destination is on whenever it is being driven, and a destination with nothing wired to
     * it at all is on permanently, so that the common case of a plain arrival pad needs no
     * redstone. Switching it off gives up the name, which is how a build routes one transporter to
     * several places by turning destinations on and off.
     */
    public static SelfTriggeringICLogic destination() {
        return new Destination();
    }

    /** The name a sign uses to pair with its other end. */
    private static String nameOn(ChipState state) {
        return state.sign().trimmedText(NAME_LINE);
    }

    /** Where a transporter picks people up from. */
    private static Optional<Vec3i> pickupPoint(ChipState state) {
        return switch (state.mode().behaviour()) {
            case TELEPORT_PAD, TELEPORT_PAD_FORCED_PRESSURE_PLATE ->
                    Optional.of(state.signPosition().offset(state.facing()).add(0, PAD_HEIGHT, 0));
            default -> state.world().firstPassableAtOrAbove(state.backPosition());
        };
    }

    /** Holds a name for as long as it is switched on, and says where its arrivals land. */
    private static final class Destination implements SelfTriggeringICLogic {

        private @Nullable Destinations registry;
        private @Nullable String heldName;
        private @Nullable Landing landing;

        @Override
        public boolean alwaysSelfTriggering() {
            return true;
        }

        @Override
        public void load(ChipState state) {
            apply(state, shouldBeActiveUndriven(state));
        }

        @Override
        public void tick(ChipState state) {
            apply(state, shouldBeActiveUndriven(state));
        }

        @Override
        public void trigger(ChipState state) {
            apply(state, state.isAnyInputActive());
        }

        @Override
        public void unload(ChipState state) {
            release();
        }

        /**
         * Whether the destination should be on, allowing for having nothing wired to it.
         *
         * <p>A destination nobody has wired anything to is on. Once a builder wires something,
         * that wiring decides.
         */
        private static boolean shouldBeActiveUndriven(ChipState state) {
            for (int input = 0; input < state.inputCount(); input++) {
                if (state.isConnected(input)) {
                    return state.isAnyInputActive();
                }
            }
            return true;
        }

        private void apply(ChipState state, boolean shouldBeActive) {
            String name = nameOn(state);
            if (!shouldBeActive || name.isEmpty()) {
                release();
                return;
            }

            Landing arrival = currentLanding(state);
            if (arrival == null) {
                // Solid all the way up, so there is nowhere to put anybody. The name is given up
                // rather than left pointing at a place people cannot arrive in.
                release();
                return;
            }

            if (!name.equals(heldName)) {
                release();
                if (state.destinations().claim(name, this, arrival)) {
                    registry = state.destinations();
                    heldName = name;
                }
                return;
            }

            state.destinations().update(name, this, arrival);
        }

        /**
         * Where arrivals land, worked out again only when the place it was using has been built
         * over.
         */
        private @Nullable Landing currentLanding(ChipState state) {
            if (landing != null && state.world().isPassable(landing.block())) {
                return landing;
            }
            landing = state.world()
                    .firstPassableAtOrAbove(state.backPosition())
                    .map(spot -> new Landing(state.world().id(), spot, state.facing().opposite()))
                    .orElse(null);
            return landing;
        }

        private void release() {
            if (registry != null && heldName != null) {
                registry.release(heldName, this);
            }
            registry = null;
            heldName = null;
        }
    }
}
