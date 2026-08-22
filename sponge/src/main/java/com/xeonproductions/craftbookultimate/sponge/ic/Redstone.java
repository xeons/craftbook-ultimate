// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.tag.BlockTypeTags;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * Reading how much power a block is carrying.
 *
 * <p>Sponge describes a block's state through keys rather than through a type per shape of data,
 * so where the Paper binding asks whether a block's data is {@code Powerable}, this asks whether
 * the state carries {@code IS_POWERED}. The order matters and is the same in both: a level before
 * a flag, because a comparator carries both and its level is the answer.
 */
@NullMarked
public final class Redstone {

    public static final int FULL_POWER = 15;

    /**
     * The blocks that put power out rather than merely conduct it.
     *
     * <p>Named individually because the game has no tag for "is a power source" — the buttons and
     * the pressure plates do have tags, and are asked for by tag so a block added to either later
     * is included without this list being remembered.
     */
    private static final Set<BlockType> POWER_SOURCES = Set.of(
            BlockTypes.REDSTONE_WIRE.get(),
            BlockTypes.REDSTONE_BLOCK.get(),
            BlockTypes.REDSTONE_TORCH.get(),
            BlockTypes.REDSTONE_WALL_TORCH.get(),
            BlockTypes.REPEATER.get(),
            BlockTypes.COMPARATOR.get(),
            BlockTypes.OBSERVER.get(),
            BlockTypes.LEVER.get(),
            BlockTypes.DAYLIGHT_DETECTOR.get(),
            BlockTypes.TRIPWIRE_HOOK.get(),
            BlockTypes.TARGET.get(),
            BlockTypes.SCULK_SENSOR.get(),
            BlockTypes.CALIBRATED_SCULK_SENSOR.get(),
            BlockTypes.LIGHTNING_ROD.get());

    private Redstone() {}

    public static boolean isPowerSource(BlockState state) {
        BlockType type = state.type();
        return POWER_SOURCES.contains(type)
                || type.is(BlockTypeTags.BUTTONS)
                || type.is(BlockTypeTags.PRESSURE_PLATES);
    }

    public static boolean isAlwaysOn(BlockState state) {
        return state.type().equals(BlockTypes.REDSTONE_BLOCK.get());
    }

    public static int powerLevel(BlockState state) {
        // A level before a flag: a comparator carries both, and what it is putting out is the
        // level rather than whether it is on at all.
        Integer level = state.get(Keys.POWER).orElse(null);
        if (level != null) {
            return level;
        }
        if (state.get(Keys.IS_POWERED).orElse(false)) {
            return FULL_POWER;
        }
        if (isAlwaysOn(state)) {
            return FULL_POWER;
        }
        // A redstone torch reports its emission through whether it is lit, and is on when lit.
        if (state.get(Keys.IS_LIT).orElse(false)) {
            return FULL_POWER;
        }
        return 0;
    }

    public static boolean isPowered(BlockState state) {
        return powerLevel(state) > 0;
    }

    /**
     * Whether a place is carrying power, from anything at all.
     *
     * <p>Asks the block itself first and then whether the game is feeding it from somewhere else,
     * which together are what Bukkit's block power reports as one number.
     */
    public static boolean isPowered(ServerWorld world, Vec3i position) {
        BlockState state = world.block(position.x(), position.y(), position.z());
        return isPowered(state) || state.get(Keys.IS_INDIRECTLY_POWERED).orElse(false);
    }
}
