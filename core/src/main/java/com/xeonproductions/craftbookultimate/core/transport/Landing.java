// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.transport;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * Where a destination puts the travellers sent to it, and which way they end up looking.
 *
 * <p>A destination works this out for itself, from its own blocks on its own thread, and
 * publishes it. A transporter then only ever reads these three values, so sending someone across
 * the world involves no reading of blocks that belong to somewhere else.
 *
 * @param world the world the destination is in, which need not be the transporter's
 * @param block the block a traveller arrives standing in
 * @param facing the direction a traveller ends up looking
 */
@NullMarked
public record Landing(UUID world, Vec3i block, BlockFace facing) {}
