// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import static com.xeonproductions.craftbookultimate.core.config.StubBlockNames.key;
import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import com.xeonproductions.craftbookultimate.core.mechanic.SneakState;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The mechanics file")
class MechanicsDocumentTest {

    private final List<String> complaints = new ArrayList<>();

    private final MapTree tree = new MapTree();

    private final StubBlockNames names = new StubBlockNames();

    private MechanicSettings read() {
        return new MechanicsDocument(names, complaints::add).applyTo(tree);
    }

    @Nested
    @DisplayName("read by a server that has never had one")
    class Fresh {

        @Test
        @DisplayName("comes out as the defaults")
        void comesOutAsTheDefaults() {
            assertThat(read()).isEqualTo(MechanicSettings.DEFAULTS);
        }

        @Test
        @DisplayName("leaves every mechanic switched off, so a fresh server changes nothing")
        void leavesEveryMechanicOff() {
            MechanicSettings settings = read();

            assertThat(settings.running()).isEmpty();
            for (String mechanic : Mechanics.ALL) {
                assertThat(settings.allows(mechanic)).as(mechanic).isFalse();
                assertThat(tree.values).containsEntry(mechanic + ".enabled", false);
            }
        }

        @Test
        @DisplayName("gives every mechanic a section, including the ones with nothing to set")
        void givesEveryMechanicASection() {
            read();

            for (String mechanic : Mechanics.ALL) {
                assertThat(tree.values).containsKey(mechanic + ".enabled");
            }
        }

        @Test
        @DisplayName("puts each mechanic's own settings under its own name")
        void putsEachSettingUnderItsMechanic() {
            read();

            assertThat(tree.values).containsKeys(
                    "Gate.radius", "Elevator.tolerance", "Area.max-blocks",
                    "GlowStone.off-block", "Netherrack.fire-blocks", "LightSwitch.range",
                    "Ammeter.item", "LightStone.item", "BounceBlocks.sensitivity",
                    "Teleporter.range", "XPStorer.per-bottle", "Snow.piling",
                    "Chairs.heal-amount", "HeadDrops.drop-rate");
        }

        @Test
        @DisplayName("keeps the two rules belonging to no mechanic at the top")
        void keepsTheSharedRulesAtTheTop() {
            read();

            assertThat(tree.values)
                    .containsKeys("redstone", "depower-on-source-removal");
        }

        @Test
        @DisplayName("is left explained, header and all")
        void isLeftExplained() {
            read();

            assertThat(tree.header).isNotEmpty();
            assertThat(tree.comments).containsKeys("Gate", "Gate.radius", "redstone");
        }
    }

    @Nested
    @DisplayName("switching a mechanic on")
    class Enabling {

        @Test
        @DisplayName("is the mechanic's own section saying so, and reaches no other")
        void isTheMechanicsOwnSectionSayingSo() {
            tree.values.put("Gate.enabled", true);

            MechanicSettings settings = read();

            assertThat(settings.allows(Mechanics.GATE)).isTrue();
            assertThat(settings.allows(Mechanics.BRIDGE)).isFalse();
        }

        @Test
        @DisplayName("leaves a switched-off mechanic's settings readable, so turning it on works")
        void leavesTheOtherSettingsReadable() {
            tree.values.put("Gate.radius", 3);

            assertThat(read().gate().radius()).isEqualTo(3);
        }

        @Test
        @DisplayName("ignores a section named after nothing at all")
        void ignoresASectionNamedAfterNothing() {
            tree.values.put("Nonesuch.enabled", true);

            assertThat(read().enabled()).isEmpty();
        }

        @Test
        @DisplayName("leaves a mechanic the file never mentions switched off")
        void leavesAMechanicTheFileNeverMentionsOff() {
            // A file an operator has trimmed by hand, or one written before the mechanic
            // existed. Neither is a reason to start changing what blocks in the world do.
            tree.values.put("Gate.enabled", true);

            assertThat(read().allows(Mechanics.CHAIRS)).isFalse();
        }
    }

    @Nested
    @DisplayName("read by a server that already has one")
    class Existing {

        @Test
        @DisplayName("keeps what the operator wrote")
        void keepsWhatTheOperatorWrote() {
            tree.values.put("Elevator.tolerance", 2);

            assertThat(read().elevator().tolerance()).isEqualTo(2);
            assertThat(tree.values.get("Elevator.tolerance")).isEqualTo(2);
        }

        @Test
        @DisplayName("gains a setting it did not have, at its default")
        void gainsASettingItDidNotHave() {
            tree.values.put("Snow.piling", true);

            MechanicSettings settings = read();

            assertThat(settings.snow().piling()).isTrue();
            assertThat(settings.gate().radius())
                    .isEqualTo(MechanicSettings.DEFAULTS.gate().radius());
            assertThat(tree.values).containsKey("Gate.radius");
        }

        @Test
        @DisplayName("has its explanations rewritten, so a better wording reaches an old server")
        void hasItsExplanationsRewritten() {
            tree.values.put("Gate.radius", 5);
            tree.comments.put("Gate.radius", List.of("something somebody wrote in 2019"));

            read();

            assertThat(tree.comments.get("Gate.radius"))
                    .isNotEqualTo(List.of("something somebody wrote in 2019"));
        }
    }

