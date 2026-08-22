// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/**
 * What an operator has said about the mechanics.
 *
 * <p>One record per mechanic, rather than every setting laid out flat. What holds them together is
 * only which mechanics run and the two rules belonging to no single mechanic; a question about
 * gates is answered by {@link GateSettings} and nothing else has to be read to answer it.
 *
 * <p>Nothing runs until an operator says it does. A mechanic is not something a builder opts into
 * the way a chip is — the blocks it answers to are ordinary blocks, so switching one on changes
 * what a stair or a piece of glowstone already in the world does. Which of them a server wants is
 * a decision only its operator can make, and the file makes them make it.
 *
 * <p>The bridges and doors take their building limits from {@link Settings} rather than from here,
 * because they are the same limits the building chips use: how wide a structure may be, how far it
 * may run, and what it may be made of. Nothing peculiar to those two exists, which is why neither
 * has a record of its own.
 *
 * @param enabled the mechanics that run, by name, compared without regard to case. Nothing is in
 *     it out of the box: a mechanic changes how a world behaves and an operator says which ones
 *     they want rather than which of twenty-one they do not
 * @param redstone whether redstone reaching a mechanic's sign works it
 * @param depowerOnSourceRemoval whether a powered block goes out when the redstone feeding it is
 *     mined away, rather than staying as it was
 * @param chair what may be sat on, and what a healing chair does
 * @param gate what a gate is made of and how far it looks for it
 * @param elevator how the lifts are worked
 * @param area how large and how many the saved areas may be
 * @param powerables what the blocks answering redstone turn into
 * @param heads what a death leaves behind, and how likely it is to
 * @param lightSwitch how far a light switch reaches and how much it turns
 * @param meters what is held up to a block to read it
 * @param bounce what throws somebody who jumps on it
 * @param teleporter how far somebody may be sent, and from where
 * @param xp what bottles experience, and what a bottle costs
 * @param snow how snow piles, slumps and melts
 */
