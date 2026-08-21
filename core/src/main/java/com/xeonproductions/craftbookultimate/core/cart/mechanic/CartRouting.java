// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.Cart;
import com.xeonproductions.craftbookultimate.core.cart.CartFilter;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.cart.CartWorld;
import com.xeonproductions.craftbookultimate.core.cart.RailShape;
import com.xeonproductions.craftbookultimate.core.cart.Stations;
import com.xeonproductions.craftbookultimate.core.cart.Wiring;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * The mechanics that decide where a cart goes.
 *
 * <p>A junction bends the rail ahead of the cart, a lift carries it to another floor, a station
 * holds it until it is sent on, and the two remaining ones deal with the destination a rider has
 * set: one stops for it, the other forgets it.
 *
 * <h2>How a railway routes</h2>
 *
 * <p>A rider says where they are going once, by command. Every junction between here and there
 * reads that one name and bends its rail accordingly, and the station whose name it is holds the
 * cart when it arrives. Nothing else is stored and nothing is planned in advance, so a railway
 * grows a new branch simply by somebody building a junction that asks for it.
 */
@NullMarked
public final class CartRouting {

    /** The line a mechanic reads its first choice from. */
    private static final int THIRD_LINE = 2;

    /** The line a mechanic reads its second choice from. */
    private static final int FOURTH_LINE = 3;

    /** Marks a station name rather than an ordinary filter. */
    private static final char STATION_MARKER = '#';

    /**
     * How near the middle of the rail a cart has to be before a mechanic stops it dead.
     *
     * <p>Squared, and generous: a cart stopped as it enters the block would come to rest half off
     * the end of the rail, which looks wrong and leaves it awkward to push out again.
     */
    private static final double STOPPING_DISTANCE_SQUARED = 0.1;

    /** How near the middle a direction block waits for, which is tighter still. */
    private static final double DIRECTION_STOPPING_DISTANCE_SQUARED = 0.15;

    /** Where a junction's rail sits: two above the block behind the sign. */
    private static final int JUNCTION_HEIGHT = 2;

    private CartRouting() {}

    /**
     * Bends the rail ahead of a cart, sending it one way or the other.
     *
     * <p>Line 3 says which carts go left and line 4 which go right; anything matching neither goes
     * straight on. Left and right are from the point of view of somebody standing at the sign
     * looking at it, which is how a builder reads their own junction.
     *
     * <p>The rail it bends is two blocks above the one behind the sign, which is where the track
     * carries on past the mechanism.
     */
    public static CartMechanic sorter() {
        return new Sorter();
    }

    /**
     * Carries a cart to another floor.
     *
     * <p>Line 3 says which carts go up and line 4 which go down. The lift looks for the next
     * mechanism of its own kind directly above or below with rail on top of it, and puts the cart
     * there going at the speed it arrived, so a cart keeps its momentum across floors.
     */
    public static CartMechanic lift() {
        return new Lift();
    }

    /**
     * Stops a cart and asks the rider which way they want to go.
     *
     * <p>Needs no sign. The cart is held until the rider names a direction, which they do by
     * looking that way and asking to go; a rail that does not run that way refuses.
     */
    public static CartMechanic direction() {
        return new DirectionBlock();
    }

    /**
     * Holds a cart at a platform until it is sent on.
     *
     * <p>Powered, it launches the cart away behind the sign at the cart's top speed. Unpowered, it
     * holds whatever arrives. Line 3 may name the station with a leading {@code #}, in which case
     * only a rider heading for that name is held and everybody else rolls through.
     */
    public static CartMechanic station() {
        return new Station();
    }

    /**
     * Forgets where a rider said they were going.
     *
     * <p>Built at the end of a line, so that somebody arriving is not still routed onward by every
     * junction they pass on their next journey.
     */
    public static CartMechanic stationClear() {
        return new StationClear();
    }

