package com.xeonproductions.craftbookultimate.core.variable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("The variables")
class VariablesTest {

    private final Variables variables = new Variables();

    private final VariableName score = VariableName.shared("score");

    @Nested
    @DisplayName("making one")
    class Making {

        @Test
        @DisplayName("makes a variable that was not there")
        void makesAVariableThatWasNotThere() {
            assertThat(variables.define(score, "5")).isTrue();
            assertThat(variables.get(score)).contains("5");
        }

        @Test
        @DisplayName("leaves an existing variable alone")
        void leavesAnExistingVariableAlone() {
            variables.define(score, "5");

            assertThat(variables.define(score, "9")).isFalse();
            assertThat(variables.get(score)).contains("5");
        }

        @Test
        @DisplayName("refuses a value it could not save")
        void refusesAValueItCouldNotSave() {
            assertThat(variables.define(score, "two words")).isFalse();
            assertThat(variables.has(score)).isFalse();
        }
    }

    @Nested
    @DisplayName("changing one")
    class Changing {

        @Test
        @DisplayName("changes a variable that exists")
        void changesAVariableThatExists() {
            variables.define(score, "5");

            assertThat(variables.set(score, "9")).isTrue();
            assertThat(variables.get(score)).contains("9");
        }

        @Test
        @DisplayName("refuses to make one that does not exist")
        void refusesToMakeOneThatDoesNotExist() {
            assertThat(variables.set(score, "9")).isFalse();
            assertThat(variables.has(score)).isFalse();
        }
    }

    @Nested
    @DisplayName("read as a number")
    class AsANumber {

        @Test
        @DisplayName("gives the number it holds")
        void givesTheNumberItHolds() {
            variables.define(score, "7.5");

            assertThat(variables.number(score)).hasValue(7.5);
        }

        @Test
        @DisplayName("gives nothing for a variable nobody has made")
        void givesNothingForAVariableNobodyHasMade() {
            assertThat(variables.number(score)).isEmpty();
        }

        @Test
        @DisplayName("gives nothing for a variable holding something that is not a number")
        void givesNothingForAVariableHoldingSomethingThatIsNotANumber() {
            variables.define(score, "closed");

            assertThat(variables.number(score)).isEmpty();
        }
    }

    @Nested
    @DisplayName("writing a number")
    class WritingANumber {

        @ParameterizedTest
        @DisplayName("writes it as somebody would have typed it")
        @CsvSource({"7.0,7", "7.5,7.5", "-3.0,-3", "0.0,0", "100000000000000000000.0,100000000000000000000"})
        void writesItAsSomebodyWouldHaveTypedIt(double value, String written) {
            assertThat(Variables.format(value)).isEqualTo(written);
        }

        @Test
        @DisplayName("refuses a number that is not one, rather than storing the word for it")
        void refusesANumberThatIsNotOneRatherThanStoringTheWordForIt() {
            variables.define(score, "5");

            assertThat(variables.setNumber(score, Double.NaN)).isFalse();
            assertThat(variables.setNumber(score, Double.POSITIVE_INFINITY)).isFalse();
            assertThat(variables.get(score)).contains("5");
        }
    }

    @Nested
    @DisplayName("listing them")
    class Listing {

        @Test
        @DisplayName("gives every variable in a settled order")
        void givesEveryVariableInASettledOrder() {
            variables.define(new VariableName("bob", "score"), "1");
            variables.define(VariableName.shared("apples"), "2");
            variables.define(new VariableName("alice", "score"), "3");

            assertThat(variables.names()).containsExactly(
                    new VariableName("alice", "score"),
                    new VariableName("bob", "score"),
                    VariableName.shared("apples"));
        }

        @Test
        @DisplayName("narrows to one namespace when asked")
        void narrowsToOneNamespaceWhenAsked() {
            variables.define(new VariableName("alice", "score"), "1");
            variables.define(VariableName.shared("score"), "2");

            assertThat(variables.namesIn("alice"))
                    .containsExactly(new VariableName("alice", "score"));
        }
    }

    @Nested
    @DisplayName("saved and read back")
    class Persistence {

        @Test
        @DisplayName("comes back holding what it held")
        void comesBackHoldingWhatItHeld() {
            variables.define(VariableName.shared("score"), "12");
            variables.define(new VariableName("alice", "wins"), "-3.5");

            Variables reloaded = new Variables();
            int read = reloaded.load(variables.save());

            assertThat(read).isEqualTo(2);
            assertThat(reloaded.get(VariableName.shared("score"))).contains("12");
            assertThat(reloaded.get(new VariableName("alice", "wins"))).contains("-3.5");
        }

        @ParameterizedTest
        @DisplayName("skips a line somebody has broken rather than losing the rest")
        @ValueSource(strings = {"", "global", "global score", "global sco re 5", "glo bal score 5"})
        void skipsALineSomebodyHasBrokenRatherThanLosingTheRest(String broken) {
            int read = variables.load(List.of(broken, "global score 5"));

            assertThat(read).isEqualTo(1);
            assertThat(variables.get(VariableName.shared("score"))).contains("5");
        }

        @Test
        @DisplayName("writes nothing for a store nobody has put anything in")
        void writesNothingForAStoreNobodyHasPutAnythingIn() {
            assertThat(variables.save()).isEmpty();
        }
    }

    @Test
    @DisplayName("forgets a variable that is removed")
    void forgetsAVariableThatIsRemoved() {
        variables.define(score, "5");

        assertThat(variables.remove(score)).isTrue();
        assertThat(variables.has(score)).isFalse();
        assertThat(variables.remove(score)).isFalse();
    }
}
