// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.control.Switchboard;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The chips that hold state of their own rather than computing from their inputs.
 *
 * <p>What they have in common is memory: a shift register that remembers a row of bits, a timer
 * that remembers how far through a countdown it is, and the two chips that follow a switch thrown
 * by somebody typing a command somewhere else entirely.
 */
@NullMarked
public final class Control {

    /** The line naming a switch, a register's size or a reader's target. */
    private static final int CONFIG_LINE = 2;

    /** The line a chip keeps its saved state on. */
    private static final int STATE_LINE = 3;

    /** The line a chip's title, and any running marker, is written to. */
    private static final int TITLE_LINE = 0;

    private Control() {}

    /**
     * Follows a switch that anyone may throw by command.
     *
     * <p>Line 3 names the switch. The chip drives its output to match wherever that switch is
     * standing, and leaves it alone until somebody has thrown it for the first time.
     *
     * <p>A switch exists only while a chip is following it, so the name is claimed as the chip
     * loads and given up as it unloads.
     */
    public static SelfTriggeringICLogic commandControlled() {
        return new SwitchFollower(ChipState::switchboard);
    }

    /**
     * Follows a switch that takes a password to throw.
     *
     * <p>The same chip as {@link #commandControlled()} in every respect a builder can see. The
     * difference is which switchboard it reads, and the switches on that one cannot be moved
     * without the password.
     */
    public static SelfTriggeringICLogic passwordControlled() {
        return new SwitchFollower(ChipState::guardedSwitchboard);
    }

    /**
     * Remembers a row of bits and rotates them along.
     *
     * <p>Line 3 gives how many bits the register holds, from 2 to 64, and defaults to eight.
     * Line 4 holds the bits themselves as text, written there when the chip unloads and read back
     * when it loads, so a register keeps its contents while nobody is watching it.
     *
     * <p>Pins: 0 rotates the row along when pin 1 is high, and 1 writes pin 2 into the first bit
     * when pin 0 is high. Whichever pin changed decides which of the two happens, so the other
     * acts as the selector. The output always shows the first bit.
     *
     * <p>Rotating carries the first bit around to the end rather than dropping it, so a pattern
     * loaded once goes round for as long as the chip is clocked.
     */
    public static SelfTriggeringICLogic bitShift() {
        return new BitShift();
    }

    /**
     * Waits, then turns on.
     *
     * <p>Line 3 reads {@code count:rate}, optionally followed by {@code :onCount}. A pulse starts
     * the countdown; nothing restarts or stops it once it is running. After {@code count} counts,
     * each of which takes {@code rate} ticks, the output goes high. With an {@code onCount} it
     * goes low again that many counts later; without one it stays high until the chip is started
     * again. The {@code 1} mode is shorthand for an {@code onCount} of one.
     *
     * <p>The title line shows which of the two the chip is doing, so a builder can see at a glance
     * whether a timer is waiting or running.
     */
    public static SelfTriggeringICLogic monoflop() {
        return new Monoflop();
    }

    /**
     * Mirrors the redstone at somewhere else in the world.
     *
     * <p>Line 3 reads {@code x:y:z} as a step from the chip's own sign, optionally prefixed with
     * {@code !} to show the opposite of what is there. The step may be a long way: up to 999
     * blocks horizontally and 255 vertically.
     *
     * <p>Somewhere that far off may not be readable from where this chip runs, either because it
     * is not loaded or because it belongs to another region. The chip then leaves its output where
     * it is rather than reporting the place as unpowered.
     */
    public static SelfTriggeringICLogic triggerReader() {
        return new TriggerReader();
    }

    /**
     * Drives its output from a named switch on one of the switchboards.
     *
     * @param board which switchboard this chip reads
     */
    private static final class SwitchFollower implements SelfTriggeringICLogic {

        private final Function<ChipState, Switchboard> board;

        /**
         * The name this chip claimed as it loaded, so that it gives back exactly that.
         *
         * <p>Re-reading the sign at unload is not good enough. A chip unloads because its sign has
         * gone as often as for any other reason, and a sign that has gone reads as four blank
         * lines — so the name would never be given back and the switch would stay throwable with
         * nothing following it for as long as the server ran.
         */
        private String claimed = "";

        SwitchFollower(Function<ChipState, Switchboard> board) {
            this.board = board;
        }

        @Override
        public void load(ChipState state) {
            claimed = nameOn(state);
            if (!claimed.isEmpty()) {
                board.apply(state).register(claimed);
            }
            follow(state);
        }

        @Override
        public void trigger(ChipState state) {
            follow(state);
        }

        @Override
        public void tick(ChipState state) {
            follow(state);
        }

        @Override
        public void unload(ChipState state) {
            if (!claimed.isEmpty()) {
                board.apply(state).forget(claimed);
                claimed = "";
            }
        }