@NullMarked
public record MechanicSettings(
        Set<String> enabled,
        boolean redstone,
        boolean depowerOnSourceRemoval,
        ChairSettings chair,
        GateSettings gate,
        ElevatorSettings elevator,
        AreaSettings area,
        PowerableSettings powerables,
        HeadSettings heads,
        LightSwitchSettings lightSwitch,
        MeterSettings meters,
        BounceSettings bounce,
        TeleporterSettings teleporter,
        XpSettings xp,
        SnowSettings snow) {

    /** Every mechanic switched off, which is how a server nobody has configured runs. */
    public static final MechanicSettings DEFAULTS = builder().build();

    /** Copies the list of what runs. */
    public MechanicSettings {
        enabled = compared(enabled);
    }

    /** A set of settings to alter from the defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /** These settings with everything the same but for what the builder is given. */
    public Builder toBuilder() {
        return new Builder()
                .enabled(enabled)
                .redstone(redstone)
                .depowerOnSourceRemoval(depowerOnSourceRemoval)
                .chair(chair)
                .gate(gate)
                .elevator(elevator)
                .area(area)
                .powerables(powerables)
                .heads(heads)
                .lightSwitch(lightSwitch)
                .meters(meters)
                .bounce(bounce)
                .teleporter(teleporter)
                .xp(xp)
                .snow(snow);
    }

    /**
     * The mechanics that run, spelt and ordered the way the file lists them.
     *
     * <p>{@link #enabled} is held compared rather than as written, so this is what to say to
     * somebody rather than that.
     */
    public List<String> running() {
        List<String> on = new ArrayList<>();
        for (String mechanic : Mechanics.ALL) {
            if (allows(mechanic)) {
                on.add(mechanic);
            }
        }
        return List.copyOf(on);
    }

    /**
     * Whether a mechanic runs.
     *
     * @param mechanic the mechanic's name, such as {@code Bridge}
     */
    public boolean allows(String mechanic) {
        return enabled.contains(Mechanics.compared(mechanic));
    }

    /** These settings with a different set of mechanics running. */
    public MechanicSettings withEnabled(Set<String> mechanics) {
        return toBuilder().enabled(mechanics).build();
    }

    /**
     * These settings with every mechanic running.
     *
     * <p>What an operator gets by saying so in twenty-one places, and what a test exercising a
     * mechanic wants without having to name the one it is about.
     */
    public MechanicSettings withEverythingEnabled() {
        return withEnabled(Set.copyOf(Mechanics.ALL));
    }

    /** These settings with redstone allowed or refused. */
    public MechanicSettings withRedstone(boolean allowed) {
        return toBuilder().redstone(allowed).build();
    }

    /** These settings with powered blocks going out, or not, when their source is mined away. */
    public MechanicSettings withDepowerOnSourceRemoval(boolean depowers) {
        return toBuilder().depowerOnSourceRemoval(depowers).build();
    }

    /** These settings with different chairs. */
    public MechanicSettings withChair(ChairSettings chairs) {
        return toBuilder().chair(chairs).build();
    }

    /** These settings with different heads dropping. */
    public MechanicSettings withHeads(HeadSettings dropping) {
        return toBuilder().heads(dropping).build();
    }

    /** These settings with different gates. */
    public MechanicSettings withGate(GateSettings gates) {
        return toBuilder().gate(gates).build();
    }

    /** These settings with different lifts. */
    public MechanicSettings withElevator(ElevatorSettings lifts) {
        return toBuilder().elevator(lifts).build();
    }

    /** These settings with different limits on the saved areas. */
    public MechanicSettings withArea(AreaSettings areas) {
        return toBuilder().area(areas).build();
    }

    /** These settings with different blocks answering redstone. */
    public MechanicSettings withPowerables(PowerableSettings blocks) {
        return toBuilder().powerables(blocks).build();
    }

    /** These settings with light switches reaching differently. */
    public MechanicSettings withLightSwitch(LightSwitchSettings switches) {
        return toBuilder().lightSwitch(switches).build();
    }

    /** These settings with the meters read off different items. */
    public MechanicSettings withMeters(MeterSettings dials) {
        return toBuilder().meters(dials).build();
    }

    /** These settings with different blocks throwing people. */
    public MechanicSettings withBounce(BounceSettings bounces) {
        return toBuilder().bounce(bounces).build();
    }

    /** These settings with teleporters working differently. */
    public MechanicSettings withTeleporter(TeleporterSettings teleporters) {
        return toBuilder().teleporter(teleporters).build();
    }

    /** These settings with experience bottled differently. */
    public MechanicSettings withXp(XpSettings experience) {
        return toBuilder().xp(experience).build();
    }

    /** These settings with snow behaving differently. */
    public MechanicSettings withSnow(SnowSettings snowfall) {
        return toBuilder().snow(snowfall).build();
    }

    private static Set<String> compared(Set<String> names) {
        Set<String> copy = new LinkedHashSet<>();
        for (String name : names) {
            copy.add(Mechanics.compared(name));
        }
        return Collections.unmodifiableSet(copy);
    }

    /** Assembles a set of settings, filling in the defaults for anything not given. */
    public static final class Builder {

        private Set<String> enabled = Set.of();
        private boolean redstone = true;
        private boolean depowerOnSourceRemoval = false;
        private ChairSettings chair = ChairSettings.DEFAULTS;
        private GateSettings gate = GateSettings.DEFAULTS;
        private ElevatorSettings elevator = ElevatorSettings.DEFAULTS;
        private AreaSettings area = AreaSettings.DEFAULTS;
        private PowerableSettings powerables = PowerableSettings.DEFAULTS;
        private HeadSettings heads = HeadSettings.DEFAULTS;
        private LightSwitchSettings lightSwitch = LightSwitchSettings.DEFAULTS;
        private MeterSettings meters = MeterSettings.DEFAULTS;
        private BounceSettings bounce = BounceSettings.DEFAULTS;
        private TeleporterSettings teleporter = TeleporterSettings.DEFAULTS;
        private XpSettings xp = XpSettings.DEFAULTS;
        private SnowSettings snow = SnowSettings.DEFAULTS;

        private Builder() {}

        /** The mechanics that run. Anything not named here stays switched off. */
        public Builder enabled(Set<String> mechanics) {
            this.enabled = mechanics;
            return this;
        }

        /** Whether redstone reaching a mechanic's sign works it. */
        public Builder redstone(boolean allowed) {
            this.redstone = allowed;
            return this;
        }

        /** Whether a powered block goes out when the redstone feeding it is mined away. */
        public Builder depowerOnSourceRemoval(boolean depowers) {
            this.depowerOnSourceRemoval = depowers;
            return this;
        }

        /** What may be sat on, and what a healing chair does. */
        public Builder chair(ChairSettings chairs) {
            this.chair = chairs;
            return this;
        }

        /** What a death leaves behind, and how likely it is to. */
        public Builder heads(HeadSettings dropping) {
            this.heads = dropping;
            return this;
        }

        /** What a gate is made of and how far it looks for it. */
        public Builder gate(GateSettings gates) {
            this.gate = gates;
            return this;
        }

        /** How the lifts are worked. */
        public Builder elevator(ElevatorSettings lifts) {
            this.elevator = lifts;
            return this;
        }

        /** How large and how many the saved areas may be. */
        public Builder area(AreaSettings areas) {
            this.area = areas;
            return this;
        }

        /** What the blocks answering redstone turn into. */
        public Builder powerables(PowerableSettings blocks) {
            this.powerables = blocks;
            return this;
        }

        /** How far a light switch reaches and how much it turns. */
        public Builder lightSwitch(LightSwitchSettings switches) {
            this.lightSwitch = switches;
            return this;
        }

        /** What is held up to a block to read it. */
        public Builder meters(MeterSettings dials) {
            this.meters = dials;
            return this;
        }

        /** What throws somebody who jumps on it. */
        public Builder bounce(BounceSettings bounces) {
            this.bounce = bounces;
            return this;
        }

        /** How far somebody may be sent, and from where. */
        public Builder teleporter(TeleporterSettings teleporters) {
            this.teleporter = teleporters;
            return this;
        }

        /** What bottles experience, and what a bottle costs. */
        public Builder xp(XpSettings experience) {
            this.xp = experience;
            return this;
        }

        /** How snow piles, slumps and melts. */
        public Builder snow(SnowSettings snowfall) {
            this.snow = snowfall;
            return this;
        }

        public MechanicSettings build() {
            return new MechanicSettings(
                    enabled,
                    redstone,
                    depowerOnSourceRemoval,
                    chair,
                    gate,
                    elevator,
                    area,
                    powerables,
                    heads,
                    lightSwitch,
                    meters,
                    bounce,
                    teleporter,
                    xp,
                    snow);
        }
    }
}
