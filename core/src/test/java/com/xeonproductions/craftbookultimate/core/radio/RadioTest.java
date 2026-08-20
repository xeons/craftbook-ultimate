package com.xeonproductions.craftbookultimate.core.radio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Wireless bands")
class RadioTest {

    private final Radio radio = new Radio();

    @Nested
    @DisplayName("naming a band")
    class NamingABand {

        @Test
        void keepsTheNamespaceAndTheChannelApart() {
            assertThat(Band.parse("alice", "door")).contains(new Band("alice", "door"));
        }

        @Test
        void namesNothingWithoutAChannel() {
            assertThat(Band.parse("alice", "  ")).isEmpty();
        }

        @Test
        void refusesToBeBuiltWithoutAChannel() {
            assertThatIllegalArgumentException().isThrownBy(() -> new Band("alice", ""));
        }

        @Test
        void ignoresSurroundingSpaceOnEitherHalf() {
            assertThat(new Band("  alice  ", "  door  ")).isEqualTo(new Band("alice", "door"));
        }

        @Test
        void isNotTheSameChannelInAnotherNamespace() {
            assertThat(new Band("alice", "door")).isNotEqualTo(new Band("bob", "door"));
        }

        @Test
        void appendsToItsChannelWithoutLeavingItsNamespace() {
            assertThat(new Band("alice", "lift").withSuffix("7"))
                    .isEqualTo(new Band("alice", "lift7"));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"door", "alice/door"})
        void readsBackAsSomethingAPersonCanFollow(String rendered) {
            Band band = rendered.contains("/")
                    ? new Band("alice", "door")
                    : Band.named("door");

            assertThat(band).hasToString(rendered);
        }
    }

    @Nested
    @DisplayName("carrying a signal")
    class CarryingASignal {

        @Test
        void reportsWhatWasLastTransmitted() {
            radio.transmit(Band.named("door"), true);

            assertThat(radio.signal(Band.named("door"))).contains(true);
        }

        @Test
        void reportsABandNobodyHasUsedAsUnknown() {
            assertThat(radio.signal(Band.named("door"))).isEmpty();
            assertThat(radio.isPowered(Band.named("door"))).isFalse();
        }

        @Test
        void tellsAnUnusedBandApartFromOneTransmittingLow() {
            radio.transmit(Band.named("door"), false);

            assertThat(radio.signal(Band.named("door"))).contains(false);
            assertThat(radio.signal(Band.named("gate"))).isEmpty();
        }

        @Test
        void forgetsEverythingWhenCleared() {
            radio.transmit(Band.named("door"), true);

            radio.clear();

            assertThat(radio.bandCount()).isZero();
            assertThat(radio.signal(Band.named("door"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("keeping bands between restarts")
    class KeepingBandsBetweenRestarts {

        private final Radio radio = new Radio();

        @Test
        void putsABandBackWhereItWas() {
            radio.transmit(Band.named("door"), true);

            Radio afterRestart = new Radio();
            afterRestart.load(radio.save());

            assertThat(afterRestart.signal(Band.named("door"))).contains(true);
        }

        @Test
        void keepsTheNamespaceApartFromTheChannel() {
            radio.transmit(new Band("alice", "door"), true);

            Radio afterRestart = new Radio();
            afterRestart.load(radio.save());

            assertThat(afterRestart.signal(new Band("alice", "door"))).contains(true);
            assertThat(afterRestart.signal(Band.named("door"))).isEmpty();
        }

        @Test
        void keepsAChannelNameWithSpacesInIt() {
            radio.transmit(Band.named("the north gate"), false);

            Radio afterRestart = new Radio();
            afterRestart.load(radio.save());

            assertThat(afterRestart.signal(Band.named("the north gate"))).contains(false);
        }

        @Test
        void leavesABandNothingHasTransmittedOnUnknown() {
            radio.transmit(Band.named("door"), true);

            Radio afterRestart = new Radio();
            afterRestart.load(radio.save());

            assertThat(afterRestart.signal(Band.named("gate"))).isEmpty();
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "door", "true door", "maybe - door", "true - "})
        void skipsALineThatIsNotABand(String line) {
            assertThat(radio.load(java.util.List.of(line))).isZero();
            assertThat(radio.bandCount()).isZero();
        }

        @Test
        void carriesOnPastALineItCannotRead() {
            assertThat(radio.load(java.util.List.of("nonsense", "true - door"))).isEqualTo(1);
            assertThat(radio.signal(Band.named("door"))).contains(true);
        }
    }
}
