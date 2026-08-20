package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.key.Key;
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
        int liftTolerance) {

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

    /** The mechanics as they have always worked. */
    public static final MechanicSettings DEFAULTS = new MechanicSettings(
            Set.of(), true, defaultGateBlocks(), 5, true, true, true, 5);

    /** Copies the collections and holds every limit to something a mechanic can work with. */
    public MechanicSettings {
        disabled = lowercased(disabled);
        gateBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(gateBlocks));
        gateRadius = Math.clamp(gateRadius, 1, MAX_GATE_RADIUS);
        liftTolerance = Math.max(1, liftTolerance);
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
                liftJumping, liftButtons, liftTolerance);
    }

    /** These settings with redstone allowed or refused. */
    public MechanicSettings withRedstone(boolean allowed) {
        return new MechanicSettings(
                disabled, allowed, gateBlocks, gateRadius, gateClicking,
                liftJumping, liftButtons, liftTolerance);
    }

    /** These settings with a different set of gate materials. */
    public MechanicSettings withGateBlocks(Set<Key> blocks) {
        return new MechanicSettings(
                disabled, redstone, blocks, gateRadius, gateClicking,
                liftJumping, liftButtons, liftTolerance);
    }

    /** These settings with gates reaching a different distance. */
    public MechanicSettings withGateRadius(int radius) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, radius, gateClicking,
                liftJumping, liftButtons, liftTolerance);
    }

    /** These settings with clicking a gate's material allowed or refused. */
    public MechanicSettings withGateClicking(boolean allowed) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, allowed,
                liftJumping, liftButtons, liftTolerance);
    }

    /** These settings with jump lifts allowed or refused. */
    public MechanicSettings withLiftJumping(boolean allowed) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                allowed, liftButtons, liftTolerance);
    }

    /** These settings with button lifts allowed or refused. */
    public MechanicSettings withLiftButtons(boolean allowed) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                liftJumping, allowed, liftTolerance);
    }

    /** These settings with lifts dropping somebody a different distance to find a floor. */
    public MechanicSettings withLiftTolerance(int tolerance) {
        return new MechanicSettings(
                disabled, redstone, gateBlocks, gateRadius, gateClicking,
                liftJumping, liftButtons, tolerance);
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
