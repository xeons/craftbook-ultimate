package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.VehicleKind;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.EntityType;
import org.jspecify.annotations.NullMarked;

/**
 * Reading the blocks a cart mechanic is built from.
 *
 * <p>Signs are the awkward part. A cart mechanism's sign is usually a standing one under the block
 * holding the rail, but it may be a wall sign against the side of it, and the two say which way
 * they face in different ways.
 */
@NullMarked
public final class CartBlocks {

    private CartBlocks() {}

    /** Whether a block is rail of any kind. */
    public static boolean isRail(Material material) {
        return Tag.RAILS.isTagged(material);
    }

    /** Whether a block is a sign of any kind, standing or on a wall. */
    public static boolean isSign(Block block) {
        return Tag.ALL_SIGNS.isTagged(block.getType());
    }

    /**
     * The sign at a block, as a mechanic reads it.
     *
     * <p>Empty for anything that is not a sign, or for a sign facing a way a cart cannot travel.
     */
    public static Optional<CartMechanism.MechanismSign> readSign(Block block) {
        Optional<Sign> sign = Signs.at(block);
        Optional<BlockFace> facing = facingOf(block);
        if (sign.isEmpty() || facing.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CartMechanism.MechanismSign(
                Positions.toDomain(block), Signs.read(sign.get()), facing.get()));
    }

    /**
     * Which way a sign's front faces.
     *
     * <p>A wall sign faces away from what it hangs on. A standing sign may be turned to any of
     * sixteen directions, and is rounded to the nearest of the four a rail can run, since that is
     * the only precision a cart mechanic can use.
     */
    public static Optional<BlockFace> facingOf(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof WallSign wall) {
            return nearestCardinal(wall.getFacing());
        }
        if (data instanceof Rotatable rotatable) {
            return nearestCardinal(rotatable.getRotation());
        }
        return Optional.empty();
    }

    /** The cardinal direction closest to one of the sixteen a sign may be turned to. */
    private static Optional<BlockFace> nearestCardinal(org.bukkit.block.BlockFace face) {
        int east = face.getModX();
        int south = face.getModZ();
        if (east == 0 && south == 0) {
            return Optional.empty();
        }
        if (Math.abs(east) > Math.abs(south)) {
            return Optional.of(east > 0 ? BlockFace.EAST : BlockFace.WEST);
        }
        if (Math.abs(south) > Math.abs(east)) {
            return Optional.of(south > 0 ? BlockFace.SOUTH : BlockFace.NORTH);
        }
        // Exactly diagonal, which a sign turned to an odd angle can be. Either is as near as the
        // other, so the north-south axis is taken to keep the answer the same every time.
        return Optional.of(south > 0 ? BlockFace.SOUTH : BlockFace.NORTH);
    }

    /**
     * Whether a block is something a builder runs power through.
     *
     * <p>What tells a mechanism somebody has wired a switch to it from one nobody has touched.
     */
    public static boolean carriesRedstone(Material material) {
        return material == Material.REDSTONE_WIRE
                || material == Material.REPEATER
                || material == Material.COMPARATOR
                || material == Material.REDSTONE_TORCH
                || material == Material.REDSTONE_WALL_TORCH
                || material == Material.REDSTONE_BLOCK
                || material == Material.LEVER
                || Tag.BUTTONS.isTagged(material)
                || Tag.PRESSURE_PLATES.isTagged(material);
    }

    /** The entity a dispenser puts out for a kind of vehicle. */
    public static Optional<EntityType> entityTypeOf(VehicleKind kind) {
        return Optional.of(switch (kind) {
            case MINECART -> EntityType.MINECART;
            case STORAGE -> EntityType.CHEST_MINECART;
            case HOPPER -> EntityType.HOPPER_MINECART;
            case POWERED -> EntityType.FURNACE_MINECART;
            case BOAT -> EntityType.OAK_BOAT;
        });
    }
}