        private void follow(ChipState state) {
            String name = nameOn(state);
            if (name.isEmpty()) {
                return;
            }
            board.apply(state).state(name).ifPresent(state::setMainOutput);
        }

        private static String nameOn(ChipState state) {
            return state.sign().trimmedText(CONFIG_LINE);
        }
    }

    /** A row of bits that rotates along on command. */
    private static final class BitShift implements SelfTriggeringICLogic {

        /** The fewest bits a register may hold. */
        private static final int MIN_BITS = 2;

        /** The most bits a register may hold, which is what fits in the saved form. */
        private static final int MAX_BITS = 64;

        /** How many bits a register holds when its sign does not say. */
        private static final int DEFAULT_BITS = 8;

        /** The pin that rotates the row along. */
        private static final int SHIFT_PIN = 0;

        /** The pin that writes a bit. */
        private static final int WRITE_PIN = 1;

        /** The pin holding the bit to be written. */
        private static final int DATA_PIN = 2;

        private static final Base64.Decoder DECODER = Base64.getMimeDecoder();
        private static final Base64.Encoder ENCODER = Base64.getEncoder();

        private boolean @Nullable [] bits;

        @Override
        public boolean alwaysSelfTriggering() {
            return true;
        }

        @Override
        public void load(ChipState state) {
            int size = sizeOn(state);
            boolean[] saved = decode(state.sign().trimmedText(STATE_LINE));

            bits = new boolean[size];
            System.arraycopy(saved, 0, bits, 0, Math.min(saved.length, size));
        }

        @Override
        public void unload(ChipState state) {
            if (bits != null) {
                state.setSignLine(STATE_LINE, ENCODER.encodeToString(pack(bits)));
            }
        }

        @Override
        public void trigger(ChipState state) {
            boolean[] row = bits;
            if (row == null || row.length == 0) {
                return;
            }

            // Each pin acts on the other's level, so a builder chooses which of the two things
            // happens by which pin they pulse.
            if (state.isTriggered(WRITE_PIN) && state.input(SHIFT_PIN)) {
                bits = rotated(row);
            } else if (state.isTriggered(SHIFT_PIN) && state.input(WRITE_PIN)) {
                row[0] = state.input(DATA_PIN);
            }
        }

        @Override
        public void tick(ChipState state) {
            boolean[] row = bits;
            if (row != null && row.length > 0) {
                state.setMainOutput(row[0]);
            }
        }

        /** How many bits this register holds, per its sign. */
        private static int sizeOn(ChipState state) {
            String written = state.sign().trimmedText(CONFIG_LINE);
            if (written.isEmpty()) {
                return DEFAULT_BITS;
            }
            try {
                int size = Integer.parseInt(written);
                return size < MIN_BITS || size > MAX_BITS ? DEFAULT_BITS : size;
            } catch (NumberFormatException e) {
                return DEFAULT_BITS;
            }
        }

        /** The row moved one place along, with the first bit carried around to the end. */
        private static boolean[] rotated(boolean[] row) {
            boolean[] moved = new boolean[row.length];
            System.arraycopy(row, 1, moved, 0, row.length - 1);
            moved[moved.length - 1] = row[0];
            return moved;
        }

        /** The row as bytes, first bit in the highest place of the first byte. */
        static byte[] pack(boolean[] row) {
            byte[] bytes = new byte[(row.length + Byte.SIZE - 1) / Byte.SIZE];
            for (int index = 0; index < row.length; index++) {
                if (row[index]) {
                    bytes[index / Byte.SIZE] |= (byte) (1 << (Byte.SIZE - 1 - index % Byte.SIZE));
                }
            }
            return bytes;
        }

        /** The bits held in some bytes, in the order {@link #pack} wrote them. */
        static boolean[] unpack(byte[] bytes) {
            boolean[] row = new boolean[bytes.length * Byte.SIZE];
            for (int index = 0; index < row.length; index++) {
                int mask = 1 << (Byte.SIZE - 1 - index % Byte.SIZE);
                row[index] = (bytes[index / Byte.SIZE] & mask) != 0;
            }
            return row;
        }

        /** Reads a saved row, treating anything unreadable as an empty register. */
        static boolean[] decode(String saved) {
            if (saved.isEmpty()) {
                return new boolean[0];
            }
            try {
                byte[] bytes = DECODER.decode(saved);
                return bytes.length * Byte.SIZE > MAX_BITS
                        ? unpack(Arrays.copyOf(bytes, MAX_BITS / Byte.SIZE))
                        : unpack(bytes);
            } catch (IllegalArgumentException e) {
                return new boolean[0];
            }
        }
    }

    /** Waits out a countdown, then turns on for a while or for good. */
    private static final class Monoflop implements SelfTriggeringICLogic {

        /** What the title line reads while the timer is waiting to be started. */
        private static final String READY = "^MONOFLOP";

