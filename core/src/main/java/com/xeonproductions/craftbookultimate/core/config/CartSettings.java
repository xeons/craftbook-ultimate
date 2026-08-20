package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the minecart mechanics.
 *
 * <p>A cart mechanic is a block under a piece of rail, so the setting that matters most is which
 * block. They are kept together here because they are all of a piece: which block builds which
 * mechanic, how hard the boosters push, and how fast anything that launches a cart launches it.
 *
 * @param disabled the mechanics that never run, by name, compared without regard to case
 * @param blocks which block builds which mechanic, by the mechanic's name in lower case
 * @param boosters how much each booster block multiplies a cart's speed by
 * @param launchSpeed how fast a mechanic that launches a cart launches it
 * @param returnWaterBuckets whether crafting in a cart gives back a full bucket or an empty one
 */
@NullMarked
public record CartSettings(
        Set<String> disabled,
        Map<String, Key> blocks,
        Map<Key, Double> boosters,
        double launchSpeed,
        boolean returnWaterBuckets) {

    /** The mechanics as they have always been built. */
    public static final CartSettings DEFAULTS = new CartSettings(
            Set.of(), defaultBlocks(), defaultBoosters(), 1.0, true);

    /** Copies the collections and holds the speed to something a cart can be pushed at. */
    public CartSettings {
        disabled = lowercased(disabled);
        blocks = Collections.unmodifiableMap(new LinkedHashMap<>(blocks));
        boosters = Collections.unmodifiableMap(new LinkedHashMap<>(boosters));
        launchSpeed = Math.max(0, launchSpeed);
    }

    /**
     * Whether a mechanic runs.
     *
     * @param mechanic the mechanic's name, such as {@code Station}
     */
    public boolean allows(String mechanic) {
        return !disabled.contains(mechanic.toLowerCase(Locale.ROOT));
    }

    /**
     * Which block builds a mechanic.
     *
     * <p>Empty for a mechanic that is recognised by its sign alone rather than by what it stands
     * on, and for one an operator has left without a block to build it from.
     *
     * @param mechanic the mechanic's name, such as {@code Station}
     */
    public Optional<Key> blockFor(String mechanic) {
        return Optional.ofNullable(blocks.get(mechanic.toLowerCase(Locale.ROOT)));
    }

    /** How much a booster block multiplies a cart's speed by, if it is one. */
    public Optional<Double> boostOf(Key block) {
        return Optional.ofNullable(boosters.get(block));
    }

    /** Whether a block is a booster at all. */
    public boolean isBooster(Key block) {
        return boosters.containsKey(block);
    }

    /** These settings with a different set of mechanics switched off. */
    public CartSettings withDisabled(Set<String> mechanics) {
        return new CartSettings(mechanics, blocks, boosters, launchSpeed, returnWaterBuckets);
    }

    /** These settings with one mechanic built from a different block. */
    public CartSettings withBlock(String mechanic, Key block) {
        Map<Key, Double> keptBoosters = boosters;
        Map<String, Key> changed = new LinkedHashMap<>(blocks);
        changed.put(mechanic.toLowerCase(Locale.ROOT), block);
        return new CartSettings(disabled, changed, keptBoosters, launchSpeed, returnWaterBuckets);
    }

    /** These settings with a different launch speed. */
    public CartSettings withLaunchSpeed(double speed) {
        return new CartSettings(disabled, blocks, boosters, speed, returnWaterBuckets);
    }

    /** These settings with a different set of booster blocks. */
    public CartSettings withBoosters(Map<Key, Double> boosters) {
        return new CartSettings(disabled, blocks, boosters, launchSpeed, returnWaterBuckets);
    }

    /**
     * The block each mechanic is built from unless an operator says otherwise.
     *
     * <p>Wool for most of them, so that a railway's mechanisms read as a row of colours from the
     * platform, and the odd distinctive block where a mechanic wants to look like what it does.
     * The two chest mechanics share a block and are told apart by their signs.
     */
    private static Map<String, Key> defaultBlocks() {
        Map<String, Key> blocks = new LinkedHashMap<>();
        blocks.put("station", Blocks.key("obsidian"));
        blocks.put("stationclear", Blocks.key("brown_wool"));
        blocks.put("sort", Blocks.key("netherrack"));
        blocks.put("lift", Blocks.key("orange_wool"));
        blocks.put("launch", Blocks.key("lime_wool"));
        blocks.put("delay", Blocks.key("yellow_wool"));
        blocks.put("load", Blocks.key("cyan_wool"));
        blocks.put("direction", Blocks.key("red_wool"));
        blocks.put("craft", Blocks.key("gray_wool"));
        blocks.put("collect", Blocks.key("iron_ore"));
        blocks.put("deposit", Blocks.key("iron_ore"));
        return blocks;
    }

    /** How much each booster block multiplies a cart's speed by. */
    private static Map<Key, Double> defaultBoosters() {
        Map<Key, Double> boosters = new LinkedHashMap<>();
        boosters.put(Blocks.key("gold_block"), 1000.0);
        boosters.put(Blocks.key("gold_ore"), 1.25);
        boosters.put(Blocks.key("gravel"), 0.80);
        boosters.put(Blocks.key("soul_sand"), 0.50);
        return boosters;
    }

    private static Set<String> lowercased(Set<String> names) {
        Set<String> copy = new LinkedHashSet<>();
        for (String name : names) {
            copy.add(name.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(copy);
    }
}
