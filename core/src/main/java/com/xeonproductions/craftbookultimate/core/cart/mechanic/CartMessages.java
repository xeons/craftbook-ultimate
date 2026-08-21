// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * The mechanic that says something to whoever is riding past.
 *
 * <p>Used for the announcements a railway makes: which station is next, where to change, what is
 * up the stairs at the far end.
 */
@NullMarked
public final class CartMessages {

    /** What a message sign may say to mean a new line. */
    private static final String LINE_BREAK = "\\n";

    /** How many signs deep a message may run before it is cut off. */
    private static final int MAX_SIGNS = 32;

    private CartMessages() {}

    /**
     * Reads a sign aloud to whoever is riding past.
     *
     * <p>Recognised by its sign alone rather than by the block under the rail, so a message can be
     * hung under any part of a railway without breaking whatever else that stretch is made of.
     *
     * <p>The message is the rest of the sign after its name, and carries on down any signs stacked
     * directly below it, so a long announcement is written by putting up several signs in a
     * column. {@code \n} starts a new line.
     */
    public static CartMechanic printer() {
        return new Printer();
    }

    /** Reads a sign aloud to whoever is riding past. */
    private record Printer() implements CartMechanic {

        @Override
        public String name() {
            return "Print";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean appliesTo(CartMechanism mechanism, Settings settings) {
            // The only cart mechanic with no block of its own: it is whatever the sign says it is,
            // so it never asks the settings which block builds it.
            return settings.carts().allows(name()) && mechanism.hasSign();
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || !visit.mechanism().isNamed(name())) {
                return false;
            }

            List<Bystander> listeners = visit.cart().riders();
            if (listeners.isEmpty()) {
                return false;
            }

            for (String line : read(visit)) {
                Component spoken = Component.text(line);
                for (Bystander listener : listeners) {
                    listener.tell(spoken);
                }
            }
            return false;
        }

        /**
         * The whole message, as the lines to say.
         *
         * <p>The first sign contributes everything below its name; each sign under it contributes
         * all four of its lines, so a continuation does not have to leave a line blank.
         *
         * <p>Lines are joined exactly as written, spaces and all, so a message broken across two
         * lines reads as one sentence rather than running its words together.
         */
        private static List<String> read(CartVisit visit) {
            Optional<CartMechanism.MechanismSign> sign = visit.mechanism().sign();
            if (sign.isEmpty()) {
                return List.of();
            }

            StringBuilder message = new StringBuilder();
            Vec3i at = sign.get().position();
            Optional<CartMechanism.MechanismSign> current = sign;
            for (int read = 0; current.isPresent() && read < MAX_SIGNS; read++) {
                int firstLine = read == 0 ? CartMechanism.MechanismSign.NAME_LINE + 1 : 0;
                for (int line = firstLine; line < 4; line++) {
                    message.append(current.get().lines().text(line));
                }
                at = at.offset(BlockFace.DOWN);
                current = visit.world().signAt(at);
            }

            List<String> spoken = new ArrayList<>();
            for (String line : message.toString().split(Pattern.quote(LINE_BREAK))) {
                spoken.add(line);
            }
            return spoken;
        }
    }
}
