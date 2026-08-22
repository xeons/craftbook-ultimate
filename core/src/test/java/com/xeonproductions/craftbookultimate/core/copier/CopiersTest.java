// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.copier;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The copier signs")
class CopiersTest {

    @Nested
    @DisplayName("naming one")
    class Naming {

        @Test
        @DisplayName("recognises all three")
        void recognisesAllThree() {
            assertThat(Copiers.claimed("[BannerCopier]")).contains(Copiers.BANNER_SIGN);
            assertThat(Copiers.claimed("[BookCopier]")).contains(Copiers.BOOK_SIGN);
            assertThat(Copiers.claimed("[Map]")).contains(Copiers.MAP_SIGN);
        }

        @Test
        @DisplayName("does not mind how a builder capitalised it")
        void ignoresCase() {
            assertThat(Copiers.claimed("[bannercopier]")).contains(Copiers.BANNER_SIGN);
            assertThat(Copiers.claimed("[MAP]")).contains(Copiers.MAP_SIGN);
        }

        @Test
        @DisplayName("does not mind the spaces a sign picks up")
        void ignoresSurroundingSpace() {
            assertThat(Copiers.claimed("  [Map] ")).contains(Copiers.MAP_SIGN);
        }

        @Test
        @DisplayName("claims nothing else")
        void claimsNothingElse() {
            assertThat(Copiers.claimed("[Bridge]")).isEmpty();
            assertThat(Copiers.claimed("")).isEmpty();
            assertThat(Copiers.claimed("Map")).isEmpty();
        }
    }

    @Nested
    @DisplayName("the map number on one")
    class MapNumber {

        @Test
        @DisplayName("is read off the line")
        void isRead() {
            assertThat(Copiers.mapNumber("12")).hasValue(12);
            assertThat(Copiers.mapNumber(" 12 ")).hasValue(12);
            assertThat(Copiers.mapNumber("0")).hasValue(0);
        }

        @Test
        @DisplayName("is nothing at all where the line is not a number")
        void isNothingWhenNotANumber() {
            assertThat(Copiers.mapNumber("")).isEmpty();
            assertThat(Copiers.mapNumber("north")).isEmpty();
            assertThat(Copiers.mapNumber("1.5")).isEmpty();
        }

        @Test
        @DisplayName("is nothing for a number no map could have")
        void isNothingWhenNegative() {
            assertThat(Copiers.mapNumber("-1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("the permissions")
    class Permissions {

        @Test
        @DisplayName("are named after the sign, brackets off")
        void areNamedAfterTheSign() {
            assertThat(Copiers.buildPermission(Copiers.BANNER_SIGN))
                    .isEqualTo("craftbook.bannercopier");
            assertThat(Copiers.usePermission(Copiers.MAP_SIGN)).isEqualTo("craftbook.map.use");
        }
    }

    @Nested
    @DisplayName("what somebody has copied off a sign")
    class Clipboard {

        private final SignClipboard clipboard = new SignClipboard();
        private final UUID player = UUID.randomUUID();

        @Test
        @DisplayName("is nothing until they have copied something")
        void startsEmpty() {
            assertThat(clipboard.get(player)).isEmpty();
        }

        @Test
        @DisplayName("is what they copied")
        void holdsWhatWasCopied() {
            clipboard.put(player, SignLines.of("one", "two", "three", "four"));
            assertThat(clipboard.get(player)).isPresent();
            assertThat(clipboard.get(player).get().trimmedText(0)).isEqualTo("one");
        }

        @Test
        @DisplayName("can have a line changed, counting from one as a builder does")
        void aLineCanBeChanged() {
            clipboard.put(player, SignLines.of("one", "two", "three", "four"));

            assertThat(clipboard.edit(player, 2, "changed")).isTrue();
            assertThat(clipboard.get(player).get().trimmedText(1)).isEqualTo("changed");
        }

        @Test
        @DisplayName("refuses a line the sign does not have")
        void refusesALineOffTheSign() {
            clipboard.put(player, SignLines.of("one", "two", "three", "four"));

            assertThat(clipboard.edit(player, 0, "nope")).isFalse();
            assertThat(clipboard.edit(player, 5, "nope")).isFalse();
        }

        @Test
        @DisplayName("cannot be edited by somebody who has copied nothing")
        void refusesAnEditWithNothingCopied() {
            assertThat(clipboard.edit(player, 1, "nope")).isFalse();
        }

        @Test
        @DisplayName("is forgotten when they leave")
        void isForgottenOnLeaving() {
            clipboard.put(player, SignLines.of("one", "two", "three", "four"));
            clipboard.forget(player);

            assertThat(clipboard.get(player)).isEmpty();
            assertThat(clipboard.size()).isZero();
        }
    }
}
