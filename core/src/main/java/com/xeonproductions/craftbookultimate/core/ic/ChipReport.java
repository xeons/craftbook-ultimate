// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Everything worth knowing about one chip in the world, read at a moment in time.
 *
 * <p>A chip that does nothing gives a builder almost nothing to go on: the sign looks right, the
 * levers look wired, and which of those two impressions is wrong is exactly the question. This
 * answers it by saying what the plugin thinks — which pins it believes are wired, what it reads on
 * each, what mode it resolved, and whether it is ticking.
 *
 * <p>A value rather than a printout, so that the stick, the commands and any test all say the same
 * things about the same chip, and so that what is said can be asserted on without a server.
 *
 * @param model the catalogue number the sign resolved to
 * @param shorthand the chip's readable name
 * @param name the chip's display name
 * @param world what the world is called
 * @param position where the sign is
 * @param layout how the chip is wired
 * @param mode the flags read off the end of the identifier line
 * @param selfTriggering whether this chip is actually ticking
 * @param canSelfTrigger whether this kind of chip is able to tick at all
 * @param pins every pin, in order, inputs first
 * @param poweredBehind whether the block behind the sign is a permanent power source
 * @param lines what the sign leaves blank of what the chip reads
 * @param sign the sign as it currently reads
 * @param area the stretch of world the chip works on, where it can say
 */
@NullMarked
public record ChipReport(
        String model,
        String shorthand,
        String name,
        String world,
        Vec3i position,
        PinLayout layout,
        ICMode mode,
        boolean selfTriggering,
        boolean canSelfTrigger,
        List<Pin> pins,
        boolean poweredBehind,
        LineReview lines,
        SignLines sign,
        Optional<Bounds> area) {

    public ChipReport {
        pins = List.copyOf(pins);
    }

    /**
     * One of a chip's pins as the plugin currently reads it.
     *
     * @param index the pin number within its own kind, counting from zero
     * @param input whether this is an input rather than an output
     * @param position where the pin block is
     * @param wired whether the block there is the sort of thing that wires a pin
     * @param powered whether it is carrying a signal
     * @param power how strong that signal is, from 0 to 15
     */
    public record Pin(
            int index, boolean input, Vec3i position, boolean wired, boolean powered, int power) {

        /** How the pin reads to somebody trying to work out why a chip is dead. */
        public Component describe() {
            NamedTextColor colour = !wired ? NamedTextColor.DARK_GRAY
                    : powered ? NamedTextColor.GREEN : NamedTextColor.GRAY;

            String state = !wired ? "nothing wired here"
                    : powered ? "on, power " + power
                    : "off";

            return Component.text("  " + (input ? "in " : "out") + " " + index + "  ", colour)
                    .append(Component.text(
                            position.x() + "," + position.y() + "," + position.z(),
                            NamedTextColor.DARK_GRAY))
                    .append(Component.text("  " + state, colour));
        }
    }

    /** The inputs, in order. */
    public List<Pin> inputs() {
        return pins.stream().filter(Pin::input).toList();
    }

    /** The outputs, in order. */
    public List<Pin> outputs() {
        return pins.stream().filter(pin -> !pin.input()).toList();
    }

    /** Whether anything at all is wired to an input. */
    public boolean hasAnyInputWired() {
        return inputs().stream().anyMatch(Pin::wired);
    }

    /** Whether the chip cannot work because its sign says too little. */
    public boolean broken() {
        return lines.broken();
    }

    /**
     * The whole report, as lines to send to whoever asked.
     *
     * <p>Ordered by what answers the question soonest. What the chip is comes first because a sign
     * can resolve to a chip its builder did not mean; the pins come next because a chip wired to
     * nothing is the commonest fault by a distance; and the rest follows.
     */
    public List<Component> describe() {
        List<Component> said = new ArrayList<>();

        said.add(Component.text(model + "  ", NamedTextColor.GOLD)
                .append(Component.text(name, NamedTextColor.YELLOW))
                .append(Component.text("  =" + shorthand, NamedTextColor.DARK_GRAY)));

        said.add(Component.text("  at " + position.x() + "," + position.y() + "," + position.z()
                + " in " + world, NamedTextColor.DARK_GRAY));

        describeTrouble(said);

        said.add(Component.text("Pins  ", NamedTextColor.AQUA)
                .append(Component.text(layout.code() + "  "
                        + layout.inputCount() + " in, " + layout.outputCount() + " out",
                        NamedTextColor.GRAY)));
        pins.forEach(pin -> said.add(pin.describe()));

        describeRunning(said);

        area.ifPresent(bounds -> said.add(Component.text("Area  ", NamedTextColor.AQUA)
                .append(Component.text(bounds.describe(), NamedTextColor.GRAY))));

        return said;
    }

    /** How the mode flags read, rather than the characters that selected them. */
    private String describeMode() {
        StringBuilder said = new StringBuilder();
        if (mode.behaviour() != ICMode.Behaviour.NONE) {
            said.append(mode.behaviour().symbol()).append("  ")
                    .append(mode.behaviour().name().toLowerCase(java.util.Locale.ROOT)
                            .replace('_', ' '));
        }
        mode.permutation().ifPresent(permutation -> {
            if (!said.isEmpty()) {
                said.append("; ");
            }
            said.append("pins moved about");
        });
        return said.toString();
    }

    /** Whatever is wrong, said before anything else so it is not scrolled past. */
    private void describeTrouble(List<Component> said) {
        for (LineReview.Blank blank : lines.missing()) {
            said.add(Component.text("  " + blank.said() + " It is blank, so this chip does nothing.",
                    NamedTextColor.RED));
        }
        for (LineReview.Blank blank : lines.defaulted()) {
            said.add(Component.text("  " + blank.said() + " It is blank, so the default is used.",
                    NamedTextColor.YELLOW));
        }
        if (!hasAnyInputWired() && !selfTriggering) {
            said.add(Component.text(
                    "  Nothing is wired to any input, and this chip does not tick on its own, "
                            + "so nothing will ever set it off.",
                    NamedTextColor.RED));
        }
    }

    /** How the chip is being run, and how it could be. */
    private void describeRunning(List<Component> said) {
        String ticking = selfTriggering ? "ticking every tick"
                : canSelfTrigger ? "waiting on redstone; add S to the model to make it tick"
                : "waiting on redstone; this chip cannot tick on its own";
        said.add(Component.text("Runs  ", NamedTextColor.AQUA)
                .append(Component.text(ticking, NamedTextColor.GRAY)));

        if (!mode.isNone()) {
            said.add(Component.text("Mode  ", NamedTextColor.AQUA)
                    .append(Component.text(describeMode(), NamedTextColor.GRAY)));
        }
        if (poweredBehind) {
            said.add(Component.text("      the block behind the sign is a permanent power source",
                    NamedTextColor.GRAY));
        }
    }
}
