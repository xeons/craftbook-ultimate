// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.harness;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.paper.ICCatalogue;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.plugin.Plugin;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * A server with one world in it, for the tests that cannot be written without blocks.
 *
 * <p>Most of this plugin is pure and is tested in {@code core} with no server at all. What is left
 * over is the part that reads and writes the world — the manager, the listeners — and until this
 * existed none of it was covered: a fault there was found by somebody standing in the game.
 *
 * <p>Deliberately small. It puts a wall sign on a block and hands over a manager, which is the
 * whole of what a chip needs to exist.
 */
public final class ChipWorld implements AutoCloseable {

    private final ServerMock server;
    private final WorldMock world;
    private final Plugin plugin;
    private final ICManager manager;
    private final ChipServices services = ChipServices.create();

    public ChipWorld() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.world = server.addSimpleWorld("test");
        this.manager = new ICManager(
                ICCatalogue.build(), new RegionSchedulers(plugin), services);
    }

    @Override
    public void close() {
        MockBukkit.unmock();
    }

    public ServerMock server() {
        return server;
    }

    public WorldMock world() {
        return world;
    }

    public Plugin plugin() {
        return plugin;
    }

    public ICManager manager() {
        return manager;
    }

    public ChipServices services() {
        return services;
    }

    /** A solid block for a sign to hang on. */
    public Block wallAt(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.STONE);
        return block;
    }

    /**
     * A wall sign carrying the given lines, hung on the block one step behind it.
     *
     * <p>The support is placed too, since a wall sign with nothing to hang on is not a thing the
     * game allows and not a thing worth testing against.
     *
     * @param facing the way the sign's text faces, away from its support
     */
    public Block signAt(int x, int y, int z, BlockFace facing, String... lines) {
        wallAt(x - facing.getModX(), y - facing.getModY(), z - facing.getModZ());

        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.OAK_WALL_SIGN);
        WallSign data = (WallSign) block.getBlockData();
        data.setFacing(facing);
        block.setBlockData(data);

        write(block, lines);
        return block;
    }

    /**
     * Replaces the text on a sign already in the world.
     *
     * <p>The block data is put back afterwards. Forcing a sign state through the mock server
     * resets the block to its default data, which for a wall sign means it stops facing wherever
     * it was put and faces north instead — and a chip reads its whole pin geometry off that
     * facing. Without this every sign in every test would quietly point the same way.
     */
    public void write(Block block, String... lines) {
        BlockData data = block.getBlockData();

        Sign sign = (Sign) block.getState();
        for (int index = 0; index < lines.length; index++) {
            sign.getSide(Side.FRONT).line(index, Component.text(lines[index]));
        }
        sign.update(true);

        block.setBlockData(data);
    }

    /** The block a wall sign hangs on. */
    public Block supportOf(Block sign, BlockFace facing) {
        return sign.getRelative(facing.getOppositeFace());
    }
}
