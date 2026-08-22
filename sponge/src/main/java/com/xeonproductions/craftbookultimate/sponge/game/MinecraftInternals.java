// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.game;

import com.mojang.serialization.Dynamic;
import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * The game itself, asked directly.
 *
 * <p>Sponge builds its API into the game's own classes rather than wrapping them, so a Sponge world
 * <em>is</em> a {@code ServerLevel} and a Sponge block state <em>is</em> a Minecraft one. Crossing
 * between the two is a cast, and none of what follows copies or converts anything.
 *
 * <p>Every method here is one call into a name the game chose, so every one of them is a name that
 * could change. {@link GameInternals} is where that is dealt with: this class is allowed to be
 * brittle because the thing holding it is not.
 */
@NullMarked
final class MinecraftInternals implements GameInternals {

    /**
     * How the flattening map keys a block: its number and its damage in one.
     *
     * <p>Four bits of damage, which is all a 1.12 block ever had, and is why a sign may write
     * {@code 35:14} but never {@code 35:16}.
     */
    private static final int DAMAGE_BITS = 4;

    private static final int DAMAGE_MASK = 0xF;

    /** What a 1.12 name is called in the map: the numbers are the only key it has. */
    private static final Map<String, Integer> LEGACY_IDS = legacyIds();

    /** Full rain, for a sky sent to one person rather than made real. */
    private static final float FULL = 1.0f;

    private static final float NONE = 0.0f;

    @Override
    public Optional<Map<Key, Integer>> dropsAt(ServerWorld world, Vec3i position, Key block) {
        ServerLevel level = (ServerLevel) world;
        BlockPos pos = new BlockPos(position.x(), position.y(), position.z());
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);

        // Read through the game's own registry rather than by casting the stack to Sponge's own
        // interface. Sponge only soft-implements that one on a stack, and a drop is not worth
        // resting on the one crossing here that is not a plain mixin.
        Map<Key, Integer> totals = new HashMap<>();
        for (net.minecraft.world.item.ItemStack stack :
                Block.getDrops(state, level, pos, level.getBlockEntity(pos))) {
            asKey(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                    .ifPresent(item -> totals.merge(item, stack.getCount(), Integer::sum));
        }
        return Optional.of(Map.copyOf(totals));
    }

    @Override
    public Optional<Boolean> canSurvive(ServerWorld world, Vec3i position, Key block) {
        Optional<BlockType> type = RegistryTypes.BLOCK_TYPE.get().findValue(ResourceKey.of(block));
        if (type.isEmpty()) {
            return Optional.of(false);
        }

        BlockState placing = type.get().defaultState();
        ServerLevel level = (ServerLevel) world;
        return Optional.of(((net.minecraft.world.level.block.state.BlockState) placing)
                .canSurvive(level, new BlockPos(position.x(), position.y(), position.z())));
    }

    /**
     * Shows one player weather of their own.
     *
     * <p>Two things are sent rather than one: whether it is raining, and how hard. A client told
     * only that it has started raining draws nothing until the level arrives, so the pair is what
     * makes it visible. Giving somebody the real sky back sends the world's own answer, which is
     * the only way to undo a lie the server does not remember telling.
     */
    @Override
    public boolean showSky(ServerPlayer player, Sky sky) {
        net.minecraft.server.level.ServerPlayer target =
                (net.minecraft.server.level.ServerPlayer) player;

        boolean raining = switch (sky) {
            case DOWNFALL -> true;
            case CLEAR -> false;
            case REAL -> ((ServerLevel) player.world()).isRaining();
        };

        target.connection.send(new ClientboundGameEventPacket(
                raining ? ClientboundGameEventPacket.START_RAINING
                        : ClientboundGameEventPacket.STOP_RAINING,
                NONE));
        target.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, raining ? FULL : NONE));
        return true;
    }

    /**
     * What a block written the way 1.12 wrote it is called now.
     *
     * <p>The game carries the whole flattening map, because it has to read worlds that old. Asking
     * it is the difference between an answer that is right by construction and a table somebody
     * typed out — and a wrong entry in such a table does not fail, it quietly builds the wrong
     * block.
     */
    @Override
    public Optional<Key> flattenBlock(int legacyId, int damage) {
        Dynamic<?> tag = BlockStateData.getTag(packed(legacyId, damage));
        String name = tag.get("Name").asString("");
        return name.isEmpty() ? Optional.empty() : asKey(name);
    }

    @Override
    public Optional<Key> flattenItem(int legacyId, int damage) {
        String legacyName = ItemIdFix.getItem(legacyId);
        if (legacyName == null) {
            return Optional.empty();
        }
        // Answers null where the flattening left a name alone, which is most of them.
        String flattened = ItemStackTheFlatteningFix.updateItem(legacyName, damage);
        return asKey(flattened == null ? legacyName : flattened);
    }

    @Override
    public OptionalInt legacyIdFor(String legacyName) {
        Integer id = LEGACY_IDS.get(normalise(legacyName));
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Whether the game still answers the way this expects.
     *
     * <p>Wool became coloured wool in the flattening and {@code 35:14} has been red wool ever
     * since, so an answer of anything else means the map moved and none of the rest should be
     * trusted either.
     */
    boolean selfTest() {
        return flattenBlock(35, 14)
                .filter(key -> key.value().equals("red_wool"))
                .isPresent();
    }

    private static int packed(int legacyId, int damage) {
        return legacyId << DAMAGE_BITS | (damage & DAMAGE_MASK);
    }

    private static Optional<Key> asKey(String name) {
        try {
            return Optional.of(Key.key(name));
        } catch (InvalidKeyException e) {
            return Optional.empty();
        }
    }

    /**
     * Every 1.12 name against the number it had.
     *
     * <p>Built by asking the game what each of the 256 block numbers was called, so a sign naming
     * {@code WOOL:14} rather than {@code 35:14} resolves through the same map and cannot disagree
     * with it.
     */
    private static Map<String, Integer> legacyIds() {
        Map<String, Integer> byName = new HashMap<>();
        for (int id = 0; id < 256; id++) {
            String name = BlockStateData.getTag(packed(id, 0)).get("Name").asString("");
            if (!name.isEmpty()) {
                byName.putIfAbsent(normalise(name), id);
            }
            String item = ItemIdFix.getItem(id);
            if (item != null) {
                byName.putIfAbsent(normalise(item), id);
            }
        }
        return Map.copyOf(byName);
    }

    /** A name as a sign might carry it, with the namespace and the spacing taken off. */
    private static String normalise(String name) {
        String cleaned = name.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        int separator = cleaned.indexOf(':');
        return separator < 0 ? cleaned : cleaned.substring(separator + 1);
    }
}
