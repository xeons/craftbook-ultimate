package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.ManualScheduler;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.platform.TimeSource;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.Arrays;
import org.jspecify.annotations.NullMarked;

/**
 * A {@link ChipState} held entirely in memory.
 *
 * <p>This is what a chip runs against in a unit test: set the inputs, run the logic, assert on
 * the outputs, with no world and no server involved. It is also useful at runtime for
 * simulating a chip without touching blocks.
 *
 * <p>Outputs honour {@linkplain ICMode#invertsOutputs() the inverting mode}, so a test that
 * supplies an inverting mode sees the same values a real chip would drive.
 *
 * <p>Instances are not thread safe.
 */
@NullMarked
public final class SimpleChipState implements ChipState {

    /** The highest redstone power level. */
    private static final int FULL_POWER = 15;

    private final boolean[] inputs;
    private final int[] inputPower;
    private final boolean[] connected;
    private final boolean[] outputs;
    private final ICMode mode;
    private final ManualScheduler scheduler;
    private final SimpleChipWorld world;
    private final Vec3i signPosition;
    private final BlockFace facing;
    private SignLines sign;
    private TimeSource time;
    private boolean powerSourceBehind;
    private int triggeredInput;

    private SimpleChipState(Builder builder) {
        this.inputs = builder.inputs.clone();
        this.inputPower = builder.inputPower.clone();
        this.connected = builder.connected.clone();
        this.outputs = builder.outputs.clone();
        this.sign = builder.sign;
        this.mode = builder.mode;
        this.scheduler = builder.scheduler;
        this.world = builder.world;
        this.signPosition = builder.signPosition;
        this.facing = builder.facing;
        this.time = builder.time;
        this.powerSourceBehind = builder.powerSourceBehind;
        this.triggeredInput = builder.triggeredInput;
    }

    /**
     * Starts building a chip state with the pin counts of a layout.
     *
     * @param layout the layout whose input and output counts to use
     */
    public static Builder forLayout(PinLayout layout) {
        return new Builder(layout.inputCount(), layout.outputCount());
    }

    /**
     * Starts building a chip state with explicit pin counts.
     *
     * @param inputCount the number of inputs
     * @param outputCount the number of outputs
     */
    public static Builder of(int inputCount, int outputCount) {
        return new Builder(inputCount, outputCount);
    }

    @Override
    public int inputCount() {
        return inputs.length;
    }

    @Override
    public int outputCount() {
        return outputs.length;
    }

    @Override
    public boolean input(int index) {
        checkInput(index);
        return inputs[index];
    }

    @Override
    public int inputPower(int index) {
        checkInput(index);
        return inputPower[index];
    }

    @Override
    public boolean output(int index) {
        checkOutput(index);
        return outputs[index];
    }

    @Override
    public void setOutput(int index, boolean value) {
        checkOutput(index);
        outputs[index] = mode.invertsOutputs() != value;
    }

    @Override
    public boolean isConnected(int index) {
        checkInput(index);
        return connected[index];
    }

    @Override
    public boolean hasPowerSourceBehind() {
        return powerSourceBehind;
    }

    @Override
    public int triggeredInput() {
        return triggeredInput;
    }

    @Override
    public void setSignLine(int index, String text) {
        sign = sign.withLine(index, text);
    }

    @Override
    public SignLines sign() {
        return sign;
    }

