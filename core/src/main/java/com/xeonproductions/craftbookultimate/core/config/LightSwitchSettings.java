// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the light switches.
 *
 * <p>Both are ceilings rather than settings a switch always uses: a sign may ask for less on its
 * third and fourth lines, and asking for more gets as much as it is allowed.
 *
 * @param range how far a light switch reaches, unless its sign says otherwise
 * @param maxLights the most torches one light switch turns, unless its sign says otherwise
 */
@NullMarked
public record LightSwitchSettings(int range, int maxLights) {

    /** How far a light switch reaches out for torches, as the fork had it. */
    public static final int DEFAULT_RANGE = 10;

    /** How many it turns before it stops, as the fork had it. */
    public static final int DEFAULT_LIGHTS = 20;

    /** The light switches as they have always reached. */
    public static final LightSwitchSettings DEFAULTS =
            new LightSwitchSettings(DEFAULT_RANGE, DEFAULT_LIGHTS);

    /** Holds both limits to something a switch can work with. */
    public LightSwitchSettings {
        range = Math.max(0, range);
        maxLights = Math.max(0, maxLights);
    }

    /** These settings with switches reaching a different distance. */
    public LightSwitchSettings withRange(int reach) {
        return new LightSwitchSettings(reach, maxLights);
    }

    /** These settings with switches turning a different number of torches. */
    public LightSwitchSettings withMaxLights(int lights) {
        return new LightSwitchSettings(range, lights);
    }
}
