package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One chip in the world: a sign, the logic behind it, and the pins it reads and drives.
 *
 * <p>An instance exists only while its chunk is loaded. It belongs to the region that owns its
 * sign, and every method here must be called from that region's thread.
 */
@NullMarked
public final class ICInstance {

    /**
     * How deep a chain of chips triggering one another may run before it is abandoned.
     *
     * <p>Driving an output notifies its neighbours immediately, so one chip triggering the next
     * happens inside a single redstone update, on one call stack. A chain normally ends on its
     * own, because an output is only written when its value actually changes and a settling
     * circuit soon stops changing. A circuit that oscillates never settles, and without a limit
     * would run until the stack ran out.
     *
     * <p>The limit is therefore set well above any plausible chain of chips wired in series, such
     * as the adders of a wide ripple-carry sum, and well below the depth at which the stack would
     * be at risk.
     */
    private static final int MAX_TRIGGER_DEPTH = 256;

    /** Counts how deep the current thread is in a chain of chips triggering one another. */
    private static final ThreadLocal<int[]> TRIGGER_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private final World world;
    private final Vec3i signPosition;
    private final BlockFace front;
    private final ICDefinition definition;
    private final PinLayout layout;
    private final ICMode mode;
    private final boolean selfTriggering;
    private final ChipServices services;
    private final ICLogic logic;

    private Scheduler.@Nullable Task tickTask;
    private @Nullable Scheduler scheduler;
    private boolean running;
    private boolean unloaded;

    ICInstance(
            World world,
            Vec3i signPosition,
            BlockFace front,
            ICDefinition definition,
            ICMode mode,
            boolean selfTriggering,
            ChipServices services) {
        this.world = world;
        this.signPosition = signPosition;
        this.front = front;
        this.definition = definition;
        this.layout = definition.defaultLayout();
        this.mode = mode;
        this.services = services;
        this.logic = definition.newLogic();
        this.selfTriggering = selfTriggering && logic instanceof SelfTriggeringICLogic;
    }

    /** The chip this is an instance of. */
    public ICDefinition definition() {
        return definition;
    }

    /** The world the chip is in. */
    public World world() {
        return world;
    }

    /** The position of the chip's sign. */
    public Vec3i signPosition() {
        return signPosition;
    }

    /** The key identifying the chip's sign. */
    public BlockKey signKey() {
        return BlockKey.of(world, signPosition);
    }

    /** Whether this chip ticks on its own. */
    public boolean isSelfTriggering() {
        return selfTriggering;
    }

    /** Whether this chip has been unloaded and should no longer run. */
    public boolean isUnloaded() {
        return unloaded;
    }

    /** The layout this chip is wired for. */
    public PinLayout layout() {
        return layout;
    }

    /** The keys of every block this chip reads or drives. */
    public List<BlockKey> pinKeys() {
        List<BlockKey> keys = new ArrayList<>(layout.pinCount());
        for (int pin = 0; pin < layout.pinCount(); pin++) {
            keys.add(BlockKey.of(world, layout.pinPosition(mode.slotFor(pin), signPosition, front)));
        }
        return keys;
    }

    /**
     * Which input, if any, sits at a position.
     *
     * @return the input number, or {@code -1} if the position is not one of this chip's inputs
     */
    public int inputAt(Vec3i position) {
        for (int input = 0; input < layout.inputCount(); input++) {
            if (layout.pinPosition(mode.slotFor(input), signPosition, front).equals(position)) {
                return input;
            }
        }
        return -1;
    }

    /**
     * Starts the chip.
     *
     * @param scheduler used to tick the chip if it is self-triggering
     */
    void load(Scheduler scheduler) {
        this.scheduler = scheduler;
        logic.load(newState(-1));

        if (selfTriggering) {
            tickTask = scheduler.runRepeating(this::tick, 1, 1);
        }
    }

    /** Stops the chip and releases anything it holds. */
    void unload() {
        unloaded = true;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        logic.unload(newState(-1));
    }

    /**
     * Runs the chip because one of its inputs changed.
     *
     * @param triggeredInput the input that changed, or {@code -1} if it is not known
     */
    public void trigger(int triggeredInput) {
        run(() -> logic.trigger(newState(triggeredInput)));
    }

    /** Runs the chip for one tick. Does nothing unless the chip is self-triggering. */
    public void tick() {
        if (!(logic instanceof SelfTriggeringICLogic ticking)) {
            return;
        }
        run(() -> ticking.tick(newState(-1)));
    }

    /**
     * Runs a chip action, guarding against a chip triggering itself and against a chain of chips
     * driving each other without end.
     */
    private void run(Runnable action) {
        if (unloaded || running) {
            return;
        }

        int[] depth = TRIGGER_DEPTH.get();
        if (depth[0] >= MAX_TRIGGER_DEPTH) {
            return;
        }

        running = true;
        depth[0]++;
        try {
            action.run();
        } finally {
            depth[0]--;
            running = false;
        }
    }

    private BlockChipState newState(int triggeredInput) {
        BlockChipState.Builder builder = BlockChipState.at(world, signPosition, front, layout)
                .mode(mode)
                .services(services)
                .triggeredInput(triggeredInput);

        if (scheduler != null) {
            builder.scheduler(scheduler);
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return definition.model() + " at " + signPosition + " in " + world.getName();
    }
}
