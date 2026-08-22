// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.lopper;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("What regrows a felled tree")
class TreeLoppersTest {

    private static Key block(String name) {
        return Key.key("minecraft:" + name);
    }

    private static String saplingFor(String block) {
        return TreeLoppers.saplingFor(block(block)).orElseThrow().value();
    }

    @Nested
    @DisplayName("the sapling")
    class TheSapling {

        @Test
        @DisplayName("is read off the block's own name, so a wood added later replants itself")
        void isReadOffTheName() {
            assertThat(saplingFor("oak_log")).isEqualTo("oak_sapling");
            assertThat(saplingFor("cherry_log")).isEqualTo("cherry_sapling");
            assertThat(saplingFor("pale_oak_log")).isEqualTo("pale_oak_sapling");
        }

        @Test
        @DisplayName("is the same whether the wood was stripped, barked or leaves")
        void isTheSameWhicheverPart() {
            assertThat(saplingFor("stripped_birch_log")).isEqualTo("birch_sapling");
            assertThat(saplingFor("birch_wood")).isEqualTo("birch_sapling");
            assertThat(saplingFor("stripped_birch_wood")).isEqualTo("birch_sapling");
            assertThat(saplingFor("birch_leaves")).isEqualTo("birch_sapling");
        }

        @Test
        @DisplayName("is a propagule for a mangrove, which grows from no sapling")
        void isAPropaguleForAMangrove() {
            assertThat(saplingFor("mangrove_log")).isEqualTo("mangrove_propagule");
        }

        @Test
        @DisplayName("is a fungus for the nether woods, which are not trees at all")
        void isAFungusInTheNether() {
            assertThat(saplingFor("warped_stem")).isEqualTo("warped_fungus");
            assertThat(saplingFor("crimson_hyphae")).isEqualTo("crimson_fungus");
        }

        @Test
        @DisplayName("is nothing for a block that is not made of wood")
        void isNothingForSomethingElse() {
            assertThat(TreeLoppers.saplingFor(block("stone"))).isEmpty();
            assertThat(TreeLoppers.saplingFor(block("iron_ore"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("how many go back")
    class HowMany {

        @Test
        @DisplayName("is four for the woods that grow from a two-by-two")
        void isFourForTheBigOnes() {
            assertThat(TreeLoppers.saplingsFor(block("dark_oak_sapling"))).isEqualTo(4);
            assertThat(TreeLoppers.saplingsFor(block("jungle_sapling"))).isEqualTo(4);
        }

        @Test
        @DisplayName("is one for every other")
        void isOneForTheRest() {
            assertThat(TreeLoppers.saplingsFor(block("oak_sapling"))).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("what a sapling will stand on")
    class Soil {

        @Test
        @DisplayName("is the ground a tree grows out of")
        void isTheGroundATreeGrowsOutOf() {
            assertThat(TreeLoppers.isSoil(block("grass_block"))).isTrue();
            assertThat(TreeLoppers.isSoil(block("podzol"))).isTrue();
            assertThat(TreeLoppers.isSoil(block("mud"))).isTrue();
        }

        @Test
        @DisplayName("is not stone, so a tree grown on a platform is not replanted onto it")
        void isNotStone() {
            assertThat(TreeLoppers.isSoil(block("stone"))).isFalse();
            assertThat(TreeLoppers.isSoil(block("air"))).isFalse();
        }
    }
}
