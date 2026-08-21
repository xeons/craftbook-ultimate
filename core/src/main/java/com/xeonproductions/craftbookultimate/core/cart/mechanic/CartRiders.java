// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.CartFilter;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The mechanics that deal with who is riding rather than where the cart goes.
 *
 * <p>One so far: the block that turns people out of their cart.
 */
@NullMarked
public final class CartRiders {

    /** The line an ejector reads its filter from. */
    private static final int THIRD_LINE = 2;

    private CartRiders() {}

    /**
     * Turns everybody out of a cart.
     *
     * <p>Without a sign they are simply set down where the cart is, which is what a builder puts
     * at a dead end. With one they land on the block behind the sign, so a platform can take
     * people off a cart and leave them facing the way they were going, and line 3 may carry a
     * filter naming which carts to empty.
     */
    public static CartMechanic ejector() {
        return new Ejector();
    }

    /** Turns everybody out of a cart. */
    private record Ejector() implements CartMechanic {

        @Override
        public String name() {
            return "Eject";
        }

        @Override
        public boolean requiresSign() {
            return false;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || !visit.cart().isOccupied()) {
                return false;
            }

            Optional<CartMechanism.MechanismSign> sign = visit.mechanism().sign()
                    .filter(written -> visit.mechanism().isNamed(name()));
            if (sign.isPresent() && !claims(visit, visit.mechanism().line(THIRD_LINE))) {
                return false;
            }

            Optional<Vec3d> platform = sign.map(written -> Vec3d.centreOf(
                    visit.mechanism().rail().offset(written.outward()).offset(BlockFace.UP)));
            for (Bystander rider : visit.cart().ejectRiders()) {
                platform.ifPresent(rider::moveTo);
            }
            return false;
        }

        /**
         * Whether this cart is one the sign asked for.
         *
         * <p>A blank line claims every cart, so an ejector says which carts to empty only when its
         * builder wanted it to be choosy.
         */
        private static boolean claims(CartVisit visit, String written) {
            if (written.isEmpty()) {
                return true;
            }
            return CartFilter.parse(written, visit.world()::resolveItem)
                    .filter(filter -> filter.matches(visit.cart(), visit.stations()))
                    .isPresent();
        }
    }
}
