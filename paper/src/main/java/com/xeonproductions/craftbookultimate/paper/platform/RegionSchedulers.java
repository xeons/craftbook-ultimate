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
