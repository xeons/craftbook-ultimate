// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("A lever hidden behind the wall it works")
class HiddenSwitchTest {

    private static final Vec3i SIGN = new Vec3i(0, 64, 0);
    private static final Key KEY_ITEM = Key.key("minecraft:tripwire_hook");

    private static final Settings SETTINGS = Settings.builder()
            .mechanics(MechanicSettings.DEFAULTS.withEnabled(Set.of(Mechanics.HIDDEN_SWITCH)))
            .build();

    /**
     * A sign facing south, with a lever above and below it.
     *
     * <p>South-facing means its left and right run east and west, which is what the geometry test
     * below is about.
     */
    private static SimpleMechanicWorld wall(String keyLine) {
        return new SimpleMechanicWorld()
                .withSign(SIGN, BlockFace.SOUTH, keyLine, HiddenSwitch.SIGN, "", "")
                .withSwitch(SIGN.offset(BlockFace.UP))
                .withSwitch(SIGN.offset(BlockFace.DOWN));
    }

    private static boolean click(SimpleMechanicWorld world, SimpleActor who) {
        PostedSign sign = world.signAt(SIGN).orElseThrow();
        return HiddenSwitch.instance()
                .act(MechanicVisit.byHand(sign, world, SETTINGS, who));
    }

    @Nested
    @DisplayName("worked by hand")
    class ByHand {

        @Test
        @DisplayName("throws every lever touching its sign")
        void throwsEveryLever() {
            SimpleMechanicWorld world = wall("");

            assertThat(click(world, SimpleActor.named("tester"))).isTrue();
            assertThat(world.isSwitchOn(SIGN.offset(BlockFace.UP))).isTrue();
            assertThat(world.isSwitchOn(SIGN.offset(BlockFace.DOWN))).isTrue();
        }

        @Test
        @DisplayName("throws them back again when it is worked twice")
        void throwsThemBackAgain() {
            SimpleMechanicWorld world = wall("");
            SimpleActor who = SimpleActor.named("tester");

            click(world, who);
            click(world, who);

            assertThat(world.isSwitchOn(SIGN.offset(BlockFace.UP))).isFalse();
        }

        @Test
        @DisplayName("says so, since there is nothing to see from the front")
        void saysSo() {
            SimpleMechanicWorld world = wall("");
            SimpleActor who = SimpleActor.named("tester");

            click(world, who);

            assertThat(who.wasTold("switch")).isTrue();
        }

        @Test
        @DisplayName("does nothing at all when there is no lever behind the wall")
        void doesNothingWithNoLever() {
            SimpleMechanicWorld world = new SimpleMechanicWorld()
                    .withSign(SIGN, BlockFace.SOUTH, "", HiddenSwitch.SIGN, "", "");

            assertThat(click(world, SimpleActor.named("tester"))).isFalse();
        }
    }

    @Nested
    @DisplayName("with a key named on its first line")
    class WithAKey {

        @Test
        @DisplayName("answers to somebody holding one")
        void answersToSomebodyHoldingOne() {
            SimpleMechanicWorld world = wall(KEY_ITEM.asString());
            SimpleActor who = SimpleActor.named("tester").holding(KEY_ITEM);

            assertThat(click(world, who)).isTrue();
            assertThat(world.isSwitchOn(SIGN.offset(BlockFace.UP))).isTrue();
        }

        @Test
        @DisplayName("refuses somebody holding something else, and says why")
        void refusesSomebodyElse() {
            SimpleMechanicWorld world = wall(KEY_ITEM.asString());
            SimpleActor who = SimpleActor.named("tester").holding(Key.key("minecraft:stick"));

            click(world, who);

            assertThat(world.isSwitchOn(SIGN.offset(BlockFace.UP))).isFalse();
            assertThat(who.wasTold("key")).isTrue();
        }

        @Test
        @DisplayName("claims the click even so, so the wall is not built against instead")
        void claimsTheClickEvenSo() {
            SimpleMechanicWorld world = wall(KEY_ITEM.asString());
            SimpleActor who = SimpleActor.named("tester");

            assertThat(click(world, who)).isTrue();
        }

        @Test
        @DisplayName("is not asked for at all when the line is blank")
        void isNotAskedForWhenBlank() {
            SimpleMechanicWorld world = wall("");

            assertThat(HiddenSwitch.keyOn(world.signAt(SIGN).orElseThrow(), world)).isEmpty();
            assertThat(click(world, SimpleActor.named("tester").holding())).isTrue();
        }
    }

    @Nested
    @DisplayName("the levers it reaches")
    class Geometry {

        @Test
        @DisplayName("are above, below and along the wall, never through it")
        void areAlongTheWall() {
            PostedSign sign = new PostedSign(
                    SIGN, SignLines.of("", HiddenSwitch.SIGN, "", ""), BlockFace.SOUTH);

            assertThat(HiddenSwitch.switchesAround(sign))
                    .containsExactlyInAnyOrder(
                            SIGN.offset(BlockFace.UP),
                            SIGN.offset(BlockFace.DOWN),
                            SIGN.offset(BlockFace.EAST),
                            SIGN.offset(BlockFace.WEST));
        }

        @Test
        @DisplayName("turn with the sign, so a sign on an east wall reaches north and south")
        void turnWithTheSign() {
            PostedSign sign = new PostedSign(
                    SIGN, SignLines.of("", HiddenSwitch.SIGN, "", ""), BlockFace.EAST);

            assertThat(HiddenSwitch.switchesAround(sign))
                    .contains(SIGN.offset(BlockFace.NORTH), SIGN.offset(BlockFace.SOUTH));
        }
    }

    @Nested
    @DisplayName("driven by redstone")
    class ByRedstone {

        @Test
        @DisplayName("does nothing, since a switch working itself is nothing")
        void doesNothing() {
            SimpleMechanicWorld world = wall("");
            PostedSign sign = world.signAt(SIGN).orElseThrow();

            boolean acted = HiddenSwitch.instance()
                    .act(MechanicVisit.byRedstone(sign, world, SETTINGS, true));

            assertThat(acted).isFalse();
            assertThat(world.isSwitchOn(SIGN.offset(BlockFace.UP))).isFalse();
        }
    }

    @Nested
    @DisplayName("the sign")
    class TheSign {

        @Test
        @DisplayName("is claimed however the builder spelt it")
        void isClaimedHoweverSpelt() {
            assertThat(HiddenSwitch.isHiddenSwitch(SignLines.of("", "[x]", "", ""))).isTrue();
            assertThat(HiddenSwitch.isHiddenSwitch(SignLines.of("", "[X]", "", ""))).isTrue();
        }

        @Test
        @DisplayName("takes the fork's book form as well")
        void takesTheBookForm() {
            assertThat(HiddenSwitch.isHiddenSwitch(
                    SignLines.of("", HiddenSwitch.BOOK_SIGN, "", ""))).isTrue();
        }

        @Test
        @DisplayName("is not claimed when it says something else")
        void isNotClaimedOtherwise() {
            assertThat(HiddenSwitch.isHiddenSwitch(SignLines.of("", "[Gate]", "", ""))).isFalse();
        }
    }
}
