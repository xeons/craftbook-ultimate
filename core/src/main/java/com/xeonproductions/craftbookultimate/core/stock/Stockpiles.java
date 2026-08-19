package com.xeonproductions.craftbookultimate.core.stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/** The stockpiles that are not simply a store of counts. */
@NullMarked
public final class Stockpiles {

    private Stockpiles() {}

    /**
     * A bottomless stockpile.
     *
     * <p>Always has whatever is asked for and always accepts whatever is given, so a mechanic
     * backed by one never stops for want of materials. Used for builds that are meant to run
     * without anyone keeping them supplied.
     */
    public static Stockpile unlimited() {
        return Unlimited.INSTANCE;
    }

    /** A stockpile that holds nothing and accepts nothing. */
    public static Stockpile empty() {
        return Empty.INSTANCE;
    }

    /**
     * Several stockpiles treated as one.
     *
     * <p>Taking draws from each in order until it has enough, so the nearest container is emptied
     * before a further one is touched. Giving fills each in order for the same reason.
     *
     * @param parts the stockpiles to combine, in the order they should be used
     */
    public static Stockpile combined(List<Stockpile> parts) {
        List<Stockpile> copy = List.copyOf(parts);
        if (copy.isEmpty()) {
            return empty();
        }
        if (copy.size() == 1) {
            return copy.get(0);
        }
        return new Combined(copy);
    }

    /** A bottomless stockpile. */
    private enum Unlimited implements Stockpile {
        INSTANCE;

        @Override
        public int count(Key item) {
            return Integer.MAX_VALUE;
        }

        @Override
        public int take(Key item, int amount) {
            return Math.max(0, amount);
        }

        @Override
        public int give(Key item, int amount) {
            return 0;
        }

        @Override
        public int countRoomFor(Key item) {
            return Integer.MAX_VALUE;
        }

        @Override
        public Map<Key, Integer> contents() {
            return Map.of();
        }

        @Override
        public boolean isUnlimited() {
            return true;
        }
    }

    /** A stockpile with nothing in it and no room. */
    private enum Empty implements Stockpile {
        INSTANCE;

        @Override
        public int count(Key item) {
            return 0;
        }

        @Override
        public int take(Key item, int amount) {
            return 0;
        }

        @Override
        public int give(Key item, int amount) {
            return Math.max(0, amount);
        }

        @Override
        public Map<Key, Integer> contents() {
            return Map.of();
        }
    }

    /** Several stockpiles used in order, as though they were one. */
    private record Combined(List<Stockpile> parts) implements Stockpile {

        @Override
        public int count(Key item) {
            int total = 0;
            for (Stockpile part : parts) {
                if (part.isUnlimited()) {
                    return Integer.MAX_VALUE;
                }
                total += part.count(item);
            }
            return total;
        }

        @Override
        public int take(Key item, int amount) {
            int remaining = amount;
            for (Stockpile part : parts) {
                if (remaining <= 0) {
                    break;
                }
                remaining -= part.take(item, remaining);
            }
            return amount - remaining;
        }

        @Override
        public int give(Key item, int amount) {
            int remaining = amount;
            for (Stockpile part : parts) {
                if (remaining <= 0) {
                    break;
                }
                remaining = part.give(item, remaining);
            }
            return remaining;
        }

        @Override
        public int countRoomFor(Key item) {
            int total = 0;
            for (Stockpile part : parts) {
                if (part.isUnlimited()) {
                    return Integer.MAX_VALUE;
                }
                total += part.countRoomFor(item);
            }
            return total;
        }

        @Override
        public Map<Key, Integer> contents() {
            Map<Key, Integer> combined = new HashMap<>();
            for (Stockpile part : parts) {
                part.contents().forEach((item, count) -> combined.merge(item, count, Integer::sum));
            }
            return Map.copyOf(combined);
        }

        @Override
        public boolean isUnlimited() {
            for (Stockpile part : parts) {
                if (part.isUnlimited()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean takeAll(Key item, int amount) {
            if (amount <= 0) {
                return true;
            }
            if (!has(item, amount)) {
                return false;
            }

            // Taking across several containers can only be undone by putting back what was
            // already taken, so the parts are checked as a whole before any of them is touched.
            List<Integer> takenFrom = new ArrayList<>(parts.size());
            int remaining = amount;
            for (Stockpile part : parts) {
                int taken = remaining <= 0 ? 0 : part.take(item, remaining);
                takenFrom.add(taken);
                remaining -= taken;
            }

            if (remaining == 0) {
                return true;
            }

            for (int i = 0; i < parts.size(); i++) {
                parts.get(i).give(item, takenFrom.get(i));
            }
            return false;
        }
    }
}
