// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.entity.DyeColours;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Which way of building a pipe this one is.
 *
 * <p>The fork carried two, and both are still built in the world, so both are accepted. They
 * disagree about what two blocks mean — a pane carries items in one and merely keeps them going
 * straight in the other, and a piston starts a pipe in one and ends it in the other — so the
 * meaning cannot be read off a block on its own.
 *
 * <p>It is read off the block the run <em>starts</em> from instead. A sticky piston begins a glass
 * pipe and a piston with an {@code [Extractor]} sign begins a pane one, and whichever it was
 * decides how every block along that run is read. The two use different blocks to start from, so
 * nothing is ambiguous, and a pane can be part of one pipe and a junction in another without the
 * two ever disagreeing about it.
 */
@NullMarked
public enum PipeStyle {

    /**
     * Items run through glass and are handed out by pistons.
     *
     * <p>Glass carries an item to every side but the one it came from, so a run branches wherever
     * it touches more glass. A pane keeps it going straight instead, which is what lets two runs
     * cross without mixing. Stained glass and stained panes only pass to their own colour, so
     * several lines share one bundle.
     *
     * <p>A run ends at a piston, which hands what reaches it to whatever it is pointing at.
     */
    GLASS,

    /**
     * Items run through panes and are taken by whatever container the run touches.
     *
     * <p>Every pane carries in all directions, so the run is a network rather than a line, and it
     * needs no block to mark where it ends: any container it reaches is somewhere items may go.
     * Colour is not read, since a pane is the pipe here rather than a junction in one.
     */
    PANE;

    /** The block a glass pipe starts from. */
    public static final Key GLASS_INPUT = Blocks.key("sticky_piston");

    /** The block a pane pipe starts from, and the one a glass pipe hands items out through. */
    public static final Key PISTON = Blocks.key("piston");

    /** Plain glass, which carries an item onward in every direction. */
    public static final Key PLAIN_GLASS = Blocks.key("glass");

    /** A plain pane, which carries an item straight on through a glass pipe. */
    public static final Key PLAIN_PANE = Blocks.key("glass_pane");

    /** What a stained glass block is called after its colour. */
    private static final String STAINED_GLASS = "_stained_glass";

    /** What a stained pane is called after its colour. */
    private static final String STAINED_PANE = "_stained_glass_pane";

    /**
     * The name on the sign that makes a piston the head of a pane pipe.
     *
     * <p>Without it a piston is only ever somewhere a glass pipe hands items out, which is what
     * keeps the two ways of building a pipe from claiming the same block.
     */
    public static final String EXTRACTOR_SIGN = "[Extractor]";

    /**
     * The name on a sign that says what a way out will take.
     *
     * <p>The same name whichever way the pipe was built, since a filter is a filter.
     */
    public static final String PIPE_SIGN = "[Pipe]";

    /**
     * Whether a block carries items in this style of pipe.
     *
     * <p>Says nothing about which way they go next, which is {@link #onwardFrom}.
     */
    public boolean carries(Key block) {
        return switch (this) {
            case GLASS -> isGlass(block) || isPane(block);
            case PANE -> isPane(block);
        };
    }

    /**
     * Where an item goes next.
     *
     * @param block what it is passing through
     * @param cameFrom the side it arrived by, which it never goes straight back out of
     * @return every side it may leave by, which is empty for a block that carries nothing
     */
    public List<BlockFace> onwardFrom(Key block, BlockFace cameFrom) {
        if (this == GLASS && isPane(block)) {
            // A pane is a crossing rather than a junction: an item goes straight over it, so two
            // runs may share the block without either picking up the other's items.
            return List.of(cameFrom.opposite());
        }
        if (!carries(block)) {
            return List.of();
        }
        return everySideBut(cameFrom);
    }

    /**
     * Whether an item may pass from one block of pipe into the next.
     *
     * <p>Colour is the whole of it, and only a glass pipe reads it: a coloured block passes only to
     * its own colour or to something with no colour of its own, so a red line and a blue line run
     * side by side through the same bundle without mixing.
     */
    public boolean mayPass(Key from, Key into) {
        if (this != GLASS) {
            return true;
        }
        Optional<String> leaving = colourOf(from);
        Optional<String> arriving = colourOf(into);
        if (leaving.isEmpty() || arriving.isEmpty()) {
            return true;
        }
        return leaving.get().equals(arriving.get());
    }

    /** Whether a block is where a run of this kind stops and hands what it is carrying over. */
    public boolean handsOverAt(Key block) {
        return switch (this) {
            case GLASS -> PISTON.equals(block);
            // A pane pipe has no block for it: it hands over wherever it touches a container, which
            // the traversal knows and a block on its own does not.
            case PANE -> false;
        };
    }

    /**
     * Whether a block could be the head of a pipe.
     *
     * <p>Only could: a piston is the head of a pane pipe when it carries an {@code [Extractor]}
     * sign and is a way out of a glass pipe when it does not, and a block on its own cannot say
     * which. This is the cheap check that comes before reading the sign, so that most of the
     * redstone on a server is turned away without touching a block entity.
     */
    public static boolean couldStartAPipe(Key block) {
        return GLASS_INPUT.equals(block) || PISTON.equals(block);
    }

    /** Whether a sign names a piston as the head of a pane pipe. */
    public static boolean marksAnExtractor(SignLines lines) {
        return lines.trimmedText(NAME_LINE).equalsIgnoreCase(EXTRACTOR_SIGN);
    }

    /** Whether a sign says what a way out will take. */
    public static boolean marksAFilter(SignLines lines) {
        return lines.trimmedText(NAME_LINE).equalsIgnoreCase(PIPE_SIGN);
    }

    /** The line a mechanic's name goes on, which is the second, in brackets. */
    public static final int NAME_LINE = 1;

    /** Whether a block is glass, plain or stained. */
    public static boolean isGlass(Key block) {
        return PLAIN_GLASS.equals(block) || named(block, STAINED_GLASS);
    }

    /** Whether a block is a pane, plain or stained. */
    public static boolean isPane(Key block) {
        return PLAIN_PANE.equals(block) || named(block, STAINED_PANE);
    }

    /** What colour a block is, if it has one. */
    public static Optional<String> colourOf(Key block) {
        String value = block.value().toLowerCase(Locale.ROOT);
        for (String suffix : new String[] {STAINED_PANE, STAINED_GLASS}) {
            if (value.endsWith(suffix)) {
                String colour = value.substring(0, value.length() - suffix.length());
                return DyeColours.numberOf(colour).isPresent() ? Optional.of(colour) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Whether a block is a colour followed by the given ending. */
    private static boolean named(Key block, String suffix) {
        String value = block.value().toLowerCase(Locale.ROOT);
        return value.endsWith(suffix)
                && DyeColours.numberOf(value.substring(0, value.length() - suffix.length())).isPresent();
    }

    /** Every side but the one an item arrived by. */
    private static List<BlockFace> everySideBut(BlockFace cameFrom) {
        return SIDES.stream().filter(side -> side != cameFrom).toList();
    }

    /** The six sides a pipe may run in. */
    private static final List<BlockFace> SIDES = List.of(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN);

    /** The six sides a pipe may run in. */
    public static List<BlockFace> sides() {
        return SIDES;
    }
}