        /** What the title line reads while the timer is counting or on. */
        private static final String RUNNING = "%MONOFLOP";

        /** The shortest a count may be, in ticks. */
        private static final int MIN_RATE = 5;

        /** The longest a count may be, in ticks. */
        private static final int MAX_RATE = 15;

        /** The most counts a timer may wait or stay on for. */
        private static final int MAX_COUNT = 99_999;

        private boolean started;
        private int remaining;
        private int ticksThisCount;

        @Override
        public boolean alwaysSelfTriggering() {
            return true;
        }

        @Override
        public void load(ChipState state) {
            settingsOn(state).ifPresent(settings -> state.setSignLine(TITLE_LINE, READY));
        }

        @Override
        public void trigger(ChipState state) {
            Optional<Settings> settings = settingsOn(state);
            // Nothing restarts or interrupts a timer that is already running, which is what makes
            // it a one-shot rather than something that follows its input.
            if (started || settings.isEmpty() || !state.isAnyInputActive()) {
                return;
            }

            started = true;
            remaining = settings.get().count();
            ticksThisCount = 0;
            state.setMainOutput(false);
            state.setSignLine(TITLE_LINE, RUNNING);
        }

        @Override
        public void tick(ChipState state) {
            if (!started) {
                return;
            }

            Optional<Settings> settings = settingsOn(state);
            if (settings.isEmpty()) {
                return;
            }

            if (ticksThisCount >= settings.get().rate()) {
                remaining--;
                ticksThisCount = 0;
            } else {
                ticksThisCount++;
            }

            if (remaining == 0) {
                state.setMainOutput(true);
                if (settings.get().onCount() == 0) {
                    // Staying on for good, so the timer is free to be started again straight away.
                    stop(state);
                }
            } else if (remaining < 0 && -remaining >= settings.get().onCount()) {
                state.setMainOutput(false);
                stop(state);
            }
        }

        private void stop(ChipState state) {
            started = false;
            remaining = 0;
            ticksThisCount = 0;
            state.setSignLine(TITLE_LINE, READY);
        }

        /**
         * How long to wait and how long to stay on.
         *
         * @param count how many counts to wait before turning on
         * @param rate how many ticks each count takes
         * @param onCount how many counts to stay on for, or zero to stay on until started again
         */
        private record Settings(int count, int rate, int onCount) {}

        /**
         * Reads the settings line.
         *
         * <p>A fourth field is ignored. Older signs carry the timer's own progress there, which
         * this chip keeps in memory instead.
         */
        private static Optional<Settings> settingsOn(ChipState state) {
            String[] parts = state.sign().trimmedText(CONFIG_LINE).split(":");
            if (parts.length < 2) {
                return Optional.empty();
            }

            try {
                int count = Integer.parseInt(parts[0].trim());
                int rate = Integer.parseInt(parts[1].trim());
                int onCount = parts.length > 2
                        ? Integer.parseInt(parts[2].trim())
                        : (state.mode().behaviour() == ICMode.Behaviour.CYCLE_OFF ? 1 : 0);

                if (count < 1 || count > MAX_COUNT
                        || rate < MIN_RATE || rate > MAX_RATE
                        || onCount < 0 || onCount > MAX_COUNT) {
                    return Optional.empty();
                }
                return Optional.of(new Settings(count, rate, onCount));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }

    /** Shows the redstone at somewhere else in the world. */
    private static final class TriggerReader implements SelfTriggeringICLogic {

        /** The marker that makes the chip show the opposite of what it reads. */
        private static final char INVERT_MARKER = '!';

        /** How far the target may be, horizontally. */
        private static final int MAX_HORIZONTAL = 999;

        /** How far the target may be, vertically. */
        private static final int MAX_VERTICAL = 255;

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                mirror(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            mirror(state);
        }

        private static void mirror(ChipState state) {
            String written = state.sign().trimmedText(CONFIG_LINE);
            boolean invert = !written.isEmpty() && written.charAt(0) == INVERT_MARKER;

            Optional<Vec3i> step = readStep(invert ? written.substring(1) : written);
            if (step.isEmpty()) {
                return;
            }

            state.world()
                    .poweredAt(state.signPosition().add(step.get()))
                    .ifPresent(powered -> state.setMainOutput(invert != powered));
        }

        /** Reads the step from the sign to the block being watched. */
        private static Optional<Vec3i> readStep(String written) {
            String[] parts = written.split(":");
            if (parts.length != 3) {
                return Optional.empty();
            }

            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());

                if (Math.abs(x) > MAX_HORIZONTAL
                        || Math.abs(z) > MAX_HORIZONTAL
                        || Math.abs(y) > MAX_VERTICAL) {
                    return Optional.empty();
                }
                return Optional.of(new Vec3i(x, y, z));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
}
