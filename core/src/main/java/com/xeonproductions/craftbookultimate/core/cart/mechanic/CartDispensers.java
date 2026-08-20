package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.Cart;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartWorld;
import com.xeonproductions.craftbookultimate.core.cart.VehicleKind;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The dispenser, which deals with carts without being one.
 *
 * <p>Every other mechanic here is a block under a rail that a cart rolls over. This one is a chest
 * with a sign under it, and it works both ways: powered, it takes a vehicle out of the chest and
 * puts it on the track; run into by a cart, it takes the cart off the track and puts it back in
 * the chest.
 *
 * <p>That pairing is the point. One chest at the end of a line hands out carts as people arrive
 * and takes them back as they leave, so a station never runs out and never silts up.
 */
@NullMarked
public final class CartDispensers {

    /** The line naming which vehicle to put out. */
    private static final int KIND_LINE = 2;

    /** The line that may also say to push. */
    private static final int PUSH_LINE = 3;

    /** What a sign says to have the vehicle sent off rather than left standing. */
    private static final String PUSH = "push";

    /** How far from the chest a dispenser will look for somewhere to put a vehicle down. */
    private static final int SEARCH_DISTANCE = 3;

    /** What this mechanic is called, on its sign and in the settings. */
    public static final String NAME = "Dispenser";

    private CartDispensers() {}

    /**
     * Whether a sign is a dispenser's.
     *
     * @param sign the sign under the chest
     */
    public static boolean isDispenser(CartMechanism.MechanismSign sign, Settings settings) {
        return settings.carts().allows(NAME)
                && sign.lines().trimmedText(CartMechanism.MechanismSign.NAME_LINE)
                        .equalsIgnoreCase("[" + NAME + "]");
    }

    /**
     * Puts a vehicle out onto the track.
     *
     * <p>Which vehicle comes from line 3 where it says, and from whatever is in the chest where it
     * does not. The vehicle is set down on the first rail behind the sign, so a dispenser faces
     * the platform and delivers onto the line.
     *
     * @param site the chest, the sign and where they are
     * @param world the world to put it in
     * @param settings the settings in force
     * @return the vehicle, if one was put out
     */
    public static Optional<Cart> dispense(Site site, CartWorld world, Settings settings) {
        Optional<VehicleKind> asked = VehicleKind.bySignName(site.sign().lines().trimmedText(KIND_LINE));
        Optional<StoredVehicle> found = firstStored(site.chest(), asked);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        Optional<Vec3i> landing = findLanding(site, world, found.get().kind());
        if (landing.isEmpty()) {
            return Optional.empty();
        }
        if (site.chest().take(found.get().item(), 1) < 1) {
            return Optional.empty();
        }

        Optional<Cart> put = world.spawnVehicle(
                Vec3d.centreOf(landing.get()).add(new Vec3d(0, 1, 0)),
                found.get().kind(),
                Optional.empty());
        if (put.isEmpty()) {
            // Nowhere to put it after all, so the vehicle goes back in the chest rather than
            // vanishing out of it.
            site.chest().give(found.get().item(), 1);
            return Optional.empty();
        }

        if (shouldPush(site)) {
            put.get().setVelocity(
                    Vec3d.of(site.sign().outward()).multiply(settings.carts().launchSpeed()));
        }
        return put;
    }

    /**
     * Takes a cart off the track and puts it back in the chest.
     *
     * <p>What a cart running into the chest does. Anything the chest cannot take is dropped rather
     * than destroyed, so a full chest costs somebody a walk rather than a minecart.
     *
     * @return whether the cart was taken
     */
    public static boolean store(Cart cart, Stockpile chest, CartWorld world) {
        Optional<Key> item = itemFor(cart);
        if (item.isEmpty()) {
            return false;
        }

        Vec3d where = cart.position();
        if (!cart.remove()) {
            return false;
        }
        if (chest.give(item.get(), 1) > 0) {
            world.dropItem(where, item.get(), 1);
        }
        return true;
    }

    /** Whether the sign says to send the vehicle off rather than leave it standing. */
    private static boolean shouldPush(Site site) {
        return isPush(site.sign().lines().trimmedText(KIND_LINE))
                || isPush(site.sign().lines().trimmedText(PUSH_LINE));
    }

    private static boolean isPush(String written) {
        return written.trim().toLowerCase(Locale.ROOT).equals(PUSH);
    }

    /**
     * The first vehicle in the chest, of the kind asked for or of any kind.
     *
     * <p>A dispenser that has not been told what to hand out hands out whatever it has, which is
     * how one chest serves a station stocked with a mixture.
     */
    private static Optional<StoredVehicle> firstStored(Stockpile chest, Optional<VehicleKind> asked) {
        Map<Key, Integer> holding = chest.contents();
        for (VehicleKind kind : VehicleKind.values()) {
            if (asked.isPresent() && asked.get() != kind) {
                continue;
            }
            for (Map.Entry<Key, Integer> stored : holding.entrySet()) {
                if (stored.getValue() > 0 && stored.getKey().equals(kind.item())) {
                    return Optional.of(new StoredVehicle(kind, stored.getKey()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The first rail behind the sign that a vehicle can be set down on.
     *
     * <p>A boat needs no rail, so it goes down at the first place it can, one block out.
     */
    private static Optional<Vec3i> findLanding(Site site, CartWorld world, VehicleKind kind) {
        BlockFace outward = site.sign().outward();
        Vec3i from = site.chestPosition();

        for (int step = 1; step <= SEARCH_DISTANCE; step++) {
            Vec3i candidate = from.offset(outward, step);
            if (!world.isLoaded(candidate)) {
                return Optional.empty();
            }
            if (!kind.ridesOnRails() || world.isRail(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** The item a cart is stored as. */
    private static Optional<Key> itemFor(Cart cart) {
        for (VehicleKind kind : VehicleKind.values()) {
            if (kind.cartType().filter(type -> type == cart.type()).isPresent()) {
                return Optional.of(kind.item());
            }
        }
        return Optional.empty();
    }

    /**
     * A dispenser as it stands in the world.
     *
     * @param sign the sign under the chest, which says what to hand out and which way
     * @param chestPosition where the chest is
     * @param chest what is in it
     */
    public record Site(CartMechanism.MechanismSign sign, Vec3i chestPosition, Stockpile chest) {}

    /** A vehicle waiting in a chest. */
    private record StoredVehicle(VehicleKind kind, Key item) {}
}
