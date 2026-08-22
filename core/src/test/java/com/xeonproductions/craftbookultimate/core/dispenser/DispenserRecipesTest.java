// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.dispenser;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.config.DispenserSettings;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("A dispenser loaded in a pattern")
class DispenserRecipesTest {

    private static final DispenserSettings EVERYTHING = DispenserSettings.DEFAULTS;

    /** What is in the nine slots, written the way a builder loads them. */
    private static List<Key> loaded(String... items) {
        List<Key> slots = new ArrayList<>(items.length);
        for (String item : items) {
            slots.add(Key.key("minecraft", item));
        }
        return slots;
    }

    private static List<Key> cannon() {
        return loaded(
                "fire_charge", "gunpowder", "fire_charge",
                "gunpowder", "tnt", "gunpowder",
                "fire_charge", "gunpowder", "fire_charge");
    }

    @Nested
    @DisplayName("is recognised")
    class Recognised {

        @Test
        @DisplayName("by everything being in its slot")
        void byEverythingBeingInItsSlot() {
            assertThat(DispenserRecipes.matching(cannon(), EVERYTHING))
                    .contains(DispenserRecipe.CANNON);
        }

        @Test
        @DisplayName("with the empty slots being empty, not merely anything")
        void withEmptySlotsBeingEmpty() {
            List<Key> almostAnArrow = loaded(
                    "stone", "fire_charge", "air",
                    "fire_charge", "arrow", "fire_charge",
                    "air", "fire_charge", "air");

            assertThat(DispenserRecipes.matching(almostAnArrow, EVERYTHING)).isEmpty();
        }

        @Test
        @DisplayName("as nothing when one slot is wrong")
        void asNothingWhenOneSlotIsWrong() {
            List<Key> wrong = new ArrayList<>(cannon());
            wrong.set(4, Key.key("minecraft:stone"));

            assertThat(DispenserRecipes.matching(wrong, EVERYTHING)).isEmpty();
        }

        @Test
        @DisplayName("as nothing when the dispenser is empty, which is most of them")
        void asNothingWhenEmpty() {
            List<Key> empty = loaded(
                    "air", "air", "air",
                    "air", "air", "air",
                    "air", "air", "air");

            assertThat(DispenserRecipes.matching(empty, EVERYTHING)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the fan and the vacuum")
    class FanAndVacuum {

        @Test
        @DisplayName("are told apart by the piston in the middle alone")
        void areToldApartByThePiston() {
            List<Key> fan = loaded(
                    "cobweb", "oak_leaves", "cobweb",
                    "oak_leaves", "piston", "oak_leaves",
                    "cobweb", "oak_leaves", "cobweb");
            List<Key> vacuum = new ArrayList<>(fan);
            vacuum.set(4, Key.key("minecraft:sticky_piston"));

            assertThat(DispenserRecipes.matching(fan, EVERYTHING))
                    .contains(DispenserRecipe.FAN);
            assertThat(DispenserRecipes.matching(vacuum, EVERYTHING))
                    .contains(DispenserRecipe.VACUUM);
        }

        @Test
        @DisplayName("push hardest against the dispenser and weaken with every block")
        void pushHardestUpClose() {
            assertThat(DispenserRecipes.draught(0, false))
                    .isGreaterThan(DispenserRecipes.draught(3, false));
        }

        @Test
        @DisplayName("push opposite ways, which is the whole of the difference")
        void pushOppositeWays() {
            assertThat(DispenserRecipes.draught(1, true))
                    .isEqualTo(-DispenserRecipes.draught(1, false));
        }

        @Test
        @DisplayName("push nothing at all past their reach")
        void pushNothingPastTheirReach() {
            assertThat(DispenserRecipes.draught(DispenserRecipes.DRAUGHT_REACH, false))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("what an operator has switched off")
    class SwitchedOff {

        @Test
        @DisplayName("is not recognised, so the dispenser goes on dispensing")
        void isNotRecognised() {
            DispenserSettings withoutCannon =
                    DispenserSettings.DEFAULTS.with(DispenserRecipe.CANNON, false);

            assertThat(DispenserRecipes.matching(cannon(), withoutCannon)).isEmpty();
        }

        @Test
        @DisplayName("leaves the others working")
        void leavesTheOthersWorking() {
            DispenserSettings withoutCannon =
                    DispenserSettings.DEFAULTS.with(DispenserRecipe.CANNON, false);

            assertThat(withoutCannon.allows(DispenserRecipe.FAN)).isTrue();
            assertThat(withoutCannon.anythingAtAll()).isTrue();
        }

        @Test
        @DisplayName("all of them means nothing is even looked at")
        void allOfThemMeansNothingIsLookedAt() {
            DispenserSettings none = new DispenserSettings(java.util.Set.of());

            assertThat(none.anythingAtAll()).isFalse();
            assertThat(DispenserRecipes.matching(cannon(), none)).isEmpty();
        }
    }

    @Nested
    @DisplayName("every recipe there is")
    class EveryRecipe {

        @Test
        @DisplayName("fills all nine slots, so a pattern has no holes in it")
        void fillsAllNineSlots() {
            for (DispenserRecipe recipe : DispenserRecipe.values()) {
                assertThat(recipe.pattern())
                        .as(recipe.name())
                        .hasSize(DispenserRecipe.SLOTS)
                        .doesNotContainNull();
            }
        }

        @Test
        @DisplayName("is recognised from its own pattern")
        void isRecognisedFromItsOwnPattern() {
            for (DispenserRecipe recipe : DispenserRecipe.values()) {
                assertThat(DispenserRecipes.matching(recipe.pattern(), EVERYTHING))
                        .as(recipe.name())
                        .contains(recipe);
            }
        }

        @Test
        @DisplayName("has a name an operator can switch it off by")
        void hasANameToSwitchItOffBy() {
            for (DispenserRecipe recipe : DispenserRecipe.values()) {
                assertThat(recipe.settingName())
                        .as(recipe.name())
                        .isNotEmpty()
                        .doesNotContain("_");
            }
        }
    }
}
