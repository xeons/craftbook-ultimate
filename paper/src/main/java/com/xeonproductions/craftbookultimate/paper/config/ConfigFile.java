// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.config;

import com.xeonproductions.craftbookultimate.core.config.CartHabits;
import com.xeonproductions.craftbookultimate.core.config.CartSettings;
import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.paper.ic.LegacyBlocks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * The settings file, and how it becomes {@link Settings}.
 *
 * <p>Anything the file does not mention is filled in from the defaults and written back, so a
 * server started on a new version finds the settings it has just gained rather than silently
 * running without them. Values already in the file are never overwritten; only the comments are
 * rewritten, so an explanation improved in one version reaches a server that has been running
 * since an earlier one.
 *
 * <p>An entry that cannot be understood is reported and skipped rather than taking the rest of
 * the file down with it.
 */
@NullMarked
public final class ConfigFile {

    /** What the file is called inside the plugin's own folder. */
    public static final String FILE_NAME = "config.yml";

    /** Marks a list entry that names a block tag rather than a single block. */
    private static final char TAG_MARKER = '#';

    private static final String ENABLED = "enabled";
    private static final String DISABLED_WORLDS = "disabled-worlds";
    private static final String DISABLED_CHIPS = "ics.disabled";
    private static final String MAX_RADIUS = "ics.max-radius";
    private static final String MAX_WIDTH = "ics.max-width";
    private static final String MAX_LENGTH = "ics.max-length";
    private static final String MAX_PLANTER_WIDTH = "ics.max-planter-width";
    private static final String PLACEABLE_BLOCKS = "ics.placeable-blocks";

    private static final String CARTS_DISABLED = "carts.disabled";
    private static final String CART_BLOCKS = "carts.blocks";
    private static final String CART_BOOSTERS = "carts.boosters";
    private static final String CART_LAUNCH_SPEED = "carts.launch-speed";
    private static final String CART_WATER_BUCKETS = "carts.return-water-buckets";
    private static final String HABIT_DECAY_AFTER = "carts.habits.decay-empty-after";
    private static final String HABIT_DECAY_AFTER_EXIT = "carts.habits.decay-only-after-exit";
    private static final String HABIT_REMOVE_ON_EXIT = "carts.habits.remove-on-exit";
    private static final String HABIT_GIVE_BACK = "carts.habits.give-cart-back";
    private static final String HABIT_PICK_UP = "carts.habits.pick-up-items";
    private static final String HABIT_BLOCK_MOBS = "carts.habits.block-mobs";
    private static final String HABIT_CLIMB_SPEED = "carts.habits.climb-speed";
    private static final String HABIT_PLATE_CROSSINGS = "carts.habits.plate-crossings";
    private static final String HABIT_THROUGH_EMPTY = "carts.habits.pass-through-empty-carts";
    private static final String HABIT_THROUGH_FULL = "carts.habits.pass-through-full-carts";
    private static final String HABIT_RUN_DOWN = "carts.habits.run-down-what-it-hits";
    private static final String HABIT_RUN_DOWN_HURTS = "carts.habits.run-down-only-hurts";
    private static final String HABIT_RUN_DOWN_CARTS = "carts.habits.run-down-other-carts";
    private static final String PIPES_ENABLED = "pipes.enabled";
    private static final String PIPES_MAX_LENGTH = "pipes.max-length";
    private static final String PIPES_STACK_PER_PULL = "pipes.stack-per-pull";

    private static final String MECHANICS_DISABLED = "mechanics.disabled";
    private static final String MECHANICS_REDSTONE = "mechanics.redstone";
    private static final String GATE_BLOCKS = "mechanics.gate-blocks";
    private static final String GATE_RADIUS = "mechanics.gate-radius";
    private static final String GATE_CLICKING = "mechanics.gate-clicking";
    private static final String LIFT_JUMPING = "mechanics.lift-jumping";
    private static final String LIFT_BUTTONS = "mechanics.lift-buttons";
    private static final String LIFT_TOLERANCE = "mechanics.lift-tolerance";
    private static final String MAX_AREA_BLOCKS = "mechanics.max-area-blocks";
    private static final String MAX_AREAS = "mechanics.max-areas-per-name";

