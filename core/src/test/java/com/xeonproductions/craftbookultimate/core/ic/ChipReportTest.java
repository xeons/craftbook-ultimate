// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("What the debugging tools say about a chip")
class ChipReportTest {

    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    private static ChipReport.Pin input(int index, boolean wired, int power) {
        return new ChipReport.Pin(
                index, true, new Vec3i(index, 64, 0), wired, wired && power > 0, power);
    }

    private static ChipReport report(List<ChipReport.Pin> pins, boolean ticking) {
        return report(pins, ticking, LineReview.of(chip(), SignLines.EMPTY), Optional.empty());
    }

    private static ChipReport report(
            List<ChipReport.Pin> pins,
            boolean ticking,
            LineReview lines,
            Optional<Bounds> area) {

        return new ChipReport(
                "MC1000", "REPEATER", "Repeater",
                "world", new Vec3i(10, 64, 20),
                PinLayout.SISO, ICMode.NONE,
                ticking, true,
                pins, false, lines, SignLines.EMPTY, area);
    }

    private static ICDefinition chip() {
        return ICDefinition.builder("MC1000", "REPEATER").noLines().logic(() -> state -> {}).build();
    }

    /** Everything the report said, run together, so a test can look for a phrase in it. */
    private static String spoken(ChipReport report) {
        StringBuilder said = new StringBuilder();
        for (Component line : report.describe()) {
            said.append(PLAIN.serialize(line)).append('\n');
        }
        return said.toString();
    }

    @Nested
    @DisplayName("A pin")
    class Pins {

        @Test
        void saysNothingIsWiredWhenTheBlockIsNotAPowerSource() {
            assertThat(PLAIN.serialize(input(0, false, 0).describe()))
                    .contains("nothing wired here");
        }

        @Test
        void saysHowStrongTheSignalIsWhenOneIsCarried() {
            assertThat(PLAIN.serialize(input(0, true, 9).describe())).contains("on, power 9");
        }

        @Test
        void saysOffForSomethingWiredAndQuiet() {
            assertThat(PLAIN.serialize(input(0, true, 0).describe())).contains("off");
        }
    }

    @Nested
    @DisplayName("A chip that nothing can ever set off")
    class Unreachable {

        @Test
        void isCalledOutWhenNoInputIsWiredAndItDoesNotTick() {
            assertThat(spoken(report(List.of(input(0, false, 0)), false)))
                    .contains("Nothing is wired to any input");
        }

        @Test
        void isNotCalledOutWhenItTicksOnItsOwn() {
            assertThat(spoken(report(List.of(input(0, false, 0)), true)))
                    .doesNotContain("Nothing is wired to any input");
        }

        @Test
        void isNotCalledOutWhenSomethingIsWired() {
            assertThat(spoken(report(List.of(input(0, true, 0)), false)))
                    .doesNotContain("Nothing is wired to any input");
        }
    }

    @Nested
    @DisplayName("A sign that says too little")
    class Lines {

        private static final ICDefinition DEMANDING = ICDefinition.builder("MC1250", "SOUND")
                .thirdLine(LineSpec.required("the sound to play"))
                .fourthLine(LineSpec.optional("how loud"))
                .logic(() -> state -> {})
                .build();

        @Test
        void hasItsMissingLineReportedAsStoppingTheChipDead() {
            LineReview review = LineReview.of(DEMANDING, SignLines.EMPTY);

            assertThat(spoken(report(List.of(input(0, true, 15)), false, review, Optional.empty())))
                    .contains("Line 3 is the sound to play.")
                    .contains("this chip does nothing");
        }

        @Test
        void hasItsDefaultedLineReportedAsMerelyDefaulted() {
            LineReview review = LineReview.of(DEMANDING, SignLines.of("", "", "ding", ""));

            assertThat(spoken(report(List.of(input(0, true, 15)), false, review, Optional.empty())))
                    .contains("the default is used")
                    .doesNotContain("this chip does nothing");
        }
    }

    @Test
    void namesTheChipBeforeAnythingElse() {
        assertThat(report(List.of(input(0, true, 15)), false).describe().getFirst())
                .satisfies(first -> assertThat(PLAIN.serialize(first))
                        .contains("MC1000")
                        .contains("Repeater"));
    }

    @Test
    void tellsAChipThatCouldTickHowToMakeItTick() {
        assertThat(spoken(report(List.of(input(0, true, 15)), false)))
                .contains("add S to the model");
    }

    @Test
    void saysNothingAboutTickingToOneAlreadyDoingIt() {
        assertThat(spoken(report(List.of(input(0, true, 15)), true)))
                .contains("ticking every tick")
                .doesNotContain("add S to the model");
    }

    @Test
    void givesTheAreaWhereTheChipHasOne() {
        Bounds area = new Bounds(new Vec3i(0, 60, 0), new Vec3i(4, 64, 4));

        assertThat(spoken(report(
                List.of(input(0, true, 15)), false, LineReview.of(chip(), SignLines.EMPTY),
                Optional.of(area))))
                .contains("0,60,0 to 4,64,4");
    }

    @Test
    void leavesTheAreaOutForAChipThatHasNone() {
        assertThat(spoken(report(List.of(input(0, true, 15)), false))).doesNotContain("Area");
    }
}
