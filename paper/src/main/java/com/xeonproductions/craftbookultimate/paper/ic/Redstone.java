package com.xeonproductions.craftbookultimate.paper.ic;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Powerable;
import org.jspecify.annotations.NullMarked;

/**
 * Reads redstone state off blocks.
 *
 * <p>A chip distinguishes three things about a pin: whether a builder wired anything to it,
 * whether that wire is carrying a signal, and how strong the signal is. The first is decided by
 * the kind of block sitting on the pin, the other two by that block's own state.
 *
 * <p>Reading the block's state rather than asking the world whether it is powered matters: a
 * lever emits regardless of what is powering it, and asking the world instead would scan all six
 * neighbours of every pin on every chip on every redstone change.
 */
@NullMarked
public final class Redstone {

    /** The strongest signal redstone carries. */
    public static final int FULL_POWER = 15;

    /**
     * The blocks that count as wiring a pin.
     *
     * <p>Buttons and pressure plates are handled by tag, since the set of wood types grows with
     * each release.
     */
    private static final Set<Material> POWER_SOURCES = EnumSet.of(
            Material.REDSTONE_WIRE,
            Material.REDSTONE_BLOCK,
            Material.REDSTONE_TORCH,
            Material.REDSTONE_WALL_TORCH,
            Material.REPEATER,
            Material.COMPARATOR,
            Material.OBSERVER,
            Material.LEVER,
            Material.DAYLIGHT_DETECTOR,
            Material.TRIPWIRE_HOOK,
            Material.TARGET,
            Material.SCULK_SENSOR,
            Material.CALIBRATED_SCULK_SENSOR,
            Material.LIGHTNING_ROD);

    private Redstone() {}

    /** Whether a block is the sort of thing a builder wires a pin with. */
    public static boolean isPowerSource(Block block) {
        Material type = block.getType();
        return POWER_SOURCES.contains(type)
                || Tag.BUTTONS.isTagged(type)
                || Tag.PRESSURE_PLATES.isTagged(type);
    }

    /** Whether a block is a permanent power source, used for the block behind a sign. */
    public static boolean isAlwaysOn(Block block) {
        return block.getType() == Material.REDSTONE_BLOCK;
    }

    /**
     * The signal a block is emitting, from 0 to 15.
     *
     * <p>Decided from the block's own state where it has one. A block that carries no redstone
     * state of its own falls back to asking the world whether it is being powered, which is the
     * more expensive path.
     */
    public static int powerLevel(Block block) {
        BlockData data = block.getBlockData();

        if (data instanceof AnaloguePowerable analogue) {
            return analogue.getPower();
        }
        if (data instanceof Powerable powerable) {
            return powerable.isPowered() ? FULL_POWER : 0;
        }
        if (block.getType() == Material.REDSTONE_BLOCK) {
            return FULL_POWER;
        }
        // A redstone torch reports its emission through whether it is lit, and is on when lit.
        if (data instanceof Lightable lightable) {
            return lightable.isLit() ? FULL_POWER : 0;
        }

        return block.getBlockPower();
    }

    /** Whether a block is emitting any signal at all. */
    public static boolean isPowered(Block block) {
        return powerLevel(block) > 0;
    }
}
