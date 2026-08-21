// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.area;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Where the saved areas are kept.
 *
 * <p>A toggled area is a piece of the world put away and brought back. What the vault holds is
 * the blocks and the place they belong; what it offers is the three things a sign can ask for —
 * put one back, take one away, and remember what is standing there now.
 *
 * <p>Nothing here says what the store is made of. An implementation backed by files on disk
 * belongs to whoever owns those blocks, and reaches the world only through the seams above it.
 */
@NullMarked
public interface AreaVault {

    /** Whether an area has been saved under a name. */
    boolean has(AreaName name);

    /** Where a saved area belongs, or nothing if there is no such area. */
    Optional<AreaAnchor> anchorOf(AreaName name);

    /**
     * Puts a saved area back exactly where it was saved from.
     *
     * @return true if it was put back
     */
    boolean restore(AreaName name);

    /**
     * Empties the space a saved area occupies, leaving air.
     *
     * @return true if the space was emptied
     */
    boolean clear(AreaName name);

    /**
     * Replaces what is saved with whatever now stands in its place.
     *
     * <p>What makes a {@code [SaveArea]} sign different from an {@code [Area]} one: the half
     * being put away is written down as it stands rather than as it was first saved, so changes
     * made to it survive the next toggle.
     *
     * @return true if it was written down
     */
    boolean capture(AreaName name);

    /** Every area saved under a namespace, by identifier, in order. */
    List<String> idsIn(String namespace);

    /**
     * Forgets a saved area.
     *
     * @return true if there was one to forget
     */
    boolean delete(AreaName name);

    /** How many areas a namespace holds. */
    default int countIn(String namespace) {
        return idsIn(namespace).size();
    }

    /** A vault holding nothing, which is what a world with no areas saved in it has. */
    static AreaVault empty() {
        return EmptyAreaVault.INSTANCE;
    }
}
