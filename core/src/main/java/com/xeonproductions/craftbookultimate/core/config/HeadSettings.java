// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the heads a death leaves behind.
 *
 * @param playerHeads whether a player killed drops their own head
 * @param mobHeads whether a creature killed drops its head
 * @param playerKillsOnly whether only a death a player caused drops anything
 * @param dropRate how likely a head is, from nought to one
 * @param lootingRateModifier what each level of looting adds to that
 * @param showNameOnClick whether clicking a placed head says whose it is
 * @param ignoredNames accounts whose head is never handed out or named
 */
@NullMarked
public record HeadSettings(
        boolean playerHeads,
        boolean mobHeads,
        boolean playerKillsOnly,
        double dropRate,
        double lootingRateModifier,
        boolean showNameOnClick,
        Set<String> ignoredNames) {

    /** How likely a head is out of the box, as the fork had it: one death in twenty. */
    public static final double DEFAULT_DROP_RATE = 0.05;

    /** What each level of looting adds, as the fork had it. */
    public static final double DEFAULT_LOOTING_MODIFIER = 0.05;

    /**
     * The one account the fork left alone by default.
     *
     * <p>A library that used a head as a marker in the world, so handing its head out would put a
     * piece of another plugin's furniture into somebody's inventory.
     */
    public static final String DEFAULT_IGNORED = "cscorelib";

    /** The heads as they have always dropped. */
    public static final HeadSettings DEFAULTS = new HeadSettings(
            true, true, true, DEFAULT_DROP_RATE, DEFAULT_LOOTING_MODIFIER, true,
            Set.of(DEFAULT_IGNORED));

    /** Copies the list and holds both chances to something that can happen. */
    public HeadSettings {
        ignoredNames = Collections.unmodifiableSet(new LinkedHashSet<>(ignoredNames));
        dropRate = Math.clamp(dropRate, 0, 1);
        lootingRateModifier = Math.max(0, lootingRateModifier);
    }

    /** Whether anything at all drops, which decides whether a death is looked at twice. */
    public boolean anythingAtAll() {
        return (playerHeads || mobHeads) && dropRate > 0;
    }

    /** These settings with players dropping their heads, or not. */
    public HeadSettings withPlayerHeads(boolean dropping) {
        return new HeadSettings(dropping, mobHeads, playerKillsOnly, dropRate,
                lootingRateModifier, showNameOnClick, ignoredNames);
    }

    /** These settings with creatures dropping their heads, or not. */
    public HeadSettings withMobHeads(boolean dropping) {
        return new HeadSettings(playerHeads, dropping, playerKillsOnly, dropRate,
                lootingRateModifier, showNameOnClick, ignoredNames);
    }

    /** These settings with any death dropping a head, or only one a player caused. */
    public HeadSettings withPlayerKillsOnly(boolean only) {
        return new HeadSettings(playerHeads, mobHeads, only, dropRate,
                lootingRateModifier, showNameOnClick, ignoredNames);
    }

    /** These settings with heads more or less likely. */
    public HeadSettings withDropRate(double rate) {
        return new HeadSettings(playerHeads, mobHeads, playerKillsOnly, rate,
                lootingRateModifier, showNameOnClick, ignoredNames);
    }

    /** These settings with looting counting for more or less. */
    public HeadSettings withLootingRateModifier(double perLevel) {
        return new HeadSettings(playerHeads, mobHeads, playerKillsOnly, dropRate,
                perLevel, showNameOnClick, ignoredNames);
    }

    /** These settings with a clicked head saying whose it is, or staying quiet. */
    public HeadSettings withShowNameOnClick(boolean saying) {
        return new HeadSettings(playerHeads, mobHeads, playerKillsOnly, dropRate,
                lootingRateModifier, saying, ignoredNames);
    }

    /** These settings with a different set of accounts left alone. */
    public HeadSettings withIgnoredNames(Set<String> names) {
        return new HeadSettings(playerHeads, mobHeads, playerKillsOnly, dropRate,
                lootingRateModifier, showNameOnClick, names);
    }
}
