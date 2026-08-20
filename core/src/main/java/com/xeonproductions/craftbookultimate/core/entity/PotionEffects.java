package com.xeonproductions.craftbookultimate.core.entity;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The short names signs use for potion effects.
 *
 * <p>An abbreviation is the first two letters of each word of the effect's name, so night vision
 * is {@code NIVI} and poison is {@code PO}. Signs are full of them, so the table is fixed rather
 * than derived: a rule applied to whatever the game currently offers would quietly change what an
 * existing sign means every time an effect was added.
 *
 * <p>An effect may also be named outright, as {@code regeneration} or {@code minecraft:speed}.
 * That is how the effects added since the abbreviations were fixed are reached, and how
 * regeneration is reached at all — the abbreviation it would have had is taken by resistance.
 */
@NullMarked
public final class PotionEffects {

    /** The abbreviations, as signs have always spelled them. */
    private static final Map<String, String> BY_ABBREVIATION = new LinkedHashMap<>();

    static {
        BY_ABBREVIATION.put("AB", "absorption");
        BY_ABBREVIATION.put("BL", "blindness");
        BY_ABBREVIATION.put("FIRE", "fire_resistance");
        BY_ABBREVIATION.put("GL", "glowing");
        BY_ABBREVIATION.put("HA", "haste");
        BY_ABBREVIATION.put("HEBO", "health_boost");
        BY_ABBREVIATION.put("HU", "hunger");
        BY_ABBREVIATION.put("INDA", "instant_damage");
        BY_ABBREVIATION.put("INHE", "instant_health");
        BY_ABBREVIATION.put("IN", "invisibility");
        BY_ABBREVIATION.put("JUBO", "jump_boost");
        BY_ABBREVIATION.put("LE", "levitation");
        BY_ABBREVIATION.put("LU", "luck");
        BY_ABBREVIATION.put("MIFA", "mining_fatigue");
        BY_ABBREVIATION.put("NA", "nausea");
        BY_ABBREVIATION.put("NIVI", "night_vision");
        BY_ABBREVIATION.put("PO", "poison");
        BY_ABBREVIATION.put("RE", "resistance");
        BY_ABBREVIATION.put("SA", "saturation");
        BY_ABBREVIATION.put("SL", "slowness");
        BY_ABBREVIATION.put("SP", "speed");
        BY_ABBREVIATION.put("ST", "strength");
        BY_ABBREVIATION.put("UN", "unluck");
        BY_ABBREVIATION.put("WABR", "water_breathing");
        BY_ABBREVIATION.put("WE", "weakness");
        BY_ABBREVIATION.put("WI", "wither");
    }

    private PotionEffects() {}

    /**
     * Works out which effect a sign means.
     *
     * @param written an abbreviation such as {@code NIVI}, or an effect's own name
     * @return the effect, or empty if the text names none
     */
    public static Optional<Key> resolve(String written) {
        String trimmed = written.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        String abbreviated = BY_ABBREVIATION.get(trimmed.toUpperCase(Locale.ROOT));
        if (abbreviated != null) {
            return Optional.of(Key.key(Key.MINECRAFT_NAMESPACE, abbreviated));
        }

        String named = trimmed.toLowerCase(Locale.ROOT).replace(' ', '_');
        try {
            return Optional.of(named.indexOf(':') >= 0 ? Key.key(named) : Key.key(Key.MINECRAFT_NAMESPACE, named));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Every abbreviation, in the order they were fixed. */
    public static Map<String, String> abbreviations() {
        return Map.copyOf(BY_ABBREVIATION);
    }
}
