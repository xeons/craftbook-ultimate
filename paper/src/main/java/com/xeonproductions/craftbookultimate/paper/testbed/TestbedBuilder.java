package com.xeonproductions.craftbookultimate.paper.testbed;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.testbed.ChipSetup;
import com.xeonproductions.craftbookultimate.core.testbed.Rig;
import com.xeonproductions.craftbookultimate.core.testbed.Testbed;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Puts a {@link Testbed} into the world.
 *
 * <p>Everything about which block plays which part lives here rather than in the plan, so the
 * layout stays something a plain unit test can check without a server.
 *
 * <p>Work is handed to the region that owns each rig, so this is safe on a regionised server even
 * though a plane of a hundred rigs is far wider than any one region. Each rig is small and
 * contiguous, so no single rig ever spans two.
 */
@NullMarked
public final class TestbedBuilder {

    /** What the floor is made of, so a rig's own blocks stand out against it. */
    private static final Material FLOOR = Material.SMOOTH_STONE;

    /** What a lever stands on. */
    private static final Material MOUNT = Material.POLISHED_ANDESITE;

    /** What a sign hangs on. */
    private static final Material BACKING = Material.STONE_BRICKS;

    /** What lights up under an output. */
    private static final Material INDICATOR = Material.REDSTONE_LAMP;

    private static final Material WALL_SIGN = Material.OAK_WALL_SIGN;
    private static final Material STANDING_SIGN = Material.OAK_SIGN;

    private final ICManager manager;
    private final RegionSchedulers schedulers;

    public TestbedBuilder(ICManager manager, RegionSchedulers schedulers) {
        this.manager = manager;
        this.schedulers = schedulers;
    }

    /**
     * Builds a plane, and starts every chip on it.
     *
     * <p>The variable the variable chips name is made first. A chip's sign is not written by a
     * player here, so nothing reviews it, and a chip naming a variable that does not exist would
     * load and then quietly do nothing — which is exactly the failure the review normally
     * prevents.
     *
     * @param world where to build
     * @param plan what to build
     * @return how many chips came alive
     */
    public int build(World world, Testbed plan) {
        Variables variables = manager.services().variables();
        VariableName shared = VariableName.shared(ChipSetup.SHARED_NAME);
        variables.define(shared, Variables.DEFAULT_VALUE);

        layFloor(world, plan);

        List<Block> signs = new ArrayList<>();
        for (Rig rig : plan.rigs()) {
            schedulers.executeAt(world, rig.signPosition(), () -> place(world, rig, signs));
        }

        manager.loadAll(signs);
        return signs.size();
    }

    /** Clears the plane's air and lays its floor. */
    private void layFloor(World world, Testbed plan) {
        Testbed.Ground ground = plan.ground();
        for (int x = ground.from().x(); x <= ground.to().x(); x++) {
            for (int z = ground.from().z(); z <= ground.to().z(); z++) {
                Vec3i floor = new Vec3i(x, ground.from().y(), z);
                schedulers.executeAt(world, floor, () -> {
                    Positions.toBlock(world, floor).setType(FLOOR, false);
                    for (int above = 1; above <= CLEARANCE; above++) {
                        Positions.toBlock(world, floor.add(0, above, 0)).setType(Material.AIR, false);
                    }
                });
            }
        }
    }

    /** How much air is cleared above the floor, which is enough for the tallest rig and its label. */
    private static final int CLEARANCE = 6;

    /** Puts one rig in the world. */
    private void place(World world, Rig rig, List<Block> signs) {
        for (Rig.Placement placement : rig.placements()) {
            Block block = Positions.toBlock(world, placement.position());
            switch (placement.fixture()) {
                case Rig.Fixture.Mount ignored -> block.setType(MOUNT, false);
                case Rig.Fixture.Backing ignored -> block.setType(BACKING, false);
                case Rig.Fixture.Indicator ignored -> block.setType(INDICATOR, false);
                case Rig.Fixture.InputLever lever ->
                        placeLever(block, lever.mountedOn(), lever.facing());
                case Rig.Fixture.OutputLever lever ->
                        placeLever(block, lever.mountedOn(), lever.facing());
                case Rig.Fixture.Prop prop -> block.setType(materialOf(prop.block()), false);
                case Rig.Fixture.Chest chest -> fillChest(block, chest.contents());
                case Rig.Fixture.ChipSign sign -> {
                    placeWallSign(block, rig.facing(), sign.lines());
                    signs.add(block);
                }
                case Rig.Fixture.LabelSign label ->
                        placeStandingSign(block, rig.facing(), label.lines());
            }
        }
    }

    /**
     * Puts a lever on the block below.
     *
     * <p>Standing on the floor rather than clinging to a wall, because a pin may sit anywhere
     * around a sign and only the block underneath one is somewhere a rig can be sure of.
     */
    private static void placeLever(Block block, BlockFace mountedOn, BlockFace facing) {
        block.setType(Material.LEVER, false);
        BlockData data = block.getBlockData();

        if (data instanceof FaceAttachable attachable) {
            attachable.setAttachedFace(switch (mountedOn) {
                case DOWN -> FaceAttachable.AttachedFace.FLOOR;
                case UP -> FaceAttachable.AttachedFace.CEILING;
                default -> FaceAttachable.AttachedFace.WALL;
            });
        }
        // Always horizontal, whatever the lever clings to: the server refuses a vertical facing
        // outright. Which one it is was decided with the rest of the geometry.
        if (data instanceof Directional directional) {
            directional.setFacing(Directions.toServer(facing));
        }
        block.setBlockData(data, false);
    }

    /** Hangs a sign on the block behind it and writes it. */
    private static void placeWallSign(
            Block block, BlockFace facing, com.xeonproductions.craftbookultimate.core.sign.SignLines lines) {

        block.setType(WALL_SIGN, false);
        BlockData data = block.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(Directions.toServer(facing));
        }
        block.setBlockData(data, false);
        Signs.at(block).ifPresent(sign -> Signs.write(sign, lines));
    }

    /** Stands a sign on the ground facing back towards the rig. */
    private static void placeStandingSign(
            Block block, BlockFace facing, com.xeonproductions.craftbookultimate.core.sign.SignLines lines) {

        block.setType(STANDING_SIGN, false);
        BlockData data = block.getBlockData();
        if (data instanceof Rotatable rotatable) {
            rotatable.setRotation(Directions.toServer(facing));
        }
        block.setBlockData(data, false);
        Signs.at(block).ifPresent(sign -> Signs.write(sign, lines));
    }

    /** Puts a chest down and fills it. */
    private static void fillChest(Block block, Map<Key, Integer> contents) {
        block.setType(Material.CHEST, false);
        if (!(block.getState() instanceof Container container)) {
            return;
        }
        contents.forEach((item, count) ->
                container.getInventory().addItem(new ItemStack(materialOf(item), count)));
        container.update(true, false);
    }

    /** The material a domain key names, falling back to stone so a rig is never left half-built. */
    private static Material materialOf(Key key) {
        Material material = Registry.MATERIAL.get(
                new NamespacedKey(key.namespace(), key.value()));
        return material == null ? Material.STONE : material;
    }
}
