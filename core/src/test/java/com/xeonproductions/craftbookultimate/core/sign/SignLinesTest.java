// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.sign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Sign lines")
class SignLinesTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void padsMissingTrailingLines() {
            SignLines sign = SignLines.of("REPEATER", "[MC1000]");

            assertThat(sign.text(0)).isEqualTo("REPEATER");
            assertThat(sign.text(1)).isEqualTo("[MC1000]");
            assertThat(sign.text(2)).isEmpty();
            assertThat(sign.text(3)).isEmpty();
        }

        @Test
        void acceptsExactlyFourComponents() {
            SignLines sign = SignLines.of(List.of(
                    Component.text("a"), Component.text("b"),
                    Component.text("c"), Component.text("d")));

            assertThat(sign.text(3)).isEqualTo("d");
        }

        @Test
        void rejectsTooManyLines() {
            assertThatThrownBy(() -> SignLines.of("a", "b", "c", "d", "e"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsAComponentListOfTheWrongLength() {
            assertThatThrownBy(() -> SignLines.of(List.of(Component.text("a"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly 4");
        }

        @Test
        void hasAnEmptyConstant() {
            assertThat(SignLines.EMPTY.isBlank(0)).isTrue();
            assertThat(SignLines.EMPTY.lines()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("reading")
    class Reading {

        @Test
        void stripsFormattingFromTheText() {
            // Mechanics compare against plain text, so colour on a line must not change parsing.
            SignLines sign = SignLines.EMPTY.withLine(
                    1, Component.text("[MC1000]", NamedTextColor.AQUA));

            assertThat(sign.text(1)).isEqualTo("[MC1000]");
        }

        @Test
        void trimsSurroundingWhitespaceOnRequest() {
            SignLines sign = SignLines.of("  spaced  ");

            assertThat(sign.text(0)).isEqualTo("  spaced  ");
            assertThat(sign.trimmedText(0)).isEqualTo("spaced");
        }

        @Test
        void reportsBlankLines() {
            SignLines sign = SignLines.of("text", "   ");

            assertThat(sign.isBlank(0)).isFalse();
            assertThat(sign.isBlank(1)).isTrue();
        }

        @Test
        void rejectsALineOutsideTheSign() {
            assertThatThrownBy(() -> SignLines.EMPTY.text(4))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> SignLines.EMPTY.text(-1))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("editing")
    class Editing {

        @Test
        void leavesTheOriginalUntouched() {
            SignLines original = SignLines.of("a", "b", "c", "d");

            SignLines edited = original.withLine(0, "changed");

            assertThat(original.text(0)).isEqualTo("a");
            assertThat(edited.text(0)).isEqualTo("changed");
        }

        @Test
        void keepsTheOtherLines() {
            SignLines edited = SignLines.of("a", "b", "c", "d").withLine(2, "changed");

            assertThat(edited.text(0)).isEqualTo("a");
            assertThat(edited.text(1)).isEqualTo("b");
            assertThat(edited.text(3)).isEqualTo("d");
        }
    }

    @Test
    void comparesByValue() {
        assertThat(SignLines.of("a", "b")).isEqualTo(SignLines.of("a", "b"));
        assertThat(SignLines.of("a", "b")).hasSameHashCodeAs(SignLines.of("a", "b"));
        assertThat(SignLines.of("a", "b")).isNotEqualTo(SignLines.of("a", "c"));
    }

    @Test
    void describesItselfReadably() {
        assertThat(SignLines.of("REPEATER", "[MC1000]")).hasToString("SignLines[REPEATER | [MC1000] |  | ]");
    }
}
