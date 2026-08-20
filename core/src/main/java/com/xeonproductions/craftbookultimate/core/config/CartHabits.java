package com.xeonproductions.craftbookultimate.core.config;

import org.jspecify.annotations.NullMarked;

/**
 * How every cart on the server behaves, whatever it is standing on.
 *
 * <p>Kept apart from the rest of {@link CartSettings} because it answers a different question.
 * Everything there is about a mechanism — a block under a rail with a sign saying what to do, which
 * does nothing at all until somebody builds one. Everything here changes what an ordinary cart does
 * anywhere in the world, with no mechanism involved and nothing for a player to see.
 *
 * <p>That is why all of it is off out of the box. A server that has never been configured runs
 * carts exactly as the game does, and each of these is an operator saying they want something else.
 *
 * <p>The two numbers turn their own habit off when they are zero, since waiting no time at all and
 * climbing at no speed both mean not doing it.
 *
 * @param decayEmptyAfter how many ticks a cart may stand empty before it is taken away, 0 to leave
 *     empty carts alone
 * @param decayOnlyAfterExit whether only a cart somebody has got out of decays, rather than every
 *     cart from the moment it is placed
 * @param removeOnExit whether a cart is taken away the moment its rider gets out
 * @param giveCartBack whether taking it away hands the rider the cart back
 * @param pickUpItems whether a storage cart gathers up what it runs over
 * @param blockMobs whether creatures are kept out of carts, leaving them for people
 * @param climbSpeed how fast a cart climbs a ladder or a vine, 0 for a cart that cannot climb
 * @param plateIntersections whether a pressure plate carries a cart straight across as a crossroads
 * @param passThroughEmptyCarts whether a cart passes through an empty one rather than shunting it
 * @param passThroughFullCarts whether a cart passes through a laden or occupied one
 * @param runDownEntities whether an occupied cart hurts what it runs into
 * @param runDownOnlyHurts whether running something down stops at hurting it rather than removing it
 * @param runDownOtherCarts whether an occupied cart runs down other carts as well as creatures
 */
@NullMarked
public record CartHabits(
        long decayEmptyAfter,
        boolean decayOnlyAfterExit,
        boolean removeOnExit,
        boolean giveCartBack,
        boolean pickUpItems,
        boolean blockMobs,
        double climbSpeed,
        boolean plateIntersections,
        boolean passThroughEmptyCarts,
        boolean passThroughFullCarts,
        boolean runDownEntities,
        boolean runDownOnlyHurts,
        boolean runDownOtherCarts) {

    /**
     * How long the legacy fork waited before taking an empty cart away.
     *
     * <p>Not a default here, since nothing takes carts away unless it is asked to, but it is what
     * an operator switching decay on most likely wants.
     */
    public static final long CUSTOMARY_DECAY_TICKS = 40;

    /** How fast the legacy fork sent a cart up a ladder. */
    public static final double CUSTOMARY_CLIMB_SPEED = 0.15;

    /** How fast a cart is sent across a crossroads, which is as fast as it can go. */
    public static final double CROSSING_SPEED = 4;

    /** Nothing switched on: carts behave as the game runs them. */
    public static final CartHabits DEFAULTS = new CartHabits(
            0, true, false, true, false, false, 0, false, false, false, false, false, false);

    /** Holds the two numbers to something that means anything. */
    public CartHabits {
        decayEmptyAfter = Math.max(0, decayEmptyAfter);
        climbSpeed = Math.max(0, climbSpeed);
    }

    /** Whether a cart left standing empty is ever taken away. */
    public boolean decaysEmptyCarts() {
        return decayEmptyAfter > 0;
    }

    /** Whether a cart can climb a ladder or a vine at all. */
    public boolean climbsWalls() {
        return climbSpeed > 0;
    }

    /** Whether a cart passes through anything at all rather than shunting it. */
    public boolean passesThroughAnyCart() {
        return passThroughEmptyCarts || passThroughFullCarts;
    }

    /** These habits with empty carts decaying, or not. */
    public CartHabits withDecay(long afterTicks, boolean onlyAfterExit) {
        return new CartHabits(afterTicks, onlyAfterExit, removeOnExit, giveCartBack, pickUpItems,
                blockMobs, climbSpeed, plateIntersections, passThroughEmptyCarts,
                passThroughFullCarts, runDownEntities, runDownOnlyHurts, runDownOtherCarts);
    }

    /** These habits with a cart taken away when its rider leaves, or not. */
    public CartHabits withExitRemoval(boolean remove, boolean giveBack) {
        return new CartHabits(decayEmptyAfter, decayOnlyAfterExit, remove, giveBack, pickUpItems,
                blockMobs, climbSpeed, plateIntersections, passThroughEmptyCarts,
                passThroughFullCarts, runDownEntities, runDownOnlyHurts, runDownOtherCarts);
    }

    /** These habits with storage carts gathering up what they pass, or not. */
    public CartHabits withItemPickup(boolean pickUp) {
        return new CartHabits(decayEmptyAfter, decayOnlyAfterExit, removeOnExit, giveCartBack,
                pickUp, blockMobs, climbSpeed, plateIntersections, passThroughEmptyCarts,
                passThroughFullCarts, runDownEntities, runDownOnlyHurts, runDownOtherCarts);
    }

    /** These habits with creatures kept out of carts, or not. */
    public CartHabits withMobBlocking(boolean block) {
        return new CartHabits(decayEmptyAfter, decayOnlyAfterExit, removeOnExit, giveCartBack,
                pickUpItems, block, climbSpeed, plateIntersections, passThroughEmptyCarts,
                passThroughFullCarts, runDownEntities, runDownOnlyHurts, runDownOtherCarts);
    }

    /** These habits with carts climbing walls and crossing plates, or not. */
    public CartHabits withClimbing(double speed, boolean plates) {
        return new CartHabits(decayEmptyAfter, decayOnlyAfterExit, removeOnExit, giveCartBack,
                pickUpItems, blockMobs, speed, plates, passThroughEmptyCarts, passThroughFullCarts,
                runDownEntities, runDownOnlyHurts, runDownOtherCarts);
    }

    /** These habits with carts passing through one another, or not. */
    public CartHabits withPassThrough(boolean empty, boolean full) {
        return new CartHabits(decayEmptyAfter, decayOnlyAfterExit, removeOnExit, giveCartBack,
                pickUpItems, blockMobs, climbSpeed, plateIntersections, empty, full,
                runDownEntities, runDownOnlyHurts, runDownOtherCarts);
    }

    /** These habits with occupied carts hurting what they hit, or not. */
    public CartHabits withRunDown(boolean runDown, boolean onlyHurts, boolean otherCarts) {
        return new CartHabits(decayEmptyAfter, decayOnlyAfterExit, removeOnExit, giveCartBack,
                pickUpItems, blockMobs, climbSpeed, plateIntersections, passThroughEmptyCarts,
                passThroughFullCarts, runDown, onlyHurts, otherCarts);
    }
}
