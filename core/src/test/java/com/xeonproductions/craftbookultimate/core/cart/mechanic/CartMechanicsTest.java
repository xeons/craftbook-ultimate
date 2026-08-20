package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanic;
import com.xeonproductions.craftbookultimate.core.cart.CartRecipe;
import com.xeonproductions.craftbookultimate.core.cart.RailShape;
import com.xeonproductions.craftbookultimate.core.cart.SimpleCart;
import com.xeonproductions.craftbookultimate.core.cart.SimpleCartVisit;
import com.xeonproductions.craftbookultimate.core.cart.Stations;
import com.xeonproductions.craftbookultimate.core.cart.Wiring;
import com.xeonproductions.craftbookultimate.core.config.CartSettings;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The minecart mechanics")
class CartMechanicsTest {

    private static final Vec3i RAIL = new Vec3i(0, 64, 0);
    private static final Key STONE = Blocks.key("stone");

    /** Where the base block of a mechanism sits, which is one under the rail. */
    private static final Vec3i BASE = new Vec3i(0, 63, 0);

    /** Where its sign sits, which is one under the base. */
    private static final Vec3i SIGN = new Vec3i(0, 62, 0);

    @Nested
    @DisplayName("the booster")
    class TheBooster {

        private final CartMechanic booster = CartSpeed.booster();

        @Test
        void multipliesTheSpeedItsBlockIsWorth() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "gold_ore").cart(cart).build();

            booster.onCart(visit);

