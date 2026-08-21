// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The question a sign asks about a cart")
class CartFilterTest {

    private static final Key STONE = Blocks.key("stone");
    private static final Key DIRT = Blocks.key("dirt");

    /**
     * Resolves the two items these tests use, and nothing else.
     *
     * <p>A real world asks the server, which knows what is an item and what is a typo. A resolver
     * that answered every name would let a mistyped filter through.
     */
    private static final Function<String, Optional<Key>> ITEMS = written -> switch (written) {
        case "stone" -> Optional.of(STONE);
        case "dirt" -> Optional.of(DIRT);
        default -> Optional.empty();
    };

    private static final Stations NOWHERE = new Stations();

    private static boolean matches(String filter, Cart cart) {
        return matches(filter, cart, NOWHERE);
    }

    private static boolean matches(String filter, Cart cart, Stations stations) {
        Optional<CartFilter> parsed = CartFilter.parse(filter, ITEMS);
        assertThat(parsed).as("parsing \"%s\"", filter).isPresent();
        return parsed.get().matches(cart, stations);
    }

    @Nested
    @DisplayName("asking about nothing in particular")
    class AskingAboutNothingInParticular {

        @Test
        void matchesEveryCart() {
            assertThat(matches("all", SimpleCart.rideable())).isTrue();
            assertThat(matches("all", SimpleCart.storage())).isTrue();
        }

        @Test
        void matchesNoCartWhenTheLineIsBlank() {
            assertThat(matches("", SimpleCart.rideable())).isFalse();
        }

        @Test
        void matchesNoCartWhenTheLineSaysNone() {
            assertThat(matches("none", SimpleCart.rideable())).isFalse();
        }

        @Test
        void refusesAWordItDoesNotKnow() {
            assertThat(CartFilter.parse("aeroplane", ITEMS)).isEmpty();
        }
    }

    @Nested
    @DisplayName("asking what kind of cart it is")
    class AskingWhatKindOfCartItIs {

        @Test
        void tellsTheKindsApart() {
            assertThat(matches("minecart", SimpleCart.rideable())).isTrue();
            assertThat(matches("minecart", SimpleCart.storage())).isFalse();
            assertThat(matches("storage", SimpleCart.storage())).isTrue();
            assertThat(matches("hopper", SimpleCart.hopper())).isTrue();
            assertThat(matches("powered", SimpleCart.of(CartType.FURNACE))).isTrue();
            assertThat(matches("tnt", SimpleCart.of(CartType.TNT))).isTrue();
        }

        @Test
        void readsTheKindWhicheverWayItWasWritten() {
            assertThat(matches("STORAGE", SimpleCart.storage())).isTrue();
        }
    }

    @Nested
    @DisplayName("asking who is riding")
    class AskingWhoIsRiding {

        @Test
        void tellsAnEmptyCartFromAFullOne() {
            Cart empty = SimpleCart.rideable();
            Cart full = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            assertThat(matches("empty", empty)).isTrue();
            assertThat(matches("unoccupied", empty)).isTrue();
            assertThat(matches("full", empty)).isFalse();
            assertThat(matches("occupied", full)).isTrue();
        }

        @Test
        void tellsAPlayerFromAMobFromAnAnimal() {
            Cart withPlayer = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));
            Cart withZombie = SimpleCart.rideable().carrying(SimpleBystander.monster("zombie"));
            Cart withPig = SimpleCart.rideable().carrying(SimpleBystander.animal("pig"));

