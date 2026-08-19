package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    public void setWeather(boolean raining, boolean thundering) {
        world.setStorm(raining);
        world.setThundering(thundering);
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
