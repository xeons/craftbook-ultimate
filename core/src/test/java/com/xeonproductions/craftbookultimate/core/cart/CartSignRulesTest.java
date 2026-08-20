package com.xeonproductions.craftbookultimate.core.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Checking a cart mechanic's sign as it is written")
class CartSignRulesTest {

    private static final SimpleCartWorld WORLD = new SimpleCartWorld()
            .knowingOnly("stone", "coal", "torch")
            .withRecipe(new CartRecipe(
                    "torch", Map.of(Blocks.key("coal"), 1), Blocks.key("torch"), 4));

    private static Optional<String> check(String name, String... lines) {
        return CartSignRules.problemWith(name, SignLines.of(lines), WORLD);
    }

    @Nested
    @DisplayName("recognising a mechanic")
    class RecognisingAMechanic {

        @Test
        void findsTheNameOnTheSecondLine() {
            assertThat(CartSignRules.nameOn(SignLines.of("", "[Station]", "", ""))).contains("Station");
        }

        @Test
        void readsTheNameWhicheverWayItWasWritten() {
            assertThat(CartSignRules.nameOn(SignLines.of("", "[station]", "", ""))).contains("Station");
            assertThat(CartSignRules.nameOn(SignLines.of("", "[CARTLIFT]", "", ""))).contains("CartLift");
        }

        @Test
        void findsNothingOnAnOrdinarySign() {
            assertThat(CartSignRules.nameOn(SignLines.of("Welcome", "to the", "north", "line")))
                    .isEmpty();
        }

        @Test
        void findsNothingWhenTheNameIsOnTheWrongLine() {
            assertThat(CartSignRules.nameOn(SignLines.of("[Station]", "", "", ""))).isEmpty();
        }
    }

    @Nested
    @DisplayName("a delay")
    class ADelay {

        @Test
        void acceptsAWaitInSeconds() {
            assertThat(check("Delay", "", "[Delay]", "5", "")).isEmpty();
        }

        @Test
        void refusesAWaitThatIsNotANumber() {
            assertThat(check("Delay", "", "[Delay]", "soon", "")).isPresent();
        }

        @Test
        void refusesAWaitOfNothingAtAll() {
            assertThat(check("Delay", "", "[Delay]", "0", "")).isPresent();
        }

        @Test
        void refusesAWaitLongEnoughToStrandACart() {
            assertThat(check("Delay", "", "[Delay]", "99999", "")).isPresent();
        }
    }

    @Nested
    @DisplayName("the mechanics that take filters")
    class TheMechanicsThatTakeFilters {

        @Test
        void acceptsTwoGoodFilters() {
            assertThat(check("Sort", "", "[Sort]", "storage", "#north*")).isEmpty();
            assertThat(check("CartLift", "", "[CartLift]", "all", "")).isEmpty();
            assertThat(check("Launch", "", "[Launch]", "player", "empty")).isEmpty();
        }

        @Test
        void refusesAFilterItDoesNotUnderstand() {
            assertThat(check("Sort", "", "[Sort]", "aeroplane", "")).isPresent();
        }

        @Test
        void namesTheLineThatIsWrong() {
            assertThat(check("Sort", "", "[Sort]", "all", "aeroplane")).get()
                    .asString()
                    .contains("Line 4");
        }
    }

    @Nested
    @DisplayName("a station")
    class AStation {

        @Test
        void acceptsANameOnItsThirdLine() {
            assertThat(check("Station", "", "[Station]", "#northgate", "")).isEmpty();
        }

        @Test
        void acceptsSayingNothingAtAll() {
            assertThat(check("Station", "", "[Station]", "", "")).isEmpty();
        }

        @Test
        void refusesTextOnTheLinesItDoesNotRead() {
            assertThat(check("Station", "hello", "[Station]", "", "")).isPresent();
            assertThat(check("Station", "", "[Station]", "", "hello")).isPresent();
        }
    }

    @Nested
    @DisplayName("the chest mechanics")
    class TheChestMechanics {

        @Test
        void acceptsSayingNothing() {
            assertThat(check("Collect", "", "[Collect]", "", "")).isEmpty();
        }

        @Test
        void acceptsAnItemAndAnAmount() {
            assertThat(check("Deposit", "", "[Deposit]", "stone:5", "")).isEmpty();
        }

        @Test
        void refusesAnItemNothingCanResolve() {
            assertThat(check("Collect", "", "[Collect]", "notablock", "")).isPresent();
        }

        @Test
        void refusesAnAmountThatIsNotANumber() {
            assertThat(check("Collect", "", "[Collect]", "stone:lots", "")).isPresent();
        }
    }

    @Nested
    @DisplayName("a crafter")
    class ACrafter {

        @Test
        void acceptsARecipeTheServerKnows() {
            assertThat(check("Craft", "", "[Craft]", "torch", "")).isEmpty();
        }

        @Test
        void acceptsARecipeSpreadOverTwoLines() {
            assertThat(check("Craft", "", "[Craft]", "tor", "ch")).isEmpty();
        }

        @Test
        void refusesARecipeNobodyKnows() {
            assertThat(check("Craft", "", "[Craft]", "aeroplane", "")).isPresent();
        }

        @Test
        void refusesNamingNoRecipeAtAll() {
            assertThat(check("Craft", "", "[Craft]", "", "")).isPresent();
        }
    }

    @Nested
    @DisplayName("a dispenser")
    class ADispenser {

        @Test
        void acceptsSayingNothing() {
            assertThat(check("Dispenser", "", "[Dispenser]", "", "")).isEmpty();
        }

        @Test
        void acceptsAVehicleAndAPush() {
            assertThat(check("Dispenser", "", "[Dispenser]", "storage", "push")).isEmpty();
            assertThat(check("Dispenser", "", "[Dispenser]", "push", "")).isEmpty();
        }

        @Test
        void refusesAVehicleItCannotHandOut() {
            assertThat(check("Dispenser", "", "[Dispenser]", "aeroplane", "")).isPresent();
        }

        @Test
        void refusesAnythingButPushOnItsLastLine() {
            assertThat(check("Dispenser", "", "[Dispenser]", "", "shove")).isPresent();
        }
    }

    @Test
    void hasARuleForEveryNameItRecognises() {
        // A name with no rule would be created without anything on it being checked.
        assertThat(CartSignRules.names()).contains("Station", "Sort", "CartLift", "Launch",
                "Delay", "Print", "Collect", "Deposit", "Craft", "Dispenser");
    }
}
