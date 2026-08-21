package com.xeonproductions.craftbookultimate.core.math;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A box of blocks")
class BoundsTest {

    @Test
    void putsItsCornersTheRightWayRoundHoweverTheyWereGiven() {
        Bounds given = new Bounds(new Vec3i(10, 70, -3), new Vec3i(2, 64, 5));

        assertThat(given.from()).isEqualTo(new Vec3i(2, 64, -3));
        assertThat(given.to()).isEqualTo(new Vec3i(10, 70, 5));
    }

    @Test
    void countsBothCornersAsInside() {
        Bounds box = new Bounds(new Vec3i(0, 0, 0), new Vec3i(2, 2, 2));

        assertThat(box.width()).isEqualTo(3);
        assertThat(box.height()).isEqualTo(3);
        assertThat(box.length()).isEqualTo(3);
        assertThat(box.volume()).isEqualTo(27);
    }

    @Test
    void isOneBlockWhenBothCornersAreTheSame() {
        Bounds box = Bounds.of(new Vec3i(4, 64, 4));

        assertThat(box.volume()).isEqualTo(1);
        assertThat(box.contains(new Vec3i(4, 64, 4))).isTrue();
    }

    @Test
    void reachesAsFarEachWayAsItsRadius() {
        Bounds box = Bounds.around(new Vec3i(0, 64, 0), 5);

        assertThat(box.from()).isEqualTo(new Vec3i(-5, 59, -5));
        assertThat(box.to()).isEqualTo(new Vec3i(5, 69, 5));
    }

    @Test
    void reachesDifferentDistancesUpAndDownWhenAsked() {
        Bounds box = Bounds.around(new Vec3i(0, 64, 0), 3, 1, 8);

        assertThat(box.from()).isEqualTo(new Vec3i(-3, 63, -3));
        assertThat(box.to()).isEqualTo(new Vec3i(3, 72, 3));
    }

    @Test
    void knowsWhatIsOutsideIt() {
        Bounds box = new Bounds(new Vec3i(0, 0, 0), new Vec3i(1, 1, 1));

        assertThat(box.contains(new Vec3i(2, 0, 0))).isFalse();
        assertThat(box.contains(new Vec3i(0, -1, 0))).isFalse();
    }

    @Test
    void readsAsItsCornersAndItsSize() {
        Bounds box = new Bounds(new Vec3i(1, 2, 3), new Vec3i(2, 4, 6));

        assertThat(box.describe()).isEqualTo("1,2,3 to 2,4,6  (2x3x4)");
    }
}
