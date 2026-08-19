package com.xeonproductions.craftbookultimate.core.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Block references on signs")
class BlockReferenceTest {

    @Nested
    @DisplayName("modern names")
    class ModernNames {

        @Test
        void readsABareName() {
            BlockReference reference = BlockReference.parse("red_wool").orElseThrow();

            assertThat(reference.name()).isEqualTo("red_wool");
            assertThat(reference.damage()).isZero();
            assertThat(reference.isNumericId()).isFalse();
            assertThat(reference.isLegacy()).isFalse();
        }

        @Test
        void readsAQualifiedName() {
            // The colon separates a namespace here, not a damage value.
            BlockReference reference = BlockReference.parse("minecraft:red_wool").orElseThrow();

            assertThat(reference.name()).isEqualTo("minecraft:red_wool");
            assertThat(reference.isLegacy()).isFalse();
            assertThat(reference.asKey().orElseThrow())
                    .isEqualTo(Blocks.key("red_wool"));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"Red Wool", "RED_WOOL", "  red_wool  "})
        void isForgivingAboutHowItIsWritten(String written) {
            assertThat(BlockReference.parse(written).orElseThrow().name()).isEqualTo("red_wool");
        }
    }

    @Nested
    @DisplayName("pre-flattening references")
    class PreFlattening {

        @Test
        void readsANumericIdAndDamageValue() {
            // Red wool, on a sign written before the flattening.
            BlockReference reference = BlockReference.parse("35:14").orElseThrow();

            assertThat(reference.isNumericId()).isTrue();
            assertThat(reference.id()).contains(35);
            assertThat(reference.damage()).isEqualTo(14);
            assertThat(reference.isLegacy()).isTrue();
        }

        @Test
        void readsABareNumericId() {
            BlockReference reference = BlockReference.parse("35").orElseThrow();

            assertThat(reference.id()).contains(35);
            assertThat(reference.damage()).isZero();
            assertThat(reference.isLegacy()).isTrue();
        }

        @Test
        void readsALegacyNameWithADamageValue() {
            BlockReference reference = BlockReference.parse("wool:14").orElseThrow();

            assertThat(reference.isNumericId()).isFalse();
            assertThat(reference.name()).isEqualTo("wool");
            assertThat(reference.damage()).isEqualTo(14);
            assertThat(reference.isLegacy()).isTrue();
        }

        @Test
        void hasNoIdForANamedBlock() {
            assertThat(BlockReference.parse("wool:14").orElseThrow().id()).isEmpty();
        }

        @Test
        void readsTheAtSpelling() {
            // The chips that set a single block write the damage value after an at sign.
            BlockReference reference = BlockReference.parse("35@14").orElseThrow();

            assertThat(reference.isNumericId()).isTrue();
            assertThat(reference.id()).contains(35);
            assertThat(reference.damage()).isEqualTo(14);
        }

        @Test
        void readsANameWithAnAtDamageValue() {
            BlockReference reference = BlockReference.parse("wool@14").orElseThrow();

            assertThat(reference.name()).isEqualTo("wool");
            assertThat(reference.damage()).isEqualTo(14);
            assertThat(reference.isNumericId()).isFalse();
        }

        @Test
        void readsAQualifiedNameWithAnAtDamageValue() {
            BlockReference reference = BlockReference.parse("minecraft:wool@14").orElseThrow();

            assertThat(reference.name()).isEqualTo("minecraft:wool");
            assertThat(reference.damage()).isEqualTo(14);
        }

        @Test
        void hasNoModernKeyForANumericId() {
            // A numeric id means nothing without the server's flattening tables.
            assertThat(BlockReference.parse("35:14").orElseThrow().asKey()).isEmpty();
        }
    }

    @Nested
    @DisplayName("telling the two apart")
    class TellingThemApart {

        @ParameterizedTest(name = "\"{0}\" -> name {1}, damage {2}, numeric {3}")
        @CsvSource({
            "'stone',            stone,           0,  false",
            "'minecraft:stone',  minecraft:stone, 0,  false",
            "'35',               35,              0,  true",
            "'35:14',            35,              14, true",
            "'wool:14',          wool,            14, false",
            "'minecraft:wool:14', minecraft:wool, 14, false",
        })
        void readsEachSpellingCorrectly(String written, String name, int damage, boolean numeric) {
            BlockReference reference = BlockReference.parse(written).orElseThrow();

            assertThat(reference.name()).isEqualTo(name);
            assertThat(reference.damage()).isEqualTo(damage);
            assertThat(reference.isNumericId()).isEqualTo(numeric);
        }

        @Test
        void aTrailingNumberIsADamageValueNotAPath() {
            assertThat(BlockReference.parse("wool:14").orElseThrow().damage()).isEqualTo(14);
        }

        @Test
        void aTrailingWordIsAPathNotADamageValue() {
            assertThat(BlockReference.parse("minecraft:wool").orElseThrow().damage()).isZero();
        }
    }

    @Nested
    @DisplayName("rejection")
    class Rejection {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "   ", ":", ":14", "@14", "wool@x"})
        void rejectsTextThatNamesNothing(String written) {
            assertThat(BlockReference.parse(written)).isEmpty();
        }
    }

    @Test
    void rendersBackToHowItWasWritten() {
        assertThat(BlockReference.parse("35:14").orElseThrow()).hasToString("35:14");
        assertThat(BlockReference.parse("red_wool").orElseThrow()).hasToString("red_wool");
    }
}
