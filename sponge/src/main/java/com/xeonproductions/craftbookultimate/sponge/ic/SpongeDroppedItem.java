// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryTypes;

/** An item lying on the ground, as a chip that collects things sees it. */
@NullMarked
public record SpongeDroppedItem(Item entity) implements DroppedItem {

    @Override
    public ItemView stack() {
        Optional<ItemStackSnapshot> held = entity.get(Keys.ITEM_STACK_SNAPSHOT);
        if (held.isEmpty()) {
            return ItemView.of(net.kyori.adventure.key.Key.key("minecraft:air"), 0);
        }
        if (!isPresent()) {
            return ItemView.of(SpongeBystander.keyOf(held.get().type()), 0);
        }
        return SpongeBystander.viewOf(held.get());
    }

    /**
     * Takes some of the stack, and reports how much was actually there to take.
     *
     * <p>Taking all of it removes the item rather than leaving an empty one lying about, which is
     * what the game does when somebody picks the last of a stack up.
     */
    @Override
    public int take(int amount) {
        if (amount <= 0 || !isPresent()) {
            return 0;
        }

        ItemStackSnapshot held = entity.require(Keys.ITEM_STACK_SNAPSHOT);
        int taken = Math.min(amount, held.quantity());
        if (taken >= held.quantity()) {
            entity.remove();
            return taken;
        }

        ItemStack left = held.asMutable();
        left.setQuantity(held.quantity() - taken);
        entity.offer(Keys.ITEM_STACK_SNAPSHOT, left.asImmutable());
        return taken;
    }

    @Override
    public boolean isPresent() {
        return !entity.isRemoved()
                && entity.get(Keys.ITEM_STACK_SNAPSHOT).filter(held -> held.quantity() > 0).isPresent();
    }
}
