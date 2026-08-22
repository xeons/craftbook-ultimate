// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.powerable;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The blocks that answer redstone")
class PowerablesTest {

    private static final MechanicSettings SETTINGS = MechanicSettings.DEFAULTS;

    private static Key block(String name) {
        return Key.key("minecraft:" + name);
    }

    @Nested
    @DisplayName("as a catalogue")
    class Catalogue {

        @Test
        @DisplayName("holds three, since the fork's two netherrack mechanics were one")
        void holdsThree() {
            assertThat(Powerables.all(SETTINGS)).hasSize(3);
        }

        @Test
        @DisplayName("leaves out one an operator has switched off")
        void leavesOutADisabledOne() {
            MechanicSettings without =
                    SETTINGS.withDisabled(Set.of(Powerables.JACK_O_LANTERN));

            assertThat(Powerables.all(without))
                    .noneMatch(powerable -> powerable.name().equals(Powerables.JACK_O_LANTERN));
        }

        @Test
        @DisplayName("finds the one that works on a block")
        void findsTheOneForABlock() {
            List<Powerable> all = Powerables.all(SETTINGS);

            assertThat(Powerables.workingOn(all, block("glowstone")))
                    .get().extracting(Powerable::name).isEqualTo(Powerables.GLOWSTONE);
            assertThat(Powerables.workingOn(all, block("soul_sand")))
                    .get().extracting(Powerable::name).isEqualTo(Powerables.GLOWSTONE);
            assertThat(Powerables.workingOn(all, block("netherrack")))
                    .get().extracting(Powerable::name).isEqualTo(Powerables.NETHERRACK);
            assertThat(Powerables.workingOn(all, block("stone"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("a block that becomes another")
    class Swapping {

        private final Powerable.Swap glowstone = new Powerable.Swap(
                Powerables.GLOWSTONE, block("soul_sand"), Powerables.GLOWSTONE_ON);

        @Test
        @DisplayName("is the lit one when power reaches it")
        void isLitWhenPowered() {
            assertThat(glowstone.wanted(true)).isEqualTo(Powerables.GLOWSTONE_ON);
        }

        @Test
        @DisplayName("is the dark one when it does not")
        void isDarkWhenUnpowered() {
            assertThat(glowstone.wanted(false)).isEqualTo(block("soul_sand"));
        }

        @Test
        @DisplayName("claims both of its own blocks and nothing else")
        void claimsBothOfItsOwn() {
            assertThat(glowstone.worksOn(block("soul_sand"))).isTrue();
            assertThat(glowstone.worksOn(Powerables.GLOWSTONE_ON)).isTrue();
            assertThat(glowstone.worksOn(block("stone"))).isFalse();
        }
    }

    @Nested
    @DisplayName("a light switch")
    class Switching {

        @Test
        @DisplayName("answers to both of the names that look like a switch")
        void answersToBothNames() {
            assertThat(LightSwitches.claims("[I]")).isTrue();
            assertThat(LightSwitches.claims("[|]")).isTrue();
            assertThat(LightSwitches.claims(" [i] ")).isTrue();
            assertThat(LightSwitches.claims("[Bridge]")).isFalse();
        }

        @Test
        @DisplayName("takes the settings' reach when its sign says nothing")
        void defaultsToTheSetting() {
            SignLines blank = SignLines.of("", "[I]", "", "");

            assertThat(LightSwitches.rangeOf(blank, SETTINGS))
                    .isEqualTo(SETTINGS.lightSwitchRange());
            assertThat(LightSwitches.lightsOf(blank, SETTINGS))
                    .isEqualTo(SETTINGS.lightSwitchMaxLights());
        }

        @Test
        @DisplayName("takes a smaller reach off its own sign")
        void takesASmallerReachOffTheSign() {
            SignLines asking = SignLines.of("", "[I]", "3", "5");

            assertThat(LightSwitches.rangeOf(asking, SETTINGS)).isEqualTo(3);
            assertThat(LightSwitches.lightsOf(asking, SETTINGS)).isEqualTo(5);
        }

        @Test
        @DisplayName("gets only what it is allowed when its sign asks for more")
        void isHeldToTheSetting() {
            SignLines greedy = SignLines.of("", "[I]", "500", "500");

            assertThat(LightSwitches.rangeOf(greedy, SETTINGS))
                    .isEqualTo(SETTINGS.lightSwitchRange());
            assertThat(LightSwitches.lightsOf(greedy, SETTINGS))
                    .isEqualTo(SETTINGS.lightSwitchMaxLights());
        }

        @Test
        @DisplayName("ignores a line that is not a number")
        void ignoresRubbishOnTheSign() {
            SignLines rubbish = SignLines.of("", "[I]", "far", "lots");

            assertThat(LightSwitches.rangeOf(rubbish, SETTINGS))
                    .isEqualTo(SETTINGS.lightSwitchRange());
        }

        @Test
        @DisplayName("reaches a ball around itself, evenly in every direction")
        void reachesEvenlyInEveryDirection() {
            List<Vec3i> within = LightSwitches.reach(2);

            assertThat(within).contains(new Vec3i(-2, 0, 0), new Vec3i(2, 0, 0));
            assertThat(within).doesNotContain(new Vec3i(2, 2, 2));
        }

        @Test
        @DisplayName("puts what is nearest first, since the limit bites in that order")
        void putsTheNearestFirst() {
            List<Vec3i> within = LightSwitches.reach(3);

            assertThat(within.getFirst()).isEqualTo(new Vec3i(0, 0, 0));
            assertThat(within).isSortedAccordingTo((left, right) -> Long.compare(
                    (long) left.x() * left.x() + (long) left.y() * left.y()
                            + (long) left.z() * left.z(),
                    (long) right.x() * right.x() + (long) right.y() * right.y()
                            + (long) right.z() * right.z()));
        }
    }
}
