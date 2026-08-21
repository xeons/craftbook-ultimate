package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.mechanic.SimpleActor;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Variable chips")
class VariableChipsTest {

    private final ChipServices services = ChipServices.create();

    private final VariableName score = VariableName.shared("score");

    /** Where the sign hangs, and so where the block behind it and the chest above that are. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    /** The chest an item counter reads: above the block the sign hangs on, behind the sign. */
    private static final Vec3i CHEST = SIGN.offset(BlockFace.NORTH).add(0, 1, 0);

    private static final Key COAL = Key.key("minecraft", "coal");
    private static final Key IRON = Key.key("minecraft", "iron_ingot");

    private Variables variables() {
        return services.variables();
    }

    private SimpleChipState.Builder chip(String model, String variable, String argument) {
        return SimpleChipState.forLayout(PinLayout.SISO)
                .services(services)
                .at(SIGN, BlockFace.SOUTH)
                .sign("", "[" + model + "]", variable, argument);
    }

    @Nested
    @DisplayName("the modifier")
    class Modifier {

        private final ICLogic chip = VariableChips.modifier();

        private SimpleChipState powered(String variable, String sum) {
            return chip("VAR100", variable, sum).inputs(true).build();
        }

        @ParameterizedTest
        @DisplayName("does the sum its sign asks for")
        @CsvSource({
            "10, +:5, 15",
            "10, -:5, 5",
            "10, *:5, 50",
            "10, /:5, 2",
            "10, '%:3', 1",
            "10, add:5, 15",
            "10, x:3, 30",
        })
        void doesTheSumItsSignAsksFor(String start, String sum, String expected) {
            variables().define(score, start);

            chip.trigger(powered("score", sum));

            assertThat(variables().get(score)).contains(expected);
        }

