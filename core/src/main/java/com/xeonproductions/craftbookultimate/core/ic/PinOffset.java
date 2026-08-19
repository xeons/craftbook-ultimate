package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import org.jspecify.annotations.NullMarked;

/**
 * The position of a single IC pin, expressed relative to the sign rather than to the world.
 *
 * <p>Offsets are given in the sign's own frame of reference, which keeps a layout definition
 * readable and independent of which way the chip was built:
 *
 * <ul>
 *   <li>{@code forward} runs along the side of the sign carrying the text, so positive values
 *       are in front of the sign and negative values pass through the block it is mounted on;
 *   <li>{@code right} runs clockwise from {@code forward} when viewed from above, so it is to
 *       the right of a player reading the sign;
 *   <li>{@code up} is the vertical axis.
 * </ul>
 *
 * @param forward blocks in front of the sign; negative reaches behind it
 * @param right blocks to the reader's right; negative is to their left
 * @param up blocks above the sign; negative is below it
 */
@NullMarked
public record PinOffset(int forward, int right, int up) {

    /**
     * Resolves this offset against a concrete sign orientation.
     *
     * @param front the direction the sign's text faces; must be a cardinal direction
     * @return the offset from the sign's own block, in world axes
     * @throws IllegalArgumentException if {@code front} is not a cardinal direction
     */
    public Vec3i resolve(BlockFace front) {
        if (!front.isCardinal()) {
            throw new IllegalArgumentException("An IC sign must face a cardinal direction, got " + front);
        }
        BlockFace rightFace = front.rotateClockwise();
        return new Vec3i(
                front.deltaX() * forward + rightFace.deltaX() * right,
                up,
                front.deltaZ() * forward + rightFace.deltaZ() * right);
    }
}
