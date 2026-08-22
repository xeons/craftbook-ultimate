// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.adapter;

import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import com.xeonproductions.craftbookultimate.sponge.game.GameInternals;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.registry.RegistryTypes;

/**
 * Reading a block written the way a 1.12 sign wrote it.
 *
 * <p>Signs in the worlds this plugin is for name blocks as {@code 35:14} or {@code WOOL:14}, so
 * those spellings have to keep working. The game still knows what they became — it has to, to read
 * worlds that old — and {@link GameInternals} asks it. That answer is right by construction, where
 * a table somebody typed out would not fail when it was wrong; it would quietly build the wrong
 * block.
 *
 * <p>There is no second source and deliberately so. Where the game will not answer, modern names
 * still resolve and legacy spellings do not, which reports as a sign that cannot be read rather
 * than as something else.
 */
@NullMarked
public final class LegacyBlocks {

    private LegacyBlocks() {}

    /** Whether legacy spellings will resolve at all. */
    public static boolean readsLegacySpellings() {
        return GameInternals.get().isAvailable();
    }

    public static Optional<Key> resolve(String written) {
        return resolve(written, LegacyBlocks::isBlock, true);
    }

    public static Optional<Key> resolveItem(String written) {
        return resolve(written, LegacyBlocks::isItem, false);
    }

    private static Optional<Key> resolve(String written, Predicate<Key> exists, boolean asBlock) {
        Optional<BlockReference> parsed = BlockReference.parse(written);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }

        BlockReference reference = parsed.get();

        if (!reference.isLegacy()) {
            return reference.asKey().filter(exists);
        }

        Optional<Key> flattened = fromGame(reference, asBlock).filter(exists);
        if (flattened.isPresent()) {
            return flattened;
        }

        // A modern name carrying a damage value that no longer means anything. The damage is
        // dropped rather than the whole reference being refused. A bare number has no such
        // fallback: without the flattening map it names nothing at all.
        return reference.isNumericId() ? Optional.empty() : reference.asKey().filter(exists);
    }

    /** What the game says a legacy spelling became, where it is answering. */
    private static Optional<Key> fromGame(BlockReference reference, boolean asBlock) {
        GameInternals game = GameInternals.get();
        if (!game.isAvailable()) {
            return Optional.empty();
        }

        // The flattening map is keyed by the number, so a name has to become one first.
        Optional<Integer> numeric = reference.id();
        OptionalInt id = numeric.isPresent()
                ? OptionalInt.of(numeric.get())
                : game.legacyIdFor(reference.name());
        if (id.isEmpty()) {
            return Optional.empty();
        }

        return asBlock
                ? game.flattenBlock(id.getAsInt(), reference.damage())
                : game.flattenItem(id.getAsInt(), reference.damage());
    }

    private static boolean isBlock(Key key) {
        return RegistryTypes.BLOCK_TYPE.get().findValue(ResourceKey.of(key)).isPresent();
    }

    private static boolean isItem(Key key) {
        return RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.of(key)).isPresent();
    }
}
