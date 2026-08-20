package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.mechanic.CartDispensers;
import com.xeonproductions.craftbookultimate.core.cart.mechanic.CartRouting;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.cart.BukkitCartWorld;
import com.xeonproductions.craftbookultimate.paper.cart.CartBlocks;
import com.xeonproductions.craftbookultimate.paper.cart.CartDispatcher;
import com.xeonproductions.craftbookultimate.paper.cart.CartMechanisms;
import com.xeonproductions.craftbookultimate.paper.cart.SpreadStockpile;
import java.util.List;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NullMarked;

/**
 * Sets off the two cart mechanics that answer to redstone rather than to a passing cart.
 *
 * <p>A station launches whatever is sitting at it, and a dispenser hands a cart out. Both act on
 * the moment power arrives rather than while it is held, so holding a button down sends one cart
 * rather than a stream of them.
 *
 * <p>Only the blocks touching the one whose power changed are looked at, so an ordinary redstone
 * contraption costs a handful of type checks and nothing more.
 */
@NullMarked
public final class CartRedstoneListener implements Listener {

    /** The sides power may arrive from, and the block itself. */
    private static final BlockFace[] NEIGHBOURS = {
        BlockFace.SELF,
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST,
        BlockFace.UP,
        BlockFace.DOWN
    };

    /** How far a cart may be from a station's rail and still be launched by it. */
    private static final double LAUNCH_REACH = 1.5;

    private final CartDispatcher dispatcher;
    private final Configuration configuration;

    public CartRedstoneListener(CartDispatcher dispatcher, Configuration configuration) {
        this.dispatcher = dispatcher;
        this.configuration = configuration;
    }

    /** Acts on the moment power arrives at something a cart mechanic is built from. */
    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (event.getOldCurrent() > 0 || event.getNewCurrent() <= 0) {
            return;
        }
        Block changed = event.getBlock();
        if (!configuration.settings().allowsWorld(changed.getWorld().getName())) {
            return;
        }

        for (BlockFace side : NEIGHBOURS) {
            Block neighbour = changed.getRelative(side);
            if (CartBlocks.isRail(neighbour.getType())) {
                launchStationAt(neighbour);
            } else if (neighbour.getState(false) instanceof InventoryHolder) {
                dispenseFrom(neighbour);
            }
        }
    }

    /** Sends off whatever is sitting at a station whose power has just come on. */
    private void launchStationAt(Block rail) {
        Optional<CartMechanism> mechanism = CartMechanisms.atRail(rail);
        if (mechanism.isEmpty() || !mechanism.get().isNamed("Station")) {
            return;
        }
        for (var entity : rail.getWorld().getNearbyEntities(
                rail.getLocation().add(0.5, 0.5, 0.5), LAUNCH_REACH, LAUNCH_REACH, LAUNCH_REACH)) {
            if (entity instanceof Minecart minecart) {
                dispatcher.atCart(minecart, CartRouting::launchFromStation);
            }
        }
    }

    /**
     * Hands a vehicle out of a chest whose power has just come on.
     *
     * <p>The sign is under the chest, one or two blocks down, which is where a builder can reach
     * to write on it without having to break the chest.
     */
    private void dispenseFrom(Block chest) {
        Optional<CartMechanism.MechanismSign> sign = signUnder(chest);
        if (sign.isEmpty() || !CartDispensers.isDispenser(sign.get(), configuration.settings())) {
            return;
        }
        if (!(chest.getState(false) instanceof InventoryHolder holder)) {
            return;
        }

        Stockpile stock = new SpreadStockpile(List.of(holder.getInventory()));
        CartDispensers.dispense(
                new CartDispensers.Site(sign.get(), Positions.toDomain(chest), stock),
                new BukkitCartWorld(chest.getWorld(), dispatcher.recipes()),
                configuration.settings());
    }

    /** The sign a dispenser's chest sits on. */
    private static Optional<CartMechanism.MechanismSign> signUnder(Block chest) {
        for (int down = 1; down <= 2; down++) {
            Block below = chest.getRelative(BlockFace.DOWN, down);
            if (CartBlocks.isSign(below)) {
                return CartBlocks.readSign(below);
            }
        }
        return Optional.empty();
    }
}
