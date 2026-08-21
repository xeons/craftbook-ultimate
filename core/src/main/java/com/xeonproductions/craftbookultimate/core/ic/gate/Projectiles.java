// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that throw things.
 *
 * <p>All of them fire out of the back of the sign, away from the block it hangs on, from just
 * outside that block's face so nothing hits the thing it was fired from.
 *
 * <p>Line 3 reads {@code speed[:spread]} and line 4 is a vertical velocity, which is how a shooter
 * is aimed up or down. The spread is the game's own notion of inaccuracy, the same one a dispenser
 * uses, so a shooter left on its defaults scatters the way a dispenser does.
 *
 * <pre>
 *   1.5:0     fast and dead straight
 *   0.5:40    slow and wild
 * </pre>
 *
 * <p>A barrage is a shooter that fires five at once. Every one of these is restricted.
 */
@NullMarked
public final class Projectiles {

    /** The line carrying the speed and the spread. */
    private static final int SPEED_LINE = 2;

    /** The line carrying the vertical velocity. */
    private static final int VERTICAL_LINE = 3;

    /** How fast a shooter throws when its sign does not say. */
    private static final double DEFAULT_SPEED = 0.5;

    /** The slowest and fastest a sign may ask for. */
    private static final double MIN_SPEED = 0.3;

    private static final double MAX_SPEED = 2.0;

    /** How wide a shooter scatters when its sign does not say. */
    private static final double DEFAULT_SPREAD = 12.0;

    /** The widest scatter a sign may ask for. */
    private static final double MAX_SPREAD = 50.0;

    /** The steepest a sign may aim, up or down. */
    private static final double MAX_VERTICAL = 1.0;

    /**
     * How far outside the face of its supporting block a shot starts.
     *
     * <p>Half a block puts it exactly on the face, which the block then blocks, so it starts a
     * little further out than that.
     */
    private static final double MUZZLE_CLEARANCE = 0.55;

    /** How many a barrage fires at once. */
    private static final int BARRAGE_SIZE = 5;

    /** The steepest a fireball may be aimed, as a fraction of straight up. */
    private static final double MAX_FIREBALL_PITCH = 1.0;

    /** The furthest a fireball may be turned from straight out the back, in degrees. */
    private static final double MAX_FIREBALL_ROTATION = 90.0;

    private Projectiles() {}

    /** Fires one arrow. */
    public static ICLogic arrowShooter() {
        return shooter(Blocks.key("arrow"), 1);
    }

    /** Fires five arrows at once. */
    public static ICLogic arrowBarrage() {
        return shooter(Blocks.key("arrow"), BARRAGE_SIZE);
    }

    /** Throws one snowball. */
    public static ICLogic snowShooter() {
        return shooter(Blocks.key("snowball"), 1);
    }

    /** Throws five snowballs at once. */
    public static ICLogic snowBarrage() {
        return shooter(Blocks.key("snowball"), BARRAGE_SIZE);
    }

    /** Throws one egg. */
    public static ICLogic eggShooter() {
        return shooter(Blocks.key("egg"), 1);
    }

    /** Throws five eggs at once. */
    public static ICLogic eggBarrage() {
        return shooter(Blocks.key("egg"), BARRAGE_SIZE);
    }

    /**
     * Launches a ghast fireball.
     *
     * <p>Aimed differently from the rest: line 4 reads {@code rotation[:pitch]}, where the rotation
     * turns it up to ninety degrees either way from straight out the back and the pitch tilts it,
     * from straight down at {@code -1} to straight up at {@code 1}.
     *
     * <p>A fireball steers itself once it is away, so the speed on line 3 makes no difference to
     * where it ends up and the explosion is whatever the game gives a ghast's.
     */
    public static ICLogic fireballShooter() {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            double rotation =
                    boundedNumber(
                            firstField(state.sign().trimmedText(VERTICAL_LINE)),
                            -MAX_FIREBALL_ROTATION,
                            MAX_FIREBALL_ROTATION,
                            0);
            double pitch =
                    boundedNumber(
                            secondField(state.sign().trimmedText(VERTICAL_LINE)),
                            -MAX_FIREBALL_PITCH,
                            MAX_FIREBALL_PITCH,
                            0);

            BlockFace away = state.facing().opposite();
            Vec3d aim = new Vec3d(away.deltaX(), 0, away.deltaZ())
                    .rotateAroundY(rotation)
                    .add(0, pitch, 0);

            state.world().launchProjectile(muzzle(state), Blocks.key("fireball"), aim, 1, 0);
        };
    }

    /**
     * A chip that throws a number of one thing whenever it is driven.
     *
     * @param projectile what to throw
     * @param count how many at once
     */
    private static ICLogic shooter(Key projectile, int count) {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            String settings = state.sign().trimmedText(SPEED_LINE);
            double speed = boundedNumber(firstField(settings), MIN_SPEED, MAX_SPEED, DEFAULT_SPEED);
            double spread = boundedNumber(secondField(settings), 0, MAX_SPREAD, DEFAULT_SPREAD);
            double vertical =
                    boundedNumber(
                            state.sign().trimmedText(VERTICAL_LINE), -MAX_VERTICAL, MAX_VERTICAL, 0);

            BlockFace away = state.facing().opposite();
            Vec3d aim = new Vec3d(away.deltaX(), vertical, away.deltaZ());
            Vec3d from = muzzle(state);

            for (int shot = 0; shot < count; shot++) {
                state.world().launchProjectile(from, projectile, aim, speed, spread);
            }
        };
    }

    /** Where a shot starts: just outside the far face of the block the sign hangs on. */
    private static Vec3d muzzle(ChipState state) {
        BlockFace away = state.facing().opposite();
        return Vec3d.middleOf(state.backPosition()).add(Vec3d.of(away).multiply(MUZZLE_CLEARANCE));
    }

    /** The part of a setting before the colon. */
    private static String firstField(String written) {
        int colon = written.indexOf(':');
        return colon < 0 ? written : written.substring(0, colon);
    }

    /** The part of a setting after the colon, or nothing when there is none. */
    private static String secondField(String written) {
        int colon = written.indexOf(':');
        return colon < 0 ? "" : written.substring(colon + 1);
    }

    /**
     * A number from a sign, held within bounds.
     *
     * <p>The bounds are applied every time the sign is read rather than only when it is created,
     * so a sign edited afterwards cannot ask for a shot outside what the chip is meant to do.
     */
    private static double boundedNumber(String written, double lowest, double highest, double fallback) {
        String trimmed = written.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        try {
            return Math.clamp(Double.parseDouble(trimmed), lowest, highest);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
