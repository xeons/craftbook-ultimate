package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

/**
 * The box of blocks a bridge or a door fills and empties.
 *
 * <p>Worked out afresh every time the mechanic runs, from the two signs and the blocks around
 * them, which is what lets a builder widen a bridge by laying another row of blocks at each end
 * rather than by rewriting the sign.
 *
 * @param min the lowest corner
 * @param max the highest corner
 */
@NullMarked
public record Panel(Vec3i min, Vec3i max) {

    /** The box with two opposite corners in it, whichever way round they are given. */
    public static Panel between(Vec3i one, Vec3i other) {
        return new Panel(
                new Vec3i(
                        Math.min(one.x(), other.x()),
                        Math.min(one.y(), other.y()),
                        Math.min(one.z(), other.z())),
                new Vec3i(
                        Math.max(one.x(), other.x()),
                        Math.max(one.y(), other.y()),
                        Math.max(one.z(), other.z())));
    }

    /** Whether a position is inside the box. */
    public boolean contains(Vec3i position) {
        return position.x() >= min.x() && position.x() <= max.x()
                && position.y() >= min.y() && position.y() <= max.y()
                && position.z() >= min.z() && position.z() <= max.z();
    }

    /** Runs something for every block in the box. */
    public void forEach(Consumer<Vec3i> action) {
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    action.accept(new Vec3i(x, y, z));
                }
            }
        }
    }
}
