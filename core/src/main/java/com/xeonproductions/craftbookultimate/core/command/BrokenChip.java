// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * A loaded chip that cannot work as written, and why.
 *
 * <p>Everything needed to report one, read off the chip before the answer is built: what it is,
 * where it is, and what its sign leaves out. Nothing here holds a world or a block, so the listing
 * survives the chunk unloading underneath it while the reply is still being written.
 */
@NullMarked
public record BrokenChip(
        ICDefinition definition, UUID world, String worldName, Vec3i at, LineReview review) {

    /** What the chip is, as a listing names it. */
    public String model() {
        return definition.model() + "  " + definition.name();
    }

    /** Where it is, as a builder would go and find it. */
    public String where() {
        return at.x() + ", " + at.y() + ", " + at.z() + " in " + worldName;
    }
}
