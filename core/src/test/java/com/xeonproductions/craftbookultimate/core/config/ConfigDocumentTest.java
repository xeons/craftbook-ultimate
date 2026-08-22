// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import static com.xeonproductions.craftbookultimate.core.config.StubBlockNames.key;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The settings file")
class ConfigDocumentTest {

    private final List<String> complaints = new ArrayList<>();

    private final MapTree tree = new MapTree();

    private final StubBlockNames names = new StubBlockNames();

    private Settings read() {
        return new ConfigDocument(names, complaints::add).applyTo(tree);
    }

    @Nested
    @DisplayName("read by a server that has never had one")
    class Fresh {

        @Test
        @DisplayName("comes out as the defaults")
        void comesOutAsTheDefaults() {
            assertThat(read()).isEqualTo(Settings.DEFAULTS);
        }

        @Test
        @DisplayName("is left holding every setting, so the operator can see what there is")
        void isLeftHoldingEverySetting() {
            read();

            assertThat(tree.values)
                    .containsKeys("enabled", "ics.max-radius", "vehicles.carts.climb-speed",
                            "vehicles.boats.water-place-only", "pipes.max-length");
        }

        @Test
        @DisplayName("is left explained, header and all")
        void isLeftExplained() {
            read();

            assertThat(tree.header).isNotEmpty();
            assertThat(tree.comments).containsKeys("enabled", "ics.disabled");
        }
    }

    @Nested
    @DisplayName("read by a server that already has one")
    class Existing {

        @Test
        @DisplayName("keeps what the operator wrote")
        void keepsWhatTheOperatorWrote() {
            tree.values.put("ics.max-radius", 3);

            assertThat(read().maxRadius()).isEqualTo(3);
            assertThat(tree.values.get("ics.max-radius")).isEqualTo(3);
        }

        @Test
        @DisplayName("gains a setting it did not have, at its default")
        void gainsASettingItDidNotHave() {
            tree.values.put("enabled", false);

            Settings settings = read();

            assertThat(settings.enabled()).isFalse();
            assertThat(settings.maxRadius()).isEqualTo(Settings.DEFAULTS.maxRadius());
            assertThat(tree.values).containsKey("ics.max-radius");
        }

        @Test
        @DisplayName("has its explanations rewritten, so a better wording reaches an old server")
        void hasItsExplanationsRewritten() {
            tree.values.put("enabled", true);
            tree.comments.put("enabled", List.of("something somebody wrote in 2019"));

            read();

            assertThat(tree.comments.get("enabled"))
                    .isNotEqualTo(List.of("something somebody wrote in 2019"));
        }
    }

    @Nested
    @DisplayName("naming blocks")
    class NamingBlocks {

        @Test
        @DisplayName("takes the ones the server knows")
        void takesTheOnesTheServerKnows() {
            tree.values.put("ics.placeable-blocks", List.of("stone", "dirt"));

            assertThat(read().placeableBlocks())
                    .containsExactlyInAnyOrder(key("stone"), key("dirt"));
        }

        @Test
        @DisplayName("complains about one it does not, and keeps the rest of the list")
        void complainsAboutOneItDoesNot() {
            names.unknown.add("chees");
            tree.values.put("ics.placeable-blocks", List.of("stone", "chees", "dirt"));

            assertThat(read().placeableBlocks())
                    .containsExactlyInAnyOrder(key("stone"), key("dirt"));
            assertThat(complaints).anyMatch(said -> said.contains("chees"));
        }

        @Test
        @DisplayName("expands a tag to whatever the server currently has in it")
        void expandsATag() {
            names.tags.put("minecraft:planks", Set.of(key("oak_planks"), key("birch_planks")));
            tree.values.put("ics.placeable-blocks", List.of("#minecraft:planks"));

            assertThat(read().placeableBlocks())
                    .containsExactlyInAnyOrder(key("oak_planks"), key("birch_planks"));
        }

        @Test
        @DisplayName("complains about a tag that is not there rather than allowing everything")
        void complainsAboutATagThatIsNotThere() {
            tree.values.put("ics.placeable-blocks", List.of("#minecraft:nonesuch"));

            assertThat(read().placeableBlocks()).isEmpty();
            assertThat(complaints).anyMatch(said -> said.contains("nonesuch"));
        }
    }

    @Nested
    @DisplayName("reading the sections an operator names themselves")
    class NamedSections {

        @Test
        @DisplayName("takes which block builds which cart mechanic")
        void takesWhichBlockBuildsWhichCartMechanic() {
            tree.values.put("carts.blocks.station", "gold_block");

            assertThat(read().carts().blocks()).containsEntry("station", key("gold_block"));
        }

        @Test
        @DisplayName("takes how much each block boosts a cart")
        void takesHowMuchEachBlockBoosts() {
            tree.values.put("carts.boosters.gold_block", 2.5);

            assertThat(read().carts().boosters()).containsEntry(key("gold_block"), 2.5);
        }

        @Test
        @DisplayName("loses only the mechanic whose block is not there, and says which")
        void losesOnlyTheMechanicWhoseBlockIsNotThere() {
            names.unknown.add("chees");
            tree.values.put("carts.blocks.station", "chees");

            Map<String, Key> blocks = read().carts().blocks();

            // One bad entry costs that one mechanic, the same way one bad entry in a list of
            // blocks costs that one block. The rest of the railway keeps working.
            assertThat(blocks).doesNotContainKey("station");
            assertThat(blocks).containsAllEntriesOf(
                    Map.of("sort", key("netherrack"), "lift", key("orange_wool")));
            assertThat(complaints).anyMatch(said -> said.contains("chees") && said.contains("station"));
        }
    }

}
