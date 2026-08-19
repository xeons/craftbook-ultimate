package com.xeonproductions.craftbookultimate.core.sign;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Offsets written on signs")
class SignOffsetTest {

    @ParameterizedTest(name = "\"{0}\" -> ({1}, {2}, {3})")
    @CsvSource({
        "'X+5',  5,  0,  0",
        "'X-5', -5,  0,  0",
        "'Y+1',  0,  1,  0",
        "'Y-2',  0, -2,  0",
        "'Z+3',  0,  0,  3",
        "'Z-9',  0,  0, -9",
    })
    void readsAStepAlongOneAxis(String written, int x, int y, int z) {
        assertThat(SignOffset.parse(written)).contains(new Vec3i(x, y, z));
    }

    @Test
    void doesNotCareAboutCase() {
        assertThat(SignOffset.parse("y+1")).contains(new Vec3i(0, 1, 0));
    }

    @Test
    void ignoresSurroundingSpace() {
        assertThat(SignOffset.parse("  Z-3  ")).contains(new Vec3i(0, 0, -3));
    }

    @Test
    void readsTheFurthestStepItAllows() {
        assertThat(SignOffset.parse("X+9")).contains(new Vec3i(9, 0, 0));
    }

    @Test
    void refusesToReachFurtherThanThat() {
        // A chip is meant to work on the blocks around its own sign.
        assertThat(SignOffset.parse("X+10")).isEmpty();
        assertThat(SignOffset.parse("X-10")).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", "X", "X+", "W+1", "X*1", "1+1", "XY+1", "X+one"})
    void refusesWhatIsNotAStep(String written) {
        assertThat(SignOffset.parse(written)).isEmpty();
    }
}
