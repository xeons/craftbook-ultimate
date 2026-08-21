// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Naming a potion effect on a sign")
class PotionEffectsTest {

    private static Key vanilla(String name) {
        return Key.key(Key.MINECRAFT_NAMESPACE, name);
    }

    @Test
    void readsAShortNameOfOneWord() {
        assertThat(PotionEffects.resolve("PO")).contains(vanilla("poison"));
    }

    @Test
    void readsAShortNameOfTwoWords() {
        assertThat(PotionEffects.resolve("NIVI")).contains(vanilla("night_vision"));
    }

    @Test
    void ignoresTheCaseAShortNameWasWrittenIn() {
        assertThat(PotionEffects.resolve("nivi")).contains(vanilla("night_vision"));
    }

    @Test
    void keepsResistanceOnTheShortNameBothItAndRegenerationWouldHaveHad() {
        // Signs have meant resistance by RE since before this rewrite, so it keeps the name.
        assertThat(PotionEffects.resolve("RE")).contains(vanilla("resistance"));
    }

    @Test
    void reachesRegenerationByItsFullName() {
        assertThat(PotionEffects.resolve("regeneration")).contains(vanilla("regeneration"));
    }

    @Test
    void readsAnEffectAddedSinceTheShortNamesWereFixed() {
        assertThat(PotionEffects.resolve("slow_falling")).contains(vanilla("slow_falling"));
    }

    @Test
    void readsAFullyQualifiedName() {
        assertThat(PotionEffects.resolve("minecraft:speed")).contains(vanilla("speed"));
    }

    @Test
    void refusesNothingAtAll() {
        assertThat(PotionEffects.resolve("")).isEmpty();
        assertThat(PotionEffects.resolve("   ")).isEmpty();
    }

    @Test
    void givesEveryShortNameADifferentEffect() {
        assertThat(PotionEffects.abbreviations().values())
                .doesNotHaveDuplicates();
    }
}
