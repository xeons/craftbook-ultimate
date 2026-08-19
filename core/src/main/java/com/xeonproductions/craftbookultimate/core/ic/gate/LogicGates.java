package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import org.jspecify.annotations.NullMarked;

/**
 * The combinational gates: chips whose output depends only on the inputs wired to them.
 *
 * <p>Two shapes appear here. An <em>any-input</em> gate folds every wired input into one answer
 * and so works with however many wires a builder connected. A <em>two-input</em> gate takes the
 * first two wired inputs and ignores the rest.
 *
 * <p>Both shapes count only pins that are actually wired. A gate with a bare pin behaves as a
 * gate with fewer inputs rather than treating the bare pin as reading low.
 */
@NullMarked
public final class LogicGates {

    private LogicGates() {}

    /**
     * Folds every wired input into a single output.
     *
     * <p>Implementations answer in terms of how many inputs are wired and how many of those are
     * powered, which is what lets one gate serve any number of wires.
     */
    @FunctionalInterface
    public interface AnyInputGate extends ICLogic {

        /**
         * Computes the output.
         *
         * @param wired how many inputs a builder has connected
         * @param powered how many of those are currently receiving power
         */
        boolean compute(int wired, int powered);

        @Override
        default void trigger(ChipState state) {
            state.setMainOutput(compute(state.connectedInputCount(), state.connectedPoweredCount()));
        }
    }

    /**
     * Combines the first two wired inputs.
     *
     * <p>A gate of this shape does nothing at all until two inputs are wired, because a binary
     * operation on one value is not defined.
     */
    @FunctionalInterface
    public interface TwoInputGate extends ICLogic {

        /** Combines the two operands. */
        boolean compute(boolean a, boolean b);

        @Override
        default void trigger(ChipState state) {
            int found = 0;
            boolean a = false;
            boolean b = false;

            for (int i = 0; i < state.inputCount() && found < 2; i++) {
                if (!state.isConnected(i)) {
                    continue;
                }
                if (found == 0) {
                    a = state.input(i);
                } else {
                    b = state.input(i);
                }
                found++;
            }

            if (found == 2) {
                state.setMainOutput(compute(a, b));
            }
        }
    }

    /** Outputs high when at least two inputs are wired and every wired input is powered. */
    public static AnyInputGate and() {
        return (wired, powered) -> wired >= 2 && wired == powered;
    }

    /** Outputs high when at least one input is wired and not all wired inputs are powered. */
    public static AnyInputGate nand() {
        return (wired, powered) -> wired > 0 && powered != wired;
    }

    /** Outputs high when the two operands differ. */
    public static TwoInputGate xor() {
        return (a, b) -> a != b;
    }

    /** Outputs high when the two operands match. */
    public static TwoInputGate xnor() {
        return (a, b) -> a == b;
    }

    /**
     * Mirrors its input.
     *
     * <p>Driven by any wired input or by a power source behind the sign, so it acts as an OR of
     * everything attached to it.
     */
    public static ICLogic repeater() {
        return state -> state.setMainOutput(state.isAnyInputActive());
    }

    /** Outputs the opposite of {@link #repeater()}. */
    public static ICLogic inverter() {
        return state -> state.setMainOutput(!state.isAnyInputActive());
    }
}
