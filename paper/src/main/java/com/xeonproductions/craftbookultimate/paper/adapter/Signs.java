package com.xeonproductions.craftbookultimate.paper.adapter;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.jspecify.annotations.NullMarked;

/**
 * Reads and writes signs in the world.
 *
 * <p>Mechanics are written on the front of a wall sign. Signs have had two writable sides since
 * 1.20, and only the front is consulted here, so text on the back is free for players to use as
 * a label without it being mistaken for configuration.
 */
@NullMarked
public final class Signs {

    private Signs() {}

    /**
     * The sign at a block, if there is one.
     *
     * <p>Reads the block's captured state, so the result is a snapshot rather than a live view.
     */
    public static Optional<Sign> at(Block block) {
        return block.getState(false) instanceof Sign sign ? Optional.of(sign) : Optional.empty();
    }

    /**
     * The direction a wall sign's text faces, which is away from the block it hangs on.
     *
     * <p>Only wall signs carry mechanics. A standing sign has no block behind it to build a chip
     * into, so it is reported as having no facing.
     */
    public static Optional<BlockFace> facing(Block block) {
        if (!(block.getBlockData() instanceof WallSign wallSign)) {
            return Optional.empty();
        }
        return Directions.toDomain(wallSign.getFacing());
    }

    /** The four lines on the front of a sign. */
    public static SignLines read(Sign sign) {
        return SignLines.of(sign.getSide(Side.FRONT).lines());
    }

    /**
     * Replaces one line on the front of a sign and saves it.
     *
     * @param sign the sign state to update
     * @param index the line number, from zero to three
     * @param text the replacement text
     */
    public static void writeLine(Sign sign, int index, String text) {
        writeLine(sign, index, Component.text(text));
    }

    /**
     * Replaces one line on the front of a sign and saves it, keeping whatever formatting the
     * replacement carries.
     *
     * @param sign the sign state to update
     * @param index the line number, from zero to three
     * @param text the replacement text
     */
    public static void writeLine(Sign sign, int index, Component text) {
        SignSide front = sign.getSide(Side.FRONT);
        front.line(index, text);
        sign.update(false, false);
    }

    /** Replaces every line on the front of a sign and saves it. */
    public static void write(Sign sign, SignLines lines) {
        SignSide front = sign.getSide(Side.FRONT);
        for (int i = 0; i < SignLines.LINE_COUNT; i++) {
            front.line(i, lines.line(i));
        }
        sign.update(false, false);
    }
}
