// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Where one pipe reaches, worked out once.
 *
 * <p>A pipe is a great many blocks and is asked to carry something every time its input is
 * powered, so what matters is that following it is not done again on every pulse. This is the
 * answer to having followed it: everywhere it can put something, in the order it would reach
 * them, and every block it is made of.
 *
 * <p>The blocks are kept so the answer can be thrown away the moment one of them changes. That is
 * the whole of how it stays right: nothing here is maintained as a pipe is built and broken, it is
 * only ever discarded and worked out again, so there is no state to drift.
 *
 * @param style which way this pipe was built, decided by the block it starts from
 * @param source where items are taken from, which is never also somewhere they are put
 * @param deliveries everywhere items may go, nearest first
 * @param members every block the pipe is made of, the input and the deliveries included
 * @param whole whether the pipe was followed to its end rather than cut short by its limit
 */
@NullMarked
public record PipeNetwork(
        PipeStyle style,
        Optional<Vec3i> source,
        List<Delivery> deliveries,
        Set<Vec3i> members,
        boolean whole) {

    /** Copies the collections so a network cannot be changed after it is traced. */
    public PipeNetwork {
        deliveries = List.copyOf(deliveries);
        members = Set.copyOf(members);
    }

    /** A pipe that goes nowhere, which is what an input with nothing attached traces to. */
    public static PipeNetwork nothing(PipeStyle style) {
        return new PipeNetwork(style, Optional.empty(), List.of(), Set.of(), true);
    }

    /** Whether this pipe can put anything anywhere. */
    public boolean reachesAnywhere() {
        return !deliveries.isEmpty();
    }

    /** Whether a block being changed makes this answer worth throwing away. */
    public boolean touches(Vec3i position) {
        return members.contains(position)
                || source.filter(position::equals).isPresent()
                || deliveries.stream().anyMatch(delivery -> delivery.container().equals(position));
    }

    /** Everywhere this pipe may put a particular item, in the order it would try them. */
    public List<Delivery> deliveriesFor(Key item) {
        return deliveries.stream().filter(delivery -> delivery.filter().carries(item)).toList();
    }

    /**
     * Somewhere a pipe may put what it is carrying.
     *
     * @param container where the items go
     * @param face which side of it they arrive at, which a furnace cares about and a chest does not
     * @param filter what this particular way out will accept
     */
    public record Delivery(Vec3i container, BlockFace face, PipeFilter filter) {}
}
