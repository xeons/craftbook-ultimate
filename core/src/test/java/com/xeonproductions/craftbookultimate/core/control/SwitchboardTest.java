// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Switches thrown by command")
class SwitchboardTest {

    private final Switchboard board = new Switchboard();

    @Nested
    @DisplayName("which switches exist")
    class WhichSwitchesExist {

        @Test
        void onlyThoseAChipIsFollowing() {
            assertThat(board.isKnown("door")).isFalse();

            board.register("door");

            assertThat(board.isKnown("door")).isTrue();
        }

        @Test
        void forgetsOneWhenNothingFollowsItAnyMore() {
            board.register("door");
            board.set("door", true);

            board.forget("door");

            assertThat(board.isKnown("door")).isFalse();
        }

        @Test
        void keepsWhereASwitchWasThrownAfterEverythingStopsFollowingIt() {
            // A chunk going out of view must not swing a door shut.
            board.register("door");
            board.set("door", true);

            board.forget("door");
            board.register("door");

            assertThat(board.state("door")).contains(true);
        }

        @Test
        void keepsASwitchWhileAnotherChipIsStillFollowingIt() {
            board.register("door");
            board.register("door");
            board.set("door", true);

            board.forget("door");

            assertThat(board.isKnown("door")).isTrue();
            assertThat(board.state("door")).contains(true);
        }

        @Test
        void refusesToThrowASwitchNothingIsFollowing() {
            assertThat(board.set("door", true)).isFalse();
            assertThat(board.state("door")).isEmpty();
        }

        @Test
        void keepsAPositionWhenAnotherChipRegistersTheSameName() {
            // Two chips may follow one switch, and the second loading must not reset it.
            board.register("door");
            board.set("door", true);

            board.register("door");

            assertThat(board.state("door")).contains(true);
        }

        @Test
        void listsWhatItKnowsInOrder() {
            board.register("gate");
            board.register("door");
            board.register("hatch");

            assertThat(board.names()).containsExactly("door", "gate", "hatch");
        }
    }

    @Nested
    @DisplayName("throwing a switch")
    class ThrowingASwitch {

        @Test
        void setsItOneWayOrTheOther() {
            board.register("door");

            assertThat(board.set("door", true)).isTrue();
            assertThat(board.state("door")).contains(true);

            board.set("door", false);
            assertThat(board.state("door")).contains(false);
        }

        @Test
        void refusesANameNoChipIsFollowing() {
            assertThat(board.set("door", true)).isFalse();
            assertThat(board.state("door")).isEmpty();
        }

        @Test
        void togglesToTheOtherPosition() {
            board.register("door");
            board.set("door", true);

            assertThat(board.toggle("door")).contains(false);
            assertThat(board.toggle("door")).contains(true);
        }

        @Test
        void cannotToggleOneThatHasNeverBeenThrown() {
            // There is no other position to go to, so a command has to say which way.
            board.register("door");

            assertThat(board.toggle("door")).isEmpty();
            assertThat(board.state("door")).isEmpty();
        }

        @Test
        void cannotToggleANameNoChipIsFollowing() {
            assertThat(board.toggle("door")).isEmpty();
        }
    }

    @Test
    void keepsSwitchesOfTheSameNameOnDifferentBoardsApart() {
        Switchboard other = new Switchboard();
        board.register("door");
        other.register("door");

        board.set("door", true);

        assertThat(board.state("door")).contains(true);
        assertThat(other.state("door")).isEmpty();
    }

    @Nested
    @DisplayName("keeping switches between restarts")
    class KeepingSwitchesBetweenRestarts {

        @Test
        void putsASwitchBackWhereItWas() {
            board.register("door");
            board.set("door", true);

            Switchboard afterRestart = new Switchboard();
            afterRestart.load(board.save());
            afterRestart.register("door");

            assertThat(afterRestart.state("door")).contains(true);
        }

        @Test
        void putsBackASwitchNothingIsFollowingYet() {
            // The file is read before any chip has loaded, so nothing is following anything.
            Switchboard afterRestart = new Switchboard();
            afterRestart.load(List.of("false Front Door"));

            assertThat(afterRestart.state("Front Door")).contains(false);
            assertThat(afterRestart.isKnown("Front Door")).isFalse();
        }

        @Test
        void keepsANameWithSpacesInIt() {
            board.register("the north gate");
            board.set("the north gate", true);

            Switchboard afterRestart = new Switchboard();
            afterRestart.load(board.save());

            assertThat(afterRestart.state("the north gate")).contains(true);
        }

        @Test
        void writesNothingForASwitchNobodyHasThrown() {
            board.register("door");

            assertThat(board.save()).isEmpty();
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "door", "maybe door", " ", "true "})
        void skipsALineThatIsNotASwitch(String line) {
            assertThat(board.load(List.of(line))).isZero();
            assertThat(board.rememberedCount()).isZero();
        }

        @Test
        void carriesOnPastALineItCannotRead() {
            assertThat(board.load(List.of("nonsense", "true door"))).isEqualTo(1);
            assertThat(board.state("door")).contains(true);
        }
    }
}