        @Test
        @DisplayName("says it did the sum")
        void saysItDidTheSum() {
            variables().define(score, "10");
            SimpleChipState state = powered("score", "+:5");

            chip.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        @DisplayName("does nothing at all while its input is low")
        void doesNothingAtAllWhileItsInputIsLow() {
            variables().define(score, "10");

            chip.trigger(chip("VAR100", "score", "+:5").inputs(false).build());

            assertThat(variables().get(score)).contains("10");
        }

        @Test
        @DisplayName("leaves a counter alone rather than poisoning it when asked to divide by zero")
        void leavesACounterAloneRatherThanPoisoningItWhenAskedToDivideByZero() {
            variables().define(score, "10");

            chip.trigger(powered("score", "/:0"));

            assertThat(variables().get(score)).contains("10");
        }

        @Test
        @DisplayName("leaves a counter alone rather than poisoning it when asked for a remainder by zero")
        void leavesACounterAloneRatherThanPoisoningItWhenAskedForARemainderByZero() {
            variables().define(score, "10");

            chip.trigger(powered("score", "%:0"));

            assertThat(variables().get(score)).contains("10");
            assertThat(variables().number(score)).hasValue(10);
        }

        @Test
        @DisplayName("reports failure when the variable has gone away, rather than throwing")
        void reportsFailureWhenTheVariableHasGoneAwayRatherThanThrowing() {
            SimpleChipState state = powered("score", "+:5");

            chip.trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        @DisplayName("reports failure when the variable holds something that is not a number")
        void reportsFailureWhenTheVariableHoldsSomethingThatIsNotANumber() {
            variables().define(score, "closed");
            SimpleChipState state = powered("score", "+:5");

            chip.trigger(state);

            assertThat(state.mainOutput()).isFalse();
            assertThat(variables().get(score)).contains("closed");
        }

        @Test
        @DisplayName("counts up a whole number without a decimal point appearing")
        void countsUpAWholeNumberWithoutADecimalPointAppearing() {
            variables().define(score, "0");
            SimpleChipState state = powered("score", "+:1");

            chip.trigger(state);
            chip.trigger(state);

            assertThat(variables().get(score)).contains("2");
        }
    }

    @Nested
    @DisplayName("is at least")
    class IsAtLeast {

        private final SelfTriggeringICLogic chip = VariableChips.isAtLeast();

        @Test
        @DisplayName("goes high once the variable has reached the number")
        void goesHighOnceTheVariableHasReachedTheNumber() {
            variables().define(score, "10");
            SimpleChipState state = chip("VAR170", "score", "10").build();

            chip.tick(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        @DisplayName("stays low while the variable is short of it")
        void staysLowWhileTheVariableIsShortOfIt() {
            variables().define(score, "9");
            SimpleChipState state = chip("VAR170", "score", "10").build();

            chip.tick(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        @DisplayName("follows the variable as it changes")
        void followsTheVariableAsItChanges() {
            variables().define(score, "0");
            SimpleChipState state = chip("VAR170", "score", "5").build();

            chip.tick(state);
            assertThat(state.mainOutput()).isFalse();

            variables().set(score, "5");
            chip.tick(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        @DisplayName("reads the variable when its input goes high, for a chip driven by a clock")
        void readsTheVariableWhenItsInputGoesHighForAChipDrivenByAClock() {
            variables().define(score, "10");
            SimpleChipState state = chip("VAR170", "score", "5").inputs(true).build();

            chip.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        @DisplayName("goes low rather than throwing when the variable has gone away")
        void goesLowRatherThanThrowingWhenTheVariableHasGoneAway() {
            variables().define(score, "10");
            SimpleChipState state = chip("VAR170", "score", "5").build();
            chip.tick(state);
            assertThat(state.mainOutput()).isTrue();

            variables().remove(score);
            chip.tick(state);

            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        @DisplayName("goes low when the variable holds something that is not a number")
        void goesLowWhenTheVariableHoldsSomethingThatIsNotANumber() {
            variables().define(score, "closed");
            SimpleChipState state = chip("VAR170", "score", "5").build();

            chip.tick(state);

            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("the item counter")
    class ItemCounter {

        private final ICLogic chip = VariableChips.itemCounter();

        private SimpleChipState counting(String item, SimpleStockpile chest) {
            return chip("VAR200", "score", item)
                    .inputs(true)
                    .build()
                    .withStockpileAt(CHEST, chest);
        }

        @Test
        @DisplayName("adds what it counted to the variable")
        void addsWhatItCountedToTheVariable() {
            variables().define(score, "0");

            chip.trigger(counting("coal", SimpleStockpile.empty().with(COAL, 12)));

            assertThat(variables().get(score)).contains("12");
        }

        @Test
        @DisplayName("counts only what its sign names")
        void countsOnlyWhatItsSignNames() {
            variables().define(score, "0");

            chip.trigger(counting("coal",
                    SimpleStockpile.empty().with(COAL, 12).with(IRON, 5)));

            assertThat(variables().get(score)).contains("12");
        }

        @Test
        @DisplayName("counts everything when its sign names nothing")
        void countsEverythingWhenItsSignNamesNothing() {
            variables().define(score, "0");

            chip.trigger(counting("", SimpleStockpile.empty().with(COAL, 12).with(IRON, 5)));

            assertThat(variables().get(score)).contains("17");
        }

        @Test
        @DisplayName("adds to what the variable already held")
        void addsToWhatTheVariableAlreadyHeld() {
            variables().define(score, "100");

            chip.trigger(counting("coal", SimpleStockpile.empty().with(COAL, 12)));

            assertThat(variables().get(score)).contains("112");
        }

        @Test
        @DisplayName("goes high when it counted something")
        void goesHighWhenItCountedSomething() {
            variables().define(score, "0");
            SimpleChipState state = counting("coal", SimpleStockpile.empty().with(COAL, 1));

            chip.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        @DisplayName("goes low over an empty chest, and adds nothing")
        void goesLowOverAnEmptyChestAndAddsNothing() {
            variables().define(score, "7");
            SimpleChipState state = counting("coal", SimpleStockpile.empty());

            chip.trigger(state);

            assertThat(state.mainOutput()).isFalse();
            assertThat(variables().get(score)).contains("7");
        }

        @Test
        @DisplayName("still reports what it found when the variable has gone away")
        void stillReportsWhatItFoundWhenTheVariableHasGoneAway() {
            SimpleChipState state = counting("coal", SimpleStockpile.empty().with(COAL, 3));

            chip.trigger(state);

            assertThat(state.mainOutput()).isTrue();
        }
    }

    @Nested
    @DisplayName("reviewing a sign as it is written")
    class Reviewing {

        private final SimpleActor builder = SimpleActor.named("alice");

        private SignLines lines(String model, String variable, String argument) {
            return SignLines.of("", "[" + model + "]", variable, argument);
        }

        @Test
        @DisplayName("accepts a sign naming a variable that exists")
        void acceptsASignNamingAVariableThatExists() {
            variables().define(score, "0");

            assertThat(VariableChips.modifier()
                    .reviewSign(lines("VAR100", "score", "+:1"), services, builder))
                    .isEmpty();
        }

        @Test
        @DisplayName("refuses a sign naming a variable nobody has made")
        void refusesASignNamingAVariableNobodyHasMade() {
            assertThat(VariableChips.modifier()
                    .reviewSign(lines("VAR100", "score", "+:1"), services, builder))
                    .get()
                    .asString()
                    .contains("no variable called score");
        }

        @Test
        @DisplayName("refuses a sign whose sum is not a function and an amount")
        void refusesASignWhoseSumIsNotAFunctionAndAnAmount() {
            variables().define(score, "0");

            assertThat(VariableChips.modifier()
                    .reviewSign(lines("VAR100", "score", "nonsense"), services, builder))
                    .isPresent();
        }

        @Test
        @DisplayName("refuses a comparison whose number is not one")
        void refusesAComparisonWhoseNumberIsNotOne() {
            variables().define(score, "0");

            assertThat(VariableChips.isAtLeast()
                    .reviewSign(lines("VAR170", "score", "soon"), services, builder))
                    .isPresent();
        }

        @Test
        @DisplayName("accepts an item counter whose fourth line is blank")
        void acceptsAnItemCounterWhoseFourthLineIsBlank() {
            variables().define(score, "0");

            assertThat(VariableChips.itemCounter()
                    .reviewSign(lines("VAR200", "score", ""), services, builder))
                    .isEmpty();
        }

        @Test
        @DisplayName("lets a builder use their own namespace")
        void letsABuilderUseTheirOwnNamespace() {
            variables().define(new VariableName("alice", "score"), "0");

            assertThat(VariableChips.modifier()
                    .reviewSign(lines("VAR100", "alice|score", "+:1"), services, builder))
                    .isEmpty();
        }

        @Test
        @DisplayName("refuses somebody else's namespace without the permission for it")
        void refusesSomebodyElsesNamespaceWithoutThePermissionForIt() {
            variables().define(new VariableName("bob", "score"), "0");

            assertThat(VariableChips.modifier()
                    .reviewSign(lines("VAR100", "bob|score", "+:1"), services,
                            builder.allowedOnly()))
                    .get()
                    .asString()
                    .contains("belongs to bob");
        }

        @Test
        @DisplayName("allows somebody else's namespace with the permission for it")
        void allowsSomebodyElsesNamespaceWithThePermissionForIt() {
            variables().define(new VariableName("bob", "score"), "0");

            assertThat(VariableChips.modifier()
                    .reviewSign(lines("VAR100", "bob|score", "+:1"), services,
                            builder.allowedOnly(VariableChips.OTHER_NAMESPACE_PERMISSION)))
                    .isEmpty();
        }

        @Test
        @DisplayName("refuses a sign whose third line is not a name at all")
        void refusesASignWhoseThirdLineIsNotANameAtAll() {
            assertThat(VariableChips.itemCounter()
                    .reviewSign(lines("VAR200", "", ""), services, builder))
                    .isPresent();
        }
    }
}
