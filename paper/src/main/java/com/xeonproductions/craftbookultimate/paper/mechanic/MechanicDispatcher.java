// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.mechanic;

import com.xeonproductions.craftbookultimate.core.area.AreaVault;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.mechanic.MechanicVisit;
import com.xeonproductions.craftbookultimate.core.mechanic.PostedSign;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanic;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanics;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Works out which sign mechanic a click or a redstone change belongs to, and runs it.
 *
 * <p>One place that resolves the sign and checks what is allowed, rather than each mechanic
 * watching the world for itself. Three things can set a mechanic off and all three come through
 * here: a hand on its own sign, a hand on something standing in for that sign — a button in front
 * of a lift, a fence a gate is made of — and redstone arriving beside it.
 */
@NullMarked
public final class MechanicDispatcher {

    /** How far in front of a lift's sign its button is. */
    private static final int BUTTON_REACH = 2;

    /** The sides power may arrive from, and the block itself. */
    private static final BlockFace[] NEIGHBOURS = {
        BlockFace.SELF,
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST,
        BlockFace.UP,
        BlockFace.DOWN
    };

    private final Configuration configuration;
    private final AreaVault vault;

    public MechanicDispatcher(Configuration configuration, AreaVault vault) {
        this.configuration = configuration;
        this.vault = vault;
    }

    /** The world as a mechanic sees it, with the saved areas behind it. */
    public BukkitMechanicWorld worldOf(World world) {
        return new BukkitMechanicWorld(world, vault);
    }

    /**
     * Runs whatever mechanic a right click belongs to.
     *
     * @param clicked the block that was clicked
     * @param interactionPoint exactly where on it the click landed, if the server said
     * @param who clicked it
     * @return true if a mechanic claimed the click, which is what stops it doing anything else
     */
    public boolean onInteract(Block clicked, Optional<Location> interactionPoint, Player who) {
        World world = clicked.getWorld();
        Settings settings = configuration.settings();
        if (!settings.allowsWorld(world.getName())) {
            return false;
        }

        BukkitMechanicWorld mechanicWorld = worldOf(world);
        PlayerActor actor = new PlayerActor(who);
        OptionalDouble height = heightWithin(clicked, interactionPoint);

        Optional<PostedSign> onTheSign = mechanicWorld.signAt(Positions.toDomain(clicked));
        if (onTheSign.isPresent()) {
            return run(onTheSign.get(), height, mechanicWorld, settings, actor);
        }

        if (settings.mechanics().elevator().buttons() && Tag.BUTTONS.isTagged(clicked.getType())) {
            Optional<PostedSign> behind = signBehind(mechanicWorld, clicked);
            if (behind.isPresent()
                    && SignMechanics.elevator().claims(behind.get().lines())) {
                return run(behind.get(), height, mechanicWorld, settings, actor);
            }
        }

        if (!who.isSneaking() && settings.mechanics().gate().clicking()) {
            Optional<PostedSign> gateSign = gateSignFor(clicked, mechanicWorld, settings);
            if (gateSign.isPresent()) {
                return run(gateSign.get(), height, mechanicWorld, settings, actor);
            }
        }
        return false;
    }

    /**
     * Runs the mechanics beside a block whose redstone signal has changed.
     *
     * <p>Power arriving shuts them and power leaving opens them, so a bridge wired to a lever
     * follows the lever however many times it is thrown and however many other things are
     * flickering nearby.
     */
    public void onRedstone(Block changed, boolean powered) {
        Settings settings = configuration.settings();
        if (!settings.mechanics().redstone()
                || !settings.allowsWorld(changed.getWorld().getName())) {
            return;
        }

        BukkitMechanicWorld world = worldOf(changed.getWorld());
        for (BlockFace side : NEIGHBOURS) {
            Block neighbour = changed.getRelative(side);
            if (!Tag.ALL_SIGNS.isTagged(neighbour.getType())) {
                continue;
            }
            Vec3i position = Positions.toDomain(neighbour);
            world.signAt(position).ifPresent(sign -> SignMechanics.claiming(sign.lines())
                    .map(SignMechanics.Claim::mechanic)
                    .filter(mechanic -> settings.mechanics().allows(mechanic.name()))
                    .ifPresent(mechanic -> mechanic.act(
                            MechanicVisit.byRedstone(sign, world, settings, powered))));
        }
    }

