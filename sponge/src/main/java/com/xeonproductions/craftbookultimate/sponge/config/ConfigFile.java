// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.config;

import com.xeonproductions.craftbookultimate.core.config.BlockNames;
import com.xeonproductions.craftbookultimate.core.config.ConfigDocument;
import com.xeonproductions.craftbookultimate.core.config.ConfigTree;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.sponge.adapter.LegacyBlocks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.tag.Tag;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * The settings file on disk, read and written through Configurate.
 *
 * <p>Deliberately the same {@code config.yml}, in the same YAML, saying the same things as the
 * Paper build's — {@link ConfigDocument} holds every name, default and explanation, and neither
 * platform has any of its own. An operator moving a server between the two keeps their settings.
 *
 * <p>What is here is the file itself and the two questions only a server can answer: what a block
 * name means, and what is in a block tag.
 */
@NullMarked
public final class ConfigFile {

    /** What the file is called inside the plugin's own folder. */
    public static final String FILE_NAME = "config.yml";

    /** How a dotted path is cut into the steps Configurate addresses a node by. */
    private static final String PATH_SEPARATOR = "\\.";

    private final Path file;
    private final ConfigDocument document;

    /**
     * @param directory the plugin's own folder
     * @param report where to send a complaint about an entry that could not be understood
     */
    public ConfigFile(Path directory, Consumer<String> report) {
        this.file = directory.resolve(FILE_NAME);
        this.document = new ConfigDocument(new GameBlockNames(), report);
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
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();

        CommentedConfigurationNode root;
        try {
            root = loader.load();
        } catch (ConfigurateException e) {
            throw new IOException("The settings file is not valid YAML", e);
        }

        Settings settings = document.applyTo(new NodeTree(root));

        try {
            loader.save(root);
        } catch (ConfigurateException e) {
            throw new IOException("The settings file could not be written", e);
        }
        return settings;
    }

    /** Configurate's node tree, said the way the document reads a file. */
    private record NodeTree(CommentedConfigurationNode root) implements ConfigTree {

        @Override
        public boolean has(String path) {
            return !at(path).virtual();
        }

        @Override
        public void set(String path, Object value) {
            try {
                at(path).set(value);
            } catch (SerializationException e) {
                // Only ever a default being written back, so the value is one of this codebase's
                // own and cannot be something Configurate has no idea what to do with.
                throw new IllegalStateException("Could not write the default for " + path, e);
            }
        }

        /**
         * Writes what a setting is for.
         *
         * <p>Configurate holds a comment as one string rather than as lines, so they are joined;
         * a comment on the root node is what becomes the file's header.
         */
        @Override
        public void comment(String path, List<String> lines) {
            at(path).comment(String.join("\n", lines));
        }

        @Override
        public void header(List<String> lines) {
            root.comment(String.join("\n", lines));
        }

        @Override
        public boolean bool(String path, boolean fallback) {
            return at(path).getBoolean(fallback);
        }

        @Override
        public String text(String path, String fallback) {
            return at(path).getString(fallback);
        }

        @Override
        public int integer(String path, int fallback) {
            return at(path).getInt(fallback);
        }

        @Override
        public long count(String path, long fallback) {
            return at(path).getLong(fallback);
        }

        @Override
        public double number(String path, double fallback) {
            return at(path).getDouble(fallback);
        }

        @Override
        public List<String> strings(String path) {
            try {
                List<String> written = at(path).getList(String.class);
                return written == null ? List.of() : written;
            } catch (SerializationException e) {
                // A list holding something that is not a string says nothing usable, and refusing
                // to start over it would cost the operator the whole file.
                return List.of();
            }
        }

        @Override
        public Set<String> childrenOf(String path) {
            Set<String> names = new LinkedHashSet<>();
            for (Object name : at(path).childrenMap().keySet()) {
                names.add(String.valueOf(name));
            }
            return names;
        }

        private CommentedConfigurationNode at(String path) {
            return root.node((Object[]) path.split(PATH_SEPARATOR));
        }
    }

    /** What a block name means on a Sponge server. */
    private record GameBlockNames() implements BlockNames {

        @Override
        public Optional<Key> block(String written) {
            return LegacyBlocks.resolve(written);
        }

        @Override
        public Set<Key> tagged(String tag) {
            ResourceKey key;
            try {
                key = ResourceKey.resolve(tag.toLowerCase(Locale.ROOT));
            } catch (InvalidKeyException | IllegalArgumentException e) {
                return Set.of();
            }

            Tag<BlockType> found = Tag.of(RegistryTypes.BLOCK_TYPE, key);
            Set<Key> blocks = new LinkedHashSet<>();
            RegistryTypes.BLOCK_TYPE
                    .get()
                    .taggedValues(found)
                    .forEach(type -> blocks.add(RegistryTypes.BLOCK_TYPE.get().valueKey(type)));
            return blocks;
        }
    }
}
