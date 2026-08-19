package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.entity.Traveller;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.HumanEntity;
import org.jspecify.annotations.NullMarked;

/**
 * A {@link ChipWorld} backed by a real world.
 *
 * <p>Blocks are named by their namespaced key in both directions, which is how the game itself
 * names them, so no mapping table is needed and blocks added by later versions work without
 * anything here changing.
 *
 * <p>Belongs to one region and must only be used from the thread owning the blocks it touches.
 */
@NullMarked
public record BukkitChipWorld(World world) implements ChipWorld {

    /** How far around a block to ask the server for entities before checking exactly where they are. */
    private static final double ENTITY_SEARCH_RADIUS = 1.0;

    @Override
    public UUID id() {
        return world.getUID();
    }

    @Override
    public Key blockAt(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return Blocks.AIR_KEY;
        }
        return Positions.toBlock(world, position).getType().getKey();
    }

    @Override
    public boolean setBlockAt(Vec3i position, Key block) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        Material material = Registry.MATERIAL.get(block);
        if (material == null || !material.isBlock()) {
            return false;
        }

        Block target = Positions.toBlock(world, position);
        if (target.getType() == material) {
            return false;
        }

        target.setType(material);
        return true;
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return world.isChunkLoaded(position.x() >> 4, position.z() >> 4);
    }

    @Override
    public boolean isPassable(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }
        return Positions.toBlock(world, position).isPassable();
    }

    @Override
    public List<Traveller> travellersIn(Vec3i position) {
        if (!isLoaded(position)) {
            return List.of();
        }

        // The server searches a box around a point, so it is asked for a little more than one
        // block and the results are then narrowed to the people actually standing in this one.
        return world
                .getNearbyEntitiesByType(
                        HumanEntity.class, Positions.toCentre(world, position), ENTITY_SEARCH_RADIUS)
                .stream()
                .filter(entity -> Positions.toDomain(entity.getLocation()).equals(position))
                .<Traveller>map(BukkitTraveller::new)
                .toList();
    }

    @Override
    public boolean releasePressurePlate(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return false;
        }

        Block block = Positions.toBlock(world, position);
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) {
            return false;
        }

        BlockData data = block.getBlockData();
        if (data instanceof Powerable plate) {
            if (!plate.isPowered()) {
                return false;
            }
            plate.setPowered(false);
            block.setBlockData(plate, true);
            return true;
        }

        // The weighted plates carry a level rather than a flag, since they read how much is
        // standing on them.
        if (data instanceof AnaloguePowerable plate) {
            if (plate.getPower() == 0) {
                return false;
            }
            plate.setPower(0);
            block.setBlockData(plate, true);
            return true;
        }

        return false;
    }

    @Override
    public int lightLevel(Vec3i position) {
        if (!isLoaded(position) || !isInBounds(position)) {
            return 0;
        }
        return Positions.toBlock(world, position).getLightLevel();
    }

    @Override
    public boolean isRaining() {
        return world.hasStorm();
    }

    @Override
    public boolean isThundering() {
        return world.isThundering();
    }

    @Override
    public void setRaining(boolean raining, int durationTicks) {
        world.setStorm(raining);
        world.setWeatherDuration(durationTicks);
    }

    @Override
    public void setThundering(boolean thundering, int durationTicks) {
        world.setThundering(thundering);
        world.setThunderDuration(durationTicks);
    }

    @Override
    public void setWorldTicks(long worldTicks) {
        world.setFullTime(worldTicks);
    }

    @Override
    public Optional<Key> resolveBlock(String written) {
        return LegacyBlocks.resolve(written);
    }

    @Override
    public int minHeight() {
        return world.getMinHeight();
    }

    @Override
    public int maxHeight() {
        return world.getMaxHeight();
    }
}
