package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IC registry")
class ICRegistryTest {

    private ICRegistry registry;

    /** A chip that mirrors its input, standing in for any ordinary gate. */
    private static ICDefinition.Builder repeater() {
        return ICDefinition.builder("MC1000", "REPEATER")
                .name("Repeater")
                .description("Repeats a redstone signal.")
                .layout(PinLayout.AISO)
                .logic(() -> state -> state.setMainOutput(state.mainInput()));
    }

    /** A chip that only makes sense while ticking. */
    private static ICDefinition.Builder daySensor() {
        return ICDefinition.builder("MC1230", "SENSE DAY")
                .name("Daylight Sensor")
                .logic(AlwaysTickingLogic::new);
    }

    /** A chip that keeps a running total, standing in for any chip with private state. */
    private static final class CountingLogic implements ICLogic {
        private int count;

        @Override
        public void trigger(ChipState state) {
            count++;
        }
    }

    private static final class AlwaysTickingLogic implements SelfTriggeringICLogic {
        @Override
        public void trigger(ChipState state) {
            // Nothing to do; this chip only acts on a tick.
        }

        @Override
        public void tick(ChipState state) {
            state.setMainOutput(true);
        }

        @Override
        public boolean alwaysSelfTriggering() {
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new ICRegistry();
    }

    @Nested
    @DisplayName("resolution")
    class Resolution {

        @Test
        void resolvesAModelReference() {
            registry.register(repeater().build());

            ICRegistry.Resolution resolution = registry.resolve("[MC1000]").orElseThrow();

            assertThat(resolution.definition().name()).isEqualTo("Repeater");
            assertThat(resolution.selfTriggering()).isFalse();
        }

        @Test
        void resolvesAShorthandReference() {
            registry.register(repeater().build());

            assertThat(registry.resolve("=REPEATER").orElseThrow().definition().model())
                    .isEqualTo("MC1000");
        }

        @Test
        void honoursTheSelfTriggeringSuffix() {
            registry.register(repeater().build());

            assertThat(registry.resolve("[MC1000]S").orElseThrow().selfTriggering()).isTrue();
            assertThat(registry.resolve("=REPEATER ST").orElseThrow().selfTriggering()).isTrue();
        }

        @Test
        void ticksAChipThatInsistsOnItEvenWithoutTheSuffix() {
            registry.register(daySensor().build());

            assertThat(registry.resolve("[MC1230]").orElseThrow().selfTriggering()).isTrue();
        }

        @Test
        void returnsEmptyForAnUnknownChip() {
            registry.register(repeater().build());

            assertThat(registry.resolve("[MC9999]")).isEmpty();
            assertThat(registry.resolve("=NOPE")).isEmpty();
        }

        @Test
        void returnsEmptyForTextThatIsNotAnIcAtAll() {
            registry.register(repeater().build());

            assertThat(registry.resolve("Welcome home")).isEmpty();
        }
    }

    @Nested
    @DisplayName("merged chips")
    class MergedChips {

        @Test
        void resolvesARetiredModelNumberToTheChipThatReplacedIt() {
            // When two chips are merged the surviving number keeps its own identity while the
            // retired one still resolves, so signs already in the world keep working.
            registry.register(ICDefinition.builder("MCX200", "SPAWNER")
                    .name("Entity Spawner")
                    .aliases("MC1200")
                    .logic(() -> state -> {})
                    .build());

            assertThat(registry.resolve("[MC1200]").orElseThrow().definition().model())
                    .isEqualTo("MCX200");
            assertThat(registry.resolve("[MCX200]").orElseThrow().definition().model())
                    .isEqualTo("MCX200");
        }

        @Test
        void treatsASeparateSelfTriggeringNumberAsTheTickingVariant() {
            registry.register(repeater().selfTriggeringModel("MC1001").build());

            ICRegistry.Resolution plain = registry.resolve("[MC1000]").orElseThrow();
            ICRegistry.Resolution ticking = registry.resolve("[MC1001]").orElseThrow();

            assertThat(plain.selfTriggering()).isFalse();
            assertThat(ticking.selfTriggering()).isTrue();
            assertThat(ticking.definition()).isSameAs(plain.definition());
        }
    }

    @Nested
    @DisplayName("registration")
    class Registration {

