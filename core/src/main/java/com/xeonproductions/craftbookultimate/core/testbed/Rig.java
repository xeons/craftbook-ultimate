package com.xeonproductions.craftbookultimate.core.testbed;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLine;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignSupport;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * One chip, wired up so somebody can work it by hand.
 *
 * <p>A rig is everything a single chip needs to be tried: the sign, the block it hangs on, a lever
 * on each input for a person to flip, a lever on each output for the chip to flip, a lamp under
 * each of those so the output can be seen from across the room, and a label saying which chip this
 * is.
 *
 * <p>Levers on both sides is not a stylistic choice. A chip reads an input by asking what kind of
 * block sits on the pin — only a power source counts as wired at all — and drives an output by
 * toggling a lever that is already there, leaving anything else alone. A rig built from redstone
 * blocks would read as permanently on and one built from lamps would never be driven.
 *
 * <p>Positions are worked out in the sign's own frame through {@link PinLayout}, so a rig is
 * correct for whichever way it is built and for whatever layout its chip uses, rather than being a
 * shape drawn once and hoped over.
 *
 * @param chip the chip this rig is for
 * @param signPosition where the chip's sign goes
 * @param facing the direction the sign's text faces
 * @param placements everything to put in the world, in the order it should be placed
 */
@NullMarked
public record Rig(
        ICDefinition chip, Vec3i signPosition, BlockFace facing, List<Placement> placements) {

    /** How far in front of the sign the label stands. */
    private static final int LABEL_FORWARD = 3;

    /** How far the sign sits above the floor the rig stands on. */
    public static final int SIGN_HEIGHT = 2;

    public Rig {
        placements = List.copyOf(placements);
    }

    /** One thing to put at one place. */
    public record Placement(Vec3i position, Fixture fixture) {}

    /**
     * Something a rig puts in the world.
     *
     * <p>Deliberately a small closed set of roles rather than block names. What a lever or a lamp
     * actually is belongs to the platform; what a rig knows is that this position needs the thing
     * a person flips and that one needs the thing that lights up.
     */
    public sealed interface Fixture {

        /** The block a lever stands on. */
        record Mount() implements Fixture {}

        /** The block the sign hangs on, which is what the chip acts from. */
        record Backing() implements Fixture {}

        /**
         * A lever on an input, for a person to flip.
         *
         * @param input which input this is
         * @param mountedOn the direction from the lever to the block it clings to
         * @param facing which way the lever points, always a cardinal direction
         */
        record InputLever(int input, BlockFace mountedOn, BlockFace facing) implements Fixture {}

        /**
         * A lever on an output, for the chip to flip.
         *
         * @param output which output this is
         * @param mountedOn the direction from the lever to the block it clings to
         * @param facing which way the lever points, always a cardinal direction
         */
        record OutputLever(int output, BlockFace mountedOn, BlockFace facing) implements Fixture {}

        /** A lamp under an output lever, so the output can be seen. */
        record Indicator() implements Fixture {}

        /** The chip's own sign, hung on the backing block. */
        record ChipSign(SignLines lines) implements Fixture {}

        /** A sign standing in front of the rig saying what this chip is. */
        record LabelSign(SignLines lines) implements Fixture {}

        /** A container holding something, for the chips that read one. */
        record Chest(Map<Key, Integer> contents) implements Fixture {}

        /** A plain block, for the chips that need something to work on. */
        record Prop(Key block) implements Fixture {}
    }

    /**
     * Lays out a rig for one chip.
     *
     * @param chip the chip to wire up
     * @param setup what to write on its sign and what to put around it
     * @param signPosition where the sign goes
     * @param facing the direction the sign's text faces
     */
    public static Rig forChip(
            ICDefinition chip, ChipSetup setup, Vec3i signPosition, BlockFace facing) {

        PinLayout layout = chip.defaultLayout();
        List<Placement> placements = new ArrayList<>();

        Vec3i backing = SignSupport.of(signPosition, facing);
        placements.add(new Placement(backing, new Fixture.Backing()));

        // Every pin, and the sign and its backing, are places a lever may not cling to: a mount
        // put on one would bury a pin the chip reads, or the sign itself.
        Set<Vec3i> taken = new HashSet<>();
        taken.add(signPosition);
        taken.add(backing);
        for (int pin = 0; pin < layout.pinCount(); pin++) {
            taken.add(layout.pinPosition(pin, signPosition, facing));
        }

        // Only the inputs the chip actually reads get a lever. Most read one and ignore the
        // rest — an AISO chip is set off by any of its four — so wiring all of them would leave
        // three levers that do nothing but make the rig harder to read.
        int wired = ChipSetup.usesEveryInput(chip.model()) ? layout.inputCount()
                : Math.min(1, layout.inputCount());

        for (int input = 0; input < wired; input++) {
            Vec3i pin = layout.inputPosition(input, signPosition, facing);
            BlockFace mounted = mountFor(pin, taken);
            taken.add(pin.offset(mounted));
            placements.add(new Placement(pin.offset(mounted), new Fixture.Mount()));
            placements.add(new Placement(
                    pin, new Fixture.InputLever(input, mounted, leverFacing(mounted, facing))));
        }

        for (int output = 0; output < layout.outputCount(); output++) {
            Vec3i pin = layout.outputPosition(output, signPosition, facing);
            BlockFace mounted = mountFor(pin, taken);
            taken.add(pin.offset(mounted));
            placements.add(new Placement(pin.offset(mounted), new Fixture.Indicator()));
            placements.add(new Placement(
                    pin, new Fixture.OutputLever(output, mounted, leverFacing(mounted, facing))));
        }

        setup.props().forEach((place, fixture) ->
                placements.add(new Placement(place.resolve(signPosition, facing), fixture)));

        // The sign last, so the block it hangs on is already there when it is placed.
        placements.add(new Placement(signPosition, new Fixture.ChipSign(signLinesFor(chip, setup))));

        Vec3i label = signPosition
                .offset(facing, LABEL_FORWARD)
                .add(0, -1, 0);
        placements.add(new Placement(label.add(0, -1, 0), new Fixture.Mount()));
        placements.add(new Placement(label, new Fixture.LabelSign(labelFor(chip, setup))));

        return new Rig(chip, signPosition, facing, placements);
    }

    /**
     * What goes on the chip's own sign.
     *
     * <p>The identifier comes from {@link ICDefinition#canonicalLine}, so it carries whatever
     * markers that chip needs rather than being spelled out here — which is the same reason the
     * sign listener uses it, and the only way to be sure a rig's sign is one the plugin will read.
     *
     * <p>The ticking form is asked for only where {@link ChipSetup#ticks} allows it, which is the
     * chips whose tick reads something and drives an output and does nothing else. Ticking the
     * rest is dangerous rather than merely wrong: a holy smite built ticking strikes everything
     * within range every tick for as long as it is loaded, which is a lightning bolt per entity
     * per tick and a server out of memory in about a minute.
     *
     * <p>Everything else is built as the plain model number, which is what a builder writing that
     * number gets, and can be given an {@code S} by hand.
     */
    private static SignLines signLinesFor(ICDefinition chip, ChipSetup setup) {
        ICLine written = ICLine.parse(chip.modelReference())
                .orElseThrow(() -> new IllegalStateException(
                        "IC " + chip.model() + " has a model reference that does not parse"));

        // Ticking only where the chip's own tick merely reads something. See ChipSetup#ticks:
        // a bed built ticking wholesale takes the server down, and one built with none of it
        // leaves every sensor and receiver looking broken.
        boolean ticking = chip.supportsSelfTriggering() && ChipSetup.ticks(chip.model());
        String identifier = ticking
                ? tickingReference(chip, written)
                : chip.canonicalLine(written, false).render();

        return SignLines.of(chip.shorthand(), identifier, setup.thirdLine(), setup.fourthLine());
    }

    /**
     * How to write a chip that is meant to tick.
     *
     * <p>Most of the catalogue's ticking chips have a model number of their own for it —
     * {@code MC0111} is the receiver that follows its band, where {@code MC1111} waits to be
     * clocked. Where one exists it is written, because that is the number a builder writes and
     * because otherwise those numbers appear nowhere on the bed at all. The {@code S} flag is the
     * fallback for the few chips that can tick without having been catalogued twice.
     */
    private static String tickingReference(ICDefinition chip, ICLine written) {
        return chip.selfTriggeringModel()
                .map(model -> new ICLine(
                        ICLine.Kind.MODEL, model, false, chip.requiresAuthorisation(), "").render())
                .orElseGet(() -> chip.canonicalLine(written, true).render());
    }

    /**
     * What goes on the label.
     *
     * <p>Never brackets and never a leading equals sign. Both are how a chip is named, and a label
     * carrying one on its second line would quietly become a second chip of whatever it named.
     */
    private static SignLines labelFor(ICDefinition chip, ChipSetup setup) {
        return SignLines.of(
                chip.model(),
                chip.name(),
                chip.defaultLayout().code() + (chip.restricted() ? " restricted" : ""),
                setup.note());
    }

    /**
     * Which way a lever on a pin should cling.
     *
     * <p>Underneath by preference, which is the tidiest and reads as a lever standing on the
     * floor. Some layouts stack their pins — the four inputs of a {@code UISO} chip include one
     * directly below another — so a lever there has to take a wall or a ceiling instead. Falls
     * back to underneath when a pin is hemmed in on every side, which no layout in the catalogue
     * manages, so that a rig is always built rather than half-built.
     */
    private static BlockFace mountFor(Vec3i pin, Set<Vec3i> taken) {
        for (BlockFace candidate : MOUNT_CANDIDATES) {
            if (!taken.contains(pin.offset(candidate))) {
                return candidate;
            }
        }
        return BlockFace.DOWN;
    }

    /**
     * Which way a lever points.
     *
     * <p>Always a cardinal direction, whatever it clings to. A lever's facing is its rotation
     * about the vertical axis and nothing else, so a lever on a floor or a ceiling still has to be
     * given one of the four horizontal directions — the server refuses {@code UP} and {@code DOWN}
     * outright rather than ignoring them.
     *
     * <p>On a wall it points away from what it clings to, which is how a lever is built by hand.
     * On a floor or a ceiling there is no such constraint, so it faces the same way as the sign
     * and the whole rig reads consistently from the front.
     */
    private static BlockFace leverFacing(BlockFace mountedOn, BlockFace signFacing) {
        return mountedOn.isCardinal() ? mountedOn.opposite() : signFacing;
    }

    /** Where a lever will look for something to cling to, in the order it prefers. */
    private static final List<BlockFace> MOUNT_CANDIDATES = List.of(
            BlockFace.DOWN,
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP);

    /** Where this rig reaches, as offsets from its sign, for working out how far apart to space them. */
    public Bounds bounds() {
        int minX = 0;
        int maxX = 0;
        int minZ = 0;
        int maxZ = 0;
        for (Placement placement : placements) {
            Vec3i offset = placement.position().subtract(signPosition);
            minX = Math.min(minX, offset.x());
            maxX = Math.max(maxX, offset.x());
            minZ = Math.min(minZ, offset.z());
            maxZ = Math.max(maxZ, offset.z());
        }
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    /** How far a rig reaches around its sign, on the ground plane. */
    public record Bounds(int minX, int maxX, int minZ, int maxZ) {

        /** How wide this is, east to west. */
        public int width() {
            return maxX - minX + 1;
        }

        /** How deep this is, north to south. */
        public int depth() {
            return maxZ - minZ + 1;
        }
    }
}
