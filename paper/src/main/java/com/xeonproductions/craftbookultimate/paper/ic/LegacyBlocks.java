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
 */
@NullMarked
public final class LegacyBlocks {

    /** Legacy materials by the numeric id they used to have. */
    private static final Map<Integer, Material> BY_LEGACY_ID = indexLegacyMaterials();

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
                .map(legacy -> fromLegacy(legacy, reference.damage()))
                .filter(material -> material != null && material.isBlock())
                .map(Material::getKey);
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
            return Optional.ofNullable(BY_LEGACY_ID.get(id.get()));
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
    private static @Nullable Material fromLegacy(Material legacy, int damage) {
        if (!legacy.isLegacy()) {
            return legacy;
        }
        return Bukkit.getUnsafe().fromLegacy(new MaterialData(legacy, (byte) damage));
    }

    /**
     * Indexes the legacy materials by their old numeric id.
     *
     * <p>Ids were reused between blocks and items, so only the block half is kept: a sign naming
     * a block by id means the block.
     */
    private static Map<Integer, Material> indexLegacyMaterials() {
        Map<Integer, Material> byId = new HashMap<>();
        for (Material material : Material.values()) {
            if (!material.isLegacy() || !material.isBlock()) {
                continue;
            }
            byId.putIfAbsent(material.getId(), material);
        }
        return Map.copyOf(byId);
    }
}
