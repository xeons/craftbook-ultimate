// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.powerable;

import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The blocks that answer redstone, and what each of them does.
 *
 * <p>Three, where the fork had four. Its {@code Netherrack} and {@code LightNetherrack} were the
 * same mechanic written twice: both put a fire above the block when it was powered and took it away
 * when it was not, and the only difference was that one had the block it worked on hardcoded and
 * the other let an operator name it. One implementation answers for both, under the name the fork
 * that owns this codebase used, and the block is a setting.
 */
@NullMarked
public final class Powerables {

    /** Glowstone, which goes dark rather than away. */
    public static final String GLOWSTONE = "GlowStone";

    /** A carved pumpkin that lights up. */
    public static final String JACK_O_LANTERN = "JackOLantern";

    /** Netherrack, and whatever else an operator says catches light. */
    public static final String NETHERRACK = "Netherrack";

    /** What a lit glowstone is. */
    public static final Key GLOWSTONE_ON = Key.key("minecraft:glowstone");

    /** A carved pumpkin, unlit. */
    public static final Key PUMPKIN_OFF = Key.key("minecraft:carved_pumpkin");

    /** A carved pumpkin with a candle in it. */
    public static final Key PUMPKIN_ON = Key.key("minecraft:jack_o_lantern");

    private Powerables() {
    }

    /** Every one of them, as an operator has asked for them. */
    public static List<Powerable> all(MechanicSettings settings) {
        List<Powerable> powerables = new ArrayList<>(3);
        add(powerables, settings, new Powerable.Swap(
                GLOWSTONE, settings.glowstoneOffBlock(), GLOWSTONE_ON));
        add(powerables, settings, new Powerable.Swap(
                JACK_O_LANTERN, PUMPKIN_OFF, PUMPKIN_ON));
        add(powerables, settings, new Powerable.Fire(NETHERRACK, settings.fireBlocks()));
        return List.copyOf(powerables);
    }

    /**
     * The one that works on a block, if any does.
     *
     * <p>The first that claims it wins, and nothing claims the same block twice: a glowstone is not
     * a pumpkin and neither catches fire. An operator who names glowstone as a fire block gets the
     * fire, which is the order they are declared in and the only sensible reading of having asked
     * for both.
     */
    public static Optional<Powerable> workingOn(List<Powerable> powerables, Key block) {
        for (Powerable powerable : powerables) {
            if (powerable.worksOn(block)) {
                return Optional.of(powerable);
            }
        }
        return Optional.empty();
    }

    private static void add(
            List<Powerable> powerables, MechanicSettings settings, Powerable powerable) {
        if (settings.allows(powerable.name())) {
            powerables.add(powerable);
        }
    }
}
