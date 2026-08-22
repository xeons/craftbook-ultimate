// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.head;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Dropping the head of whatever was killed.
 *
 * <p>Nothing is built and nothing carries a sign: switching this on changes what every death in
 * the world leaves behind. A player drops their own head, and a creature drops either the head the
 * game already has for it or a player head wearing the face {@link MobHeads} keeps for it.
 *
 * <p>What is here is the part that is arithmetic and naming: how likely a head is, which head a
 * creature has, and what the thing is called once it is in a hand.
 */
@NullMarked
public final class HeadDrops {

    /** The name an operator switches it off by. */
    public static final String NAME = Mechanics.HEAD_DROPS;

    /** Getting a head for killing something. */
    public static final String KILL = "craftbook.headdrops.kill";

    /** The player head, which is also what every creature the game has no head for wears. */
    public static final Key PLAYER_HEAD = Key.key("minecraft:player_head");

    /** The heads the game has of its own, by what they came off. */
    private static final Map<Key, Key> VANILLA = Map.of(
            creature("player"), PLAYER_HEAD,
            creature("zombie"), Key.key("minecraft:zombie_head"),
            creature("creeper"), Key.key("minecraft:creeper_head"),
            creature("skeleton"), Key.key("minecraft:skeleton_skull"),
            creature("wither_skeleton"), Key.key("minecraft:wither_skeleton_skull"),
            creature("ender_dragon"), Key.key("minecraft:dragon_head"),
            creature("piglin"), Key.key("minecraft:piglin_head"));

    private HeadDrops() {
    }

    /**
     * Which head the game already has for a creature, if it has one.
     *
     * <p>Asked before {@link MobHeads}, so the seven the game knows about drop the real thing
     * rather than a player head wearing a picture of one.
     */
    public static Optional<Key> vanillaHead(Key creature) {
        return Optional.ofNullable(VANILLA.get(creature));
    }

    /** Whether anything at all drops a head for a creature. */
    public static boolean hasHead(Key creature) {
        return VANILLA.containsKey(creature) || MobHeads.ownerOf(creature).isPresent();
    }

    /**
     * How likely a head is, given what the killer was holding.
     *
     * <p>Looting adds to the chance rather than multiplying it, which is what the setting has
     * always meant: a five per cent chance with a five per cent modifier and looting three is
     * twenty per cent. Never past certain, and never below nothing.
     *
     * @param rate the chance before anything is taken into account
     * @param perLevel what each level of looting adds
     * @param looting the level of looting on whatever did the killing, or zero for none
     */
    public static double chanceOf(double rate, double perLevel, int looting) {
        return Math.clamp(rate + Math.max(0, perLevel) * Math.max(0, looting), 0, 1);
    }

    /**
     * What a head is called once it is an item.
     *
     * <p>A player's head is theirs — {@code Steve's Head} — and a creature's is the creature's
     * kind, which is what the sign on it would say. The underscores in a creature's name become
     * spaces and each word is capitalised, so {@code cave_spider} reads as {@code Cave Spider
     * Head}.
     *
     * @param creature what was killed
     * @param player the player's name where a player was killed, or empty otherwise
     */
    public static String nameOf(Key creature, String player) {
        if (!player.isEmpty()) {
            return player + "'s Head";
        }
        return titled(creature.value()) + " Head";
    }

    /** What a placed head says about itself when somebody clicks it. */
    public static String describe(Key creature, String owner) {
        return creature.equals(creature("player"))
                ? "The severed head of " + owner
                : "The severed head of a " + titled(creature.value()).toLowerCase(Locale.ROOT);
    }

    /**
     * Whether a name is one an operator has asked to be left alone.
     *
     * <p>Compared without regard to case, because a list of names is written by hand and an
     * operator who wrote one in the wrong case meant the account rather than the spelling.
     */
    public static boolean isIgnored(Set<String> ignored, String name) {
        for (String written : ignored) {
            if (written.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** A creature's name with its underscores out and each word capitalised. */
    private static String titled(String name) {
        StringBuilder titled = new StringBuilder(name.length());
        boolean starting = true;
        for (char letter : name.toCharArray()) {
            if (letter == '_') {
                titled.append(' ');
                starting = true;
                continue;
            }
            titled.append(starting ? Character.toUpperCase(letter) : letter);
            starting = false;
        }
        return titled.toString();
    }

    private static Key creature(String name) {
        return Key.key(Key.MINECRAFT_NAMESPACE, name);
    }
}
