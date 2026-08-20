package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.area.AreaName;
import com.xeonproductions.craftbookultimate.core.area.SimpleAreaVault;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The toggled area")
class ToggleAreaTest {

    private static final ToggleArea AREA = new ToggleArea();

    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final String OWNER = "tester";

    private static final AreaName HOUSE = new AreaName(OWNER, "house");
    private static final AreaName DAY = new AreaName(OWNER, "day");
    private static final AreaName NIGHT = new AreaName(OWNER, "night");

    private final SimpleAreaVault vault = new SimpleAreaVault();
    private final SimpleMechanicWorld world = new SimpleMechanicWorld().withVault(vault);

    /** A sign of the given kind, naming one or two areas. */
    private SimpleMechanicWorld withSign(String kind, String placed, String other) {
        return world.withSign(SIGN, BlockFace.SOUTH, OWNER, kind, placed, other);
    }

    private MechanicVisit byHand() {
        return MechanicVisit.byHand(
                world.signAt(SIGN).orElseThrow(),
                world,
                Settings.DEFAULTS,
                SimpleActor.at(new Vec3i(0, 64, 1)));
    }

    private MechanicVisit byRedstone(boolean powered) {
        return MechanicVisit.byRedstone(
                world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, powered);
    }

    /** What the sign says now. */
    private String line(int index) {
        return world.signAt(SIGN).orElseThrow().line(index);
    }

    @Nested
    @DisplayName("an area that clears its own space")
    class AnAreaThatClearsItsOwnSpace {

        @Test
        void putsTheAreaUpTheFirstTimeItIsUsed() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            assertThat(AREA.act(byHand())).isTrue();

            assertThat(vault.restored()).containsExactly(HOUSE);
        }

        @Test
        void marksTheAreaAsStandingOnItsOwnSign() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            AREA.act(byHand());

