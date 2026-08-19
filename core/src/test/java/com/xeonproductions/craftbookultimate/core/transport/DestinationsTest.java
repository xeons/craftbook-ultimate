package com.xeonproductions.craftbookultimate.core.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The register of named destinations")
class DestinationsTest {

    private static final UUID WORLD = UUID.randomUUID();

    private final Destinations destinations = new Destinations();
    private final Object first = new Object();
    private final Object second = new Object();

    private static Landing at(int y) {
        return new Landing(WORLD, new Vec3i(0, y, 0), BlockFace.NORTH);
    }

    @Nested
    @DisplayName("taking a name")
    class TakingAName {

        @Test
        void givesAFreeNameToWhoeverAsks() {
            assertThat(destinations.claim("atrium", first, at(64))).isTrue();
            assertThat(destinations.find("atrium")).contains(at(64));
        }

        @Test
        void refusesANameAlreadyInUse() {
            destinations.claim("atrium", first, at(64));

            assertThat(destinations.claim("atrium", second, at(70))).isFalse();
            assertThat(destinations.find("atrium")).contains(at(64));
        }

        @Test
        void letsTheHolderClaimItsOwnNameAgain() {
            destinations.claim("atrium", first, at(64));

            assertThat(destinations.claim("atrium", first, at(70))).isTrue();
            assertThat(destinations.find("atrium")).contains(at(70));
        }

        @Test
        void keepsSeparateNamesApart() {
            destinations.claim("atrium", first, at(64));
            destinations.claim("cellar", second, at(20));

            assertThat(destinations.find("atrium")).contains(at(64));
            assertThat(destinations.find("cellar")).contains(at(20));
        }
    }

    @Nested
    @DisplayName("moving where arrivals land")
    class MovingTheArrivalPoint {

        @Test
        void followsTheHolder() {
            destinations.claim("atrium", first, at(64));

            assertThat(destinations.update("atrium", first, at(65))).isTrue();
            assertThat(destinations.find("atrium")).contains(at(65));
        }

        @Test
        void ignoresAnyoneElse() {
            destinations.claim("atrium", first, at(64));

            assertThat(destinations.update("atrium", second, at(65))).isFalse();
            assertThat(destinations.find("atrium")).contains(at(64));
        }

        @Test
        void ignoresANameNobodyHolds() {
            assertThat(destinations.update("atrium", first, at(64))).isFalse();
            assertThat(destinations.find("atrium")).isEmpty();
        }
    }

    @Nested
    @DisplayName("giving a name up")
    class GivingANameUp {

        @Test
        void freesItForTheNextToAsk() {
            destinations.claim("atrium", first, at(64));

            assertThat(destinations.release("atrium", first)).isTrue();
            assertThat(destinations.claim("atrium", second, at(70))).isTrue();
        }

        @Test
        void cannotBeDoneByAnyoneButTheHolder() {
            destinations.claim("atrium", first, at(64));

            assertThat(destinations.release("atrium", second)).isFalse();
            assertThat(destinations.find("atrium")).contains(at(64));
        }

        @Test
        void doesNothingForANameNobodyHolds() {
            assertThat(destinations.release("atrium", first)).isFalse();
        }
    }

    @Test
    void reportsWhoHoldsWhat() {
        destinations.claim("atrium", first, at(64));

        assertThat(destinations.isHeldBy("atrium", first)).isTrue();
        assertThat(destinations.isHeldBy("atrium", second)).isFalse();
        assertThat(destinations.isHeldBy("cellar", first)).isFalse();
    }
}
