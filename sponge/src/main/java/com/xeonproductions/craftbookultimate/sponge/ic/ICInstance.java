// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.world.BlockKey;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * One chip in the world: its sign, its wiring, and the logic behind it.
 *
 * <p>Nothing here is state a chip keeps between runs — that belongs to the logic. What this holds is
 * where the chip is and whether it is still there.
 */
@NullMarked
public final class ICInstance {

    /**
     * How deep one chip setting off another may go before it is left alone.
     *
     * <p>A ring of chips driving each other would otherwise recurse until the thread's stack ran
     * out, which takes the whole region with it rather than the one build that is at fault.
     */
    private static final int MAX_TRIGGER_DEPTH = 256;

    private static final ThreadLocal<int[]> TRIGGER_DEPTH =
            ThreadLocal.withInitial(() -> new int[1]);

    private final ServerWorld world;
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
            ServerWorld world,
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

    public ICDefinition definition() {
        return definition;
    }

    public ServerWorld world() {
        return world;
    }

    public Vec3i signPosition() {
        return signPosition;
    }

    public BlockKey signKey() {
        return BlockKey.of(world.uniqueId(), signPosition);
    }

    public ICMode mode() {
        return mode;
    }

    public ICLogic logic() {
        return logic;
    }

    public BlockChipState inspectionState() {
        return newState(-1);
    }

    public ChipServices services() {
        return services;
    }

    public boolean isSelfTriggering() {
        return selfTriggering;
    }

    public boolean isUnloaded() {
        return unloaded;
    }

    public PinLayout layout() {
        return layout;
    }

    /** Every block this chip reads or drives, which is what it is indexed by. */
    public List<BlockKey> pinKeys() {
        List<BlockKey> keys = new ArrayList<>(layout.pinCount());
        for (int pin = 0; pin < layout.pinCount(); pin++) {
            keys.add(BlockKey.of(
                    world.uniqueId(), layout.pinPosition(mode.slotFor(pin), signPosition, front)));
        }
        return keys;
    }

    /** Which input a block is, or -1 where it is not one. */
    public int inputAt(Vec3i position) {
        for (int input = 0; input < layout.inputCount(); input++) {
            if (layout.pinPosition(mode.slotFor(input), signPosition, front).equals(position)) {
                return input;
            }
        }
        return -1;
    }

    void load(Scheduler scheduler) {
        this.scheduler = scheduler;
        logic.load(newState(-1));

        if (selfTriggering) {
            tickTask = scheduler.runRepeating(this::tick, 1, 1);
        }
    }

    void unload() {
        unloaded = true;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        logic.unload(newState(-1));
    }

    public void trigger(int triggeredInput) {
        run(() -> logic.trigger(newState(triggeredInput)));
    }

    public void tick() {
        if (!(logic instanceof SelfTriggeringICLogic ticking)) {
            return;
        }
        run(() -> ticking.tick(newState(-1)));
    }

    /**
     * Runs a chip once, and only once at a time.
     *
     * <p>A chip that drives a lever sets off whatever reads that lever, which may lead back here.
     * Refusing to re-enter is what stops a chip triggering itself, and the depth count is what
     * stops a ring of them doing it between themselves.
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
        return definition.model() + " at " + signPosition + " in " + world.key().asString();
    }
}
