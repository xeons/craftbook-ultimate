package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.effect.FireworkBurst;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DyeColours;
import com.xeonproductions.craftbookultimate.core.entity.PotionDose;
import com.xeonproductions.craftbookultimate.core.entity.PotionEffects;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that make something happen without changing the world.
 *
 * <p>A potion area doses whoever walks through it, a particle emitter decorates a build, and a
 * firework chip sets one off. None of them place or break anything.
 */
@NullMarked
public final class Effects {

    /** The line carrying the effect, the particle, or the offset to a book. */
    private static final int SUBJECT_LINE = 2;

    /** The line carrying the range and filter, or the particle's offset. */
    private static final int SETTINGS_LINE = 3;

    /** Written after the model reference to say the settings live in a book. */
    private static final char BOOK_MODE = 'B';

    /** Where the book sits when a book-driven chip's sign does not say. */
    private static final Vec3i DEFAULT_BOOK_OFFSET = new Vec3i(0, 1, 0);

    /** Separates the fields of a potion effect. */
    private static final String FIELD_SEPARATOR = ":";

    /** Separates a potion area's reach from the sort of creature it doses. */
    private static final char FILTER_SEPARATOR = '@';

    /** Written as a duration to mean the dose never wears off. */
    private static final String FOREVER = "INF";

    /** How many ticks a second of a potion effect lasts. */
    private static final int TICKS_PER_SECOND = 20;

    /** The longest a dose may be written to last, in seconds. */
    private static final int MAX_DOSE_SECONDS = 999;

    /** The strongest a dose may be. */
    private static final int MAX_AMPLIFIER = 255;

    /** How far a potion area reaches when its sign does not say. */
    private static final int DEFAULT_POTION_RANGE = 5;

    /** The furthest a potion area may reach. */
    private static final int MAX_POTION_RANGE = 32;

    /** The furthest a particle may be shown from the block the sign hangs on. */
    private static final int MAX_PARTICLE_OFFSET = 32;

    /** Separates a particle from the block it takes its appearance from. */
    private static final char PARTICLE_BLOCK_SEPARATOR = ':';

    /** How long a firework climbs before it goes off. */
    private static final int FIREWORK_FLIGHT_TICKS = 1;

    private Effects() {}

    /**
     * Gives potion effects to whatever is standing in an area.
     *
     * <p>Line 3 reads {@code effect:seconds:strength}, where the effect is its short name such as
     * {@code NIVI} for night vision or {@code SP} for speed, and the duration may be {@code INF} to
     * mean it never wears off. Line 4 reads {@code range[:x:y:z][@filter]}, where the offset moves
     * the middle of the area and the filter is {@code P} for players, {@code M} for hostile mobs,
     * {@code A} for animals or {@code E} for everything alive. Players are the default.
     *
     * <p>Writing {@code B} after the model reference reads all of that out of a book instead, one
     * setting per line, which lets an area carry more effects than a sign has room for. Line 3 then
     * gives the offset to the container holding the book, and defaults to the block above.
     *
     * <p>Effects are added to whatever a creature already has rather than replacing them, so
     * walking through does not strip the potions somebody drank.
     */
    public static SelfTriggeringICLogic potionArea() {
        return new PotionArea();
    }

