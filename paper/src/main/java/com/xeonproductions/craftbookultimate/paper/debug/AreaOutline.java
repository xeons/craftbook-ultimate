package com.xeonproductions.craftbookultimate.paper.debug;

import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Draws the edges of a box in particles, for one person.
 *
 * <p>The legacy fork asked WorldEdit to draw this through its selection overlay. No part of this
 * plugin depends on WorldEdit, so the box is drawn directly instead — which has the side benefit of
 * working for a builder who has no client mod at all.
 *
 * <p>Shown to the one player who asked, so several people can look at different chips in the same
 * place without drawing over each other, and nobody else sees anything.
 */
@NullMarked
public final class AreaOutline {

    /** How long the outline stays up. */
    private static final int SECONDS = 8;

    /** How often it is redrawn, in ticks. Particles live about a second, so this overlaps. */
    private static final int PERIOD = 10;

    /** How far apart the dots along an edge are. */
    private static final double SPACING = 0.5;

    /** The most dots one drawing will place, so an enormous box cannot flood a client. */
    private static final int MAX_DOTS = 4000;

    private final RegionSchedulers schedulers;

    public AreaOutline(RegionSchedulers schedulers) {
        this.schedulers = schedulers;
    }

    /**
     * Outlines a box for one player, for a few seconds.
     *
     * <p>The dots are worked out once and then sent again on a timer. Working them out is the
     * expensive half and the box does not move, so a box that would be too dense to draw is thinned
     * once rather than on every frame.
     */
    public void show(Player player, World world, Bounds bounds) {
        List<Location> dots = edgeDots(world, bounds);

        schedulers.at(player.getLocation()).runRepeating(new Runnable() {
            private int drawn;

            @Override
            public void run() {
                if (drawn++ >= SECONDS * 20 / PERIOD || !player.isOnline()) {
                    return;
                }
                for (Location dot : dots) {
                    player.spawnParticle(Particle.HAPPY_VILLAGER, dot, 1, 0, 0, 0, 0);
                }
            }
        }, 1, PERIOD);
    }

    /**
     * A dot every so often along each of the box's twelve edges.
     *
     * <p>Edges rather than faces or a fill: a filled box of any size is unreadable from inside it,
     * and a builder standing in a sensor's area is exactly who is asking.
     */
    private static List<Location> edgeDots(World world, Bounds bounds) {
        Vec3i from = bounds.from();
        Vec3i to = bounds.to();

        // The corners sit on block boundaries rather than block centres, so the outline encloses
        // the blocks it describes instead of running through the middle of the outermost ones.
        double x0 = from.x();
        double y0 = from.y();
        double z0 = from.z();
        double x1 = to.x() + 1.0;
        double y1 = to.y() + 1.0;
        double z1 = to.z() + 1.0;

        double step = spacingFor(bounds);
        List<Location> dots = new ArrayList<>();

        for (double x = x0; x <= x1 && dots.size() < MAX_DOTS; x += step) {
            dots.add(new Location(world, x, y0, z0));
            dots.add(new Location(world, x, y0, z1));
            dots.add(new Location(world, x, y1, z0));
            dots.add(new Location(world, x, y1, z1));
        }
        for (double y = y0; y <= y1 && dots.size() < MAX_DOTS; y += step) {
            dots.add(new Location(world, x0, y, z0));
            dots.add(new Location(world, x0, y, z1));
            dots.add(new Location(world, x1, y, z0));
            dots.add(new Location(world, x1, y, z1));
        }
        for (double z = z0; z <= z1 && dots.size() < MAX_DOTS; z += step) {
            dots.add(new Location(world, x0, y0, z));
            dots.add(new Location(world, x0, y1, z));
            dots.add(new Location(world, x1, y0, z));
            dots.add(new Location(world, x1, y1, z));
        }
        return dots;
    }

    /**
     * How far apart to space the dots.
     *
     * <p>Widened for a large box so that the count stays bounded. A sparse outline of something
     * enormous still says where its edges are; four thousand dots a second does not.
     */
    private static double spacingFor(Bounds bounds) {
        long edge = (long) bounds.width() + bounds.height() + bounds.length();
        double needed = edge * 4.0 / MAX_DOTS;
        return Math.max(SPACING, needed);
    }
}