            assertThat(matches("player", withPlayer)).isTrue();
            assertThat(matches("mob", withZombie)).isTrue();
            assertThat(matches("animal", withPig)).isTrue();
            assertThat(matches("mob", withPig)).isFalse();
        }

        @Test
        void matchesTheRidersNameExactly() {
            Cart cart = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            assertThat(matches("ply:Ada", cart)).isTrue();
            assertThat(matches("ply:Adam", cart)).isFalse();
            assertThat(matches("ply:!Ada", cart)).isFalse();
            assertThat(matches("ply:!Bob", cart)).isTrue();
        }

        @Test
        void matchesPartOfTheRidersName() {
            Cart cart = SimpleCart.rideable().carrying(SimpleBystander.player("Adalovelace"));

            assertThat(matches("plym:love", cart)).isTrue();
            assertThat(matches("plym:!love", cart)).isFalse();
        }

        @Test
        void matchesTheGroupTheRiderIsIn() {
            Cart cart = SimpleCart.rideable()
                    .carrying(SimpleBystander.player("Ada").inGroup("staff"));

            assertThat(matches("group:staff", cart)).isTrue();
            assertThat(matches("group:guest", cart)).isFalse();
            assertThat(matches("group:!guest", cart)).isTrue();
        }

        @Test
        void matchesNobodyWhenTheCartIsEmpty() {
            Cart cart = SimpleCart.rideable();

            assertThat(matches("ply:Ada", cart)).isFalse();
            assertThat(matches("ply:!Ada", cart)).isFalse();
            assertThat(matches("group:staff", cart)).isFalse();
        }
    }

    @Nested
    @DisplayName("asking what is being carried")
    class AskingWhatIsBeingCarried {

        @Test
        void tellsALoadedStorageCartFromAnEmptyOne() {
            Cart loaded = SimpleCart.storage().holding(STONE, 4);
            Cart empty = SimpleCart.storage();

            assertThat(matches("ctns", loaded)).isTrue();
            assertThat(matches("ctns", empty)).isFalse();
            assertThat(matches("!ctns", empty)).isTrue();
            assertThat(matches("!ctns", loaded)).isFalse();
        }

        @Test
        void refusesACartThatCannotCarryAnything() {
            assertThat(matches("ctns", SimpleCart.rideable())).isFalse();
            assertThat(matches("!ctns", SimpleCart.rideable())).isFalse();
        }

        @Test
        void matchesAnItemAnywhereInTheCart() {
            Cart cart = SimpleCart.storage().holding(STONE, 4);

            assertThat(matches("sci+:stone", cart)).isTrue();
            assertThat(matches("sci+:dirt", cart)).isFalse();
        }

        @Test
        void matchesAnItemInTheFirstSlotOnly() {
            Cart cart = SimpleCart.storage()
                    .holding(DIRT, 1)
                    .withFirstSlot(ItemView.of(STONE, 1));

            assertThat(matches("sci:stone", cart)).isTrue();
            assertThat(matches("sci:dirt", cart)).isFalse();
        }

        @Test
        void countsHowManyWereAskedFor() {
            Cart cart = SimpleCart.storage().holding(STONE, 4);

            assertThat(matches("sci+:stone:4", cart)).isTrue();
            assertThat(matches("sci+:stone:5", cart)).isFalse();
        }

        @Test
        void refusesAnItemNothingCanResolve() {
            assertThat(CartFilter.parse("sci+:notablock", ITEMS)).isEmpty();
        }

        @Test
        void refusesAnItemFilterWithNoItem() {
            assertThat(CartFilter.parse("sci+", ITEMS)).isEmpty();
            assertThat(CartFilter.parse("held", ITEMS)).isEmpty();
        }
    }

    @Nested
    @DisplayName("asking what the rider has")
    class AskingWhatTheRiderHas {

        @Test
        void matchesWhatTheyAreHolding() {
            Cart cart = SimpleCart.rideable().carrying(
                    SimpleBystander.player("Ada").holding(ItemView.of(STONE, 1)));

            assertThat(matches("held:stone", cart)).isTrue();
            assertThat(matches("held:dirt", cart)).isFalse();
        }

        @Test
        void matchesAnEmptyHand() {
            Cart cart = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            assertThat(matches("held:none", cart)).isTrue();
            assertThat(matches("held:!none", cart)).isFalse();
        }

        @Test
        void asksForAnItemWithoutThrowingWhenTheHandIsEmpty() {
            // The old filter read the held item without checking there was one, so a rider with
            // empty hands rolling over a junction threw out of the move listener.
            Cart cart = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            assertThat(matches("held:stone", cart)).isFalse();
        }

        @Test
        void matchesWhatIsInTheirPack() {
            Cart cart = SimpleCart.rideable().carrying(
                    SimpleBystander.player("Ada").carryingPack(SimpleStockpile.empty().with(STONE, 3)));

            assertThat(matches("inv:stone", cart)).isTrue();
            assertThat(matches("inv:stone:3", cart)).isTrue();
            assertThat(matches("inv:stone:4", cart)).isFalse();
            assertThat(matches("inv:dirt", cart)).isFalse();
        }

        @Test
        void matchesAnEmptyPack() {
            Cart cart = SimpleCart.rideable().carrying(
                    SimpleBystander.player("Ada").carryingPack(SimpleStockpile.empty()));

            assertThat(matches("inv:none", cart)).isTrue();
        }
    }

    @Nested
    @DisplayName("asking about the cart itself")
    class AskingAboutTheCartItself {

        @Test
        void matchesTheNameGivenToTheCart() {
            Cart cart = SimpleCart.storage().named("Ore Train");

            assertThat(matches("cart:Ore Train", cart)).isTrue();
            assertThat(matches("cart:Coal Train", cart)).isFalse();
            assertThat(matches("cartm:Ore", cart)).isTrue();
            assertThat(matches("cartm:!Ore", cart)).isFalse();
        }

        @Test
        void matchesNothingWhenTheCartHasNoName() {
            assertThat(matches("cart:Ore Train", SimpleCart.storage())).isFalse();
        }
    }

    @Nested
    @DisplayName("asking where the rider is going")
    class AskingWhereTheRiderIsGoing {

        @Test
        void matchesTheDestinationTheySet() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            Cart cart = SimpleCart.rideable().carrying(rider);
            Stations stations = new Stations();
            stations.setDestination(rider.uniqueId().orElseThrow(), "northgate");

            assertThat(matches("#northgate", cart, stations)).isTrue();
            assertThat(matches("#north*", cart, stations)).isTrue();
            assertThat(matches("#south*", cart, stations)).isFalse();
            assertThat(matches("#!south*", cart, stations)).isTrue();
        }

        @Test
        void matchesARiderWhoHasNotSaid() {
            Cart cart = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));

            assertThat(matches("nostop", cart)).isTrue();
        }

        @Test
        void refusesARiderWhoHasSaid() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            Cart cart = SimpleCart.rideable().carrying(rider);
            Stations stations = new Stations();
            stations.setDestination(rider.uniqueId().orElseThrow(), "northgate");

            assertThat(matches("nostop", cart, stations)).isFalse();
        }

        @Test
        void refusesAStationNameWithNothingAfterTheMarker() {
            assertThat(CartFilter.parse("#", ITEMS)).isEmpty();
        }
    }
}
