// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that call lightning down.
 *
 * <p>One strikes a single place, one strikes across an area, and one strikes whatever is standing
 * near it. All three are restricted: lightning sets fires, kills, and turns pigs into piglins.
 */
@NullMarked
public final class LightningChips {

    /** The line carrying where or how far to strike. */
    private static final int PLACE_LINE = 2;

    /** The line carrying the chance or the range. */
    private static final int EXTRA_LINE = 3;

    /** Separates the area from the offset to its middle. */
    private static final char CENTRE_SEPARATOR = '=';

    /** The furthest above or below itself a single bolt may be aimed. */
    private static final int MAX_HEIGHT_OFFSET = 255;

    /** How far a bolt strike reaches across when its sign does not say. */
    private static final int DEFAULT_AREA_RADIUS = 1;

    /** How far a smite reaches for things to strike when its sign does not say. */
    private static final int DEFAULT_SMITE_RANGE = 5;

    /** The furthest a smite may reach. */
    private static final int MAX_SMITE_RANGE = 64;

    /** A chance out of this many. */
    private static final int CERTAIN = 100;

    private LightningChips() {}

    /**
     * Strikes one place with lightning.
     *
     * <p>Line 3 is how far above or below the block the sign hangs on to strike, which lets a bolt
     * be aimed at a rooftop from a control room below. Leaving it blank strikes the block itself.
     *
     * <p>The output reports whether the bolt landed, and goes low again when the input does.
     */
    public static ICLogic lightning() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            int height =
                    boundedNumber(
                            state.sign().trimmedText(PLACE_LINE),
                            -MAX_HEIGHT_OFFSET,
                            MAX_HEIGHT_OFFSET,
                            0);
            state.setMainOutput(state.world().strikeLightning(state.backPosition().add(0, height, 0)));
        };
    }

    /**
     * Strikes everything solid across an area.
     *
     * <p>Line 3 is the reach, either one number for a cube or {@code x,y,z} for a box, and may be
     * followed by {@code =x:y:z} moving the middle of that area away from the block the sign hangs
     * on. Line 4 is the chance out of a hundred that any one block is struck, which is how a storm
     * is made to look scattered rather than total.
     *
     * <p>Only blocks that are not air are struck, so a bolt lands on the ground rather than in the
     * sky above it.
     *
     * @param random where the scattering comes from
     */
    public static ICLogic zeusBolt(RandomGenerator random) {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            Area area = Area.on(state);
            int chance = boundedNumber(state.sign().trimmedText(EXTRA_LINE), 0, CERTAIN, CERTAIN);
            ChipWorld world = state.world();
            Vec3i centre = state.backPosition().add(area.centreOffset());

            for (int x = -area.radiusX(); x <= area.radiusX(); x++) {
                for (int y = -area.radiusY(); y <= area.radiusY(); y++) {
                    for (int z = -area.radiusZ(); z <= area.radiusZ(); z++) {
                        Vec3i target = centre.add(x, y, z);
                        if (world.isAir(target) || random.nextInt(CERTAIN) >= chance) {
                            continue;
                        }
                        world.strikeLightning(target);
                    }
                }
            }
        };
    }

    /**
     * Strikes everything standing near it.
     *
     * <p>Line 4 is how far to reach, which defaults to five blocks. Line 3 is unused.
     *
     * <p>Everything in range is struck, players included, which is the whole point of the chip.
     */
    public static SelfTriggeringICLogic holySmite() {
        return new HolySmite();
    }

    /** Strikes whatever is nearby, on a pulse or on every tick. */
    private static final class HolySmite implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                smite(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            smite(state);
        }

        private static void smite(ChipState state) {
            int range =
                    boundedNumber(
                            state.sign().trimmedText(EXTRA_LINE), 1, MAX_SMITE_RANGE, DEFAULT_SMITE_RANGE);

            Vec3d centre = Vec3d.centreOf(state.signPosition());
            for (Bystander bystander : state.world().bystandersNear(centre, range)) {
                if (bystander.isPresent()) {
                    state.world().strikeLightning(bystander.position().toBlock());
                }
            }
        }
    }

    /**
     * The box a bolt strike covers.
     *
     * @param radiusX how far it reaches east and west
     * @param radiusY how far it reaches up and down
     * @param radiusZ how far it reaches north and south
     * @param centreOffset how far the middle sits from the block the sign hangs on
     */
    private record Area(int radiusX, int radiusY, int radiusZ, Vec3i centreOffset) {

        static Area on(ChipState state) {
            String written = state.sign().trimmedText(PLACE_LINE);
            int separator = written.indexOf(CENTRE_SEPARATOR);
            String reach = separator < 0 ? written : written.substring(0, separator);
            Vec3i offset =
                    separator < 0
                            ? Vec3i.ZERO
                            : parseOffset(written.substring(separator + 1)).orElse(Vec3i.ZERO);

            Settings settings = state.settings();
            String[] parts = reach.split(",");
            if (parts.length >= 3) {
                return new Area(
                        radius(parts[0], settings),
                        radius(parts[1], settings),
                        radius(parts[2], settings),
                        offset);
            }
            int uniform = radius(reach, settings);
            return new Area(uniform, uniform, uniform, offset);
        }

        /** A reach off the sign, held to what the settings allow a chip to cover. */
        private static int radius(String written, Settings settings) {
            return boundedNumber(written, 0, settings.maxRadius(), DEFAULT_AREA_RADIUS);
        }

        private static Optional<Vec3i> parseOffset(String written) {
            String[] parts = written.split(":");
            if (parts.length != 3) {
                return Optional.empty();
            }
            try {
                return Optional.of(
                        new Vec3i(
                                Integer.parseInt(parts[0].trim()),
                                Integer.parseInt(parts[1].trim()),
                                Integer.parseInt(parts[2].trim())));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
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
