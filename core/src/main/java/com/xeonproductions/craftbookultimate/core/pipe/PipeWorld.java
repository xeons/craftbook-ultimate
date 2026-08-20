package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The world as a pipe sees it.
 *
 * <p>Small on purpose: tracing a pipe is reading blocks and following them, and everything else —
 * pulling a stack out of a chest, offering it to another, dropping what nobody will take — is the
 * server's work and stays on the other side of this seam. That is what lets a whole network's
 * shape be worked out against a world built in a test.
 *
 * <p>Nothing here reaches beyond the run itself. A pipe is a line of touching blocks, so all of it
 * belongs to one region however long it grows.
 */
@NullMarked
public interface PipeWorld {

    /** What block is at a position. */
    Key blockAt(Vec3i position);

    /** Whether a position is loaded and can be read. */
    boolean isLoaded(Vec3i position);

    /**
     * Which way a piston is pointing.
     *
     * <p>Both ends of a glass pipe are pistons, and both care: the sticky one at the start points
     * at whatever it pulls from, and the plain ones along the way point at whatever they fill.
     */
    Optional<BlockFace> facingAt(Vec3i position);

    /**
     * Whether something at a position will hold items.
     *
     * <p>Any container at all, since a pipe is asked to fill chests, barrels, furnaces, hoppers and
     * whatever else a version of the game adds without this having to name them.
     */
    boolean holdsItemsAt(Vec3i position);

    /**
     * The sign fixed to a block, if it carries one.
     *
     * <p>What a pipe reads its filter off. A sign hung on a piston or on a container belongs to
     * that block, so this asks the block rather than the sign's own position.
     */
    Optional<SignLines> signOn(Vec3i position);

    /**
     * Reads an item's name, the modern way or the way it was written before the flattening.
     *
     * <p>Signs from before the flattening name items as a number and a damage value, and those
     * signs are still in the ground, so both spellings resolve.
     */
    default Optional<Key> resolveItem(String written) {
        return BlockReference.parse(written).flatMap(BlockReference::asKey);
    }

    /** Whether a position holds nothing at all. */
    default boolean isEmpty(Vec3i position) {
        return Blocks.AIR.contains(blockAt(position));
    }
}