    /**
     * Carries somebody who has jumped or crouched on a two-way lift.
     *
     * @param sign the lift's sign
     * @param up whether they jumped rather than crouched
     * @return true if they were carried
     */
    public boolean rideLift(PostedSign sign, World world, Player who, boolean up) {
        Settings settings = configuration.settings();
        if (!settings.allowsWorld(world.getName())
                || !settings.mechanics().allows(SignMechanics.elevator().name())) {
            return false;
        }
        return SignMechanics.elevator().ride(
                MechanicVisit.byHand(
                        sign, worldOf(world), settings, new PlayerActor(who)),
                up
                        ? com.xeonproductions.craftbookultimate.core.math.BlockFace.UP
                        : com.xeonproductions.craftbookultimate.core.math.BlockFace.DOWN);
    }

    /** The settings currently in force. */
    public Settings settings() {
        return configuration.settings();
    }

    /** Reads the sign at a block, whatever mechanic it belongs to. */
    public Optional<PostedSign> signAt(Block block) {
        return worldOf(block.getWorld()).signAt(Positions.toDomain(block));
    }

    /**
     * Runs the mechanic a sign names.
     *
     * @return true if the mechanic claimed the click, whether or not it managed to do anything
     */
    private static boolean run(
            PostedSign sign,
            OptionalDouble height,
            BukkitMechanicWorld world,
            Settings settings,
            PlayerActor actor) {
        Optional<SignMechanics.Claim> claim = SignMechanics.claiming(sign.lines());
        if (claim.isEmpty()) {
            return false;
        }

        SignMechanic mechanic = claim.get().mechanic();
        if (!settings.mechanics().allows(mechanic.name())) {
            return false;
        }
        if (!actor.mayUse(mechanic.usePermission())) {
            actor.complain(
                    "You may not use a " + mechanic.name().toLowerCase(Locale.ROOT) + ".");
            return true;
        }

        mechanic.act(MechanicVisit.byHand(sign, height, world, settings, actor));
        return true;
    }

    /**
     * The sign a button stands in front of.
     *
     * <p>Two blocks behind it, so the button sits on the near face of a wall and the sign on the
     * far one. That is how a lift is built where the shaft is on the other side of the wall from
     * the person calling it.
     */
    private static Optional<PostedSign> signBehind(BukkitMechanicWorld world, Block button) {
        if (!(button.getBlockData() instanceof Directional directional)) {
            return Optional.empty();
        }
        BlockFace back = directional.getFacing().getOppositeFace();
        return world.signAt(Positions.toDomain(button.getRelative(back, BUTTON_REACH)));
    }

    /**
     * The sign of the gate a block belongs to, where the block is a gate's material and some
     * gate near it says it answers to a hand on its own fence.
     */
    private static Optional<PostedSign> gateSignFor(
            Block clicked, BukkitMechanicWorld world, Settings settings) {
        Material material = clicked.getType();
        if (!settings.mechanics().gate().allows(material.getKey())) {
            return Optional.empty();
        }

        int radius = settings.mechanics().gate().radius();
        World bukkitWorld = clicked.getWorld();
        int x = clicked.getX();
        int y = clicked.getY();
        int z = clicked.getZ();

        for (int dy = radius * 2; dy >= -radius; dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block candidate = bukkitWorld.getBlockAt(x + dx, y + dy, z + dz);
                    if (!Tag.ALL_SIGNS.isTagged(candidate.getType())) {
                        continue;
                    }
                    Optional<PostedSign> sign = world.signAt(Positions.toDomain(candidate))
                            .filter(found -> SignMechanics.gate()
                                    .answersToTouchOn(found, material.getKey(), settings));
                    if (sign.isPresent()) {
                        return sign;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** How far up a block's own height the click landed, from nothing at all if unknown. */
    private static OptionalDouble heightWithin(Block block, Optional<Location> point) {
        return point.map(at -> OptionalDouble.of(at.getY() - block.getY()))
                .orElseGet(OptionalDouble::empty);
    }
}
