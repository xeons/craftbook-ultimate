package com.xeonproductions.craftbookultimate.core.ic;

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
     * <p>The default does nothing. Override to release anything the chip registered elsewhere.
     */
    default void unload() {
        // Most chips hold nothing that needs releasing.
    }
}
