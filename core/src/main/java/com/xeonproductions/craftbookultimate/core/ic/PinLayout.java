package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * The physical arrangement of an IC's redstone pins around its sign.
 *
 * <p>A layout is pure data: it maps a pin index to a {@link PinOffset}, and knows nothing about
 * blocks, worlds or redstone. Everything that reads or drives a pin resolves its position
 * through here, so the wiring geometry has exactly one definition and can be tested directly.
 *
 * <p>Pins are numbered with the inputs first and the outputs after them, so a 3I3O chip has
 * inputs 0-2 and outputs 3-5. The names match the codes written on IC signs and in the
 * documentation, where {@code S} means single, {@code A} means all, {@code Z} means zero,
 * and the digits count inputs and outputs respectively.
 */
@NullMarked
public enum PinLayout {

    /** One input, one output. The workhorse layout for simple chips. */
    SISO("SISO",
            List.of(new PinOffset(1, 0, 0)),
            List.of(new PinOffset(-2, 0, 0))),

    /** Three inputs, one output. The default layout for logic gates. */
    THREE_I_SO("3ISO",
            List.of(new PinOffset(1, 0, 0), new PinOffset(0, 1, 0), new PinOffset(0, -1, 0)),
            List.of(new PinOffset(-2, 0, 0))),

    /** Four inputs surrounding the sign, one output. Any input can trigger the chip. */
    AISO("AISO",
            List.of(new PinOffset(1, 0, 0), new PinOffset(0, -1, 0),
                    new PinOffset(0, 0, -1), new PinOffset(0, 1, 0)),
            List.of(new PinOffset(-2, 0, 0))),

    /** Three inputs and no outputs, for chips whose effect is entirely on the world. */
    AIZO("AIZO",
            List.of(new PinOffset(1, 0, 0), new PinOffset(0, -1, 0), new PinOffset(0, 1, 0)),
            List.of()),

    /** One input, three outputs. */
    SI3O("SI3O",
            List.of(new PinOffset(1, 0, 0)),
            List.of(new PinOffset(-2, 0, 0), new PinOffset(-1, -1, 0), new PinOffset(-1, 1, 0))),

    /** One input, five outputs. */
    SI5O("SI5O",
            List.of(new PinOffset(1, 0, 0)),
            List.of(new PinOffset(-4, 0, 0),
                    new PinOffset(-2, -1, 0), new PinOffset(-2, 1, 0),
                    new PinOffset(-3, -1, 0), new PinOffset(-3, 1, 0))),

    /** Three inputs, three outputs. Used by the adders and subtractors. */
    THREE_I_3O("3I3O",
            List.of(new PinOffset(1, 0, 0), new PinOffset(0, 1, 0), new PinOffset(0, -1, 0)),
            List.of(new PinOffset(-3, 0, 0), new PinOffset(-2, -1, 0), new PinOffset(-2, 1, 0))),

    /** Three inputs, five outputs. Used by the demultiplexer. */
    THREE_I_5O("3I5O",
            List.of(new PinOffset(1, 0, 0), new PinOffset(0, 1, 0), new PinOffset(0, -1, 0)),
            List.of(new PinOffset(-3, 0, 0),
                    new PinOffset(-2, -1, 0), new PinOffset(-2, 1, 0),
                    new PinOffset(1, -1, 0), new PinOffset(1, 1, 0)));

    private static final Map<String, PinLayout> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(PinLayout::code, Function.identity()));

    private final String code;
    private final List<PinOffset> inputs;
    private final List<PinOffset> outputs;
    private final List<PinOffset> all;

    PinLayout(String code, List<PinOffset> inputs, List<PinOffset> outputs) {
        this.code = code;
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.all = Stream.concat(inputs.stream(), outputs.stream()).toList();
    }

    /** The code used on signs and in the IC registry, such as {@code 3ISO}. */
    public String code() {
        return code;
    }

    public int inputCount() {
        return inputs.size();
    }

    public int outputCount() {
        return outputs.size();
    }

    /** The total number of pins; input indices come first, then output indices. */
    public int pinCount() {
        return all.size();
    }

    /** Translates an output number into its pin index. */
    public int outputPin(int output) {
        checkOutput(output);
        return inputs.size() + output;
    }

    /**
     * The offset of a pin from the sign's own block.
     *
     * @param pin the pin index, inputs first then outputs
     * @throws IndexOutOfBoundsException if the pin does not exist in this layout
     */
    public PinOffset offset(int pin) {
        if (pin < 0 || pin >= all.size()) {
            throw new IndexOutOfBoundsException(
                    "Pin " + pin + " is outside layout " + code + ", which has " + all.size() + " pins");
        }
        return all.get(pin);
    }

    /**
     * The world position of a pin.
     *
     * @param pin the pin index, inputs first then outputs
     * @param sign the position of the IC's sign
     * @param front the direction the sign's text faces
     */
    public Vec3i pinPosition(int pin, Vec3i sign, BlockFace front) {
        return sign.add(offset(pin).resolve(front));
    }

    /**
     * The world position of an input.
     *
     * @param input the input number, counted from zero
     */
    public Vec3i inputPosition(int input, Vec3i sign, BlockFace front) {
        checkInput(input);
        return pinPosition(input, sign, front);
    }

    /**
     * The world position of an output.
     *
     * @param output the output number, counted from zero
     */
    public Vec3i outputPosition(int output, Vec3i sign, BlockFace front) {
        return pinPosition(outputPin(output), sign, front);
    }

    /**
     * Finds which pin, if any, sits at a given world position.
     *
     * <p>Used to work out which pin a redstone change belongs to.
     *
     * @return the pin index, or empty if the position is not part of this chip
     */
    public Optional<Integer> pinAt(Vec3i position, Vec3i sign, BlockFace front) {
        for (int pin = 0; pin < all.size(); pin++) {
            if (pinPosition(pin, sign, front).equals(position)) {
                return Optional.of(pin);
            }
        }
        return Optional.empty();
    }

    /** True if the pin index refers to an input rather than an output. */
    public boolean isInput(int pin) {
        return pin >= 0 && pin < inputs.size();
    }

    /** True if the pin index refers to an output rather than an input. */
    public boolean isOutput(int pin) {
        return pin >= inputs.size() && pin < all.size();
    }

    /**
     * Looks up a layout by its sign code, ignoring case.
     *
     * @return the layout, or empty if no layout uses that code
     */
    public static Optional<PinLayout> byCode(String code) {
        return Optional.ofNullable(BY_CODE.get(code.trim().toUpperCase(Locale.ROOT)));
    }

    /** The layout used by chips that do not name one explicitly. */
    public static PinLayout defaultLayout() {
        return THREE_I_SO;
    }

    private void checkInput(int input) {
        if (input < 0 || input >= inputs.size()) {
            throw new IndexOutOfBoundsException(
                    "Input " + input + " is outside layout " + code + ", which has " + inputs.size() + " inputs");
        }
    }

    private void checkOutput(int output) {
        if (output < 0 || output >= outputs.size()) {
            throw new IndexOutOfBoundsException(
                    "Output " + output + " is outside layout " + code + ", which has " + outputs.size() + " outputs");
        }
    }
}
