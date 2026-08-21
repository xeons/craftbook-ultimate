// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that move items between the ground and the chests around them.
 *
 * <p>One takes items out of a chest and drops them; the other picks dropped items up and puts them
 * away. Both work from the chests near the sign, or from one particular container when the sign
 * gives an offset to it.
 *
 * <p>Line 3 names the item each deals in. Leaving it blank, or writing {@code -1}, means any item
 * at all, which is what a general-purpose collector wants.
 */
@NullMarked
public final class Containers {

    /** The line naming which item the chip deals in. */
    private static final int ITEM_LINE = 2;

    /** The line carrying the amount or range, and any offset to a container. */
    private static final int SETTINGS_LINE = 3;

    /** Written on line 3 to mean any item at all. */
    private static final String ANY_ITEM = "-1";

    /** Separates an amount or range from the offset to a container. */
    private static final char OFFSET_SEPARATOR = '@';

    /** How far a chip looks for containers when the sign does not name one. */
    private static final int CONTAINER_SEARCH_RADIUS = 5;

    /** The furthest a sign may point at a container in any one direction. */
    private static final int MAX_CONTAINER_OFFSET = 8;

    /** How far above itself a dispenser looks for somewhere to drop what it takes out. */
    private static final int DISPENSER_REACH = 10;

    /** The most items one pulse may dispense, which is a double chest full. */
    private static final int MAX_DISPENSED = 9 * 6 * 64;

    /** The largest stack the game moves in one go. */
    private static final int STACK = 64;

    /** How far a collector reaches for dropped items when the sign does not say. */
    private static final int DEFAULT_COLLECTION_RANGE = 8;

    /** The furthest a collector may reach for dropped items. */
    private static final int MAX_COLLECTION_RANGE = 8;

    /** The containers a collector fills when its sign does not name a kind. */
    private static final Set<Key> DEFAULT_CONTAINERS =
            Set.of(Blocks.key("chest"), Blocks.key("trapped_chest"));

    /** The kinds of container a collector's mode letter can name. */
    private static final Map<Character, Key> CONTAINERS_BY_LETTER = Map.of(
            'c', Blocks.key("chest"),
            't', Blocks.key("trapped_chest"),
            'd', Blocks.key("dispenser"),
            'r', Blocks.key("dropper"),
            'h', Blocks.key("hopper"));

    private Containers() {}

