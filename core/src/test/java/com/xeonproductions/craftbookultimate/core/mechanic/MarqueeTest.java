// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("A sign that says what a variable says")
class MarqueeTest {

    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Settings SETTINGS = Settings.builder()
            .mechanics(MechanicSettings.DEFAULTS.withEnabled(Set.of(Mechanics.MARQUEE)))
            .build();

    private Variables variables;

    @BeforeEach
    void makeVariables() {
        variables = new Variables();
        variables.define(VariableName.shared("stock"), "42");
        variables.define(new VariableName("alice", "score"), "seven");
    }

    /** A world holding the variables, with a marquee sign in it. */
    private SimpleMechanicWorld world(String variable, String namespace) {
        return new SimpleMechanicWorld()
                .withVariables(variables)
                .withSign(SIGN, BlockFace.SOUTH, "", Marquee.SIGN, variable, namespace);
    }

    private static boolean click(SimpleMechanicWorld world, SimpleActor who) {
        return SignMechanics.marquee().act(
                MechanicVisit.byHand(world.signAt(SIGN).orElseThrow(), world, SETTINGS, who));
    }

    private SignReview review(String variable, String namespace, SimpleActor builder) {
        return SignMechanics.marquee().review(
                SignLines.of("", Marquee.SIGN, variable, namespace),
                builder,
                world(variable, namespace));
    }

    @Nested
    @DisplayName("clicked")
    class Clicked {

        @Test
        @DisplayName("says what the variable says")
        void saysWhatTheVariableSays() {
            SimpleActor who = SimpleActor.named("bob");

            assertThat(click(world("stock", ""), who)).isTrue();
            assertThat(who.wasTold("42")).isTrue();
        }

        @Test
        @DisplayName("takes the namespace off line 4, so a plain name is somebody's own")
        void takesTheNamespaceOffLineFour() {
            SimpleActor who = SimpleActor.named("bob");

            click(world("score", "alice"), who);

            assertThat(who.wasTold("seven")).isTrue();
        }

        @Test
        @DisplayName("takes a namespace written on line 3 as well, which wins")
        void takesANamespaceOnLineThree() {
            SimpleActor who = SimpleActor.named("bob");

            click(world("alice|score", ""), who);

            assertThat(who.wasTold("seven")).isTrue();
        }

        @Test
        @DisplayName("says so when the variable has gone since the sign was written")
        void saysSoWhenTheVariableHasGone() {
            SimpleMechanicWorld world = world("stock", "");
            variables.remove(VariableName.shared("stock"));
            SimpleActor who = SimpleActor.named("bob");

            assertThat(click(world, who)).isTrue();
            assertThat(who.wasTold("no variable")).isTrue();
        }

        @Test
        @DisplayName("says so when the sign names nothing readable at all")
        void saysSoWhenTheSignNamesNothing() {
            SimpleActor who = SimpleActor.named("bob");

            assertThat(click(world("", ""), who)).isTrue();
            assertThat(who.wasTold("does not name a variable")).isTrue();
        }
    }

    @Nested
    @DisplayName("driven by redstone")
    class ByRedstone {

        @Test
        @DisplayName("does nothing, since there is nobody to tell")
        void doesNothing() {
            SimpleMechanicWorld world = world("stock", "");

            boolean acted = SignMechanics.marquee().act(
                    MechanicVisit.byRedstone(
                            world.signAt(SIGN).orElseThrow(), world, SETTINGS, true));

            assertThat(acted).isFalse();
        }
    }

    @Nested
    @DisplayName("written")
    class Written {

        @Test
        @DisplayName("is kept when it names a variable that exists")
        void isKeptWhenTheVariableExists() {
            assertThat(review("stock", "", SimpleActor.named("bob")))
                    .isInstanceOf(SignReview.Accepted.class);
        }

        @Test
        @DisplayName("is refused when nobody has made that variable, and says how to")
        void isRefusedWhenNobodyHasMadeIt() {
            SignReview review = review("takings", "", SimpleActor.named("bob"));

            assertThat(review).isInstanceOf(SignReview.Refused.class);
            assertThat(((SignReview.Refused) review).why()).contains("/var define");
        }

        @Test
        @DisplayName("is refused when it names nothing a variable could be called")
        void isRefusedWhenItNamesNothing() {
            assertThat(review("", "", SimpleActor.named("bob")))
                    .isInstanceOf(SignReview.Refused.class);
        }

        @Test
        @DisplayName("is refused when it reads somebody else's without the permission")
        void isRefusedWhenItReadsSomebodyElses() {
            SignReview review =
                    review("score", "alice", SimpleActor.named("bob").allowedOnly());

            assertThat(review).isInstanceOf(SignReview.Refused.class);
            assertThat(((SignReview.Refused) review).why()).contains("alice");
        }

        @Test
        @DisplayName("is kept when somebody has the permission to read anybody's")
        void isKeptWithThePermission() {
            SimpleActor bob = SimpleActor.named("bob")
                    .allowedOnly(VariableChips.OTHER_NAMESPACE_PERMISSION);

            assertThat(review("score", "alice", bob))
                    .isInstanceOf(SignReview.Accepted.class);
        }

        @Test
        @DisplayName("is kept when somebody reads their own")
        void isKeptWhenSomebodyReadsTheirOwn() {
            assertThat(review("score", "alice", SimpleActor.named("alice")))
                    .isInstanceOf(SignReview.Accepted.class);
        }
    }

    @Nested
    @DisplayName("the sign")
    class TheSign {

        @Test
        @DisplayName("is claimed however the builder spelt it")
        void isClaimedHoweverSpelt() {
            assertThat(SignMechanics.marquee().claims(
                    SignLines.of("", "[marquee]", "stock", ""))).isTrue();
        }

        @Test
        @DisplayName("belongs to the mechanic named in the settings file")
        void belongsToTheNamedMechanic() {
            assertThat(SignMechanics.marquee().name()).isEqualTo(Mechanics.MARQUEE);
            assertThat(SignMechanics.marquee().usePermission())
                    .isEqualTo("craftbook.marquee.use");
        }
    }
}
