package com.xeonproductions.craftbookultimate.core.cart.mechanic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.cart.Cart;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartType;
import com.xeonproductions.craftbookultimate.core.cart.RailShape;
import com.xeonproductions.craftbookultimate.core.cart.SimpleCart;
import com.xeonproductions.craftbookultimate.core.cart.SimpleCartWorld;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The dispenser")
class CartDispensersTest {

    private static final Vec3i CHEST = new Vec3i(0, 64, 0);
    private static final Vec3i SIGN = new Vec3i(0, 63, 0);
    private static final Key MINECART = Blocks.key("minecart");
    private static final Key CHEST_MINECART = Blocks.key("chest_minecart");

    /**
     * A dispenser facing south, so it puts vehicles out to the north of the chest.
     *
     * @param lines what the sign says
     */
    private static CartDispensers.Site site(SimpleStockpile stock, String... lines) {
        CartMechanism.MechanismSign sign =
                new CartMechanism.MechanismSign(SIGN, SignLines.of(lines), BlockFace.SOUTH);
        return new CartDispensers.Site(sign, CHEST, stock);
    }

    /** A world with rail laid one block north of the chest, which is where a vehicle lands. */
    private static SimpleCartWorld withTrack() {
        return new SimpleCartWorld().withRail(CHEST.offset(BlockFace.NORTH), RailShape.NORTH_SOUTH);
    }

    @Nested
    @DisplayName("handing a vehicle out")
    class HandingAVehicleOut {

        @Test
        void putsACartOnTheTrackBehindTheSign() {
            SimpleStockpile stock = SimpleStockpile.empty().with(MINECART, 3);
            SimpleCartWorld world = withTrack();

            Optional<Cart> put = CartDispensers.dispense(
                    site(stock, "", "[Dispenser]", "", ""), world, Settings.DEFAULTS);

            assertThat(put).isPresent();
            assertThat(put.get().type()).isEqualTo(CartType.RIDEABLE);
            assertThat(stock.count(MINECART)).isEqualTo(2);
        }

        @Test
        void handsOutTheKindItsSignNames() {
            SimpleStockpile stock = SimpleStockpile.empty()
                    .with(MINECART, 1)
                    .with(CHEST_MINECART, 1);
            SimpleCartWorld world = withTrack();

            Optional<Cart> put = CartDispensers.dispense(
                    site(stock, "", "[Dispenser]", "storage", ""), world, Settings.DEFAULTS);

            assertThat(put).isPresent();
            assertThat(put.get().type()).isEqualTo(CartType.CHEST);
            assertThat(stock.count(MINECART)).isEqualTo(1);
        }

        @Test
        void handsOutWhateverItHasWhenItsSignDoesNotSay() {
            SimpleStockpile stock = SimpleStockpile.empty().with(CHEST_MINECART, 1);
            SimpleCartWorld world = withTrack();

            Optional<Cart> put = CartDispensers.dispense(
                    site(stock, "", "[Dispenser]", "", ""), world, Settings.DEFAULTS);

            assertThat(put).isPresent();
            assertThat(put.get().type()).isEqualTo(CartType.CHEST);
        }

        @Test
        void sendsTheCartOffWhenItsSignSaysToPush() {
            SimpleStockpile stock = SimpleStockpile.empty().with(MINECART, 1);
            SimpleCartWorld world = withTrack();

            Optional<Cart> put = CartDispensers.dispense(
                    site(stock, "", "[Dispenser]", "", "push"), world, Settings.DEFAULTS);

            assertThat(put).isPresent();
            assertThat(put.get().velocity().z()).isEqualTo(-1.0);
        }

        @Test
        void leavesTheCartStandingWhenItsSignDoesNot() {
            SimpleStockpile stock = SimpleStockpile.empty().with(MINECART, 1);
            SimpleCartWorld world = withTrack();

            Optional<Cart> put = CartDispensers.dispense(
                    site(stock, "", "[Dispenser]", "", ""), world, Settings.DEFAULTS);

            assertThat(put.orElseThrow().speed()).isZero();
        }

        @Test
        void handsOutNothingWithAnEmptyChest() {
            SimpleStockpile stock = SimpleStockpile.empty();

            assertThat(CartDispensers.dispense(
                    site(stock, "", "[Dispenser]", "", ""), withTrack(), Settings.DEFAULTS))
                    .isEmpty();
        }

        @Test
        void handsOutNothingWithNoTrackToPutItOn() {
            SimpleStockpile stock = SimpleStockpile.empty().with(MINECART, 1);

            assertThat(CartDispensers.dispense(
                    site(stock, "", "[Dispenser]", "", ""), new SimpleCartWorld(), Settings.DEFAULTS))
                    .isEmpty();
            assertThat(stock.count(MINECART)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("taking a cart back")
    class TakingACartBack {

        @Test
        void putsTheCartBackInTheChest() {
            SimpleStockpile stock = SimpleStockpile.empty();
            SimpleCart cart = SimpleCart.storage();

            assertThat(CartDispensers.store(cart, stock, new SimpleCartWorld())).isTrue();
            assertThat(stock.count(CHEST_MINECART)).isEqualTo(1);
            assertThat(cart.isPresent()).isFalse();
        }

        @Test
        void dropsWhatAFullChestWillNotTake() {
            SimpleStockpile full = SimpleStockpile.withCapacity(0);
            SimpleCartWorld world = new SimpleCartWorld();

            assertThat(CartDispensers.store(SimpleCart.rideable(), full, world)).isTrue();
            assertThat(world.droppedItems()).hasSize(1);
            assertThat(world.droppedItems().getFirst().item()).isEqualTo(MINECART);
        }

        @Test
        void leavesACartItHasNoItemFor() {
            SimpleStockpile stock = SimpleStockpile.empty();
            SimpleCart spawner = SimpleCart.of(CartType.SPAWNER);

            assertThat(CartDispensers.store(spawner, stock, new SimpleCartWorld())).isFalse();
            assertThat(spawner.isPresent()).isTrue();
        }
    }

    @Test
    void knowsItsOwnSign() {
        CartMechanism.MechanismSign mine = new CartMechanism.MechanismSign(
                SIGN, SignLines.of("", "[Dispenser]", "", ""), BlockFace.SOUTH);
        CartMechanism.MechanismSign other = new CartMechanism.MechanismSign(
                SIGN, SignLines.of("", "[Station]", "", ""), BlockFace.SOUTH);

        assertThat(CartDispensers.isDispenser(mine, Settings.DEFAULTS)).isTrue();
        assertThat(CartDispensers.isDispenser(other, Settings.DEFAULTS)).isFalse();
    }
}
