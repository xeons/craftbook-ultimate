package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Runs chips when the redstone around them changes.
 *
 * <p>The event fires for the block whose signal changed, which the manager turns into the set of
 * chips reading that block. Chips run during the redstone update rather than a tick later, so a
 * chain of them settles within the same update, as builders expect.
 */
@NullMarked
public final class ICRedstoneListener implements Listener {

    private final ICManager manager;

    public ICRedstoneListener(ICManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        // A change that leaves the signal where it was cannot alter what any chip reads.
        if (event.getOldCurrent() == event.getNewCurrent()) {
            return;
        }

        manager.triggerAt(event.getBlock());
    }
}
