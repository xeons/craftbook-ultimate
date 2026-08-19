package com.xeonproductions.craftbookultimate.core.control;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
