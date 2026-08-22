// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.stock;

import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.registry.RegistryTypes;

/**
 * A place to take materials from and give them back to, backed by a container.
 *
 * <p>Sponge's inventories are queried and offered to rather than read and written as an array of
 * slots, so taking works slot by slot rather than by rewriting the whole contents. The outcome is
 * the same and the reason for the difference is that a partial take has to leave the rest alone.
 */
@NullMarked
public record InventoryStockpile(Inventory inventory) implements Stockpile {

    @Override
    public int count(Key item) {
        Optional<ItemType> type = typeFor(item);
        if (type.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (Slot slot : inventory.slots()) {
            ItemStack stack = slot.peek();
            if (stack.type().equals(type.get())) {
                total += stack.quantity();
            }
        }
        return total;
    }

    @Override
    public int take(Key item, int amount) {
        Optional<ItemType> type = typeFor(item);
        if (type.isEmpty() || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        for (Slot slot : inventory.slots()) {
            if (remaining <= 0) {
                break;
            }

            ItemStack stack = slot.peek();
            if (!stack.type().equals(type.get()) || stack.isEmpty()) {
                continue;
            }

            int taken = Math.min(stack.quantity(), remaining);
            remaining -= taken;

            if (taken == stack.quantity()) {
                slot.clear();
            } else {
                stack.setQuantity(stack.quantity() - taken);
                slot.set(stack);
            }
        }
        return amount - remaining;
    }

    /** Gives materials back, reporting how many would not fit. */
    @Override
    public int give(Key item, int amount) {
        Optional<ItemType> type = typeFor(item);
        if (type.isEmpty() || amount <= 0) {
            return Math.max(0, amount);
        }

        InventoryTransactionResult result = inventory.offer(ItemStack.of(type.get(), amount));
        int rejected = 0;
        for (var stack : result.rejectedItems()) {
            rejected += stack.quantity();
        }
        return rejected;
    }

    @Override
    public Map<Key, Integer> contents() {
        Map<Key, Integer> totals = new HashMap<>();
        for (Slot slot : inventory.slots()) {
            ItemStack stack = slot.peek();
            if (stack.isEmpty()) {
                continue;
            }
            totals.merge(keyOf(stack.type()), stack.quantity(), Integer::sum);
        }
        return totals;
    }

    /**
     * How much more of something would fit.
     *
     * <p>Counted as empty slots at a full stack each plus the room left in stacks already holding
     * it, which is what the game itself would use when something is put in.
     */
    @Override
    public int countRoomFor(Key item) {
        Optional<ItemType> type = typeFor(item);
        if (type.isEmpty()) {
            return 0;
        }

        int stackSize = type.get().maxStackQuantity();
        int room = 0;
        for (Slot slot : inventory.slots()) {
            ItemStack stack = slot.peek();
            if (stack.isEmpty()) {
                room += stackSize;
            } else if (stack.type().equals(type.get())) {
                room += Math.max(0, stackSize - stack.quantity());
            }
        }
        return room;
    }

    private static Optional<ItemType> typeFor(Key item) {
        return RegistryTypes.ITEM_TYPE.get().findValue(ResourceKey.of(item));
    }

    private static Key keyOf(ItemType type) {
        return RegistryTypes.ITEM_TYPE.get().valueKey(type);
    }
}
