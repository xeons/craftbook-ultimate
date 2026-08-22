// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.platform;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;

/**
 * Where work is handed to be run.
 *
 * <p>SpongeVanilla ticks every world on one server thread, so there is one scheduler rather than
 * one per region, and asking which place a piece of work belongs to changes nothing about where it
 * runs. The shape is kept all the same, because what a caller says — this work belongs at that
 * block — stays true whether or not the server can act on it.
 */
@NullMarked
public final class ServerSchedulers {

    private static final long MINIMUM_DELAY_TICKS = 1;

    private final PluginContainer plugin;

    public ServerSchedulers(PluginContainer plugin) {
        this.plugin = plugin;
    }

    public Scheduler at(ServerWorld world, Vec3i position) {
        return new ServerScheduler(plugin);
    }

    public Scheduler global() {
        return new ServerScheduler(plugin);
    }

    /**
     * Whether the calling thread may touch blocks at a place.
     *
     * <p>True on the server thread and false anywhere else, which is the whole of the answer on a
     * server that does not split a world up.
     */
    public boolean ownsCurrentThread(ServerWorld world, Vec3i position) {
        return Sponge.server().onMainThread();
    }

    /** Runs work at a place, now where that is already safe and on the server thread if not. */
    public boolean executeAt(ServerWorld world, Vec3i position, Runnable task) {
        if (ownsCurrentThread(world, position)) {
            task.run();
            return true;
        }

        submit(plugin, task, MINIMUM_DELAY_TICKS, 0);
        return false;
    }

    /**
     * Runs work away from the thread that ticks the world.
     *
     * <p>Sponge keeps a scheduler of its own for this, separate from the server's, so nothing here
     * competes with a tick. Only the deliberately slow work goes on it — checking a password —
     * and none of it may touch a block.
     */
    public void async(Runnable work) {
        Sponge.asyncScheduler().submit(org.spongepowered.api.scheduler.Task.builder()
                .plugin(plugin)
                .execute(work)
                .build());
    }

    private record ServerScheduler(PluginContainer plugin) implements Scheduler {

        @Override
        public Scheduler.Task runLater(Runnable task, long delayTicks) {
            return new TaskHandle(submit(plugin, task, delayTicks, 0));
        }

        @Override
        public Scheduler.Task runRepeating(Runnable task, long delayTicks, long periodTicks) {
            return new TaskHandle(submit(plugin, task, delayTicks, periodTicks));
        }
    }

    private static ScheduledTask submit(
            PluginContainer plugin, Runnable task, long delayTicks, long periodTicks) {
        org.spongepowered.api.scheduler.Task.Builder builder =
                org.spongepowered.api.scheduler.Task.builder()
                        .plugin(plugin)
                        .execute(task)
                        .delay(Ticks.of(atLeastOneTick(delayTicks)));

        if (periodTicks > 0) {
            builder.interval(Ticks.of(atLeastOneTick(periodTicks)));
        }

        return Sponge.server().scheduler().submit(builder.build());
    }

    /**
     * A running task.
     *
     * <p>Sponge's scheduler answers whether a task is still known to it rather than whether it was
     * cancelled, so cancelling is remembered here: a caller asking after cancelling wants to hear
     * yes, and a task that merely finished is not the same thing.
     */
    private static final class TaskHandle implements Scheduler.Task {

        private final ScheduledTask task;

        private volatile boolean cancelled;

        private TaskHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            cancelled = true;
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    private static long atLeastOneTick(long ticks) {
        return Math.max(MINIMUM_DELAY_TICKS, ticks);
    }
}
