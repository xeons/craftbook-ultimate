package com.xeonproductions.craftbookultimate.core.entity;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A stack of items held in memory, so a test can drop seeds in front of a chip and see what it
 * does with them.
 */
@NullMarked
public final class SimpleDroppedItem implements DroppedItem {

    private final Key type;
    private int count;

    public SimpleDroppedItem(Key type, int count) {
        this.type = type;
        this.count = count;
    }

    /** A stack of a vanilla item named by its bare name. */
    public static SimpleDroppedItem of(String itemName, int count) {
        return new SimpleDroppedItem(Blocks.key(itemName), count);
    }

    @Override
    public Key type() {
        return type;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public int take(int amount) {
        int taken = Math.min(Math.max(amount, 0), count);
        count -= taken;
        return taken;
    }

    @Override
    public boolean isPresent() {
        return count > 0;
    }

    @Override
    public String toString() {
        return count + " x " + type.asString();
    }
}
