package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.entity.SimpleRoster;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.message.SimpleAnnouncer;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("The chips that say something")
class MessagesTest {

    /** Where every chip in these tests hangs its sign. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    /** The block it hangs on, which is what its range is measured from. */
    private static final Vec3i BEHIND = new Vec3i(0, 64, -1);

    private final SimpleAnnouncer announcer = new SimpleAnnouncer();
    private final ChipServices services = ChipServices.create(SimpleRoster.empty(), announcer);
    private final SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState.Builder chip(String... lines) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .services(services)
                .world(world)
                .at(SIGN, BlockFace.SOUTH)
                .sign(lines);
    }

    /** Somebody standing a given number of blocks north of the block the sign hangs on. */
    private SimpleBystander standing(String name, double away) {
        SimpleBystander person =
                SimpleBystander.player(name).at(Vec3d.middleOf(BEHIND).add(0, 0, away));
        world.withBystander(person);
        return person;
    }

    @Nested
    @DisplayName("messaging one player")
    class MessagingOnePlayer {

        @Test
        void saysWhatTheSignSaysToTheNamedPlayer() {
            announcer.withOnline("Alice");
            SimpleChipState state = chip("", "[MC1510]", "Alice", "the gate is open")
                    .inputs(true, false, false)
                    .build();

            Messages.playerMessenger().trigger(state);

            assertThat(announcer.to("Alice")).containsExactly("the gate is open");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void reportsThatSomebodyOfflineHeardNothing() {
            SimpleChipState state = chip("", "[MC1510]", "Alice", "the gate is open")
                    .inputs(true, false, false)
                    .build();

            Messages.playerMessenger().trigger(state);

            assertThat(announcer.to("Alice")).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void saysNothingWithoutANameToSayItTo() {
            announcer.withOnline("Alice");
            SimpleChipState state =
                    chip("", "[MC1510]", "", "the gate is open").inputs(true, false, false).build();

            Messages.playerMessenger().trigger(state);

            assertThat(announcer.to("Alice")).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void saysNothingWhileNothingDrivesIt() {
            announcer.withOnline("Alice");
            SimpleChipState state = chip("", "[MC1510]", "Alice", "the gate is open")
                    .inputs(false, false, false)
                    .build();

            Messages.playerMessenger().trigger(state);

            assertThat(announcer.to("Alice")).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("messaging the server")
    class MessagingTheServer {

        @Test
        void saysWhatTheSignSaysToEverybody() {
            SimpleChipState state =
                    chip("", "[MC1511]", "the shop is open", "").inputs(true, false, false).build();

            Messages.messageAll().trigger(state);

            assertThat(announcer.everyone()).containsExactly("the shop is open");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void saysNothingWithNothingWrittenToSay() {
            SimpleChipState state = chip("", "[MC1511]", "", "").inputs(true, false, false).build();

            Messages.messageAll().trigger(state);

            assertThat(announcer.everyone()).isEmpty();
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void saysNothingWhileNothingDrivesIt() {
            SimpleChipState state = chip("", "[MC1511]", "the shop is open", "")
                    .inputs(false, false, false)
                    .build();

            Messages.messageAll().trigger(state);

            assertThat(announcer.everyone()).isEmpty();
        }
    }

    @Nested
    @DisplayName("messaging whoever is nearby")
    class MessagingWhoeverIsNearby {

        @Test
        void tellsEverybodyWithinRange() {
            SimpleBystander near = standing("Alice", 3);
            SimpleBystander alsoNear = standing("Bob", 6);
            SimpleChipState state =
                    chip("10", "[MCX512]", "mind the ", "gap").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);

            assertThat(near.plainMessages()).containsExactly("mind the gap");
            assertThat(alsoNear.plainMessages()).containsExactly("mind the gap");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void leavesOutSomebodyBeyondItsRange() {
            SimpleBystander far = standing("Alice", 20);
            SimpleChipState state =
                    chip("10", "[MCX512]", "mind the gap", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);

            assertThat(far.plainMessages()).isEmpty();
        }

        @Test
        void reachesSixtyFourBlocksWhenTheSignDoesNotSay() {
            SimpleBystander far = standing("Alice", 50);
            SimpleChipState state =
                    chip("", "[MCX512]", "mind the gap", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);

            assertThat(far.plainMessages()).containsExactly("mind the gap");
        }

        @ParameterizedTest(name = "line 1 reading \"{0}\"")
        @CsvSource({"MessageNearby10, 10", "'10', 10", "MessageNearby, 64", "'', 64"})
        void readsItsRangeFromTheEndOfTheFirstLine(String written, int expected) {
            SimpleChipState state = chip(written, "[MCX512]", "hello", "").build();

            assertThat(Messages.rangeOn(state)).isEqualTo(expected);
        }

        @Test
        void neverSpeaksToSomebodyWhoIsNotMeantToBeSeen() {
            SimpleBystander hidden = standing("Alice", 3).hidden();
            SimpleChipState state =
                    chip("10", "[MCX512]", "mind the gap", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);

            assertThat(hidden.plainMessages()).isEmpty();
        }

        @Test
        void readsAScriptOutOfABookWhenTheSignAsksItTo() {
            SimpleBystander near = standing("Alice", 3);
            world.withBook(BEHIND.add(0, 1, 0), List.of("first line\nsecond line"));
            SimpleChipState state =
                    chip("10", "[MCX512]B", "", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);

            assertThat(near.plainMessages()).containsExactly("first line", "second line");
        }

        @Test
        void looksForTheBookWhereTheSignSays() {
            SimpleBystander near = standing("Alice", 3);
            world.withBook(BEHIND.add(2, 0, 0), List.of("over there"));
            SimpleChipState state =
                    chip("10", "[MCX512]B", "2:0:0", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);

            assertThat(near.plainMessages()).containsExactly("over there");
        }

        @Test
        void holdsBackEverythingAfterAWaitUntilTheWaitIsOver() {
            SimpleBystander near = standing("Alice", 3);
            world.withBook(BEHIND.add(0, 1, 0), List.of("now\n[DELAY:2:S]\nlater"));
            SimpleChipState state =
                    chip("10", "[MCX512]B", "", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);
            assertThat(near.plainMessages()).containsExactly("now");

            state.manualScheduler().advance(40);
            assertThat(near.plainMessages()).containsExactly("now", "later");
        }

        @Test
        void keepsTheOrderOfLinesSpacedOutByDifferentWaits() {
            SimpleBystander near = standing("Alice", 3);
            world.withBook(BEHIND.add(0, 1, 0), List.of("[DELAY:40]\nsecond\n[DELAY:20]\nthird"));
            SimpleChipState state =
                    chip("10", "[MCX512]B", "", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);

            state.manualScheduler().advance(40);
            assertThat(near.plainMessages()).containsExactly("second");
            state.manualScheduler().advance(20);
            assertThat(near.plainMessages()).containsExactly("second", "third");
        }

        @Test
        void saysNothingLaterToSomebodyWhoHasGone() {
            SimpleBystander near = standing("Alice", 3);
            world.withBook(BEHIND.add(0, 1, 0), List.of("[DELAY:20]\nlater"));
            SimpleChipState state =
                    chip("10", "[MCX512]B", "", "").inputs(true, false, false).build();

            Messages.messageNearby().trigger(state);
            near.remove();
            state.manualScheduler().advance(20);

            assertThat(near.plainMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("naming the nearest player")
    class NamingTheNearestPlayer {

        @Test
        void putsTheNearestPlayersNameInWhatEverybodyIsTold() {
            SimpleBystander nearest = standing("Alice", 2);
            SimpleBystander other = standing("Bob", 8);
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .services(services)
                    .world(world)
                    .at(SIGN, BlockFace.SOUTH)
                    .sign("20", "[MCX513]", "welcome, %p", "")
                    .inputs(true, false, false, false)
                    .build();

            Messages.namedNearby().trigger(state);

            assertThat(nearest.plainMessages()).containsExactly("welcome, Alice");
            assertThat(other.plainMessages()).containsExactly("welcome, Alice");
        }

        @Test
        void breaksTheMessageWhereTheSignAsksItTo() {
            SimpleBystander near = standing("Alice", 2);
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .services(services)
                    .world(world)
                    .at(SIGN, BlockFace.SOUTH)
                    .sign("20", "[MCX513]", "one/ntwo", "")
                    .inputs(true, false, false, false)
                    .build();

            Messages.namedNearby().trigger(state);

            assertThat(near.plainMessages()).containsExactly("one\ntwo");
        }

        @Test
        void readsTheColourCodesTheBuilderWrote() {
            SimpleBystander near = standing("Alice", 2);
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .services(services)
                    .world(world)
                    .at(SIGN, BlockFace.SOUTH)
                    .sign("20", "[MCX513]", "&cdanger", "")
                    .inputs(true, false, false, false)
                    .build();

            Messages.namedNearby().trigger(state);

            assertThat(near.plainMessages()).containsExactly("danger");
            assertThat(near.messages()).hasSize(1);
            assertThat(near.messages().getFirst().color()).isNotNull();
        }

        @Test
        void saysNothingWithNobodyInRangeToName() {
            standing("Alice", 40);
            SimpleChipState state = SimpleChipState.forLayout(PinLayout.AISO)
                    .services(services)
                    .world(world)
                    .at(SIGN, BlockFace.SOUTH)
                    .sign("10", "[MCX513]", "welcome, %p", "")
                    .inputs(true, false, false, false)
                    .build();

            Messages.namedNearby().trigger(state);

            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("writing to the log")
    class WritingToTheLog {

        @Test
        void writesWhatTheSignSaysAcrossItsLastTwoLines() {
            SimpleChipState state =
                    chip("", "[MCX515]", "the gate ", "opened").inputs(true, false, false).build();

            Messages.serverLog().trigger(state);

            assertThat(announcer.log()).containsExactly("[CB!] the gate opened");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void namesTheNearestPlayer() {
            standing("Alice", 2);
            standing("Bob", 9);
            SimpleChipState state =
                    chip("20", "[MCX516]", "%p came through", "").inputs(true, false, false).build();

            Messages.serverLogNearby().trigger(state);

            assertThat(announcer.log()).containsExactly("[CB!] Alice came through");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void saysSoWhenThereWasNobodyToName() {
            SimpleChipState state =
                    chip("20", "[MCX516]", "%p came through", "").inputs(true, false, false).build();

            Messages.serverLogNearby().trigger(state);

            assertThat(announcer.log()).containsExactly("[CB!] [NONE_FOUND] came through");
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void keepsToItsRangeRatherThanTheFullSixtyFour() {
            standing("Alice", 30);
            SimpleChipState state =
                    chip("10", "[MCX516]", "%p came through", "").inputs(true, false, false).build();

            Messages.serverLogNearby().trigger(state);

            assertThat(announcer.log()).containsExactly("[CB!] [NONE_FOUND] came through");
        }

        @Test
        void neverNamesSomebodyWhoIsNotMeantToBeSeen() {
            standing("Alice", 2).hidden();
            SimpleChipState state =
                    chip("20", "[MCX516]", "%p came through", "").inputs(true, false, false).build();

            Messages.serverLogNearby().trigger(state);

            assertThat(announcer.log()).containsExactly("[CB!] [NONE_FOUND] came through");
        }

        @Test
        void tellsThePlayerItNamedWhenTheSignAsksItTo() {
            SimpleBystander near = standing("Alice", 2);
            SimpleChipState state = chip("20", "[MCX516]+", "%p came through", "")
                    .mode(ICMode.parse("+"))
                    .inputs(true, false, false)
                    .build();

            Messages.serverLogNearby().trigger(state);

            assertThat(near.plainMessages()).containsExactly("[CB!] Alice came through");
        }

        @Test
        void keepsQuietWhenTheSignDoesNotAskItToTellAnybody() {
            SimpleBystander near = standing("Alice", 2);
            SimpleChipState state =
                    chip("20", "[MCX516]", "%p came through", "").inputs(true, false, false).build();

            Messages.serverLogNearby().trigger(state);

            assertThat(near.plainMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("writing to the log with everybody in range")
    class WritingToTheLogWithEverybodyInRange {

        @Test
        void listsEverybodyWithHowFarOffTheyAre() {
            standing("Alice", 2);
            standing("Bob", 4);
            SimpleChipState state =
                    chip("20", "[MCX517]", "%a", "").inputs(true, false, false).build();

            Messages.serverLogNearbyPlus().trigger(state);

            assertThat(announcer.log())
                    .containsExactly("[CB!] In range players: Alice distance: 2.0 Bob distance: 4.0");
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void writesTheNearestPlayerWithTheirDistanceToo() {
            standing("Alice", 2);
            standing("Bob", 4);
            SimpleChipState state =
                    chip("20", "[MCX517]", "%p", "").inputs(true, false, false).build();

            Messages.serverLogNearbyPlus().trigger(state);

            assertThat(announcer.log()).containsExactly("[CB!] In range players: Alice distance: 2.0");
        }

        @Test
        void saysSoWhenThereWasNobodyInRangeAtAll() {
            SimpleChipState state =
                    chip("20", "[MCX517]", "%p saw %a", "").inputs(true, false, false).build();

            Messages.serverLogNearbyPlus().trigger(state);

            assertThat(announcer.log()).containsExactly("[CB!] [NONE_FOUND] saw [NONE_FOUND]");
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void tellsEverybodyItNamedWhenTheSignAsksItTo() {
            SimpleBystander alice = standing("Alice", 2);
            SimpleBystander bob = standing("Bob", 4);
            SimpleChipState state = chip("20", "[MCX517]+", "%p", "")
                    .mode(ICMode.parse("+"))
                    .inputs(true, false, false)
                    .build();

            Messages.serverLogNearbyPlus().trigger(state);

            assertThat(alice.plainMessages()).containsExactly("[CB!] In range players: Alice distance: 2.0");
            assertThat(bob.plainMessages()).containsExactly("[CB!] In range players: Alice distance: 2.0");
        }
    }
}
