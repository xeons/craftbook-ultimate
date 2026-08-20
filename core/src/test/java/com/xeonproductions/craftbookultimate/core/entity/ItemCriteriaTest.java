package com.xeonproductions.craftbookultimate.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Describing an item on a sign")
class ItemCriteriaTest {

    /** Resolves item names the way a world with no server behind it does. */
    private static final Function<String, Optional<Key>> ITEMS = EntitySpec.DEFAULT_ITEMS;

    private static ItemCriteria parse(String written) {
        Optional<ItemCriteria> criteria = ItemCriteria.parse(written, ITEMS);
        assertThat(criteria).as("parsing \"%s\"", written).isPresent();
        return criteria.get();
    }

    @Nested
    @DisplayName("checking what it is")
    class CheckingWhatItIs {

        @Test
        void matchesTheNamedItem() {
            ItemCriteria criteria = parse("ID:stone");

            assertThat(criteria.matches(ItemView.of(Blocks.key("stone"), 1))).isTrue();
            assertThat(criteria.matches(ItemView.of(Blocks.key("dirt"), 1))).isFalse();
        }

        @Test
        void handsTheItemNameToWhoeverCanResolveIt() {
            // Only the server can turn a name written before the flattening into an item, so the
            // text after the sort goes through untouched to whatever was given the job.
            Optional<ItemCriteria> criteria = ItemCriteria.parse(
                    "ID:35@14",
                    written -> written.equals("35@14")
                            ? Optional.of(Blocks.key("red_wool"))
                            : Optional.empty());

            assertThat(criteria.orElseThrow().item()).contains(Blocks.key("red_wool"));
        }

        @Test
        void refusesAnItemNothingCanResolve() {
            assertThat(ItemCriteria.parse("ID:", ITEMS)).isEmpty();
        }
    }

    @Nested
    @DisplayName("checking the rest")
    class CheckingTheRest {

        @Test
        void matchesAStackOfExactlyTheRightSize() {
            ItemCriteria criteria = parse("STACK:64");

            assertThat(criteria.matches(ItemView.of(Blocks.key("stone"), 64))).isTrue();
            assertThat(criteria.matches(ItemView.of(Blocks.key("stone"), 63))).isFalse();
        }

        @Test
        void refusesAStackSizeNoStackCouldHave() {
            assertThat(ItemCriteria.parse("STACK:100", ITEMS)).isEmpty();
        }

        @Test
        void matchesTheNameSomebodyGaveIt() {
            ItemCriteria criteria = parse("NAME:Front Door Key");

            assertThat(criteria.matches(named("Front Door Key"))).isTrue();
            assertThat(criteria.matches(named("Back Door Key"))).isFalse();
            assertThat(criteria.matches(ItemView.of(Blocks.key("stone"), 1))).isFalse();
        }

        @Test
        void matchesAFragmentOfWhatIsWrittenOnIt() {
            ItemCriteria criteria = parse("LORE:quest");

            assertThat(criteria.matches(withLore("part of a quest"))).isTrue();
            assertThat(criteria.matches(withLore("just a rock"))).isFalse();
        }

        @Test
        void keepsAValueContainingItsOwnSeparator() {
            assertThat(parse("NAME:a:b").displayName()).contains("a:b");
        }

        @Test
        void ignoresTheCaseTheSortWasWrittenIn() {
            assertThat(parse("stack:5").stackSize()).hasValue(5);
        }

        @Test
        void refusesASortItDoesNotKnow() {
            assertThat(ItemCriteria.parse("COLOUR:red", ITEMS)).isEmpty();
        }

        @Test
        void refusesALineWithNothingToCheckFor() {
            assertThat(ItemCriteria.parse("STACK", ITEMS)).isEmpty();
        }

        private static ItemView named(String name) {
            return new ItemView(Blocks.key("stone"), 1, Optional.of(name), List.of());
        }

        private static ItemView withLore(String line) {
            return new ItemView(Blocks.key("stone"), 1, Optional.empty(), List.of(line));
        }
    }

    @Nested
    @DisplayName("checking several things at once")
    class CheckingSeveralThingsAtOnce {

        @Test
        void requiresEveryOneOfThem() {
            ItemCriteria criteria = parse("ID:stone")
                    .and("STACK:5", ITEMS)
                    .orElseThrow();

            assertThat(criteria.matches(ItemView.of(Blocks.key("stone"), 5))).isTrue();
            assertThat(criteria.matches(ItemView.of(Blocks.key("stone"), 4))).isFalse();
            assertThat(criteria.matches(ItemView.of(Blocks.key("dirt"), 5))).isFalse();
        }

        @Test
        void letsALaterLineReplaceAnEarlierOneOfTheSameSort() {
            ItemCriteria criteria = parse("STACK:5").and("STACK:9", ITEMS).orElseThrow();

            assertThat(criteria.stackSize()).hasValue(9);
        }
    }

    @Test
    void matchesAnythingWhenNothingWasAskedFor() {
        assertThat(ItemCriteria.ANY.isAny()).isTrue();
        assertThat(ItemCriteria.ANY.matches(ItemView.of(Blocks.key("stone"), 1))).isTrue();
    }
}
