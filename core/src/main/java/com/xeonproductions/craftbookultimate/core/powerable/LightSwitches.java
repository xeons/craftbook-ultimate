// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.powerable;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/**
 * The sign that turns every torch around it on or off at once.
 *
 * <p>A torch on top of the sign is both the switch and the sign of which way it is thrown: a
 * redstone torch means the lights are on, a plain one means they are off. Clicking the sign turns
 * every torch within reach into the other kind, the one above it included, so the indicator and the
 * lights it stands for can never disagree.
 *
 * <p>Two limits, either of which a sign may lower for itself: how far it reaches on the third line
 * and how many torches it turns on the fourth. A sign asking for more than the settings allow gets
 * what the settings allow, which is the rule the whole configuration follows.
 */
@NullMarked
public final class LightSwitches {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.LIGHT_SWITCH;

    /** The names it claims, both of which are meant to look like a switch. */
    public static final List<String> SIGN_NAMES = List.of("[I]", "[|]");

    /** The line a sign says how far to reach on. */
    public static final int RANGE_LINE = 2;

    /** The line a sign says how many torches to turn on. */
    public static final int LIGHTS_LINE = 3;

    private LightSwitches() {
    }

    /** Whether a sign's second line is one of the two names. */
    public static boolean claims(String nameLine) {
        String written = nameLine.trim();
        for (String name : SIGN_NAMES) {
            if (written.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How far this sign reaches.
     *
     * <p>A sign may ask for less than the settings allow but never more, so narrowing the setting
     * shortens every switch on the server rather than breaking the ones that named a number.
     */
    public static int rangeOf(SignLines lines, MechanicSettings settings) {
        return Math.min(
                written(lines, RANGE_LINE, settings.lightSwitch().range()),
                settings.lightSwitch().range());
    }

    /** How many torches this sign turns, held the same way. */
    public static int lightsOf(SignLines lines, MechanicSettings settings) {
        return Math.min(
                written(lines, LIGHTS_LINE, settings.lightSwitch().maxLights()),
                settings.lightSwitch().maxLights());
    }

    /**
     * Every place within reach of the switch, nearest first.
     *
     * <p>A ball rather than a box, and symmetric about the sign — the fork swept a box running from
     * minus the range to one short of it, so a switch reached a block further west than east.
     * Nearest first because the limit on how many torches are turned bites in whatever order they
     * are found, and turning the ones by the switch is what somebody clicking it expects.
     */
    public static List<Vec3i> reach(int range) {
        List<Vec3i> within = new ArrayList<>();
        long limit = (long) range * range;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if ((long) x * x + (long) y * y + (long) z * z <= limit) {
                        within.add(new Vec3i(x, y, z));
                    }
                }
            }
        }

        within.sort((left, right) -> Long.compare(lengthSquared(left), lengthSquared(right)));
        return within;
    }

    /** A number written on a line, or the fallback where the line says nothing usable. */
    private static int written(SignLines lines, int index, int fallback) {
        String text = lines.trimmedText(index);
        if (text.isEmpty()) {
            return fallback;
        }
        try {
            int asked = Integer.parseInt(text.toLowerCase(Locale.ROOT));
            return asked < 0 ? fallback : asked;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long lengthSquared(Vec3i offset) {
        return (long) offset.x() * offset.x()
                + (long) offset.y() * offset.y()
                + (long) offset.z() * offset.z();
    }
}
