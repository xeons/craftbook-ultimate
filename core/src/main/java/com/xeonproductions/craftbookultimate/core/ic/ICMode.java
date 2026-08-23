// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import java.util.Arrays;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The optional modifier written after the closing bracket of an IC's model reference.
 *
 * <p>A mode string is at most two things: a single leading character selecting a behaviour, and
 * an optional pin permutation that renames the chip's pins so a build can be wired from a
 * different side. Both parts are optional, so {@code [MC1000]} carries no mode at all.
 *
 * <pre>
 *   [MC1000]!         invert the outputs
 *   [MC1000]ba        swap the first two inputs
 *   [MC1000]!ba       both at once
 * </pre>
 *
 * <p>Parsing is total: anything that is not recognised yields {@link Behaviour#NONE} with no
 * permutation, because a typo in a mode string should leave a working chip rather than a broken
 * one. Whether a permutation the letters do describe is one a given chip can honour is a second
 * question, asked by {@link #fittedTo} once that chip's own wiring is known.
 *
 * @param behaviour the selected behaviour, or {@link Behaviour#NONE}
 * @param permutation the pin permutation, or empty when the pins keep their normal order
 */
@NullMarked
public record ICMode(Behaviour behaviour, Optional<PinPermutation> permutation) {

    /** A mode carrying no behaviour and no permutation. */
    public static final ICMode NONE = new ICMode(Behaviour.NONE, Optional.empty());

    /** The lowest letter usable in a pin permutation. */
    private static final char FIRST_PIN_LETTER = 'a';

    /** The number of distinct pin letters, {@code a} through {@code f}. */
    private static final int MAX_PERMUTED_PINS = 6;

    /**
     * The behaviour a mode character selects.
     *
     * <p>Most of these are consumed by one specific family of chips and ignored by the rest;
     * the invert mode is the only one the framework itself applies, when writing outputs.
     */
    public enum Behaviour {
        /** No modifier. */
        NONE(' '),
        /** Invert every output the chip writes. */
        INVERT('!'),
        /** Also announce the chip's action to nearby players. */
        LOG_TO_NEARBY_PLAYERS('+'),
        /** Return to the off state after acting, rather than latching. */
        CYCLE_OFF('1'),
        /** Do not apply the usual vertical offset when acting on the world. */
        NO_Y_OFFSET('='),
        /** Run the chip's effect in reverse. */
        REVERSE('r'),
        /** Treat the weather condition as a thunderstorm rather than rain. */
        THUNDER_STORM('t'),
        /** Spare non-player entities that the chip would otherwise affect. */
        HUMANS_ONLY_EXTRA('-'),
        /** Behave as a teleport pad. */
        TELEPORT_PAD('p'),
        /** Behave as a teleport pad that insists on a pressure plate. */
        TELEPORT_PAD_FORCED_PRESSURE_PLATE('P');

        private final char symbol;

        Behaviour(char symbol) {
            this.symbol = symbol;
        }

        /** The character that selects this behaviour on a sign. */
        public char symbol() {
            return symbol;
        }

        /**
         * Looks up a behaviour by its mode character. Matching is case sensitive, because
         * {@code p} and {@code P} are two different modes.
         */
        public static Optional<Behaviour> bySymbol(char symbol) {
            for (Behaviour behaviour : values()) {
                if (behaviour != NONE && behaviour.symbol == symbol) {
                    return Optional.of(behaviour);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * A renaming of a chip's pins.
     *
     * <p>Written on the sign as a run of the letters {@code a} to {@code f}, where the position
     * of a letter is the pin being described and the letter itself is the physical position that
     * pin should use. So {@code bacd} sends pin 0 to physical slot 1, pin 1 to slot 0, and leaves
     * the two after them where they are.
     *
     * <p>The letters stand for the pins in the order the layout numbers them, which is the inputs
     * first and the outputs after. Whether a particular run of them is one a particular chip can
     * honour is {@link #fits}.
     */
    public record PinPermutation(int[] slots) {

        public PinPermutation {
            slots = slots.clone();
        }

        /** The number of pins this permutation describes. */
        public int size() {
            return slots.length;
        }

        /**
         * Whether a chip wired this way can honour the permutation.
         *
         * <p>Three things have to hold.
         *
         * <p>It cannot describe more pins than the chip has, since the extra letters would name
         * blocks that are not part of the chip at all.
         *
         * <p>Its letters have to be exactly the pins it describes rather than any few of them,
         * because a short permutation leaves the pins past its end where they are: {@code ac}
         * sends pin 1 to slot 2 while pin 2 is already sitting there, which would put two of the
         * chip's pins on one block.
         *
         * <p>And every pin has to stay on its own side — an input on an input, an output on an
         * output. A chip whose pins are shuffled is still meant to be the same chip wired from
         * somewhere else, not one that reads the block it drives.
         *
         * @param layout the wiring the chip is actually built with
         */
        public boolean fits(PinLayout layout) {
            if (slots.length > layout.pinCount()) {
                return false;
            }
            for (int pin = 0; pin < slots.length; pin++) {
                if (slots[pin] >= slots.length || layout.isInput(pin) != layout.isInput(slots[pin])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * The physical slot a logical pin should use.
         *
         * <p>Pins beyond the end of the permutation keep their own position, so a short
         * permutation leaves the remaining pins untouched rather than failing.
         */
        public int slotFor(int pin) {
            if (pin < 0) {
                throw new IndexOutOfBoundsException("Pin index must not be negative, got " + pin);
            }
            return pin < slots.length ? slots[pin] : pin;
        }

        @Override
        public int[] slots() {
            return slots.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof PinPermutation that && Arrays.equals(slots, that.slots);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(slots);
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder(slots.length);
            for (int slot : slots) {
                out.append((char) (FIRST_PIN_LETTER + slot));
            }
            return out.toString();
        }
    }

    /**
     * Parses the mode string taken from an IC sign.
     *
     * @param raw the text following the closing bracket, with its case intact
     * @return the parsed mode, never null and never failing
     */
    public static ICMode parse(String raw) {
        String text = raw.strip();
        if (text.isEmpty()) {
            return NONE;
        }

        // A mode string is a behaviour character followed by an optional permutation. When the
        // first character is itself a pin letter there is no behaviour, only a permutation.
        Behaviour behaviour = Behaviour.NONE;
        String remainder = text;
        Optional<Behaviour> leading = Behaviour.bySymbol(text.charAt(0));
        if (leading.isPresent()) {
            behaviour = leading.get();
            remainder = text.substring(1);
        }

        return new ICMode(behaviour, parsePermutation(remainder));
    }

    /**
     * Reads a pin permutation.
     *
     * <p>A run of letters is a permutation when every letter is in range and appears at most
     * once. Anything else is treated as noise and ignored.
     *
     * @return the permutation, or empty when the text is not one
     */
    private static Optional<PinPermutation> parsePermutation(String text) {
        if (text.isEmpty() || text.length() > MAX_PERMUTED_PINS) {
            return Optional.empty();
        }

        int[] slots = new int[text.length()];
        boolean[] used = new boolean[MAX_PERMUTED_PINS];
        for (int i = 0; i < text.length(); i++) {
            int slot = text.charAt(i) - FIRST_PIN_LETTER;
            if (slot < 0 || slot >= MAX_PERMUTED_PINS || used[slot]) {
                return Optional.empty();
            }
            used[slot] = true;
            slots[i] = slot;
        }

        return Optional.of(new PinPermutation(slots));
    }

    /** True when the outputs of the chip should be written inverted. */
    public boolean invertsOutputs() {
        return behaviour == Behaviour.INVERT;
    }

    /**
     * The physical slot a logical pin should use, taking any permutation into account.
     *
     * @param pin the logical pin index
     */
    public int slotFor(int pin) {
        return permutation.map(p -> p.slotFor(pin)).orElse(pin);
    }

    /**
     * This mode with any permutation the chip's own wiring cannot honour dropped.
     *
     * <p>A permutation is read without knowing which chip it is written on, because a mode string
     * is read before the chip is looked up. Whether the letters name pins that chip has, and
     * whether they leave each one on its own side, cannot be answered until the two meet.
     *
     * <p>Dropping it is what keeps the promise {@link #parse} makes, that a mode string nobody can
     * honour leaves a working chip rather than a broken one.
     *
     * @param layout the wiring the chip is actually built with
     */
    public ICMode fittedTo(PinLayout layout) {
        if (permutation.isEmpty() || permutation.get().fits(layout)) {
            return this;
        }
        return new ICMode(behaviour, Optional.empty());
    }

    /** True when this mode does nothing at all. */
    public boolean isNone() {
        return behaviour == Behaviour.NONE && permutation.isEmpty();
    }
}
