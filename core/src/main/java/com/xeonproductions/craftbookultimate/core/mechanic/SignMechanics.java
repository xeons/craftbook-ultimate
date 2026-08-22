// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The sign mechanics there are.
 *
 * <p>Stateless, so one of each is enough for a whole server. Whatever resolves a sign in the
 * world asks here which mechanic claims it, and gets the mechanic together with the proper
 * spelling of the name so the sign can be tidied up as it is written.
 */
@NullMarked
public final class SignMechanics {

    private static final Elevator ELEVATOR = new Elevator();
    private static final Bridge BRIDGE = new Bridge();
    private static final Door DOOR = new Door();
    private static final Gate GATE = new Gate();
    private static final ToggleArea AREA = new ToggleArea();
    private static final HiddenSwitch HIDDEN = HiddenSwitch.instance();
    private static final Marquee MARQUEE = Marquee.instance();

    private static final List<SignMechanic> ALL =
            List.of(ELEVATOR, BRIDGE, DOOR, GATE, AREA, HIDDEN, MARQUEE);

    private SignMechanics() {}

    /** Every mechanic. */
    public static List<SignMechanic> all() {
        return ALL;
    }

    /** The lift. */
    public static Elevator elevator() {
        return ELEVATOR;
    }

    /** The gate, which is the one that answers to a hand on its own material. */
    public static Gate gate() {
        return GATE;
    }

    /** The marquee, which is the one that only ever reads. */
    public static Marquee marquee() {
        return MARQUEE;
    }

    /** The hidden switch, which is the one whose own sign is never what gets clicked. */
    public static HiddenSwitch hiddenSwitch() {
        return HIDDEN;
    }

    /** The toggled area, which is the one that keeps its blocks somewhere other than the world. */
    public static ToggleArea area() {
        return AREA;
    }

    /**
     * The mechanic a sign names, and the proper spelling of that name.
     *
     * <p>Answers whatever the settings say, because a sign is still what it is when the mechanic
     * it names has been switched off; whoever acts on it decides what to do about that.
     */
    public static Optional<Claim> claiming(SignLines lines) {
        for (SignMechanic mechanic : ALL) {
            Optional<String> name = mechanic.nameOn(lines);
            if (name.isPresent()) {
                return Optional.of(new Claim(mechanic, name.get()));
            }
        }
        return Optional.empty();
    }

    /** Whether a mechanic runs at all here. */
    public static boolean isRunning(SignMechanic mechanic, Settings settings, String world) {
        return settings.runsMechanicIn(mechanic.name(), world);
    }

    /** Every sign name any mechanic answers to, for anything that needs the whole list. */
    public static List<String> everySignName() {
        List<String> names = new ArrayList<>();
        for (SignMechanic mechanic : ALL) {
            names.addAll(mechanic.signNames());
        }
        return List.copyOf(names);
    }

    /**
     * A mechanic and the name the sign carries it under.
     *
     * @param mechanic what the sign builds
     * @param signName the name in its proper spelling, however the builder typed it
     */
    public record Claim(SignMechanic mechanic, String signName) {}
}
