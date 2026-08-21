// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.pipe;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.pipe.PipeNetwork;
import com.xeonproductions.craftbookultimate.core.pipe.PipeNetworks;
import com.xeonproductions.craftbookultimate.core.pipe.PipeStyle;
import com.xeonproductions.craftbookultimate.core.pipe.Pipes;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * The single way a pipe is set going.
 *
 * <p>Both ways of building one come through here, in the same way every minecart mechanic comes
 * through one dispatcher. What differs between them is decided in the domain; what is left here is
 * pulling a stack out of one container and offering it to another.
 *
 * <p>Whole stacks travel. What a pipe carries keeps whatever was done to it — its name, its
 * enchantments, its damage — because the stack itself is moved rather than a count of a kind of
 * item, and nothing in the domain ever looks at more of it than what sort of thing it is.
 */
@NullMarked
public final class PipeDispatcher {

    private final Configuration configuration;
    private final PipeNetworks networks;

    public PipeDispatcher(Configuration configuration, PipeNetworks networks) {
        this.configuration = configuration;
        this.networks = networks;
    }

    /** What each pipe was last found to reach. */
    public PipeNetworks networks() {
        return networks;
    }

    /**
     * Whether a block could be the head of a pipe.
     *
     * <p>The cheap half of the question, asked of every piece of redstone that changes. Whether it
     * really is one takes reading its sign, which {@link #run} does and this does not.
     */
    public boolean couldBeInput(Block block) {
        return PipeStyle.couldStartAPipe(block.getType().getKey());
    }

    /**
     * Sets a pipe going.
     *
     * <p>Everything happens on the thread that owns the input, which is the thread that owns the
     * whole pipe: a pipe is a line of touching blocks, so it never spans two regions.
     *
     * @return whether anything was carried
     */
    public boolean run(Block input) {
        PipeSettings settings = configuration.settings().pipes();
        if (!settings.enabled()) {
            return false;
        }

        World world = input.getWorld();
        BukkitPipeWorld seen = new BukkitPipeWorld(world);
        Vec3i at = Positions.toDomain(input);

        PipeNetwork network = networks.from(seen, world.getUID(), at, settings);
        if (!network.reachesAnywhere()) {
            return false;
        }

        Optional<Inventory> source = Pipes.sourceFor(seen, at)
                .map(position -> Positions.toBlock(world, position))
                .flatMap(PipeDispatcher::inventoryOf);
        if (source.isEmpty()) {
            return false;
        }

        return carry(world, source.get(), network, settings);
    }

    /**
     * Takes what the source holds and puts it wherever the pipe reaches.
     *
     * <p>A stack that nowhere will take is left where it was, so a pipe pointed at a full network
     * quietly does nothing rather than spilling its contents on the floor.
     */
    private static boolean carry(
            World world, Inventory source, PipeNetwork network, PipeSettings settings) {
        boolean carried = false;
        for (ItemStack stack : takeableFrom(source, settings)) {
            ItemStack left = offerAlong(world, network, stack);
            int moved = stack.getAmount() - (left == null ? 0 : left.getAmount());
            if (moved <= 0) {
                continue;
            }
            source.removeItem(withAmount(stack, moved));
            carried = true;
        }
        return carried;
    }

    /** What a single pulse is allowed to move. */
    private static List<ItemStack> takeableFrom(Inventory source, PipeSettings settings) {
        List<ItemStack> found = new ArrayList<>();
        for (ItemStack stack : source.getContents()) {
            if (stack == null || stack.getAmount() <= 0) {
                continue;
            }
            found.add(stack.clone());
            if (settings.stackPerPull()) {
                break;
            }
        }
        return found;
    }

    /**
     * Offers a stack to each way out in turn.
     *
     * @return what nobody would take, or null if all of it went
     */
    private static ItemStack offerAlong(World world, PipeNetwork network, ItemStack stack) {
        ItemStack carrying = stack.clone();
        for (PipeNetwork.Delivery delivery : network.deliveriesFor(stack.getType().getKey())) {
            Optional<Inventory> into =
                    inventoryOf(Positions.toBlock(world, delivery.container()));
            if (into.isEmpty()) {
                continue;
            }
            carrying = into.get() instanceof FurnaceInventory furnace
                    ? offerToFurnace(furnace, delivery.face(), carrying)
                    : leftoverOf(into.get().addItem(carrying));
            if (carrying == null) {
                return null;
            }
        }
        return carrying;
    }

    /**
     * Fills a furnace by the side items arrive at, the way a hopper does.
     *
     * <p>Down through the top is what is being smelted and in from any side is fuel, so one pipe
     * over the top of a row of furnaces and another along their side keeps them lit and fed
     * without either having to say which is which.
     *
     * @param arrivingBy the way the items are travelling, so the top is reached by going down
     * @return what would not fit, or null if all of it did
     */
    private static ItemStack offerToFurnace(
            FurnaceInventory furnace, com.xeonproductions.craftbookultimate.core.math.BlockFace
                    arrivingBy, ItemStack stack) {
        boolean smelting =
                arrivingBy == com.xeonproductions.craftbookultimate.core.math.BlockFace.DOWN;
        ItemStack held = smelting ? furnace.getSmelting() : furnace.getFuel();

        if (held != null && !held.isEmpty() && !held.isSimilar(stack)) {
            return stack;
        }

        int already = held == null || held.isEmpty() ? 0 : held.getAmount();
        int room = stack.getMaxStackSize() - already;
        if (room <= 0) {
            return stack;
        }

        int fitting = Math.min(room, stack.getAmount());
        ItemStack put = withAmount(stack, already + fitting);
        if (smelting) {
            furnace.setSmelting(put);
        } else {
            furnace.setFuel(put);
        }
        return fitting == stack.getAmount() ? null : withAmount(stack, stack.getAmount() - fitting);
    }

    /** What an inventory would not take, or null if it took the lot. */
    private static ItemStack leftoverOf(java.util.Map<Integer, ItemStack> rejected) {
        for (ItemStack left : rejected.values()) {
            return left;
        }
        return null;
    }

    private static ItemStack withAmount(ItemStack stack, int amount) {
        ItemStack copy = stack.clone();
        copy.setAmount(amount);
        return copy;
    }

    private static Optional<Inventory> inventoryOf(Block block) {
        return block.getState(false) instanceof InventoryHolder holder
                ? Optional.of(holder.getInventory())
                : Optional.empty();
    }

    /** Where a block is, as a place in the world. */
    public static Location locationOf(World world, Vec3i position) {
        return new Location(world, position.x(), position.y(), position.z());
    }
}
