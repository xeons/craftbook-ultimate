// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.config;

import com.xeonproductions.craftbookultimate.core.config.BlockNames;
import com.xeonproductions.craftbookultimate.core.config.ConfigDocument;
import com.xeonproductions.craftbookultimate.core.config.ConfigTree;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.config.MechanicsDocument;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.paper.ic.LegacyBlocks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * The settings files on disk, read and written through Bukkit's YAML.
 *
 * <p>Two of them: {@code config.yml} for the chips, the carts, the vehicles and the pipes, and
 * {@code mechanics.yml} for the twenty mechanics. They are read and written the same way and end
 * up in one {@link Settings}; what keeps them apart is only that an operator looking for a
 * mechanic should not have to read past everything else to find it.
 *
 * <p>What every setting is called, what it defaults to and what it is for lives in
 * {@link ConfigDocument} and {@link MechanicsDocument}, neither of which knows anything about a
 * server. What is here is the files themselves and the two questions only a server can answer:
 * what a block name means, and what is in a block tag.
 */
@NullMarked
public final class ConfigFile {

    /** What the main file is called inside the plugin's own folder. */
    public static final String FILE_NAME = "config.yml";

    /** What the mechanics file is called beside it. */
    public static final String MECHANICS_FILE_NAME = "mechanics.yml";

    private final Path file;
    private final Path mechanicsFile;
    private final ConfigDocument document;
    private final MechanicsDocument mechanicsDocument;

    /**
     * @param directory the plugin's own folder
     * @param server the server whose block tags an entry may name
     * @param report where to send a complaint about an entry that could not be understood
     */
    public ConfigFile(Path directory, Server server, Consumer<String> report) {
        this(directory, new ServerBlockNames(server), report);
    }

    /**
     * @param directory the plugin's own folder
     * @param names what a written block name means
     * @param report where to send a complaint about an entry that could not be understood
     */
    ConfigFile(Path directory, BlockNames names, Consumer<String> report) {
        this.file = directory.resolve(FILE_NAME);
        this.mechanicsFile = directory.resolve(MECHANICS_FILE_NAME);
        this.document = new ConfigDocument(names, report);
        this.mechanicsDocument = new MechanicsDocument(names, report);
    }

    /** Where the settings are kept. */
    public Path path() {
        return file;
    }

    /** Where what an operator has said about the mechanics is kept. */
    public Path mechanicsPath() {
        return mechanicsFile;
    }

    /**
     * Reads both files, writing back anything either was missing.
     *
     * @throws IOException if a file exists but cannot be read or written
     */
    public Settings load() throws IOException {
        YamlConfiguration yaml = read(file);
        Settings settings = document.applyTo(new YamlTree(yaml));
        write(yaml, file);

        YamlConfiguration mechanicsYaml = read(mechanicsFile);
        MechanicSettings mechanics = mechanicsDocument.applyTo(new YamlTree(mechanicsYaml));
        write(mechanicsYaml, mechanicsFile);

        return settings.toBuilder().mechanics(mechanics).build();
    }

    private static YamlConfiguration read(Path from) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        if (!Files.isRegularFile(from)) {
            return yaml;
        }
        try {
            yaml.load(from.toFile());
        } catch (InvalidConfigurationException e) {
            throw new IOException(from.getFileName() + " is not valid YAML", e);
        }
        return yaml;
    }

    private static void write(YamlConfiguration yaml, Path to) throws IOException {
        Path parent = to.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        yaml.save(to.toFile());
    }

    /** Bukkit's YAML, said the way the document reads a file. */
    private record YamlTree(YamlConfiguration yaml) implements ConfigTree {

        @Override
        public boolean has(String path) {
            return yaml.isSet(path);
        }

        @Override
        public void set(String path, Object value) {
            yaml.set(path, value);
        }

        @Override
        public void comment(String path, List<String> lines) {
            yaml.setComments(path, lines);
        }

        @Override
        public void header(List<String> lines) {
            yaml.options().setHeader(lines);
        }

        @Override
        public boolean bool(String path, boolean fallback) {
            return yaml.getBoolean(path, fallback);
        }

        @Override
        public String text(String path, String fallback) {
            String written = yaml.getString(path, fallback);
            return written == null ? fallback : written;
        }

        @Override
        public int integer(String path, int fallback) {
            return yaml.getInt(path, fallback);
        }

        @Override
        public long count(String path, long fallback) {
            return yaml.getLong(path, fallback);
        }

        @Override
        public double number(String path, double fallback) {
            return yaml.getDouble(path, fallback);
        }

        @Override
        public List<String> strings(String path) {
            return yaml.getStringList(path);
        }

        @Override
        public Set<String> childrenOf(String path) {
            ConfigurationSection section = yaml.getConfigurationSection(path);
            return section == null ? Set.of() : section.getKeys(false);
        }
    }

    /** What a block or item name means on a Bukkit server. */
    private record ServerBlockNames(Server server) implements BlockNames {

        @Override
        public Optional<Key> block(String written) {
            return LegacyBlocks.resolve(written);
        }

        @Override
        public Optional<Key> item(String written) {
            return LegacyBlocks.resolveItem(written);
        }

        @Override
        public Set<Key> tagged(String tag) {
            NamespacedKey key = NamespacedKey.fromString(tag.toLowerCase(Locale.ROOT));
            if (key == null) {
                return Set.of();
            }

            Tag<Material> found = server.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
            if (found == null) {
                return Set.of();
            }

            Set<Key> blocks = new LinkedHashSet<>();
            for (Material material : found.getValues()) {
                blocks.add(material.getKey());
            }
            return blocks;
        }
    }
}
