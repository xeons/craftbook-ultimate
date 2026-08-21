// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import org.jspecify.annotations.NullMarked;

/**
 * The single-bit arithmetic chips.
 *
 * <p>All four use the 3I3O layout and share an output convention: output 0 carries the sum or
 * difference, and outputs 1 and 2 both carry the carry or borrow. Duplicating the carry onto two
 * pins lets a builder chain units in either direction without a separate splitter.
 */
@NullMarked
public final class Arithmetic {

    private Arithmetic() {}

    /**
     * Adds two bits.
     *
     * <p>Input 0 is unused, so a half adder can be dropped in where a full adder was without
     * rewiring the operands.
     *
     * <p>Pins: 1 and 2 are the addends; output 0 is the sum, outputs 1 and 2 the carry.
     */
    public static ICLogic halfAdder() {
        return state -> {
            boolean a = state.input(1);
            boolean b = state.input(2);

            writeResult(state, a ^ b, a & b);
        };
    }

    /**
     * Adds two bits and a carry.
     *
     * <p>Pins: 0 is the carry in, 1 and 2 the addends; output 0 is the sum, outputs 1 and 2 the
     * carry out.
     */
    public static ICLogic fullAdder() {
        return state -> {
            boolean carryIn = state.input(0);
            boolean a = state.input(1);
            boolean b = state.input(2);

            boolean sum = carryIn ^ a ^ b;
            boolean carryOut = (a & b) | ((a ^ b) & carryIn);

            writeResult(state, sum, carryOut);
        };
    }

    /**
     * Subtracts one bit from another.
     *
     * <p>Input 0 is unused, matching {@link #halfAdder()}.
     *
     * <p>Pins: 1 is the minuend and 2 the subtrahend; output 0 is the difference, outputs 1 and 2
     * the borrow.
     */
    public static ICLogic halfSubtractor() {
        return state -> {
            boolean minuend = state.input(1);
            boolean subtrahend = state.input(2);

            writeResult(state, minuend ^ subtrahend, !minuend & subtrahend);
        };
    }

    /**
     * Subtracts a bit and a borrow from another bit.
     *
     * <p>Pins: 0 is the minuend, 1 the subtrahend and 2 the borrow in; output 0 is the
     * difference, outputs 1 and 2 the borrow out.
     */
    public static ICLogic fullSubtractor() {
        return state -> {
            boolean minuend = state.input(0);
            boolean subtrahend = state.input(1);
            boolean borrowIn = state.input(2);

            boolean difference = minuend ^ subtrahend ^ borrowIn;
            boolean borrowOut = (!minuend & subtrahend)
                    | (!minuend & borrowIn)
                    | (subtrahend & borrowIn);

            writeResult(state, difference, borrowOut);
        };
    }

    /** Writes the shared output convention: the value on output 0, the carry on outputs 1 and 2. */
    private static void writeResult(ChipState state, boolean value, boolean carry) {
        state.setOutput(0, value);
        state.setOutput(1, carry);
        state.setOutput(2, carry);
    }
}
