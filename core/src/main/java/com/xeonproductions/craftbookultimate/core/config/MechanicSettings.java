// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import com.xeonproductions.craftbookultimate.core.mechanic.SneakState;
import com.xeonproductions.craftbookultimate.core.mechanic.XpStorers;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the sign mechanics.
 *
 * <p>The bridges and doors take their limits from the settings the building chips already use,
 * because they are the same limits: how wide a structure may be, how far it may run, and what it
 * may be made of. What is here is what only these mechanics have — the gates, which are made of
 * something else and found a different way, and the lifts, which build nothing at all.
 *
 * @param disabled the mechanics that never run, by name, compared without regard to case
 * @param redstone whether redstone reaching a mechanic's sign works it
 * @param gateBlocks what a gate may be made of
 * @param gateRadius how far around its sign a gate looks for its own material
 * @param gateClicking whether clicking a gate's material works the gate as well as its sign does
 * @param liftJumping whether jumping and crouching work a lift
 * @param liftButtons whether a button in front of a lift's sign works it
 * @param liftTolerance how far a lift will drop somebody to find them a floor
 * @param maxAreaBlocks the most blocks one saved area may hold, or zero for no limit
 * @param maxAreasPerNamespace the most areas one name may have saved, or zero for no limit
 * @param glowstoneOffBlock what a glowstone is while it is dark
 * @param fireBlocks what catches light on top of itself while it is powered
 * @param depowerOnSourceRemoval whether a powered block goes out when the redstone feeding it is
 *     mined away, rather than staying as it was
 * @param lightSwitchRange how far a light switch reaches, unless its sign says otherwise
 * @param lightSwitchMaxLights the most torches one light switch turns, unless its sign says
 *     otherwise
 * @param lightStoneItem what is held to read a light level off a block
 * @param ammeterItem what is held to read a redstone power level off a block
 * @param bounceBlocks what may throw somebody when a [Jump] sign is under it
 * @param autoBounceBlocks what throws somebody with no sign at all, and how hard
 * @param bounceSensitivity how much of a jump counts as one
 * @param teleporterButtons whether a button opposite a teleporter sign works it
 * @param teleporterRequireSign whether the far end of a teleport needs a sign of its own
 * @param teleporterRange how far a teleporter may send somebody, or a negative number for no limit
 * @param xpStorerBlock what turns experience into bottles when it is clicked
 * @param xpPerBottle how much experience one bottle costs
 * @param xpRequiresBottle whether the player must be carrying empty bottles to fill
 * @param xpSneakState whether the player must be crouching to use one
 * @param snow how snow piles, slumps and melts
 */
