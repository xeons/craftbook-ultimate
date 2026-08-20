package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.mechanic.Elevator;
import com.xeonproductions.craftbookultimate.core.mechanic.PostedSign;
import com.xeonproductions.craftbookultimate.paper.mechanic.MechanicDispatcher;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Works a two-way lift for somebody standing on it who jumps or crouches.
 *
 * <p>The sign is on the side of the block being stood on, so a pad in a floor carries whoever
 * steps onto it without their having to click anything: jumping goes up and crouching goes down.
 *
 * <p>A rider is carried at most once every half second. Without that, holding crouch would drop
 * somebody through every floor of the building in the time it takes to let go, because they land
 * on the next pad still crouching.
 */
@NullMarked
public final class LiftMoveListener implements Listener {

    /** How long before the same person can be carried again by jumping or crouching. */
    private static final long REST_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    /** The sides of a block a pad's sign can be fixed to. */
    private static final BlockFace[] SIDES = {
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };

    private final MechanicDispatcher dispatcher;
    private final ConcurrentHashMap<UUID, Long> lastCarried = new ConcurrentHashMap<>();

    public LiftMoveListener(MechanicDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (!dispatcher.settings().mechanics().liftJumping()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();
        boolean rising = to.getY() > event.getFrom().getY();
        if (!rising && !player.isSneaking()) {
            return;
        }
        if (isResting(player)) {
            return;
        }

        World world = to.getWorld();
        Block ground = to.getBlock().getRelative(BlockFace.DOWN);
        Optional<PostedSign> pad = twoWaySignOn(ground);
        if (pad.isEmpty()) {
            return;
        }

        if (dispatcher.rideLift(pad.get(), world, player, rising)) {
            lastCarried.put(player.getUniqueId(), System.nanoTime());
        }
    }

    /** Forgets somebody who has left, so the map does not grow with the server's uptime. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastCarried.remove(event.getPlayer().getUniqueId());
    }

    /** Whether somebody was carried too recently to be carried again. */
    private boolean isResting(Player player) {
        Long last = lastCarried.get(player.getUniqueId());
        return last != null && System.nanoTime() - last < REST_NANOS;
    }

    /**
     * The two-way lift sign fixed to the side of a block, if there is one.
     *
     * <p>Only a sign actually hanging on this block counts, so a sign on the far side of the
     * neighbouring block is not mistaken for a pad.
     */
    private Optional<PostedSign> twoWaySignOn(Block block) {
        for (BlockFace side : SIDES) {
            Block candidate = block.getRelative(side);
            if (!Tag.WALL_SIGNS.isTagged(candidate.getType())
                    || !(candidate.getBlockData() instanceof WallSign wallSign)
                    || wallSign.getFacing() != side) {
                continue;
            }
            Optional<PostedSign> sign = dispatcher.signAt(candidate)
                    .filter(found -> found.isNamed(Elevator.BOTH));
            if (sign.isPresent()) {
                return sign;
            }
        }
        return Optional.empty();
    }
}
