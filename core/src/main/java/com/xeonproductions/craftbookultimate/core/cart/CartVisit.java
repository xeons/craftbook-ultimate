package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import org.jspecify.annotations.NullMarked;

/**
 * A cart arriving at a mechanism, and everything the mechanic may do about it.
 *
 * <p>The whole contract between a {@link CartMechanic} and the world, in the same way
 * {@code ChipState} is for a chip. A mechanic reads the cart, the three blocks it is standing on
 * and the settings, and acts through the seams here; it never touches a server.
 */
@NullMarked
public interface CartVisit {

    /** The cart that has arrived. */
    Cart cart();

    /** The blocks the cart has arrived at. */
    CartMechanism mechanism();

    /**
     * Whether the cart is still inside the block it was in last tick.
     *
     * <p>A cart spends most of its ticks crossing a single block, and a mechanic that acted on
     * every one of them would fire ten times for one arrival. Almost every mechanic ignores a
     * minor move and acts only when the cart has actually come from somewhere.
     */
    boolean isMinor();

    /** Where the cart was before this move. */
    Vec3d from();

    /** Whether anything has been wired to the mechanism, and whether it is on. */
    Wiring wiring();

    /** The world the mechanism is in. */
    CartWorld world();

    /** Where every rider has said they are going. */
    Stations stations();

    /** The settings in force. */
    Settings settings();

    /**
     * Schedules work on the region owning the mechanism.
     *
     * <p>A mechanic that acts after a delay goes through here, so that the work runs on the thread
     * allowed to touch these blocks.
     */
    Scheduler scheduler();

    /** Whether the mechanic should act at all, which is to say nobody has switched it off. */
    default boolean isAllowed() {
        return wiring().allows();
    }

    /**
     * Whether the cart has properly arrived rather than merely shuffling within one block.
     *
     * <p>What nearly every mechanic checks first.
     */
    default boolean hasArrived() {
        return !isMinor() && isAllowed();
    }

    /**
     * How far the cart was from the middle of the rail before this move.
     *
     * <p>The mechanics that stop a cart dead wait until it is nearly centred, so that it comes to
     * rest on the block rather than half off the end of it.
     */
    default double distanceFromRailCentre() {
        Vec3d centre = Vec3d.centreOf(mechanism().rail());
        return from().distanceSquared(centre);
    }
}
