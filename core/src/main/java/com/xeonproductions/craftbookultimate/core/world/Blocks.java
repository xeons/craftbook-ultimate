package com.xeonproductions.craftbookultimate.core.world;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The block keys the mechanics name directly, and the small groups they ask about.
 *
 * <p>Only blocks that some rule genuinely depends on appear here. Anything a player names on a
 * sign is parsed into a key at that point rather than being listed.
 */
@NullMarked
public final class Blocks {

    /** The namespace every vanilla block lives in. */
    public static final String MINECRAFT = "minecraft";

    public static final Key AIR_KEY = key("air");
    public static final Key CAVE_AIR = key("cave_air");
    public static final Key VOID_AIR = key("void_air");
    public static final Key WATER_KEY = key("water");
    public static final Key BUBBLE_COLUMN = key("bubble_column");
    public static final Key LAVA_KEY = key("lava");
    public static final Key LEVER = key("lever");

    /**
     * Everything that counts as empty space.
     *
     * <p>The game has three kinds of air and they are not interchangeable: cave air fills carved
     * areas and void air sits outside the world's vertical bounds.
     */
    public static final Set<Key> AIR = Set.of(AIR_KEY, CAVE_AIR, VOID_AIR);

    /**
     * Everything that counts as water.
     *
     * <p>Flowing and still water became one block with a level, so unlike the old two-block
     * arrangement only one key is needed. A bubble column is water with air running through it
     * and still drowns you, so it counts too.
     */
    public static final Set<Key> WATER = Set.of(WATER_KEY, BUBBLE_COLUMN);

    /** Everything that counts as lava, flowing or still. */
    public static final Set<Key> LAVA = Set.of(LAVA_KEY);

    private Blocks() {}

    /** A vanilla block key from its name, so {@code stone} gives {@code minecraft:stone}. */
    public static Key key(String name) {
        return Key.key(MINECRAFT, name);
    }

    /**
     * Parses a block name as written on a sign.
     *
     * <p>Accepts a bare name or a fully qualified key, in any case, and treats spaces as
     * underscores so that {@code Iron Block} names the same block as {@code iron_block}.
     *
     * @return the key, or empty if the text is not a usable block name
     */
    public static Optional<Key> parse(String text) {
        String cleaned = text.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(cleaned.indexOf(':') >= 0 ? Key.key(cleaned) : key(cleaned));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
