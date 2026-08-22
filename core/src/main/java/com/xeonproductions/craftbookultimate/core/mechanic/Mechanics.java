// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/**
 * Every mechanic there is, by the name an operator switches it off by.
 *
 * <p>One list rather than a name declared beside each mechanic, because the name is asked for in
 * three places that have to agree: the settings file writes a section per mechanic, the settings
 * themselves record which are switched off, and each mechanic asks whether it may run. A name
 * spelt differently in any of those reads exactly like a mechanic that has been turned off.
 *
 * <p>Names have nothing to do with sign names. A builder writes {@code [BannerCopier]} on a sign
 * and an operator writes {@code BannerCopier} in the file, and the brackets belong only to the
 * first of those.
 *
 * <p>This is not what a mechanic <em>is</em> — nothing here knows how to build a bridge. It is only
 * what each is called, which is why it can sit below every one of them and be imported by all.
 */
@NullMarked
public final class Mechanics {

    /** Reads a block's redstone power off an item held up to it. */
    public static final String AMMETER = "Ammeter";

    /** Swaps a whole saved region in and out. */
    public static final String AREA = "Area";

    /** Hands out copies of a banner. */
    public static final String BANNER_COPIER = "BannerCopier";

    /** Hands out copies of a written book. */
    public static final String BOOK_COPIER = "BookCopier";

    /** Throws whoever jumps on it. */
    public static final String BOUNCE_BLOCKS = "BounceBlocks";

    /** Runs a walkway out across a gap. */
    public static final String BRIDGE = "Bridge";

    /** Fills a doorway from its lintel or its threshold. */
    public static final String DOOR = "Door";

    /** Carries people between floors. */
    public static final String ELEVATOR = "Elevator";

    /** Drops a curtain of fence or pane from its lintel. */
    public static final String GATE = "Gate";

    /** Glowstone that goes dark rather than away. */
    public static final String GLOW_STONE = "GlowStone";

    /** A carved pumpkin that lights up. */
    public static final String JACK_O_LANTERN = "JackOLantern";

    /** Reads a block's light level off an item held up to it. */
    public static final String LIGHT_STONE = "LightStone";

    /** Turns every torch within reach on or off at once. */
    public static final String LIGHT_SWITCH = "LightSwitch";

    /** Hands out a numbered map. */
    public static final String MAP_COPIER = "MapCopier";

    /** A block that catches light on top of itself while it is powered. */
    public static final String NETHERRACK = "Netherrack";

    /** Copies what one sign says onto another. */
    public static final String SIGN_COPIER = "SignCopier";

    /** Snow that piles, slumps and melts. */
    public static final String SNOW = "Snow";

    /** Sends whoever clicks it somewhere else in the same world. */
    public static final String TELEPORTER = "Teleporter";

    /** Turns the experience somebody is carrying into bottles. */
    public static final String XP_STORER = "XPStorer";

    /**
     * All of them, in the order an operator's file lists them.
     *
     * <p>Alphabetical, because a file of twenty sections is read by looking a name up rather than
     * by reading it through, and nothing about a mechanic says where it ought to come.
     */
    public static final List<String> ALL = List.of(
            AMMETER,
            AREA,
            BANNER_COPIER,
            BOOK_COPIER,
            BOUNCE_BLOCKS,
            BRIDGE,
            DOOR,
            ELEVATOR,
            GATE,
            GLOW_STONE,
            JACK_O_LANTERN,
            LIGHT_STONE,
            LIGHT_SWITCH,
            MAP_COPIER,
            NETHERRACK,
            SIGN_COPIER,
            SNOW,
            TELEPORTER,
            XP_STORER);

    private Mechanics() {
    }

    /**
     * The proper spelling of a name written any way at all, or nothing where no mechanic has it.
     *
     * <p>What lets an operator write {@code bridge} and have it mean the same as {@code Bridge}.
     */
    public static String properly(String name, String fallback) {
        for (String mechanic : ALL) {
            if (mechanic.equalsIgnoreCase(name)) {
                return mechanic;
            }
        }
        return fallback;
    }

    /** Whether a name belongs to a mechanic at all, however it is spelt. */
    public static boolean exists(String name) {
        return !properly(name, "").isEmpty();
    }

    /** A name as it is compared, which is without regard to case. */
    public static String compared(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
