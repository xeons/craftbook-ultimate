// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The variable commands")
class VariableActionsTest {

    private final Variables variables = new Variables();

    private int saves;

    private final VariableActions actions = new VariableActions(variables, () -> saves++);

    private final RecordingCaller caller = new RecordingCaller();

    private static VariableName shared(String name) {
        return VariableName.parse(name, VariableName.SHARED).orElseThrow();
    }

    @Nested
    @DisplayName("making one")
    class Defining {

        @Test
        @DisplayName("makes it, and writes the variables out")
        void makesIt() {
            assertThat(actions.define(caller, "score", "5")).isTrue();

            assertThat(variables.get(shared("score"))).contains("5");
            assertThat(saves).isEqualTo(1);
        }

        @Test
        @DisplayName("will not quietly overwrite one that is already there")
        void willNotOverwrite() {
            variables.define(shared("score"), "5");

            assertThat(actions.define(caller, "score", "9")).isFalse();

            assertThat(variables.get(shared("score"))).contains("5");
            assertThat(caller.everything()).contains("already a variable");
        }

        @Test
        @DisplayName("refuses a value that could never be written back out")
        void refusesAnUnstorableValue() {
            assertThat(actions.define(caller, "score", "two words")).isFalse();
            assertThat(variables.has(shared("score"))).isFalse();
        }
    }

    @Nested
    @DisplayName("changing one")
    class Setting {

        @Test
        @DisplayName("will not make one that is not there, so a misspelling cannot make a second")
        void willNotCreate() {
            assertThat(actions.set(caller, "score", "9")).isFalse();

            assertThat(variables.has(shared("score"))).isFalse();
            assertThat(caller.everything()).contains("no variable called");
        }
    }

    @Nested
    @DisplayName("naming one")
    class Naming {

        @Test
        @DisplayName("means the shared variable when no namespace is written, as it does on a sign")
        void aBareNameIsShared() {
            variables.define(shared("score"), "5");

            assertThat(actions.get(caller.called("Steve"), "score")).isTrue();
            assertThat(caller.everything()).contains("5");
        }

        @Test
        @DisplayName("lets somebody touch their own")
        void ownNamespaceIsAllowed() {
            RecordingCaller steve = caller.called("Steve");
            assertThat(actions.define(steve, "Steve|score", "5")).isTrue();
        }

        @Test
        @DisplayName("keeps somebody out of another player's without the permission for it")
        void otherNamespaceIsRefused() {
            RecordingCaller steve = caller.called("Steve");

            assertThat(actions.define(steve, "Alex|score", "5")).isFalse();
            assertThat(steve.everything()).contains("belongs to alex");
        }

        @Test
        @DisplayName("lets them in with it")
        void otherNamespaceIsAllowedWithPermission() {
            RecordingCaller steve = caller
                    .called("Steve")
                    .allowed(VariableChips.OTHER_NAMESPACE_PERMISSION);

            assertThat(actions.define(steve, "Alex|score", "5")).isTrue();
        }
    }

    @Nested
    @DisplayName("doing a sum to one")
    class Arithmetic {

        @Test
        @DisplayName("does it")
        void doesTheSum() {
            variables.define(shared("score"), "5");

            assertThat(actions.apply(caller, "score", VariableChips.Function.ADD, 3)).isTrue();
            assertThat(variables.number(shared("score"))).hasValue(8);
        }

        @Test
        @DisplayName("says so when the variable holds something that is not a number")
        void refusesSomethingThatIsNotANumber() {
            variables.define(shared("label"), "north");

            assertThat(actions.apply(caller, "label", VariableChips.Function.ADD, 3)).isFalse();
            assertThat(caller.everything()).contains("does not hold a number");
        }
    }

    @Nested
    @DisplayName("listing them")
    class Listing {

        @Test
        @DisplayName("says there are none rather than showing an empty list")
        void saysThereAreNone() {
            assertThat(actions.list(caller, Optional.empty())).isTrue();
            assertThat(caller.everything()).contains("no variables yet");
        }
    }
}
