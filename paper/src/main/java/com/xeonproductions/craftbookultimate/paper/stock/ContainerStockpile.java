// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.stock;

import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A stockpile backed by a container in the world.
 *
 * <p>Slots are the container's business, not the caller's: taking spreads across as many slots as
 * it needs, and giving fills partial stacks before starting new ones, the way a player would.
 *
 * <p>Belongs to the region owning the container and must only be used from that thread.
 */
@NullMarked
public record ContainerStockpile(Inventory inventory) implements Stockpile {

    @Override
    public int count(Key item) {
        Material material = materialFor(item);
        if (material == null) {
            return 0;
        }

        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (matches(stack, material)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    @Override
    public int take(Key item, int amount) {
        Material material = materialFor(item);
        if (material == null || amount <= 0) {
            return 0;
        }

        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;

        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!matches(stack, material)) {
                continue;
            }

            int taken = Math.min(stack.getAmount(), remaining);
            remaining -= taken;

            if (taken == stack.getAmount()) {
                contents[slot] = null;
            } else {
                stack.setAmount(stack.getAmount() - taken);
            }
        }

        inventory.setStorageContents(contents);
        return amount - remaining;
    }

    @Override
    public int give(Key item, int amount) {
        Material material = materialFor(item);
        if (material == null) {
            return Math.max(0, amount);
        }
        if (amount <= 0) {
            return 0;
        }

        ItemStack[] contents = inventory.getStorageContents();
        int maxStack = material.getMaxStackSize();
        int remaining = amount;

        // Top up partial stacks before opening new slots, so a container does not fragment into
        // many half-full stacks over time.
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!matches(stack, material) || stack.getAmount() >= maxStack) {
                continue;
            }

            int added = Math.min(maxStack - stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() + added);
            remaining -= added;
        }

        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            if (contents[slot] != null && contents[slot].getType() != Material.AIR) {
                continue;
            }

            int added = Math.min(maxStack, remaining);
            contents[slot] = new ItemStack(material, added);
            remaining -= added;
        }

        inventory.setStorageContents(contents);
        return remaining;
    }

    @Override
    public int countRoomFor(Key item) {
        Material material = materialFor(item);
        if (material == null) {
            return 0;
        }

        int maxStack = material.getMaxStackSize();
        int room = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                room += maxStack;
            } else if (stack.getType() == material) {
                room += Math.max(0, maxStack - stack.getAmount());
            }
        }
        return room;
    }

    @Override
    public Map<Key, Integer> contents() {
        Map<Key, Integer> totals = new HashMap<>();
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            totals.merge(stack.getType().getKey(), stack.getAmount(), Integer::sum);
        }
        return Map.copyOf(totals);
    }

    private static boolean matches(@Nullable ItemStack stack, Material material) {
        return stack != null && stack.getType() == material;
    }

    private static @Nullable Material materialFor(Key item) {
        Material material = Registry.MATERIAL.get(item);
        return material == null || material.isAir() ? null : material;
    }
}
