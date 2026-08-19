package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.radio.Band;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Wireless chips")
class WirelessTest {

    private final ChipServices services = ChipServices.create();

    private SimpleChipState.Builder transmitter(String narrow, String wide) {
        return SimpleChipState.forLayout(PinLayout.AIZO)
                .services(services)
                .sign("", "[MC1110]", narrow, wide);
    }

    private SimpleChipState.Builder receiver(String narrow, String wide) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .services(services)
                .sign("", "[MC1111]", narrow, wide);
    }

    private SimpleChipState.Builder analog(String settings, String wide) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .services(services)
                .sign("", "[MC6543]", settings, wide);
    }

    @Nested
    @DisplayName("transmitting")
    class Transmitting {

        @Test
        void raisesItsBandWhileItIsDriven() {
            SimpleChipState state = transmitter("door", "").inputs(true, false, false).build();

            Wireless.transmitter().trigger(state);

            assertThat(services.radio().signal(Band.named("door"))).contains(true);
        }

        @Test
        void lowersItsBandWhenNothingDrivesIt() {
            services.radio().transmit(Band.named("door"), true);
            SimpleChipState state = transmitter("door", "").inputs(false, false, false).build();

            Wireless.transmitter().trigger(state);

            assertThat(services.radio().signal(Band.named("door"))).contains(false);
        }

        @Test
        void countsAPowerSourceBehindTheSignAsBeingDriven() {
            SimpleChipState state = transmitter("door", "")
                    .inputs(false, false, false)
                    .powerSourceBehind(true)
                    .build();

            Wireless.transmitter().trigger(state);

            assertThat(services.radio().signal(Band.named("door"))).contains(true);
        }

        @Test
        void keepsTheNamespaceSeparateFromTheChannel() {
            SimpleChipState mine = transmitter("door", "alice").inputs(true, false, false).build();

            Wireless.transmitter().trigger(mine);

            assertThat(services.radio().signal(new Band("alice", "door"))).contains(true);
            assertThat(services.radio().signal(Band.named("door"))).isEmpty();
        }

        @ParameterizedTest(name = "channel \"{0}\"")
        @ValueSource(strings = {"", "   "})
        void saysNothingWithoutAChannelName(String channel) {
            SimpleChipState state = transmitter(channel, "").inputs(true, false, false).build();

            Wireless.transmitter().trigger(state);

            assertThat(services.radio().bandCount()).isZero();
        }
    }

    @Nested
    @DisplayName("receiving")
    class Receiving {

        @Test
        void followsItsBandOnEveryTick() {
            services.radio().transmit(Band.named("door"), true);
            SimpleChipState state = receiver("door", "").inputs(false, false, false).build();

            Wireless.receiver().tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void ticksRegardlessOfItsInputs() {
            services.radio().transmit(Band.named("door"), true);
            SimpleChipState state = receiver("door", "")
                    .inputs(false, false, false)
                    .connected(false, false, false)
                    .build();

            Wireless.receiver().tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void needsItsClockWhenItIsNotTicking() {
            services.radio().transmit(Band.named("door"), true);
            SimpleChipState state = receiver("door", "").inputs(false, false, false).build();

            Wireless.receiver().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void readsItsBandWhenItsClockFires() {
            services.radio().transmit(Band.named("door"), true);
            SimpleChipState state = receiver("door", "").inputs(true, false, false).build();

            Wireless.receiver().trigger(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void holdsWhatItWasShowingUntilSomethingTransmits() {
            SimpleChipState state = receiver("door", "").build().withRawOutput(0, true);

            Wireless.receiver().tick(state);

            assertThat(state.output(0)).isTrue();
        }

        @Test
        void doesNotHearANamespaceItIsNotIn() {
            services.radio().transmit(new Band("alice", "door"), true);
            SimpleChipState state = receiver("door", "bob").build().withRawOutput(0, false);

            Wireless.receiver().tick(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Test
    void carriesASignalFromOneChipToAnother() {
        SimpleChipState sender = transmitter("lamp", "").inputs(true, false, false).build();
        SimpleChipState listener = receiver("lamp", "").build();

        Wireless.transmitter().trigger(sender);
        Wireless.receiver().tick(listener);

        assertThat(listener.output(0)).isTrue();
    }

    @Nested
    @DisplayName("the analog transmitter's settings line")
    class AnalogSettingsLine {

        @ParameterizedTest(name = "\"{0}\" -> {1} to {2}, all at once {3}")
        @CsvSource({
            "'lift',         lift, 0,  15, false",
            "'lift:3:9',     lift, 3,  9,  false",
            "'lift:3:9:T',   lift, 3,  9,  true",
            "'lift:T',       lift, 0,  15, true",
            "'lift:true',    lift, 0,  15, true",
        })
        void readsWhatItIsGiven(String line, String channel, int lowest, int highest, boolean cumulative) {
            Wireless.AnalogSettings settings = Wireless.AnalogSettings.parse(line).orElseThrow();

            assertThat(settings.channel()).isEqualTo(channel);
            assertThat(settings.lowest()).isEqualTo(lowest);
            assertThat(settings.highest()).isEqualTo(highest);
            assertThat(settings.cumulative()).isEqualTo(cumulative);
        }

        @Test
        void ignoresASingleBoundOnItsOwn() {
            // Bounds are only read as a pair, which is how signs already in the world behave.
            Wireless.AnalogSettings settings = Wireless.AnalogSettings.parse("lift:3").orElseThrow();

            assertThat(settings.lowest()).isZero();
            assertThat(settings.highest()).isEqualTo(15);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", ":3:9", "lift:9:3", "lift:x:9", "lift:0:16", "lift:0:9:T:extra"})
        void refusesWhatItCannotUse(String line) {
            assertThat(Wireless.AnalogSettings.parse(line)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the analog transmitter")
    class AnalogTransmitting {

        @Test
        void raisesTheBandForItsInputLevel() {
            SimpleChipState state = analog("lift", "").build().withInputPower(0, 7);

            Wireless.analogTransmitter().trigger(state);

            assertThat(services.radio().signal(Band.named("lift7"))).contains(true);
        }

        @Test
        void lowersThePreviousBandAsItRaisesTheNext() {
            SimpleChipState state = analog("lift", "").build().withInputPower(0, 7);
            var chip = Wireless.analogTransmitter();

            chip.trigger(state);
            chip.trigger(state.withInputPower(0, 4));

            assertThat(services.radio().signal(Band.named("lift7"))).contains(false);
            assertThat(services.radio().signal(Band.named("lift4"))).contains(true);
        }

        @Test
        void leavesLevelsOutsideItsRangeAlone() {
            SimpleChipState state = analog("lift:3:9", "").build().withInputPower(0, 12);

            Wireless.analogTransmitter().trigger(state);

            assertThat(services.radio().bandCount()).isZero();
        }

        @Test
        void raisesEveryBandUpToTheLevelWhenAskedTo() {
            SimpleChipState state = analog("bar:0:3:T", "").build().withInputPower(0, 2);

            Wireless.analogTransmitter().trigger(state);

            assertThat(services.radio().isPowered(Band.named("bar0"))).isTrue();
            assertThat(services.radio().isPowered(Band.named("bar2"))).isTrue();
            assertThat(services.radio().isPowered(Band.named("bar3"))).isFalse();
        }

        @Test
        void ignoresTheClockPinWhenReadingItsLevel() {
            // The pin below the sign carries the clock, not part of the number.
            SimpleChipState state = analog("lift", "").build()
                    .withInputPower(0, 4)
                    .withInputPower(2, 15);

            Wireless.analogTransmitter().trigger(state);

            assertThat(services.radio().signal(Band.named("lift4"))).contains(true);
            assertThat(services.radio().signal(Band.named("lift15"))).isEmpty();
        }

        @Test
        void saysNothingUntilItHasSeenPowerForTheFirstTime() {
            SimpleChipState state = analog("lift", "").build();

            Wireless.analogTransmitter().load(state);

            assertThat(services.radio().bandCount()).isZero();
        }

        @Test
        void reportsZeroOnceItHasSeenPower() {
            SimpleChipState state = analog("lift", "").build().withInputPower(0, 5);
            var chip = Wireless.analogTransmitter();

            chip.trigger(state);
            chip.trigger(state.withInputPower(0, 0));

            assertThat(services.radio().signal(Band.named("lift5"))).contains(false);
            assertThat(services.radio().signal(Band.named("lift0"))).contains(true);
        }

        @Test
        void asksToBeToldAboutPowerLevelsRatherThanJustOnAndOff() {
            assertThat(Wireless.analogTransmitter().requiresAnalogRedstone()).isTrue();
        }
    }
}
