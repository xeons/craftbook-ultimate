package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A question a sign asks about a cart.
 *
 * <p>Every cart mechanic that chooses between two courses asks the same question, written the same
 * way: a sorting sign puts one filter on line 3 and another on line 4 and sends the cart left or
 * right, a lift reads them as up and down, and a launcher reads them as when to hold and when to
 * let go. That is why the grammar lives here rather than in any one of them.
 *
 * <h2>The grammar</h2>
 *
 * <p>A filter is a word, sometimes followed by a colon and an argument:
 *
 * <ul>
 *   <li>{@code all} matches anything, and {@code none} or a blank line matches nothing.
 *   <li>{@code minecart}, {@code storage}, {@code powered}, {@code hopper} and {@code tnt} match
 *       one kind of cart.
 *   <li>{@code empty} or {@code unoccupied} matches a cart nobody is riding, and {@code full} or
 *       {@code occupied} one somebody is.
 *   <li>{@code player}, {@code mob} and {@code animal} ask what is riding.
 *   <li>{@code nostop} matches a player who has not said where they are going.
 *   <li>{@code ctns} matches a chest or hopper cart carrying something, and {@code !ctns} one
 *       carrying nothing.
 *   <li>{@code held:item} and {@code inv:item} ask what the rider has, {@code sci:item} what is in
 *       the cart's first slot and {@code sci+:item} what is anywhere in the cart.
 *   <li>{@code ply:name} and {@code plym:part} match the rider by name, exactly or in part, and
 *       {@code group:name} by the group they belong to.
 *   <li>{@code cart:name} and {@code cartm:part} match the name given to the cart itself.
 *   <li>{@code #station} matches a rider heading for that destination, where {@code *} stands for
 *       any run of characters.
 * </ul>
 *
 * <p>An argument beginning with {@code !} asks the opposite, so {@code ply:!Steve} matches every
 * rider but Steve and {@code #!north*} everybody not heading north.
 *
 * <p>An item is named as {@code item}, {@code item:amount}, {@code item@damage} or
 * {@code item@damage:amount}, the damage being how blocks and items were spelled before the
 * flattening. {@code none} in place of an item asks for nothing at all.
 */
@NullMarked
@FunctionalInterface
public interface CartFilter {

    /** Separates a filter's name from its argument. */
    String ARGUMENT_SEPARATOR = ":";

    /** Marks a station name rather than a filter. */
    char STATION_MARKER = '#';

    /** Turns an argument into its opposite. */
    char NEGATION = '!';

    /** Stands in for no item at all, and for no filter at all. */
    String NOTHING = "none";

    /** Separates an item from the damage value it had before the flattening. */
    char DAMAGE_SEPARATOR = '@';

    /** Matches every cart. */
    CartFilter ANY = (cart, stations) -> true;

    /** Matches no cart, which is what a blank line means. */
    CartFilter NOTHING_AT_ALL = (cart, stations) -> false;

    /**
     * Whether a cart answers this filter.
     *
     * @param cart the cart to ask about
     * @param stations where each rider has said they are going
     */
    boolean matches(Cart cart, Stations stations);

    /**
     * Reads a filter off a sign.
     *
     * <p>Empty when the line is not a filter at all, which is how a sign is refused as it is
     * written rather than being left to do nothing once built.
     *
     * @param written the line as it was typed
     * @param items resolves an item's name, including the spellings from before the flattening
     */
    static Optional<CartFilter> parse(String written, Function<String, Optional<Key>> items) {
        String line = written.trim();
        if (line.isEmpty()) {
            return Optional.of(NOTHING_AT_ALL);
        }
        if (line.charAt(0) == STATION_MARKER) {
            return line.length() > 1 ? Optional.of(headingFor(line.substring(1))) : Optional.empty();
        }

        String[] halves = line.split(ARGUMENT_SEPARATOR, 2);
        String name = halves[0].toLowerCase(Locale.ROOT);
        String argument = halves.length > 1 ? halves[1] : "";
        boolean hasArgument = halves.length > 1;

        return switch (name) {
            case "all" -> Optional.of(ANY);
            case NOTHING -> Optional.of(NOTHING_AT_ALL);
            case "minecart" -> Optional.of(ofType(CartType.RIDEABLE));
            case "storage" -> Optional.of(ofType(CartType.CHEST));
            case "powered" -> Optional.of(ofType(CartType.FURNACE));
            case "hopper" -> Optional.of(ofType(CartType.HOPPER));
            case "tnt" -> Optional.of(ofType(CartType.TNT));
            case "unoccupied", "empty" -> Optional.of((cart, stations) -> !cart.isOccupied());
            case "occupied", "full" -> Optional.of((cart, stations) -> cart.isOccupied());
            case "player" -> Optional.of((cart, stations) -> cart.ridingPlayer().isPresent());
            case "mob" -> Optional.of(riderIs(Bystander::isMonster));
            case "animal" -> Optional.of(riderIs(Bystander::isAnimal));
            case "nostop" -> Optional.of(CartFilter::withoutADestination);
            case "ctns" -> Optional.of(carrying(true));
            case "!ctns" -> Optional.of(carrying(false));
            case "held" -> itemFilter(argument, hasArgument, items, CartFilter::held);
            case "inv" -> itemFilter(argument, hasArgument, items, CartFilter::inInventory);
            case "sci" -> itemFilter(argument, hasArgument, items, CartFilter::inFirstSlot);
            case "sci+" -> itemFilter(argument, hasArgument, items, CartFilter::anywhereInCart);
            case "ply" -> named(argument, hasArgument, false);
            case "plym" -> named(argument, hasArgument, true);
            case "group" -> hasArgument ? Optional.of(inGroup(argument)) : Optional.empty();
            case "cart" -> cartNamed(argument, hasArgument, false);
            case "cartm" -> cartNamed(argument, hasArgument, true);
            default -> Optional.empty();
        };
    }

    /** Whether a line is a filter a sign may carry. */
    static boolean isWellFormed(String written, Function<String, Optional<Key>> items) {
        return parse(written, items).isPresent();
    }

    /** Matches one kind of cart. */
    private static CartFilter ofType(CartType type) {
        return (cart, stations) -> cart.type() == type;
    }

    /** Matches when whoever is riding nearest the cart answers a question about themselves. */
    private static CartFilter riderIs(java.util.function.Predicate<Bystander> question) {
        return (cart, stations) -> cart.firstRider().filter(question).isPresent();
    }

    /** Matches a player who has not said where they are going. */
    private static boolean withoutADestination(Cart cart, Stations stations) {
        return cart.ridingPlayer()
                .flatMap(Bystander::uniqueId)
                .map(rider -> stations.destination(rider).isEmpty())
                .orElse(false);
    }

    /** Matches a chest or hopper cart by whether it is carrying anything. */
    private static CartFilter carrying(boolean wanted) {
        return (cart, stations) -> {
            if (!cart.type().holdsItems()) {
                return false;
            }
            return cart.contents().map(held -> !held.contents().isEmpty()).orElse(false) == wanted;
        };
    }

    /** Matches a rider heading for a destination, or deliberately not heading for one. */
    private static CartFilter headingFor(String pattern) {
        boolean negated = pattern.charAt(0) == NEGATION;
        String wanted = negated ? pattern.substring(1) : pattern;
        return (cart, stations) -> {
            Optional<java.util.UUID> rider = cart.ridingPlayer().flatMap(Bystander::uniqueId);
            if (rider.isEmpty()) {
                // Nobody is heading anywhere, which answers "not heading there" but not "heading
                // there". A cart with no rider is not sent down somebody else's branch.
                return false;
            }
            return stations.isHeadingFor(rider.get(), wanted) != negated;
        };
    }

    /** Matches the rider by the name they go by. */
    private static Optional<CartFilter> named(String argument, boolean hasArgument, boolean partial) {
        if (!hasArgument) {
            return Optional.empty();
        }
        boolean negated = !argument.isEmpty() && argument.charAt(0) == NEGATION;
        String wanted = negated ? argument.substring(1) : argument;
        return Optional.of((cart, stations) -> cart.ridingPlayer()
                .map(rider -> (partial ? rider.name().contains(wanted) : rider.name().equals(wanted)) != negated)
                .orElse(false));
    }

    /** Matches the rider by a group somebody else decides they belong to. */
    private static CartFilter inGroup(String argument) {
        boolean negated = !argument.isEmpty() && argument.charAt(0) == NEGATION;
        String group = (negated ? argument.substring(1) : argument).toLowerCase(Locale.ROOT);
        return (cart, stations) -> cart.ridingPlayer()
                .map(rider -> rider.isInGroup(group) != negated)
                .orElse(false);
    }

    /** Matches the name somebody gave the cart itself. */
    private static Optional<CartFilter> cartNamed(String argument, boolean hasArgument, boolean partial) {
        if (!hasArgument) {
            return Optional.empty();
        }
        boolean negated = !argument.isEmpty() && argument.charAt(0) == NEGATION;
        String wanted = negated ? argument.substring(1) : argument;
        return Optional.of((cart, stations) -> cart.customName()
                .map(name -> (partial ? name.contains(wanted) : name.equals(wanted)) != negated)
                .orElse(false));
    }

    /** Builds one of the four filters that name an item, refusing a line that names none. */
    private static Optional<CartFilter> itemFilter(
            String argument,
            boolean hasArgument,
            Function<String, Optional<Key>> items,
            Function<ItemWanted, CartFilter> build) {

        if (!hasArgument) {
            return Optional.empty();
        }
        return ItemWanted.parse(argument, items).map(build);
    }

    /** Matches what the rider is holding. */
    private static CartFilter held(ItemWanted wanted) {
        return (cart, stations) -> cart.ridingPlayer()
                .map(rider -> wanted.matchesHeld(rider.heldItem()))
                .orElse(false);
    }

    /** Matches what the rider has about them. */
    private static CartFilter inInventory(ItemWanted wanted) {
        return (cart, stations) -> cart.ridingPlayer()
                .flatMap(Bystander::inventory)
                .map(wanted::matchesStore)
                .orElse(false);
    }

    /** Matches what is in the cart's first slot. */
    private static CartFilter inFirstSlot(ItemWanted wanted) {
        return (cart, stations) -> cart.type().holdsItems() && wanted.matchesHeld(cart.firstStoredItem());
    }

    /** Matches what is anywhere in the cart. */
    private static CartFilter anywhereInCart(ItemWanted wanted) {
        return (cart, stations) ->
                cart.type().holdsItems() && cart.contents().map(wanted::matchesStore).orElse(false);
    }

    /**
     * An item a filter is looking for, and how much of it.
     *
     * @param item what to look for, or empty to look for nothing at all
     * @param amount how much is wanted; a store has to hold at least this many
     * @param negated whether the filter wants the item absent rather than present
     */
    record ItemWanted(Optional<Key> item, int amount, boolean negated) {

        /** How many are wanted when the line does not say. */
        private static final int ONE = 1;

        /**
         * Reads {@code item}, {@code item:amount}, {@code item@damage} or the two together.
         *
         * <p>The damage value is part of the item's name from before the flattening, so it goes to
         * whoever resolves names rather than being kept and compared separately.
         */
        static Optional<ItemWanted> parse(String written, Function<String, Optional<Key>> items) {
            String argument = written.trim();
            boolean negated = !argument.isEmpty() && argument.charAt(0) == NEGATION;
            String wanted = negated ? argument.substring(1) : argument;
            if (wanted.isEmpty()) {
                return Optional.empty();
            }
            if (wanted.equalsIgnoreCase(NOTHING)) {
                return Optional.of(new ItemWanted(Optional.empty(), ONE, negated));
            }

            int colon = wanted.lastIndexOf(ARGUMENT_SEPARATOR);
            String named = colon < 0 ? wanted : wanted.substring(0, colon);
            int asked = ONE;
            if (colon >= 0) {
                try {
                    asked = Integer.parseInt(wanted.substring(colon + 1).trim());
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }

            int amount = Math.max(ONE, asked);
            Optional<Key> item = items.apply(named.trim());
            return item.map(key -> new ItemWanted(Optional.of(key), amount, negated));
        }

        /** Whether the item wanted is nothing at all. */
        boolean wantsNothing() {
            return item.isEmpty();
        }

        /** Whether a single item, such as what somebody is holding, is what was wanted. */
        boolean matchesHeld(Optional<ItemView> holding) {
            if (wantsNothing()) {
                return holding.isEmpty() != negated;
            }
            boolean found = holding
                    .filter(view -> view.type().equals(item.orElseThrow()) && view.count() >= amount)
                    .isPresent();
            return found != negated;
        }

        /** Whether a store, such as a rider's pack or a cart's hold, has what was wanted. */
        boolean matchesStore(Stockpile store) {
            if (wantsNothing()) {
                return store.contents().isEmpty() != negated;
            }
            return store.has(item.orElseThrow(), amount) != negated;
        }
    }
}
