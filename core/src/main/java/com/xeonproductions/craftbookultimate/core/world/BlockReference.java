package com.xeonproductions.craftbookultimate.core.world;

import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A block as it is written on a sign, before it is looked up.
 *
 * <p>Signs in worlds that predate the 1.13 flattening name blocks by a numeric id and a damage
 * value, so red wool is {@code 35:14}. Newer signs name blocks the way the game does, so the same
 * block is {@code red_wool}. Both have to keep working, and telling them apart is a matter of
 * reading the text rather than of looking anything up, which is why it happens here.
 *
 * <p>Resolving a reference to an actual block needs the server's own flattening tables and so
 * happens in the platform layer.
 *
 * @param name the block's name, or its numeric id as written, depending on {@link #isNumericId()}
 * @param damage the damage value, which selected a variant before the flattening; zero if absent
 * @param numericId whether the name is a legacy numeric id rather than a block name
 */
@NullMarked
public record BlockReference(String name, int damage, boolean numericId) {

    /** Separates a block from its damage value, and a namespace from its path. */
    private static final char SEPARATOR = ':';

    /**
     * The other separator between a block and its damage value.
     *
     * <p>Two sign formats grew up side by side: the area-building chips write {@code 35:14} and
     * the ones that set a single block write {@code 35@14}. Both are on signs in the world, and
     * an {@code @} never appears in a block name, so both are read here.
     */
    private static final char AT_SEPARATOR = '@';

    public BlockReference {
        name = name.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A block reference must name something");
        }
        if (damage < 0) {
            throw new IllegalArgumentException("Damage must not be negative, got " + damage);
        }
    }

    /**
     * Reads a block reference as written on a sign.
     *
     * <p>Four spellings are accepted:
     *
     * <pre>
     *   red_wool           a block name
     *   minecraft:red_wool a fully qualified block name
     *   35:14              a legacy numeric id and damage value
     *   wool:14            a legacy block name and damage value
     *   35@14              the same, in the spelling the single-block chips use
     * </pre>
     *
     * <p>A qualified name is told apart from a name with a damage value by what follows the
     * colon: a number means a damage value, anything else means a path within a namespace. An
     * {@code @} always separates a damage value, since no block name contains one.
     *
     * @return the reference, or empty if the text names nothing usable
     */
    public static Optional<BlockReference> parse(String text) {
        String cleaned = text.trim();
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }

        int at = cleaned.lastIndexOf(AT_SEPARATOR);
        if (at >= 0) {
            return asWholeNumber(cleaned.substring(at + 1))
                    .flatMap(damage -> of(cleaned.substring(0, at), damage));
        }

        int separator = cleaned.lastIndexOf(SEPARATOR);
        if (separator < 0) {
            return of(cleaned, 0);
        }

        String before = cleaned.substring(0, separator);
        String after = cleaned.substring(separator + 1);

        Optional<Integer> damage = asWholeNumber(after);
        if (damage.isEmpty()) {
            // Not a damage value, so the colon separates a namespace from a path.
            return of(cleaned, 0);
        }
        if (before.isEmpty()) {
            return Optional.empty();
        }

        return of(before, damage.get());
    }

    private static Optional<BlockReference> of(String name, int damage) {
        String cleaned = name.trim();
        if (cleaned.isEmpty() || !hasAnyLetterOrDigit(cleaned)) {
            return Optional.empty();
        }

        Optional<Integer> id = asWholeNumber(cleaned);
        if (id.isPresent()) {
            return Optional.of(new BlockReference(cleaned, damage, true));
        }
        return Optional.of(new BlockReference(cleaned, damage, false));
    }

    /** Whether the text contains anything that could be part of a name. */
    private static boolean hasAnyLetterOrDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Integer> asWholeNumber(String text) {
        String cleaned = text.trim();
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        for (int i = 0; i < cleaned.length(); i++) {
            if (!Character.isDigit(cleaned.charAt(i))) {
                return Optional.empty();
            }
        }
        try {
            return Optional.of(Integer.parseInt(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Whether this reference names a block by its pre-flattening numeric id. */
    public boolean isNumericId() {
        return numericId;
    }

    /** Whether this reference carries a damage value, and so came from a pre-flattening sign. */
    public boolean isLegacy() {
        return numericId || damage > 0;
    }

    /** The numeric id, for a reference that has one. */
    public Optional<Integer> id() {
        return numericId ? Optional.of(Integer.parseInt(name)) : Optional.empty();
    }

    /**
     * This reference as a modern block key, ignoring any damage value.
     *
     * <p>Meaningful only for a reference that names a block rather than giving a numeric id.
     */
    public Optional<Key> asKey() {
        return numericId ? Optional.empty() : Blocks.parse(name);
    }

    @Override
    public String toString() {
        return damage > 0 ? name + SEPARATOR + damage : name;
    }
}
