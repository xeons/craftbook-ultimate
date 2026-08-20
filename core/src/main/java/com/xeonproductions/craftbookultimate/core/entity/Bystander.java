package com.xeonproductions.craftbookultimate.core.entity;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * Something standing near a chip that the chip can look at and act on.
 *
 * <p>Mostly living things, since the chips that use this hurt, remove or dose what they find, but
 * a sign may also name a minecart, a boat or a dropped stack, so this is not restricted to what
 * breathes. {@link Traveller} is the narrower view used by the chips that only move people about.
 *
 * <p>A reference is only good for as long as its subject is there. Anything acting on a list
 * gathered a moment ago should check {@link #isPresent()} first, since a chip may kill something
 * partway down the list and take its riders with it.
 */
@NullMarked
public interface Bystander {

    /** What kind of thing this is, named the way the game names it. */
    Key type();

    /** Where it is, which is the point it stands on rather than the block containing it. */
    Vec3d position();

    /** Whether it is a player. */
    boolean isPlayer();

    /**
     * Whether it is alive, in the sense of having health and being able to take a potion.
     *
     * <p>True for players, mobs and animals; false for a minecart, a boat or a dropped stack.
     */
    boolean isLiving();

    /**
     * Whether the game counts it as hostile.
     *
     * <p>The game's own grouping rather than a list kept here, so a mob added by a later version
     * is classified without anything here changing.
     */
    boolean isMonster();

    /** Whether the game counts it as an animal. */
    boolean isAnimal();

    /** Whether somebody has tamed it. False for anything that cannot be tamed. */
    boolean isTamed();

    /** What it is called. For a player this is their account name. */
    String name();

    /**
     * Whether a player belongs to a permission group.
     *
     * <p>False for anything that is not a player, since nothing else has permissions.
     */
    boolean isInGroup(String group);

    /** The few properties signs can ask about beyond the kind of thing it is. */
    Traits traits();

    /** What a dropped stack holds, empty for anything that is not one. */
    Optional<Key> carriedItem();

    /**
     * What it is holding in its main hand, empty for anything holding nothing.
     *
     * <p>Only players and the mobs that can hold things ever have one.
     */
    Optional<ItemView> heldItem();

    /**
     * Whether it is really there to be sensed.
     *
     * <p>A player in spectator mode is walking through walls and a vanished one is not meant to be
     * seen at all, so neither should set off a sensor they pass. Everything else is visible.
     */
    boolean isVisible();

    /**
     * What it is carrying about, for anything that carries a pack.
     *
     * <p>Only a player has one. A cart sorter is the one thing that asks, so that a junction can
     * send a rider carrying a pickaxe down the mine and everybody else past it.
     */
    Optional<Stockpile> inventory();

    /**
     * The name the server knows it by, which never changes and is never reused.
     *
     * <p>Only useful for a player, and only where something has to be remembered about them
     * between one place and another: where they said they were going, most of all. Everything else
     * about somebody is read from the entity in front of the mechanic.
     */
    Optional<UUID> uniqueId();

    /**
     * Says something to it.
     *
     * <p>Only a player hears anything; everything else quietly ignores it, so a mechanic speaking
     * to whoever is riding does not have to ask what kind of rider it has first.
     *
     * @return whether there was anybody there to hear it
     */
    boolean tell(Component message);

    /** The things riding on it. */
    List<Bystander> riders();

    /** Whether it is still in the world. */
    boolean isPresent();

    /**
     * Hurts it, ignoring armour.
     *
     * @param amount how many half-hearts to take
     * @return true if the damage was dealt
     */
    boolean damage(double amount);

    /**
     * Takes it out of the world without dropping anything.
     *
     * @return true if it was there to remove
     */
    boolean remove();

    /**
     * Adds potion effects to whatever it already has.
     *
     * <p>Adds rather than replaces, so a chip dosing a passing player does not strip the potions
     * they were already carrying.
     *
     * @return true if any effect was applied
     */
    boolean applyEffects(List<PotionDose> doses);

    /**
     * The handful of properties a sign can name beyond the kind of thing.
     *
     * @param charged whether a creeper has been struck by lightning
     * @param saddled whether a pig is wearing a saddle
     * @param dyeColour the colour of a sheep's wool, if it has one
     */
    record Traits(boolean charged, boolean saddled, Optional<String> dyeColour) {

        /** Nothing in particular, which is what most things have. */
        public static final Traits NONE = new Traits(false, false, Optional.empty());

        /** A creeper's state. */
        public static Traits ofCreeper(boolean charged) {
            return new Traits(charged, false, Optional.empty());
        }

        /** A pig's state. */
        public static Traits ofPig(boolean saddled) {
            return new Traits(false, saddled, Optional.empty());
        }

        /** A sheep's state. */
        public static Traits ofSheep(String dyeColour) {
            return new Traits(false, false, Optional.of(dyeColour));
        }
    }
}
