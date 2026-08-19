package com.xeonproductions.craftbookultimate.core.entity;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A stack of items lying on the ground.
 *
 * <p>The planting chips work from these: they watch for seeds and saplings dropped nearby and
 * put them in the earth one at a time, so a farm can be fed by throwing a stack at it.
 */
@NullMarked
public interface DroppedItem {

    /** What kind of item this is. */
    Key type();

    /** How many are in the stack. */
    int count();

    /**
     * Takes one out of the stack, removing it entirely when that was the last.
     *
     * @return true if one was taken
     */
    boolean takeOne();

    /** Whether this stack is still lying there and has anything left in it. */
    boolean isPresent();
}