@NullMarked
public record MechanicSettings(
        Set<String> disabled,
        boolean redstone,
        Set<Key> gateBlocks,
        int gateRadius,
        boolean gateClicking,
        boolean liftJumping,
        boolean liftButtons,
        int liftTolerance,
        int maxAreaBlocks,
        int maxAreasPerNamespace,
        Key glowstoneOffBlock,
        Set<Key> fireBlocks,
        boolean depowerOnSourceRemoval,
        int lightSwitchRange,
        int lightSwitchMaxLights,
        Key lightStoneItem,
        Key ammeterItem,
        Set<Key> bounceBlocks,
        Map<Key, String> autoBounceBlocks,
        double bounceSensitivity,
        boolean teleporterButtons,
        boolean teleporterRequireSign,
        double teleporterRange,
        Key xpStorerBlock,
        int xpPerBottle,
        boolean xpRequiresBottle,
        SneakState xpSneakState,
        SnowSettings snow) {

    /** Every kind of fence, which is what most gates are made of. */
    private static final String[] FENCES = {
        "oak_fence", "spruce_fence", "birch_fence", "jungle_fence", "acacia_fence",
        "dark_oak_fence", "pale_oak_fence", "mangrove_fence", "cherry_fence", "bamboo_fence",
        "crimson_fence", "warped_fence", "nether_brick_fence"
    };

    /** Every kind of glass pane, which is what a gate somebody wants to see through is made of. */
    private static final String[] PANES = {
        "glass_pane", "white_stained_glass_pane", "orange_stained_glass_pane",
        "magenta_stained_glass_pane", "light_blue_stained_glass_pane",
        "yellow_stained_glass_pane", "lime_stained_glass_pane", "pink_stained_glass_pane",
        "gray_stained_glass_pane", "light_gray_stained_glass_pane", "cyan_stained_glass_pane",
        "purple_stained_glass_pane", "blue_stained_glass_pane", "brown_stained_glass_pane",
        "green_stained_glass_pane", "red_stained_glass_pane", "black_stained_glass_pane"
    };

    /** The most a gate may reach, however wide an operator makes it. */
    public static final int MAX_GATE_RADIUS = 16;

    /** What a glowstone is while it is dark, as the fork had it. */
    public static final Key DEFAULT_GLOWSTONE_OFF = Key.key("minecraft:soul_sand");

    /** What catches light on top of itself while it is powered, as the fork had it. */
    public static final Key DEFAULT_FIRE_BLOCK = Key.key("minecraft:netherrack");

    /** How far a light switch reaches out for torches, as the fork had it. */
    public static final int DEFAULT_LIGHT_SWITCH_RANGE = 10;

    /** How many it turns before it stops, as the fork had it. */
    public static final int DEFAULT_LIGHT_SWITCH_LIGHTS = 20;

    /** What is held up to a block to read its light level. */
    public static final Key DEFAULT_LIGHT_STONE_ITEM = Key.key("minecraft:glowstone_dust");

    /** What is held up to a block to read its redstone power. */
    public static final Key DEFAULT_AMMETER_ITEM = Key.key("minecraft:charcoal");

    /** What throws somebody when a sign under it says how hard. */
    public static final Key DEFAULT_BOUNCE_BLOCK = Key.key("minecraft:diamond_block");

    /** What throws somebody with no sign at all. */
    public static final Key DEFAULT_AUTO_BOUNCE_BLOCK = Key.key("minecraft:orange_terracotta");

    /** How hard it throws them, in the frozen grammar a sign would use. */
    public static final String DEFAULT_AUTO_BOUNCE = "2,1,2";

    /** How much of a jump counts as one, as the fork had it. */
    public static final double DEFAULT_BOUNCE_SENSITIVITY = 0.1;

    /** What turns experience into bottles, as the fork had it. */
    public static final Key DEFAULT_XP_STORER_BLOCK = Key.key("minecraft:spawner");

    /** The mechanics as they have always been built. */
    public static final MechanicSettings DEFAULTS = new MechanicSettings(
            Set.of(), true, defaultGateBlocks(), 5, true, true, true, 5, 5000, 30,
            DEFAULT_GLOWSTONE_OFF, Set.of(DEFAULT_FIRE_BLOCK), false,
            DEFAULT_LIGHT_SWITCH_RANGE, DEFAULT_LIGHT_SWITCH_LIGHTS, DEFAULT_LIGHT_STONE_ITEM,
            DEFAULT_AMMETER_ITEM,
            Set.of(DEFAULT_BOUNCE_BLOCK),
            Map.of(DEFAULT_AUTO_BOUNCE_BLOCK, DEFAULT_AUTO_BOUNCE),
            DEFAULT_BOUNCE_SENSITIVITY, true, true, -1,
            DEFAULT_XP_STORER_BLOCK, XpStorers.DEFAULT_PER_BOTTLE, false, SneakState.MUST_NOT,
            SnowSettings.DEFAULTS);

    /** Copies the collections and holds every limit to something a mechanic can work with. */
    public MechanicSettings {
        disabled = lowercased(disabled);
        gateBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(gateBlocks));
        fireBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(fireBlocks));
        bounceBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(bounceBlocks));
        autoBounceBlocks = Collections.unmodifiableMap(new LinkedHashMap<>(autoBounceBlocks));
        bounceSensitivity = Math.max(0, bounceSensitivity);
        xpPerBottle = Math.max(1, xpPerBottle);
        lightSwitchRange = Math.max(0, lightSwitchRange);
        lightSwitchMaxLights = Math.max(0, lightSwitchMaxLights);
        gateRadius = Math.clamp(gateRadius, 1, MAX_GATE_RADIUS);
        liftTolerance = Math.max(1, liftTolerance);
        maxAreaBlocks = Math.max(0, maxAreaBlocks);
        maxAreasPerNamespace = Math.max(0, maxAreasPerNamespace);
    }

    /**
     * Whether an area of a size may be saved.
     *
     * <p>A limit of zero is no limit rather than none allowed, which is what the setting has
     * always been documented to mean.
     */
    public boolean allowsAreaOf(int blocks) {
        return maxAreaBlocks == 0 || blocks <= maxAreaBlocks;
    }

    /** Whether a namespace already holding this many areas may have another. */
    public boolean allowsAnotherArea(int held) {
        return maxAreasPerNamespace == 0 || held < maxAreasPerNamespace;
    }

    /**
     * Whether a mechanic runs.
     *
     * @param mechanic the mechanic's name, such as {@code Bridge}
     */
    public boolean allows(String mechanic) {
        return !disabled.contains(mechanic.toLowerCase(Locale.ROOT));
    }

    /** Whether a block is one a gate may be made of. */
    public boolean isGateBlock(Key block) {
        return gateBlocks.contains(block);
    }

    /** These settings with a different set of mechanics switched off. */
    public MechanicSettings withDisabled(Set<String> mechanics) {
        return new MechanicSettings(
                mechanics, redstone, gateBlocks, gateRadius, gateClicking,
                liftJumping, liftButtons, liftTolerance,
                maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with redstone allowed or refused. */
    public MechanicSettings withRedstone(boolean allowed) {
        return new MechanicSettings(
                disabled, allowed, gateBlocks, gateRadius, gateClicking,
                liftJumping, liftButtons, liftTolerance,
                maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with a different set of gate materials. */
    public MechanicSettings withGateBlocks(Set<Key> blocks) {
        return new MechanicSettings(
                disabled, redstone, blocks, gateRadius, gateClicking,
                liftJumping, liftButtons, liftTolerance,
                maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with gates reaching a different distance. */
    public MechanicSettings withGateRadius(int radius) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, radius, gateClicking,
                liftJumping, liftButtons, liftTolerance,
                maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with clicking a gate's material allowed or refused. */
    public MechanicSettings withGateClicking(boolean allowed) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, allowed,
                liftJumping, liftButtons, liftTolerance,
                maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with jump lifts allowed or refused. */
    public MechanicSettings withLiftJumping(boolean allowed) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                allowed, liftButtons, liftTolerance,
                maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with button lifts allowed or refused. */
    public MechanicSettings withLiftButtons(boolean allowed) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                liftJumping, allowed, liftTolerance, maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with a different limit on how big one area may be. */
    public MechanicSettings withMaxAreaBlocks(int blocks) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                liftJumping, liftButtons, liftTolerance, blocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with a different limit on how many areas one name may have. */
    public MechanicSettings withMaxAreasPerNamespace(int areas) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                liftJumping, liftButtons, liftTolerance, maxAreaBlocks, areas,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** These settings with lifts dropping somebody a different distance to find a floor. */
    public MechanicSettings withLiftTolerance(int tolerance) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                liftJumping, liftButtons, tolerance, maxAreaBlocks, maxAreasPerNamespace,
                glowstoneOffBlock, fireBlocks, depowerOnSourceRemoval,
                lightSwitchRange, lightSwitchMaxLights, lightStoneItem, ammeterItem,
                bounceBlocks, autoBounceBlocks, bounceSensitivity,
                teleporterButtons, teleporterRequireSign, teleporterRange,
                xpStorerBlock, xpPerBottle, xpRequiresBottle, xpSneakState, snow);
    }

    /** What a gate may be made of when nobody has said otherwise. */
    private static Set<Key> defaultGateBlocks() {
        Set<Key> blocks = new LinkedHashSet<>();
        for (String[] group : new String[][] {FENCES, PANES}) {
            for (String name : group) {
                blocks.add(Blocks.key(name));
            }
        }
        blocks.add(Blocks.key("iron_bars"));
        return Collections.unmodifiableSet(blocks);
    }

    private static Set<String> lowercased(Set<String> names) {
        Set<String> copy = new LinkedHashSet<>();
        for (String name : names) {
            copy.add(name.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(copy);
    }
}