    @Nested
    @DisplayName("naming blocks")
    class NamingBlocks {

        @Test
        @DisplayName("takes what a gate may be made of")
        void takesWhatAGateMayBeMadeOf() {
            tree.values.put("Gate.blocks", List.of("iron_bars"));

            assertThat(read().gate().blocks()).containsExactly(key("iron_bars"));
        }

        @Test
        @DisplayName("complains about one the server does not know, and keeps the rest")
        void complainsAboutOneItDoesNotKnow() {
            names.unknown.add("chees");
            tree.values.put("Netherrack.fire-blocks", List.of("netherrack", "chees"));

            assertThat(read().powerables().fireBlocks()).containsExactly(key("netherrack"));
            assertThat(complaints).anyMatch(said -> said.contains("chees"));
        }

        @Test
        @DisplayName("falls back to the default where the whole list is unreadable")
        void fallsBackWhereTheWholeListIsUnreadable() {
            names.unknown.add("chees");
            tree.values.put("Gate.blocks", List.of("chees"));

            assertThat(read().gate().blocks())
                    .isEqualTo(MechanicSettings.DEFAULTS.gate().blocks());
        }
    }

    @Nested
    @DisplayName("reading the sections an operator names themselves")
    class NamedSections {

        @Test
        @DisplayName("takes what each block throws somebody with no sign at all")
        void takesWhatEachBlockThrows() {
            tree.values.put("BounceBlocks.automatic.gold_block", "!5");

            assertThat(read().bounce().automatic())
                    .containsEntry(key("gold_block"), "!5");
        }

        @Test
        @DisplayName("loses only the block that is not there, and says which")
        void losesOnlyTheBlockThatIsNotThere() {
            names.unknown.add("chees");
            tree.values.put("BounceBlocks.automatic.chees", "3");
            tree.values.put("BounceBlocks.automatic.gold_block", "3");

            assertThat(read().bounce().automatic())
                    .containsKey(key("gold_block"))
                    .doesNotContainKey(key("chees"));
            assertThat(complaints).anyMatch(said -> said.contains("chees"));
        }
    }

    @Nested
    @DisplayName("reading the settings that are not blocks or numbers")
    class OtherKinds {

        @Test
        @DisplayName("takes whether somebody must be crouching")
        void takesWhetherSomebodyMustBeCrouching() {
            tree.values.put("XPStorer.sneaking", SneakState.MUST.written());

            assertThat(read().xp().sneaking()).isEqualTo(SneakState.MUST);
        }

        @Test
        @DisplayName("keeps the default where the crouching rule is not one of the three")
        void keepsTheDefaultWhereTheCrouchingRuleIsNonsense() {
            tree.values.put("XPStorer.sneaking", "sideways");

            assertThat(read().xp().sneaking())
                    .isEqualTo(MechanicSettings.DEFAULTS.xp().sneaking());
        }

        @Test
        @DisplayName("takes a teleporter reaching nowhere at all")
        void takesATeleporterReachingNowhere() {
            tree.values.put("Teleporter.range", 0.0);

            assertThat(read().teleporter().range()).isZero();
        }
    }

    @Nested
    @DisplayName("the chairs")
    class TheChairs {

        @Test
        @DisplayName("are written as the tag rather than sixty block names")
        void areWrittenAsTheTag() {
            read();

            assertThat(tree.values.get("Chairs.blocks"))
                    .isEqualTo(ChairSettings.DEFAULT_BLOCK_NAMES);
        }

        @Test
        @DisplayName("fall back to every stair where the server has no such tag")
        void fallBackToEveryStair() {
            assertThat(read().chair().blocks())
                    .isEqualTo(ChairSettings.DEFAULTS.blocks())
                    .contains(key("oak_stairs"), key("polished_blackstone_brick_stairs"));
        }

        @Test
        @DisplayName("take a list an operator wrote instead")
        void takeAListAnOperatorWrote() {
            tree.values.put("Chairs.blocks", List.of("oak_slab"));

            assertThat(read().chair().blocks()).containsExactly(key("oak_slab"));
        }
    }

    @Nested
    @DisplayName("the names it is keyed by")
    class Names {

        @Test
        @DisplayName("are every mechanic there is, each named once")
        void areEveryMechanicNamedOnce() {
            assertThat(Mechanics.ALL).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("carry no brackets, whatever the sign says")
        void carryNoBrackets() {
            assertThat(Mechanics.ALL).noneMatch(name -> name.contains("[") || name.contains("]"));
        }

        @Test
        @DisplayName("are matched however an operator spells them")
        void areMatchedHoweverSpelt() {
            MechanicSettings settings =
                    MechanicSettings.DEFAULTS.withEnabled(Set.of("GATE", "bridge"));

            assertThat(settings.allows(Mechanics.GATE)).isTrue();
            assertThat(settings.allows(Mechanics.BRIDGE)).isTrue();
            assertThat(settings.allows(Mechanics.DOOR)).isFalse();
        }

        @Test
        @DisplayName("are said back properly spelt, whatever an operator wrote")
        void areSaidBackProperlySpelt() {
            MechanicSettings settings =
                    MechanicSettings.DEFAULTS.withEnabled(Set.of("GATE", "bridge"));

            assertThat(settings.running()).containsExactly(Mechanics.BRIDGE, Mechanics.GATE);
        }
    }
}
