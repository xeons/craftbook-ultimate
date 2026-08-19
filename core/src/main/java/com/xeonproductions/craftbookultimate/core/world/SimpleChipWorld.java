package com.xeonproductions.craftbookultimate.core.world;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A world held entirely in memory.
 *
 * <p>Everything not explicitly placed is air, so a test describes only the blocks it cares about.
 * That makes a rule about the world assertable without a server: place a block, run the chip,
 * check what changed.
 *
 * <p>Instances are not thread safe.
 */
@NullMarked
public final class SimpleChipWorld implements ChipWorld {

    private static final int DEFAULT_MIN_HEIGHT = -64;
    private static final int DEFAULT_MAX_HEIGHT = 320;
    private static final int DEFAULT_LIGHT = 15;

    private final Map<Vec3i, Key> blocks = new HashMap<>();
    private final Map<Vec3i, Integer> light = new HashMap<>();
    private final Set<Vec3i> unloaded = new HashSet<>();

    private int minHeight = DEFAULT_MIN_HEIGHT;
    private int maxHeight = DEFAULT_MAX_HEIGHT;
    private int ambientLight = DEFAULT_LIGHT;
    private boolean raining;
    private boolean thundering;
    private int rainDuration;
    private int thunderDuration;
    private long worldTicks;

    @Override
    public Key blockAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Blocks.AIR_KEY;
        }
        return blocks.getOrDefault(position, Blocks.AIR_KEY);
    }

    @Override
    public boolean setBlockAt(Vec3i position, Key block) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        Key previous = blocks.put(position, block);
        return !block.equals(previous);
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return !unloaded.contains(position);
    }

    @Override
    public int lightLevel(Vec3i position) {
        return light.getOrDefault(position, ambientLight);
    }

    @Override
    public boolean isRaining() {
        return raining;
    }

    @Override
    public boolean isThundering() {
        return thundering;
    }

    @Override
    public void setRaining(boolean raining, int durationTicks) {
        this.raining = raining;
        this.rainDuration = durationTicks;
    }

    @Override
    public void setThundering(boolean thundering, int durationTicks) {
        this.thundering = thundering;
        this.thunderDuration = durationTicks;
    }

    @Override
    public void setWorldTicks(long worldTicks) {
        this.worldTicks = worldTicks;
    }

    /** The world age this world was last set to. */
    public long worldTicks() {
        return worldTicks;
    }

    /** How long the current precipitation was set to last. */
    public int rainDuration() {
        return rainDuration;
    }

    /** How long the current thunderstorm was set to last. */
    public int thunderDuration() {
        return thunderDuration;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public int maxHeight() {
        return maxHeight;
    }

    /** Places a block. */
    public SimpleChipWorld withBlock(Vec3i position, Key block) {
        blocks.put(position, block);
        return this;
    }

    /** Places a vanilla block by its bare name. */
    public SimpleChipWorld withBlock(Vec3i position, String blockName) {
        return withBlock(position, Blocks.key(blockName));
    }

    /** Sets the light level at one position, overriding the ambient level. */
    public SimpleChipWorld withLight(Vec3i position, int level) {
        light.put(position, level);
        return this;
    }

    /** Sets the light level everywhere that has not been given one of its own. */
    public SimpleChipWorld withAmbientLight(int level) {
        this.ambientLight = level;
        return this;
    }

    /** Marks a position as being in an unloaded chunk. */
    public SimpleChipWorld withUnloaded(Vec3i position) {
        unloaded.add(position);
        return this;
    }

    /** Sets the weather. */
    public SimpleChipWorld withWeather(boolean raining, boolean thundering) {
        setWeather(raining, thundering);
        return this;
    }

    /** Sets the world's vertical bounds. */
    public SimpleChipWorld withHeights(int minHeight, int maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        return this;
    }

    /** The number of blocks explicitly placed, which is what a test asserts changes against. */
    public int placedBlockCount() {
        return blocks.size();
    }
}
