// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.Cart;
import com.xeonproductions.craftbookultimate.core.cart.CartFilter;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The mechanics that change how fast a cart is going.
 *
 * <p>One multiplies whatever speed the cart already had, one stops it and lets it go again after a
 * while, and one holds it until somebody climbs in.
 */
@NullMarked
public final class CartSpeed {

    /** The line a delay reads its wait from, and a launcher reads its departure rule from. */
    private static final int THIRD_LINE = 2;

    /** The line a launcher reads its arrival rule from. */
    private static final int FOURTH_LINE = 3;

    /** How many ticks a second lasts, which is what a delay is written in. */
    private static final int TICKS_PER_SECOND = 20;

    /** The longest a delay may hold a cart, so a mistyped sign cannot strand one for a day. */
    private static final int MAX_DELAY_SECONDS = 3600;

    private CartSpeed() {}

    /**
     * Multiplies a cart's speed by whatever its block is worth.
     *
     * <p>Needs no sign: the block is the whole of it, so a gold block under the rail sends a cart
     * away at full speed and gravel slows it to a crawl. Which block is worth what is a setting.
     */
    public static CartMechanic booster() {
        return new Booster();
    }

    /**
     * Stops a cart, then sends it on its way again after a wait.
     *
     * <p>Line 3 is the wait in seconds. The cart leaves behind the sign, so a delay on a platform
     * faces the passengers and sends the cart off down the track.
     *
     * <p>A cart that has been moved or pushed in the meantime is left alone, so a delay cannot
     * reach out and grab a cart somebody has since taken away.
     */
    public static CartMechanic delay() {
        return new Delay();
    }

    /**
     * Holds a cart until somebody gets in, then sends it off.
     *
     * <p>Line 4 says which carts to hold and line 3 which riders to launch, both written as
     * ordinary cart filters. A blank line means every cart and every rider, which is what an
     * unadorned launcher does.
     */
    public static CartMechanic launcher() {
        return new Launcher();
    }

    /** Multiplies a cart's speed by what its block is worth. */
    private record Booster() implements CartMechanic {

        @Override
        public String name() {
            return "Booster";
        }

        @Override
        public boolean requiresSign() {
            return false;
        }

        @Override
        public boolean appliesTo(CartMechanism mechanism, Settings settings) {
            // Not one block but a set of them, each worth a different multiplier, so this asks
            // the boosters rather than the single block every other mechanic is built from.
            return settings.carts().allows(name()) && settings.carts().isBooster(mechanism.baseBlock());
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived()) {
                return false;
            }
            visit.settings()
                    .carts()
                    .boostOf(visit.mechanism().baseBlock())
                    .ifPresent(multiplier -> visit.cart().setVelocity(visit.cart().velocity().multiply(multiplier)));
            return false;
        }
    }

    /** Stops a cart and lets it go again after a wait. */
    private record Delay() implements CartMechanic {

        @Override
        public String name() {
            return "Delay";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || !visit.mechanism().isNamed(name())) {
                return false;
            }

            Optional<Integer> wait = secondsOn(visit.mechanism());
            if (wait.isEmpty()) {
                return false;
            }

            Cart cart = visit.cart();
            cart.stop();

            Vec3d restingAt = cart.position();
            Vec3d departure = departureOf(visit);
            visit.scheduler().runLater(() -> release(cart, restingAt, departure),
                    (long) wait.get() * TICKS_PER_SECOND);
            return false;
        }

        /**
         * Sends the cart off, unless it is no longer the cart that was left here.
         *
         * <p>A cart that has been pushed or carried away in the meantime is not the one this delay
         * stopped, and pushing it from here would be reaching across the railway to move somebody
         * else's cart.
         */
        private static void release(Cart cart, Vec3d restingAt, Vec3d departure) {
            if (!cart.isPresent() || cart.speed() > 0 || !cart.position().equals(restingAt)) {
                return;
            }
            cart.setVelocity(departure);
        }

        /** How long to wait, or empty when the sign does not say a sensible number. */
        private static Optional<Integer> secondsOn(CartMechanism mechanism) {
            try {
                int seconds = Integer.parseInt(mechanism.line(THIRD_LINE).trim());
                return seconds > 0 && seconds <= MAX_DELAY_SECONDS ? Optional.of(seconds) : Optional.empty();
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }

    /** Holds a cart until somebody gets in. */
    private record Launcher() implements CartMechanic {

        @Override
        public String name() {
            return "Launch";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || !visit.mechanism().isNamed(name())) {
                return false;
            }
            if (visit.cart().isOccupied()) {
                return false;
            }
            if (holds(visit, visit.mechanism().line(FOURTH_LINE))) {
                visit.cart().stop();
            }
            return false;
        }

        /**
         * Sends a cart off because somebody has just got in.
         *
         * <p>Called from the moment of mounting rather than from a cart arriving, which is the one
         * thing this mechanic does that no rolling cart triggers.
         *
         * @return whether the cart was launched
         */
        static boolean onMount(CartVisit visit) {
            if (!visit.isAllowed() || !visit.mechanism().isNamed("Launch")) {
                return false;
            }
            if (!holds(visit, visit.mechanism().line(THIRD_LINE))) {
                return false;
            }
            return visit.cart().setVelocity(departureOf(visit));
        }

        /**
         * Whether a line applies to this cart.
         *
         * <p>A blank line means every cart, which is not what a blank filter means anywhere else:
         * a launcher with nothing written on it holds everything and launches everybody, and that
         * is what an unadorned one has always done.
         */
        private static boolean holds(CartVisit visit, String written) {
            if (written.isBlank()) {
                return true;
            }
            return CartFilter.parse(written, visit.world()::resolveItem)
                    .filter(filter -> filter.matches(visit.cart(), visit.stations()))
                    .isPresent();
        }
    }

    /** Which way and how fast a cart leaves a mechanism, which is away behind its sign. */
    private static Vec3d departureOf(CartVisit visit) {
        return visit.mechanism()
                .sign()
                .map(sign -> Vec3d.of(sign.outward()).multiply(visit.settings().carts().launchSpeed()))
                .orElse(Vec3d.ZERO);
    }

    /**
     * Sends a cart off because somebody climbed into it on a launcher.
     *
     * <p>The one thing a cart mechanic does that a rolling cart does not set off, so it is offered
     * here for whoever is watching people get into carts.
     *
     * @return whether the cart was launched
     */
    public static boolean launchOnMount(CartVisit visit) {
        return Launcher.onMount(visit);
    }
}
