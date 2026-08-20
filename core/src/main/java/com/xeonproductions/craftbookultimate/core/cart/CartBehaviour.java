package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.config.CartHabits;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What happens to a cart that is not standing on a mechanism.
 *
 * <p>The seven habits an operator may give a railway, gathered in one place because they are all
 * the same kind of thing: none of them is built, none of them has a sign, and every one of them
 * changes what every cart in the world does. A mechanic is a place on the track; these are the
 * weather it runs in.
 *
 * <p>Everything here is a decision rather than an act, so each can be exercised against a cart
 * built in a test. Carrying the answers out is the listener's work.
 *
 * <p>All of them are off unless an operator has said otherwise, which {@link CartHabits} is where
 * to read.
 */
@NullMarked
public final class CartBehaviour {

    /** How far from a cart something has to be lying to be gathered up. */
    public static final double PICKUP_REACH = 1.5;

    /** How hard something run down is thrown clear. */
    private static final double KNOCKBACK = 1.6;

    /** How far up something run down is thrown, so it does not merely slide along the track. */
    private static final double KNOCKBACK_LIFT = 0.3;

    /** How much a cart hurts what it runs into, in half hearts. */
    private static final double RUN_DOWN_DAMAGE = 10;

    /** The blocks that carry a cart straight across as a crossroads. */
    private static final Set<Key> CROSSING_PLATES = Set.of(
            key("stone_pressure_plate"),
            key("oak_pressure_plate"),
            key("spruce_pressure_plate"),
            key("birch_pressure_plate"),
            key("jungle_pressure_plate"),
            key("acacia_pressure_plate"),
            key("cherry_pressure_plate"),
            key("dark_oak_pressure_plate"),
            key("pale_oak_pressure_plate"),
            key("mangrove_pressure_plate"),
            key("bamboo_pressure_plate"),
            key("crimson_pressure_plate"),
            key("warped_pressure_plate"),
            key("polished_blackstone_pressure_plate"),
            key("light_weighted_pressure_plate"),
            key("heavy_weighted_pressure_plate"));

    private CartBehaviour() {}

    /**
     * Whether somebody may get into a cart.
     *
     * <p>People always may. Creatures may unless an operator has kept them out, which is what stops
     * a station filling up with whatever wandered onto the platform overnight.
     */
    public static boolean mayRide(Bystander who, CartHabits habits) {
        return who.isPlayer() || !habits.blockMobs();
    }

    /**
     * Whether a cart standing here should be taken away.
     *
     * <p>Asked once the waiting is over rather than at the moment it began, so a cart somebody has
     * climbed back into in the meantime is left where it is.
     */
    public static boolean hasStoodEmpty(Cart cart, CartHabits habits) {
        return habits.decaysEmptyCarts() && cart.isPresent() && !cart.isOccupied();
    }

    /**
     * Whether a cart is one that another passes through rather than shunts.
     *
     * <p>An empty cart and a laden one are told apart because they are wanted for opposite reasons:
     * passing through empty ones stops a siding of spares from blocking the line, and passing
     * through laden ones lets a train of goods share a track with the people using it.
     */
    public static boolean passesThrough(Cart other, CartHabits habits) {
        boolean plainAndEmpty = other.type() == CartType.RIDEABLE && !other.isOccupied();
        return plainAndEmpty ? habits.passThroughEmptyCarts() : habits.passThroughFullCarts();
    }

    /**
     * What a moving cart does to something it runs into.
     *
     * <p>Only a cart with somebody aboard does anything at all: an empty one rolling downhill is
     * not a weapon, and a railway would be unusable if it were.
     *
     * @param cart the cart doing the running down
     * @param hit whatever is in its way
     */
    public static Optional<RunDown> runDown(Cart cart, Bystander hit, CartHabits habits) {
        if (!habits.runDownEntities() || !cart.isOccupied() || isAboard(cart, hit)) {
            return Optional.empty();
        }

        boolean anotherCart = hit.type().value().endsWith("minecart");
        if (anotherCart && !habits.runDownOtherCarts()) {
            return Optional.empty();
        }
        if (hit.isLiving()) {
            return Optional.of(new RunDown(RUN_DOWN_DAMAGE, thrownClear(cart)));
        }
        // Nothing that is not alive can be hurt, so where hurting is the most that is allowed
        // there is nothing left to do to it.
        return habits.runDownOnlyHurts() ? Optional.empty() : Optional.of(RunDown.REMOVED);
    }

