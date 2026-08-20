package com.xeonproductions.craftbookultimate.paper.config;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.paper.ic.LegacyBlocks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
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
                .build();
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
