package com.xeonproductions.craftbookultimate.core.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has asked the plugin to do.
 *
 * <p>A value, not a file. Whoever reads the file turns it into one of these, which means the
 * settings can be exercised in a plain unit test and a chip that respects them can be tested
 * against a set built in two lines.
 *
 * <p>Everything here is either a limit on how far a chip may reach or a statement about what may
 * run at all. Nothing here changes what a sign means: the sign grammar is the same whatever the
 * settings say, and a sign asking for more than it is allowed gets as much as it is allowed
 * rather than being refused.
 *
 * @param enabled whether chips run at all; false leaves every sign in place and inert
 * @param disabledWorlds worlds where no chip runs, by name, compared without regard to case
 * @param disabledChips model numbers that are never created, compared without regard to case
 * @param maxRadius the furthest a chip may reach when its sign gives a radius
 * @param maxWidth the widest a bridge, door, or harvested area may be
 * @param maxLength the furthest a bridge, door, or harvested area may run from its sign
 * @param maxPlanterWidth the largest field an area planter may sow, along either side
 * @param placeableBlocks what a building chip may place, or empty for anything at all
 * @param carts what an operator has said about the minecart mechanics
 * @param mechanics what an operator has said about the sign mechanics
 * @param pipes what an operator has said about the pipes
 */
@NullMarked
public record Settings(
        boolean enabled,
        Set<String> disabledWorlds,
        Set<String> disabledChips,
        int maxRadius,
        int maxWidth,
        int maxLength,
        int maxPlanterWidth,
        Set<Key> placeableBlocks,
        CartSettings carts,
        MechanicSettings mechanics,
        PipeSettings pipes) {

    /**
     * The settings in force when nobody has said otherwise.
     *
     * <p>The limits are the ones the plugin has always imposed, so a world full of existing signs
     * behaves the same on a server that has never been configured.
     */
    public static final Settings DEFAULTS = builder().build();

    /** Copies the collections and holds every limit to something a chip can work with. */
    public Settings {
        disabledWorlds = lowercased(disabledWorlds);
        disabledChips = uppercased(disabledChips);
        placeableBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(placeableBlocks));
        maxRadius = Math.max(0, maxRadius);
        maxWidth = Math.max(1, maxWidth);
        maxLength = Math.max(1, maxLength);
        maxPlanterWidth = Math.max(1, maxPlanterWidth);
    }

    /** A set of settings to alter from the defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /** These settings with everything the same but for what the builder is given. */
    public Builder toBuilder() {
        return new Builder()
                .enabled(enabled)
                .disabledWorlds(disabledWorlds)
                .disabledChips(disabledChips)
                .maxRadius(maxRadius)
                .maxWidth(maxWidth)
                .maxLength(maxLength)
                .maxPlanterWidth(maxPlanterWidth)
                .placeableBlocks(placeableBlocks)
                .carts(carts)
                .mechanics(mechanics)
                .pipes(pipes);
    }

    /**
     * Whether chips run in a world.
     *
     * @param world the world's name
     */
    public boolean allowsWorld(String world) {
        return enabled && !disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }

    /**
     * Whether a chip may be created and run.
     *
     * <p>Every model number a chip answers to is offered, so switching off a retired number
     * switches off the chip that took it over rather than leaving a way round the setting.
     *
     * @param models the model numbers the chip answers to
     */
    public boolean allowsChip(Set<String> models) {
        if (!enabled) {
            return false;
        }
        for (String model : models) {
            if (disabledChips.contains(model.toUpperCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a building chip may place a block.
     *
     * <p>An empty list is not an empty permission: it means nothing was singled out, so anything
     * may be placed.
     */
    public boolean mayPlace(Key block) {
        return placeableBlocks.isEmpty() || placeableBlocks.contains(block);
    }

    /** A width from a sign, brought within what is allowed. */
    public int limitWidth(int width) {
        return Math.clamp(width, 1, maxWidth);
    }

    /** A length or height from a sign, brought within what is allowed. */
    public int limitLength(int length) {
        return Math.clamp(length, 1, maxLength);
    }

    /** A planter's field side, brought within what is allowed. */
    public int limitPlanterWidth(int width) {
        return Math.clamp(width, 1, maxPlanterWidth);
    }

    private static Set<String> lowercased(Set<String> names) {
        Set<String> copy = new LinkedHashSet<>();
        for (String name : names) {
            copy.add(name.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> uppercased(Set<String> names) {
        Set<String> copy = new LinkedHashSet<>();
        for (String name : names) {
            copy.add(name.toUpperCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(copy);
    }

    /** Assembles a set of settings, filling in the defaults for anything not given. */
    public static final class Builder {

        private boolean enabled = true;
        private Set<String> disabledWorlds = Set.of();
        private Set<String> disabledChips = Set.of();
        private int maxRadius = 10;
        private int maxWidth = 5;
        private int maxLength = 16;
        private int maxPlanterWidth = 4;
        private Set<Key> placeableBlocks = DefaultBlocks.PLACEABLE;
        private CartSettings carts = CartSettings.DEFAULTS;
        private MechanicSettings mechanics = MechanicSettings.DEFAULTS;
        private PipeSettings pipes = PipeSettings.DEFAULTS;

        private Builder() {}

        /** Whether chips run at all. */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /** The worlds no chip runs in. */
        public Builder disabledWorlds(Set<String> worlds) {
            this.disabledWorlds = worlds;
            return this;
        }

        /** The model numbers that are never created. */
        public Builder disabledChips(Set<String> models) {
            this.disabledChips = models;
            return this;
        }

        /** The furthest a chip may reach when its sign gives a radius. */
        public Builder maxRadius(int radius) {
            this.maxRadius = radius;
            return this;
        }

        /** The widest a bridge, door, or harvested area may be. */
        public Builder maxWidth(int width) {
            this.maxWidth = width;
            return this;
        }

        /** The furthest a bridge, door, or harvested area may run from its sign. */
        public Builder maxLength(int length) {
            this.maxLength = length;
            return this;
        }

        /** The largest field an area planter may sow, along either side. */
        public Builder maxPlanterWidth(int width) {
            this.maxPlanterWidth = width;
            return this;
        }

        /** What a building chip may place, or empty for anything at all. */
        public Builder placeableBlocks(Set<Key> blocks) {
            this.placeableBlocks = blocks;
            return this;
        }

        /** What an operator has said about the minecart mechanics. */
        public Builder carts(CartSettings carts) {
            this.carts = carts;
            return this;
        }

        /** What an operator has said about the pipes. */
        public Builder pipes(PipeSettings pipes) {
            this.pipes = pipes;
            return this;
        }

        /** What an operator has said about the sign mechanics. */
        public Builder mechanics(MechanicSettings mechanics) {
            this.mechanics = mechanics;
            return this;
        }

        /** Lets a building chip place anything. */
        public Builder placeAnything() {
            return placeableBlocks(Set.of());
        }

        public Settings build() {
            return new Settings(
                    enabled,
                    disabledWorlds,
                    disabledChips,
                    maxRadius,
                    maxWidth,
                    maxLength,
                    maxPlanterWidth,
                    placeableBlocks,
                    carts,
                    mechanics,
                    pipes);
        }
    }
}
