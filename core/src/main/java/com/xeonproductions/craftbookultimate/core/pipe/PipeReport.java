// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * What the plugin believes about one pipe.
 *
 * <p>A pipe that will not deliver looks exactly like a pipe that is not a pipe at all, and a
 * builder standing in front of a run of glass cannot tell which they have. Everything that decides
 * the answer is invisible: which style the run was read as, which block it starts from, how far it
 * was followed, what each way out will accept, and the order they are tried in. This says all of
 * it.
 *
 * <p>A value rather than a printout, for the reason {@link
 * com.xeonproductions.craftbookultimate.core.ic.ChipReport} is one: the stick and any test then
 * say the same things about the same pipe, and what is said can be asserted on with no server
 * running.
 *
 * <p>Nothing here traces anything. A report is made from an answer somebody else worked out, so
 * asking about a pipe costs a lookup where the answer was already remembered.
 *
 * @param world what the world is called
 * @param clicked the block the report was asked about
 * @param input where the pipe is driven from, which is the block the trace started at
 * @param network where that pipe was found to reach
 * @param remembered whether the answer was already held rather than worked out for this report
 * @param sourceHoldsItems whether there is really a container where the pipe takes from
 */
@NullMarked
public record PipeReport(
        String world,
        Vec3i clicked,
        Vec3i input,
        PipeNetwork network,
        boolean remembered,
        boolean sourceHoldsItems) {

    /** How many ways out are named before the report counts the rest. */
    private static final int DELIVERY_LIMIT = 8;

    /** Says everything known about the pipe, one line at a time. */
    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.text("Pipe at " + describe(input), NamedTextColor.GOLD));
        lines.add(fact("World", world));
        lines.add(fact("Style", styleName()));
        lines.add(fact("Blocks in the run", String.valueOf(network.members().size())));
        if (!network.touches(clicked)) {
            lines.add(Component.text(
                    "The block you clicked is beside this pipe rather than part of it.",
                    NamedTextColor.YELLOW));
        }
        lines.add(source());
        lines.addAll(reach());
        lines.add(fact("Answer", remembered ? "remembered" : "worked out just now"));

        if (!network.whole()) {
            lines.add(Component.text(
                    "This pipe was cut short by its length limit, so what lies past that point "
                            + "is not reachable. Raise pipes.max-length or shorten the run.",
                    NamedTextColor.YELLOW));
        }
        return lines;
    }

    /** Which grammar the run was read with, said the way a builder would recognise it. */
    private String styleName() {
        return switch (network.style()) {
            case GLASS -> "glass, started from a piston";
            case PANE -> "panes, started from an [Extractor] piston";
        };
    }

    /**
     * Where the pipe takes items from, which is the commonest thing to have got wrong.
     *
     * <p>Two different failures and they read alike from outside. A pipe whose input faces nowhere
     * has no source at all; one whose input faces an empty block has a source with nothing in it.
     * Only the second is fixed by putting a chest down, so they are said differently.
     */
    private Component source() {
        Optional<Vec3i> from = network.source();
        if (from.isEmpty()) {
            return Component.text("Takes from: nothing", NamedTextColor.RED)
                    .append(Component.text(
                            "  the input block faces nowhere", NamedTextColor.GRAY));
        }
        if (!sourceHoldsItems) {
            return Component.text("Takes from: " + describe(from.get()), NamedTextColor.RED)
                    .append(Component.text(
                            "  nothing there holds items, so this pipe has nothing to carry",
                            NamedTextColor.GRAY));
        }
        return fact("Takes from", describe(from.get()));
    }

    /** Everywhere the pipe can put something, in the order it would try them. */
    private List<Component> reach() {
        if (!network.reachesAnywhere()) {
            return List.of(Component.text("Reaches: nowhere", NamedTextColor.RED)
                    .append(Component.text(
                            "  the run touches no container it may fill", NamedTextColor.GRAY)));
        }

        List<Component> lines = new ArrayList<>();
        lines.add(fact("Reaches", network.deliveries().size() + " way(s) out, nearest first"));

        int shown = Math.min(network.deliveries().size(), DELIVERY_LIMIT);
        for (int index = 0; index < shown; index++) {
            PipeNetwork.Delivery delivery = network.deliveries().get(index);
            lines.add(Component.text("  " + (index + 1) + ". ", NamedTextColor.GRAY)
                    .append(Component.text(describe(delivery.container()), NamedTextColor.WHITE))
                    .append(Component.text(
                            " from the " + delivery.face().name().toLowerCase(Locale.ROOT)
                                    + ", " + filterOf(delivery),
                            NamedTextColor.GRAY)));
        }
        if (network.deliveries().size() > shown) {
            lines.add(Component.text(
                    "  and " + (network.deliveries().size() - shown) + " more",
                    NamedTextColor.GRAY));
        }
        return lines;
    }

    /** What one way out will accept, said shortly enough to sit on the end of a line. */
    private static String filterOf(PipeNetwork.Delivery delivery) {
        PipeFilter filter = delivery.filter();
        if (filter.isAnything()) {
            return "takes anything";
        }

        StringBuilder said = new StringBuilder("takes ");
        said.append(filter.wanted().isEmpty() ? "anything" : names(filter.wanted()));
        if (!filter.refused().isEmpty()) {
            said.append(" but not ").append(names(filter.refused()));
        }
        return said.toString();
    }

    /** A few item names, counting the rest rather than filling the screen with them. */
    private static String names(Set<Key> keys) {
        List<String> shown = keys.stream().limit(3).map(Key::value).toList();
        String joined = String.join(", ", shown);
        return keys.size() > shown.size() ? joined + " and " + (keys.size() - shown.size()) + " more" : joined;
    }

    private static Component fact(String what, String value) {
        return Component.text(what + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private static String describe(Vec3i position) {
        return position.x() + ", " + position.y() + ", " + position.z();
    }
}
