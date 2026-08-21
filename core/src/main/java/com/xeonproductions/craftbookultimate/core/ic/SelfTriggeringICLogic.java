// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import org.jspecify.annotations.NullMarked;

/**
 * A chip that also runs on its own, without waiting for an input to change.
 *
 * <p>Clocks, sensors and anything that watches the world need this: a daylight sensor has no
 * input to react to, so it is stepped once per server tick instead.
 *
 * <p>Self-triggering is opt-in per sign, written as the {@code S} suffix on the model reference,
 * because a world full of chips ticking every tick is expensive. A chip that only makes sense
 * when ticking can force it on by overriding {@link #alwaysSelfTriggering()}.
 */
@NullMarked
public interface SelfTriggeringICLogic extends ICLogic {

    /**
     * Runs the chip for one server tick.
     *
     * <p>Called on every tick the chip is loaded and self-triggering, so implementations should
     * return quickly and do their own rate limiting if they act less often than that.
     *
     * @param state the chip's inputs, outputs and configuration
     */
    void tick(ChipState state);

    /**
     * Whether this chip ticks whether or not the sign asks it to.
     *
     * <p>Chips with no meaningful input, such as a daylight sensor, should answer true so that a
     * player cannot create one that can never do anything.
     */
    default boolean alwaysSelfTriggering() {
        return false;
    }
}
