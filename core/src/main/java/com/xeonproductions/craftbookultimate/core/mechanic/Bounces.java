// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The block that throws whoever jumps on it.
 *
 * <p>A block with a {@code [Jump]} sign underneath it, or one an operator has named in the settings
 * so it needs no sign at all. Jumping on it sets the jumper's speed to whatever the sign asks for
 * and cancels their fall, so they land wherever it sent them without hurting themselves.
 *
 * <p>The grammar on the sign's third line is frozen: {@code 5} throws you five blocks straight up,
 * {@code 2,1,2} adds a push along the ground, and a leading {@code !} means the push is due
 * north-and-east rather than wherever you happen to be facing.
 */
@NullMarked
public final class Bounces {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = "BounceBlocks";

    /** The sign that makes one. */
    public static final String SIGN_NAME = "[Jump]";

    /** The permission to build one. */
    public static final String BUILD = "craftbook.bounceblocks";

    /** The permission to be thrown by one. */
    public static final String USE = "craftbook.bounceblocks.use";

    /** The line the throw is written on. */
    public static final int VELOCITY_LINE = 2;

    /** What a sign that says nothing throws you, which is a hop rather than a launch. */
    public static final double DEFAULT_UP = 0.5;

    /** Marks a throw that ignores which way the jumper is facing. */
    private static final char STRAIGHT = '!';

    private Bounces() {
    }

    /**
     * The throw a sign asks for, or nothing where the line says something unusable.
     *
     * <p>Nothing rather than a default, because a bounce block is built to throw somebody
     * somewhere: a sign whose numbers are wrong should do nothing and be noticed, not quietly hop.
     * A sign left blank is a different thing and does get the default.
     */
    public static Optional<Bounce> parse(String written) {
        String text = written.trim();
        boolean facing = true;
        if (!text.isEmpty() && text.charAt(0) == STRAIGHT) {
            facing = false;
            text = text.substring(1).trim();
        }

        if (text.isEmpty()) {
            return Optional.of(new Bounce(new Vec3d(0, DEFAULT_UP, 0), facing));
        }

        String[] parts = text.split(",");
        try {
            if (parts.length == 1) {
                return Optional.of(new Bounce(
                        new Vec3d(0, Double.parseDouble(parts[0].trim()), 0), facing));
            }
            if (parts.length == 3) {
                return Optional.of(new Bounce(new Vec3d(
                        Double.parseDouble(parts[0].trim()),
                        Double.parseDouble(parts[1].trim()),
                        Double.parseDouble(parts[2].trim())), facing));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * How hard and which way a bounce block throws somebody.
     *
     * @param speed how fast, before any turning
     * @param alongFacing whether the sideways push follows where the jumper is looking
     */
    public record Bounce(Vec3d speed, boolean alongFacing) {

        /**
         * The throw as it applies to somebody looking a particular way.
         *
         * <p>Up is never turned — a sign asking to be thrown five blocks up means up wherever you
         * are standing. Only the sideways push follows the jumper, so one sign at the foot of a
         * tower can throw people out in whichever direction they were heading.
         *
         * @param yaw which way they are looking, in degrees
         */
        public Vec3d forFacing(double yaw) {
            if (!alongFacing) {
                return speed;
            }
            double radians = Math.toRadians(yaw + 90);
            return new Vec3d(
                    speed.x() * Math.cos(radians),
                    speed.y(),
                    speed.z() * Math.sin(radians));
        }
    }
}
