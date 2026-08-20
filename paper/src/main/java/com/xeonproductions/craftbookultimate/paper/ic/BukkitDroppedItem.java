package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import net.kyori.adventure.key.Key;
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
    public Key type() {
        return entity.getItemStack().getType().getKey();
    }

    @Override
    public int count() {
        return isPresent() ? entity.getItemStack().getAmount() : 0;
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
