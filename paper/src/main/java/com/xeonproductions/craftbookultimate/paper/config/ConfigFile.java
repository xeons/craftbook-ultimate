package com.xeonproductions.craftbookultimate.paper.config;

import com.xeonproductions.craftbookultimate.core.config.CartSettings;
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

    private static final String MECHANICS_DISABLED = "mechanics.disabled";
    private static final String MECHANICS_REDSTONE = "mechanics.redstone";
    private static final String GATE_BLOCKS = "mechanics.gate-blocks";
    private static final String GATE_RADIUS = "mechanics.gate-radius";
    private static final String GATE_CLICKING = "mechanics.gate-clicking";
    private static final String LIFT_JUMPING = "mechanics.lift-jumping";
    private static final String LIFT_BUTTONS = "mechanics.lift-buttons";
    private static final String LIFT_TOLERANCE = "mechanics.lift-tolerance";

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

        MechanicSettings mechanics = defaults.mechanics();
        setIfAbsent(yaml, MECHANICS_DISABLED, new ArrayList<>(mechanics.disabled()));
        setIfAbsent(yaml, MECHANICS_REDSTONE, mechanics.redstone());
        setIfAbsent(yaml, GATE_BLOCKS, names(mechanics.gateBlocks()));
        setIfAbsent(yaml, GATE_RADIUS, mechanics.gateRadius());
        setIfAbsent(yaml, GATE_CLICKING, mechanics.gateClicking());
        setIfAbsent(yaml, LIFT_JUMPING, mechanics.liftJumping());
        setIfAbsent(yaml, LIFT_BUTTONS, mechanics.liftButtons());
        setIfAbsent(yaml, LIFT_TOLERANCE, mechanics.liftTolerance());
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

        yaml.setComments(CART_LAUNCH_SPEED, List.of(
                "",
                "How fast a mechanic that launches a cart launches it: a delay letting one go, a",
                "launcher somebody has climbed into, or a dispenser told to push."));

        yaml.setComments(CART_WATER_BUCKETS, List.of(
                "Whether crafting in a cart gives a water bucket back full rather than empty.",
                "Vanilla gives back an empty one; this is a kindness to anybody crafting in bulk."));

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
                yaml.getInt(LIFT_TOLERANCE, defaults.liftTolerance()));
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
                yaml.getBoolean(CART_WATER_BUCKETS, defaults.returnWaterBuckets()));
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
