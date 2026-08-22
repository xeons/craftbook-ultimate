// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.lopper;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Felling a whole tree by breaking one log of it.
 *
 * <p>Nothing is built and nothing carries a sign: an axe in the hand and a log in front of it are
 * the whole of the declaration. What comes down is the trunk, and its leaves as well where an
 * operator has asked for that.
 *
 * <p>The sapling that regrows a tree is worked out from the block that was felled rather than kept
 * as a list, because the two are named the same way and a version of the game that adds a wood
 * adds its sapling in the same breath. A tree of a kind nobody has heard of simply leaves nothing
 * behind.
 */
@NullMarked
public final class TreeLoppers {

    /** What this is called, for the setting that switches it off. */
    public static final String NAME = Mechanics.TREE_LOPPER;

    /** The permission to fell a tree. */
    public static final String USE = "craftbook.treelopper.use";

    /** The permission to have the felled tree replanted. */
    public static final String SAPLING = "craftbook.treelopper.sapling";

    /** The permission to turn it off for oneself. */
    public static final String TOGGLE = "craftbook.treelopper.toggle";

    /** What the game calls the ground a sapling will take. */
    private static final String[] SOIL = {
        "dirt", "coarse_dirt", "rooted_dirt", "grass_block", "podzol", "mycelium", "moss_block",
        "mud", "muddy_mangrove_roots", "farmland",
    };

    /** Wood whose seedling is not called a sapling. */
    private static final String MANGROVE = "mangrove";

    /** The nether woods, which grow from a fungus rather than a sapling. */
    private static final String[] FUNGI = {"warped", "crimson"};

    private TreeLoppers() {
    }

    /**
     * The sapling that regrows a felled block, or nothing where none does.
     *
     * <p>Read off the name: {@code cherry_log}, {@code stripped_cherry_wood} and
     * {@code cherry_leaves} all name a cherry, and a cherry is regrown by a
     * {@code cherry_sapling}. Mangroves are the one exception, since what they drop is a propagule
     * rather than a sapling.
     */
    public static Optional<Key> saplingFor(Key felled) {
        String wood = woodOf(felled.value());
        if (wood.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Key.key(felled.namespace(), wood + seedling(wood)));
    }

    /** What the seedling of a wood is called, which is not always a sapling. */
    private static String seedling(String wood) {
        if (wood.equals(MANGROVE)) {
            return "_propagule";
        }
        for (String fungus : FUNGI) {
            if (wood.equals(fungus)) {
                return "_fungus";
            }
        }
        return "_sapling";
    }

    /** Whether a sapling will stay planted on a block. */
    public static boolean isSoil(Key block) {
        for (String soil : SOIL) {
            if (block.value().equals(soil)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How many saplings one felled tree is worth.
     *
     * <p>One for most, and four for the woods that grow from a two-by-two of saplings and so have
     * four trunks standing on the ground.
     */
    public static int saplingsFor(Key sapling) {
        String name = sapling.value();
        return name.startsWith("dark_oak") || name.startsWith("jungle") ? 4 : 1;
    }

    /**
     * The kind of wood a block is made of, or an empty string where it is not made of wood.
     *
     * <p>Stripping the prefixes and suffixes the game names these blocks with, rather than a table
     * of every one of them: {@code stripped_pale_oak_wood} is pale oak, and so is
     * {@code pale_oak_leaves}.
     */
    private static String woodOf(String block) {
        String name = block.toLowerCase(Locale.ROOT);
        name = strip(name, "stripped_", true);

        for (String suffix : new String[] {"_log", "_wood", "_leaves", "_stem", "_hyphae"}) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return "";
    }

    private static String strip(String name, String affix, boolean leading) {
        if (leading && name.startsWith(affix)) {
            return name.substring(affix.length());
        }
        return name;
    }
}
