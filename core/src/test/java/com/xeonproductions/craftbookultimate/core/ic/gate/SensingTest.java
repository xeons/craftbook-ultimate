// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.entity.SimpleDroppedItem;
import com.xeonproductions.craftbookultimate.core.entity.SimpleRoster;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Sensing chips")
class SensingTest {

    /** A south-facing sign, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND = SIGN.offset(BlockFace.NORTH);

    /** The first place above the sign's support that somebody could stand. */
    private static final Vec3d STANDING = Vec3d.centreOf(BEHIND);

    private final SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState.Builder chip(String model, String third, String fourth) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .sign("SENSOR", "[" + model + "]", third, fourth)
                .inputs(true, false, false);
    }

    @Nested
    @DisplayName("mob above")
    class MobAbove {

        @Test
        void reportsACreatureStandingOnTheSupport() {
            world.withBystander(SimpleBystander.monster("zombie").at(STANDING));
            SimpleChipState state = chip("MCM116", "", "").build();

            Sensing.mobAbove().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void reportsNothingWhenTheBlockIsEmpty() {
            SimpleChipState state = chip("MCM116", "", "").build();

            Sensing.mobAbove().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void reportsOnlyWhatItsSignNames() {
            world.withBystander(SimpleBystander.monster("zombie").at(STANDING));
            SimpleChipState state = chip("MCM116", "creeper", "").build();

            Sensing.mobAbove().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void reportsAnimalsAsWellAsMonstersWhenItsSignIsBlank() {
            world.withBystander(SimpleBystander.animal("cow").at(STANDING));
            SimpleChipState state = chip("MCM116", "", "").build();

            Sensing.mobAbove().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void looksAboveWhateverIsStackedOnTheSupport() {
            world.withBlock(BEHIND, "stone");
            world.withBystander(SimpleBystander.monster("zombie").at(Vec3d.centreOf(BEHIND.add(0, 1, 0))));
            SimpleChipState state = chip("MCM116", "", "").build();

            Sensing.mobAbove().trigger(state);

            assertThat(state.output(0)).isTrue();
        }
    }

    @Nested
    @DisplayName("player above")
    class PlayerAbove {

        @Test
        void reportsSomebodyStandingOverIt() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING));
            SimpleChipState state = chip("MCX116", "", "").build();

            Sensing.playerAbove().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ignoresCreaturesThatAreNotPlayers() {
            world.withBystander(SimpleBystander.monster("zombie").at(STANDING));
            SimpleChipState state = chip("MCX116", "", "").build();

            Sensing.playerAbove().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresSomebodyWhoIsNotReallyThere() {
            // A spectator walks through walls and a vanished player is meant to be unseen.
            world.withBystander(SimpleBystander.player("Notch").at(STANDING).hidden());
            SimpleChipState state = chip("MCX116", "", "").build();

            Sensing.playerAbove().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void picksOneNamedPlayerOut() {
            world.withBystander(SimpleBystander.player("Herobrine").at(STANDING));
            SimpleChipState state = chip("MCX116", "p:Notch", "").build();

            Sensing.playerAbove().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void reachesAsFarAcrossAsItsFourthLineSays() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING.add(3, 0, 0)));
            SimpleChipState state = chip("MCX116", "", "4").build();

            Sensing.playerAbove().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void keepsToItsOwnColumnWhenItsSignIsBlank() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING.add(3, 0, 0)));
            SimpleChipState state = chip("MCX116", "", "").build();

            Sensing.playerAbove().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void needsRoomForSomebodyToStandRatherThanJustAGap() {
            world.withBlock(BEHIND.add(0, 1, 0), "stone");
            world.withBystander(SimpleBystander.player("Notch").at(STANDING));
            SimpleChipState state = chip("MCX116", "", "").build();

            Sensing.playerAbove().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("player below")
    class PlayerBelow {

        @Test
        void reportsSomebodyStandingUnderIt() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING.add(0, -3, 0)));
            SimpleChipState state = chip("MCX117", "", "").build();

            Sensing.playerBelow().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ignoresSomebodyStandingOverIt() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING));
            SimpleChipState state = chip("MCX117", "", "").build();

            Sensing.playerBelow().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void reachesAsDeepAsItsFourthLineSays() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING.add(0, -8, 0)));
            SimpleChipState state = chip("MCX117", "", "1:6").build();

            Sensing.playerBelow().trigger(state);

            assertThat(state.output(0)).isTrue();
        }
    }

    @Nested
    @DisplayName("player near")
    class PlayerNear {

        @Test
        void reportsSomebodyWithinRange() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING.add(3, 0, 0)));
            SimpleChipState state = chip("MCX118", "", "").build();

            Sensing.playerNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ignoresSomebodyBeyondIt() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING.add(30, 0, 0)));
            SimpleChipState state = chip("MCX118", "", "5").build();

            Sensing.playerNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void reachesAsFarAsItsFourthLineSays() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING.add(30, 0, 0)));
            SimpleChipState state = chip("MCX118", "", "40").build();

            Sensing.playerNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void matchesAPermissionGroup() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING).inGroup("admin"));
            SimpleChipState state = chip("MCX118", "g:admin", "").build();

            Sensing.playerNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void readsNothingWhileNothingDrivesIt() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING));
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("SENSOR", "[MCX118]", "", "")
                    .build();

            Sensing.playerNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void readsOnEveryTickWhenItTicks() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING));
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("SENSOR", "[MCZ118]", "", "")
                    .build();

            Sensing.playerNear().tick(state);

            assertThat(state.output(0)).isTrue();
        }
    }

    @Nested
    @DisplayName("mob near")
    class MobNear {

        @Test
        void reportsEitherAMonsterOrAnAnimalWhenItsSignIsBlank() {
            world.withBystander(SimpleBystander.animal("cow").at(Vec3d.middleOf(SIGN).add(2, 0, 0)));
            SimpleChipState state = chip("MCX119", "", "").build();

            Sensing.mobNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void reportsOnlyMonstersWhenAskedForThem() {
            world.withBystander(SimpleBystander.animal("cow").at(Vec3d.middleOf(SIGN).add(2, 0, 0)));
            SimpleChipState state = chip("MCX119", "mobs", "").build();

            Sensing.mobNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresPlayers() {
            world.withBystander(SimpleBystander.player("Notch").at(Vec3d.middleOf(SIGN).add(2, 0, 0)));
            SimpleChipState state = chip("MCX119", "", "").build();

            Sensing.mobNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("item not near")
    class ItemNotNear {

        @Test
        void reportsHighWhenNothingMatchingIsLyingNearby() {
            SimpleChipState state = chip("MC1265", "ID:diamond", "").build();

            Sensing.itemNotNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void reportsLowWhenSomethingMatchingIs() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("diamond", 1));
            SimpleChipState state = chip("MC1265", "ID:diamond", "").build();

            Sensing.itemNotNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresAStackOfSomethingElse() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("dirt", 1));
            SimpleChipState state = chip("MC1265", "ID:diamond", "").build();

            Sensing.itemNotNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void isTheOppositeOfTheItemSensorOnTheSameSign() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("diamond", 1));

            SimpleChipState found = chip("MCX138", "ID:diamond", "").build();
            SimpleChipState notFound = chip("MC1265", "ID:diamond", "").build();

            Sensing.itemNear().trigger(found);
            Sensing.itemNotNear().trigger(notFound);

            assertThat(notFound.output(0)).isNotEqualTo(found.output(0));
        }

        @Test
        void staysLowOnALineItCannotRead() {
            // Not high: a sensor that cannot tell has not found an empty world.
            SimpleChipState state = chip("MC1265", "item:diamond", "").build();

            Sensing.itemNotNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("movement sensor")
    class MovementSensor {

        @Test
        void reportsSomethingGoingSomewhere() {
            world.withBystander(SimpleBystander.monster("zombie").at(STANDING).moving());
            SimpleChipState state = chip("MC1267", "", "").build();

            Sensing.movementNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ignoresSomethingStandingStill() {
            world.withBystander(SimpleBystander.monster("zombie").at(STANDING));
            SimpleChipState state = chip("MC1267", "", "").build();

            Sensing.movementNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void takesWhatCountsOffLineThree() {
            world.withBystander(SimpleBystander.monster("zombie").at(STANDING).moving());
            SimpleChipState state = chip("MC1267", "cow", "").build();

            Sensing.movementNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void takesHowFarOffLineFour() {
            world.withBystander(
                    SimpleBystander.monster("zombie")
                            .at(Vec3d.centreOf(BEHIND).add(8, 0, 0))
                            .moving());

            SimpleChipState near = chip("MC1267", "", "2").build();
            SimpleChipState far = chip("MC1267", "", "10").build();

            Sensing.movementNear().trigger(near);
            Sensing.movementNear().trigger(far);

            assertThat(near.output(0)).isFalse();
            assertThat(far.output(0)).isTrue();
        }

        @Test
        void needsItsInputWhenItIsNotTicking() {
            world.withBystander(SimpleBystander.monster("zombie").at(STANDING).moving());
            SimpleChipState state =
                    chip("MC1267", "", "").inputs(false, false, false).build();

            Sensing.movementNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("item near")
    class ItemNear {

        @Test
        void reportsAMatchingStackLyingNearby() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("diamond", 1));
            SimpleChipState state = chip("MCX138", "ID:diamond", "").build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ignoresAStackOfSomethingElse() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("dirt", 1));
            SimpleChipState state = chip("MCX138", "ID:diamond", "").build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void checksHowManyAreInTheStack() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("diamond", 3));
            SimpleChipState state = chip("MCX138", "STACK:64", "").build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void checksWhatIsWrittenOnIt() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("paper", 1).withLore("part of a quest"));
            SimpleChipState state = chip("MCX138", "LORE:quest", "").build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void reportsNothingWhenItsSignSaysNothingUsable() {
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("diamond", 1));
            SimpleChipState state = chip("MCX138", "", "").build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void requiresEveryCheckInABookAtOnce() {
            world.withBook(BEHIND.add(0, 1, 0), List.of("ID:diamond\nSTACK:3"));
            world.withDroppedItem(BEHIND, SimpleDroppedItem.of("diamond", 3));
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("SENSOR", "[MCX138]B", "", "")
                    .inputs(true, false, false)
                    .build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void reachesAsFarAsItsFourthLineSays() {
            world.withDroppedItem(BEHIND.add(20, 0, 0), SimpleDroppedItem.of("diamond", 1));
            SimpleChipState state = chip("MCX138", "ID:diamond", "25").build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void keepsToItsRangeRatherThanIgnoringIt() {
            // The old chip took its range only when the number was out of bounds, so a sensible
            // one on the sign was thrown away and the default of five used instead.
            world.withDroppedItem(BEHIND.add(20, 0, 0), SimpleDroppedItem.of("diamond", 1));
            SimpleChipState state = chip("MCX138", "ID:diamond", "10").build();

            Sensing.itemNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("held item near")
    class HeldItemNear {

        @Test
        void reportsSomebodyHoldingWhatItsSignNames() {
            world.withBystander(SimpleBystander.player("Notch")
                    .at(STANDING)
                    .holding(ItemView.of(Blocks.key("diamond_sword"), 1)));
            SimpleChipState state = chip("MCX139", "ID:diamond_sword", "").build();

            Sensing.heldItemNear().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ignoresSomebodyHoldingSomethingElse() {
            world.withBystander(SimpleBystander.player("Notch")
                    .at(STANDING)
                    .holding(ItemView.of(Blocks.key("stick"), 1)));
            SimpleChipState state = chip("MCX139", "ID:diamond_sword", "").build();

            Sensing.heldItemNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresSomebodyHoldingNothing() {
            world.withBystander(SimpleBystander.player("Notch").at(STANDING));
            SimpleChipState state = chip("MCX139", "ID:diamond_sword", "").build();

            Sensing.heldItemNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresACreatureHoldingTheRightThing() {
            world.withBystander(SimpleBystander.monster("zombie")
                    .at(STANDING)
                    .holding(ItemView.of(Blocks.key("diamond_sword"), 1)));
            SimpleChipState state = chip("MCX139", "ID:diamond_sword", "").build();

            Sensing.heldItemNear().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("in area")
    class InArea {

        @Test
        void reportsSomethingInsideTheBox() {
            world.withBystander(SimpleBystander.animal("pig").at(Vec3d.middleOf(SIGN).add(0, 1, 0)));
            SimpleChipState state = chip("MCX140", "pig", "").build();

            Sensing.inArea().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ignoresSomethingOutsideIt() {
            world.withBystander(SimpleBystander.animal("pig").at(Vec3d.middleOf(SIGN).add(10, 1, 0)));
            SimpleChipState state = chip("MCX140", "pig", "").build();

            Sensing.inArea().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void findsSomethingInTheCornerOfTheBox() {
            // The old chip narrowed its search to a ball half the size of the box it then filtered
            // against, so anything out towards a corner was never looked at.
            world.withBystander(SimpleBystander.animal("pig").at(Vec3d.middleOf(SIGN).add(15, 0, 15)));
            SimpleChipState state = chip("MCX140", "pig", "16:16:16/0:0:0").build();

            Sensing.inArea().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void requiresTheRiderItsSignNames() {
            world.withBystander(SimpleBystander.animal("pig").at(Vec3d.middleOf(SIGN).add(0, 1, 0)));
            SimpleChipState state = chip("MCX140", "pig+player", "").build();

            Sensing.inArea().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void reportsSomebodyRidingWhatItsSignNames() {
            world.withBystander(SimpleBystander.animal("pig")
                    .at(Vec3d.middleOf(SIGN).add(0, 1, 0))
                    .carrying(SimpleBystander.player("Notch")));
            SimpleChipState state = chip("MCX140", "pig+player", "").build();

            Sensing.inArea().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void movesItsBoxWhereTheOffsetSays() {
            world.withBystander(SimpleBystander.animal("pig").at(Vec3d.middleOf(SIGN).add(8, 0, 0)));
            SimpleChipState state = chip("MCX140", "pig", "2:2:2/8:0:0").build();

            Sensing.inArea().trigger(state);

            assertThat(state.output(0)).isTrue();
        }
    }

    @Nested
    @DisplayName("player online")
    class PlayerOnline {

        private SimpleChipState onlineChip(String wanted, String... names) {
            ChipServices services = ChipServices.create(SimpleRoster.of(names));
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .services(services)
                    .sign("SENSOR", "[MC1500]", wanted, "")
                    .inputs(true, false, false)
                    .build();
        }

        @Test
        void reportsANamedPlayerBeingLoggedIn() {
            SimpleChipState state = onlineChip("Notch", "Notch", "Herobrine");

            Sensing.playerOnline().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void reportsNothingWhenNobodyMatches() {
            SimpleChipState state = onlineChip("Notch", "Herobrine");

            Sensing.playerOnline().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void matchesPartOfAName() {
            SimpleChipState state = onlineChip("otc", "Notch");

            Sensing.playerOnline().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void reportsNothingWhenItsSignNamesNobody() {
            SimpleChipState state = onlineChip("", "Notch");

            Sensing.playerOnline().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }
}
