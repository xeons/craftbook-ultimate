package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import org.jspecify.annotations.NullMarked;

/**
 * Something that happens to a cart when it rolls over a particular block.
 *
 * <p>The mechanic itself is stateless: everything it needs comes in on the {@link CartVisit}, and
 * everything it remembers is written on its sign or kept in a registry both ends can reach. That
 * is what lets one be exercised against a cart built in a test with no server running.
 */
@NullMarked
public interface CartMechanic {

    /**
     * What this mechanic is called.
     *
     * <p>Used three ways, and they have to agree: it is the name in brackets on the sign, the key
     * naming the block it is built from in the settings, and the name an operator switches it off
     * by.
     */
    String name();

    /** Whether the mechanic needs a sign as well as a block. */
    boolean requiresSign();

    /**
     * What happens when a cart arrives.
     *
     * @return whether to stop the cart moving any further this tick
     */
    boolean onCart(CartVisit visit);

    /**
     * Whether this mechanic is what has been built here.
     *
     * <p>The block under the rail says which mechanic it is, and a mechanic that wants a sign is
     * not built without one. Two mechanics sharing a block tell themselves apart by also checking
     * the name on the sign.
     */
    default boolean appliesTo(CartMechanism mechanism, Settings settings) {
        if (!settings.carts().allows(name())) {
            return false;
        }
        if (requiresSign() && !mechanism.hasSign()) {
            return false;
        }
        return settings.carts()
                .blockFor(name())
                .filter(block -> block.equals(mechanism.baseBlock()))
                .isPresent();
    }
}
