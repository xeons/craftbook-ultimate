package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("IC identifier line")
class ICLineTest {

    @Nested
    @DisplayName("model references")
    class ModelReferences {

        @Test
        void parsesAPlainModel() {
            ICLine line = ICLine.parse("[MC1000]").orElseThrow();

            assertThat(line.kind()).isEqualTo(ICLine.Kind.MODEL);
            assertThat(line.identifier()).isEqualTo("MC1000");
            assertThat(line.selfTriggering()).isFalse();
            assertThat(line.awaitingAuthorisation()).isFalse();
            assertThat(line.mode()).isEmpty();
        }

        @Test
        void treatsTheSSuffixAsSelfTriggering() {
            ICLine line = ICLine.parse("[MC1000]S").orElseThrow();

            assertThat(line.identifier()).isEqualTo("MC1000");
            assertThat(line.selfTriggering()).isTrue();
            assertThat(line.mode()).isEmpty();
        }

        @Test
        void treatsTheStarSuffixAsTheAuthorisationMarker() {
            ICLine line = ICLine.parse("[MCX131]*").orElseThrow();

            assertThat(line.awaitingAuthorisation()).isTrue();
            assertThat(line.selfTriggering()).isFalse();
        }

        @Test
        void findsBothFlagsRegardlessOfOrder() {
            // Both flags are recognised wherever they appear, so neither marker can hide the other.
            assertThat(ICLine.parse("[MCX131]S*").orElseThrow().selfTriggering()).isTrue();
            assertThat(ICLine.parse("[MCX131]*S").orElseThrow().selfTriggering()).isTrue();
            assertThat(ICLine.parse("[MCX131]S*").orElseThrow().awaitingAuthorisation()).isTrue();
            assertThat(ICLine.parse("[MCX131]*S").orElseThrow().awaitingAuthorisation()).isTrue();
        }

        @Test
        void keepsTheRemainingSuffixAsTheModeString() {
            ICLine line = ICLine.parse("[MC1000]!").orElseThrow();

            assertThat(line.mode()).isEqualTo("!");
            assertThat(line.hasMode()).isTrue();
        }

        @Test
        void separatesFlagsFromTheModeString() {
            ICLine line = ICLine.parse("[MC1000]S!").orElseThrow();

            assertThat(line.selfTriggering()).isTrue();
            assertThat(line.mode()).isEqualTo("!");
        }

        @Test
        void preservesTheCaseOfTheModeString() {
            // Lower case p and upper case P select two different teleport pad modes, so the
            // suffix must not be upper-cased along with the model number.
            assertThat(ICLine.parse("[MC1000]p").orElseThrow().mode()).isEqualTo("p");
            assertThat(ICLine.parse("[MC1000]P").orElseThrow().mode()).isEqualTo("P");
        }

        @Test
        void acceptsTheExtendedModelSeries() {
            assertThat(ICLine.parse("[MCX112]").orElseThrow().identifier()).isEqualTo("MCX112");
            assertThat(ICLine.parse("[MCU113]").orElseThrow().identifier()).isEqualTo("MCU113");
            assertThat(ICLine.parse("[MCM116]").orElseThrow().identifier()).isEqualTo("MCM116");
        }
    }

    @Nested
    @DisplayName("shorthand references")
    class ShorthandReferences {

        @Test
        void parsesAPlainShorthand() {
            ICLine line = ICLine.parse("=REPEATER").orElseThrow();

            assertThat(line.kind()).isEqualTo(ICLine.Kind.SHORTHAND);
            assertThat(line.identifier()).isEqualTo("REPEATER");
            assertThat(line.selfTriggering()).isFalse();
        }

        @Test
        void preservesInternalSpaces() {
            assertThat(ICLine.parse("=RE T FLIP").orElseThrow().identifier()).isEqualTo("RE T FLIP");
        }

        @Test
        void treatsATrailingStMarkerAsSelfTriggering() {
            ICLine line = ICLine.parse("=REPEATER ST").orElseThrow();

            assertThat(line.identifier()).isEqualTo("REPEATER");
            assertThat(line.selfTriggering()).isTrue();
        }

        @Test
        void separatesTheStMarkerFromAMultiWordShorthand() {
            ICLine line = ICLine.parse("=RE T FLIP ST").orElseThrow();

            assertThat(line.identifier()).isEqualTo("RE T FLIP");
            assertThat(line.selfTriggering()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"=PLAYER ONLINE?", "=BRIDGE+", "=RS-NOR", "=SERVER LOG NEARBY+"})
        void acceptsTheFullShorthandAlphabet(String raw) {
            assertThat(ICLine.parse(raw)).isPresent();
        }
    }

    @Nested
    @DisplayName("normalisation")
    class Normalisation {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"[mc1000]", "  [MC1000]  ", "[Mc1000]"})
        void isCaseAndWhitespaceInsensitiveForTheModel(String raw) {
            assertThat(ICLine.parse(raw).orElseThrow().identifier()).isEqualTo("MC1000");
        }

        @Test
        void upperCasesShorthandsAndTheirMarker() {
            ICLine line = ICLine.parse("=repeater st").orElseThrow();

            assertThat(line.identifier()).isEqualTo("REPEATER");
            assertThat(line.selfTriggering()).isTrue();
        }
    }

    @Nested
    @DisplayName("rejection")
    class Rejection {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {
            "",
            "   ",
            "MC1000",   // missing brackets entirely
            "[MC1000",  // unterminated
            "MC1000]",  // unopened
            "[]",       // empty model
            "Welcome home",
            "=",
        })
        void rejectsMalformedLines(String raw) {
            assertThat(ICLine.parse(raw)).isEmpty();
        }

        @Test
        void rejectsAnEmbeddedModelReference() {
            // A model reference is only an IC when it is the whole line, so ordinary sign text
            // that happens to mention one stays ordinary sign text.
            assertThat(ICLine.parse("look at [MC1000]")).isEmpty();
        }
    }

    @Nested
    @DisplayName("rendering")
    class Rendering {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
            "[MC1000]", "[MC1000]S", "[MCX131]*", "[MCX131]S*", "[MC1000]!", "[MC1000]S!",
            "=REPEATER", "=REPEATER ST", "=RE T FLIP", "=RE T FLIP ST"
        })
        void roundTrips(String raw) {
            ICLine line = ICLine.parse(raw).orElseThrow();

            assertThat(line.render()).isEqualTo(raw);
            assertThat(ICLine.parse(line.render())).contains(line);
        }

        @Test
        void normalisesFlagOrderWhenRendering() {
            // Both spellings mean the same thing, so both render to the canonical order.
            assertThat(ICLine.parse("[MCX131]*S").orElseThrow().render()).isEqualTo("[MCX131]S*");
        }

        @Test
        void applyingTheAuthorisationMarkerIsIdempotent() {
            ICLine line = ICLine.parse("[MCX131]").orElseThrow();

            assertThat(line.withAwaitingAuthorisation().render()).isEqualTo("[MCX131]*");
            assertThat(line.withAwaitingAuthorisation().withAwaitingAuthorisation().render()).isEqualTo("[MCX131]*");
        }

        @Test
        void applyingTheSelfTriggeringMarkerKeepsTheModeString() {
            ICLine line = ICLine.parse("[MC1000]!").orElseThrow();

            assertThat(line.withSelfTriggering().render()).isEqualTo("[MC1000]S!");
        }
    }
}
