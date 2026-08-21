package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.math.Bounds;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * A chip that works on a stretch of the world, and can say which stretch.
 *
 * <p>Most chips reach exactly as far as their own pins and have nothing to declare. The ones that
 * implement this are the sensors, the clearers and the growers — chips whose whole behaviour is
 * decided by a radius written on a sign, where the single most common fault is a builder having a
 * different box in mind than the chip does.
 *
 * <p>The answer is worked out from the sign every time it is asked for rather than kept, because
 * the sign is what decides it and the sign can be rewritten under a running chip.
 *
 * <p>This exists for the debugging tools and for nothing else. Nothing about how a chip runs
 * depends on it, so a chip that has an area and does not implement this is less useful to debug
 * and otherwise entirely correct.
 */
@NullMarked
public interface AreaAwareICLogic extends ICLogic {

    /**
     * The box this chip is working on as its sign currently reads.
     *
     * @param state the chip's own state, for reading the sign and the settings
     * @return the box, or empty if the chip cannot say — a sensor that has found nothing to
     *     measure from has no area rather than an empty one
     */
    Optional<Bounds> area(ChipState state);
}
