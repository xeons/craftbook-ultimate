// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.listener;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import com.xeonproductions.craftbookultimate.sponge.ic.ICManager;
import com.xeonproductions.craftbookultimate.sponge.ic.Redstone;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * Setting chips off when the redstone beside them changes.
 *
 * <p>Bukkit has an event for exactly this question and Sponge has none, which was the thing that
 * decided whether this port was possible at all. What it has instead is
 * {@link ChangeBlockEvent.Post}, which carries every block change that actually happened, with what
 * the block was and what it became. Comparing the power in those two states asks the same question
 * — was it carrying a signal, is it carrying a different one now — and a change that leaves the
 * signal where it was cannot alter what any chip reads, so it is dropped here.
 *
 * <p>This fires for every block change on the server, not only redstone, so the cost of it matters.
 * It stays cheap because {@link ICManager} keeps an index from each pin block to the chips reading
 * it: a change nowhere near a chip costs one map lookup and nothing else.
 *
 * <p>Watched after the fact, deliberately. What has already happened is what the chips should read,
 * and nothing here cancels or alters a change somebody else made.
 */
@NullMarked
public final class ICRedstoneListener {

    private final ICManager manager;

    public ICRedstoneListener(ICManager manager) {
        this.manager = manager;
    }

    @Listener(order = Order.POST)
    public void onBlocksChanged(ChangeBlockEvent.Post event) {
        ServerWorld world = event.world();

        for (BlockTransactionReceipt receipt : event.receipts()) {
            int before = Redstone.powerLevel(receipt.originalBlock().state());
            int after = Redstone.powerLevel(receipt.finalBlock().state());
            if (before == after) {
                continue;
            }

            Vec3i position = Positions.toDomain(receipt.finalBlock().position());
            manager.triggerAt(world, position);
        }
    }
}
