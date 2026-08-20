package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.Wiring;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jspecify.annotations.NullMarked;

/**
 * Finding the three blocks a cart mechanic is built from.
 *
 * <p>A mechanism is a piece of rail, the block under it, and a sign. The sign may be directly
 * under the base block, two under it, or against one of its four sides, and all three arrangements
 * are in the ground on servers that have been running for years.
 */
@NullMarked
public final class CartMechanisms {

    /** The sides a sign may be found on, and the order they are looked at in. */
    private static final BlockFace[] SIDES = {
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };

    private CartMechanisms() {}

    /**
     * The mechanism a piece of rail belongs to.
     *
     * <p>Empty when the block is not rail, or when what is under it cannot be read because its
     * chunk is not loaded.
     */
    public static Optional<CartMechanism> atRail(Block rail) {
        if (!CartBlocks.isRail(rail.getType())) {
            return Optional.empty();
        }

        Block base = rail.getRelative(BlockFace.DOWN);
        return Optional.of(new CartMechanism(
                Positions.toDomain(rail),
                Positions.toDomain(base),
                base.getType().getKey(),
                findSign(base)));
    }

    /**
     * The mechanism a sign belongs to.
     *
     * <p>Used when somebody puts a sign up, to work out what they have just built. Looks the other
     * way round from {@link #atRail}: up for the rail rather than down for the sign.
     */
    public static Optional<CartMechanism> atSign(Block sign) {
        if (!CartBlocks.isSign(sign)) {
            return Optional.empty();
        }

        Block twoUp = sign.getRelative(BlockFace.UP, 2);
        if (CartBlocks.isRail(twoUp.getType())) {
            return atRail(twoUp);
        }
        Block threeUp = sign.getRelative(BlockFace.UP, 3);
        if (CartBlocks.isRail(threeUp.getType())) {
            return atRail(threeUp);
        }

        // A sign on the side of the base block, which is how a mechanism is built where there is
        // no room below it.
        for (BlockFace side : SIDES) {
            Block acrossAndUp = sign.getRelative(side).getRelative(BlockFace.UP);
            if (CartBlocks.isRail(acrossAndUp.getType())) {
                return atRail(acrossAndUp);
            }
        }
        return Optional.empty();
    }

    /** The sign belonging to a mechanism's base block, wherever it is hung. */
    private static Optional<CartMechanism.MechanismSign> findSign(Block base) {
        Block below = base.getRelative(BlockFace.DOWN);
        if (CartBlocks.isSign(below)) {
            return CartBlocks.readSign(below);
        }
        Block twoBelow = below.getRelative(BlockFace.DOWN);
        if (CartBlocks.isSign(twoBelow)) {
            return CartBlocks.readSign(twoBelow);
        }
        for (BlockFace side : SIDES) {
            Block beside = base.getRelative(side);
            if (CartBlocks.isSign(beside)) {
                return CartBlocks.readSign(beside);
            }
        }
        return Optional.empty();
    }

    /**
     * Whether anything has been wired to a mechanism, and whether it is on.
     *
     * <p>Any of the three blocks being powered switches the mechanic on. A wire that is there and
     * reading low holds it back; no wire at all leaves it working as it always has, which is what
     * lets a switch be added to an existing mechanism without having to power it from then on.
     */
    public static Wiring wiringOf(Block rail, CartMechanism mechanism) {
        boolean anythingWired = false;

        for (Block part : partsOf(rail, mechanism)) {
            if (part.isBlockIndirectlyPowered() || part.isBlockPowered()) {
                return Wiring.ON;
            }
            if (isWiredTo(part)) {
                anythingWired = true;
            }
        }
        return anythingWired ? Wiring.OFF : Wiring.NONE;
    }

    /** The blocks that make up a mechanism, as blocks on the server. */
    private static Block[] partsOf(Block rail, CartMechanism mechanism) {
        Block base = rail.getRelative(BlockFace.DOWN);
        return mechanism.sign()
                .map(sign -> new Block[] {
                    rail, base, rail.getWorld().getBlockAt(
                            sign.position().x(), sign.position().y(), sign.position().z())
                })
                .orElseGet(() -> new Block[] {rail, base});
    }

    /**
     * Whether something that carries redstone is touching a block.
     *
     * <p>Only the four sides, matching where a builder runs a wire up to a mechanism.
     */
    private static boolean isWiredTo(Block block) {
        for (BlockFace side : SIDES) {
            if (CartBlocks.carriesRedstone(block.getRelative(side).getType())) {
                return true;
            }
        }
        return false;
    }
}
