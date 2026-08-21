// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("The sign mechanics there are")
class SignMechanicsTest {

    private static SignLines named(String name) {
        return SignLines.of("", name, "", "");
    }

    @Nested
    @DisplayName("which mechanic a sign names")
    class WhichMechanicASignNames {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"[Bridge]", "[Bridge End]"})
        void theBridge(String written) {
            assertThat(SignMechanics.claiming(named(written)))
                    .map(SignMechanics.Claim::mechanic)
                    .containsInstanceOf(Bridge.class);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"[Door Up]", "[Door Down]", "[Door]"})
        void theDoor(String written) {
            assertThat(SignMechanics.claiming(named(written)))
                    .map(SignMechanics.Claim::mechanic)
                    .containsInstanceOf(Door.class);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"[Gate]", "[DGate]C", "[GlassGate]", "[NetherDGate]C"})
        void theGate(String written) {
            assertThat(SignMechanics.claiming(named(written)))
                    .map(SignMechanics.Claim::mechanic)
                    .containsInstanceOf(Gate.class);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"[Lift Up]", "[Lift Down]", "[Lift]", "[Lift UpDown]"})
        void theLift(String written) {
            assertThat(SignMechanics.claiming(named(written)))
                    .map(SignMechanics.Claim::mechanic)
                    .containsInstanceOf(Elevator.class);
        }

        @Test
        void nothingAtAllForAnOrdinarySign() {
            assertThat(SignMechanics.claiming(SignLines.of("Welcome", "to", "the", "shop")))
                    .isEmpty();
        }

        @Test
        void nothingForAChipsSign() {
            assertThat(SignMechanics.claiming(named("[MC1001]"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("tidying up a name as it is written")
    class TidyingUpANameAsItIsWritten {

        @Test
        void answersInTheProperSpellingHoweverItWasTyped() {
            assertThat(SignMechanics.claiming(named("[bridge end]")))
                    .map(SignMechanics.Claim::signName)
                    .contains("[Bridge End]");
        }

        @Test
        void keepsTheClickableLetterOnAGate() {
            assertThat(SignMechanics.claiming(named("[glassgate]c")))
                    .map(SignMechanics.Claim::signName)
                    .contains("[GlassGate]C");
        }

        @Test
        void ignoresSpaceAroundTheName() {
            assertThat(SignMechanics.claiming(SignLines.of("", "  [Gate]  ", "", "")))
                    .map(SignMechanics.Claim::signName)
                    .contains("[Gate]");
        }
    }

    @Nested
    @DisplayName("what an operator controls")
    class WhatAnOperatorControls {

        @Test
        void everyMechanicIsOnWhenNobodyHasSaidOtherwise() {
            for (SignMechanic mechanic : SignMechanics.all()) {
                assertThat(SignMechanics.isRunning(mechanic, Settings.DEFAULTS, "world")).isTrue();
            }
        }

        @Test
        void switchingOneOffLeavesTheRestRunning() {
            Settings settings = Settings.builder()
                    .mechanics(Settings.DEFAULTS.mechanics().withDisabled(Set.of("gate")))
                    .build();

            assertThat(SignMechanics.isRunning(SignMechanics.gate(), settings, "world")).isFalse();
            assertThat(SignMechanics.isRunning(SignMechanics.elevator(), settings, "world"))
                    .isTrue();
        }

        @Test
        void aWorldWithNothingRunningInItStopsThemAll() {
            Settings settings = Settings.builder().disabledWorlds(Set.of("nether")).build();

            for (SignMechanic mechanic : SignMechanics.all()) {
                assertThat(SignMechanics.isRunning(mechanic, settings, "nether")).isFalse();
            }
        }
    }

    @Test
    void noTwoMechanicsAnswerToTheSameSign() {
        Set<String> seen = new HashSet<>();
        for (String name : SignMechanics.everySignName()) {
            assertThat(seen.add(name.toLowerCase(Locale.ROOT)))
                    .withFailMessage("More than one mechanic answers to " + name)
                    .isTrue();
        }
    }

    @Test
    void eachMechanicHasItsOwnPairOfPermissions() {
        Set<String> seen = new HashSet<>();
        for (SignMechanic mechanic : SignMechanics.all()) {
            assertThat(seen.add(mechanic.buildPermission())).isTrue();
            assertThat(seen.add(mechanic.usePermission())).isTrue();
            assertThat(mechanic.usePermission()).startsWith(mechanic.buildPermission());
        }
    }
}