    /**
     * How a cart is pushed by whatever it has rolled into.
     *
     * <p>Two habits share this because they are the same act: a cart is given a shove by a block
     * that is not rail. A ladder or a vine sends it up the wall it clings to, and a pressure plate
     * sends it straight on across a crossroads at full pelt.
     *
     * @param at the block the cart has moved into
     * @return the velocity to give the cart, or nothing where the block is an ordinary one
     */
    public static Optional<Vec3d> pushFrom(
            CartWorld world, Vec3i at, Vec3d velocity, CartHabits habits) {
        if (habits.plateIntersections() && CROSSING_PLATES.contains(world.blockAt(at))) {
            // Nothing to send on where the cart is standing still, and no way to send it.
            Vec3d heading = velocity.normalise();
            return heading.length() == 0
                    ? Optional.empty()
                    : Optional.of(heading.multiply(CartHabits.CROSSING_SPEED));
        }
        if (!habits.climbsWalls()) {
            return Optional.empty();
        }

        Vec3d wall = towards(world.climbableSidesAt(at));
        if (wall.length() == 0) {
            return Optional.empty();
        }
        return Optional.of(velocity.add(wall.normalise()).add(0, habits.climbSpeed(), 0));
    }

    /**
     * Gathers up what a storage cart has rolled over.
     *
     * <p>A cart passes through a dropped item rather than colliding with it, so this asks what is
     * lying nearby rather than waiting to be told about a collision. A stack that will not all fit
     * is left where it is rather than half taken, so nothing disappears into a full cart.
     *
     * @return how many items were gathered
     */
    public static int gatherItems(Cart cart, CartWorld world, CartHabits habits) {
        if (!habits.pickUpItems() || !cart.type().holdsItems()) {
            return 0;
        }
        Optional<Stockpile> hold = cart.contents();
        if (hold.isEmpty()) {
            return 0;
        }

        int gathered = 0;
        for (DroppedItem item : world.itemsNear(cart.position(), PICKUP_REACH)) {
            int count = item.count();
            if (count > 0 && hold.get().hasRoomFor(item.type(), count)) {
                gathered += item.take(count);
                hold.get().give(item.type(), count);
            }
        }
        return gathered;
    }

    /** Whether something is riding the cart that is running it down. */
    private static boolean isAboard(Cart cart, Bystander hit) {
        for (Bystander rider : cart.riders()) {
            if (rider.equals(hit)) {
                return true;
            }
        }
        return false;
    }

    /** Which way something run down is thrown, which is on and up from where the cart is going. */
    private static Vec3d thrownClear(Cart cart) {
        Vec3d heading = cart.velocity().normalise();
        return heading.length() == 0
                ? Vec3d.ZERO
                : heading.multiply(KNOCKBACK).add(0, KNOCKBACK_LIFT, 0);
    }

    /** A vector towards whatever a climbable block is clinging to. */
    private static Vec3d towards(Set<BlockFace> sides) {
        Vec3d sum = Vec3d.ZERO;
        for (BlockFace side : sides) {
            sum = sum.add(Vec3d.of(side));
        }
        return sum;
    }

    private static Key key(String name) {
        return Key.key(Key.MINECRAFT_NAMESPACE, name);
    }

    /**
     * What happens to something a cart has run into.
     *
     * @param damage how many half hearts to take, none where it is to be removed instead
     * @param thrownClear which way to throw it, which is nowhere where the cart is not moving
     */
    public record RunDown(double damage, Vec3d thrownClear) {

        /** Taken out of the world rather than hurt, which is all that can be done to a thing. */
        public static final RunDown REMOVED = new RunDown(0, Vec3d.ZERO);

        /** Whether this is a removal rather than an injury. */
        public boolean removes() {
            return damage <= 0;
        }
    }
}
