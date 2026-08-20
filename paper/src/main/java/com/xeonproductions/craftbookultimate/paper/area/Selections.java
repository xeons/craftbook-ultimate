package com.xeonproductions.craftbookultimate.paper.area;

import com.xeonproductions.craftbookultimate.core.area.AreaAnchor;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The two corners each player has picked out.
 *
 * <p>Saving an area needs a region, and the plugin has no world editor to borrow one from, so it
 * keeps its own: two commands, two corners, and the box between them. Nothing is written down —
 * a selection lasts as long as the player is online and is theirs alone.
 */
@NullMarked
public final class Selections {

    /** How far a player may be from the block they are picking out. */
    public static final int REACH = 100;

    private final Map<UUID, Corner> first = new ConcurrentHashMap<>();
    private final Map<UUID, Corner> second = new ConcurrentHashMap<>();

    /**
     * Picks out the block a player is looking at, or the one under their feet if they are looking
     * at the sky.
     */
    public static Vec3i pointedAtBy(Player player) {
        Block looking = player.getTargetBlockExact(REACH);
        return looking != null
                ? Positions.toDomain(looking)
                : Positions.toDomain(player.getLocation()).add(0, -1, 0);
    }

    /** Sets a player's first corner. */
    public void setFirst(Player player, Vec3i at) {
        first.put(player.getUniqueId(), new Corner(player.getWorld().getUID(), at));
    }

    /** Sets a player's second corner. */
    public void setSecond(Player player, Vec3i at) {
        second.put(player.getUniqueId(), new Corner(player.getWorld().getUID(), at));
    }

    /**
     * The box between a player's two corners.
     *
     * @return the box, or nothing if either corner is unset or the two are in different worlds
     */
    public Optional<AreaAnchor> selectionOf(Player player) {
        @Nullable Corner one = first.get(player.getUniqueId());
        @Nullable Corner other = second.get(player.getUniqueId());
        if (one == null || other == null || !one.world().equals(other.world())) {
            return Optional.empty();
        }
        return Optional.of(AreaAnchor.between(one.world(), one.at(), other.at()));
    }

    /** Forgets a player's corners, which is what happens when they leave. */
    public void forget(Player player) {
        first.remove(player.getUniqueId());
        second.remove(player.getUniqueId());
    }

    /** One corner somebody has picked out. */
    private record Corner(UUID world, Vec3i at) {}
}
