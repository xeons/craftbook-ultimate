package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * A minecart, as much of one as a mechanic needs to see.
 *
 * <p>Everything a cart mechanic does to a cart goes through here: reading what kind it is and who
 * is aboard, changing where it is going, and emptying or filling it. That keeps the mechanics
 * themselves free of the server, so a sorting rule or a launch decision can be exercised against a
 * cart built in a test.
 *
 * <p>A cart may stop existing while something still holds one of these, so anything that acts on
 * the world checks {@link #isPresent()} first, and every method that changes it reports whether it
 * managed to.
 */
@NullMarked
public interface Cart {

    /** What kind of cart this is. */
    CartType type();

    /** Where the cart is. */
    Vec3d position();

    /** How fast the cart is going, and which way. */
    Vec3d velocity();

    /**
     * Sets where the cart is going.
     *
     * @return whether the cart was still there to be pushed
     */
    boolean setVelocity(Vec3d velocity);

    /** The fastest this cart is allowed to go, which is what a station launches it at. */
    double maximumSpeed();

    /**
     * Who and what is riding, nearest the cart first.
     *
     * <p>A rider is a {@link Bystander}, so a mechanic can ask whether it is a player, what it is
     * called, what it is holding and what group it belongs to without knowing anything more.
     */
    List<Bystander> riders();

    /** The name somebody gave this cart, if it has one. */
    Optional<String> customName();

    /**
     * What the cart is carrying, for the kinds that carry anything.
     *
     * <p>Empty for a cart that holds nothing, which is not the same as a chest cart holding
     * nothing: that one answers with an empty stockpile.
     */
    Optional<Stockpile> contents();

    /**
     * What is in the first of the cart's slots.
     *
     * <p>The one place a mechanic cares which slot something is in: a junction can be set to sort
     * on the first slot alone, so that a loader filling the rest of the cart does not change which
     * way it goes.
     */
    Optional<ItemView> firstStoredItem();

    /**
     * Puts somebody in the cart.
     *
     * @return whether they got in, which they do not if the cart is full or either is gone
     */
    boolean board(Bystander rider);

    /** Whether the cart is still in the world. */
    boolean isPresent();

    /**
     * Takes the cart out of the world.
     *
     * @return whether there was still a cart to take
     */
    boolean remove();

    /**
     * Moves the cart somewhere else in the same world.
     *
     * @return whether the cart was still there to move
     */
    boolean teleport(Vec3d to);

    /** Whether anybody or anything is riding. */
    default boolean isOccupied() {
        return !riders().isEmpty();
    }

    /** Whoever is riding nearest the cart, which is who a mechanic speaks to and about. */
    default Optional<Bystander> firstRider() {
        List<Bystander> riders = riders();
        return riders.isEmpty() ? Optional.empty() : Optional.of(riders.getFirst());
    }

    /** The player riding nearest the cart, if the first rider is a player. */
    default Optional<Bystander> ridingPlayer() {
        return firstRider().filter(Bystander::isPlayer);
    }

    /** How fast the cart is going, without regard to direction. */
    default double speed() {
        return velocity().length();
    }

    /** Stops the cart where it is. */
    default boolean stop() {
        return setVelocity(Vec3d.ZERO);
    }
}
