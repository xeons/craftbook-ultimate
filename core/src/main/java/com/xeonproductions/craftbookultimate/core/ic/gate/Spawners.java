package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that put things in the world out of nothing.
 *
 * <p>Both look upwards from the block their sign hangs on for the first place something could
 * stand, and put what they make there. That means a spawner buried in a floor delivers onto the
 * floor rather than inside it, and one under a stack of blocks delivers on top of the stack.
 *
 * <p>Both are restricted, since between them they can make anything the game has.
 */
@NullMarked
public final class Spawners {

    /** The line naming what to make. */
    private static final int SUBJECT_LINE = 2;

    /** The line carrying how many, which the entity spawner treats as more of line three. */
    private static final int AMOUNT_LINE = 3;

    /** Separates a creature description from the number of them to spawn. */
    private static final char AMOUNT_SEPARATOR = '*';

    /** The most creatures one pulse may spawn. */
    private static final int MAX_CREATURES = 100;

    /** The most items one pulse may drop, which is a stack. */
    private static final int MAX_ITEMS = 64;

    private Spawners() {}

    /**
     * Puts creatures in the world.
     *
     * <p>Lines 3 and 4 are read as one long description, so a stack of riders too long for one
     * line can run onto the next. An asterisk ends the description and what follows is how many to
     * spawn, from one to a hundred.
     *
     * <pre>
     *   zombie          one zombie
     *   pig+zombie*5    five zombies, each on a pig of its own
     *   sheep@13*20     twenty green sheep
     * </pre>
     */
    public static ICLogic entitySpawner() {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            String written = state.sign().trimmedText(SUBJECT_LINE) + state.sign().trimmedText(AMOUNT_LINE);
            int separator = written.indexOf(AMOUNT_SEPARATOR);
            String described = separator < 0 ? written : written.substring(0, separator);
            int count =
                    separator < 0
                            ? 1
                            : boundedNumber(written.substring(separator + 1), 1, MAX_CREATURES, 1);

            Optional<EntitySpec> spec = EntitySpec.parse(described, state.world()::resolveItem)
                    .filter(EntitySpec::isSpawnable);
            if (spec.isEmpty()) {
                return;
            }

            deliveryPoint(state).ifPresent(spot -> state.world().spawn(spot, spec.get(), count));
        };
    }

    /**
     * Drops items in the world.
     *
     * <p>Line 3 names the item, which a sign written before the flattening may name by number and
     * damage. Line 4 is how many, up to a stack, and defaults to one.
     */
    public static ICLogic itemSpawner() {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            Optional<Key> item = state.world().resolveItem(state.sign().trimmedText(SUBJECT_LINE));
            if (item.isEmpty()) {
                return;
            }

            int count = boundedNumber(state.sign().trimmedText(AMOUNT_LINE), 1, MAX_ITEMS, 1);
            deliveryPoint(state).ifPresent(spot -> state.world().dropItem(spot, item.get(), count));
        };
    }

    /**
     * Where a spawner puts what it makes.
     *
     * <p>The first place at or above the block the sign hangs on that something could stand in,
     * horizontally in the middle of it, which is where the game itself puts anything it spawns.
     */
    private static Optional<Vec3d> deliveryPoint(ChipState state) {
        ChipWorld world = state.world();
        Vec3i back = state.backPosition();
        if (!world.isLoaded(back)) {
            return Optional.empty();
        }
        return world.firstPassableAtOrAbove(back).map(Vec3d::centreOf);
    }

    /** A number from a sign, held within bounds, falling back when the text is not one. */
    private static int boundedNumber(String written, int lowest, int highest, int fallback) {
        if (written.isEmpty()) {
            return fallback;
        }
        try {
            return Math.clamp(Integer.parseInt(written.trim()), lowest, highest);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
