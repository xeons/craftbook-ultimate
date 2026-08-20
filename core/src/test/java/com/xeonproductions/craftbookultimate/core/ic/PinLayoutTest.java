package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("IC pin layouts")
class PinLayoutTest {

    private static final Vec3i SIGN = new Vec3i(100, 64, 200);

    @Nested
    @DisplayName("geometry")
    class Geometry {

        @Test
        void placesTheSingleInputInFrontOfTheSign() {
            // A sign whose text faces south has its input in the block to the south.
            Vec3i input = PinLayout.SISO.inputPosition(0, SIGN, BlockFace.SOUTH);

            assertThat(input).isEqualTo(new Vec3i(100, 64, 201));
        }

        @Test
        void placesTheSingleOutputTwoBlocksBehindTheSign() {
            Vec3i output = PinLayout.SISO.outputPosition(0, SIGN, BlockFace.SOUTH);

            assertThat(output).isEqualTo(new Vec3i(100, 64, 198));
        }

        @Test
        void putsTheSecondaryInputsToEitherSide() {
            // Reading a south-facing sign the player looks north, so their right hand is west.
            Vec3i right = PinLayout.THREE_I_SO.inputPosition(1, SIGN, BlockFace.SOUTH);
            Vec3i left = PinLayout.THREE_I_SO.inputPosition(2, SIGN, BlockFace.SOUTH);

            assertThat(right).isEqualTo(new Vec3i(99, 64, 200));
            assertThat(left).isEqualTo(new Vec3i(101, 64, 200));
        }

        @Test
        void putsTheThirdInputOfTheAnyLayoutBelowTheSign() {
            Vec3i below = PinLayout.AISO.inputPosition(2, SIGN, BlockFace.SOUTH);

            assertThat(below).isEqualTo(new Vec3i(100, 63, 200));
        }

        @Test
        void rotatesTheWholeLayoutWithTheSign() {
            // The same pin, on a sign facing east, must move with the sign rather than stay put.
            Vec3i east = PinLayout.SISO.inputPosition(0, SIGN, BlockFace.EAST);
            Vec3i west = PinLayout.SISO.inputPosition(0, SIGN, BlockFace.WEST);

            assertThat(east).isEqualTo(new Vec3i(101, 64, 200));
            assertThat(west).isEqualTo(new Vec3i(99, 64, 200));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(PinLayout.class)
        void keepsEveryPinInADistinctBlock(PinLayout layout) {
            // Two pins sharing a block would make the chip read or drive the wrong wire.
            for (BlockFace front : BlockFace.horizontals()) {
                Set<Vec3i> seen = new HashSet<>();
                for (int pin = 0; pin < layout.pinCount(); pin++) {
                    Vec3i position = layout.pinPosition(pin, SIGN, front);
                    assertThat(seen.add(position))
                            .as("layout %s facing %s has two pins at %s", layout.code(), front, position)
                            .isTrue();
                }
            }
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(PinLayout.class)
        void neverPlacesAPinOnTheSignItself(PinLayout layout) {
            for (BlockFace front : BlockFace.horizontals()) {
                for (int pin = 0; pin < layout.pinCount(); pin++) {
                    assertThat(layout.pinPosition(pin, SIGN, front))
                            .as("layout %s facing %s puts pin %d on the sign", layout.code(), front, pin)
                            .isNotEqualTo(SIGN);
                }
            }
        }
    }

    @Nested
    @DisplayName("pin numbering")
    class PinNumbering {

        @ParameterizedTest(name = "{0}")
        @EnumSource(PinLayout.class)
        void countsInputsThenOutputs(PinLayout layout) {
            assertThat(layout.pinCount()).isEqualTo(layout.inputCount() + layout.outputCount());

            for (int pin = 0; pin < layout.inputCount(); pin++) {
                assertThat(layout.isInput(pin)).isTrue();
                assertThat(layout.isOutput(pin)).isFalse();
            }
            for (int pin = layout.inputCount(); pin < layout.pinCount(); pin++) {
                assertThat(layout.isOutput(pin)).isTrue();
                assertThat(layout.isInput(pin)).isFalse();
            }
        }

        @Test
        void mapsOutputNumbersOntoPinIndices() {
            assertThat(PinLayout.THREE_I_3O.outputPin(0)).isEqualTo(3);
            assertThat(PinLayout.THREE_I_3O.outputPin(2)).isEqualTo(5);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(PinLayout.class)
        void recognisesEveryOneOfItsOwnPinsByPosition(PinLayout layout) {
            for (BlockFace front : BlockFace.horizontals()) {
                for (int pin = 0; pin < layout.pinCount(); pin++) {
                    Vec3i position = layout.pinPosition(pin, SIGN, front);

                    assertThat(layout.pinAt(position, SIGN, front)).contains(pin);
                }
            }
        }

        @Test
        void reportsNoPinForAnUnrelatedBlock() {
            assertThat(PinLayout.SISO.pinAt(new Vec3i(0, 0, 0), SIGN, BlockFace.SOUTH)).isEmpty();
        }
    }

    @Nested
    @DisplayName("lookup")
    class Lookup {

        @ParameterizedTest(name = "{0}")
        @EnumSource(PinLayout.class)
        void resolvesEveryLayoutByItsOwnCode(PinLayout layout) {
            assertThat(PinLayout.byCode(layout.code())).contains(layout);
        }

        @Test
        void isCaseAndWhitespaceInsensitive() {
            assertThat(PinLayout.byCode(" 3iso ")).contains(PinLayout.THREE_I_SO);
        }

        @Test
        void spellsTheThreeInFiveOutCodeWithTheLetterO() {
            // This layout code ends in a letter O, not a digit zero.
            assertThat(PinLayout.THREE_I_5O.code()).isEqualTo("3I5O");
            assertThat(PinLayout.byCode("3I50")).isEmpty();
        }

        @Test
        void returnsEmptyForAnUnknownCode() {
            assertThat(PinLayout.byCode("NOPE")).isEmpty();
        }

        @Test
        void defaultsToTheLogicGateLayout() {
            assertThat(PinLayout.defaultLayout()).isEqualTo(PinLayout.THREE_I_SO);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        void rejectsAPinIndexOutsideTheLayout() {
            assertThatThrownBy(() -> PinLayout.SISO.offset(2))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessageContaining("SISO");
        }

        @Test
        void rejectsAnOutputOnALayoutThatHasNone() {
            assertThatThrownBy(() -> PinLayout.AIZO.outputPin(0))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessageContaining("0 outputs");
        }

        @Test
        void rejectsANonCardinalSignFacing() {
            assertThatThrownBy(() -> PinLayout.SISO.inputPosition(0, SIGN, BlockFace.UP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cardinal");
        }
    }
}
