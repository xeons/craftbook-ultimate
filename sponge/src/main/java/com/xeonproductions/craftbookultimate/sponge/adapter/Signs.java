// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.adapter;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.tag.BlockTypeTags;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * Reading and writing the sign a chip is.
 *
 * <p>Only ever the front. {@code Keys.SIGN_LINES} on a sign means its front text, which is the side
 * the whole sign grammar has always been written on; the back is left to whoever wrote it.
 */
@NullMarked
public final class Signs {

    private Signs() {}

    public static Optional<Sign> at(ServerWorld world, Vec3i position) {
        return Positions.toLocation(world, position)
                .blockEntity()
                .filter(Sign.class::isInstance)
                .map(Sign.class::cast);
    }

    /**
     * Which way a wall sign looks.
     *
     * <p>Empty for a standing sign, which is deliberate: a chip's pins are laid out behind and
     * beside the block a wall sign hangs on, and a sign on a post has no such block.
     */
    public static Optional<BlockFace> facing(ServerWorld world, Vec3i position) {
        var state = world.block(position.x(), position.y(), position.z());
        if (!state.type().is(BlockTypeTags.WALL_SIGNS)) {
            return Optional.empty();
        }
        return state.get(Keys.DIRECTION).flatMap(Directions::toDomain);
    }

    public static SignLines read(Sign sign) {
        return SignLines.of(sign.get(Keys.SIGN_LINES).orElse(List.of()));
    }

    public static void writeLine(Sign sign, int index, String text) {
        writeLine(sign, index, Component.text(text));
    }

    public static void writeLine(Sign sign, int index, Component text) {
        List<Component> lines = new ArrayList<>(sign.get(Keys.SIGN_LINES).orElse(List.of()));
        while (lines.size() < SignLines.LINE_COUNT) {
            lines.add(Component.empty());
        }
        if (index < 0 || index >= SignLines.LINE_COUNT) {
            return;
        }
        lines.set(index, text);
        sign.offer(Keys.SIGN_LINES, lines);
    }

    public static void write(Sign sign, SignLines lines) {
        List<Component> written = new ArrayList<>(SignLines.LINE_COUNT);
        for (int i = 0; i < SignLines.LINE_COUNT; i++) {
            written.add(lines.line(i));
        }
        sign.offer(Keys.SIGN_LINES, written);
    }
}
