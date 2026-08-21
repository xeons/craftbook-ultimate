// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.testbed;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * A flat plane carrying a working rig for every chip in the catalogue.
 *
 * <p>Laid out from {@link ICRegistry} rather than drawn once, for the reason the catalogue page is
 * generated rather than written: a chip added to the catalogue and forgotten in the test bed is a
 * chip nobody tries, and a rig wired from a remembered pin layout rather than the real one is a
 * chip that reads as broken when it is the bed that is wrong.
 *
 * <p>Every rig is spaced by the widest and deepest rig in the whole plan, so the grid is even and
 * no rig can reach into its neighbour whatever layout it uses. That wastes a little ground under
 * the smaller chips and is worth it: a rig that overlapped the next one would look like a chip
 * misbehaving.
 *
 * @param origin the north-west corner of the plane
 * @param facing the direction every sign faces
 * @param columns how many rigs there are in a row
 * @param cell how far apart the rigs are, on both axes
 * @param rigs the rigs themselves, in catalogue order
 */
@NullMarked
public record Testbed(Vec3i origin, BlockFace facing, int columns, Cell cell, List<Rig> rigs) {

    /** Ground left clear between one rig and the next. */
    private static final int GAP = 2;

    /** How wide the plane is allowed to get before it wraps to another row. */
    private static final int DEFAULT_COLUMNS = 10;

    public Testbed {
        rigs = List.copyOf(rigs);
    }

    /** How far apart rigs sit. */
    public record Cell(int width, int depth) {}

    /** Lays out a plane for a whole catalogue. */
    public static Testbed plan(ICRegistry registry, Vec3i origin, BlockFace facing) {
        return plan(registry, origin, facing, DEFAULT_COLUMNS);
    }

    /**
     * Lays out a plane for a whole catalogue.
     *
     * <p>Planned twice over: once at the origin to find out how much room the biggest rig needs,
     * and again with every rig spaced by that. Measuring the real rigs rather than assuming a size
     * means a layout added later cannot quietly overflow its cell.
     *
     * @param registry the chips to build rigs for
     * @param origin the north-west corner of the plane
     * @param facing the direction every sign faces
     * @param columns how many rigs to a row
     */
    public static Testbed plan(
            ICRegistry registry, Vec3i origin, BlockFace facing, int columns) {

        List<ICDefinition> chips = new ArrayList<>(registry.definitions());
        chips.sort(Comparator.comparing(ICDefinition::model));

        Cell cell = measure(chips, facing);
        int across = Math.max(1, columns);

        List<Rig> rigs = new ArrayList<>();
        for (int index = 0; index < chips.size(); index++) {
            ICDefinition chip = chips.get(index);
            Vec3i sign = origin.add(
                    (index % across) * cell.width(),
                    Rig.SIGN_HEIGHT,
                    (index / across) * cell.depth());
            rigs.add(Rig.forChip(chip, ChipSetup.forModel(chip.model()), sign, facing));
        }

        return new Testbed(origin, facing, across, cell, rigs);
    }

    /** How much room the biggest rig in a catalogue needs, plus the gap between rigs. */
    private static Cell measure(List<ICDefinition> chips, BlockFace facing) {
        int width = 1;
        int depth = 1;
        for (ICDefinition chip : chips) {
            Rig.Bounds bounds =
                    Rig.forChip(chip, ChipSetup.forModel(chip.model()), Vec3i.ZERO, facing).bounds();
            width = Math.max(width, bounds.width());
            depth = Math.max(depth, bounds.depth());
        }
        return new Cell(width + GAP, depth + GAP);
    }

    /** How many rigs there are. */
    public int size() {
        return rigs.size();
    }

    /** How many rows the plane runs to. */
    public int rows() {
        return (rigs.size() + columns - 1) / Math.max(1, columns);
    }

    /**
     * The ground the plane needs, as its two opposite corners.
     *
     * <p>Worked out from where the rigs actually reach rather than from the grid, so the floor
     * covers the parts of a rig that hang outside its own cell.
     */
    public Ground ground() {
        int minX = origin.x();
        int maxX = origin.x();
        int minZ = origin.z();
        int maxZ = origin.z();

        for (Rig rig : rigs) {
            for (Rig.Placement placement : rig.placements()) {
                minX = Math.min(minX, placement.position().x());
                maxX = Math.max(maxX, placement.position().x());
                minZ = Math.min(minZ, placement.position().z());
                maxZ = Math.max(maxZ, placement.position().z());
            }
        }

        return new Ground(
                new Vec3i(minX - 1, origin.y(), minZ - 1),
                new Vec3i(maxX + 1, origin.y(), maxZ + 1));
    }

    /**
     * Everything building this bed writes over: the floor, and all the air the rigs stand up into.
     *
     * <p>Whoever builds one has to clear the chips already standing there first, and a block
     * replaced wholesale raises no break event to do it for them. Measured from the rigs
     * themselves rather than from a remembered height, so a rig that grows taller cannot quietly
     * start leaving the previous bed's chips behind.
     */
    public Bounds overwritten() {
        Ground ground = ground();
        int highest = ground.from().y();
        for (Rig rig : rigs) {
            for (Rig.Placement placement : rig.placements()) {
                highest = Math.max(highest, placement.position().y());
            }
        }
        return new Bounds(ground.from(), new Vec3i(ground.to().x(), highest, ground.to().z()));
    }

    /** The floor a plane stands on, as two opposite corners at the same height. */
    public record Ground(Vec3i from, Vec3i to) {

        /** How many blocks the floor covers. */
        public int area() {
            return (to.x() - from.x() + 1) * (to.z() - from.z() + 1);
        }
    }

    /**
     * Every chip the test bed knows it cannot finish setting up, in catalogue order.
     *
     * <p>Only the ones explicitly marked, which in practice means the ones wanting a file an
     * operator has to supply. A chip missing from the setup table is not here: most of the
     * catalogue needs nothing said to it, and reporting every unconfigured chip as broken would
     * bury the few that really are unfinished.
     */
    public List<ICDefinition> needingSetup() {
        return rigs.stream()
                .map(Rig::chip)
                .filter(chip -> !ChipSetup.forModel(chip.model()).note().isEmpty())
                .toList();
    }

    /**
     * Every chip whose sign was left blank because the setup table has no entry for it.
     *
     * <p>Most of these are right to be blank — a logic gate wired to levers needs nothing. The
     * rest are chips whose third and fourth lines nobody has filled in yet, and the catalogue
     * does not record which is which, so this is a list to read rather than a fault to report.
     */
    public List<ICDefinition> unconfigured() {
        return rigs.stream()
                .map(Rig::chip)
                .filter(chip -> !ChipSetup.isKnown(chip.model()))
                .toList();
    }
}
