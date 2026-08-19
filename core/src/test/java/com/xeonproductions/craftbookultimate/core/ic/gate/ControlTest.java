package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Chips that hold state of their own")
class ControlTest {

    private final ChipServices services = ChipServices.create();

    @Nested
    @DisplayName("command controlled")
    class CommandControlled {

        private SimpleChipState.Builder chip(String name) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .services(services)
                    .sign("COMMAND CTRL", "[MCX120]", name, "");
        }

        @Test
        void claimsItsSwitchAsItLoads() {
            SimpleChipState state = chip("door").build();

            Control.commandControlled().load(state);

            assertThat(services.switchboard().isKnown("door")).isTrue();
        }

        @Test
        void followsWhereTheSwitchIsThrown() {
            SimpleChipState state = chip("door").build();
            SelfTriggeringICLogic logic = Control.commandControlled();
            logic.load(state);

            services.switchboard().set("door", true);
            logic.tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void followsItWhateverItsInputsAreDoing() {
            // Nothing in the world moves this switch, so the chip needs no input to read it.
            SimpleChipState state = chip("door").inputs(false, false, false).build();
            SelfTriggeringICLogic logic = Control.commandControlled();
            logic.load(state);

            services.switchboard().set("door", true);
            logic.trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void leavesItsOutputAloneUntilTheSwitchIsFirstThrown() {
            SimpleChipState state = chip("door").build().withRawOutput(0, true);
            SelfTriggeringICLogic logic = Control.commandControlled();

            logic.load(state);
            logic.tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void givesUpItsSwitchAsItUnloads() {
            SimpleChipState state = chip("door").build();
            SelfTriggeringICLogic logic = Control.commandControlled();
            logic.load(state);

            logic.unload(state);

            assertThat(services.switchboard().isKnown("door")).isFalse();
        }

        @Test
        void claimsNothingWithoutANameOnItsSign() {
            SimpleChipState state = chip("").build();

            Control.commandControlled().load(state);

            assertThat(services.switchboard().size()).isZero();
        }

        @Test
        void doesNotShareASwitchWithTheGuardedKind() {
            SimpleChipState open = chip("door").build();
            SimpleChipState guarded = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .services(services)
                    .sign("PASSWORD CTRL", "[MCX121]", "door", "")
                    .build()
                    .withRawOutput(0, false);

            Control.commandControlled().load(open);
            Control.passwordControlled().load(guarded);

            services.switchboard().set("door", true);
            Control.passwordControlled().tick(guarded);

            assertThat(guarded.output(0)).isFalse();
        }
    }

    @Nested
    @DisplayName("bit shift")
    class BitShift {

        private SimpleChipState.Builder chip(String size, String saved) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .sign("BITSHIFT", "[MC2022]", size, saved);
        }

        @Test
        void showsItsFirstBitOnEveryTick() {
            SimpleChipState state = chip("4", "").build();
            SelfTriggeringICLogic logic = Control.bitShift();
            logic.load(state);

            // Write a one into the first bit: pulse the shift pin with the write pin held.
            logic.trigger(state.withInputs(true, true, true).withTriggeredInput(0));
            logic.tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void rotatesTheRowAlongCarryingTheFirstBitToTheEnd() {
            SimpleChipState state = chip("2", "").build();
            SelfTriggeringICLogic logic = Control.bitShift();
            logic.load(state);

            logic.trigger(state.withInputs(true, true, true).withTriggeredInput(0));
            logic.trigger(state.withInputs(true, true, false).withTriggeredInput(1));
            logic.tick(state);
            assertThat(state.output(0)).isFalse();

            // Two bits, so rotating twice brings the one back round to the front.
            logic.trigger(state.withInputs(true, true, false).withTriggeredInput(1));
            logic.tick(state);
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void keepsItsBitsOnItsSignWhenItUnloads() {
            SimpleChipState state = chip("8", "").build();
            SelfTriggeringICLogic logic = Control.bitShift();
            logic.load(state);
            logic.trigger(state.withInputs(true, true, true).withTriggeredInput(0));

            logic.unload(state);

            assertThat(state.sign().trimmedText(3)).isNotEmpty();
        }

        @Test
        void readsBackTheBitsItSaved() {
            SimpleChipState first = chip("8", "").build();
            SelfTriggeringICLogic logic = Control.bitShift();
            logic.load(first);
            logic.trigger(first.withInputs(true, true, true).withTriggeredInput(0));
            logic.unload(first);

            SimpleChipState reloaded = chip("8", first.sign().trimmedText(3)).build();
            SelfTriggeringICLogic other = Control.bitShift();
            other.load(reloaded);
            other.tick(reloaded);

            assertThat(reloaded.output(0)).isTrue();
        }

        @Test
        void fallsBackToEightBitsForAnUnreadableSize() {
            SimpleChipState state = chip("nonsense", "").build();

            Control.bitShift().load(state);

            // Nothing to assert on directly beyond it working at all rather than failing to load.
            Control.bitShift().tick(state);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ticksWhetherOrNotItsSignAsksItTo() {
            assertThat(Control.bitShift().alwaysSelfTriggering()).isTrue();
        }
    }

    @Nested
    @DisplayName("monoflop")
    class Monoflop {

        private SimpleChipState.Builder chip(String settings) {
            return SimpleChipState.forLayout(PinLayout.AISO)
                    .sign("^MONOFLOP", "[MCU440]", settings, "");
        }

        /** Steps the timer far enough for one count to pass. */
        private void advance(SelfTriggeringICLogic logic, SimpleChipState state, int counts, int rate) {
            for (int i = 0; i < counts * (rate + 1); i++) {
                logic.tick(state);
            }
        }

        @Test
        void staysOffUntilItHasCountedDown() {
            SimpleChipState state = chip("2:5").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 1, 5);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void turnsOnOnceTheCountdownIsDone() {
            SimpleChipState state = chip("2:5").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 3, 5);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void staysOnUntilItIsStartedAgainWithNoOnCount() {
            SimpleChipState state = chip("1:5").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 10, 5);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void turnsOffAgainAfterItsOnCount() {
            SimpleChipState state = chip("1:5:2").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 2, 5);
            assertThat(state.output(0)).isTrue();

            advance(logic, state, 3, 5);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void takesItsOnCountFromTheCycleOffMode() {
            SimpleChipState state = chip("1:5")
                    .inputs(true, false, false, false)
                    .mode(ICMode.parse("1"))
                    .build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 1, 5);
            assertThat(state.output(0)).isTrue();

            // The mode is shorthand for staying on a single count, so the next one ends it.
            advance(logic, state, 1, 5);
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresBeingDrivenWhileItIsAlreadyRunning() {
            SimpleChipState state = chip("3:5").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 2, 5);
            logic.trigger(state);
            advance(logic, state, 2, 5);

            // Restarting would have pushed the finish out past this point.
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void showsOnItsTitleLineWhetherItIsRunning() {
            SimpleChipState state = chip("1:5").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.load(state);
            assertThat(state.sign().trimmedText(0)).isEqualTo("^MONOFLOP");

            logic.trigger(state);
            assertThat(state.sign().trimmedText(0)).isEqualTo("%MONOFLOP");

            advance(logic, state, 2, 5);
            assertThat(state.sign().trimmedText(0)).isEqualTo("^MONOFLOP");
        }

        @Test
        void ignoresASettingsLineItCannotUse() {
            SimpleChipState state = chip("nonsense").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 10, 5);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void refusesACountRateOutsideWhatTheHardwareAllows() {
            SimpleChipState state = chip("1:1").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 10, 1);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresAFourthFieldLeftBehindByAnOlderSign() {
            SimpleChipState state = chip("1:5:2:1").inputs(true, false, false, false).build();
            SelfTriggeringICLogic logic = Control.monoflop();

            logic.trigger(state);
            advance(logic, state, 2, 5);

            assertThat(state.output(0)).isTrue();
        }
    }

    @Nested
    @DisplayName("trigger reader")
    class TriggerReader {

        private static final Vec3i SIGN = new Vec3i(0, 64, 0);

        private SimpleChipState.Builder chip(SimpleChipWorld world, String target) {
            return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .at(SIGN, BlockFace.SOUTH)
                    .world(world)
                    .sign("TRIGGER READER", "[MCX295]", target, "");
        }

        @Test
        void showsThePowerAtWhereItIsPointed() {
            Vec3i target = SIGN.add(10, 0, -20);
            SimpleChipWorld world = new SimpleChipWorld().withPowered(target);
            SimpleChipState state = chip(world, "10:0:-20").build();

            Control.triggerReader().tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void showsThatSomewhereUnpoweredIsUnpowered() {
            SimpleChipState state =
                    chip(new SimpleChipWorld(), "10:0:-20").build().withRawOutput(0, true);

            Control.triggerReader().tick(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void showsTheOppositeWhenAskedTo() {
            SimpleChipWorld world = new SimpleChipWorld().withPowered(SIGN.add(1, 0, 0));
            SimpleChipState state = chip(world, "!1:0:0").build().withRawOutput(0, true);

            Control.triggerReader().tick(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void needsItsInputWhenItIsNotTicking() {
            SimpleChipWorld world = new SimpleChipWorld().withPowered(SIGN.add(1, 0, 0));
            SimpleChipState state = chip(world, "1:0:0").inputs(false, false, false).build();

            Control.triggerReader().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void readsOnThePulseWhenItIsNotTicking() {
            SimpleChipWorld world = new SimpleChipWorld().withPowered(SIGN.add(1, 0, 0));
            SimpleChipState state = chip(world, "1:0:0").inputs(true, false, false).build();

            Control.triggerReader().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void leavesItsOutputAloneWhereItCannotRead() {
            // Somewhere unloaded, or belonging to a thread that is not this one.
            Vec3i target = SIGN.add(0, 0, -20);
            SimpleChipWorld world = new SimpleChipWorld().withUnreadable(target);
            SimpleChipState state = chip(world, "0:0:-20").build().withRawOutput(0, true);

            Control.triggerReader().tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void refusesATargetFurtherThanItMayReach() {
            SimpleChipWorld world = new SimpleChipWorld().withPowered(SIGN.add(1000, 0, 0));
            SimpleChipState state = chip(world, "1000:0:0").build().withRawOutput(0, false);

            Control.triggerReader().tick(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void ignoresATargetItCannotRead() {
            SimpleChipState state = chip(new SimpleChipWorld(), "over there")
                    .build()
                    .withRawOutput(0, true);

            Control.triggerReader().tick(state);

            assertThat(state.output(0)).isTrue();
        }
    }
}
