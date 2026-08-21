// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.platform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * A {@link Scheduler} whose clock only moves when it is told to.
 *
 * <p>Nothing runs until {@link #advance(long)} is called, and then everything due runs in the
 * order it came due. That turns a chip whose behaviour is spread over time into something a test
 * can drive: schedule, advance, assert, without waiting for a real second to pass.
 *
 * <p>Instances are not thread safe.
 */
@NullMarked
public final class ManualScheduler implements Scheduler {

    private final List<PendingTask> pending = new ArrayList<>();

    private long currentTick;

    /** The number of ticks this scheduler has been advanced by in total. */
    public long currentTick() {
        return currentTick;
    }

    /** The number of tasks waiting to run. */
    public int pendingCount() {
        return pending.size();
    }

    @Override
    public Task runLater(Runnable task, long delayTicks) {
        return schedule(task, delayTicks, 0);
    }

    @Override
    public Task runRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (periodTicks < 1) {
            throw new IllegalArgumentException("A repeating task needs a period of at least one tick");
        }
        return schedule(task, delayTicks, periodTicks);
    }

    /**
     * Moves the clock forward, running whatever falls due.
     *
     * <p>A repeating task may come due more than once in a single advance, and will run once for
     * each time it does.
     *
     * @param ticks how far to move forward; must not be negative
     * @return the number of task runs that happened
     */
    public int advance(long ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Time does not run backwards, got " + ticks);
        }

        long target = currentTick + ticks;
        int runs = 0;

        while (true) {
            PendingTask next = nextDueBy(target);
            if (next == null) {
                break;
            }

            currentTick = next.dueAt;
            next.run();
            runs++;
        }

        currentTick = target;
        return runs;
    }

    /** Runs everything currently due without moving the clock. */
    public int runDue() {
        return advance(0);
    }

    private PendingTask nextDueBy(long target) {
        return pending.stream()
                .filter(task -> !task.cancelled && task.dueAt <= target)
                .min(Comparator.comparingLong(task -> task.dueAt))
                .orElse(null);
    }

    private Task schedule(Runnable task, long delayTicks, long periodTicks) {
        PendingTask pendingTask =
                new PendingTask(task, currentTick + Math.max(1, delayTicks), periodTicks);
        pending.add(pendingTask);
        return pendingTask;
    }

    /** One piece of scheduled work. */
    private final class PendingTask implements Task {

        private final Runnable body;
        private final long periodTicks;

        private long dueAt;
        private boolean cancelled;

        PendingTask(Runnable body, long dueAt, long periodTicks) {
            this.body = body;
            this.dueAt = dueAt;
            this.periodTicks = periodTicks;
        }

        void run() {
            if (periodTicks > 0) {
                dueAt += periodTicks;
            } else {
                pending.remove(this);
            }
            body.run();
        }

        @Override
        public void cancel() {
            cancelled = true;
            pending.remove(this);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
