// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.entity.ItemCriteria;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("What a sign line will take")
class LineFormTest {

    private static final LineContext ANY_SERVER = LineContext.lenient();

    private static Optional<String> fault(LineForm form, String written) {
        return form.fault(written, ANY_SERVER);
    }

    @Nested
    @DisplayName("every form in the library")
    class EveryForm {

        private static List<LineForm> all() {
            return List.of(
                    LineForms.itemFilter(),
                    LineForms.entity(),
                    LineForms.oneEntity(),
                    LineForms.block(),
                    LineForms.variable(),
                    LineForms.band(),
                    LineForms.blockPair('|'),
                    LineForms.offset(),
                    LineForms.offsetAndBlock(),
                    LineForms.wholeNumber(1, 30),
                    LineForms.number(0, 10),
                    LineForms.measurements("width", "length"),
                    LineForms.oneOf("h"),
                    LineForms.either(LineForms.itemFilter(), LineForms.offset()));
        }

        @Test
        @DisplayName("reads its own example, so what it promises is what it accepts")
        void readsItsOwnExample() {
            for (LineForm form : all()) {
                String example = form.example().orElseThrow();

                assertThat(fault(form, example))
                        .as("%s rejected its own example %s", form.accepted(), example)
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("says what it takes, so a refusal has something to quote")
        void saysWhatItTakes() {
            for (LineForm form : all()) {
                assertThat(form.accepted())
                        .as("%s", form.example())
                        .isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("a line that takes anything")
    class Free {

        @Test
        @DisplayName("never faults, whatever is written on it")
        void neverFaults() {
            assertThat(fault(LineForm.free(), "anything at all")).isEmpty();
        }

        @Test
        @DisplayName("says it takes nothing in particular, so nothing is printed for it")
        void saysNothingInParticular() {
            assertThat(LineForm.free().checksAnything()).isFalse();
        }
    }

    @Nested
    @DisplayName("the item filter the item sensors read")
    class TheItemFilter {

        private final LineForm form = LineForms.itemFilter();

        @Test
        @DisplayName("takes every check the parser has")
        void takesEveryCheckTheParserHas() {
            assertThat(fault(form, "ID:stone")).isEmpty();
            assertThat(fault(form, "STACK:64")).isEmpty();
            assertThat(fault(form, "NAME:Key")).isEmpty();
            assertThat(fault(form, "LORE:quest")).isEmpty();
        }

        @Test
        @DisplayName("refuses a check nobody has heard of, and says what it was")
        void refusesACheckNobodyHasHeardOf() {
            assertThat(fault(form, "item:stone"))
                    .isPresent()
                    .get(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                    .contains("item:stone");
        }

        @Test
        @DisplayName("refuses a bare block name, which is the other easy mistake")
        void refusesABareBlockName() {
            assertThat(fault(form, "stone")).isPresent();
        }

        @Test
        @DisplayName("lists exactly the checks the parser declares")
        void listsExactlyWhatTheParserDeclares() {
            assertThat(form.accepted()).isEqualTo(ItemCriteria.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("the description the creature sensors read")
    class TheEntityDescription {

        private final LineForm form = LineForms.entity();

        @Test
        @DisplayName("takes item:stone, which the item filter does not")
        void takesItemStone() {
            assertThat(fault(form, "item:stone")).isEmpty();
        }

        @Test
        @DisplayName("takes a rider on top of something")
        void takesARider() {
            assertThat(fault(form, "pig+player")).isEmpty();
        }

        @Test
        @DisplayName("refuses a creature the game does not have")
        void refusesACreatureTheGameDoesNotHave() {
            assertThat(fault(form, "p:")).isPresent();
        }

        @Test
        @DisplayName("does not offer the item checks, which belong to the other grammar")
        void doesNotOfferTheItemChecks() {
            assertThat(form.accepted()).doesNotContain(ItemCriteria.ACCEPTED.get(1));
            assertThat(form.accepted()).isEqualTo(EntitySpec.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("a line that may be written either of two ways")
    class Either {

        private final LineForm form = LineForms.either(LineForms.itemFilter(), LineForms.offset());

        @Test
        @DisplayName("takes both of them")
        void takesBothOfThem() {
            assertThat(fault(form, "ID:stone")).isEmpty();
            assertThat(fault(form, "0:1:0")).isEmpty();
        }

        @Test
        @DisplayName("refuses what neither of them takes")
        void refusesWhatNeitherTakes() {
            assertThat(fault(form, "item:stone")).isPresent();
        }

        @Test
        @DisplayName("offers what both of them take")
        void offersWhatBothTake() {
            assertThat(form.accepted())
                    .containsAll(ItemCriteria.ACCEPTED)
                    .contains("<x>:<y>:<z>");
        }
    }

    @Nested
    @DisplayName("reading a whole sign")
    class ReadingASign {

        private static final ICDefinition SENSOR = ICDefinition.builder("MC9001", "TEST")
                .thirdLine(LineSpec.required("one thing to check", LineForms.itemFilter()))
                .fourthLine(LineSpec.optional("how far to reach", LineForms.wholeNumber(1, 30)))
                .logic(() -> state -> { })
                .build();

        @Test
        @DisplayName("refuses a required line the chip cannot read")
        void refusesARequiredLineItCannotRead() {
            LineReview review = LineReview.of(
                    SENSOR, SignLines.of("T", "[MC9001]", "item:stone", ""));

            assertThat(review.broken()).isTrue();
            assertThat(review.refusals()).hasSize(1);
            assertThat(review.refusals().get(0)).contains("item:stone").contains("ID:<item>");
        }

        @Test
        @DisplayName("only warns about an optional line it cannot read")
        void onlyWarnsAboutAnOptionalLine() {
            LineReview review = LineReview.of(
                    SENSOR, SignLines.of("T", "[MC9001]", "ID:stone", "far"));

            assertThat(review.broken()).isFalse();
            assertThat(review.warnings()).hasSize(1);
            assertThat(review.warnings().get(0)).contains("default");
        }

        @Test
        @DisplayName("says nothing at all about a sign that is written properly")
        void saysNothingAboutAGoodSign() {
            LineReview review = LineReview.of(
                    SENSOR, SignLines.of("T", "[MC9001]", "ID:stone", "12"));

            assertThat(review.hasAnythingToSay()).isFalse();
        }

        @Test
        @DisplayName("still refuses a required line left blank")
        void stillRefusesABlankRequiredLine() {
            LineReview review = LineReview.of(SENSOR, SignLines.of("T", "[MC9001]", "", ""));

            assertThat(review.broken()).isTrue();
            assertThat(review.refusals().get(0)).contains("one thing to check");
        }
    }
}
