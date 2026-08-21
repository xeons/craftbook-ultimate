// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Writing the catalogue out")
class ICDocsTest {

    private static ICDefinition.Builder chip(String model, String shorthand) {
        return ICDefinition.builder(model, shorthand)
                .name("Test Chip")
                .description("Does a thing.")
                .logic(() -> state -> {});
    }

    private static String pageFor(ICDefinition... chips) {
        ICRegistry registry = new ICRegistry();
        for (ICDefinition chip : chips) {
            registry.register(chip);
        }
        return ICDocs.markdown(registry);
    }

    @Nested
    @DisplayName("the table everybody reads first")
    class TheTableEverybodyReadsFirst {

        @Test
        void listsEveryChipTheRegistryHolds() {
            String page = pageFor(chip("MC1000", "ONE").build(), chip("MC2000", "TWO").build());

            assertThat(page).contains("`MC1000`").contains("`MC2000`");
        }

        @Test
        void linksEachRowToWhereItIsWrittenAbout() {
            String page = pageFor(chip("MC1000", "ONE").name("Repeater").build());

            assertThat(page).contains("[`MC1000`](#mc1000--repeater)");
            assertThat(page).contains("### MC1000 — Repeater");
        }

        @Test
        void putsTheChipsInOrderOfTheirNumber() {
            String page = pageFor(chip("MC2000", "TWO").build(), chip("MC1000", "ONE").build());

            assertThat(page.indexOf("### MC1000")).isLessThan(page.indexOf("### MC2000"));
        }

        @Test
        void keepsADescriptionFromBreakingTheTableItSitsIn() {
            String page = pageFor(chip("MC1000", "ONE")
                    .description("A description with a | pipe in it.")
                    .build());

            assertThat(page).contains("A description with a \\| pipe in it.");
        }
    }

    @Nested
    @DisplayName("what each chip's entry says")
    class WhatEachChipsEntrySays {

        @Test
        void givesBothWaysOfNamingItOnASign() {
            // The shorthand goes after an equals sign, not in brackets. [REPEATER] names nothing.
            String page = pageFor(chip("MC1000", "REPEATER").build());

            assertThat(page).contains("`[MC1000]`, or `=REPEATER`");
            assertThat(page).doesNotContain("or `[REPEATER]`");
        }

        @Test
        void saysHowManyPinsItsWiringHas() {
            String page = pageFor(chip("MC1000", "ONE").layout(PinLayout.SISO).build());

            assertThat(page).contains("`SISO`, 1 input, 1 output");
        }

        @Test
        void countsNoOutputsAsNoneRatherThanZero() {
            String page = pageFor(chip("MC1000", "ONE").layout(PinLayout.AIZO).build());

            assertThat(page).contains("3 inputs, no outputs");
        }

        @Test
        void namesTheNumberItsSelfTriggeringFormAnswersTo() {
            String page = pageFor(chip("MC1000", "ONE").selfTriggeringModel("MC0000").build());

            assertThat(page).contains("**Runs on its own as** — `[MC0000]`");
        }

        @Test
        void namesTheRetiredNumbersItStillAnswersTo() {
            String page = pageFor(chip("MC1000", "ONE").aliases("MC1200").build());

            assertThat(page).contains("**Also answers to** — `MC1200`");
        }

        @Test
        void saysNothingAboutAliasesWhereThereAreNone() {
            String page = pageFor(chip("MC1000", "ONE").build());

            assertThat(page).doesNotContain("Also answers to");
        }

        @Test
        void warnsThatARestrictedChipIsNotForEverybody() {
            String page = pageFor(chip("MC1000", "ONE").restricted().build());

            assertThat(page).contains("**Restricted**");
            assertThat(page).contains("`craftbook.ic.restricted.mc1000`");
        }

        @Test
        void saysWhichLineWritingUuidIsReadOn() {
            String page = pageFor(chip("MC1000", "ONE").playerIdentityLine(2).build());

            assertThat(page).contains("Writing `uuid` on line 3");
        }
    }

    @Nested
    @DisplayName("checking its own work")
    class CheckingItsOwnWork {

        @Test
        void findsNothingMissingFromAPageItJustWrote() {
            ICRegistry registry = new ICRegistry();
            registry.register(chip("MC1000", "ONE").build());

            assertThat(ICDocs.whatIsMissing(registry, ICDocs.markdown(registry))).isEmpty();
        }

        @Test
        void namesTheChipAPageHasLeftOut() {
            ICRegistry registry = new ICRegistry();
            registry.register(chip("MC1000", "ONE").build());

            assertThat(ICDocs.whatIsMissing(registry, "# Nothing at all")).contains("MC1000");
        }
    }
}
