package com.xeonproductions.craftbookultimate.paper.pipe;

import com.xeonproductions.craftbookultimate.core.config.PipeSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.pipe.PipeNetwork;
import com.xeonproductions.craftbookultimate.core.pipe.Pipes;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

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

    /** What a pipe reaches, following it again only if the answer is not already known. */
    public PipeNetwork from(BukkitPipeWorld world, Vec3i input, PipeSettings settings) {
        UUID id = world.world().getUID();
        PipeNetwork known = this.known
                .computeIfAbsent(id, key -> new ConcurrentHashMap<>())
                .get(input);
        if (known != null) {
            return known;
        }

        PipeNetwork traced = Pipes.trace(world, input, settings);
        remember(id, input, traced);
        return traced;
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
        Map<Vec3i, PipeNetwork> networks = known.get(world);
        if (networks == null) {
            return;
        }
        for (Vec3i input : inputs) {
            PipeNetwork dropped = networks.remove(input);
            if (dropped != null) {
                unindex(index, input, dropped);
            }
        }
    }

    /** Forgets everything, which is what a reloaded configuration wants. */
    public void forgetEverything() {
        known.clear();
        usedBy.clear();
    }

    /** How many pipes are remembered, which is what the plugin reports about itself. */
    public int size() {
        return known.values().stream().mapToInt(Map::size).sum();
    }

    private void remember(UUID world, Vec3i input, PipeNetwork network) {
        known.computeIfAbsent(world, key -> new ConcurrentHashMap<>()).put(input, network);
        Map<Vec3i, Set<Vec3i>> index = usedBy.computeIfAbsent(world, key -> new ConcurrentHashMap<>());
        for (Vec3i part : partsOf(network, input)) {
            index.computeIfAbsent(part, key -> ConcurrentHashMap.newKeySet()).add(input);
        }
    }

    private static void unindex(Map<Vec3i, Set<Vec3i>> index, Vec3i input, PipeNetwork network) {
        for (Vec3i part : partsOf(network, input)) {
            Set<Vec3i> inputs = index.get(part);
            if (inputs != null) {
                inputs.remove(input);
                if (inputs.isEmpty()) {
                    index.remove(part);
                }
            }
        }
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
            for (var side : com.xeonproductions.craftbookultimate.core.pipe.PipeStyle.sides()) {
                parts.add(part.offset(side));
            }
        }
        return parts;
    }
}
