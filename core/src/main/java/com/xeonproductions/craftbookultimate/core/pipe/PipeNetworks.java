package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * What each pipe was last found to reach.
 *
 * <p>Following a pipe means reading every block of it, and a pipe is asked to carry something every
 * time its input is powered, so the answer is kept rather than worked out again on every pulse.
 *
 * <p>It is a memory of an answer, not a picture of the world. Nothing here is kept up to date as a
 * pipe is built or broken: a block changing throws away every answer that mentioned it, and the
 * next pulse works it out afresh. That is what makes it safe to be wrong — the worst an entry can
 * do is not be there.
 *
 * <p>Which also makes it safe on a regionised server. A pipe is a line of touching blocks and so
 * belongs to one region, so the thread that runs a pipe is the thread that owns its blocks.
 * Another region can only ever cause an entry to be dropped, and a dropped entry costs a walk.
 */
@NullMarked
public final class PipeNetworks {

    /** What each input was last found to reach, by world and then by input block. */
    private final Map<UUID, Map<Vec3i, PipeNetwork>> known = new ConcurrentHashMap<>();

    /**
     * Which inputs to forget about when a block changes.
     *
     * <p>Kept the other way round from {@link #known} so that a block being placed or broken costs
     * one lookup rather than a walk through every pipe on the server.
     */
    private final Map<UUID, Map<Vec3i, Set<Vec3i>>> usedBy = new ConcurrentHashMap<>();

    /**
     * Which inputs to forget about when a chunk goes away.
     *
     * <p>Kept for the same reason as {@link #usedBy} and dropped the same way. Without it an
     * answer would outlive the blocks it describes: a pipe nobody visits again would hold its
     * share of the index until the server stopped, which is what the mechanic this replaces did.
     */
    private final Map<UUID, Map<Long, Set<Vec3i>>> inChunk = new ConcurrentHashMap<>();

    /**
     * What a pipe reaches, following it again only if the answer is not already known.
     *
     * @param world which world it is in, so two worlds never share an answer
     */
    public PipeNetwork from(PipeWorld seen, UUID world, Vec3i input, PipeSettings settings) {
        Map<Vec3i, PipeNetwork> networks =
                known.computeIfAbsent(world, key -> new ConcurrentHashMap<>());
        PipeNetwork remembered = networks.get(input);
        if (remembered != null) {
            return remembered;
        }

        PipeNetwork traced = Pipes.trace(seen, input, settings);
        remember(world, input, traced);
        return traced;
    }

    /** Whether a pipe's shape is already known, which is what a test asserts on. */
    public boolean remembers(UUID world, Vec3i input) {
        Map<Vec3i, PipeNetwork> networks = known.get(world);
        return networks != null && networks.containsKey(input);
    }

    /**
     * Forgets every pipe a block was part of.
     *
     * <p>Called for any block that changes anywhere near a pipe. Forgetting one that had nothing to
     * do with it costs a single walk the next time it runs, so this errs towards forgetting.
     */
    public void forgetAbout(UUID world, Vec3i changed) {
        Map<Vec3i, Set<Vec3i>> index = usedBy.get(world);
        if (index == null) {
            return;
        }
        Set<Vec3i> inputs = index.remove(changed);
        if (inputs == null) {
            return;
        }
        forget(world, inputs);
    }

    /**
     * Forgets every pipe that reaches into a chunk.
     *
     * <p>Called as a chunk goes away. What is remembered is only ever an answer, so letting one go
     * costs a walk if that pipe is ever powered again and nothing at all if it is not.
     */
    public void forgetChunk(UUID world, int chunkX, int chunkZ) {
        Map<Long, Set<Vec3i>> chunks = inChunk.get(world);
        if (chunks == null) {
            return;
        }
        Set<Vec3i> inputs = chunks.remove(keyOf(chunkX, chunkZ));
        if (inputs != null) {
            forget(world, inputs);
        }
    }

    /** Forgets every pipe in a world, which is what a world going away wants. */
    public void forgetWorld(UUID world) {
        known.remove(world);
        usedBy.remove(world);
        inChunk.remove(world);
    }

    /** Forgets everything, which is what a reloaded configuration wants. */
    public void forgetEverything() {
        known.clear();
        usedBy.clear();
        inChunk.clear();
    }

    /** How many pipes are remembered, which is what the plugin reports about itself. */
    public int size() {
        return known.values().stream().mapToInt(Map::size).sum();
    }

    /** Drops a set of answers and everything that pointed at them. */
    private void forget(UUID world, Set<Vec3i> inputs) {
        Map<Vec3i, PipeNetwork> networks = known.get(world);
        if (networks == null) {
            return;
        }
        Map<Vec3i, Set<Vec3i>> index = usedBy.get(world);
        Map<Long, Set<Vec3i>> chunks = inChunk.get(world);
        for (Vec3i input : Set.copyOf(inputs)) {
            PipeNetwork dropped = networks.remove(input);
            if (dropped != null) {
                unindex(index, chunks, input, dropped);
            }
        }
    }

    private void remember(UUID world, Vec3i input, PipeNetwork network) {
        known.computeIfAbsent(world, key -> new ConcurrentHashMap<>()).put(input, network);
        Map<Vec3i, Set<Vec3i>> index =
                usedBy.computeIfAbsent(world, key -> new ConcurrentHashMap<>());
        Map<Long, Set<Vec3i>> chunks =
                inChunk.computeIfAbsent(world, key -> new ConcurrentHashMap<>());
        for (Vec3i part : partsOf(network, input)) {
            index.computeIfAbsent(part, key -> ConcurrentHashMap.newKeySet()).add(input);
            chunks.computeIfAbsent(keyOf(part), key -> ConcurrentHashMap.newKeySet()).add(input);
        }
    }

    private static void unindex(
            @Nullable Map<Vec3i, Set<Vec3i>> index,
            @Nullable Map<Long, Set<Vec3i>> chunks,
            Vec3i input,
            PipeNetwork network) {
        for (Vec3i part : partsOf(network, input)) {
            drop(index, part, input);
            drop(chunks, keyOf(part), input);
        }
    }

    /** Takes one input out of an index, and the entry with it if that was the last. */
    private static <K> void drop(@Nullable Map<K, Set<Vec3i>> index, K at, Vec3i input) {
        if (index == null) {
            return;
        }
        Set<Vec3i> inputs = index.get(at);
        if (inputs != null) {
            inputs.remove(input);
            if (inputs.isEmpty()) {
                index.remove(at);
            }
        }
    }

    /** Which chunk a block is in, as one number. */
    private static long keyOf(Vec3i position) {
        return keyOf(position.x() >> 4, position.z() >> 4);
    }

    /** A chunk's two coordinates as one number, so it can be a key. */
    private static long keyOf(int chunkX, int chunkZ) {
        return ((long) chunkX << Integer.SIZE) | (chunkZ & 0xffffffffL);
    }

    /** Every block whose changing makes an answer worth throwing away. */
    private static Set<Vec3i> partsOf(PipeNetwork network, Vec3i input) {
        Set<Vec3i> parts = ConcurrentHashMap.newKeySet();
        parts.add(input);
        parts.addAll(network.members());
        network.source().ifPresent(parts::add);
        network.deliveries().forEach(delivery -> parts.add(delivery.container()));
        // The blocks just outside the pipe matter too: a pane placed against the end of a run
        // lengthens it, and nothing inside the run has changed to say so.
        for (Vec3i part : Set.copyOf(parts)) {
            for (var side : PipeStyle.sides()) {
                parts.add(part.offset(side));
            }
        }
        return parts;
    }
}
