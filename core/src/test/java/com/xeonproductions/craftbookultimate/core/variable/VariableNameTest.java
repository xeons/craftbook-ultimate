// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.variable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("A variable name")
class VariableNameTest {

    @Nested
    @DisplayName("read off a sign")
    class Reading {

        @Test
        @DisplayName("puts a bare name in the shared namespace")
        void putsABareNameInTheSharedNamespace() {
            VariableName name = VariableName.parse("score").orElseThrow();

            assertThat(name.namespace()).isEqualTo(VariableName.SHARED);
            assertThat(name.name()).isEqualTo("score");
            assertThat(name.isShared()).isTrue();
        }

        @Test
        @DisplayName("takes the namespace written before the bar")
        void takesTheNamespaceWrittenBeforeTheBar() {
            VariableName name = VariableName.parse("alice|score").orElseThrow();

            assertThat(name.namespace()).isEqualTo("alice");
            assertThat(name.name()).isEqualTo("score");
            assertThat(name.isShared()).isFalse();
        }

        @Test
        @DisplayName("reads the same variable however it is capitalised")
        void readsTheSameVariableHoweverItIsCapitalised() {
            assertThat(VariableName.parse("Alice|Score"))
                    .contains(VariableName.parse("alice|score").orElseThrow());
        }

        @Test
        @DisplayName("ignores space around what was written")
        void ignoresSpaceAroundWhatWasWritten() {
            assertThat(VariableName.parse("  score  ")).contains(VariableName.shared("score"));
        }

        @ParameterizedTest
        @DisplayName("refuses anything that is not a name")
        @ValueSource(strings = {"", "   ", "a b", "sco re", "score!", "sco.re", "|score", "alice|", "|"})
        void refusesAnythingThatIsNotAName(String written) {
            assertThat(VariableName.parse(written)).isEmpty();
        }
    }

    @Nested
    @DisplayName("read from a command")
    class FromACommand {

        @Test
        @DisplayName("falls back to the namespace given alongside it")
        void fallsBackToTheNamespaceGivenAlongsideIt() {
            assertThat(VariableName.parse("score", "alice"))
                    .contains(new VariableName("alice", "score"));
        }

        @Test
        @DisplayName("keeps a namespace written into the name over the one given alongside it")
        void keepsANamespaceWrittenIntoTheNameOverTheOneGivenAlongsideIt() {
            assertThat(VariableName.parse("bob|score", "alice"))
                    .contains(new VariableName("bob", "score"));
        }
    }

    @Nested
    @DisplayName("written back out")
    class WritingOut {

        @Test
        @DisplayName("gives a shared variable as its bare name")
        void givesASharedVariableAsItsBareName() {
            assertThat(VariableName.shared("score")).hasToString("score");
        }

        @Test
        @DisplayName("gives somebody else's with its namespace")
        void givesSomebodyElsesWithItsNamespace() {
            assertThat(new VariableName("alice", "score")).hasToString("alice|score");
        }

        @Test
        @DisplayName("names the shared namespace when asked for the name in full")
        void namesTheSharedNamespaceWhenAskedForTheNameInFull() {
            assertThat(VariableName.shared("score").qualified()).isEqualTo("global|score");
        }

        @Test
        @DisplayName("reads back as itself")
        void readsBackAsItself() {
            for (VariableName name :
                    java.util.List.of(VariableName.shared("score"), new VariableName("alice", "score"))) {
                assertThat(VariableName.parse(name.toString())).contains(name);
            }
        }
    }

    @Test
    @DisplayName("is a different variable in a different namespace")
    void isADifferentVariableInADifferentNamespace() {
        assertThat(new VariableName("alice", "score"))
                .isNotEqualTo(new VariableName("bob", "score"))
                .isNotEqualTo(VariableName.shared("score"));
    }

    @Test
    @DisplayName("has no reading at all when the whole line is blank")
    void hasNoReadingAtAllWhenTheWholeLineIsBlank() {
        assertThat(VariableName.parse("")).isEqualTo(Optional.empty());
    }
}
