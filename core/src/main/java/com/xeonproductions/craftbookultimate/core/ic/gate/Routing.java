package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that steer a signal rather than compute one, plus the random sources.
 */
@NullMarked
public final class Routing {

    private Routing() {}

    /**
     * Copies its data input onto whichever outputs are selected.
     *
     * <p>An unselected output is left as it was rather than driven low, so a builder can hold a
     * branch at its last value by dropping its select line.
     *
     * <p>Pins: 0 is the data, 1 and 2 select outputs 1 and 2 respectively.
     */
    public static ICLogic dispatcher() {
        return state -> {
            boolean value = state.input(0);

            if (state.input(1)) {
                state.setOutput(1, value);
            }
            if (state.input(2)) {
                state.setOutput(2, value);
            }
        };
    }

    /**
     * Selects one of two data inputs.
     *
     * <p>The chip deliberately does nothing on the run where the selector itself changed, so
     * switching sources does not glitch the output through the old value.
     *
     * <p>Pins: 0 selects, 1 is chosen when the selector is high and 2 when it is low.
     */
    public static ICLogic multiplexer() {
        return state -> {
            if (state.isTriggered(0)) {
                return;
            }
            state.setMainOutput(state.input(0) ? state.input(1) : state.input(2));
        };
    }

    /**
     * Raises exactly one output, chosen by reading the selected inputs as a binary number.
     *
     * <p>The first named input is the least significant bit. Every other output is driven low, so
     * exactly one line is ever high.
     *
     * @param selectorInputs the input pins forming the address, least significant first
     */
    public static ICLogic demultiplexer(int... selectorInputs) {
        int[] selectors = selectorInputs.clone();
        return state -> {
            int address = 0;
            for (int bit = 0; bit < selectors.length; bit++) {
                if (state.input(selectors[bit])) {
                    address |= 1 << bit;
                }
            }

            for (int output = 0; output < state.outputCount(); output++) {
                state.setOutput(output, output == address);
            }
        };
    }

    /**
     * Raises a random selection of its outputs each time it is driven.
     *
     * <p>The number raised is chosen uniformly between the two bounds, and which outputs those
     * are is shuffled, so every combination of the chosen size is equally likely. Bounds beyond
     * the number of outputs are pulled back to fit.
     *
     * @param minimumOn the fewest outputs to raise
     * @param maximumOn the most outputs to raise
     * @param random the source of randomness
     */
    public static ICLogic randomBits(int minimumOn, int maximumOn, RandomGenerator random) {
        return state -> {
            if (!state.isAnyInputActive()) {
                return;
            }

            int outputs = state.outputCount();
            if (outputs == 0) {
                return;
            }

            int low = Math.max(0, Math.min(minimumOn, outputs));
            int high = Math.max(low, Math.min(maximumOn, outputs));
            int onCount = low + random.nextInt(high - low + 1);

            boolean[] values = new boolean[outputs];
            for (int i = 0; i < onCount; i++) {
                values[i] = true;
            }
            shuffle(values, random);

            for (int i = 0; i < outputs; i++) {
                state.setOutput(i, values[i]);
            }
        };
    }

    /**
     * A random source that takes its bounds from the sign.
     *
     * <p>Line 2 reads either {@code max} on its own or {@code min:max}. With nothing usable
     * there, any number of outputs from none to all of them may be raised.
     *
     * @param random the source of randomness
     */
    public static ICLogic randomBitsFromSign(RandomGenerator random) {
        return state -> {
            int outputs = state.outputCount();
            String line = state.sign().trimmedText(2);

            int minimum = 0;
            int maximum = outputs;
            if (!line.isEmpty()) {
                String[] parts = line.split(":");
                try {
                    if (parts.length >= 2) {
                        minimum = Integer.parseInt(parts[0].trim());
                        maximum = Integer.parseInt(parts[1].trim());
                    } else {
                        maximum = Integer.parseInt(parts[0].trim());
                    }
                } catch (NumberFormatException e) {
                    minimum = 0;
                    maximum = outputs;
                }
            }

            randomBits(minimum, maximum, random).trigger(state);
        };
    }

    /** Fisher-Yates, driven by the supplied source so a seeded generator gives a repeatable run. */
    private static void shuffle(boolean[] values, RandomGenerator random) {
        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            boolean swap = values[i];
            values[i] = values[j];
            values[j] = swap;
        }
    }
}
