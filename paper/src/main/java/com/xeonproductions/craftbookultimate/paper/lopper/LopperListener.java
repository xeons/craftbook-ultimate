// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.lopper;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.LopperSettings;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.config.TreeSettings;
import com.xeonproductions.craftbookultimate.core.lopper.LopperRules;
import com.xeonproductions.craftbookultimate.core.lopper.LopperSight;
import com.xeonproductions.craftbookultimate.core.lopper.Loppers;
import com.xeonproductions.craftbookultimate.core.lopper.TreeLoppers;
import com.xeonproductions.craftbookultimate.core.lopper.VeinMiners;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Felling a tree and mining a seam, both from one broken block.
 *
 * <p>One listener because they are one mechanic twice: the same {@link Loppers} run, differing
 * only in what it follows and what has to be in the hand. The tree lopper is tried first and a
 * swing that fells anything never becomes a mining swing, so a block an operator has put on both
 * lists is felled rather than run through twice.
 *
 * <p>Everything a run touches is within a few blocks of the swing, so nothing here reaches into
 * another region. The blocks come away through the server's own {@code breakNaturally}, which is
 * what makes the drops, the fortune on the tool and the experience all behave as the game says
 * rather than as this code remembers they used to.
 *
 * <p><b>The tool wears out and the run stops with it.</b> A swing that takes twenty logs costs an
 * axe twenty points unless an operator has said otherwise, and an axe that breaks partway leaves
 * the rest of the tree standing. Taking a whole tree for one point of wear is a decision an
 * operator makes rather than something that falls out of how this is written.
 */
@NullMarked
public final class LopperListener implements Listener {

    /** How long to wait before replanting, so the game has finished emptying the block. */
    private static final long REPLANT_DELAY = 2;

    private final Configuration configuration;
    private final Lopping lopping;
    private final RegionSchedulers schedulers;

    public LopperListener(
            Configuration configuration, Lopping lopping, RegionSchedulers schedulers) {
        this.configuration = configuration;
        this.lopping = lopping;
        this.schedulers = schedulers;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player who = event.getPlayer();
        if (who.getGameMode() == GameMode.CREATIVE) {
            // A creative swing already takes the block it hit, and taking the tree with it would
            // make building anything out of logs impossible.
            return;
        }

        Settings settings = configuration.settings();
        String world = event.getBlock().getWorld().getName();

        if (settings.runsMechanicIn(TreeLoppers.NAME, world)
                && who.hasPermission(TreeLoppers.USE)
                && lopping.fellsTrees(who)
                && fell(event.getBlock(), who, settings.mechanics().tree())) {
            return;
        }

        if (settings.runsMechanicIn(VeinMiners.NAME, world)
                && who.hasPermission(VeinMiners.USE)
                && lopping.minesSeams(who)) {
            run(event.getBlock(), who, settings.mechanics().vein(), Set.of(), null);
        }
    }

    /** Fells a tree, replanting it where that has been asked for and allowed. */
    private boolean fell(Block broken, Player who, TreeSettings trees) {
        Key sapling = trees.placeSaplings() && who.hasPermission(TreeLoppers.SAPLING)
                ? TreeLoppers.saplingFor(nameOf(broken)).orElse(null)
                : null;
        return run(broken, who, trees.lopper(), trees.alsoTaken(), sapling);
    }

    /**
     * Takes everything one swing reaches.
     *
     * @param sapling what to replant where a trunk stood, or nothing to replant nothing
     * @return true if anything beyond the block that was struck came away
     */
    private boolean run(
            Block broken,
            Player who,
            LopperSettings settings,
            Set<Key> alsoTake,
            @Nullable Key sapling) {
        ItemStack tool = who.getInventory().getItemInMainHand();
        LopperRules rules = settings.rules();
        if (!rules.worksWith(tool.getType().getKey())) {
            return false;
        }

        World world = broken.getWorld();
        List<Vec3i> reached =
                Loppers.reach(Positions.toDomain(broken), rules, alsoTake, sightOf(world));
        if (reached.size() <= 1) {
            return false;
        }

        Material held = tool.getType();
        int planted = 0;

        // The first is the block the swing struck, which the server is already dealing with.
        for (Vec3i place : reached.subList(1, reached.size())) {
            if (!settings.singleUse() && who.getInventory().getItemInMainHand().getType() != held) {
                // The tool wore out partway. What is left of the tree stays standing.
                break;
            }

            Block block = world.getBlockAt(place.x(), place.y(), place.z());
            boolean replanting = sapling != null
                    && planted < TreeLoppers.saplingsFor(sapling)
                    && TreeLoppers.isSoil(nameOf(block.getRelative(0, -1, 0)));

            block.breakNaturally(tool);

            if (!settings.singleUse()) {
                who.damageItemStack(EquipmentSlot.HAND, 1);
            }
            if (replanting && plant(world, place, sapling)) {
                planted++;
            }
        }
        return true;
    }

    /**
     * Puts a sapling back a moment later.
     *
     * <p>Not straight away: the block is only being broken now and the game has not finished with
     * it, so a sapling written this tick is written over. A tick or two later the hole is a hole.
     */
    private boolean plant(World world, Vec3i place, Key sapling) {
        Material material = Material.matchMaterial(sapling.asString());
        if (material == null || !material.isBlock()) {
            return false;
        }

        schedulers.at(world, place).runLater(() -> {
            Block block = world.getBlockAt(place.x(), place.y(), place.z());
            if (block.getType().isAir()) {
                block.setType(material);
            }
        }, REPLANT_DELAY);
        return true;
    }

    /** What a block is called. */
    private static Key nameOf(Block block) {
        return block.getType().getKey();
    }

    /** The blocks of a world, as a run may read them. */
    private static LopperSight sightOf(World world) {
        return new LopperSight() {

            @Override
            public Key blockAt(Vec3i position) {
                return world.getBlockAt(position.x(), position.y(), position.z())
                        .getType()
                        .getKey();
            }

            @Override
            public boolean isReadable(Vec3i position) {
                return position.y() >= world.getMinHeight()
                        && position.y() < world.getMaxHeight()
                        && world.isChunkLoaded(position.x() >> 4, position.z() >> 4);
            }
        };
    }
}
