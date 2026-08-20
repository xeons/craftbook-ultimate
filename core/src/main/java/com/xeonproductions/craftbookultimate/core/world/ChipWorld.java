package com.xeonproductions.craftbookultimate.core.world;

import com.xeonproductions.craftbookultimate.core.effect.FireworkBurst;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Map;
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
     * @param how which way it should point and whether the neighbours are told
     * @return true if the world changed
     */
    boolean setBlockAt(Vec3i position, Key block, Placement how);

    /** Replaces the block at a position, the ordinary way. */
    default boolean setBlockAt(Vec3i position, Key block) {
        return setBlockAt(position, block, Placement.NORMAL);
    }

    /**
     * Whether a block could legally sit at a position.
     *
     * <p>The game itself decides, so a chip planting something does not need its own table of
     * what grows on what.
     */
    boolean canPlace(Vec3i position, Key block, Placement how);

    /** Whether a block could legally sit at a position, placed the ordinary way. */
    default boolean canPlace(Vec3i position, Key block) {
        return canPlace(position, block, Placement.NORMAL);
    }

    /**
     * Whether whatever grows at a position has finished growing.
     *
     * <p>True for anything that does not grow at all, so a chip can ask about any block and get a
     * sensible answer.
     */
    boolean isFullyGrown(Vec3i position);

    /**
     * What breaking the block at a position would yield.
     *
     * <p>The game's own drops, so a harvested crop gives what harvesting it by hand would give
     * rather than the crop block itself.
     */
    Map<Key, Integer> dropsAt(Vec3i position);

    /**
     * The items lying on the ground near a position.
     *
     * @param centre the block to measure from
     * @param radius how far to look, in blocks
     */
    List<DroppedItem> itemsNear(Vec3i centre, int radius);

    /**
     * Puts one or more creatures in the world.
     *
     * <p>Everything a sign can describe is spawned here, including riders and any extra data the
     * description carried. A description that names something the game does not have, or that
     * cannot be created at all, spawns nothing.
     *
     * @param at where to put them
     * @param what the description read off the sign
     * @param count how many
     * @return how many were actually put in the world
     */
    int spawn(Vec3d at, EntitySpec what, int count);

    /**
     * Drops a stack of items on the ground.
     *
     * @return true if the stack was dropped
     */
    boolean dropItem(Vec3d at, Key item, int count);

    /**
     * Throws a projectile.
     *
     * <p>The spread is the game's own notion of inaccuracy, in the same units the dispensers use,
     * so a shooter with the default spread scatters like a dispenser does. The randomness belongs
     * to the world rather than the chip, which is what keeps a chip's own behaviour repeatable.
     *
     * @param from where it leaves
     * @param projectile what to throw
     * @param direction which way, which need not be a unit vector
     * @param speed how fast
     * @param spread how far it may stray from the direction given
     * @return true if it was thrown
     */
    boolean launchProjectile(Vec3d from, Key projectile, Vec3d direction, double speed, double spread);

    /**
     * Sets off a firework.
     *
     * @param at where it launches from
     * @param burst what it looks like when it goes off
     * @param flightTicks how long it climbs first
     * @return true if it was launched
     */
    boolean launchFirework(Vec3d at, FireworkBurst burst, int flightTicks);

    /**
     * Calls down a lightning bolt.
     *
     * @return true if it struck
     */
    boolean strikeLightning(Vec3i position);

    /**
     * The creatures near a point.
     *
     * <p>Includes players, dropped stacks and vehicles, since a sign can name any of them; see
     * {@link Bystander}.
     *
     * @param centre the point to measure from
     * @param radius how far to look, in blocks
     */
    List<Bystander> bystandersNear(Vec3d centre, double radius);

    /**
     * Shows a particle to everyone who can see the place.
     *
     * @param at where it appears
     * @param particle which particle
     * @param block the block a particle that needs one takes its appearance from
     * @return true if it was shown
     */
    boolean showParticle(Vec3d at, Key particle, Optional<Key> block);

    /**
     * Plays a sound to everyone in earshot.
     *
     * @return true if it was played
     */
    boolean playSound(Vec3d at, Key sound, float volume, float pitch);

    /**
     * The pages of the first book in the container at a position.
     *
     * <p>A book is a way of giving a chip more configuration than four sign lines hold, and of
     * letting a builder change that configuration without breaking the sign. Each page is one
     * string with its line breaks intact.
     *
     * @return the pages, or nothing if there is no container there or no book in it
     */
    List<String> bookPagesAt(Vec3i position);

    /** Whether the chunk holding a position is loaded and safe to read. */
    boolean isLoaded(Vec3i position);

    /**
     * Whether a block anywhere in the world is receiving redstone power.
     *
     * <p>Unlike a chip's own pins this may name somewhere far away, which is not always readable
     * from where the asking chip runs: the place may not be loaded, or on a regionised server it
     * may belong to a thread that is not this one. Either way the answer is empty rather than a
     * guess, and a chip reading it should leave its output where it is.
     */
    Optional<Boolean> poweredAt(Vec3i position);

    /**
     * Whether something could stand in the block at a position without being inside it.
     *
     * <p>Broader than being air: a torch, tall grass, an open door or a body of water all leave
     * room, and a chip looking for somewhere to put a player wants any of them.
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
     * Works out which item a sign means.
     *
     * <p>The same two spellings as {@link #resolveBlock(String)}, since a sign written before the
     * flattening names items by number too.
     *
     * @param written the text as it appears on the sign
     * @return the item, or empty if the text names nothing that exists
     */
    default Optional<Key> resolveItem(String written) {
        return BlockReference.parse(written).flatMap(BlockReference::asKey);
    }

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
