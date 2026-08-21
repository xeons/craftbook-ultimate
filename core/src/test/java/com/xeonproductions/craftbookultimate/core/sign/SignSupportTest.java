// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.sign;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("What a wall sign hangs on")
class SignSupportTest {

    private static final Vec3i SIGN = new Vec3i(10, 64, 20);

    @Test
    void isTheBlockBehindIt() {
        // The sign faces south, so it is stuck to the north side of the block it hangs on.
        assertThat(SignSupport.of(SIGN, BlockFace.SOUTH)).isEqualTo(new Vec3i(10, 64, 19));
    }

    @Test
    void isNeverTheBlockTheTextFaces() {
        assertThat(SignSupport.of(SIGN, BlockFace.SOUTH))
                .isNotEqualTo(SIGN.offset(BlockFace.SOUTH));
    }

    @ParameterizedTest(name = "facing {0}")
    @EnumSource(value = BlockFace.class, names = {"NORTH", "EAST", "SOUTH", "WEST"})
    void isOneStepAwayWhicheverWayTheSignFaces(BlockFace facing) {
        Vec3i support = SignSupport.of(SIGN, facing);

        assertThat(Math.abs(support.x() - SIGN.x()) + Math.abs(support.z() - SIGN.z())).isEqualTo(1);
        assertThat(support.y()).isEqualTo(SIGN.y());
    }

    @Test
    void holdsUpTheSignThatFacesAwayFromIt() {
        Vec3i support = new Vec3i(10, 64, 19);

        assertThat(SignSupport.hangsOn(SIGN, BlockFace.SOUTH, support)).isTrue();
    }

    @Test
    void doesNotHoldUpASignBesideItThatFacesElsewhere() {
        // The block breaking is north of the sign, but the sign faces east, so it is stuck to the
        // block on its west and this one is nothing to do with it. Reading the rule backwards
        // would take this sign down and leave the one that really was hanging there.
        Vec3i elsewhere = new Vec3i(10, 64, 19);

        assertThat(SignSupport.hangsOn(SIGN, BlockFace.EAST, elsewhere)).isFalse();
    }

    @ParameterizedTest(name = "facing {0}")
    @EnumSource(value = BlockFace.class, names = {"NORTH", "EAST", "SOUTH", "WEST"})
    void holdsUpExactlyOneOfTheFourSignsThatCouldSurroundIt(BlockFace facing) {
        Vec3i support = new Vec3i(0, 64, 0);

        long hanging = java.util.stream.Stream
                .of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
                .filter(side -> SignSupport.hangsOn(support.offset(side), facing, support))
                .count();

        assertThat(hanging).isEqualTo(1);
    }
}
