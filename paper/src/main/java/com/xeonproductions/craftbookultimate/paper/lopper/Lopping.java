// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.lopper;

import com.xeonproductions.craftbookultimate.core.lopper.TreeLoppers;
import com.xeonproductions.craftbookultimate.core.lopper.VeinMiners;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * Whether one player wants the loppers working for them.
 *
 * <p>Kept in the player's own data rather than as a list of identifiers in the settings file,
 * which is what the fork did and what the chairs already rejected: a preference is the player's,
 * the game persists it for us, and an operator's file has no business growing a line every time
 * somebody changes their mind.
 *
 * <p>Both mechanics are answered here because they are one question asked twice. Somebody who has
 * turned the tree lopper off has said nothing about the vein miner, so each is remembered
 * separately, but neither wants its own copy of how remembering works.
 */
@NullMarked
public final class Lopping {

    private final NamespacedKey treeOff;
    private final NamespacedKey veinOff;

    public Lopping(Plugin plugin) {
        this.treeOff = new NamespacedKey(plugin, "treelopper-off");
        this.veinOff = new NamespacedKey(plugin, "veinminer-off");
    }

    /** Whether felling trees is switched on for somebody, which it is until they say otherwise. */
    public boolean fellsTrees(Player who) {
        return !isOff(who, treeOff);
    }

    /** Whether mining seams is switched on for somebody. */
    public boolean minesSeams(Player who) {
        return !isOff(who, veinOff);
    }

    /**
     * Turns felling trees on or off for somebody.
     *
     * @return whether it is now on
     */
    public boolean fellTrees(Player who, boolean wanted) {
        return set(who, treeOff, wanted);
    }

    /**
     * Turns mining seams on or off for somebody.
     *
     * @return whether it is now on
     */
    public boolean mineSeams(Player who, boolean wanted) {
        return set(who, veinOff, wanted);
    }

    /** The permission to turn the tree lopper off for oneself. */
    public static String treeTogglePermission() {
        return TreeLoppers.TOGGLE;
    }

    /** The permission to turn the vein miner off for oneself. */
    public static String veinTogglePermission() {
        return VeinMiners.TOGGLE;
    }

    /**
     * Whether a mechanic is switched off for somebody.
     *
     * <p>Stored as being off rather than being on, so a player who has never said anything carries
     * nothing at all and the mechanic works for them.
     */
    private static boolean isOff(Player who, NamespacedKey mark) {
        Byte stored = who.getPersistentDataContainer().get(mark, PersistentDataType.BYTE);
        return stored != null && stored != 0;
    }

    private static boolean set(Player who, NamespacedKey mark, boolean wanted) {
        if (wanted) {
            who.getPersistentDataContainer().remove(mark);
        } else {
            who.getPersistentDataContainer().set(mark, PersistentDataType.BYTE, (byte) 1);
        }
        return wanted;
    }
}