    @Override
    public ICMode mode() {
        return mode;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public TimeSource time() {
        return time;
    }

    @Override
    public ChipWorld world() {
        return world;
    }

    @Override
    public Vec3i signPosition() {
        return signPosition;
    }

    @Override
    public BlockFace facing() {
        return facing;
    }

    /** The world this chip sees, so a test can place blocks in it and read back what changed. */
    public SimpleChipWorld simpleWorld() {
        return world;
    }

    /**
     * The scheduler this chip uses, so a test can advance it and see what the chip does next.
     */
    public ManualScheduler manualScheduler() {
        return scheduler;
    }

    /** Moves the clock this chip reads. */
    public SimpleChipState withTime(TimeSource time) {
        this.time = time;
        return this;
    }

    /** Moves the world clock this chip reads, leaving the wall clock alone. */
    public SimpleChipState withWorldTicks(long worldTicks) {
        return withTime(TimeSource.fixed(worldTicks, time.unixSeconds()));
    }

    /** Moves the wall clock this chip reads, leaving the world clock alone. */
    public SimpleChipState withUnixSeconds(long unixSeconds) {
        return withTime(TimeSource.fixed(time.worldTicks(), unixSeconds));
    }

    /**
     * Sets an input, updating its power level to match.
     *
     * <p>Returns this state so a test can chain several changes together.
     */
    public SimpleChipState withInput(int index, boolean value) {
        checkInput(index);
        inputs[index] = value;
        inputPower[index] = value ? FULL_POWER : 0;
        return this;
    }

    /**
     * Sets the power level on an input, updating its boolean value to match.
     *
     * @param power the power level, from 0 to 15
     */
    public SimpleChipState withInputPower(int index, int power) {
        checkInput(index);
        checkPower(power);
        inputPower[index] = power;
        inputs[index] = power > 0;
        return this;
    }

    /** Sets every input at once, in order. */
    public SimpleChipState withInputs(boolean... values) {
        if (values.length != inputs.length) {
            throw new IllegalArgumentException(
                    "Expected " + inputs.length + " inputs, got " + values.length);
        }
        for (int i = 0; i < values.length; i++) {
            withInput(i, values[i]);
        }
        return this;
    }

    /** Marks whether a builder has wired anything to an input. */
    public SimpleChipState withConnected(int index, boolean value) {
        checkInput(index);
        connected[index] = value;
        return this;
    }

    /** Marks whether the block behind the sign is itself a power source. */
    public SimpleChipState withPowerSourceBehind(boolean value) {
        this.powerSourceBehind = value;
        return this;
    }

    /** Records which input caused the run, or {@code -1} for none. */
    public SimpleChipState withTriggeredInput(int index) {
        if (index < -1 || index >= inputs.length) {
            throw new IndexOutOfBoundsException(
                    "Triggered input " + index + " is outside the range -1 to " + (inputs.length - 1));
        }
        this.triggeredInput = index;
        return this;
    }

    /**
     * Presets an output without going through the inverting mode.
     *
     * <p>Used to set up a starting condition; chips that read their own previous output, such as
     * the flip-flops, need this.
     */
    public SimpleChipState withRawOutput(int index, boolean value) {
        checkOutput(index);
        outputs[index] = value;
        return this;
    }

    /** Every output, in order. */
    public boolean[] outputs() {
        return outputs.clone();
    }

    /** Every input, in order. */
    public boolean[] inputs() {
        return inputs.clone();
    }

    private void checkInput(int index) {
        if (index < 0 || index >= inputs.length) {
            throw new IndexOutOfBoundsException(
                    "Input " + index + " is outside the range 0 to " + (inputs.length - 1));
        }
    }

    private void checkOutput(int index) {
        if (index < 0 || index >= outputs.length) {
            throw new IndexOutOfBoundsException(
                    "Output " + index + " is outside the range 0 to " + (outputs.length - 1));
        }
    }

    private static void checkPower(int power) {
        if (power < 0 || power > FULL_POWER) {
            throw new IllegalArgumentException(
                    "Redstone power must be between 0 and " + FULL_POWER + ", got " + power);
        }
    }

    @Override
    public String toString() {
        return "SimpleChipState[in=" + Arrays.toString(inputs)
                + ", out=" + Arrays.toString(outputs)
                + ", triggered=" + triggeredInput + ']';
    }

    /** Assembles a {@link SimpleChipState}. */
    public static final class Builder {

        private final boolean[] inputs;
        private final int[] inputPower;
        private final boolean[] connected;
        private final boolean[] outputs;
        private SignLines sign = SignLines.EMPTY;
        private ICMode mode = ICMode.NONE;
        private final ManualScheduler scheduler = new ManualScheduler();
        private SimpleChipWorld world = new SimpleChipWorld();
        private Vec3i signPosition = Vec3i.ZERO;
        private BlockFace facing = BlockFace.SOUTH;
        private TimeSource time = TimeSource.fixed(0, 0);
        private boolean powerSourceBehind;
        private int triggeredInput = -1;

        private Builder(int inputCount, int outputCount) {
            if (inputCount < 0 || outputCount < 0) {
                throw new IllegalArgumentException("Pin counts must not be negative");
            }
            this.inputs = new boolean[inputCount];
            this.inputPower = new int[inputCount];
            this.connected = new boolean[inputCount];
            this.outputs = new boolean[outputCount];
            // A chip is assumed fully wired unless the caller says otherwise.
            Arrays.fill(this.connected, true);
        }

        /** Sets the initial state of every input, in order. */
        public Builder inputs(boolean... values) {
            if (values.length != inputs.length) {
                throw new IllegalArgumentException(
                        "Expected " + inputs.length + " inputs, got " + values.length);
            }
            for (int i = 0; i < values.length; i++) {
                inputs[i] = values[i];
                inputPower[i] = values[i] ? FULL_POWER : 0;
            }
            return this;
        }

        /** Sets which inputs a builder has wired something to, in order. */
        public Builder connected(boolean... values) {
            if (values.length != connected.length) {
                throw new IllegalArgumentException(
                        "Expected " + connected.length + " inputs, got " + values.length);
            }
            System.arraycopy(values, 0, connected, 0, values.length);
            return this;
        }

        /** Sets the world this chip sees. */
        public Builder world(SimpleChipWorld world) {
            this.world = world;
            return this;
        }

        /** Places this chip's sign, which is what everything it does is measured from. */
        public Builder at(Vec3i signPosition, BlockFace facing) {
            this.signPosition = signPosition;
            this.facing = facing;
            return this;
        }

        /** Sets the clock this chip reads. */
        public Builder time(TimeSource time) {
            this.time = time;
            return this;
        }

        /** Sets the world age this chip reads, leaving the wall clock at zero. */
        public Builder worldTicks(long worldTicks) {
            return time(TimeSource.fixed(worldTicks, 0));
        }

        /** Marks the block behind the sign as a power source. */
        public Builder powerSourceBehind(boolean value) {
            this.powerSourceBehind = value;
            return this;
        }

        /** Sets the sign the chip reads its configuration from. */
        public Builder sign(SignLines sign) {
            this.sign = sign;
            return this;
        }

        /** Sets the sign from plain text lines. */
        public Builder sign(String... lines) {
            return sign(SignLines.of(lines));
        }

        /** Sets the mode written on the sign. */
        public Builder mode(ICMode mode) {
            this.mode = mode;
            return this;
        }

        /** Records which input caused the run. */
        public Builder triggeredInput(int index) {
            this.triggeredInput = index;
            return this;
        }

        public SimpleChipState build() {
            return new SimpleChipState(this);
        }
    }
}
