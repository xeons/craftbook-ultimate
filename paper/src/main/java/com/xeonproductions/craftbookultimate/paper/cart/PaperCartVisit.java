package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.cart.Cart;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartVisit;
import com.xeonproductions.craftbookultimate.core.cart.CartWorld;
import com.xeonproductions.craftbookultimate.core.cart.Stations;
import com.xeonproductions.craftbookultimate.core.cart.Wiring;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import org.jspecify.annotations.NullMarked;

/**
 * A cart arriving at a mechanism on a real server.
 *
 * <p>Assembled once per cart move and handed to every mechanic that applies, so the blocks around
 * the mechanism are read once rather than once per mechanic.
 *
 * @param cart the cart that has arrived
 * @param mechanism the blocks it has arrived at
 * @param isMinor whether it is still inside the block it was in last tick
 * @param from where it was before this move
 * @param wiring whether anything has been wired to the mechanism
 * @param world the world it is in
 * @param stations where every rider has said they are going
 * @param settings the settings in force
 * @param scheduler work bound to the region owning these blocks
 */
@NullMarked
public record PaperCartVisit(
        Cart cart,
        CartMechanism mechanism,
        boolean isMinor,
        Vec3d from,
        Wiring wiring,
        CartWorld world,
        Stations stations,
        Settings settings,
        Scheduler scheduler) implements CartVisit {}
