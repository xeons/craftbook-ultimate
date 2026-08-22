// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.head;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The head a death leaves behind")
class HeadDropsTest {

    private static Key creature(String name) {
        return Key.key(Key.MINECRAFT_NAMESPACE, name);
    }

    @Nested
    @DisplayName("which head comes off what")
    class WhichHead {

        @Test
        @DisplayName("is the game's own where the game has one")
        void isTheGamesOwnWhereItHasOne() {
            assertThat(HeadDrops.vanillaHead(creature("creeper")))
                    .contains(creature("creeper_head"));
            assertThat(HeadDrops.vanillaHead(creature("wither_skeleton")))
                    .contains(creature("wither_skeleton_skull"));
        }

        @Test
        @DisplayName("is nothing of the game's for a creature it has no head for")
        void isNothingOfTheGamesForACow() {
            assertThat(HeadDrops.vanillaHead(creature("cow"))).isEmpty();
        }

        @Test
        @DisplayName("is somebody's face for a creature the game has no head for")
        void isSomebodysFaceForACow() {
            assertThat(MobHeads.ownerOf(creature("cow"))).isPresent();
            assertThat(HeadDrops.hasHead(creature("cow"))).isTrue();
        }

        @Test
        @DisplayName("is nothing at all for a creature neither knows about")
        void isNothingForACreatureNeitherKnows() {
            assertThat(HeadDrops.hasHead(creature("armadillo"))).isFalse();
        }

        @Test
        @DisplayName("is the dragon's own rather than a face wearing it")
        void isTheDragonsOwn() {
            assertThat(HeadDrops.vanillaHead(creature("ender_dragon")))
                    .contains(creature("dragon_head"));
            assertThat(MobHeads.ownerOf(creature("ender_dragon"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("how likely a head is")
    class HowLikely {

        @Test
        @DisplayName("is the plain chance where nothing was enchanted")
        void isThePlainChance() {
            assertThat(HeadDrops.chanceOf(0.05, 0.05, 0)).isEqualTo(0.05);
        }

        @Test
        @DisplayName("adds the modifier once per level of looting")
        void addsPerLevelOfLooting() {
            assertThat(HeadDrops.chanceOf(0.05, 0.05, 3)).isEqualTo(0.20);
        }

        @Test
        @DisplayName("never goes past certain")
        void neverGoesPastCertain() {
            assertThat(HeadDrops.chanceOf(0.9, 0.5, 5)).isEqualTo(1);
        }

        @Test
        @DisplayName("is never negative, whatever a setting says")
        void isNeverNegative() {
            assertThat(HeadDrops.chanceOf(0, -1, 3)).isZero();
        }
    }

    @Nested
    @DisplayName("what a head is called")
    class WhatItIsCalled {

        @Test
        @DisplayName("belongs to the player it came off")
        void belongsToThePlayer() {
            assertThat(HeadDrops.nameOf(creature("player"), "Steve"))
                    .isEqualTo("Steve's Head");
        }

        @Test
        @DisplayName("is the creature's kind, read as somebody would say it")
        void isTheCreaturesKind() {
            assertThat(HeadDrops.nameOf(creature("cave_spider"), ""))
                    .isEqualTo("Cave Spider Head");
        }

        @Test
        @DisplayName("says what a placed head is without naming the account behind it")
        void saysWhatAPlacedHeadIs() {
            assertThat(HeadDrops.describe(creature("cow"), "MHF_Cow"))
                    .isEqualTo("The severed head of a cow");
        }

        @Test
        @DisplayName("names the player whose head it is")
        void namesThePlayer() {
            assertThat(HeadDrops.describe(creature("player"), "Steve"))
                    .isEqualTo("The severed head of Steve");
        }
    }

    @Nested
    @DisplayName("the accounts an operator has asked to be left alone")
    class Ignored {

        @Test
        @DisplayName("are matched however either is capitalised")
        void areMatchedHoweverCapitalised() {
            assertThat(HeadDrops.isIgnored(Set.of("CSCoreLib"), "cscorelib")).isTrue();
        }

        @Test
        @DisplayName("leave everybody else alone")
        void leaveEverybodyElseAlone() {
            assertThat(HeadDrops.isIgnored(Set.of("cscorelib"), "Steve")).isFalse();
        }
    }

    @Nested
    @DisplayName("the faces creatures wear")
    class TheFaces {

        @Test
        @DisplayName("are each one account, and no account is two creatures")
        void areEachOneAccount() {
            assertThat(MobHeads.all().values().stream().map(MobHeads.Owner::id).toList())
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("can be looked up backwards, so a placed head says what it is")
        void canBeLookedUpBackwards() {
            for (Map.Entry<Key, MobHeads.Owner> entry : MobHeads.all().entrySet()) {
                assertThat(MobHeads.creatureOwning(entry.getValue().id()))
                        .contains(entry.getKey());
            }
        }

        @Test
        @DisplayName("belong to nobody the game already has a head for")
        void doNotOverlapTheGamesOwn() {
            for (Key creature : MobHeads.all().keySet()) {
                assertThat(HeadDrops.vanillaHead(creature))
                        .as("%s has a head of its own", creature)
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("are not claimed by an account nobody has")
        void areNotClaimedByNobody() {
            Optional<Key> nobody = MobHeads.creatureOwning(
                    UUID.fromString("00000000-0000-0000-0000-000000000000"));

            assertThat(nobody).isEmpty();
        }
    }
}
