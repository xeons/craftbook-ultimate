// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.physics;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A ladder with nothing under it")
class FallingLaddersTest {

    private static final Key LADDER = Key.key("minecraft:ladder");
    private static final Key AIR = Key.key("minecraft:air");
    private static final Key STONE = Key.key("minecraft:stone");

    @Test
    @DisplayName("falls")
    void falls() {
        assertThat(FallingLadders.falls(LADDER, AIR)).isTrue();
    }

    @Test
    @DisplayName("stays where it is when something is holding it up")
    void staysWhenHeldUp() {
        assertThat(FallingLadders.falls(LADDER, STONE)).isFalse();
        assertThat(FallingLadders.falls(LADDER, LADDER)).isFalse();
    }

    @Test
    @DisplayName("counts the air of a cave and of the void, which are air by another name")
    void countsEveryKindOfAir() {
        assertThat(FallingLadders.falls(LADDER, Key.key("minecraft:cave_air"))).isTrue();
        assertThat(FallingLadders.falls(LADDER, Key.key("minecraft:void_air"))).isTrue();
    }

    @Test
    @DisplayName("is only a ladder; nothing else in the world is made to fall")
    void isOnlyALadder() {
        assertThat(FallingLadders.falls(STONE, AIR)).isFalse();
        assertThat(FallingLadders.falls(Key.key("minecraft:vine"), AIR)).isFalse();
    }
}
