// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.lopper;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Following one broken block outward")
class LoppersTest {

    private static final Key LOG = Key.key("minecraft:oak_log");
    private static final Key SPRUCE = Key.key("minecraft:spruce_log");
    private static final Key LEAVES = Key.key("minecraft:oak_leaves");
    private static final Key IRON = Key.key("minecraft:iron_ore");
    private static final Key DEEPSLATE_IRON = Key.key("minecraft:deepslate_iron_ore");
    private static final Key AIR = Key.key("minecraft:air");
    private static final Key AXE = Key.key("minecraft:iron_axe");

    /** A world written a block at a time, with air everywhere nothing was put. */
    private static final class Written implements LopperSight {

        private final Map<Vec3i, Key> blocks = new HashMap<>();
        private final Set<Vec3i> unreadable = new HashSet<>();

        Written put(int x, int y, int z, Key block) {
            blocks.put(new Vec3i(x, y, z), block);
            return this;
        }

        Written column(int x, int z, int from, int to, Key block) {
            for (int y = from; y <= to; y++) {
                put(x, y, z, block);
            }
            return this;
        }

        Written hide(Vec3i position) {
            unreadable.add(position);
            return this;
        }

        @Override
        public Key blockAt(Vec3i position) {
            return blocks.getOrDefault(position, AIR);
        }

        @Override
        public boolean isReadable(Vec3i position) {
            return !unreadable.contains(position);
        }
    }

    private static LopperRules rules(Set<Key> blocks, int maxSize) {
        return new LopperRules(blocks, Set.of(AXE), maxSize, false, false);
    }

    @Nested
    @DisplayName("a run")
    class ARun {

        @Test
        @DisplayName("takes the whole column a trunk stands in")
        void takesTheWholeTrunk() {
            Written world = new Written().column(0, 0, 0, 5, LOG);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 30), Set.of(), world);

