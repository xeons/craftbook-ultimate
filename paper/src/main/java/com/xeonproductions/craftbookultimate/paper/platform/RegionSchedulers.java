// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.platform;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * Builds {@link Scheduler}s bound to a place in the world.
 *
 * <p>On a regionised server every block belongs to a region owned by one thread, and work on that
 * block has to run on that thread. Binding a scheduler to a location is what makes that automatic
 * for callers: a chip asks its own scheduler to run something later and the work lands on the
 * right thread without the chip knowing regions exist.
 *
 * <p>The region schedulers exist on ordinary Paper too, where every region is the main thread, so
 * there is no separate path for the two servers.
 */
@NullMarked
public final class RegionSchedulers {

    /** The shortest delay the server accepts. A request for less is rounded up to this. */
    private static final long MINIMUM_DELAY_TICKS = 1;

    private final Plugin plugin;

    public RegionSchedulers(Plugin plugin) {
        this.plugin = plugin;
    }

    /** A scheduler that runs work on the region owning a block. */
    public Scheduler at(World world, Vec3i position) {
        return new LocationScheduler(plugin, new Location(world, position.x(), position.y(), position.z()));
    }

    /** A scheduler that runs work on the region owning a location. */
    public Scheduler at(Location location) {
        return new LocationScheduler(plugin, location.clone());
    }

    /**
     * A scheduler for work that belongs to no particular place, such as a periodic sweep over
     * plugin-wide state.
     */
    public Scheduler global() {
        return new GlobalScheduler(plugin);
    }

    /**
     * Whether the calling thread is allowed to touch a place in the world right now.
     *
     * <p>Always true on a server that ticks everything on one thread.
     */
    public boolean ownsCurrentThread(World world, Vec3i position) {
        return Bukkit.isOwnedByCurrentRegion(world, position.x() >> 4, position.z() >> 4);
    }

    /**
     * Runs work against a place in the world, on the thread allowed to touch it.
     *
     * <p>Runs straight away when the caller already owns that place, which is the usual case and
     * keeps a chain of chips settling within one redstone update. Otherwise the work is handed to
     * the owning region and runs shortly afterwards.
     *
     * <p>This is what makes an action at a distance safe: a chip that drives something far away
     * cannot reach into another region's blocks, but it can ask that region to do the work.
     *
     * @param world the world the work touches
     * @param position the place in that world the work touches
     * @param task the work to run
     * @return true if the work ran immediately, false if it was handed to another region
     */
    public boolean executeAt(World world, Vec3i position, Runnable task) {
        if (ownsCurrentThread(world, position)) {
            task.run();
            return true;
        }

        Bukkit.getRegionScheduler().execute(
                plugin, new Location(world, position.x(), position.y(), position.z()), task);
        return false;
    }

    /** Runs work on the region that owns one location. */
    private record LocationScheduler(Plugin plugin, Location location) implements Scheduler {

        @Override
        public Task runLater(Runnable task, long delayTicks) {
            return new TaskHandle(Bukkit.getRegionScheduler()
                    .runDelayed(plugin, location, scheduled -> task.run(), atLeastOneTick(delayTicks)));
        }

        @Override
        public Task runRepeating(Runnable task, long delayTicks, long periodTicks) {
            return new TaskHandle(Bukkit.getRegionScheduler().runAtFixedRate(
                    plugin,
                    location,
                    scheduled -> task.run(),
                    atLeastOneTick(delayTicks),
                    atLeastOneTick(periodTicks)));
        }
    }

    /** Runs work that is not tied to any region. */
    private record GlobalScheduler(Plugin plugin) implements Scheduler {

        @Override
        public Task runLater(Runnable task, long delayTicks) {
            return new TaskHandle(Bukkit.getGlobalRegionScheduler()
                    .runDelayed(plugin, scheduled -> task.run(), atLeastOneTick(delayTicks)));
        }

        @Override
        public Task runRepeating(Runnable task, long delayTicks, long periodTicks) {
            return new TaskHandle(Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    scheduled -> task.run(),
                    atLeastOneTick(delayTicks),
                    atLeastOneTick(periodTicks)));
        }
    }

    /** Wraps a server task so callers never see the server's own scheduling types. */
    private record TaskHandle(ScheduledTask task) implements Scheduler.Task {

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }

    private static long atLeastOneTick(long ticks) {
        return Math.max(MINIMUM_DELAY_TICKS, ticks);
    }
}
