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
        "[MCX207]", "[MCX208]", "[MCX209]", "[MCX210]", "[MCX206]",
        "[MC1205]", "[MC1206]", "[MC1207]",
        "[MC1110]", "[MC1111]", "[MC0111]", "[MC6543]", "[MCX112]", "[MCU113]",
        "[MCX211]", "[MC1249]", "[MCX213]", "[MCX215]", "[MCZ215]", "[MCX216]", "[MCZ216]",
        "[MCX120]", "[MCZ120]", "[MCX121]", "[MCZ121]", "[MC2022]", "[MCU440]", "[MCX295]", "[MCZ295]",
        "[MCX200]", "[MC1200]", "[MCX201]", "[MC1201]", "[MCX202]", "[MC1202]", "[MCX203]", "[MCZ203]",
        "[MC1240]", "[MC1241]", "[MCX242]", "[MCX243]", "[MCX244]", "[MCX245]", "[MCX246]",
        "[MCX255]", "[MC1203]", "[MCX256]", "[MCZ256]",
        "[MCX130]", "[MCZ130]", "[MCX131]", "[MCU131]", "[MCX132]", "[MCU132]",
        "[MCX146]", "[MCU146]", "[MCX250]", "[MC1250]", "[MC1253]",
        "[MCM116]", "[MCO116]", "[MCX116]", "[MCZ116]", "[MCX117]", "[MCZ117]",
        "[MCX118]", "[MCZ118]", "[MCX119]", "[MCZ119]",
        "[MCX133]", "[MCZ133]", "[MCX138]", "[MCZ138]", "[MCX139]", "[MCZ139]",
        "[MCX140]", "[MCU140]", "[MC1500]", "[MC0500]",
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
        "=BRIDGE", "=DOOR", "=BRIDGE+", "=DOOR+", "=FLEX SET", "=FLEX SET ADMIN",
        "=SET ABOVE", "=SET BELOW",
        "=TRANSMITTER", "=RECEIVER", "=REDCODER", "=TRANSPORTER", "=DESTINATION",
        "=TOGGLE BLOCK", "=BLOCK REPLACER", "=HARVESTER", "=AREA PLANTER", "=PLANTER",
        "=COMMAND CTRL", "=PASSWORD CTRL", "=BITSHIFT", "=^MONOFLOP", "=TRIGGER READER",
        "=ENTITY SPAWNER", "=ITEM SPAWNER", "=CHEST DISPENSER", "=CHEST COLLECTOR",
        "=ARROW SHOOTER", "=ARROW BARRAGE", "=SNOW SHOOTER", "=SNOW BARRAGE",
        "=EGG SHOOTER", "=EGG BARRAGE", "=FIREBALL",
        "=LIGHTNING", "=ZEUS BOLT", "=HOLY SMITE",
        "=MOB ZAPPER", "=HIT PLAYER ABV", "=HIT MOB ABOVE",
        "=POTION AREA", "=PARTICLE", "=FIREWORKS", "=FIREWORK",
        "=MOB ABOVE?", "=PLAYER ABOVE?", "=PLAYER BELOW?", "=PLAYER NEAR?", "=MOB NEAR?",
        "=HUMANS ONLY", "=ITEM NEAR?", "=HELD ITEM NEAR?", "=IN AREA", "=PLAYER ONLINE?",
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
        "MC1110, AIZO",
        "MC6543, AISO",
        "MCX131, UISO",
        "MCX130, SISO",
        "MCX133, SISO",
        "MCX140, UISO",
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
        assertThat(REGISTRY.resolve("[MC0111]").orElseThrow().selfTriggering()).isTrue();
        assertThat(REGISTRY.resolve("[MC1111]").orElseThrow().selfTriggering()).isFalse();
        assertThat(REGISTRY.resolve("[MCZ215]").orElseThrow().selfTriggering()).isTrue();
        assertThat(REGISTRY.resolve("[MCZ216]").orElseThrow().selfTriggering()).isTrue();
        assertThat(REGISTRY.resolve("[MCZ120]").orElseThrow().selfTriggering()).isTrue();
        assertThat(REGISTRY.resolve("[MCZ295]").orElseThrow().selfTriggering()).isTrue();
        assertThat(REGISTRY.resolve("[MC1420]").orElseThrow().definition().model())
                .isEqualTo("MC1420");
    }

    @Test
    void ticksTheChipsThatHaveNothingToReactTo() {
        assertThat(REGISTRY.byModel("MC1420").orElseThrow().supportsSelfTriggering()).isTrue();
        assertThat(REGISTRY.byModel("MC1230").orElseThrow().supportsSelfTriggering()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"MCX233", "MCT233", "MC3231"})
    void restrictsTheChipsThatChangeTheWholeWorld(String model) {
        // Weather and time affect every player in the world, not just what is near the sign.
        assertThat(REGISTRY.byModel(model).orElseThrow().restricted()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"MC1000", "MC3002", "MC4000", "MC1260", "MC1420"})
    void leavesTheChipsThatOnlyMoveRedstoneUnrestricted(String model) {
        assertThat(REGISTRY.byModel(model).orElseThrow().restricted()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"MCX207", "MCX208", "MCX213"})
    void marksTheBuildingChipsAsNeedingAuthorisation(String model) {
        // These are created unauthorised so they cannot be dropped over someone's structure and
        // used to take it apart.
        assertThat(REGISTRY.byModel(model).orElseThrow().requiresAuthorisation()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"MCX209", "MCX210"})
    void restrictsTheForcingVariantsInstead(String model) {
        // A forcing chip skips the authorisation check, so permission is the only thing left
        // standing between it and someone else's blocks.
        assertThat(REGISTRY.byModel(model).orElseThrow().restricted()).isTrue();
        assertThat(REGISTRY.byModel(model).orElseThrow().requiresAuthorisation()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"MCX112", "MCU113"})
    void restrictsTheChipsThatMovePeopleAround(String model) {
        // A transporter can drop somebody anywhere its far end happens to be.
        assertThat(REGISTRY.byModel(model).orElseThrow().restricted()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"MC1110", "MC1111", "MC6543"})
    void letsTheBandChipsNameTheirOwnerAsTheirNamespace(String model) {
        assertThat(REGISTRY.byModel(model).orElseThrow().playerIdentityLine()).hasValue(3);
    }

    @Test
    void keepsTheTwoCommandControlledChipsApart() {
        // They are the same chip in every respect a builder can see, but they read different
        // switchboards, so one cannot be used to throw the other's switches.
        assertThat(REGISTRY.byModel("MCX120").orElseThrow().newLogic())
                .isNotEqualTo(REGISTRY.byModel("MCX121").orElseThrow().newLogic());
    }

    @Test
    void keepsTheTwoFlexSettersApart() {
        // One pays for its block out of a nearby chest; the other conjures it and so is
        // restricted instead.
        assertThat(REGISTRY.byModel("MCX206").orElseThrow().restricted()).isFalse();
        assertThat(REGISTRY.byModel("MC1207").orElseThrow().restricted()).isTrue();
        assertThat(REGISTRY.byModel("MCX206").orElseThrow().newLogic())
                .isNotEqualTo(REGISTRY.byModel("MC1207").orElseThrow().newLogic());
    }

    @Test
    void keepsTheWeatherControlShorthandOnTheSimplerChip() {
        // Both weather chips were catalogued under the same shorthand, so only one can keep it.
        assertThat(REGISTRY.byShorthand("WEATHER CONTROL").orElseThrow().model()).isEqualTo("MCX233");
        assertThat(REGISTRY.byModel("MCT233")).isPresent();
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
