package com.xeonproductions.craftbookultimate.paper;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("IC catalogue")
class ICCatalogueTest {

    private static final ICRegistry REGISTRY = ICCatalogue.build();

    @Test
    void buildsWithoutAnyClashingNumbersOrShorthands() {
        // The registry rejects a duplicate at registration time, so simply building the
        // catalogue proves every entry is distinct.
        assertThat(REGISTRY.size()).isPositive();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "[MC1000]", "[MC1001]",
        "[MC3002]", "[MC3003]", "[MC3020]", "[MC3021]",
        "[MC3030]", "[MC3031]", "[MC3032]", "[MC3033]", "[MC3034]", "[MC3036]",
        "[MC1017]", "[MC1018]", "[MC3050]", "[MC3101]", "[MC3102]",
        "[MC4000]", "[MC4010]", "[MC4100]", "[MC4110]",
        "[MC4200]", "[MC3040]", "[MC4040]",
        "[MC1020]", "[MC2020]", "[MC6020]",
        "[MC1420]", "[MC1230]", "[MCX027]", "[MC1025]", "[MC1026]", "[MCX010]", "[MCX011]",
        "[MC1260]", "[MC1261]", "[MC1262]", "[MCX230]", "[MCX231]", "[MCX205]",
    })
    void resolvesEveryRegisteredModelNumber(String signLine) {
        assertThat(REGISTRY.resolve(signLine)).isPresent();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "=REPEATER", "=INVERTER", "=AND", "=NAND", "=XOR", "=XNOR",
        "=RS-NOR", "=RS-NAND", "=JK FLIP", "=EDGE-D", "=LEVEL-D",
        "=RE T FLIP", "=FE T FLIP", "=COMBO", "=COUNTER", "=DOWN COUNTER",
        "=FULL ADDER", "=HALF ADDER", "=FULL SUBTR", "=HALF SUBTR",
        "=DISPATCH", "=MULTIPLEXER", "=DEMULTIPLEXER",
        "=RANDOM BIT", "=RANDOM 3", "=RANDOM 5",
        "=CLOCK", "=SENSE DAY", "=BETWEEN TIME", "=TIME MODULUS", "=UNIX TIME",
        "=PULSE", "=SIGNAL EXTENDER",
        "=SENSE WATER", "=SENSE LAVA", "=SENSE LIGHT", "=IS IT RAIN", "=IS IT A STORM",
        "=DETECT BLOCK",
    })
    void resolvesEveryRegisteredShorthand(String signLine) {
        assertThat(REGISTRY.resolve(signLine)).isPresent();
    }

    @ParameterizedTest(name = "{0} uses {1}")
    @CsvSource({
        "MC1000, AISO",
        "MC3002, 3ISO",
        "MC4000, 3I3O",
        "MC4040, 3I5O",
        "MC2020, SI3O",
        "MC6020, SI5O",
    })
    void wiresEachChipForItsDocumentedLayout(String model, String layoutCode) {
        ICDefinition definition = REGISTRY.byModel(model).orElseThrow();

        assertThat(definition.defaultLayout()).isEqualTo(PinLayout.byCode(layoutCode).orElseThrow());
    }

    @Test
    void givesTheTwoNandLatchesTheSameBehaviour() {
        ICDefinition plain = REGISTRY.byModel("MC3033").orElseThrow();
        ICDefinition inverse = REGISTRY.byModel("MC3031").orElseThrow();

        assertThat(plain.newLogic()).hasSameClassAs(inverse.newLogic());
    }

    @Test
    void resolvesTheSeparateSelfTriggeringNumbers() {
        // A few chips were catalogued twice, once ticking and once not.
        assertThat(REGISTRY.resolve("[MC0420]").orElseThrow().selfTriggering()).isTrue();
        assertThat(REGISTRY.resolve("[MC0230]").orElseThrow().selfTriggering()).isTrue();
        assertThat(REGISTRY.resolve("[MC1420]").orElseThrow().definition().model())
                .isEqualTo("MC1420");
    }

    @Test
    void ticksTheChipsThatHaveNothingToReactTo() {
        assertThat(REGISTRY.byModel("MC1420").orElseThrow().supportsSelfTriggering()).isTrue();
        assertThat(REGISTRY.byModel("MC1230").orElseThrow().supportsSelfTriggering()).isTrue();
    }

    @Test
    void marksNothingAsRestrictedYet() {
        // Every chip registered so far only moves redstone about, so none of them needs
        // elevated permission to build.
        assertThat(REGISTRY.definitions()).allSatisfy(definition ->
                assertThat(definition.restricted()).isFalse());
    }

    @Test
    void givesEveryChipADescription() {
        assertThat(REGISTRY.definitions()).allSatisfy(definition -> {
            assertThat(definition.name()).isNotBlank();
            assertThat(definition.description()).isNotBlank();
        });
    }

    @Test
    void derivesADistinctPermissionForEveryChip() {
        assertThat(REGISTRY.definitions())
                .extracting(ICDefinition::permission)
                .doesNotHaveDuplicates();
    }
}