    /**
     * Takes items out of a container and drops them above itself.
     *
     * <p>Line 4 reads {@code amount}, with an optional {@code @x:y:z} naming a container relative
     * to the sign. Without one the chip uses whatever chests are near it.
     *
     * <p>Items land in the first free block above the one the sign hangs on, so a dispenser built
     * under a hopper feeds it.
     */
    public static ICLogic chestDispenser() {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            Settings settings = Settings.on(state, 1, MAX_DISPENSED, 1);
            Stockpile pile = pileFor(state, settings, Set.of());
            Optional<Vec3d> spot = dropPoint(state);
            if (spot.isEmpty()) {
                return;
            }

            Optional<Key> wanted = wantedItem(state);
            int remaining = settings.amount();
            while (remaining > 0) {
                Key item = wanted.or(() -> anythingIn(pile)).orElse(null);
                if (item == null) {
                    return;
                }

                int taken = pile.take(item, Math.min(remaining, STACK));
                if (taken == 0) {
                    return;
                }
                if (!state.world().dropItem(spot.get(), item, taken)) {
                    pile.give(item, taken);
                    return;
                }
                remaining -= taken;
            }
        };
    }

    /**
     * Picks dropped items up and puts them in a container.
     *
     * <p>Line 4 reads {@code range}, with an optional {@code :x:y:z} naming a container relative to
     * the sign. The range defaults to eight blocks and cannot exceed it.
     *
     * <p>A letter after the model reference says which kind of container to fill: {@code c} for a
     * chest, {@code t} for a trapped chest, {@code d} for a dispenser, {@code r} for a dropper and
     * {@code h} for a hopper. Without one it fills the chests near it.
     *
     * <p>The output reports whether anything was picked up, so a hopper line can be driven from it.
     */
    public static SelfTriggeringICLogic chestCollector() {
        return new Collector();
    }

    /** Picks dropped items up, on a pulse or on every tick. */
    private static final class Collector implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                collect(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            collect(state);
        }

        private static void collect(ChipState state) {
            Settings settings =
                    Settings.on(state, 1, MAX_COLLECTION_RANGE, DEFAULT_COLLECTION_RANGE);
            Set<Key> kinds = containerKinds(state);
            Stockpile pile = pileFor(state, settings, kinds);
            Optional<Key> wanted = wantedItem(state);

            boolean collected = false;
            for (DroppedItem item : state.world().itemsNear(state.signPosition(), settings.amount())) {
                if (!item.isPresent() || wanted.filter(key -> !key.equals(item.type())).isPresent()) {
                    continue;
                }

                int held = item.count();
                int refused = pile.give(item.type(), held);
                int stored = held - refused;
                if (stored > 0) {
                    item.take(stored);
                    collected = true;
                }
            }

            state.setMainOutput(collected);
        }

        /** Which containers this collector fills, from the letter after its model reference. */
        private static Set<Key> containerKinds(ChipState state) {
            String mode = state.modeText();
            if (mode.isEmpty()) {
                return DEFAULT_CONTAINERS;
            }
            Key named =
                    CONTAINERS_BY_LETTER.get(Character.toLowerCase(mode.charAt(mode.length() - 1)));
            return named == null ? DEFAULT_CONTAINERS : Set.of(named);
        }
    }

    /** The item a chip's sign says it deals in, empty when the sign says any item at all. */
    private static Optional<Key> wantedItem(ChipState state) {
        String written = state.sign().trimmedText(ITEM_LINE);
        if (written.isEmpty() || written.equals(ANY_ITEM)) {
            return Optional.empty();
        }
        return state.world().resolveItem(written);
    }

    /** Whatever a stockpile happens to hold, for a dispenser that was not told what to send. */
    private static Optional<Key> anythingIn(Stockpile pile) {
        for (Map.Entry<Key, Integer> held : pile.contents().entrySet()) {
            if (held.getValue() > 0) {
                return Optional.of(held.getKey());
            }
        }
        return Optional.empty();
    }

    /** The container a chip works with: the one its sign points at, or the ones near it. */
    private static Stockpile pileFor(ChipState state, Settings settings, Set<Key> kinds) {
        return settings
                .offset()
                .map(offset -> state.stockpileNear(state.signPosition().add(offset), 0, kinds))
                .orElseGet(() -> state.stockpileNear(state.signPosition(), CONTAINER_SEARCH_RADIUS, kinds));
    }

    /** Where a dispenser drops what it takes out: the first free block above its support. */
    private static Optional<Vec3d> dropPoint(ChipState state) {
        ChipWorld world = state.world();
        Vec3i back = state.backPosition();
        for (int above = 1; above <= DISPENSER_REACH; above++) {
            Vec3i candidate = back.add(0, above, 0);
            if (!world.isInBounds(candidate) || !world.isLoaded(candidate)) {
                break;
            }
            if (world.isAir(candidate)) {
                return Optional.of(Vec3d.centreOf(candidate));
            }
        }
        return Optional.empty();
    }

    /**
     * What line 4 says: a number, and possibly where the container is.
     *
     * <p>The two chips separate the offset differently — a dispenser writes {@code 8@1:0:0} and a
     * collector writes {@code 8:1:0:0} — so both separators are accepted and whichever comes first
     * is the one that counts.
     *
     * @param amount how many to dispense, or how far to reach for dropped items
     * @param offset where the container is relative to the sign, if the sign named one
     */
    private record Settings(int amount, Optional<Vec3i> offset) {

        static Settings on(ChipState state, int lowest, int highest, int fallback) {
            String written = state.sign().trimmedText(SETTINGS_LINE);
            if (written.isEmpty()) {
                return new Settings(fallback, Optional.empty());
            }

            int at = written.indexOf(OFFSET_SEPARATOR);
            int colon = written.indexOf(':');
            int split = at >= 0 ? at : colon;

            String number = split < 0 ? written : written.substring(0, split);
            Optional<Vec3i> offset =
                    split < 0 ? Optional.empty() : parseOffset(written.substring(split + 1));

            return new Settings(number(number, lowest, highest, fallback), offset);
        }

        private static Optional<Vec3i> parseOffset(String written) {
            String[] parts = written.split(":");
            if (parts.length != 3) {
                return Optional.empty();
            }
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) > MAX_CONTAINER_OFFSET) {
                    return Optional.empty();
                }
                return Optional.of(new Vec3i(x, y, z));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        private static int number(String written, int lowest, int highest, int fallback) {
            if (written.isBlank()) {
                return fallback;
            }
            try {
                return Math.clamp(Integer.parseInt(written.trim()), lowest, highest);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
