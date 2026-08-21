// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Reading a sign against what its chip says its lines are for")
class LineReviewTest {

    /** A chip wanting a file it cannot work without, and a volume it can default. */
    private static ICDefinition demanding() {
        return chip()
                .thirdLine(LineSpec.required("the file to play"))
                .fourthLine(LineSpec.optional("how loud, defaulting to full"))
                .build();
    }

    private static ICDefinition.Builder chip() {
        return ICDefinition.builder("MC9000", "TESTER")
                .name("Tester")
                .logic(() -> state -> {});
    }

    @Nested
    @DisplayName("A line the chip cannot work without")
    class Required {

        @Test
        void isReportedMissingWhenTheSignLeavesItBlank() {
            LineReview review = LineReview.of(demanding(), SignLines.of("TESTER", "[MC9000]", "", "3"));

            assertThat(review.missing()).singleElement()
                    .extracting(LineReview.Blank::index).isEqualTo(ICDefinition.THIRD_LINE);
            assertThat(review.broken()).isTrue();
        }

        @Test
        void isNotReportedWhenTheSignFillsItIn() {
            LineReview review =
                    LineReview.of(demanding(), SignLines.of("TESTER", "[MC9000]", "tune.mid", ""));

            assertThat(review.missing()).isEmpty();
            assertThat(review.broken()).isFalse();
        }

        @Test
        void countsAsBlankWhenTheSignCarriesOnlySpaces() {
            LineReview review =
                    LineReview.of(demanding(), SignLines.of("TESTER", "[MC9000]", "   ", ""));

            assertThat(review.broken()).isTrue();
        }
    }

    @Nested
    @DisplayName("A line the chip has a default for")
    class Optional {

        @Test
        void isReportedAsDefaultedRatherThanMissing() {
            LineReview review =
                    LineReview.of(demanding(), SignLines.of("TESTER", "[MC9000]", "tune.mid", ""));

            assertThat(review.defaulted()).singleElement()
                    .extracting(LineReview.Blank::index).isEqualTo(ICDefinition.FOURTH_LINE);
        }

        @Test
        void leavesTheChipWorking() {
            LineReview review =
                    LineReview.of(demanding(), SignLines.of("TESTER", "[MC9000]", "tune.mid", ""));

            assertThat(review.broken()).isFalse();
        }
    }

    @Test
    void findsNothingToSayAboutAChipThatReadsNeitherLine() {
        LineReview review = LineReview.of(
                chip().noLines().build(), SignLines.of("TESTER", "[MC9000]", "", ""));

        assertThat(review.missing()).isEmpty();
        assertThat(review.defaulted()).isEmpty();
        assertThat(review.broken()).isFalse();
    }

    @Test
    void readsEachBlankLineBackInTermsOfWhatItIsFor() {
        LineReview review = LineReview.of(demanding(), SignLines.of("TESTER", "[MC9000]", "", ""));

        assertThat(review.all()).extracting(LineReview.Blank::said)
                .containsExactly("Line 3 is the file to play.",
                        "Line 4 is how loud, defaulting to full.");
    }
}