            assertThat(line(ToggleArea.ON_LINE)).isEqualTo("-house-");
        }

        @Test
        void emptiesTheSpaceTheSecondTime() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            AREA.act(byHand());
            AREA.act(byHand());

            assertThat(vault.cleared()).containsExactly(HOUSE);
        }

        @Test
        void comesBackToWhereItStartedAfterTwoUses() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            AREA.act(byHand());
            AREA.act(byHand());

            assertThat(line(ToggleArea.ON_LINE)).isEqualTo("house");
            assertThat(line(ToggleArea.OFF_LINE)).isEqualTo(AreaName.NONE);
        }

        @Test
        void goesBackAndForthForeverWithoutDrifting() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            for (int use = 0; use < 6; use++) {
                AREA.act(byHand());
            }

            assertThat(vault.restored()).containsExactly(HOUSE, HOUSE, HOUSE);
            assertThat(vault.cleared()).containsExactly(HOUSE, HOUSE, HOUSE);
        }
    }

    @Nested
    @DisplayName("an area that swaps with another")
    class AnAreaThatSwapsWithAnother {

        @Test
        void putsTheSecondAreaUpFirst() {
            // A sign is written next to the area it already names, so the first use is the one
            // that takes it away.
            withSign(ToggleArea.TOGGLE, "day", "night");
            vault.with(DAY, world.id()).with(NIGHT, world.id());

            AREA.act(byHand());

            assertThat(vault.restored()).containsExactly(NIGHT);
            assertThat(vault.cleared()).isEmpty();
        }

        @Test
        void swapsBackToTheFirstOnTheNextUse() {
            withSign(ToggleArea.TOGGLE, "day", "night");
            vault.with(DAY, world.id()).with(NIGHT, world.id());

            AREA.act(byHand());
            AREA.act(byHand());

            assertThat(vault.restored()).containsExactly(NIGHT, DAY);
        }

        @Test
        void marksWhicheverHalfIsStanding() {
            withSign(ToggleArea.TOGGLE, "day", "night");
            vault.with(DAY, world.id()).with(NIGHT, world.id());

            AREA.act(byHand());
            assertThat(line(ToggleArea.OFF_LINE)).isEqualTo("-night-");
            assertThat(line(ToggleArea.ON_LINE)).isEqualTo("day");

            AREA.act(byHand());
            assertThat(line(ToggleArea.ON_LINE)).isEqualTo("-day-");
            assertThat(line(ToggleArea.OFF_LINE)).isEqualTo("night");
        }

        @Test
        void neverEmptiesTheSpace() {
            withSign(ToggleArea.TOGGLE, "day", "night");
            vault.with(DAY, world.id()).with(NIGHT, world.id());

            for (int use = 0; use < 4; use++) {
                AREA.act(byHand());
            }

            assertThat(vault.cleared()).isEmpty();
        }
    }

    @Nested
    @DisplayName("the sign that writes back over itself")
    class TheSignThatWritesBackOverItself {

        @Test
        void writesDownTheHalfItIsPuttingAway() {
            withSign(ToggleArea.SAVING, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            AREA.act(byHand());
            AREA.act(byHand());

            assertThat(vault.captured()).containsExactly(HOUSE);
        }

        @Test
        void writesDownTheOtherHalfBeforeReplacingIt() {
            withSign(ToggleArea.SAVING, "day", "night");
            vault.with(DAY, world.id()).with(NIGHT, world.id());

            AREA.act(byHand());
            AREA.act(byHand());

            assertThat(vault.captured()).containsExactly(DAY, NIGHT);
        }

        @Test
        void theOrdinarySignWritesDownNothing() {
            withSign(ToggleArea.TOGGLE, "day", "night");
            vault.with(DAY, world.id()).with(NIGHT, world.id());

            AREA.act(byHand());
            AREA.act(byHand());

            assertThat(vault.captured()).isEmpty();
        }
    }

    @Nested
    @DisplayName("driven by redstone")
    class DrivenByRedstone {

        @Test
        void putsTheAreaUpWhenPowerArrives() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            AREA.act(byRedstone(true));

            assertThat(vault.restored()).containsExactly(HOUSE);
        }

        @Test
        void takesItAwayWhenPowerLeaves() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            AREA.act(byRedstone(true));
            AREA.act(byRedstone(false));

            assertThat(vault.cleared()).containsExactly(HOUSE);
        }

        @Test
        void staysUpWhenPowerArrivesAgain() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id());

            AREA.act(byRedstone(true));
            AREA.act(byRedstone(true));

            assertThat(vault.cleared()).isEmpty();
            assertThat(vault.restored()).containsExactly(HOUSE, HOUSE);
        }
    }

    @Nested
    @DisplayName("what it refuses")
    class WhatItRefuses {

        @Test
        void aSignNamingAnAreaNobodyHasSaved() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = AREA.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("no area saved")).isTrue();
        }

        @Test
        void aSignNamingNothingAtAll() {
            withSign(ToggleArea.TOGGLE, "", AreaName.NONE);
            SimpleActor who = SimpleActor.at(new Vec3i(0, 64, 1));

            boolean acted = AREA.act(MechanicVisit.byHand(
                    world.signAt(SIGN).orElseThrow(), world, Settings.DEFAULTS, who));

            assertThat(acted).isFalse();
            assertThat(who.wasTold("does not name an area")).isTrue();
        }

        @Test
        void leavesTheSignAloneWhenTheStoreCannotBeReached() {
            withSign(ToggleArea.TOGGLE, "house", AreaName.NONE);
            vault.with(HOUSE, world.id()).broken();

            assertThat(AREA.act(byHand())).isFalse();

            assertThat(line(ToggleArea.ON_LINE)).isEqualTo("house");
        }
    }

    @Nested
    @DisplayName("checking a sign as it is written")
    class CheckingASignAsItIsWritten {

        private final SimpleActor builder = SimpleActor.named(OWNER);

        private SignReview review(String namespace, String kind, String placed, String other) {
            return AREA.review(
                    SignLines.of(namespace, kind, placed, other), builder, world);
        }

        @Test
        void putsTheBuildersOwnNameOnASignThatNamesNobody() {
            vault.with(HOUSE, world.id());

            SignReview review = review("", ToggleArea.TOGGLE, "house", AreaName.NONE);

            assertThat(review).isInstanceOfSatisfying(SignReview.Accepted.class, accepted ->
                    assertThat(accepted.lines().trimmedText(ToggleArea.NAMESPACE_LINE))
                            .isEqualTo(OWNER));
        }

        @Test
        void refusesASignNamingAnAreaNobodyHasSaved() {
            SignReview review = review("", ToggleArea.TOGGLE, "house", AreaName.NONE);

            assertThat(review).isInstanceOfSatisfying(SignReview.Refused.class, refused ->
                    assertThat(refused.why()).contains("no area saved"));
        }

        @Test
        void refusesASignWhoseSecondAreaIsMissing() {
            vault.with(DAY, world.id());

            SignReview review = review("", ToggleArea.TOGGLE, "day", "night");

            assertThat(review).isInstanceOf(SignReview.Refused.class);
        }

        @Test
        void acceptsASignThatClearsItsSpaceInsteadOfSwapping() {
            vault.with(HOUSE, world.id());

            SignReview review = review("", ToggleArea.TOGGLE, "house", AreaName.NONE);

            assertThat(review).isInstanceOf(SignReview.Accepted.class);
        }

        @Test
        void refusesAnIdentifierNoAreaCouldBeSavedUnder() {
            SignReview review = review("", ToggleArea.TOGGLE, "a name with spaces", AreaName.NONE);

            assertThat(review).isInstanceOfSatisfying(SignReview.Refused.class, refused ->
                    assertThat(refused.why()).contains("third line"));
        }

        @Test
        void refusesTheSharedNamespaceToSomebodyWithoutThePermission() {
            SimpleActor plain = SimpleActor.named(OWNER).allowedOnly();

            SignReview review = AREA.review(
                    SignLines.of(AreaName.GLOBAL, ToggleArea.TOGGLE, "house", AreaName.NONE),
                    plain,
                    world);

            assertThat(review).isInstanceOfSatisfying(SignReview.Refused.class, refused ->
                    assertThat(refused.why()).contains("everybody shares"));
        }

        @Test
        void allowsTheSharedNamespaceToSomebodyWithIt() {
            vault.with(new AreaName(AreaName.GLOBAL, "house"), world.id());
            SimpleActor staff = SimpleActor.named(OWNER)
                    .allowedOnly(ToggleArea.GLOBAL_PERMISSION);

            SignReview review = AREA.review(
                    SignLines.of("global", ToggleArea.TOGGLE, "house", AreaName.NONE),
                    staff,
                    world);

            assertThat(review).isInstanceOfSatisfying(SignReview.Accepted.class, accepted ->
                    assertThat(accepted.lines().trimmedText(ToggleArea.NAMESPACE_LINE))
                            .isEqualTo(AreaName.GLOBAL));
        }

        @Test
        void refusesSomebodyElsesAreasToSomebodyWithoutThePermission() {
            SimpleActor plain = SimpleActor.named(OWNER).allowedOnly();

            SignReview review = AREA.review(
                    SignLines.of("alice", ToggleArea.TOGGLE, "house", AreaName.NONE),
                    plain,
                    world);

            assertThat(review).isInstanceOfSatisfying(SignReview.Refused.class, refused ->
                    assertThat(refused.why()).contains("somebody else"));
        }

        @Test
        void refusesTheWritingBackSignToSomebodyWithoutThePermission() {
            vault.with(HOUSE, world.id());
            SimpleActor plain = SimpleActor.named(OWNER).allowedOnly();

            SignReview review = AREA.review(
                    SignLines.of("", ToggleArea.SAVING, "house", AreaName.NONE), plain, world);

            assertThat(review).isInstanceOfSatisfying(SignReview.Refused.class, refused ->
                    assertThat(refused.why()).contains("writes back"));
        }

        @Test
        void allowsTheWritingBackSignToSomebodyWithIt() {
            vault.with(HOUSE, world.id());
            SimpleActor staff = SimpleActor.named(OWNER)
                    .allowedOnly(ToggleArea.SAVE_SIGN_PERMISSION);

            SignReview review = AREA.review(
                    SignLines.of("", ToggleArea.SAVING, "house", AreaName.NONE), staff, world);

            assertThat(review).isInstanceOf(SignReview.Accepted.class);
        }
    }
}
