package com.xeonproductions.craftbookultimate.core.world;

import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The world as a chip or mechanic sees it.
 *
 * <p>Blocks are named by their namespaced key, {@code minecraft:stone} and the like, which is
 * both how the game names them and how players write them on signs. Nothing here exposes a
 * server type, so the rules about what a chip does to the world can be tested against
 * {@link SimpleChipWorld} rather than a running server.
 *
 * <p>An implementation backed by a real world belongs to one region and must only be used from
 * the thread owning the blocks it touches.
 */
@NullMarked
public interface ChipWorld {

    /**
     * What identifies this world to the rest of the server.
     *
     * <p>Needed by the chips that act across worlds, which have to name a world without holding
     * on to one.
     */
    UUID id();

    /** The key of the block at a position. Unloaded or out-of-bounds positions read as air. */
    Key blockAt(Vec3i position);

    /**
     * Replaces the block at a position.
     *
     * <p>Does nothing when the position is not loaded, so a mechanic cannot force chunks in by
     * reaching past the edge of what is already there.
     *
     * @param position where to place
     * @param block the block's key
     * @return true if the world changed
     */
    boolean setBlockAt(Vec3i position, Key block);

    /** Whether the chunk holding a position is loaded and safe to read. */
    boolean isLoaded(Vec3i position);

    /**
     * Whether something could stand in the block at a position without being inside it.
     *
     * <p>Broader than being air: a torch, a carpet or an open door all leave room, and a chip
     * looking for somewhere to put a player wants any of them.
     */
    boolean isPassable(Vec3i position);

    /**
     * The people standing in the block at a position.
     *
     * <p>Ordinary mobs are not included; see {@link Traveller}.
     */
    List<Traveller> travellersIn(Vec3i position);

    /**
     * Releases a pressed pressure plate.
     *
     * <p>A plate normally stays pressed until whatever is standing on it moves off, which never
     * happens when a chip takes that person somewhere else instead. Releasing it by hand is what
     * lets a teleport pad built from a plate fire again straight away.
     *
     * @return true if there was a pressed plate there and it is now released
     */
    boolean releasePressurePlate(Vec3i position);

    /**
     * The light level at a position, from 0 to 15.
     *
     * <p>This is the combined light a player would see by, not the sky or block component alone.
     */
    int lightLevel(Vec3i position);

    /** Whether it is raining or snowing. */
    boolean isRaining();

    /** Whether a thunderstorm is running. */
    boolean isThundering();

    /**
     * Starts or stops precipitation.
     *
     * @param raining whether precipitation should fall
     * @param durationTicks how long before the weather is free to change on its own
     */
    void setRaining(boolean raining, int durationTicks);

    /**
     * Starts or stops a thunderstorm.
     *
     * @param thundering whether a thunderstorm should run
     * @param durationTicks how long before the weather is free to change on its own
     */
    void setThundering(boolean thundering, int durationTicks);

    /**
     * Sets the world's age in ticks, which moves the time of day with it.
     *
     * <p>Given as a full age rather than a time of day so that a caller can move the world
     * forward into the next day rather than backwards within the current one.
     */
    void setWorldTicks(long worldTicks);

    /**
     * Sets both kinds of weather at once, leaving each free to change again after a day.
     *
     * @param raining whether precipitation should fall
     * @param thundering whether a thunderstorm should run
     */
    default void setWeather(boolean raining, boolean thundering) {
        setRaining(raining, (int) TICKS_PER_DAY);
        setThundering(thundering, (int) TICKS_PER_DAY);
    }

    /** The length of a Minecraft day in ticks. */
    long TICKS_PER_DAY = 24_000L;

    /**
     * Works out which block a sign means.
     *
     * <p>Signs written before the 1.13 flattening name blocks by a numeric id and damage value,
     * and only the server holds the tables that map those onto modern blocks. A world backed by
     * a real server resolves them; this default understands modern names only.
     *
     * @param written the text as it appears on the sign
     * @return the block, or empty if the text names nothing that exists
     */
    default Optional<Key> resolveBlock(String written) {
        return BlockReference.parse(written).flatMap(BlockReference::asKey);
    }

    /** The lowest y coordinate that can hold a block. */
    int minHeight();

    /** One past the highest y coordinate that can hold a block. */
    int maxHeight();

    /** Whether a position is within the world's vertical bounds. */
    default boolean isInBounds(Vec3i position) {
        return position.y() >= minHeight() && position.y() < maxHeight();
    }

    /**
     * The first block at or above a position that something could stand in.
     *
     * <p>Chips that put a player somewhere measure from a solid block and rise out of it, so that
     * a pad built into a floor delivers people onto the floor rather than inside it.
     *
     * @return the position, or empty if everything up to the top of the world is solid
     */
    default Optional<Vec3i> firstPassableAtOrAbove(Vec3i start) {
        for (int y = start.y(); y < maxHeight(); y++) {
            Vec3i candidate = new Vec3i(start.x(), y, start.z());
            if (isPassable(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Whether the block at a position is air. */
    default boolean isAir(Vec3i position) {
        return Blocks.AIR.contains(blockAt(position));
    }

    /** Whether the block at a position is water, flowing or still. */
    default boolean isWater(Vec3i position) {
        return Blocks.WATER.contains(blockAt(position));
    }

    /** Whether the block at a position is lava, flowing or still. */
    default boolean isLava(Vec3i position) {
        return Blocks.LAVA.contains(blockAt(position));
    }
}
