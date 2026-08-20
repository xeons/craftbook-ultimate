package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that hurt or remove what is standing near them.
 *
 * <p>Two of them work on the first place above their own support that something could stand, which
 * is what makes a trap: put the chip under the floor and whoever walks over it is in range. The
 * third works on a radius and is used to keep an area clear.
 *
 * <p>Line 3 says what to act on, in the same spelling the spawner uses, and line 4 says how hard or
 * how far. All three are restricted.
 */
@NullMarked
public final class Combat {

    /** The line naming what to act on. */
    private static final int SUBJECT_LINE = 2;

    /** The line carrying the damage or the range. */
    private static final int AMOUNT_LINE = 3;

    /** How far a zapper reaches when its sign does not say. */
    private static final int DEFAULT_ZAP_RANGE = 5;

    /** The furthest a zapper may reach. */
    private static final int MAX_ZAP_RANGE = 64;

    /** How hard the two trap chips hit when their sign does not say. */
    private static final int DEFAULT_DAMAGE = 1;

    /** The hardest a trap chip may hit, which is more than enough to kill anything. */
    private static final int MAX_DAMAGE = 1000;

    /**
     * How far from the middle of a block a trap chip looks.
     *
     * <p>Half a block, so it catches whoever is standing in that block and nobody in the next one.
     */
    private static final double TRAP_REACH = 0.5;

    private Combat() {}

    /**
     * Removes creatures near it, without dropping anything.
     *
     * <p>Line 3 says what to remove and defaults to hostile mobs. Line 4 is how far to reach,
     * which defaults to five blocks and cannot exceed sixty-four.
     *
     * <p>The output reports whether anything was removed.
     */
    public static SelfTriggeringICLogic mobZapper() {
        return new Zapper();
    }

    /**
     * Hurts players standing above it.
     *
     * <p>Line 3 picks which players, as {@code p:Notch}, {@code g:admin} or {@code m:ott}, each of
     * which may be turned around with a {@code !}. Blank means anyone. Line 4 is how many
     * half-hearts to take, which defaults to one and ignores armour.
     */
    public static SelfTriggeringICLogic hitPlayerAbove() {
        return new Trap(EntitySpec.Person.ANY, true);
    }

    /**
     * Hurts creatures standing above it.
     *
     * <p>The same as {@link #hitPlayerAbove()} but for everything that is not a player. Line 3
     * defaults to hostile mobs.
     */
    public static SelfTriggeringICLogic hitMobAbove() {
        return new Trap(new EntitySpec.Category(EntitySpec.Group.MONSTERS), false);
    }

    /** Clears creatures out of a radius, on a pulse or on every tick. */
    private static final class Zapper implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                zap(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            zap(state);
        }

        private static void zap(ChipState state) {
            EntitySpec wanted =
                    subjectOn(state, new EntitySpec.Category(EntitySpec.Group.MONSTERS));
            int range =
                    boundedNumber(
                            state.sign().trimmedText(AMOUNT_LINE), 1, MAX_ZAP_RANGE, DEFAULT_ZAP_RANGE);

            boolean removed = false;
            Vec3d centre = Vec3d.centreOf(state.signPosition());
            for (Bystander bystander : state.world().bystandersNear(centre, range)) {
                if (wanted.matches(bystander) && bystander.remove()) {
                    removed = true;
                }
            }
            state.setMainOutput(removed);
        }
    }

    /**
     * Hurts whatever is standing in the first free block above the chip's support.
     *
     * @param fallback what to act on when the sign does not say
     * @param playersOnly whether the sign's own description is read as a player description
     */
    private record Trap(EntitySpec fallback, boolean playersOnly) implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                strike(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            strike(state);
        }

        private void strike(ChipState state) {
            Optional<Vec3i> spot = state.world().firstPassableAtOrAbove(state.backPosition().add(0, 1, 0));
            if (spot.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            EntitySpec wanted = subjectOn(state, fallback);
            int damage =
                    boundedNumber(
                            state.sign().trimmedText(AMOUNT_LINE), 1, MAX_DAMAGE, DEFAULT_DAMAGE);

            boolean hit = false;
            List<Bystander> standing =
                    state.world().bystandersNear(Vec3d.centreOf(spot.get()), TRAP_REACH);
            for (Bystander bystander : standing) {
                if (bystander.isPlayer() != playersOnly || !wanted.matches(bystander)) {
                    continue;
                }
                if (bystander.damage(damage)) {
                    hit = true;
                }
            }
            state.setMainOutput(hit);
        }
    }

    /** What a chip's sign says to act on, or the chip's own default when the line is blank. */
    private static EntitySpec subjectOn(ChipState state, EntitySpec fallback) {
        String written = state.sign().trimmedText(SUBJECT_LINE);
        if (written.isEmpty()) {
            return fallback;
        }
        return EntitySpec.parse(written, state.world()::resolveItem).orElse(fallback);
    }

    /** A number from a sign, held within bounds, falling back when the text is not one. */
    private static int boundedNumber(String written, int lowest, int highest, int fallback) {
        String trimmed = written.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        try {
            return Math.clamp(Integer.parseInt(trimmed), lowest, highest);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
