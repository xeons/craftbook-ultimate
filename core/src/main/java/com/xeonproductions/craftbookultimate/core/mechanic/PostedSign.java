package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/**
 * A mechanic's sign, where it hangs and which way it looks.
 *
 * <p>Everything a mechanic is told is on these four lines, and most of what it does is measured
 * from where they hang. A bridge runs away behind its sign, a door stands to either side of its
 * sign, and both count their width to the left and the right of somebody reading it.
 *
 * @param position where the sign is
 * @param lines what it says
 * @param facing the way the sign looks, which is out of whatever it is fixed to
 */
@NullMarked
public record PostedSign(Vec3i position, SignLines lines, BlockFace facing) {

    /** The line carrying the mechanic's name in brackets. */
    public static final int NAME_LINE = 1;

    /** The line a builder writes {@code ADMIN} on to have the mechanic build out of nothing. */
    public static final int SUPPLY_LINE = 0;

    /** What a builder writes on the first line to have a mechanic supply itself. */
    public static final String ADMIN = "ADMIN";

    /** The name written on the sign, brackets and all. */
    public String name() {
        return lines.trimmedText(NAME_LINE);
    }

    /** A line of the sign, with the surrounding space taken off. */
    public String line(int index) {
        return lines.trimmedText(index);
    }

    /** Whether the sign carries a name, however the builder typed it. */
    public boolean isNamed(String bracketed) {
        return name().equalsIgnoreCase(bracketed);
    }

    /**
     * Whether this mechanic builds out of nothing rather than out of nearby chests.
     *
     * <p>Written by an operator when the mechanic is made, and refused to anybody without the
     * permission for it, so a sign carrying it has already been vouched for.
     */
    public boolean isAdminSupplied() {
        return line(SUPPLY_LINE).toUpperCase(Locale.ROOT).equals(ADMIN);
    }

    /** The way into the block the sign hangs on, which is where a bridge runs. */
    public BlockFace back() {
        return facing.opposite();
    }

    /** The left hand of somebody reading the sign. */
    public BlockFace left() {
        return facing.rotateCounterClockwise();
    }

    /** The right hand of somebody reading the sign. */
    public BlockFace right() {
        return facing.rotateClockwise();
    }
}
