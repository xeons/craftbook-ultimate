package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * Putting a bridge or a door up, and taking it down again.
 *
 * <p>The two mechanics differ in how they measure their box and not at all in what they do with
 * it: fill every gap in it out of the chests nearby, or empty it back into them.
 *
 * <p>Neither half is done by halves. Filling takes every block it needs before it lays any of
 * them, so a chest that is one short leaves the bridge retracted rather than leaving a hole
 * somebody walks into; emptying checks there is room for all of them before it takes any away,
 * so a full chest leaves the bridge out rather than dropping half of it on the floor.
 */
@NullMarked
final class Panels {

    private Panels() {}

    /**
     * Whether a block is one of these mechanics could be built from.
     *
     * <p>Air is not, however permissive the settings are: a bridge made of nothing is a sign with
     * nothing under it.
     */
    static boolean isBuildable(Key block, Settings settings) {
        return !Blocks.AIR.contains(block) && settings.mayPlace(block);
    }

    /**
     * How far a structure carries on to one side.
     *
     * <p>Both ends have to agree, so a bridge is as wide as the narrower of its two landings and
     * a door is as wide as the narrower of its two frames.
     *
     * @param world where to look
     * @param one one end's edge block
     * @param other the other end's edge block
     * @param towards which way to count
     * @param material what both ends are made of
     * @param limit the furthest to count, from the settings
     */
    static int widthAlong(
            MechanicWorld world,
            Vec3i one,
            Vec3i other,
            BlockFace towards,
            Key material,
            int limit) {
        int width = 0;
        for (int step = 1; step <= limit; step++) {
            if (!world.blockAt(one.offset(towards, step)).equals(material)) {
                break;
            }
            if (!world.blockAt(other.offset(towards, step)).equals(material)) {
                break;
            }
            width++;
        }
        return width;
    }

    /**
     * Whether anything that is neither air nor the structure itself is in the way.
     *
     * <p>A structure driven by redstone builds around an obstruction rather than complaining
     * about it, because there is nobody to complain to and a gate held shut by a signal should
     * stay shut.
     */
    static boolean isObstructed(MechanicWorld world, Panel panel, Key material) {
        boolean[] blocked = {false};
        panel.forEach(position -> {
            if (blocked[0]) {
                return;
            }
            Key block = world.blockAt(position);
            if (!Blocks.AIR.contains(block) && !block.equals(material)) {
                blocked[0] = true;
            }
        });
        return blocked[0];
    }

    /**
     * Fills the gaps in a box, or empties it, whichever the visit calls for.
     *
     * @param visit what set the mechanic off
     * @param panel the box to work on
     * @param material what the structure is made of
     * @param thing what to call the structure when explaining a refusal
     * @return true if the mechanic acted
     */
    static boolean toggle(MechanicVisit visit, Panel panel, Key material, String thing) {
        List<Vec3i> gaps = positionsOf(visit.world(), panel, Blocks.AIR_KEY);
        boolean shut = visit.askedToShut().orElse(!gaps.isEmpty());
        return shut
                ? fill(visit, gaps, material, thing)
                : empty(visit, positionsOf(visit.world(), panel, material), material, thing);
    }

    /** Lays the structure's material in every gap. */
    private static boolean fill(
            MechanicVisit visit, List<Vec3i> gaps, Key material, String thing) {
        if (gaps.isEmpty()) {
            return true;
        }
        if (!visit.settings().mayPlace(material)) {
            visit.complain("A " + thing + " cannot be made of " + material.value() + ".");
            return false;
        }

        Stockpile stockpile = visit.stockpile();
        if (!stockpile.takeAll(material, gaps.size())) {
            visit.complain("There are not enough blocks nearby to put the " + thing + " out.");
            return false;
        }

        int laid = 0;
        for (Vec3i position : gaps) {
            if (visit.world().setBlockAt(position, material)) {
                laid++;
            }
        }
        stockpile.give(material, gaps.size() - laid);
        return true;
    }

    /** Takes the structure's material away and puts it back in the chests. */
    private static boolean empty(
            MechanicVisit visit, List<Vec3i> laid, Key material, String thing) {
        if (laid.isEmpty()) {
            return true;
        }

        Stockpile stockpile = visit.stockpile();
        if (!stockpile.hasRoomFor(material, laid.size())) {
            visit.complain("There is nowhere to put the " + thing + "'s blocks.");
            return false;
        }

        int taken = 0;
        for (Vec3i position : laid) {
            if (visit.world().clearAt(position)) {
                taken++;
            }
        }
        stockpile.give(material, taken);
        return true;
    }

    /** Every position in a box holding a particular block. */
    private static List<Vec3i> positionsOf(MechanicWorld world, Panel panel, Key wanted) {
        List<Vec3i> found = new ArrayList<>();
        boolean air = Blocks.AIR.contains(wanted);
        panel.forEach(position -> {
            Key block = world.blockAt(position);
            if (air ? Blocks.AIR.contains(block) : block.equals(wanted)) {
                found.add(position);
            }
        });
        return found;
    }
}
