// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.platform;

import org.jspecify.annotations.NullMarked;

/**
 * Runs work later, on whichever thread owns the thing being worked on.
 *
 * <p>Chips and mechanics schedule through this rather than through a server API directly, which
 * keeps their timing behaviour testable and keeps them honest on a regionised server: an
 * instance is always bound to one place in the world, and the work it runs happens on the thread
 * that owns that place.
 *
 * <p>Delays are counted in server ticks, twenty to the second.
 */
@NullMarked
public interface Scheduler {

    /**
     * Runs a task once, after a delay.
     *
     * @param task the work to run
     * @param delayTicks how long to wait; anything below one tick runs on the next tick
     * @return a handle for cancelling the task before it runs
     */
    Task runLater(Runnable task, long delayTicks);

    /**
     * Runs a task over and over until it is cancelled.
     *
     * @param task the work to run
     * @param delayTicks how long to wait before the first run
     * @param periodTicks how long to wait between runs; must be at least one tick
     * @return a handle for stopping the task
     */
    Task runRepeating(Runnable task, long delayTicks, long periodTicks);

    /** A scheduled piece of work that has not necessarily run yet. */
    interface Task {

        /**
         * Stops the task running again.
         *
         * <p>Cancelling a task that has already finished, or has already been cancelled, does
         * nothing.
         */
        void cancel();

        /** Whether the task has been cancelled. */
        boolean isCancelled();
    }
}
