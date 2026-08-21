// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * A stack of items lying on the ground in a real world.
 *
 * <p>Taking one from the stack shrinks it, and taking the last removes the entity, which is what
 * a player picking items up one at a time would leave behind.
 */
@NullMarked
public record BukkitDroppedItem(Item entity) implements DroppedItem {

    @Override
    public ItemView stack() {
        if (!isPresent()) {
            return ItemView.of(entity.getItemStack().getType().getKey(), 0);
        }
        return BukkitBystander.viewOf(entity.getItemStack())
                .orElseGet(() -> ItemView.of(entity.getItemStack().getType().getKey(), 0));
    }

    @Override
    public int take(int amount) {
        if (amount <= 0 || !isPresent()) {
            return 0;
        }

        ItemStack stack = entity.getItemStack();
        int taken = Math.min(amount, stack.getAmount());
        if (taken >= stack.getAmount()) {
            entity.remove();
            return taken;
        }

        stack.setAmount(stack.getAmount() - taken);
        entity.setItemStack(stack);
        return taken;
    }

    @Override
    public boolean isPresent() {
        return entity.isValid() && entity.getItemStack().getAmount() > 0;
    }
}
