// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.game;

import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * The four questions SpongeAPI cannot answer, asked of the game itself.
 *
 * <p>SpongeVanilla puts a plugin on the game module layer under Mojang's own names, so its classes
 * can be called directly. That is not the API and does not have the API's promises: a name the game
 * changes is a name that stops working. So every one of these is allowed to answer nothing, every
 * caller has something sensible to do with nothing, and the whole layer stands down permanently the
 * first time the game refuses it — a server that no longer offers these keeps running with the
 * chips that need them doing less, rather than failing to start.
 *
 * <p>What is reached for here is deliberately small and deliberately read-only. Nothing changes how
 * the game behaves; each of these asks it something the API has no way to ask.
 */
@NullMarked
public interface GameInternals {

    /** What breaking a block would actually give, loot tables and all. */
    Optional<Map<Key, Integer>> dropsAt(ServerWorld world, Vec3i position, Key block);

    /** Whether a block would stay where it was put, rather than falling off. */
    Optional<Boolean> canSurvive(ServerWorld world, Vec3i position, Key block);

    /** Feeds bonemeal to whatever is at a position, answering whether it took. */
    Optional<Boolean> applyBonemeal(ServerWorld world, Vec3i position);

    /** Shows one player weather the world is not having. */
    boolean showSky(ServerPlayer player, Sky sky);

    /** What a block spelled the way 1.12 spelled it is called now. */
    Optional<Key> flattenBlock(int legacyId, int damage);

    /** What an item spelled the way 1.12 spelled it is called now. */
    Optional<Key> flattenItem(int legacyId, int damage);

    /** The legacy number a 1.12 name had, which is the only form the flattening map is keyed by. */
    OptionalInt legacyIdFor(String legacyName);

    /** Whether the game is answering at all. */
    boolean isAvailable();

    /**
     * The one in use.
     *
     * <p>Settled once, on first use, by asking the game something with a known answer. A server
     * where that throws gets {@link #NONE} for the rest of its life rather than a layer that throws
     * again on every chip that ticks.
     */
    static GameInternals get() {
        return Holder.INSTANCE;
    }

    /** What is used where the game will not answer: nothing, said plainly. */
    GameInternals NONE = new GameInternals() {

        @Override
        public Optional<Map<Key, Integer>> dropsAt(ServerWorld world, Vec3i position, Key block) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> canSurvive(ServerWorld world, Vec3i position, Key block) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> applyBonemeal(ServerWorld world, Vec3i position) {
            return Optional.empty();
        }

        @Override
        public boolean showSky(ServerPlayer player, Sky sky) {
            return false;
        }

        @Override
        public Optional<Key> flattenBlock(int legacyId, int damage) {
            return Optional.empty();
        }

        @Override
        public Optional<Key> flattenItem(int legacyId, int damage) {
            return Optional.empty();
        }

        @Override
        public OptionalInt legacyIdFor(String legacyName) {
            return OptionalInt.empty();
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    };

    /** Holds the choice, made once and never revisited. */
    final class Holder {

        static final GameInternals INSTANCE = choose();

        private Holder() {}

        private static GameInternals choose() {
            try {
                MinecraftInternals internals = new MinecraftInternals();
                // Asked something whose answer is known, so a game that has moved on is found out
                // here rather than the first time a chip needs it.
                return internals.selfTest() ? internals : NONE;
            } catch (Throwable failed) {
                return NONE;
            }
        }
    }
}
