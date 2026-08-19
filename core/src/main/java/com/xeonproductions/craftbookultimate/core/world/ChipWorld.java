package com.xeonproductions.craftbookultimate.core.world;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
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
     * Sets the weather.
     *
     * @param raining whether precipitation should fall
     * @param thundering whether a thunderstorm should run
     */
    void setWeather(boolean raining, boolean thundering);

    /** The lowest y coordinate that can hold a block. */
    int minHeight();

    /** One past the highest y coordinate that can hold a block. */
    int maxHeight();

    /** Whether a position is within the world's vertical bounds. */
    default boolean isInBounds(Vec3i position) {
        return position.y() >= minHeight() && position.y() < maxHeight();
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
