// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The settings an operator puts in force")
class SettingsTest {

    private static final Key STONE = Blocks.key("stone");
    private static final Key BEDROCK = Blocks.key("bedrock");

    @Nested
    @DisplayName("what may run")
    class WhatMayRun {

        @Test
        void runsEverywhereUntilAWorldIsNamed() {
            assertThat(Settings.DEFAULTS.allowsWorld("world")).isTrue();
        }

        @Test
        void staysOutOfAWorldThatWasNamed() {
            Settings settings = Settings.builder().disabledWorlds(Set.of("nether")).build();

            assertThat(settings.allowsWorld("nether")).isFalse();
            assertThat(settings.allowsWorld("world")).isTrue();
        }

        @Test
        void readsAWorldNameWhicheverWayItWasWritten() {
            Settings settings = Settings.builder().disabledWorlds(Set.of("World_Nether")).build();

            assertThat(settings.allowsWorld("world_nether")).isFalse();
        }

        @Test
        void runsNoMechanicUntilAnOperatorSaysSo() {
            assertThat(Settings.DEFAULTS.runsMechanicIn(Mechanics.GATE, "world")).isFalse();
        }

        @Test
        void keepsAMechanicOutOfAWorldTheWholePluginIsOutOf() {
            Settings settings = everythingOn().disabledWorlds(Set.of("nether")).build();

            assertThat(settings.runsMechanicIn(Mechanics.GATE, "nether")).isFalse();
            assertThat(settings.runsMechanicIn(Mechanics.GATE, "world")).isTrue();
        }

        @Test
        void stopsEveryMechanicWhenTheWholePluginIsSwitchedOff() {
            Settings settings = everythingOn().enabled(false).build();

            assertThat(settings.runsMechanicIn(Mechanics.GATE, "world")).isFalse();
        }

        @Test
        void runsOnlyTheMechanicThatWasNamed() {
            Settings settings = Settings.builder()
                    .mechanics(MechanicSettings.DEFAULTS.withEnabled(Set.of(Mechanics.GATE)))
                    .build();

            assertThat(settings.runsMechanicIn(Mechanics.GATE, "world")).isTrue();
            assertThat(settings.runsMechanicIn(Mechanics.BRIDGE, "world")).isFalse();
        }

        /** Settings an operator has turned every mechanic on in, which is not the default. */
        private Settings.Builder everythingOn() {
            return Settings.builder()
                    .mechanics(MechanicSettings.DEFAULTS.withEverythingEnabled());
        }

        @Test
        void runsEveryChipUntilOneIsNamed() {
            assertThat(Settings.DEFAULTS.allowsChip(Set.of("MCX207"))).isTrue();
        }

        @Test
        void refusesAChipThatWasNamed() {
            Settings settings = Settings.builder().disabledChips(Set.of("mcx207")).build();

            assertThat(settings.allowsChip(Set.of("MCX207"))).isFalse();
            assertThat(settings.allowsChip(Set.of("MCX208"))).isTrue();
        }

        @Test
        void refusesAChipNamedByAnyNumberItAnswersTo() {
            // Naming a retired number switches off the chip that took it over, rather than
            // leaving that chip reachable by its current number.
            Settings settings = Settings.builder().disabledChips(Set.of("MC1200")).build();

            assertThat(settings.allowsChip(Set.of("MCX200", "MC1200"))).isFalse();
        }

        @Test
        void refusesEveryChipWhenSwitchedOffAltogether() {
            Settings settings = Settings.builder().enabled(false).build();

            assertThat(settings.allowsChip(Set.of("MCX207"))).isFalse();
            assertThat(settings.allowsWorld("world")).isFalse();
        }
    }

    @Nested
    @DisplayName("what may be placed")
    class WhatMayBePlaced {

        @Test
        void allowsTheBlocksOnTheList() {
            Settings settings = Settings.builder().placeableBlocks(Set.of(STONE)).build();

            assertThat(settings.mayPlace(STONE)).isTrue();
            assertThat(settings.mayPlace(BEDROCK)).isFalse();
        }

        @Test
        void allowsAnythingWhenNothingWasSingledOut() {
            Settings settings = Settings.builder().placeAnything().build();

            assertThat(settings.mayPlace(BEDROCK)).isTrue();
        }

        @Test
        void startsOutAllowingBuildingMaterialsAndNothingThatActsOnItsOwn() {
            assertThat(Settings.DEFAULTS.mayPlace(STONE)).isTrue();
            assertThat(Settings.DEFAULTS.mayPlace(Blocks.key("oak_planks"))).isTrue();
            assertThat(Settings.DEFAULTS.mayPlace(Blocks.key("tnt"))).isFalse();
            assertThat(Settings.DEFAULTS.mayPlace(Blocks.key("spawner"))).isFalse();
        }
    }

    @Nested
    @DisplayName("how far a chip may reach")
    class HowFarAChipMayReach {

        @Test
        void leavesAnAskingWithinTheLimitAlone() {
            Settings settings = Settings.builder().maxWidth(5).maxLength(16).build();

            assertThat(settings.limitWidth(3)).isEqualTo(3);
            assertThat(settings.limitLength(9)).isEqualTo(9);
        }

        @Test
        void cutsAnAskingBeyondTheLimitDownToIt() {
            Settings settings = Settings.builder().maxWidth(5).maxLength(16).build();

            assertThat(settings.limitWidth(40)).isEqualTo(5);
            assertThat(settings.limitLength(200)).isEqualTo(16);
        }

        @Test
        void neverReportsSomethingSmallerThanOneBlock() {
            assertThat(Settings.DEFAULTS.limitWidth(0)).isEqualTo(1);
            assertThat(Settings.DEFAULTS.limitLength(-4)).isEqualTo(1);
            assertThat(Settings.DEFAULTS.limitPlanterWidth(0)).isEqualTo(1);
        }

        @Test
        void holdsAPlanterToItsOwnLimitRatherThanTheBuildingOne() {
            Settings settings = Settings.builder().maxWidth(16).maxPlanterWidth(4).build();

            assertThat(settings.limitPlanterWidth(9)).isEqualTo(4);
        }

        @Test
        void refusesALimitThatWouldLeaveAChipUnableToActAtAll() {
            Settings settings = Settings.builder().maxWidth(0).maxLength(-1).build();

            assertThat(settings.maxWidth()).isEqualTo(1);
            assertThat(settings.maxLength()).isEqualTo(1);
        }
    }

    @Test
    void cannotBeChangedThroughTheCollectionsItWasGiven() {
        Set<String> worlds = new java.util.HashSet<>(Set.of("nether"));
        Settings settings = Settings.builder().disabledWorlds(worlds).build();

        worlds.add("world");

        assertThat(settings.allowsWorld("world")).isTrue();
    }
}
