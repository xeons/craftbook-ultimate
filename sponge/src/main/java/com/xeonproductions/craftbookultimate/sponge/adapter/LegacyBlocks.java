// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.adapter;

import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.registry.RegistryTypes;

/**
 * Reading a block written the way a 1.12 sign wrote it.
 *
 * <p>Signs in the worlds this plugin is for name blocks as {@code 35:14} or {@code WOOL:14}, so
 * those spellings have to keep working. The game stopped carrying that mapping when blocks were
 * flattened, and where Bukkit still ships it — its {@code fromLegacy} runs the game's own data
 * fixers over a 1.12 block tag — Sponge exposes nothing equivalent, and the data fixers themselves
 * are server internals rather than API.
 *
 * <p>So the mapping is carried as data: {@code legacy-blocks.properties} in the jar, one line per
 * spelling, written out on a Paper server by {@code /craftbook legacy-table} and read here. That
 * keeps it derived from the game's own answer rather than from anybody's recollection of what
 * {@code 35:14} used to be, which is the one thing that must not be guessed — a wrong entry does
 * not fail, it quietly builds the wrong block.
 *
 * <p>With no table present, modern names still resolve and legacy spellings do not. That is the
 * honest failure: a sign that cannot be read reports as unreadable rather than as something else.
 */
@NullMarked
public final class LegacyBlocks {

    private static final String TABLE = "legacy-blocks.properties";

    private static final Map<String, Key> BLOCKS = new HashMap<>();

    private static final Map<String, Key> ITEMS = new HashMap<>();

    static {
        load();
    }

    private LegacyBlocks() {}

    /** Whether the generated mapping was found, which is what says legacy spellings will resolve. */
    public static boolean hasTable() {
        return !BLOCKS.isEmpty();
    }

    public static Optional<Key> resolve(String written) {
        return resolve(written, BLOCKS, LegacyBlocks::isBlock);
    }

    public static Optional<Key> resolveItem(String written) {
        return resolve(written, ITEMS, LegacyBlocks::isItem);
    }

    private static Optional<Key> resolve(
            String written, Map<String, Key> table, java.util.function.Predicate<Key> exists) {
        Optional<BlockReference> parsed = BlockReference.parse(written);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }

        BlockReference reference = parsed.get();

        if (!reference.isLegacy()) {
            return reference.asKey().filter(exists);
        }

        Key mapped = table.get(entryFor(reference));
        if (mapped != null) {
            return Optional.of(mapped);
        }

        // A modern name carrying a damage value that no longer means anything. The damage is
        // dropped rather than the whole reference being refused.
        return reference.isNumericId()
                ? Optional.empty()
                : reference.asKey().filter(exists);
    }

    private static String entryFor(BlockReference reference) {
        return reference.name().toLowerCase(Locale.ROOT) + ':' + reference.damage();
    }

    private static boolean isBlock(Key key) {
        return RegistryTypes.BLOCK_TYPE.get().findValue(ResourceKey.of(key)).isPresent();
    }

    private static boolean isItem(Key key) {
        return RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.of(key)).isPresent();
    }

    private static void load() {
        try (InputStream source = LegacyBlocks.class.getClassLoader().getResourceAsStream(TABLE)) {
            if (source == null) {
                return;
            }
            read(source);
        } catch (IOException e) {
            // A table that cannot be read is the same situation as no table at all: modern names
            // work and legacy ones do not. Failing to start over it would be worse.
            BLOCKS.clear();
            ITEMS.clear();
        }
    }

    private static void read(InputStream source) throws IOException {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String cleaned = line.trim();
                if (cleaned.isEmpty() || cleaned.charAt(0) == '#') {
                    continue;
                }
                accept(cleaned);
            }
        }
    }

    /** One line of the table: {@code block|35:14|minecraft:red_wool}. */
    private static void accept(String line) {
        String[] parts = line.split("[|]", 3);
        if (parts.length != 3) {
            return;
        }
        try {
            Key key = Key.key(parts[2].trim());
            if ("block".equals(parts[0])) {
                BLOCKS.put(parts[1].trim().toLowerCase(Locale.ROOT), key);
            } else if ("item".equals(parts[0])) {
                ITEMS.put(parts[1].trim().toLowerCase(Locale.ROOT), key);
            }
        } catch (InvalidKeyException e) {
            // A line naming something that is not a key says nothing usable; the rest of the table
            // is still good.
        }
    }
}
