package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Following a pipe from the block that starts it.
 *
 * <p>One tracer for both ways of building a pipe. What differs between them — which blocks carry
 * items, which way they carry them next, and what counts as somewhere to put them — is all in
 * {@link PipeStyle}, so this walks a network without knowing which of the two it is walking.
 *
 * <p>Nearest first. A pipe is followed a step at a time outward from its input, so the closest way
 * out is the first one tried and a branch two blocks along is preferred to one twenty blocks
 * along. That is not how either of the mechanics this replaces chose, both of which followed one
 * branch to its end before looking at the next and so delivered somewhere that depended on which
 * side of the glass had been placed first.
 *
 * <p>Following stops at the limit an operator has set, and a pipe cut short says so rather than
 * pretending it found the end.
 */
@NullMarked
public final class Pipes {

    private Pipes() {}

    /**
     * Follows a pipe from the block that starts it.
     *
     * <p>Answers a network that reaches nowhere if the block starts no pipe, if nothing is attached
     * to it, or if any of it is somewhere this thread may not read.
     */
    public static PipeNetwork trace(PipeWorld world, Vec3i input, PipeSettings settings) {
        Optional<PipeStyle> style = PipeStyle.startingAt(world.blockAt(input));
        Optional<BlockFace> facing = world.facingAt(input);
        if (style.isEmpty() || facing.isEmpty() || !world.isLoaded(input)) {
            return PipeNetwork.nothing(style.orElse(PipeStyle.GLASS));
        }
        return new Trace(world, input, style.get(), facing.get(), settings).follow();
    }

    /**
     * Where a pipe takes its items from.
     *
     * <p>The two disagree, and both are how they have always been built. A sticky piston faces
     * whatever it is emptying and pushes into the pipe behind and around it; an extractor faces the
     * pipe and takes from what is behind it.
     */
    public static Optional<Vec3i> sourceFor(PipeWorld world, Vec3i input) {
        Optional<PipeStyle> style = PipeStyle.startingAt(world.blockAt(input));
        return style.flatMap(kind -> world.facingAt(input).map(facing ->
                kind == PipeStyle.GLASS ? input.offset(facing) : input.offset(facing.opposite())));
    }

    /** One walk outward from an input, kept together so nothing has to be threaded through. */
    private static final class Trace {

        private final PipeWorld world;
        private final Vec3i input;
        private final PipeStyle style;
        private final BlockFace facing;
        private final PipeSettings settings;

        private final Set<Vec3i> members = new LinkedHashSet<>();
        private final List<PipeNetwork.Delivery> deliveries = new ArrayList<>();
        private final Set<Vec3i> claimed = new LinkedHashSet<>();
        private final Deque<Step> queue = new ArrayDeque<>();

        private boolean whole = true;

        private Trace(PipeWorld world, Vec3i input, PipeStyle style, BlockFace facing,
                PipeSettings settings) {
            this.world = world;
            this.input = input;
            this.style = style;
            this.facing = facing;
            this.settings = settings;
        }

        private PipeNetwork follow() {
            Vec3i source = style == PipeStyle.GLASS
                    ? input.offset(facing)
                    : input.offset(facing.opposite());

            members.add(input);
            claimed.add(input);
            claimed.add(source);
            for (BlockFace side : firstSteps()) {
                queue.add(new Step(input.offset(side), side.opposite()));
            }

            while (!queue.isEmpty()) {
                if (members.size() >= settings.maxLength()) {
                    whole = false;
                    break;
                }
                take(queue.poll());
            }
            return new PipeNetwork(
                    style, Optional.of(source), deliveries, members, whole);
        }

        /**
         * Which way a pipe leaves its input.
         *
         * <p>A sticky piston pushes out of every side but the one it is emptying, since the glass
         * may be wrapped around it. An extractor pushes into the one block it points at, since a
         * pane pipe spreads from there on its own.
         */
        private List<BlockFace> firstSteps() {
            return style == PipeStyle.GLASS
                    ? PipeStyle.sides().stream().filter(side -> side != facing).toList()
                    : List.of(facing);
        }

        /** Reads one block and queues wherever it leads. */
        private void take(Step step) {
            Vec3i at = step.position();
            if (!claimed.add(at)) {
                return;
            }
            if (!world.isLoaded(at)) {
                // Somewhere this thread may not read is not somewhere to guess about, so the pipe
                // is reported as cut short rather than as ending here.
                whole = false;
                return;
            }

            Key block = world.blockAt(at);
            if (style.handsOverAt(block)) {
                handOverThrough(at);
                return;
            }
            if (!style.carries(block) || !style.mayPass(world.blockAt(step.from()), block)) {
                return;
            }

            members.add(at);
            if (style == PipeStyle.PANE) {
                collectContainersBeside(at);
            }
            for (BlockFace onward : style.onwardFrom(block, step.cameFrom())) {
                queue.add(new Step(at.offset(onward), onward.opposite()));
            }
        }

        /** A piston at the end of a glass pipe fills whatever it points at. */
        private void handOverThrough(Vec3i piston) {
            members.add(piston);
            world.facingAt(piston)
                    .map(piston::offset)
                    .filter(world::holdsItemsAt)
                    .ifPresent(container -> add(container, world.facingAt(piston).orElseThrow(), piston));
        }

        /** A pane pipe fills whatever it happens to touch. */
        private void collectContainersBeside(Vec3i pane) {
            for (BlockFace side : PipeStyle.sides()) {
                Vec3i beside = pane.offset(side);
                if (!beside.equals(input) && world.isLoaded(beside) && world.holdsItemsAt(beside)) {
                    add(beside, side, beside);
                }
            }
        }

        /**
         * Records somewhere items may go.
         *
         * <p>Never the block the pipe is emptying, which would otherwise take back what it had just
         * given up and leave the pipe shuffling one chest into itself.
         *
         * @param signOn the block whose sign filters this way out
         */
        private void add(Vec3i container, BlockFace face, Vec3i signOn) {
            if (claimed.contains(container) && !container.equals(signOn)) {
                return;
            }
            for (PipeNetwork.Delivery already : deliveries) {
                if (already.container().equals(container)) {
                    return;
                }
            }
            if (container.equals(input.offset(facing)) || container.equals(input.offset(facing.opposite()))) {
                return;
            }
            deliveries.add(new PipeNetwork.Delivery(container, face, filterOn(signOn)));
        }

        /** What a way out will accept, which is anything unless a sign of its own says otherwise. */
        private PipeFilter filterOn(Vec3i block) {
            return world.signOn(block)
                    .filter(lines -> lines.trimmedText(1).equalsIgnoreCase(style.signName()))
                    .map(lines -> PipeFilter.on(lines, world::resolveItem))
                    .orElse(PipeFilter.ANYTHING);
        }
    }

    /**
     * One block still to be read, and how the pipe arrived at it.
     *
     * @param position the block to read
     * @param cameFrom the side it is being entered by, which it never goes straight back out of
     */
    private record Step(Vec3i position, BlockFace cameFrom) {

        /** The block the pipe came from, which is what a colour is compared against. */
        Vec3i from() {
            return position.offset(cameFrom);
        }
    }
}
