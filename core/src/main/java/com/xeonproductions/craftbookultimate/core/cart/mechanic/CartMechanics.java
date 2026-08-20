package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Every cart mechanic, in the order a passing cart meets them.
 *
 * <p>The order matters where two could act on the same cart in the same tick. Speed comes first,
 * so a booster's push is what a later mechanic sees; routing next, so the rail is bent before
 * anything can stop the cart on it; and the ones that move goods or people last, since they never
 * change where the cart is going.
 *
 * <p>The ejector is last of all, after the printer: somebody turned out onto a platform should
 * have read whatever the sign beside them had to say first.
 *
 * <p>Every applicable mechanic runs even after one has asked for the cart to be held, matching how
 * each of them used to watch carts independently of the others.
 */
@NullMarked
public final class CartMechanics {

    private static final List<CartMechanic> ALL = List.of(
            CartSpeed.booster(),
            CartSpeed.delay(),
            CartSpeed.launcher(),
            CartRouting.sorter(),
            CartRouting.reverser(),
            CartRouting.lift(),
            CartRouting.direction(),
            CartRouting.station(),
            CartRouting.stationClear(),
            CartCargo.collector(),
            CartCargo.depositor(),
            CartCrafting.crafter(),
            CartCargo.loader(),
            CartMessages.printer(),
            CartRiders.ejector());

    private CartMechanics() {}

    /** Every mechanic a cart may roll over. */
    public static List<CartMechanic> all() {
        return ALL;
    }

    /** The names of every mechanic, which is what an operator switches them off by. */
    public static List<String> names() {
        return ALL.stream().map(CartMechanic::name).toList();
    }
}
