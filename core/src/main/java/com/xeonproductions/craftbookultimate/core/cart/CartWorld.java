// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The world as a cart mechanic sees it.
 *
 * <p>Separate from the seam the chips use because the two want different things. A chip asks about
 * redstone, weather and growing crops; a cart mechanic asks about rails, signs and what is in the
 * chests beside the track. Keeping them apart means neither has to carry the other's methods.
 *
 * <p>Everything here is about the mechanism's own surroundings, a few blocks at most, so a mechanic
 * never reaches across a regionised server into somewhere another thread owns.
 */
@NullMarked
public interface CartWorld {

    /** What block is at a position. */
    Key blockAt(Vec3i position);

    /** Whether a position holds rail of any kind. */
    boolean isRail(Vec3i position);

    /** Which way the rail at a position runs, empty if there is no rail there. */
    Optional<RailShape> railShapeAt(Vec3i position);

    /**
     * Bends the rail at a position.
     *
     * <p>How a junction steers a cart: the sorting sign sets the rail ahead of the cart before the
     * cart reaches it.
     *
     * @return whether there was rail there to bend
     */
    boolean setRailShapeAt(Vec3i position, RailShape shape);

    /** The sign at a position, empty if there is no sign there. */
    Optional<CartMechanism.MechanismSign> signAt(Vec3i position);

    /** Whether a position is loaded and can be read. */
    boolean isLoaded(Vec3i position);

    /**
     * Which sides a climbable block clings to.
     *
     * <p>A ladder clings to the one wall it is nailed to and a vine to however many it has grown
     * across. Empty for anything that is not climbable, which is nearly every block.
     *
     * <p>This is what lets a cart climb one: it is pushed up and into whatever the ladder or vine
     * is holding on to, so it rides the face rather than sliding off it.
     */
    Set<BlockFace> climbableSidesAt(Vec3i position);

    /** The lowest block a world has. */
    int minHeight();

    /** One past the highest block a world has. */
    int maxHeight();

    /** The carts within a distance of a point. */
    List<Cart> cartsNear(Vec3d centre, double radius);

    /**
     * The people within a distance of a point.
     *
     * <p>Only those who are really there: somebody spectating or hidden is not loaded into a cart
     * any more than they set off a sensor.
     */
    List<Bystander> playersNear(Vec3d centre, double radius);

    /**
     * Everything lying on the ground within a distance of a point.
     *
     * <p>What a storage cart gathers up as it passes. A cart runs through a dropped item rather
     * than colliding with it, so this is asked as the cart moves rather than when it hits
     * something.
     */
    List<DroppedItem> itemsNear(Vec3d centre, double radius);

    /**
     * The containers at a list of positions, taken as one stockpile.
     *
     * <p>The caller says exactly which blocks to look at, because the shape a cart mechanic
     * searches is its own: a flat spread either side of the track, and a couple of blocks directly
     * above it for a hopper feeding down.
     *
     * <p>The two halves of a double chest count once.
     */
    Stockpile containersAt(List<Vec3i> positions);

    /**
     * Drops an item into the world.
     *
     * @return whether anything was dropped
     */
    boolean dropItem(Vec3d at, Key item, int count);

    /**
     * Puts a vehicle into the world.
     *
     * @param at where to put it
     * @param kind what to put there
     * @param name what to call it, or empty to leave it unnamed
     * @return the vehicle, or empty if it could not be placed
     */
    Optional<Cart> spawnVehicle(Vec3d at, VehicleKind kind, Optional<String> name);

    /**
     * The recipe a sign names, if the server knows one.
     *
     * <p>Named the way a sign writes it: the recipe's own name with its underscores taken out, so
     * that {@code goldenapple} finds the golden apple.
     */
    Optional<CartRecipe> recipeNamed(String signName);

    /**
     * Reads an item's name, the modern way or the way it was written before the flattening.
     *
     * <p>Signs from before the flattening name items as a number and a damage value, and those
     * signs are still in the ground, so both spellings resolve.
     */
    default Optional<Key> resolveItem(String written) {
        return BlockReference.parse(written).flatMap(BlockReference::asKey);
    }

    /** Whether a position is within the world's floor and ceiling. */
    default boolean isInBounds(Vec3i position) {
        return position.y() >= minHeight() && position.y() < maxHeight();
    }
}
