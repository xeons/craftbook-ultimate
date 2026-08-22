// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.area.AreaVault;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The world as a sign mechanic sees it.
 *
 * <p>Separate from the seams the chips and the cart mechanics use, because these mechanics want
 * different things again: signs a few blocks away in a straight line, whole boxes of blocks put
 * up and taken down, and the chests to pay for them out of.
 *
 * <p>Everything here is about the mechanic's own surroundings — the far end of a bridge, the
 * floor a lift reaches — so nothing reaches across a regionised server into somewhere another
 * thread owns. The one exception is the lift, which asks the server to carry a player rather
 * than putting them down itself.
 */
@NullMarked
public interface MechanicWorld {

    /** What identifies this world to the rest of the server. */
    UUID id();

    /** What block is at a position. Unloaded or out-of-bounds positions read as air. */
    Key blockAt(Vec3i position);

    /**
     * Replaces the block at a position.
     *
     * @return true if the world changed
     */
    boolean setBlockAt(Vec3i position, Key block);

    /** The sign at a position, or nothing if there is no sign there. */
    Optional<PostedSign> signAt(Vec3i position);

    /**
     * Replaces what a sign says.
     *
     * <p>A toggled area is the one mechanic that keeps anything between one use and the next, and
     * what it keeps is which of its two halves is standing. That goes on its own sign, in the
     * dashes a builder can see, rather than anywhere a builder cannot.
     *
     * @return true if there was a sign there and it now says this
     */
    boolean writeSign(Vec3i position, SignLines lines);

    /**
     * Where the saved areas are kept.
     *
     * <p>Server-wide rather than a property of the world, and reached from here because a
     * mechanic reaches everything through this seam. A world with no store behind it holds no
     * areas rather than refusing to answer.
     */
    default AreaVault vault() {
        return AreaVault.empty();
    }

    /** Whether the chunk holding a position is loaded and safe to read. */
    boolean isLoaded(Vec3i position);

    /**
     * Whether something could stand in the block at a position without being inside it.
     *
     * <p>Broader than being air: water, tall grass and an open door all leave room, and a lift
     * looking for somewhere to put a player wants any of them.
     */
    boolean isPassable(Vec3i position);

    /**
     * The materials a mechanic at a position can draw on and give back to.
     *
     * <p>The chests near it, taken together as one.
     */
    Stockpile stockpileAround(Vec3i position);

    /**
     * Throws a lever or presses a button at a position.
     *
     * <p>Its own method rather than writing a block, because what a switch is doing is part of
     * that block's state rather than its name, and a hidden switch has to leave a lever thrown
     * where it found it thrown. A button is pressed and springs back on its own, as it would
     * under a hand.
     *
     * @return true if there was a switch there to work
     */
    default boolean workSwitchAt(Vec3i position) {
        return false;
    }

    /** The lowest y coordinate that can hold a block. */
    int minHeight();

    /** One past the highest y coordinate that can hold a block. */
    int maxHeight();

    /** Takes the block at a position away. */
    default boolean clearAt(Vec3i position) {
        return setBlockAt(position, Blocks.AIR_KEY);
    }

    /** Whether the block at a position is air. */
    default boolean isAir(Vec3i position) {
        return Blocks.AIR.contains(blockAt(position));
    }

    /** Whether a position is within the world's floor and ceiling. */
    default boolean isInBounds(Vec3i position) {
        return position.y() >= minHeight() && position.y() < maxHeight();
    }

    /**
     * Works out which block a sign means.
     *
     * <p>Signs written before the 1.13 flattening name blocks by a numeric id and damage value,
     * and only the server holds the tables that map those onto modern blocks. A world backed by
     * a real server resolves them; this default understands modern names only.
     */
    default Optional<Key> resolveBlock(String written) {
        return BlockReference.parse(written).flatMap(BlockReference::asKey);
    }

    /**
     * Works out which item a sign means.
     *
     * <p>Separate from {@link #resolveBlock} because an id named either one before the flattening,
     * and because what a hidden switch takes as a key is usually not a block at all.
     */
    default Optional<Key> resolveItem(String written) {
        return resolveBlock(written);
    }

    /**
     * The next sign along a direction that a mechanic recognises.
     *
     * <p>How both ends of a bridge and both ends of a door find each other: the far sign is
     * looked for in a straight line, and anything else in the way is simply passed over.
     *
     * @param from where to start, which is not itself considered
     * @param direction which way to look
     * @param distance how many blocks to look along
     * @param wanted what makes a sign the one being looked for
     */
    default Optional<PostedSign> nextSign(
            Vec3i from, BlockFace direction, int distance, Predicate<PostedSign> wanted) {
        Vec3i at = from;
        for (int step = 0; step < distance; step++) {
            at = at.offset(direction);
            if (!isInBounds(at) || !isLoaded(at)) {
                return Optional.empty();
            }
            Optional<PostedSign> sign = signAt(at).filter(wanted);
            if (sign.isPresent()) {
                return sign;
            }
        }
        return Optional.empty();
    }
}