            assertThat(cart.velocity().z()).isEqualTo(-0.5);
        }

        @Test
        void slowsACartOnTheSlowBlocks() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "soul_sand").cart(cart).build();

            booster.onCart(visit);

            assertThat(cart.velocity().z()).isEqualTo(-0.2);
        }

        @Test
        void isBuiltFromEveryBoosterBlockAndNothingElse() {
            assertThat(booster.appliesTo(
                    SimpleCartVisit.at(RAIL, "gold_block").build().mechanism(), Settings.DEFAULTS))
                    .isTrue();
            assertThat(booster.appliesTo(
                    SimpleCartVisit.at(RAIL, "stone").build().mechanism(), Settings.DEFAULTS))
                    .isFalse();
        }

        @Test
        void leavesACartAloneWhileItIsStillCrossingOneBlock() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit =
                    SimpleCartVisit.at(RAIL, "gold_ore").cart(cart).minor().build();

            booster.onCart(visit);

            assertThat(cart.velocity().z()).isEqualTo(-0.4);
        }

        @Test
        void leavesACartAloneWhenItHasBeenSwitchedOff() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "gold_ore")
                    .cart(cart)
                    .wiring(Wiring.OFF)
                    .build();

            booster.onCart(visit);

            assertThat(cart.velocity().z()).isEqualTo(-0.4);
        }
    }

    @Nested
    @DisplayName("the delay")
    class TheDelay {

        private final CartMechanic delay = CartSpeed.delay();

        @Test
        void stopsTheCartAndSendsItOnAfterTheWait() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "yellow_wool", BlockFace.SOUTH, "", "[Delay]", "3", "")
                    .cart(cart)
                    .build();

            delay.onCart(visit);
            assertThat(cart.velocity()).isEqualTo(Vec3d.ZERO);

            visit.manualScheduler().advance(3 * 20);

            // The sign faces south, so the cart leaves northward, away behind it.
            assertThat(cart.velocity().z()).isEqualTo(-1.0);
        }

        @Test
        void leavesACartThatHasSinceBeenPushed() {
            SimpleCart cart = SimpleCart.rideable();
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "yellow_wool", BlockFace.SOUTH, "", "[Delay]", "3", "")
                    .cart(cart)
                    .build();

            delay.onCart(visit);
            cart.setVelocity(new Vec3d(1, 0, 0));
            visit.manualScheduler().advance(3 * 20);

            assertThat(cart.velocity()).isEqualTo(new Vec3d(1, 0, 0));
        }

        @Test
        void leavesACartThatHasSinceBeenCarriedAway() {
            SimpleCart cart = SimpleCart.rideable();
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "yellow_wool", BlockFace.SOUTH, "", "[Delay]", "3", "")
                    .cart(cart)
                    .build();

            delay.onCart(visit);
            cart.teleport(new Vec3d(40, 64, 40));
            visit.manualScheduler().advance(3 * 20);

            assertThat(cart.velocity()).isEqualTo(Vec3d.ZERO);
        }

        @Test
        void doesNothingWhenTheWaitIsNotANumber() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "yellow_wool", BlockFace.SOUTH, "", "[Delay]", "soon", "")
                    .cart(cart)
                    .build();

            delay.onCart(visit);

            assertThat(visit.manualScheduler().pendingCount()).isZero();
        }
    }

    @Nested
    @DisplayName("the launcher")
    class TheLauncher {

        private final CartMechanic launcher = CartSpeed.launcher();

        @Test
        void holdsAnEmptyCart() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "lime_wool", BlockFace.SOUTH, "", "[Launch]", "", "")
                    .cart(cart)
                    .build();

            launcher.onCart(visit);

            assertThat(cart.velocity()).isEqualTo(Vec3d.ZERO);
        }

        @Test
        void letsAnOccupiedCartRollThrough() {
            SimpleCart cart = SimpleCart.rideable()
                    .moving(new Vec3d(0, 0, -0.4))
                    .carrying(SimpleBystander.player("Ada"));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "lime_wool", BlockFace.SOUTH, "", "[Launch]", "", "")
                    .cart(cart)
                    .build();

            launcher.onCart(visit);

            assertThat(cart.velocity().z()).isEqualTo(-0.4);
        }

        @Test
        void holdsOnlyTheCartsItsFourthLineNames() {
            SimpleCart chest = SimpleCart.storage().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "lime_wool", BlockFace.SOUTH, "", "[Launch]", "", "minecart")
                    .cart(chest)
                    .build();

            launcher.onCart(visit);

            assertThat(chest.velocity().z()).isEqualTo(-0.4);
        }

        @Test
        void sendsACartOffWhenSomebodyGetsIn() {
            SimpleCart cart = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "lime_wool", BlockFace.SOUTH, "", "[Launch]", "", "")
                    .cart(cart)
                    .build();

            assertThat(CartSpeed.launchOnMount(visit)).isTrue();
            assertThat(cart.velocity().z()).isEqualTo(-1.0);
        }

        @Test
        void keepsHoldOfSomebodyItsThirdLineDoesNotName() {
            SimpleCart cart = SimpleCart.rideable().carrying(SimpleBystander.player("Ada"));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "lime_wool", BlockFace.SOUTH, "", "[Launch]", "group:staff", "")
                    .cart(cart)
                    .build();

            assertThat(CartSpeed.launchOnMount(visit)).isFalse();
            assertThat(cart.velocity()).isEqualTo(Vec3d.ZERO);
        }
    }

    @Nested
    @DisplayName("the junction")
    class TheJunction {

        private final CartMechanic sorter = CartRouting.sorter();

        /** Where a junction's rail sits: two above the block behind the sign. */
        private static final Vec3i JUNCTION = new Vec3i(0, 64, -1);

        private SimpleCartVisit.Builder junction(String left, String right) {
            return SimpleCartVisit.at(RAIL, "netherrack", BlockFace.SOUTH, "", "[Sort]", left, right)
                    .world(world -> world.withRail(JUNCTION, RailShape.NORTH_SOUTH));
        }

        @Test
        void sendsAMatchingCartLeft() {
            SimpleCartVisit visit = junction("storage", "").cart(SimpleCart.storage()).build();

            sorter.onCart(visit);

            // The sign faces south, so the cart travels north and left of that is west.
            assertThat(visit.simpleWorld().railShapeAt(JUNCTION)).contains(RailShape.SOUTH_WEST);
        }

        @Test
        void sendsAMatchingCartRight() {
            SimpleCartVisit visit = junction("", "storage").cart(SimpleCart.storage()).build();

            sorter.onCart(visit);

            assertThat(visit.simpleWorld().railShapeAt(JUNCTION)).contains(RailShape.SOUTH_EAST);
        }

        @Test
        void sendsAnythingElseStraightOn() {
            SimpleCartVisit visit = junction("storage", "hopper").cart(SimpleCart.rideable()).build();

            sorter.onCart(visit);

            assertThat(visit.simpleWorld().railShapeAt(JUNCTION)).contains(RailShape.NORTH_SOUTH);
        }

        @Test
        void sendsACartTowardsTheDestinationItsRiderSet() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            Stations stations = new Stations();
            stations.setDestination(rider.uniqueId().orElseThrow(), "northgate");

            SimpleCartVisit visit = junction("#north*", "")
                    .cart(SimpleCart.rideable().carrying(rider))
                    .stations(stations)
                    .build();

            sorter.onCart(visit);

            assertThat(visit.simpleWorld().railShapeAt(JUNCTION)).contains(RailShape.SOUTH_WEST);
        }

        @Test
        void leavesTheRailAloneWithoutAJunctionToBend() {
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "netherrack", BlockFace.SOUTH, "", "[Sort]", "all", "")
                    .cart(SimpleCart.rideable())
                    .build();

            assertThat(sorter.onCart(visit)).isFalse();
            assertThat(visit.simpleWorld().railShapeAt(JUNCTION)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the lift")
    class TheLift {

        private final CartMechanic lift = CartRouting.lift();

        @Test
        void carriesACartUpToTheNextLanding() {
            Vec3i landing = new Vec3i(0, 70, 0);
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "orange_wool", BlockFace.SOUTH, "", "[CartLift]", "all", "")
                    .cart(cart)
                    .world(world -> world
                            .withBlock(landing, "orange_wool")
                            .withRail(landing.add(0, 1, 0), RailShape.NORTH_SOUTH))
                    .build();

            lift.onCart(visit);

            assertThat(cart.position().y()).isEqualTo(71);
            assertThat(cart.speed()).isEqualTo(0.4);
        }

        @Test
        void carriesACartDownWhenItsFourthLineMatches() {
            Vec3i landing = new Vec3i(0, 50, 0);
            SimpleCart cart = SimpleCart.storage().moving(new Vec3d(0, 0, -0.4));

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "orange_wool", BlockFace.SOUTH, "", "[CartLift]", "minecart", "storage")
                    .cart(cart)
                    .world(world -> world
                            .withBlock(landing, "orange_wool")
                            .withRail(landing.add(0, 1, 0), RailShape.NORTH_SOUTH))
                    .build();

            lift.onCart(visit);

            assertThat(cart.position().y()).isEqualTo(51);
        }

        @Test
        void leavesACartAloneWithNoLandingToReach() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "orange_wool", BlockFace.SOUTH, "", "[CartLift]", "all", "")
                    .cart(cart)
                    .build();

            lift.onCart(visit);

            assertThat(cart.position()).isEqualTo(Vec3d.centreOf(RAIL));
        }

        @Test
        void leavesACartAloneWhenNeitherLineNamesIt() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "orange_wool", BlockFace.SOUTH, "", "[CartLift]", "storage", "hopper")
                    .cart(cart)
                    .world(world -> world
                            .withBlock(new Vec3i(0, 70, 0), "orange_wool")
                            .withRail(new Vec3i(0, 71, 0), RailShape.NORTH_SOUTH))
                    .build();

            lift.onCart(visit);

            assertThat(cart.position()).isEqualTo(Vec3d.centreOf(RAIL));
        }
    }

    @Nested
    @DisplayName("the station")
    class TheStation {

        private final CartMechanic station = CartRouting.station();

        @Test
        void holdsACartThatHasCometoRest() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "obsidian", BlockFace.SOUTH, "", "[Station]", "", "")
                    .cart(cart)
                    .build();

            assertThat(station.onCart(visit)).isTrue();
            assertThat(cart.velocity()).isEqualTo(Vec3d.ZERO);
        }

        @Test
        void letsACartFinishRollingOntoTheBlockFirst() {
            SimpleCart cart = SimpleCart.rideable().moving(new Vec3d(0, 0, -0.4));
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "obsidian", BlockFace.SOUTH, "", "[Station]", "", "")
                    .cart(cart)
                    .stillRolling()
                    .build();

            assertThat(station.onCart(visit)).isFalse();
            assertThat(cart.velocity().z()).isEqualTo(-0.4);
        }

        @Test
        void sendsACartOffWhenItIsPowered() {
            SimpleCart cart = SimpleCart.rideable().withMaximumSpeed(0.4);
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "obsidian", BlockFace.SOUTH, "", "[Station]", "", "")
                    .cart(cart)
                    .wiring(Wiring.ON)
                    .build();

            assertThat(station.onCart(visit)).isFalse();
            assertThat(cart.velocity().z()).isEqualTo(-0.4);
        }

        @Test
        void holdsOnlyTheRidersHeadingForItsOwnName() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            Stations stations = new Stations();
            stations.setDestination(rider.uniqueId().orElseThrow(), "northgate");

            SimpleCart stopping = SimpleCart.rideable().carrying(rider);
            SimpleCartVisit atNorthgate = SimpleCartVisit.at(
                            RAIL, "obsidian", BlockFace.SOUTH, "", "[Station]", "#northgate", "")
                    .cart(stopping)
                    .stations(stations)
                    .build();

            assertThat(station.onCart(atNorthgate)).isTrue();
        }

        @Test
        void letsAnyoneElseRollThrough() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            Stations stations = new Stations();
            stations.setDestination(rider.uniqueId().orElseThrow(), "southgate");

            SimpleCart passing = SimpleCart.rideable()
                    .moving(new Vec3d(0, 0, -0.4))
                    .carrying(rider);
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "obsidian", BlockFace.SOUTH, "", "[Station]", "#northgate", "")
                    .cart(passing)
                    .stations(stations)
                    .build();

            assertThat(station.onCart(visit)).isFalse();
            assertThat(passing.velocity().z()).isEqualTo(-0.4);
        }
    }

    @Nested
    @DisplayName("clearing a destination")
    class ClearingADestination {

        private final CartMechanic clear = CartRouting.stationClear();

        @Test
        void forgetsWhereTheRiderWasGoingAndSaysSo() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            Stations stations = new Stations();
            stations.setDestination(rider.uniqueId().orElseThrow(), "northgate");

            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "brown_wool")
                    .cart(SimpleCart.rideable().carrying(rider))
                    .stations(stations)
                    .build();

            clear.onCart(visit);

            assertThat(stations.destination(rider.uniqueId().orElseThrow())).isEmpty();
            assertThat(rider.plainMessages()).hasSize(1);
        }

        @Test
        void saysNothingToARiderWhoHadSaidNothing() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "brown_wool")
                    .cart(SimpleCart.rideable().carrying(rider))
                    .build();

            clear.onCart(visit);

            assertThat(rider.plainMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("the loading bay")
    class TheLoadingBay {

        private final CartMechanic loader = CartCargo.loader();

        @Test
        void putsSomebodyStandingNearbyIntoAnEmptyCart() {
            SimpleBystander waiting =
                    SimpleBystander.player("Ada").at(Vec3d.centreOf(RAIL).add(new Vec3d(1, 0, 0)));
            SimpleCart cart = SimpleCart.rideable();

            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "cyan_wool")
                    .cart(cart)
                    .world(world -> world.withPerson(waiting))
                    .build();

            loader.onCart(visit);

            assertThat(cart.riders()).containsExactly(waiting);
        }

        @Test
        void leavesAnOccupiedCartAlone() {
            SimpleBystander riding = SimpleBystander.player("Ada");
            SimpleBystander waiting =
                    SimpleBystander.player("Bob").at(Vec3d.centreOf(RAIL));
            SimpleCart cart = SimpleCart.rideable().carrying(riding);

            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "cyan_wool")
                    .cart(cart)
                    .world(world -> world.withPerson(waiting))
                    .build();

            loader.onCart(visit);

            assertThat(cart.riders()).containsExactly(riding);
        }

        @Test
        void leavesSomebodyStandingTooFarAway() {
            SimpleBystander waiting =
                    SimpleBystander.player("Ada").at(Vec3d.centreOf(RAIL).add(new Vec3d(9, 0, 0)));
            SimpleCart cart = SimpleCart.rideable();

            SimpleCartVisit visit = SimpleCartVisit.at(RAIL, "cyan_wool")
                    .cart(cart)
                    .world(world -> world.withPerson(waiting))
                    .build();

            loader.onCart(visit);

            assertThat(cart.riders()).isEmpty();
        }
    }

    @Nested
    @DisplayName("the chest mechanics")
    class TheChestMechanics {

        private final CartMechanic collector = CartCargo.collector();
        private final CartMechanic depositor = CartCargo.depositor();

        /** A chest beside the track, within the spread a mechanism reaches. */
        private static final Vec3i CHEST = new Vec3i(1, 64, 0);

        @Test
        void emptiesAPassingCartIntoTheChest() {
            SimpleStockpile chest = SimpleStockpile.empty();
            SimpleCart cart = SimpleCart.storage().holding(STONE, 12);

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "iron_ore", BlockFace.SOUTH, "", "[Collect]", "", "")
                    .cart(cart)
                    .world(world -> world.withContainer(CHEST, chest))
                    .build();

            collector.onCart(visit);

            assertThat(chest.count(STONE)).isEqualTo(12);
            assertThat(cart.hold().count(STONE)).isZero();
        }

        @Test
        void takesOnlyWhatTheSignNames() {
            SimpleStockpile chest = SimpleStockpile.empty();
            SimpleCart cart = SimpleCart.storage().holding(STONE, 12).holding(Blocks.key("dirt"), 5);

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "iron_ore", BlockFace.SOUTH, "", "[Collect]", "stone", "")
                    .cart(cart)
                    .world(world -> world.withContainer(CHEST, chest))
                    .build();

            collector.onCart(visit);

            assertThat(chest.count(STONE)).isEqualTo(12);
            assertThat(cart.hold().count(Blocks.key("dirt"))).isEqualTo(5);
        }

        @Test
        void takesOnlyAsManyAsTheSignAsksFor() {
            SimpleStockpile chest = SimpleStockpile.empty();
            SimpleCart cart = SimpleCart.storage().holding(STONE, 12);

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "iron_ore", BlockFace.SOUTH, "", "[Collect]", "stone:5", "")
                    .cart(cart)
                    .world(world -> world.withContainer(CHEST, chest))
                    .build();

            collector.onCart(visit);

            assertThat(chest.count(STONE)).isEqualTo(5);
            assertThat(cart.hold().count(STONE)).isEqualTo(7);
        }

        @Test
        void fillsAPassingCartFromTheChest() {
            SimpleStockpile chest = SimpleStockpile.empty().with(STONE, 8);
            SimpleCart cart = SimpleCart.storage();

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "iron_ore", BlockFace.SOUTH, "", "[Deposit]", "", "")
                    .cart(cart)
                    .world(world -> world.withContainer(CHEST, chest))
                    .build();

            depositor.onCart(visit);

            assertThat(cart.hold().count(STONE)).isEqualTo(8);
            assertThat(chest.count(STONE)).isZero();
        }

        @Test
        void leavesACartThatCarriesNothingAlone() {
            SimpleStockpile chest = SimpleStockpile.empty().with(STONE, 8);
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "iron_ore", BlockFace.SOUTH, "", "[Deposit]", "", "")
                    .cart(SimpleCart.rideable())
                    .world(world -> world.withContainer(CHEST, chest))
                    .build();

            depositor.onCart(visit);

            assertThat(chest.count(STONE)).isEqualTo(8);
        }

        @Test
        void tellsTheTwoApartByTheirSigns() {
            SimpleStockpile chest = SimpleStockpile.empty();
            SimpleCart cart = SimpleCart.storage().holding(STONE, 4);

            SimpleCartVisit deposit = SimpleCartVisit.at(
                            RAIL, "iron_ore", BlockFace.SOUTH, "", "[Deposit]", "", "")
                    .cart(cart)
                    .world(world -> world.withContainer(CHEST, chest))
                    .build();

            // Both are built from the same block, so a collector must not act on a deposit sign.
            collector.onCart(deposit);

            assertThat(cart.hold().count(STONE)).isEqualTo(4);
            assertThat(chest.count(STONE)).isZero();
        }
    }

    @Nested
    @DisplayName("the crafter")
    class TheCrafter {

        private final CartMechanic crafter = CartCrafting.crafter();

        private static final CartRecipe TORCHES = new CartRecipe(
                "torch",
                Map.of(Blocks.key("coal"), 1, Blocks.key("stick"), 1),
                Blocks.key("torch"),
                4);

        @Test
        void craftsOutOfWhatTheCartIsCarrying() {
            SimpleCart cart = SimpleCart.storage()
                    .holding(Blocks.key("coal"), 1)
                    .holding(Blocks.key("stick"), 1);

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "gray_wool", BlockFace.SOUTH, "", "[Craft]", "torch", "")
                    .cart(cart)
                    .world(world -> world.withRecipe(TORCHES))
                    .build();

            crafter.onCart(visit);

            assertThat(cart.hold().count(Blocks.key("torch"))).isEqualTo(4);
            assertThat(cart.hold().count(Blocks.key("coal"))).isZero();
        }

        @Test
        void leavesACartThatIsShortOfAnIngredient() {
            SimpleCart cart = SimpleCart.storage().holding(Blocks.key("coal"), 1);

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "gray_wool", BlockFace.SOUTH, "", "[Craft]", "torch", "")
                    .cart(cart)
                    .world(world -> world.withRecipe(TORCHES))
                    .build();

            crafter.onCart(visit);

            assertThat(cart.hold().count(Blocks.key("torch"))).isZero();
            assertThat(cart.hold().count(Blocks.key("coal"))).isEqualTo(1);
        }

        @Test
        void readsARecipeNameSpreadOverTwoLines() {
            CartRecipe recipe = new CartRecipe(
                    "goldenapple", Map.of(Blocks.key("apple"), 1), Blocks.key("golden_apple"), 1);
            SimpleCart cart = SimpleCart.storage().holding(Blocks.key("apple"), 1);

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "gray_wool", BlockFace.SOUTH, "", "[Craft]", "goldena", "pple")
                    .cart(cart)
                    .world(world -> world.withRecipe(recipe))
                    .build();

            crafter.onCart(visit);

            assertThat(cart.hold().count(Blocks.key("golden_apple"))).isEqualTo(1);
        }

        @Test
        void givesTheBucketBackWhenARecipeUsesOne() {
            CartRecipe cake = new CartRecipe(
                    "cake", Map.of(Blocks.key("milk_bucket"), 3), Blocks.key("cake"), 1);
            SimpleCart cart = SimpleCart.storage().holding(Blocks.key("milk_bucket"), 3);

            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "gray_wool", BlockFace.SOUTH, "", "[Craft]", "cake", "")
                    .cart(cart)
                    .world(world -> world.withRecipe(cake))
                    .build();

            crafter.onCart(visit);

            assertThat(cart.hold().count(Blocks.key("bucket"))).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("the message sign")
    class TheMessageSign {

        private final CartMechanic printer = CartMessages.printer();

        @Test
        void readsTheSignToWhoeverIsRiding() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "stone", BlockFace.SOUTH, "", "[Print]", "Next stop", " Northgate")
                    .cart(SimpleCart.rideable().carrying(rider))
                    .build();

            printer.onCart(visit);

            assertThat(rider.plainMessages()).containsExactly("Next stop Northgate");
        }

        @Test
        void carriesOnDownASignBelowIt() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "stone", BlockFace.SOUTH, "", "[Print]", "Change here", "")
                    .cart(SimpleCart.rideable().carrying(rider))
                    .world(world -> world.withSign(
                            SIGN.add(0, -1, 0), BlockFace.SOUTH, " for the", " east line", "", ""))
                    .build();

            printer.onCart(visit);

            assertThat(rider.plainMessages()).containsExactly("Change here for the east line");
        }

        @Test
        void startsANewLineWhereTheSignSaysTo() {
            SimpleBystander rider = SimpleBystander.player("Ada");
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "stone", BlockFace.SOUTH, "", "[Print]", "one\\ntwo", "")
                    .cart(SimpleCart.rideable().carrying(rider))
                    .build();

            printer.onCart(visit);

            assertThat(rider.plainMessages()).containsExactly("one", "two");
        }

        @Test
        void saysNothingToAnEmptyCart() {
            SimpleCartVisit visit = SimpleCartVisit.at(
                            RAIL, "stone", BlockFace.SOUTH, "", "[Print]", "Next stop", "")
                    .cart(SimpleCart.rideable())
                    .build();

            assertThat(printer.onCart(visit)).isFalse();
        }

        @Test
        void isBuiltOnWhateverBlockItIsHungUnder() {
            // The one mechanic with no block of its own, so it applies wherever its sign does.
            assertThat(printer.appliesTo(
                    SimpleCartVisit.at(RAIL, "diamond_block", BlockFace.SOUTH, "", "[Print]", "", "")
                            .build()
                            .mechanism(),
                    Settings.DEFAULTS))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("choosing which mechanic is built here")
    class ChoosingWhichMechanicIsBuiltHere {

        @Test
        void picksTheOneWhoseBlockIsUnderTheRail() {
            var mechanism = SimpleCartVisit.at(RAIL, "obsidian", BlockFace.SOUTH, "", "[Station]", "", "")
                    .build()
                    .mechanism();

            assertThat(CartRouting.station().appliesTo(mechanism, Settings.DEFAULTS)).isTrue();
            assertThat(CartRouting.sorter().appliesTo(mechanism, Settings.DEFAULTS)).isFalse();
        }

        @Test
        void refusesAMechanicAnOperatorHasSwitchedOff() {
            Settings settings = Settings.builder()
                    .carts(CartSettings.DEFAULTS.withDisabled(java.util.Set.of("station")))
                    .build();
            var mechanism = SimpleCartVisit.at(RAIL, "obsidian", BlockFace.SOUTH, "", "[Station]", "", "")
                    .build()
                    .mechanism();

            assertThat(CartRouting.station().appliesTo(mechanism, settings)).isFalse();
        }

        @Test
        void followsTheBlockAnOperatorChose() {
            Settings settings = Settings.builder()
                    .carts(CartSettings.DEFAULTS.withBlock("station", Blocks.key("bedrock")))
                    .build();
            var onBedrock = SimpleCartVisit.at(RAIL, "bedrock", BlockFace.SOUTH, "", "[Station]", "", "")
                    .build()
                    .mechanism();

            assertThat(CartRouting.station().appliesTo(onBedrock, settings)).isTrue();
        }

        @Test
        void refusesAMechanicThatWantsASignAndHasNone() {
            var mechanism = SimpleCartVisit.at(RAIL, "obsidian").build().mechanism();

            assertThat(CartRouting.station().appliesTo(mechanism, Settings.DEFAULTS)).isFalse();
        }
    }
}
