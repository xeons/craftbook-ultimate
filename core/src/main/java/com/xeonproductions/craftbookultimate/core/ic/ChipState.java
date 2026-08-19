package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.platform.TimeSource;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import org.jspecify.annotations.NullMarked;

/**
 * Everything a chip can see and change when it runs.
 *
 * <p>This is the whole contract between an {@link ICLogic} and the world. A chip reads its
 * inputs, reads its configuration off the sign, and writes its outputs; it never touches a
 * block, a server or a scheduler directly. That is what allows a gate's behaviour to be
 * exercised in a plain unit test against {@link SimpleChipState}, with no server running.
 *
 * <p>Pins are numbered per {@link PinLayout}: inputs are numbered from zero, and outputs are
 * numbered from zero separately, so a 3I3O chip has inputs 0-2 and outputs 0-2.
 *
 * <p>Implementations are free to read the world lazily, so a chip should treat repeated reads
 * of the same input as potentially expensive rather than free.
 */
@NullMarked
public interface ChipState {

    /** The number of inputs this chip has. */
    int inputCount();

    /** The number of outputs this chip has. */
    int outputCount();

    /**
     * Whether an input is receiving power.
     *
     * @param index the input number, counted from zero
     * @throws IndexOutOfBoundsException if the input does not exist
     */
    boolean input(int index);

    /**
     * The redstone power level on an input, from 0 to 15.
     *
     * <p>Only the chips that advertise {@link ICLogic#requiresAnalogRedstone()} need this;
     * the rest should use {@link #input(int)}.
     *
     * @param index the input number, counted from zero
     * @throws IndexOutOfBoundsException if the input does not exist
     */
    int inputPower(int index);

    /**
     * Whether an output is currently powered.
     *
     * @param index the output number, counted from zero
     * @throws IndexOutOfBoundsException if the output does not exist
     */
    boolean output(int index);

    /**
     * Drives an output.
     *
     * <p>Any {@linkplain ICMode#invertsOutputs() inverting mode} is applied by the
     * implementation, so a chip always writes the logical value it means.
     *
     * @param index the output number, counted from zero
     * @param value the value to drive
     * @throws IndexOutOfBoundsException if the output does not exist
     */
    void setOutput(int index, boolean value);

    /**
     * Whether anything is wired to an input.
     *
     * <p>An unwired pin is not the same as a pin reading low. Gates count only the pins a
     * builder actually connected, so an AND gate with two wires behaves as a two-input gate
     * rather than failing because its third pin is bare.
     *
     * @param index the input number, counted from zero
     * @throws IndexOutOfBoundsException if the input does not exist
     */
    boolean isConnected(int index);

    /**
     * Whether the block the sign is mounted on is itself a power source.
     *
     * <p>Builders use a block of redstone behind the sign to hold a chip permanently on, which
     * counts as an active input even though no pin is wired.
     */
    boolean hasPowerSourceBehind();

    /**
     * The input that caused this run, or {@code -1} when the chip ran for some other reason,
     * such as a self-triggering tick.
     */
    int triggeredInput();

    /** The text on the chip's sign, which is where its configuration lives. */
    SignLines sign();

    /**
     * Rewrites one line of the chip's sign.
     *
     * <p>A few chips keep their running state on the sign, where a player can read and edit it.
     *
     * @param index the line number, from zero to three
     * @param text the replacement text
     * @throws IndexOutOfBoundsException if the line does not exist
     */
    void setSignLine(int index, String text);

    /** The mode written after the model reference on the sign. */
    ICMode mode();

    /**
     * Schedules work on the region owning this chip.
     *
     * <p>A chip that acts after a delay, or repeatedly, goes through here. Work scheduled this
     * way runs on the thread allowed to touch the chip's blocks, and stops when the chip is
     * unloaded.
     */
    Scheduler scheduler();

    /** Where this chip reads the time from. */
    TimeSource time();

    /**
     * Whether a particular input caused this run.
     *
     * @param index the input number, counted from zero
     */
    default boolean isTriggered(int index) {
        return triggeredInput() == index;
    }

    /** Whether the chip ran because of an input rather than a tick. */
    default boolean isTriggeredByInput() {
        return triggeredInput() >= 0;
    }

    /** Whether any input at all is receiving power. */
    default boolean anyInput() {
        for (int i = 0; i < inputCount(); i++) {
            if (input(i)) {
                return true;
            }
        }
        return false;
    }

    /** Whether every input is receiving power. */
    default boolean allInputs() {
        for (int i = 0; i < inputCount(); i++) {
            if (!input(i)) {
                return false;
            }
        }
        return inputCount() > 0;
    }

    /** The number of inputs a builder has wired something to. */
    default int connectedInputCount() {
        int count = 0;
        for (int i = 0; i < inputCount(); i++) {
            if (isConnected(i)) {
                count++;
            }
        }
        return count;
    }

    /** The number of wired inputs currently receiving power. */
    default int connectedPoweredCount() {
        int count = 0;
        for (int i = 0; i < inputCount(); i++) {
            if (isConnected(i) && input(i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Whether the chip is being driven at all, by any wired input or by a power source behind
     * the sign.
     *
     * <p>This is the notion of "on" used by the chips that have no particular input of interest,
     * such as the repeater and the inverter.
     */
    default boolean isAnyInputActive() {
        for (int i = 0; i < inputCount(); i++) {
            if (isConnected(i) && input(i)) {
                return true;
            }
        }
        return hasPowerSourceBehind();
    }

    /** The number of inputs currently receiving power. */
    default int poweredInputCount() {
        int count = 0;
        for (int i = 0; i < inputCount(); i++) {
            if (input(i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Drives every output to the same value.
     *
     * <p>Convenient for chips whose outputs all mirror one another.
     */
    default void setAllOutputs(boolean value) {
        for (int i = 0; i < outputCount(); i++) {
            setOutput(i, value);
        }
    }

    /**
     * The chip's main input, which is input zero.
     *
     * <p>Named for readability in the many chips that only care about one input.
     */
    default boolean mainInput() {
        return input(0);
    }

    /** Drives the chip's main output, which is output zero. */
    default void setMainOutput(boolean value) {
        setOutput(0, value);
    }

    /** The chip's main output, which is output zero. */
    default boolean mainOutput() {
        return output(0);
    }
}
