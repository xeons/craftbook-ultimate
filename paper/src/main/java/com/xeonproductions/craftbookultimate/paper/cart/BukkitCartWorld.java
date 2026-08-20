package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.cart.Cart;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.CartRecipe;
import com.xeonproductions.craftbookultimate.core.cart.CartWorld;
import com.xeonproductions.craftbookultimate.core.cart.RailShape;
import com.xeonproductions.craftbookultimate.core.cart.VehicleKind;
import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitBystander;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitDroppedItem;
import com.xeonproductions.craftbookultimate.paper.ic.LegacyBlocks;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * The world a cart mechanic runs in, on a real server.
 *
 * <p>Bound to one world, and only ever asked about the few blocks around a mechanism, so nothing
 * here reaches beyond what its own thread owns.
 */
@NullMarked
public record BukkitCartWorld(World world, CartRecipes recipes) implements CartWorld {

    @Override
    public Key blockAt(Vec3i position) {
        return blockAt(position, world).getType().getKey();
    }

    @Override
    public boolean isRail(Vec3i position) {
        return CartBlocks.isRail(blockAt(position, world).getType());
    }

    @Override
    public Optional<RailShape> railShapeAt(Vec3i position) {
        BlockData data = blockAt(position, world).getBlockData();
        if (!(data instanceof Rail rail)) {
            return Optional.empty();
        }
        return Optional.of(RailShape.valueOf(rail.getShape().name()));
    }

    @Override
    public boolean setRailShapeAt(Vec3i position, RailShape shape) {
        Block block = blockAt(position, world);
        BlockData data = block.getBlockData();
        if (!(data instanceof Rail rail)) {
            return false;
        }
        Rail.Shape wanted = Rail.Shape.valueOf(shape.name());
        if (!rail.getShapes().contains(wanted)) {
            // A powered or detector rail bends only along the four straight ways, so a junction
            // asking one to curve is refused rather than left in an impossible state.
            return false;
        }
        rail.setShape(wanted);
        block.setBlockData(rail, false);
        return true;
    }

    @Override
    public Optional<CartMechanism.MechanismSign> signAt(Vec3i position) {
        return CartBlocks.readSign(blockAt(position, world));
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return world.isChunkLoaded(position.x() >> 4, position.z() >> 4);
    }

    @Override
    public Set<BlockFace> climbableSidesAt(Vec3i position) {
        if (!isLoaded(position)) {
            return Set.of();
        }
        BlockData data = world.getBlockAt(position.x(), position.y(), position.z()).getBlockData();
        if (data instanceof Ladder ladder) {
            // A ladder's facing points away from the wall it is nailed to.
            return sidesOf(List.of(ladder.getFacing().getOppositeFace()));
        }
        if (data instanceof MultipleFacing vine && data.getMaterial() == Material.VINE) {
            return sidesOf(vine.getFaces());
        }
        return Set.of();
    }

    /** The horizontal sides among a set of faces, as the domain names them. */
    private static Set<BlockFace> sidesOf(Iterable<org.bukkit.block.BlockFace> faces) {
        Set<BlockFace> sides = new LinkedHashSet<>();
        for (org.bukkit.block.BlockFace face : faces) {
            Directions.toDomain(face).filter(BlockFace::isHorizontal).ifPresent(sides::add);
        }
        return sides;
    }

    @Override
    public int minHeight() {
        return world.getMinHeight();
    }

    @Override
    public int maxHeight() {
        return world.getMaxHeight();
    }

    @Override
    public List<Cart> cartsNear(Vec3d centre, double radius) {
        List<Cart> found = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(
                new Location(world, centre.x(), centre.y(), centre.z()), radius, radius, radius)) {
            if (entity instanceof Minecart minecart) {
                found.add(new BukkitCart(minecart));
            }
        }
        return found;
    }

    @Override
    public List<Bystander> playersNear(Vec3d centre, double radius) {
        List<Bystander> found = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(
                new Location(world, centre.x(), centre.y(), centre.z()), radius, radius, radius)) {
            if (!(entity instanceof Player player) || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            BukkitBystander waiting = new BukkitBystander(player);
            if (waiting.isVisible()) {
                found.add(waiting);
            }
        }
        return found;
    }

    @Override
    public List<DroppedItem> itemsNear(Vec3d centre, double radius) {
        List<DroppedItem> found = new ArrayList<>();
        for (Item item : world.getNearbyEntitiesByType(
                Item.class, new Location(world, centre.x(), centre.y(), centre.z()), radius)) {
            if (item.isValid()) {
                found.add(new BukkitDroppedItem(item));
            }
        }
        return found;
    }

    @Override
    public Stockpile containersAt(List<Vec3i> positions) {
        List<Inventory> found = new ArrayList<>();
        Set<Location> alreadyCounted = new LinkedHashSet<>();

        for (Vec3i position : positions) {
            Block block = blockAt(position, world);
            if (!(block.getState(false) instanceof InventoryHolder holder)) {
                continue;
            }
            Inventory inventory = holder.getInventory();
            // The two halves of a double chest share one inventory, so counting each half would
            // count what is in it twice.
            if (!alreadyCounted.add(inventory.getLocation() == null
                    ? block.getLocation()
                    : inventory.getLocation())) {
                continue;
            }
            found.add(inventory);
        }
        return new SpreadStockpile(found);
    }

    @Override
    public boolean dropItem(Vec3d at, Key item, int count) {
        Material material = Material.matchMaterial(item.asString());
        if (material == null || count < 1) {
            return false;
        }
        world.dropItemNaturally(
                new Location(world, at.x(), at.y(), at.z()), new ItemStack(material, count));
        return true;
    }

    @Override
    public Optional<Cart> spawnVehicle(Vec3d at, VehicleKind kind, Optional<String> name) {
        Location where = new Location(world, at.x(), at.y(), at.z());
        Optional<EntityType> type = CartBlocks.entityTypeOf(kind);
        if (type.isEmpty()) {
            return Optional.empty();
        }

        Entity put = world.spawnEntity(where, type.get());
        name.ifPresent(given -> put.customName(Component.text(given)));
        if (put instanceof Minecart minecart) {
            return Optional.of(new BukkitCart(minecart));
        }
        if (put instanceof Boat) {
            // A boat is a vehicle a dispenser hands out but not a cart anything can act on, so
            // there is nothing to hand back.
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public Optional<CartRecipe> recipeNamed(String signName) {
        return recipes.byName(signName);
    }

    @Override
    public Optional<Key> resolveItem(String written) {
        return LegacyBlocks.resolveItem(written);
    }

    private static Block blockAt(Vec3i position, World world) {
        return Positions.toBlock(world, position);
    }
}