    private final Path file;
    private final Server server;
    private final Consumer<String> report;

    /**
     * @param directory the plugin's own folder
     * @param server the server whose block tags an entry may name
     * @param report where to send a complaint about an entry that could not be understood
     */
    public ConfigFile(Path directory, Server server, Consumer<String> report) {
        this.file = directory.resolve(FILE_NAME);
        this.server = server;
        this.report = report;
    }

    /** Where the settings are kept. */
    public Path path() {
        return file;
    }

    /**
     * Reads the file, writing back anything it was missing.
     *
     * @throws IOException if the file exists but cannot be read or written
     */
    public Settings load() throws IOException {
        YamlConfiguration yaml = read();
        fillIn(yaml);
        explain(yaml);
        write(yaml);
        return build(yaml);
    }

    private YamlConfiguration read() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        if (!Files.isRegularFile(file)) {
            return yaml;
        }
        try {
            yaml.load(file.toFile());
        } catch (InvalidConfigurationException e) {
            throw new IOException("The settings file is not valid YAML", e);
        }
        return yaml;
    }

    private void write(YamlConfiguration yaml) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        yaml.save(file.toFile());
    }

    /** Puts the default of every setting the file does not already carry into it. */
    private static void fillIn(YamlConfiguration yaml) {
        Settings defaults = Settings.DEFAULTS;
        setIfAbsent(yaml, ENABLED, defaults.enabled());
        setIfAbsent(yaml, DISABLED_WORLDS, new ArrayList<>(defaults.disabledWorlds()));
        setIfAbsent(yaml, DISABLED_CHIPS, new ArrayList<>(defaults.disabledChips()));
        setIfAbsent(yaml, MAX_RADIUS, defaults.maxRadius());
        setIfAbsent(yaml, MAX_WIDTH, defaults.maxWidth());
        setIfAbsent(yaml, MAX_LENGTH, defaults.maxLength());
        setIfAbsent(yaml, MAX_PLANTER_WIDTH, defaults.maxPlanterWidth());
        setIfAbsent(yaml, PLACEABLE_BLOCKS, names(defaults.placeableBlocks()));

        CartSettings carts = defaults.carts();
        setIfAbsent(yaml, CARTS_DISABLED, new ArrayList<>(carts.disabled()));
        setIfAbsent(yaml, CART_LAUNCH_SPEED, carts.launchSpeed());
        setIfAbsent(yaml, CART_WATER_BUCKETS, carts.returnWaterBuckets());
        carts.blocks().forEach((mechanic, block) ->
                setIfAbsent(yaml, CART_BLOCKS + "." + mechanic, block.asString()));
        carts.boosters().forEach((block, multiplier) ->
                setIfAbsent(yaml, CART_BOOSTERS + "." + block.value(), multiplier));

        CartHabits habits = carts.habits();
        setIfAbsent(yaml, HABIT_DECAY_AFTER, habits.decayEmptyAfter());
        setIfAbsent(yaml, HABIT_DECAY_AFTER_EXIT, habits.decayOnlyAfterExit());
        setIfAbsent(yaml, HABIT_REMOVE_ON_EXIT, habits.removeOnExit());
        setIfAbsent(yaml, HABIT_GIVE_BACK, habits.giveCartBack());
        setIfAbsent(yaml, HABIT_PICK_UP, habits.pickUpItems());
        setIfAbsent(yaml, HABIT_BLOCK_MOBS, habits.blockMobs());
        setIfAbsent(yaml, HABIT_CLIMB_SPEED, habits.climbSpeed());
        setIfAbsent(yaml, HABIT_PLATE_CROSSINGS, habits.plateIntersections());
        setIfAbsent(yaml, HABIT_THROUGH_EMPTY, habits.passThroughEmptyCarts());
        setIfAbsent(yaml, HABIT_THROUGH_FULL, habits.passThroughFullCarts());
        setIfAbsent(yaml, HABIT_RUN_DOWN, habits.runDownEntities());
        setIfAbsent(yaml, HABIT_RUN_DOWN_HURTS, habits.runDownOnlyHurts());
        setIfAbsent(yaml, HABIT_RUN_DOWN_CARTS, habits.runDownOtherCarts());

        PipeSettings pipes = defaults.pipes();
        setIfAbsent(yaml, PIPES_ENABLED, pipes.enabled());
        setIfAbsent(yaml, PIPES_MAX_LENGTH, pipes.maxLength());
        setIfAbsent(yaml, PIPES_STACK_PER_PULL, pipes.stackPerPull());

        MechanicSettings mechanics = defaults.mechanics();
        setIfAbsent(yaml, MECHANICS_DISABLED, new ArrayList<>(mechanics.disabled()));
        setIfAbsent(yaml, MECHANICS_REDSTONE, mechanics.redstone());
        setIfAbsent(yaml, GATE_BLOCKS, names(mechanics.gateBlocks()));
        setIfAbsent(yaml, GATE_RADIUS, mechanics.gateRadius());
        setIfAbsent(yaml, GATE_CLICKING, mechanics.gateClicking());
        setIfAbsent(yaml, LIFT_JUMPING, mechanics.liftJumping());
        setIfAbsent(yaml, LIFT_BUTTONS, mechanics.liftButtons());
        setIfAbsent(yaml, LIFT_TOLERANCE, mechanics.liftTolerance());
        setIfAbsent(yaml, MAX_AREA_BLOCKS, mechanics.maxAreaBlocks());
        setIfAbsent(yaml, MAX_AREAS, mechanics.maxAreasPerNamespace());
    }

    private static void setIfAbsent(YamlConfiguration yaml, String path, Object value) {
        if (!yaml.isSet(path)) {
            yaml.set(path, value);
        }
    }

    /** Says what each setting is for, in the file itself. */
    private static void explain(YamlConfiguration yaml) {
        yaml.options().setHeader(List.of(
                "CraftBook Ultimate.",
                "",
                "Everything here is either a limit on how far a chip may reach or a statement",
                "about what may run at all. None of it changes what a sign means: a sign asking",
                "for more than it is allowed gets as much as it is allowed, so changing a limit",
                "shortens or lengthens what an existing build does rather than breaking it.",
                "",
                "Changes take effect on the next /craftbook reload."));

        yaml.setComments(ENABLED, List.of(
                "Whether chips run at all.",
                "Setting this to false leaves every sign where it is and stops it working,",
                "which is how the plugin is taken out of service without losing anything."));

        yaml.setComments(DISABLED_WORLDS, List.of(
                "",
                "Worlds where no chip runs, by name. Chips there are left alone, not removed."));

        yaml.setComments("ics", List.of(
                "",
                "The integrated circuits: the chips built by writing a model reference on a sign."));

        yaml.setComments(DISABLED_CHIPS, List.of(
                "Chips that are never created and never run, by model number, such as MCX203.",
                "A chip answering to more than one number is switched off by any of them."));

        yaml.setComments(MAX_RADIUS, List.of(
                "",
                "The furthest a chip may reach when its sign gives a radius.",
                "Only the chips that take a free radius rather than a named range read this;",
                "the bolt strike is the one that does."));

        yaml.setComments(MAX_WIDTH, List.of(
                "",
                "The widest a bridge, a door or a harvested area may be, in blocks across."));

        yaml.setComments(MAX_LENGTH, List.of(
                "The furthest a bridge, a door or a harvested area may run from its sign.",
                "A door reads this as its height."));

        yaml.setComments(MAX_PLANTER_WIDTH, List.of(
                "The largest field an area planter may sow, along either side."));

        yaml.setComments(PLACEABLE_BLOCKS, List.of(
                "",
                "What a bridge, a door or a flex set may place. Anything else is refused.",
                "Taking a block away is never refused, so striking one off this list leaves the",
                "structures already made of it able to retract rather than stuck out.",
                "",
                "An entry is a block name, a tag written with a leading # such as",
                "#minecraft:planks, or a name from before the flattening such as 35:14.",
                "An empty list allows any block at all."));

        yaml.setComments("carts", List.of(
                "",
                "The minecart mechanics: a block under a piece of rail that a cart rolls over."));

        yaml.setComments(CARTS_DISABLED, List.of(
                "Mechanics that never run, by name, such as Craft. The signs are left alone."));

        yaml.setComments(CART_BLOCKS, List.of(
                "",
                "Which block builds which mechanic. Two mechanics may share a block, in which",
                "case their signs tell them apart. The message sign has no block of its own and",
                "works wherever it is hung."));

        yaml.setComments(CART_BOOSTERS, List.of(
                "",
                "How much a booster block multiplies a passing cart's speed by. Above one speeds",
                "a cart up and below one slows it down; the very large number is what sends a",
                "cart off at its top speed."));

        yaml.setComments("carts.habits", List.of(
                "",
                "How every cart behaves, whether or not it is standing on a mechanism. All of it",
                "is off out of the box: a server that has never been configured runs carts",
                "exactly as the game does. The two numbers switch their own habit off when they",
                "are zero, since waiting no time and climbing at no speed both mean not doing it."));

        yaml.setComments(HABIT_DECAY_AFTER, List.of(
                "How many ticks a cart may stand empty before it is taken away. 0 leaves empty",
                "carts alone; " + CartHabits.CUSTOMARY_DECAY_TICKS + " is two seconds."));

        yaml.setComments(HABIT_DECAY_AFTER_EXIT, List.of(
                "Whether only a cart somebody has got out of decays. Turning this off starts the",
                "clock on every cart the moment it is placed, including ones nobody has touched."));

        yaml.setComments(HABIT_REMOVE_ON_EXIT, List.of(
                "",
                "Whether a cart is taken away the moment its rider steps out, so a station is",
                "never left with a row of abandoned carts."));

        yaml.setComments(HABIT_GIVE_BACK, List.of(
                "Whether taking it away hands the rider the cart back. Creative mode gets",
                "nothing, having lost nothing."));

        yaml.setComments(HABIT_PICK_UP, List.of(
                "",
                "Whether a storage cart gathers up items it runs over. A stack that will not",
                "all fit is left where it lies rather than half taken."));

        yaml.setComments(HABIT_BLOCK_MOBS, List.of(
                "Whether creatures are kept out of carts, leaving them for people."));

        yaml.setComments(HABIT_CLIMB_SPEED, List.of(
                "",
                "How fast a cart climbs a ladder or a vine, which it cannot do in the game",
                "itself. 0 for a cart that cannot climb; " + CartHabits.CUSTOMARY_CLIMB_SPEED
                        + " is a comfortable pace."));

        yaml.setComments(HABIT_PLATE_CROSSINGS, List.of(
                "Whether a pressure plate carries a cart straight across it as a crossroads,",
                "at full speed, instead of the rail turning it."));

        yaml.setComments(HABIT_THROUGH_EMPTY, List.of(
                "",
                "Whether a cart passes through an empty one rather than shunting it, so a siding",
                "of spares does not block the line."));

        yaml.setComments(HABIT_THROUGH_FULL, List.of(
                "Whether a cart passes through a laden or occupied one, so goods and people can",
                "share a track."));

        yaml.setComments(HABIT_RUN_DOWN, List.of(
                "",
                "Whether a cart with somebody aboard hurts what it runs into. An empty cart",
                "rolling downhill never does."));

        yaml.setComments(HABIT_RUN_DOWN_HURTS, List.of(
                "Whether running something down stops at hurting it. Nothing is removed when",
                "this is on, including the things that cannot be hurt."));

        yaml.setComments(HABIT_RUN_DOWN_CARTS, List.of(
                "Whether an occupied cart runs down other carts as well as creatures."));

        yaml.setComments(CART_LAUNCH_SPEED, List.of(
                "",
                "How fast a mechanic that launches a cart launches it: a delay letting one go, a",
                "launcher somebody has climbed into, or a dispenser told to push."));

        yaml.setComments(CART_WATER_BUCKETS, List.of(
                "Whether crafting in a cart gives a water bucket back full rather than empty.",
                "Vanilla gives back an empty one; this is a kindness to anybody crafting in bulk."));

        yaml.setComments("pipes", List.of(
                "",
                "The pipes: a run of glass or panes that carries items from one container to",
                "another when the block at its head is powered. Both ways of building one work.",
                "A sticky piston starts a run of glass, which branches wherever it touches more",
                "glass, goes straight over a pane, keeps to its own colour where it is stained,",
                "and hands what reaches it to whatever a plain piston points at. A piston with an",
                "[Extractor] sign starts a run of panes instead, which spreads at every pane and",
                "fills any container it touches.",
                "",
                "A sign named [Pipe] or [Extractor] may name what a way out will take on its third",
                "line and what it will refuse on its fourth, separated by commas."));

        yaml.setComments(PIPES_ENABLED, List.of(
                "Whether pipes carry anything. The blocks stay where they are either way."));

        yaml.setComments(PIPES_MAX_LENGTH, List.of(
                "How many blocks of pipe are followed before the search gives up. A pipe past",
                "this carries items as far as the limit reaches rather than refusing to work."));

        yaml.setComments(PIPES_STACK_PER_PULL, List.of(
                "Whether one pulse moves a single stack. Turning this off empties as much of the",
                "container as the pipe can find room for, which is faster and far more work."));

        yaml.setComments("mechanics", List.of(
                "",
                "The sign mechanics: the bridges, doors, gates and lifts. How wide a bridge or a",
                "door may be, how far it may run and what it may be made of come from the ics",
                "section above, because those are the same limits."));

        yaml.setComments(MECHANICS_DISABLED, List.of(
                "Mechanics that never run, by name: Bridge, Door, Gate or Elevator.",
                "The signs are left alone, not removed."));

        yaml.setComments(MECHANICS_REDSTONE, List.of(
                "",
                "Whether redstone arriving beside a sign works the mechanic on it.",
                "Power arriving shuts it and power leaving opens it, so a mechanic on a lever",
                "always agrees with the lever."));

        yaml.setComments(GATE_BLOCKS, List.of(
                "",
                "What a gate may be made of. An entry is a block name, a tag written with a",
                "leading # such as #minecraft:fences, or a name from before the flattening.",
                "The glass, iron and nether gate signs each take only their own material out of",
                "this list; the plain sign takes any of it."));

        yaml.setComments(GATE_RADIUS, List.of(
                "",
                "How far around its sign a gate looks for its own material. The D forms of each",
                "gate sign ignore this and look barely past themselves, which is how two gates",
                "standing side by side are kept from catching one another."));

        yaml.setComments(GATE_CLICKING, List.of(
                "Whether a gate whose sign ends in C answers to a hand on its own fence."));

        yaml.setComments(LIFT_JUMPING, List.of(
                "",
                "Whether jumping and crouching work a [Lift UpDown] sign fixed to the block",
                "somebody is standing on."));

        yaml.setComments(LIFT_BUTTONS, List.of(
                "Whether a button two blocks in front of a lift's sign works it."));

        yaml.setComments(LIFT_TOLERANCE, List.of(
                "How far a lift will drop somebody below the far sign to find them a floor.",
                "Beyond this it says there is no floor rather than dropping them down a shaft."));

        yaml.setComments(MAX_AREA_BLOCKS, List.of(
                "",
                "The most blocks one saved area may hold. Zero is no limit.",
                "An area is saved with /area save and lives in the areas folder, one file of",
                "blocks and one saying where they go."));

        yaml.setComments(MAX_AREAS, List.of(
                "The most areas any one name may have saved. Zero is no limit.",
                "Saving over an area that already exists does not count against this."));
    }

    /** Turns what the file says into the settings the chips read. */
    private Settings build(YamlConfiguration yaml) {
        Settings defaults = Settings.DEFAULTS;
        return Settings.builder()
                .enabled(yaml.getBoolean(ENABLED, defaults.enabled()))
                .disabledWorlds(Set.copyOf(yaml.getStringList(DISABLED_WORLDS)))
                .disabledChips(Set.copyOf(yaml.getStringList(DISABLED_CHIPS)))
                .maxRadius(yaml.getInt(MAX_RADIUS, defaults.maxRadius()))
                .maxWidth(yaml.getInt(MAX_WIDTH, defaults.maxWidth()))
                .maxLength(yaml.getInt(MAX_LENGTH, defaults.maxLength()))
                .maxPlanterWidth(yaml.getInt(MAX_PLANTER_WIDTH, defaults.maxPlanterWidth()))
                .placeableBlocks(blocks(yaml.getStringList(PLACEABLE_BLOCKS)))
                .carts(carts(yaml, defaults.carts()))
                .pipes(pipes(yaml, defaults.pipes()))
                .mechanics(mechanics(yaml, defaults.mechanics()))
                .build();
    }

    /** Reads what an operator has said about the sign mechanics. */
    private MechanicSettings mechanics(YamlConfiguration yaml, MechanicSettings defaults) {
        Set<Key> gateBlocks = blocks(yaml.getStringList(GATE_BLOCKS));
        return new MechanicSettings(
                Set.copyOf(yaml.getStringList(MECHANICS_DISABLED)),
                yaml.getBoolean(MECHANICS_REDSTONE, defaults.redstone()),
                gateBlocks.isEmpty() ? defaults.gateBlocks() : gateBlocks,
                yaml.getInt(GATE_RADIUS, defaults.gateRadius()),
                yaml.getBoolean(GATE_CLICKING, defaults.gateClicking()),
                yaml.getBoolean(LIFT_JUMPING, defaults.liftJumping()),
                yaml.getBoolean(LIFT_BUTTONS, defaults.liftButtons()),
                yaml.getInt(LIFT_TOLERANCE, defaults.liftTolerance()),
                yaml.getInt(MAX_AREA_BLOCKS, defaults.maxAreaBlocks()),
                yaml.getInt(MAX_AREAS, defaults.maxAreasPerNamespace()));
    }

    /** Reads what an operator has said about the minecart mechanics. */
    private CartSettings carts(YamlConfiguration yaml, CartSettings defaults) {
        Map<String, Key> mechanicBlocks = new LinkedHashMap<>();
        ConfigurationSection blocks = yaml.getConfigurationSection(CART_BLOCKS);
        if (blocks != null) {
            for (String mechanic : blocks.getKeys(false)) {
                String written = blocks.getString(mechanic, "");
                if (written == null || written.isBlank()) {
                    continue;
                }
                Optional<Key> block = LegacyBlocks.resolve(written);
                if (block.isEmpty()) {
                    report.accept("No block called " + written
                            + ", so the " + mechanic + " cart mechanic cannot be built");
                    continue;
                }
                mechanicBlocks.put(mechanic, block.get());
            }
        }

        Map<Key, Double> boosters = new LinkedHashMap<>();
        ConfigurationSection boosting = yaml.getConfigurationSection(CART_BOOSTERS);
        if (boosting != null) {
            for (String written : boosting.getKeys(false)) {
                Optional<Key> block = LegacyBlocks.resolve(written);
                if (block.isEmpty()) {
                    report.accept("No block called " + written + ", so it boosts nothing");
                    continue;
                }
                boosters.put(block.get(), boosting.getDouble(written, 1));
            }
        }

        return new CartSettings(
                Set.copyOf(yaml.getStringList(CARTS_DISABLED)),
                mechanicBlocks.isEmpty() ? defaults.blocks() : mechanicBlocks,
                boosters.isEmpty() ? defaults.boosters() : boosters,
                yaml.getDouble(CART_LAUNCH_SPEED, defaults.launchSpeed()),
                yaml.getBoolean(CART_WATER_BUCKETS, defaults.returnWaterBuckets()),
                habits(yaml, defaults.habits()));
    }

    /** What an operator has said about the pipes. */
    private static PipeSettings pipes(YamlConfiguration yaml, PipeSettings defaults) {
        return new PipeSettings(
                yaml.getBoolean(PIPES_ENABLED, defaults.enabled()),
                yaml.getInt(PIPES_MAX_LENGTH, defaults.maxLength()),
                yaml.getBoolean(PIPES_STACK_PER_PULL, defaults.stackPerPull()));
    }

    /** How every cart behaves, whether or not it is standing on a mechanism. */
    private static CartHabits habits(YamlConfiguration yaml, CartHabits defaults) {
        return new CartHabits(
                yaml.getLong(HABIT_DECAY_AFTER, defaults.decayEmptyAfter()),
                yaml.getBoolean(HABIT_DECAY_AFTER_EXIT, defaults.decayOnlyAfterExit()),
                yaml.getBoolean(HABIT_REMOVE_ON_EXIT, defaults.removeOnExit()),
                yaml.getBoolean(HABIT_GIVE_BACK, defaults.giveCartBack()),
                yaml.getBoolean(HABIT_PICK_UP, defaults.pickUpItems()),
                yaml.getBoolean(HABIT_BLOCK_MOBS, defaults.blockMobs()),
                yaml.getDouble(HABIT_CLIMB_SPEED, defaults.climbSpeed()),
                yaml.getBoolean(HABIT_PLATE_CROSSINGS, defaults.plateIntersections()),
                yaml.getBoolean(HABIT_THROUGH_EMPTY, defaults.passThroughEmptyCarts()),
                yaml.getBoolean(HABIT_THROUGH_FULL, defaults.passThroughFullCarts()),
                yaml.getBoolean(HABIT_RUN_DOWN, defaults.runDownEntities()),
                yaml.getBoolean(HABIT_RUN_DOWN_HURTS, defaults.runDownOnlyHurts()),
                yaml.getBoolean(HABIT_RUN_DOWN_CARTS, defaults.runDownOtherCarts()));
    }

    /**
     * Reads a list of blocks, each named directly or by a tag.
     *
     * <p>A tag is expanded to whatever the server currently has in it, so a list naming
     * {@code #minecraft:planks} gains any plank a later version of the game adds without the file
     * being touched.
     */
    private Set<Key> blocks(List<String> written) {
        Set<Key> blocks = new LinkedHashSet<>();
        for (String entry : written) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.charAt(0) == TAG_MARKER) {
                Set<Key> tagged = tagged(trimmed.substring(1));
                if (tagged.isEmpty()) {
                    report.accept("No block tag called " + trimmed + ", so it allows nothing");
                }
                blocks.addAll(tagged);
                continue;
            }
            Optional<Key> block = LegacyBlocks.resolve(trimmed);
            if (block.isEmpty()) {
                report.accept("No block called " + trimmed + ", so it allows nothing");
                continue;
            }
            blocks.add(block.get());
        }
        return blocks;
    }

    /** Every block in one of the server's block tags. */
    private Set<Key> tagged(String name) {
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        if (key == null) {
            return Set.of();
        }
        Tag<Material> tag = server.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
        if (tag == null) {
            return Set.of();
        }
        Set<Key> blocks = new LinkedHashSet<>();
        for (Material material : tag.getValues()) {
            blocks.add(material.getKey());
        }
        return blocks;
    }

    /** The names to write for a set of blocks. */
    private static List<String> names(Set<Key> blocks) {
        List<String> written = new ArrayList<>(blocks.size());
        for (Key block : blocks) {
            written.add(block.asString());
        }
        return written;
    }
}
