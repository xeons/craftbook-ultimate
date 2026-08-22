// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves blocks named the way signs named them before the 1.13 flattening.
 *
 * <p>Worlds carried over from that era are full of signs reading {@code 35:14} rather than
 * {@code red_wool}. The server still ships the tables Mojang published when block states were
 * flattened, so the old pairs can be translated exactly rather than guessed at.
 *
 * <p>Numeric ids are matched against the legacy materials the server still knows about, and the
 * pair is then handed to the server's own conversion. A name with a damage value is looked up the
 * same way, so {@code wool:14} works as well as {@code 35:14}.
 *
 * <p>Items went through the same flattening and are resolved the same way, which is what lets a
 * farming chip on an old sign still name cocoa beans as {@code 351@3}.
 */
@NullMarked
public final class LegacyBlocks {

    /**
     * The two indexes, built the first time an old spelling is actually read.
     *
     * <p>Held behind a class of their own so that they are built on first use rather than when
     * {@link LegacyBlocks} is first touched. Nearly every name a server reads is a modern one and
     * never needs them, and building them walks every material the game has twice.
     */
    private static final class Index {

        /** Legacy block materials by the numeric id they used to have. */
        static final Map<Integer, Material> BLOCKS = indexLegacyMaterials(true);

        /** Legacy item materials by the numeric id they used to have. */
        static final Map<Integer, Material> ITEMS = indexLegacyMaterials(false);

        private Index() {}
    }

    private LegacyBlocks() {}

    /**
     * Works out which block a sign means, understanding both spellings.
     *
     * <p>A modern name is tried first, so nothing about the old format can shadow a block that
     * exists today.
     *
     * @param written the text as it appears on the sign
     * @return the block, or empty if the text names nothing that exists
     */
    public static Optional<Key> resolve(String written) {
        Optional<BlockReference> parsed = BlockReference.parse(written);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }

        BlockReference reference = parsed.get();

        if (!reference.isLegacy()) {
            return reference.asKey().filter(LegacyBlocks::isBlock);
        }

        return legacyMaterial(reference)
                .map(legacy -> fromLegacy(legacy, reference.damage(), false))
                .filter(material -> material != null && material.isBlock())
                .map(Material::getKey);
    }

    /**
     * Works out which item a sign means, understanding both spellings.
     *
     * <p>Ids were shared between blocks and items before the flattening, so which half of a pair
     * an id meant depended on where it was written. A farming chip names an item, so the item half
     * is what is looked up here.
     *
     * @param written the text as it appears on the sign
     * @return the item, or empty if the text names nothing that exists
     */
    public static Optional<Key> resolveItem(String written) {
        Optional<BlockReference> parsed = BlockReference.parse(written);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }

        BlockReference reference = parsed.get();

        if (!reference.isLegacy()) {
            return reference.asKey().filter(LegacyBlocks::isItem);
        }

        return legacyItem(reference)
                .map(legacy -> fromLegacy(legacy, reference.damage(), true))
                .filter(material -> material != null && material.isItem())
                .map(Material::getKey);
    }

    /** The legacy item a reference names, by id or by its old name. */
    private static Optional<Material> legacyItem(BlockReference reference) {
        Optional<Integer> id = reference.id();
        if (id.isPresent()) {
            return Optional.ofNullable(Index.ITEMS.get(id.get()));
        }

        Material legacy = Material.getMaterial(reference.name().toUpperCase(Locale.ROOT), true);
        if (legacy != null) {
            return Optional.of(legacy);
        }
        return Optional.ofNullable(Material.getMaterial(reference.name().toUpperCase(Locale.ROOT)));
    }

    /** Whether a key names an item that exists on this server. */
    private static boolean isItem(Key key) {
        Material material = org.bukkit.Registry.MATERIAL.get(key);
        return material != null && material.isItem();
    }

    /** Whether a key names a block that exists on this server. */
    private static boolean isBlock(Key key) {
        Material material = org.bukkit.Registry.MATERIAL.get(key);
        return material != null && material.isBlock();
    }

    /** The legacy material a reference names, by id or by its old name. */
    private static Optional<Material> legacyMaterial(BlockReference reference) {
        Optional<Integer> id = reference.id();
        if (id.isPresent()) {
            return Optional.ofNullable(Index.BLOCKS.get(id.get()));
        }

        // A name with a damage value is a pre-flattening name, so it is looked up among the
        // legacy materials rather than the current ones.
        Material legacy = Material.getMaterial(reference.name().toUpperCase(Locale.ROOT), true);
        if (legacy != null) {
            return Optional.of(legacy);
        }

        // Some signs carry a modern name with a damage value that no longer means anything. The
        // damage is dropped rather than the whole reference being rejected.
        return Optional.ofNullable(Material.getMaterial(reference.name().toUpperCase(Locale.ROOT)));
    }

    /**
     * Converts a legacy material and damage value into the block it became.
     *
     * <p>Uses the server's own flattening tables, which is the only place the full mapping lives.
     * The types involved are deprecated for removal, since nothing written today should be
     * producing legacy pairs, but reading signs written years ago is exactly what they are for.
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static @Nullable Material fromLegacy(Material legacy, int damage, boolean asItem) {
        if (!legacy.isLegacy()) {
            return legacy;
        }
        return Bukkit.getUnsafe().fromLegacy(new MaterialData(legacy, (byte) damage), asItem);
    }

    /**
     * Indexes the legacy materials by their old numeric id.
     *
     * <p>Ids were reused between blocks and items, so each half is indexed separately and which
     * one a sign means depends on what the chip reading it is after.
     *
     * @param blocksOnly true to index the blocks, false to index the items
     */
    private static Map<Integer, Material> indexLegacyMaterials(boolean blocksOnly) {
        Map<Integer, Material> byId = new HashMap<>();
        for (Material material : Material.values()) {
            if (!material.isLegacy() || (blocksOnly ? !material.isBlock() : !material.isItem())) {
                continue;
            }
            byId.putIfAbsent(material.getId(), material);
        }
        return Map.copyOf(byId);
    }
}
