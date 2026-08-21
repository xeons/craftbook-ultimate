// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.radio.Band;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * A chip that talks on a wireless band, and can say which.
 *
 * <p>The two ends of a wireless pair are the hardest thing in the plugin to debug from the blocks
 * alone: a transmitter and a receiver that disagree about their channel look exactly like a
 * transmitter and a receiver that agree, and neither end can see the other. This lets the
 * debugging tools read the band off each end and say what the shared registry currently holds for
 * it.
 *
 * <p>Like {@link AreaAwareICLogic}, this exists for those tools and nothing else. No chip's
 * behaviour depends on it.
 */
@NullMarked
public interface BandAwareICLogic extends ICLogic {

    /**
     * The band this chip's sign names.
     *
     * @param state the chip's own state, for reading the sign
     * @return the band, or empty if the sign names no channel at all
     */
    Optional<Band> band(ChipState state);
}
