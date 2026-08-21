// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.math;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A point between blocks")
class Vec3dTest {

    @Test
    void takesTheCornerOfABlockAsItsPosition() {
        assertThat(Vec3d.of(new Vec3i(3, 64, -2))).isEqualTo(new Vec3d(3, 64, -2));
    }

    @Test
    void findsTheMiddleOfABlockFloorForSomethingStandingOnIt() {
        assertThat(Vec3d.centreOf(new Vec3i(3, 64, -2))).isEqualTo(new Vec3d(3.5, 64, -1.5));
    }

    @Test
    void findsTheMiddleOfABlockInEveryDirection() {
        assertThat(Vec3d.middleOf(new Vec3i(3, 64, -2))).isEqualTo(new Vec3d(3.5, 64.5, -1.5));
    }

    @Test
    void keepsItsDirectionWhenGivenALengthOfOne() {
        Vec3d unit = new Vec3d(0, 0, -5).normalise();

        assertThat(unit).isEqualTo(new Vec3d(0, 0, -1));
        assertThat(unit.length()).isEqualTo(1);
    }

    @Test
    void hasNoDirectionToKeepWhenItIsNotPointingAnywhere() {
        assertThat(Vec3d.ZERO.normalise()).isEqualTo(Vec3d.ZERO);
    }

    @Test
    void turnsAQuarterCircleClockwiseSeenFromAbove() {
        Vec3d turned = new Vec3d(0, 0, -1).rotateAroundY(90);

        assertThat(turned.x()).isEqualTo(-1, within(1e-9));
        assertThat(turned.z()).isEqualTo(0, within(1e-9));
    }

    @Test
    void leavesTheVerticalAloneWhenItTurns() {
        assertThat(new Vec3d(1, 7, 0).rotateAroundY(37).y()).isEqualTo(7);
    }

    @Test
    void namesTheBlockItIsInside() {
        assertThat(new Vec3d(3.9, 64.5, -1.2).toBlock()).isEqualTo(new Vec3i(3, 64, -2));
    }
}