    /**
     * Turns a cart around.
     *
     * <p>Without a sign it reverses whatever crosses it, which is what a builder puts at the end
     * of a spur. With a sign it reverses only a cart that is not already travelling the way the
     * sign looks, which makes it a one-way: come at it the wrong way and you are sent back.
     */
    public static CartMechanic reverser() {
        return new Reverser();
    }

    /** Bends the rail ahead of a cart. */
    private record Sorter() implements CartMechanic {

        @Override
        public String name() {
            return "Sort";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || !visit.mechanism().isNamed(name())) {
                return false;
            }

            Optional<CartMechanism.MechanismSign> sign = visit.mechanism().sign();
            if (sign.isEmpty() || !sign.get().facing().isCardinal()) {
                return false;
            }

            BlockFace facing = sign.get().facing();
            Vec3i junction = sign.get().position()
                    .offset(facing.opposite())
                    .add(0, JUNCTION_HEIGHT, 0);
            if (!visit.world().isRail(junction)) {
                return false;
            }

            BlockFace onward = chosenDirection(visit, facing);
            RailShape.joining(facing, onward)
                    .ifPresent(shape -> visit.world().setRailShapeAt(junction, shape));
            return false;
        }

        /**
         * Which way the cart leaves the junction.
         *
         * <p>Straight on unless one of the two filters claims it. Left and right are worked out
         * from the way the cart is travelling, which is away behind the sign.
         */
        private static BlockFace chosenDirection(CartVisit visit, BlockFace facing) {
            BlockFace onward = facing.opposite();
            if (matches(visit, visit.mechanism().line(THIRD_LINE))) {
                return onward.rotateCounterClockwise();
            }
            if (matches(visit, visit.mechanism().line(FOURTH_LINE))) {
                return onward.rotateClockwise();
            }
            return onward;
        }
    }

    /** Carries a cart to another floor. */
    private record Lift() implements CartMechanic {

        /** The name on the sign, which is not the same as the mechanic's own name. */
        private static final String SIGN_NAME = "CartLift";

        /** The sides a lift leaves by, in the order it tries them. */
        private static final List<BlockFace> PREFERRED_EXITS =
                List.of(BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH);

        @Override
        public String name() {
            return "Lift";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || !visit.mechanism().isNamed(SIGN_NAME)) {
                return false;
            }

            boolean up = matches(visit, visit.mechanism().line(THIRD_LINE));
            boolean down = !up && matches(visit, visit.mechanism().line(FOURTH_LINE));
            if (!up && !down) {
                return false;
            }

            Optional<Vec3i> landing = findFloor(visit, up ? 1 : -1);
            if (landing.isEmpty()) {
                return false;
            }

            BlockFace leaving = departureFrom(visit.world(), landing.get());
            double speed = visit.cart().speed();
            Vec3i rail = landing.get().add(0, 1, 0);

            // Set down half a block along the way out, so the cart is clear of the middle of the
            // lift and cannot land on it and be carried straight back.
            Vec3d arrival = Vec3d.centreOf(rail).add(Vec3d.of(leaving).multiply(0.5));
            visit.cart().teleport(arrival);
            visit.cart().setVelocity(Vec3d.of(leaving).multiply(speed));
            return false;
        }

        /** The next lift directly above or below with rail on top of it. */
        private Optional<Vec3i> findFloor(CartVisit visit, int step) {
            CartWorld world = visit.world();
            Optional<Key> block = visit.settings().carts().blockFor(name());
            if (block.isEmpty()) {
                return Optional.empty();
            }

            Vec3i from = visit.mechanism().base();
            for (int y = from.y() + step; y >= world.minHeight() && y < world.maxHeight(); y += step) {
                Vec3i candidate = new Vec3i(from.x(), y, from.z());
                if (!world.isLoaded(candidate)) {
                    return Optional.empty();
                }
                if (!world.blockAt(candidate).equals(block.get())) {
                    continue;
                }
                if (world.isRail(candidate.add(0, 1, 0))) {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }

        /**
         * Which way a cart leaves the floor it has been carried to.
         *
         * <p>A sign on that floor says so; without one the lift takes whichever way the rail
         * actually runs, preferring west and working round.
         */
        private static BlockFace departureFrom(CartWorld world, Vec3i base) {
            Optional<CartMechanism.MechanismSign> below = world.signAt(base.offset(BlockFace.DOWN));
            if (below.isPresent()) {
                return below.get().outward();
            }
            for (BlockFace side : BlockFace.horizontals()) {
                Optional<CartMechanism.MechanismSign> beside = world.signAt(base.offset(side));
                if (beside.isPresent()) {
                    return beside.get().outward();
                }
            }
            return alongTheRail(world, base.add(0, 1, 0));
        }

        /** The way the rail runs, preferring west and working round. */
        private static BlockFace alongTheRail(CartWorld world, Vec3i rail) {
            Optional<RailShape> shape = world.railShapeAt(rail);
            if (shape.isEmpty()) {
                return BlockFace.NORTH;
            }
            for (BlockFace preferred : PREFERRED_EXITS) {
                if (shape.get().runs(preferred) && world.isRail(rail.offset(preferred))) {
                    return preferred;
                }
            }
            return shape.get().directions().getFirst();
        }
    }

    /** Stops a cart and asks the rider which way to go. */
    private record DirectionBlock() implements CartMechanic {

        @Override
        public String name() {
            return "Direction";
        }

        @Override
        public boolean requiresSign() {
            return false;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.isAllowed()) {
                return false;
            }
            // Waits for the cart to reach the middle rather than stopping it on the way in, so it
            // comes to rest on the block and the rider can leave by any of the ways out.
            if (visit.distanceFromRailCentre() > DIRECTION_STOPPING_DISTANCE_SQUARED) {
                return false;
            }

            visit.cart().stop();

            Optional<Bystander> rider = visit.cart().ridingPlayer();
            if (rider.isEmpty()) {
                return false;
            }
            rider.get().tell(Component.text(
                    "Stopped at a junction. Face the way you want to go and ask to go there.",
                    NamedTextColor.GOLD));
            return true;
        }
    }

    /** Holds a cart at a platform until it is sent on. */
    private record Station() implements CartMechanic {

        @Override
        public String name() {
            return "Station";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.mechanism().isNamed(name())) {
                return false;
            }
            if (!stopsFor(visit)) {
                return false;
            }

            if (visit.wiring() == Wiring.ON) {
                launch(visit);
                return false;
            }

            if (visit.distanceFromRailCentre() > STOPPING_DISTANCE_SQUARED) {
                return false;
            }
            visit.cart().stop();
            return true;
        }

        /**
         * Whether this station is one this cart stops at.
         *
         * <p>A station that names itself holds only the riders heading for that name, so an
         * express train runs through every platform but its own. One that does not name itself
         * holds everything.
         */
        private static boolean stopsFor(CartVisit visit) {
            String line = visit.mechanism().line(THIRD_LINE);
            if (line.isEmpty() || line.charAt(0) != STATION_MARKER) {
                return true;
            }
            String station = line.substring(1);
            return visit.cart()
                    .ridingPlayer()
                    .flatMap(Bystander::uniqueId)
                    .map(rider -> visit.stations().isHeadingFor(rider, station))
                    .orElse(false);
        }

        /** Sends the cart off behind the sign, as fast as it will go. */
        static void launch(CartVisit visit) {
            visit.mechanism().sign().ifPresent(sign -> visit.cart()
                    .setVelocity(Vec3d.of(sign.outward()).multiply(visit.cart().maximumSpeed())));
        }
    }

    /** Forgets where a rider said they were going. */
    private record StationClear() implements CartMechanic {

        @Override
        public String name() {
            return "StationClear";
        }

        @Override
        public boolean requiresSign() {
            return false;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (visit.isMinor()) {
                return false;
            }
            for (Bystander rider : visit.cart().riders()) {
                clear(rider, visit.stations());
            }
            return false;
        }

        private static void clear(Bystander rider, Stations stations) {
            rider.uniqueId()
                    .filter(stations::clearDestination)
                    .ifPresent(cleared -> rider.tell(Component.text(
                            "Your destination has been forgotten.", NamedTextColor.GREEN)));
        }
    }

    /** Turns a cart around. */
    private record Reverser() implements CartMechanic {

        @Override
        public String name() {
            return "Reverse";
        }

        @Override
        public boolean requiresSign() {
            return false;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || allowedThrough(visit)) {
                return false;
            }
            visit.cart().setVelocity(visit.cart().velocity().multiply(-1));
            return false;
        }

        /**
         * Whether the cart may carry on the way it was going.
         *
         * <p>Always no without a sign naming this mechanic, since a plain reverser turns back
         * everything. With one, a cart already heading the way the sign looks is left alone.
         */
        private static boolean allowedThrough(CartVisit visit) {
            if (!visit.mechanism().isNamed("Reverse")) {
                return false;
            }
            BlockFace wanted = visit.mechanism().sign().orElseThrow().facing();
            return headingOf(visit.cart()).filter(wanted::equals).isPresent();
        }

        /**
         * Which way a cart is travelling, as a direction.
         *
         * <p>The axis it is moving along fastest, so a cart coming off a curve counts as going
         * whichever way it has most nearly settled into. Nothing at all for a cart standing
         * still, which has no way to be going.
         */
        private static Optional<BlockFace> headingOf(Cart cart) {
            Vec3d velocity = cart.velocity();
            if (Math.abs(velocity.x()) > Math.abs(velocity.z())) {
                return Optional.of(velocity.x() > 0 ? BlockFace.EAST : BlockFace.WEST);
            }
            if (velocity.z() != 0) {
                return Optional.of(velocity.z() > 0 ? BlockFace.SOUTH : BlockFace.NORTH);
            }
            return Optional.empty();
        }
    }

    /**
     * Sends a cart on from a station because its redstone has just come on.
     *
     * <p>A station launches on the moment the power arrives as well as when a cart rolls in, so a
     * cart already sitting at the platform leaves when the button is pressed rather than waiting
     * for something to nudge it.
     */
    public static void launchFromStation(CartVisit visit) {
        if (visit.mechanism().isNamed("Station")) {
            Station.launch(visit);
        }
    }

    /**
     * Whether a line of a sign claims this cart.
     *
     * <p>A blank line claims nothing, which is what lets a junction name only the carts it wants
     * and let everything else go straight on.
     */
    private static boolean matches(CartVisit visit, String written) {
        return CartFilter.parse(written, visit.world()::resolveItem)
                .filter(filter -> filter.matches(visit.cart(), visit.stations()))
                .isPresent();
    }

    /**
     * Which way a rider is facing, as a cart can travel.
     *
     * <p>Used by the direction block: the rider looks the way they want to go and the junction
     * obliges if the rail runs that way.
     *
     * @param yawDegrees the rider's heading, as the game gives it
     */
    public static BlockFace facingFromYaw(float yawDegrees) {
        float turned = (yawDegrees % 360 + 360) % 360;
        if (turned < 45 || turned >= 315) {
            return BlockFace.SOUTH;
        }
        if (turned < 135) {
            return BlockFace.WEST;
        }
        if (turned < 225) {
            return BlockFace.NORTH;
        }
        return BlockFace.EAST;
    }

    /**
     * Sends a cart the way its rider asked, if the rail runs that way.
     *
     * @param cart the cart to send
     * @param shape which way the rail under it runs
     * @param wanted the way the rider wants to go
     * @param speed how fast to send it
     * @return whether the cart was sent
     */
    public static boolean sendAlong(Cart cart, RailShape shape, BlockFace wanted, double speed) {
        if (!shape.runs(wanted)) {
            return false;
        }
        return cart.setVelocity(Vec3d.of(wanted).multiply(speed));
    }
}
