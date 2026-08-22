// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.chair;

import com.xeonproductions.craftbookultimate.core.chair.Chairs;
import com.xeonproductions.craftbookultimate.core.config.ChairSettings;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Which blocks are chairs, and which sign belongs to one.
 *
 * <p>The questions that need the world but not an entity: whether a block can be sat on, whether
 * somebody can reach it from where they clicked, and which sign — if any — says what kind of chair
 * it is.
 */
@NullMarked
public final class ChairBlocks {

    /** The four ways a chair may be joined to the next one along a bench. */
    private static final List<BlockFace> ALONGSIDE =
            List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    private ChairBlocks() {
    }

    /**
     * Whether a block can be sat on at all.
     *
     * <p>Four things at once: an operator allows the block, it is not laid upside down, something
     * solid holds it up, and there is room above it for somebody's head. A stair with a block on
     * top of it is a step in a staircase rather than a seat.
     */
    public static boolean isChair(Block block, ChairSettings settings) {
        if (!settings.allows(block.getType().getKey()) || isUpsideDown(block)) {
            return false;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        boolean held = block.getY() <= block.getWorld().getMinHeight() || below.getType().isSolid();
        return held && !block.getRelative(BlockFace.UP).getType().isSolid();
    }

    /**
     * Whether somebody clicking a face of a chair can actually reach the seat.
     *
     * <p>Not from underneath, and not through the back of a stair, which is the solid half. Both
     * would have somebody sitting down through a wall or a ceiling.
     */
    public static boolean reachableFrom(Block block, BlockFace clicked) {
        if (clicked == BlockFace.DOWN) {
            return false;
        }
        return !(block.getBlockData() instanceof Directional directional)
                || directional.getFacing() != clicked;
    }

    /**
     * Which way somebody sitting in a chair should face, or nothing where the block has no front.
     *
     * <p>Out of the stair rather than into it: a stair faces the way its solid half is, so a
     * sitter looks the opposite way.
     */
    public static @Nullable Float facingOf(Block block) {
        if (!(block.getBlockData() instanceof Directional directional)) {
            return null;
        }
        return switch (directional.getFacing().getOppositeFace()) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> null;
        };
    }

    /**
     * The sign that says what a chair is, if one is within reach of it.
     *
     * <p>The search follows a run of touching blocks of the same kind, so a bench of eight stairs
     * shares whatever sign is hung at either end of it. It stops at the distance an operator
     * allows, measured from the block that was actually clicked.
     */
    public static Optional<SignLines> signFor(Block chair, ChairSettings settings) {
        Set<Block> seen = new HashSet<>();
        Deque<Block> along = new ArrayDeque<>();
        seen.add(chair);
        along.add(chair);

        while (!along.isEmpty()) {
            Block here = along.removeFirst();
            for (BlockFace face : ALONGSIDE) {
                Block beside = here.getRelative(face);
                if (!seen.add(beside) || tooFar(chair, beside, settings)) {
                    continue;
                }
                Optional<Sign> sign = Signs.at(beside);
                if (sign.isPresent() && hangsOnTheChair(beside, face)) {
                    return sign.map(Signs::read);
                }
                if (beside.getType() == here.getType()) {
                    along.addLast(beside);
                }
            }
        }
        return Optional.empty();
    }

    /** Whether a chair's sign is the one that makes it heal. */
    public static boolean healing(SignLines lines) {
        return Chairs.isHealSign(lines.trimmedText(1));
    }

    private static boolean tooFar(Block chair, Block beside, ChairSettings settings) {
        return chair.getLocation().distanceSquared(beside.getLocation())
                > (double) settings.maxSignDistance() * settings.maxSignDistance();
    }

    /**
     * Whether a sign found one step away is actually hung on the block it was found from.
     *
     * <p>A wall sign's text faces away from what holds it up, so a sign facing the same way the
     * search stepped is one hung on the block behind it. The other three signs that could stand
     * beside a chair belong to something else entirely.
     */
    private static boolean hangsOnTheChair(Block sign, BlockFace stepped) {
        return Signs.facing(sign)
                .map(Directions::toServer)
                .filter(facing -> facing == stepped)
                .isPresent();
    }

    /** Whether a block is the upper half of something, which is never a seat. */
    private static boolean isUpsideDown(Block block) {
        return block.getBlockData() instanceof Bisected bisected
                && bisected.getHalf() == Bisected.Half.TOP;
    }
}