    /**
     * Shows a particle.
     *
     * <p>Line 3 names the particle, and for the ones that take their appearance from a block adds
     * it after a colon, as {@code block:minecraft:redstone_block}. Line 4 moves it off the block
     * the sign hangs on, written as an axis letter and a distance such as {@code Y3}.
     */
    public static ICLogic particleEmitter() {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            String written = state.sign().trimmedText(SUBJECT_LINE);
            if (written.isEmpty()) {
                return;
            }

            int separator = written.indexOf(PARTICLE_BLOCK_SEPARATOR);
            String named = written;
            Optional<Key> block = Optional.empty();
            if (separator >= 0) {
                String tail = written.substring(separator + 1);
                Optional<Key> resolved = state.world().resolveBlock(tail);
                if (resolved.isPresent()) {
                    named = written.substring(0, separator);
                    block = resolved;
                }
            }

            Optional<Key> particle = Blocks.parse(named);
            if (particle.isEmpty()) {
                return;
            }

            Vec3d at = Vec3d.centreOf(state.backPosition()).add(particleOffset(state));
            state.world().showParticle(at, particle.get(), block);
        };
    }

    /**
     * Sets off a firework.
     *
     * <p>Launched straight up from the block the sign hangs on, bursting almost at once so it goes
     * off where a builder put it rather than drifting first. The colour is picked at random from
     * the sixteen dyes, so a bank of these makes a varied display without every sign having to
     * differ.
     *
     * @param random where the colour comes from
     */
    public static ICLogic fireworks(RandomGenerator random) {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            int colour = FIREWORK_PALETTE[random.nextInt(FIREWORK_PALETTE.length)];
            FireworkBurst burst =
                    new FireworkBurst(
                            FireworkBurst.Shape.BALL_LARGE, List.of(colour), List.of(), false, true);
            state.world()
                    .launchFirework(Vec3d.middleOf(state.backPosition()), burst, FIREWORK_FLIGHT_TICKS);
        };
    }

    /**
     * The sixteen dye colours as the game paints them, which is what a firework may be.
     *
     * <p>Listed in the order {@link DyeColours} names them, so the two agree about which colour a
     * number means.
     */
    private static final int[] FIREWORK_PALETTE = {
        0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA,
        0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
        0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA,
        0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
    };

    /** Doses whatever is in range, on a pulse or on every tick. */
    private static final class PotionArea implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                dose(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            dose(state);
        }

        private static void dose(ChipState state) {
            Recipe recipe = readsFromBook(state) ? Recipe.fromBook(state) : Recipe.fromSign(state);
            if (recipe.doses().isEmpty()) {
                return;
            }

            Vec3d centre = Vec3d.centreOf(state.backPosition().add(recipe.offset()));
            for (Bystander bystander : state.world().bystandersNear(centre, recipe.range())) {
                if (recipe.filter().covers(bystander)) {
                    bystander.applyEffects(recipe.doses());
                }
            }
        }

        private static boolean readsFromBook(ChipState state) {
            String mode = state.modeText();
            return !mode.isEmpty() && mode.charAt(mode.length() - 1) == BOOK_MODE;
        }
    }

    /** Which creatures a potion area treats. */
    private enum Filter {
        PLAYERS('P'),
        MONSTERS('M'),
        ANIMALS('A'),
        EVERYTHING('E');

        private final char letter;

        Filter(char letter) {
            this.letter = letter;
        }

        boolean covers(Bystander bystander) {
            return switch (this) {
                case PLAYERS -> bystander.isPlayer();
                case MONSTERS -> bystander.isMonster();
                case ANIMALS -> bystander.isAnimal();
                case EVERYTHING -> bystander.isLiving();
            };
        }

        static Filter parse(String written, Filter fallback) {
            String trimmed = written.trim().toUpperCase(Locale.ROOT);
            if (trimmed.length() != 1) {
                return fallback;
            }
            for (Filter filter : values()) {
                if (filter.letter == trimmed.charAt(0)) {
                    return filter;
                }
            }
            return fallback;
        }
    }

    /**
     * Everything a potion area was told to do.
     *
     * @param doses the effects to give
     * @param range how far the area reaches
     * @param offset how far its middle sits from the block the sign hangs on
     * @param filter which creatures it treats
     */
    private record Recipe(List<PotionDose> doses, int range, Vec3i offset, Filter filter) {

        /** Nothing to do, which is what a chip with an unreadable sign has. */
        static final Recipe NOTHING = new Recipe(List.of(), DEFAULT_POTION_RANGE, Vec3i.ZERO, Filter.PLAYERS);

        static Recipe fromSign(ChipState state) {
            Optional<PotionDose> dose = parseDose(state.sign().trimmedText(SUBJECT_LINE));
            if (dose.isEmpty()) {
                return NOTHING;
            }
            Recipe area = parseArea(state.sign().trimmedText(SETTINGS_LINE), NOTHING);
            return new Recipe(List.of(dose.get()), area.range(), area.offset(), area.filter());
        }

        static Recipe fromBook(ChipState state) {
            Vec3i where =
                    state.backPosition()
                            .add(offsetOn(state.sign().trimmedText(SUBJECT_LINE)).orElse(DEFAULT_BOOK_OFFSET));

            List<PotionDose> doses = new ArrayList<>();
            Recipe area = NOTHING;
            for (String page : state.world().bookPagesAt(where)) {
                for (String line : page.split("\\R")) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    // A line beginning with a digit is the area, everything else is an effect.
                    if (Character.isDigit(trimmed.charAt(0))) {
                        area = parseArea(trimmed, area);
                    } else {
                        parseDose(trimmed).ifPresent(doses::add);
                    }
                }
            }

            return doses.isEmpty() ? NOTHING : new Recipe(doses, area.range(), area.offset(), area.filter());
        }

        /** Reads {@code effect:seconds:strength}. */
        private static Optional<PotionDose> parseDose(String written) {
            String[] parts = written.split(FIELD_SEPARATOR);
            if (parts.length != 3) {
                return Optional.empty();
            }

            Optional<Key> effect = PotionEffects.resolve(parts[0]);
            if (effect.isEmpty()) {
                return Optional.empty();
            }

            try {
                int amplifier = Math.clamp(Integer.parseInt(parts[2].trim()), 0, MAX_AMPLIFIER);
                if (parts[1].trim().equalsIgnoreCase(FOREVER)) {
                    return Optional.of(new PotionDose(effect.get(), PotionDose.FOREVER_TICKS, amplifier));
                }
                int seconds = Math.clamp(Integer.parseInt(parts[1].trim()), 1, MAX_DOSE_SECONDS);
                return Optional.of(
                        new PotionDose(effect.get(), seconds * TICKS_PER_SECOND, amplifier));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        /** Reads {@code range[:x:y:z][@filter]}, keeping what it cannot read from the fallback. */
        private static Recipe parseArea(String written, Recipe fallback) {
            int at = written.indexOf(FILTER_SEPARATOR);
            String reach = at < 0 ? written : written.substring(0, at);
            Filter filter =
                    at < 0 ? fallback.filter() : Filter.parse(written.substring(at + 1), fallback.filter());

            String[] parts = reach.split(FIELD_SEPARATOR);
            int range = fallback.range();
            try {
                range = Math.clamp(Integer.parseInt(parts[0].trim()), 1, MAX_POTION_RANGE);
            } catch (NumberFormatException e) {
                // A range that is not a number leaves the one already in force.
            }

            Vec3i offset = fallback.offset();
            if (parts.length == 4) {
                offset = offsetOn(parts[1] + FIELD_SEPARATOR + parts[2] + FIELD_SEPARATOR + parts[3])
                        .orElse(fallback.offset());
            }

            return new Recipe(fallback.doses(), range, offset, filter);
        }
    }

    /** Reads an {@code x:y:z} offset. */
    private static Optional<Vec3i> offsetOn(String written) {
        String[] parts = written.split(FIELD_SEPARATOR);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    new Vec3i(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Reads a particle's offset, written as an axis letter and a distance such as {@code Y3}. */
    private static Vec3d particleOffset(ChipState state) {
        String written = state.sign().trimmedText(SETTINGS_LINE);
        if (written.length() < 2) {
            return Vec3d.ZERO;
        }

        int distance;
        try {
            distance = Math.clamp(
                    Integer.parseInt(written.substring(1).trim()),
                    -MAX_PARTICLE_OFFSET,
                    MAX_PARTICLE_OFFSET);
        } catch (NumberFormatException e) {
            return Vec3d.ZERO;
        }

        return switch (Character.toUpperCase(written.charAt(0))) {
            case 'X' -> new Vec3d(distance, 0, 0);
            case 'Y' -> new Vec3d(0, distance, 0);
            case 'Z' -> new Vec3d(0, 0, distance);
            default -> Vec3d.ZERO;
        };
    }
}
