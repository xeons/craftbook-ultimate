package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The mechanics that move things into and out of a passing cart.
 *
 * <p>Two shift items between the cart and the chests beside the track, and one puts a person in
 * the cart instead. All three act on a cart passing over rather than on anything being clicked, so
 * a loading bay is built once and then simply used.
 */
@NullMarked
public final class CartCargo {

    /** The line naming what to move, and how much of it. */
    private static final int ITEM_LINE = 2;

    /** How far to either side of the track the chests may stand. */
    private static final int CHEST_REACH = 2;

    /** How far above the rail a chest or hopper feeding the mechanism may be. */
    private static final int OVERHEAD_REACH = 3;

    /** How far from a loader somebody may be standing and still be picked up. */
    private static final double LOADING_REACH = 3;

    /** Separates an item from how many of it are wanted. */
    private static final String AMOUNT_SEPARATOR = ":";

    private CartCargo() {}

    /**
     * Empties a passing cart into the chests beside the track.
     *
     * <p>Line 3 may name a single item, optionally followed by {@code :amount}, in which case only
     * that is taken and only that much. A blank line empties the cart entirely.
     *
     * <p>Whatever the chests cannot hold stays in the cart, so an unloading bay that has filled up
     * sends the cart on still laden rather than destroying what it was carrying.
     */
    public static CartMechanic collector() {
        return new Collector();
    }

    /**
     * Fills a passing cart from the chests beside the track.
     *
     * <p>Line 3 reads as it does for a collector. Whatever the cart cannot hold is left in the
     * chest.
     */
    public static CartMechanic depositor() {
        return new Depositor();
    }

    /**
     * Puts whoever is standing nearby into an empty cart.
     *
     * <p>Needs no sign. Built at the end of a platform so that a cart arriving picks up whoever is
     * waiting, without their having to click it.
     */
    public static CartMechanic loader() {
        return new Loader();
    }

    /** Empties a passing cart into the chests beside the track. */
    private record Collector() implements CartMechanic {

        @Override
        public String name() {
            return "Collect";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (visit.isMinor() || !visit.isAllowed() || !visit.mechanism().isNamed(name())) {
                return false;
            }

            Optional<Stockpile> hold = visit.cart().contents();
            if (hold.isEmpty()) {
                return false;
            }
            Stockpile chests = chestsBeside(visit);

            shift(visit, hold.get(), chests);
            return false;
        }
    }

    /** Fills a passing cart from the chests beside the track. */
    private record Depositor() implements CartMechanic {

        @Override
        public String name() {
            return "Deposit";
        }

        @Override
        public boolean requiresSign() {
            return true;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (visit.isMinor() || !visit.isAllowed() || !visit.mechanism().isNamed(name())) {
                return false;
            }

            Optional<Stockpile> hold = visit.cart().contents();
            if (hold.isEmpty()) {
                return false;
            }
            Stockpile chests = chestsBeside(visit);

            shift(visit, chests, hold.get());
            return false;
        }
    }

    /** Puts whoever is standing nearby into an empty cart. */
    private record Loader() implements CartMechanic {

        @Override
        public String name() {
            return "Load";
        }

        @Override
        public boolean requiresSign() {
            return false;
        }

        @Override
        public boolean onCart(CartVisit visit) {
            if (!visit.hasArrived() || visit.cart().isOccupied()) {
                return false;
            }
            for (Bystander waiting : visit.world().playersNear(
                    Vec3d.centreOf(visit.mechanism().rail()), LOADING_REACH)) {
                if (visit.cart().board(waiting)) {
                    return false;
                }
            }
            return false;
        }
    }

    /**
     * Moves what the sign asked for from one store to the other.
     *
     * <p>An amount on the sign is a total rather than a total per item, so {@code :5} on a blank
     * item line moves five things and stops, whatever they were.
     */
    private static void shift(CartVisit visit, Stockpile from, Stockpile to) {
        Wanted wanted = Wanted.on(visit);
        int remaining = wanted.amount().orElse(Integer.MAX_VALUE);

        for (Map.Entry<Key, Integer> holding : List.copyOf(from.contents().entrySet())) {
            if (remaining <= 0) {
                return;
            }
            if (!wanted.covers(holding.getKey())) {
                continue;
            }
            remaining -= move(from, to, holding.getKey(), Math.min(remaining, holding.getValue()));
        }
    }

    /**
     * Moves as much of one item as will go, leaving behind whatever will not.
     *
     * <p>Taken and given in one step so that nothing is ever in flight: what the receiver refuses
     * is put straight back, rather than being taken out and dropped.
     */
    private static int move(Stockpile from, Stockpile to, Key item, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int taken = from.take(item, amount);
        if (taken <= 0) {
            return 0;
        }
        int refused = to.give(item, taken);
        if (refused > 0) {
            from.give(item, refused);
        }
        return taken - refused;
    }

    /** The chests a mechanism can reach, taken as one store. */
    private static Stockpile chestsBeside(CartVisit visit) {
        Vec3i rail = visit.mechanism().rail();
        List<Vec3i> places = new ArrayList<>();

        // A flat spread either side of the track, at the rail's own level and one below, which is
        // where a chest beside a platform or sunk into it sits.
        for (int x = -CHEST_REACH; x <= CHEST_REACH; x++) {
            for (int z = -CHEST_REACH; z <= CHEST_REACH; z++) {
                for (int y = -1; y <= 0; y++) {
                    places.add(rail.add(x, y, z));
                }
            }
        }
        // And directly overhead, for a hopper or chest feeding down onto the track.
        for (int y = 2; y <= OVERHEAD_REACH; y++) {
            places.add(rail.add(0, y, 0));
        }

        return visit.world().containersAt(places);
    }

    /**
     * What a sign has asked for.
     *
     * @param item the one item wanted, or empty for everything
     * @param amount how many at most, or empty for as many as there are
     */
    private record Wanted(Optional<Key> item, Optional<Integer> amount) {

        /** Everything the cart or the chests hold. */
        private static final Wanted EVERYTHING = new Wanted(Optional.empty(), Optional.empty());

        /** Reads {@code item}, {@code item:amount} or a blank line. */
        static Wanted on(CartVisit visit) {
            String line = visit.mechanism().line(ITEM_LINE);
            if (line.isBlank()) {
                return EVERYTHING;
            }

            int separator = line.lastIndexOf(AMOUNT_SEPARATOR);
            String named = separator < 0 ? line : line.substring(0, separator);
            Optional<Integer> amount = Optional.empty();
            if (separator >= 0) {
                try {
                    amount = Optional.of(Integer.parseInt(line.substring(separator + 1).trim()));
                } catch (NumberFormatException e) {
                    return EVERYTHING;
                }
            }

            Optional<Key> item = named.isBlank()
                    ? Optional.empty()
                    : visit.world().resolveItem(named.trim());
            if (!named.isBlank() && item.isEmpty()) {
                // A sign naming something that is not an item asks for nothing rather than for
                // everything, so a typo empties no carts.
                return new Wanted(Optional.of(NOTHING), amount);
            }
            return new Wanted(item, amount);
        }

        /** A name nothing answers to, which is how a mistyped sign moves nothing. */
        private static final Key NOTHING =
                Key.key(Key.MINECRAFT_NAMESPACE, "craftbook_nothing_at_all");

        /** Whether this is one of the items asked for. */
        boolean covers(Key candidate) {
            return item.isEmpty() || item.get().equals(candidate);
        }

    }
}
