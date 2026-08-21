package com.xeonproductions.craftbookultimate.core.sign;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import org.jspecify.annotations.NullMarked;

/**
 * Which block a wall sign hangs on.
 *
 * <p>One rule, written once. A wall sign faces away from what holds it up, so the block holding it
 * is one step against its facing. That single sentence decides where a chip acts from, where its
 * pins are measured, and which signs come down when a block is broken — and it had been written out
 * separately in each of those places.
 *
 * <p>Getting it backwards is the kind of mistake that looks right: every position is still one step
 * from the sign, just the wrong one, so a chip works from the air in front of itself and breaking a
 * block takes down the sign on the far side of it.
 */
@NullMarked
public final class SignSupport {

    private SignSupport() {}

    /**
     * The block a wall sign hangs on.
     *
     * @param sign where the sign itself is
     * @param facing the way its text faces, which is away from what holds it
     */
    public static Vec3i of(Vec3i sign, BlockFace facing) {
        return sign.offset(facing.opposite());
    }

    /**
     * Whether a wall sign hangs on a particular block.
     *
     * <p>A sign standing one step from a block does not necessarily hang on it: three of the four
     * signs that could surround a block face away from it and hang on something else entirely.
     *
     * @param sign where the sign itself is
     * @param facing the way its text faces
     * @param block the block in question
     */
    public static boolean hangsOn(Vec3i sign, BlockFace facing, Vec3i block) {
        return of(sign, facing).equals(block);
    }
}
