package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.mechanic.Actor;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The behaviour of one kind of chip.
 *
 * <p>A logic instance belongs to a single IC in the world and may hold that chip's private state,
 * such as a counter's running total. Implementations receive everything they need through
 * {@link ChipState}, which is why a gate can be tested by handing it a {@link SimpleChipState}
 * rather than by standing up a server.
 *
 * <p>Implementations must not block. Anything slow belongs on the platform's scheduler, reached
 * through the context a world-affecting chip is given, not from inside {@link #trigger}.
 */
@NullMarked
public interface ICLogic {

    /**
     * Runs the chip because one of its inputs changed.
     *
     * @param state the chip's inputs, outputs and configuration
     */
    void trigger(ChipState state);

    /**
     * Whether this chip reads redstone power levels rather than just on and off.
     *
     * <p>Chips that answer true are re-run when the power level on an input changes even if it
     * stays above zero, which is more expensive, so only the chips that genuinely compare
     * levels should opt in.
     */
    default boolean requiresAnalogRedstone() {
        return false;
    }

    /**
     * Called once when the chip is loaded, before any call to {@link #trigger}.
     *
     * <p>The default does nothing. Override to restore state or to bring outputs into line with
     * inputs after a restart.
     */
    default void load(ChipState state) {
        // Most chips are stateless across a restart and need no restoration.
    }

    /**
     * Called once when the chip is unloaded, after which it will not run again.
     *
     * <p>The default does nothing. Override to release anything the chip registered elsewhere, or
     * to write state a chip keeps on its sign back to it.
     *
     * <p>The sign may already be gone, if the chip is unloading because somebody broke it. Writing
     * to it is safe either way; the write simply does nothing.
     *
     * @param state the chip's inputs, outputs and configuration
     */
    default void unload(ChipState state) {
        // Most chips hold nothing that needs releasing.
    }

    /**
     * Checks a sign as it is being written, before the chip is made.
     *
     * <p>Most chips have nothing to check. A chip configured badly says so the first time somebody
     * uses it, and by then they are standing next to it. The chips that need this are the ones
     * naming something kept outside the world — a variable, say — where a sign naming one that
     * does not exist would be silently dead and its builder would have no way to tell that from a
     * wiring fault.
     *
     * <p>This is the same reasoning, and the same answer, as
     * {@link com.xeonproductions.craftbookultimate.core.mechanic.SignMechanic#review}. It is asked
     * of a throwaway logic instance built for the purpose, so an implementation must not expect
     * this to be the same object that later runs, and must not keep anything from it.
     *
     * @param lines what the builder has written
     * @param services the registries the chip would run against
     * @param builder whoever is writing it
     * @return why the sign is being refused, or empty to accept it
     */
    default Optional<String> reviewSign(SignLines lines, ChipServices services, Actor builder) {
        return Optional.empty();
    }
}