            assertThat(taken).hasSize(6);
        }

        @Test
        @DisplayName("begins with the block that was struck, so a caller can skip it")
        void beginsWithTheBlockThatWasStruck() {
            Written world = new Written().column(0, 0, 0, 2, LOG);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 1, 0), rules(Set.of(LOG), 30), Set.of(), world);

            assertThat(taken).first().isEqualTo(new Vec3i(0, 1, 0));
        }

        @Test
        @DisplayName("stops at a gap, since a run follows blocks that touch")
        void stopsAtAGap() {
            Written world = new Written()
                    .column(0, 0, 0, 2, LOG)
                    .column(0, 0, 4, 6, LOG);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 30), Set.of(), world);

            assertThat(taken).hasSize(3);
        }

        @Test
        @DisplayName("never begins on a block that is not on the list")
        void neverBeginsOffTheList() {
            Written world = new Written().column(0, 0, 0, 5, LOG);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(IRON), 30), Set.of(), world);

            assertThat(taken).isEmpty();
        }

        @Test
        @DisplayName("stops where the world stops being readable")
        void stopsWhereTheWorldStops() {
            Written world = new Written()
                    .column(0, 0, 0, 5, LOG)
                    .hide(new Vec3i(0, 3, 0));

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 30), Set.of(), world);

            assertThat(taken).hasSize(3);
        }
    }

    @Nested
    @DisplayName("what counts as the same thing")
    class SameThing {

        @Test
        @DisplayName("is the block that was struck, not everything on the list")
        void isTheBlockThatWasStruck() {
            Written world = new Written()
                    .column(0, 0, 0, 3, LOG)
                    .column(1, 0, 0, 3, SPRUCE);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG, SPRUCE), 30), Set.of(), world);

            assertThat(taken).hasSize(4);
        }

        @Test
        @DisplayName("is anything on the list once an operator has asked for that")
        void isAnythingOnTheListWhenAsked() {
            Written world = new Written()
                    .column(0, 0, 0, 3, IRON)
                    .column(0, 0, 4, 6, DEEPSLATE_IRON);

            LopperRules mixed = new LopperRules(
                    Set.of(IRON, DEEPSLATE_IRON), Set.of(AXE), 30, false, true);

            List<Vec3i> taken =
                    Loppers.reach(new Vec3i(0, 0, 0), mixed, Set.of(), world);

            assertThat(taken).hasSize(7);
        }

        @Test
        @DisplayName("takes in the leaves when a tree lopper has been given them")
        void takesInTheLeaves() {
            Written world = new Written()
                    .column(0, 0, 0, 2, LOG)
                    .put(1, 2, 0, LEAVES)
                    .put(2, 2, 0, LEAVES);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 30), Set.of(LEAVES), world);

            assertThat(taken).hasSize(5);
        }

        @Test
        @DisplayName("leaves the leaves standing when it has not")
        void leavesTheLeaves() {
            Written world = new Written()
                    .column(0, 0, 0, 2, LOG)
                    .put(1, 2, 0, LEAVES);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 30), Set.of(), world);

            assertThat(taken).hasSize(3);
        }
    }

    @Nested
    @DisplayName("the limit")
    class TheLimit {

        @Test
        @DisplayName("counts the block that was struck, so a limit of one takes nothing more")
        void countsTheBlockThatWasStruck() {
            Written world = new Written().column(0, 0, 0, 9, LOG);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 1), Set.of(), world);

            assertThat(taken).containsExactly(new Vec3i(0, 0, 0));
        }

        @Test
        @DisplayName("takes the nearest blocks, so what is left standing is the far end")
        void takesTheNearestBlocks() {
            Written world = new Written().column(0, 0, 0, 9, LOG);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 3), Set.of(), world);

            assertThat(taken).containsExactly(
                    new Vec3i(0, 0, 0), new Vec3i(0, 1, 0), new Vec3i(0, 2, 0));
        }

        @Test
        @DisplayName("of zero switches the mechanic off without a second setting saying so")
        void ofZeroSwitchesItOff() {
            Written world = new Written().column(0, 0, 0, 5, LOG);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(LOG), 0), Set.of(), world);

            assertThat(taken).isEmpty();
        }

        @Test
        @DisplayName("is reached the same way with no blocks and with no tools listed")
        void anEmptyListStopsItToo() {
            Written world = new Written().column(0, 0, 0, 5, LOG);
            LopperRules noTools = new LopperRules(Set.of(LOG), Set.of(), 30, false, false);

            assertThat(Loppers.reach(new Vec3i(0, 0, 0), noTools, Set.of(), world)).isEmpty();
        }
    }

    @Nested
    @DisplayName("following corners")
    class Corners {

        @Test
        @DisplayName("is off, so a seam split by a diagonal comes away in two swings")
        void isOffByDefault() {
            Written world = new Written()
                    .put(0, 0, 0, IRON)
                    .put(1, 1, 0, IRON);

            List<Vec3i> taken = Loppers.reach(
                    new Vec3i(0, 0, 0), rules(Set.of(IRON), 30), Set.of(), world);

            assertThat(taken).hasSize(1);
        }

        @Test
        @DisplayName("joins them up once an operator has asked for it")
        void joinsThemUpWhenAsked() {
            Written world = new Written()
                    .put(0, 0, 0, IRON)
                    .put(1, 1, 0, IRON);

            LopperRules diagonal =
                    new LopperRules(Set.of(IRON), Set.of(AXE), 30, true, false);

            assertThat(Loppers.reach(new Vec3i(0, 0, 0), diagonal, Set.of(), world))
                    .hasSize(2);
        }
    }

    @Nested
    @DisplayName("what a tool has to be")
    class Tools {

        @Test
        @DisplayName("is on the list")
        void isOnTheList() {
            assertThat(rules(Set.of(LOG), 30).worksWith(AXE)).isTrue();
        }

        @Test
        @DisplayName("and nothing else works one")
        void nothingElseWorksOne() {
            assertThat(rules(Set.of(LOG), 30).worksWith(Key.key("minecraft:iron_shovel")))
                    .isFalse();
        }
    }
}