        @Test
        void rejectsTwoChipsClaimingTheSameModelNumber() {
            registry.register(repeater().build());

            assertThatThrownBy(() -> registry.register(
                            ICDefinition.builder("MC1000", "OTHER").logic(() -> state -> {}).build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MC1000");
        }

        @Test
        void rejectsTwoChipsClaimingTheSameShorthand() {
            registry.register(repeater().build());

            assertThatThrownBy(() -> registry.register(
                            ICDefinition.builder("MC9999", "REPEATER").logic(() -> state -> {}).build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REPEATER");
        }

        @Test
        void rejectsAnAliasThatCollidesWithAnotherChip() {
            registry.register(repeater().build());

            assertThatThrownBy(() -> registry.register(
                            ICDefinition.builder("MC9999", "OTHER")
                                    .aliases("MC1000")
                                    .logic(() -> state -> {})
                                    .build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MC1000");
        }

        @Test
        void countsEachChipOnceRegardlessOfHowManyNumbersItAnswersTo() {
            registry.register(repeater().aliases("MC0111").selfTriggeringModel("MC1001").build());

            assertThat(registry.size()).isEqualTo(1);
            assertThat(registry.definitions()).hasSize(1);
        }

        @Test
        void listsChipsInModelOrder() {
            registry.register(daySensor().build());
            registry.register(repeater().build());

            assertThat(registry.definitions())
                    .extracting(ICDefinition::model)
                    .containsExactly("MC1000", "MC1230");
        }
    }

    @Nested
    @DisplayName("lookup")
    class Lookup {

        @BeforeEach
        void register() {
            registry.register(repeater().aliases("MC0111").build());
        }

        @Test
        void findsAChipByAnyOfItsNumbers() {
            assertThat(registry.byModel("MC1000")).isPresent();
            assertThat(registry.byModel("MC0111")).isPresent();
        }

        @Test
        void isCaseAndWhitespaceInsensitive() {
            assertThat(registry.byModel(" mc1000 ")).isPresent();
            assertThat(registry.byShorthand(" repeater ")).isPresent();
        }
    }

    @Nested
    @DisplayName("definitions")
    class Definitions {

        @Test
        void derivesTheSafePermissionFromTheModelNumber() {
            assertThat(repeater().build().permission()).isEqualTo("craftbook.ic.safe.mc1000");
        }

        @Test
        void derivesTheRestrictedPermissionForRestrictedChips() {
            assertThat(repeater().restricted().build().permission())
                    .isEqualTo("craftbook.ic.restricted.mc1000");
        }

        @Test
        void reportsWhetherAChipCanTick() {
            assertThat(repeater().build().supportsSelfTriggering()).isFalse();
            assertThat(daySensor().build().supportsSelfTriggering()).isTrue();
        }

        @Test
        void asksTheFactoryForLogicEveryTime() {
            // A stateless chip may hand back a shared instance, and the JVM will do exactly that
            // for a non-capturing lambda. What has to hold is that the factory is consulted per
            // chip, so a stateful chip can hand out fresh state.
            int[] built = {0};
            ICDefinition definition = ICDefinition.builder("MC1000", "REPEATER")
                    .logic(() -> {
                        built[0]++;
                        return state -> {};
                    })
                    .build();

            definition.newLogic();
            definition.newLogic();

            assertThat(built[0]).isEqualTo(2);
        }

        @Test
        void givesEachStatefulChipItsOwnState() {
            // Two signs of the same counting chip must not share a running total.
            ICDefinition definition = ICDefinition.builder("MC3102", "COUNTER")
                    .logic(CountingLogic::new)
                    .build();

            ICLogic first = definition.newLogic();
            ICLogic second = definition.newLogic();
            SimpleChipState state = SimpleChipState.of(1, 1).build();

            first.trigger(state);
            first.trigger(state);
            second.trigger(state);

            assertThat(((CountingLogic) first).count).isEqualTo(2);
            assertThat(((CountingLogic) second).count).isEqualTo(1);
        }

        @Test
        void fallsBackToTheShorthandWhenNoNameIsGiven() {
            ICDefinition definition =
                    ICDefinition.builder("MC1000", "REPEATER").logic(() -> state -> {}).build();

            assertThat(definition.name()).isEqualTo("REPEATER");
        }

        @Test
        void rendersItsCanonicalSignText() {
            assertThat(repeater().build().modelReference()).isEqualTo("[MC1000]");
        }

        @Test
        void rewritesAShorthandSignToNameTheChipByNumber() {
            ICLine written = ICLine.parse("=repeater").orElseThrow();

            assertThat(repeater().build().canonicalLine(written, false).render()).isEqualTo("[MC1000]");
        }

        @Test
        void carriesTheModeStringThroughUntouched() {
            ICLine written = ICLine.parse("[mc1000]p").orElseThrow();

            assertThat(repeater().build().canonicalLine(written, false).render()).isEqualTo("[MC1000]p");
        }

        @Test
        void recordsTheSelfTriggeringChoiceOnTheSign() {
            ICLine written = ICLine.parse("=REPEATER ST").orElseThrow();

            assertThat(repeater().build().canonicalLine(written, true).render()).isEqualTo("[MC1000]S");
        }

        @Test
        void marksARestrictedChipAsVetted() {
            ICLine written = ICLine.parse("[MC1000]").orElseThrow();

            assertThat(repeater().restricted().build().canonicalLine(written, false).render())
                    .isEqualTo("[MC1000]*");
        }

        @Test
        void producesALineThatParsesBackToTheSameChip() {
            ICRegistry registry = new ICRegistry().register(repeater().build());
            ICLine written = ICLine.parse("=repeater st").orElseThrow();

            String canonical = repeater().build().canonicalLine(written, true).render();

            ICRegistry.Resolution resolved = registry.resolve(canonical).orElseThrow();
            assertThat(resolved.definition().model()).isEqualTo("MC1000");
            assertThat(resolved.selfTriggering()).isTrue();
        }

        @Test
        void rejectsAModelNumberThatTheSignGrammarCouldNotCarry() {
            assertThatThrownBy(() -> ICDefinition.builder("MC 1000", "X").logic(() -> state -> {}).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsAChipAliasingItself() {
            assertThatThrownBy(() -> repeater().aliases("MC1000").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("its own model number");
        }
    }
}
