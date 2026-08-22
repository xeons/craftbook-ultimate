// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.area.AreaVault;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpiles;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A world that exists only in memory.
 *
 * <p>Everything a mechanic does to the world goes through here, so a bridge can be built, paid
 * for and taken down again in a plain unit test. Blocks not put here read as air, which makes a
 * test say only what the mechanic actually looks at.
 */
@NullMarked
public final class SimpleMechanicWorld implements MechanicWorld {

    /** The height a world has when nobody says otherwise. */
    private static final int DEFAULT_MIN_HEIGHT = -64;

    private static final int DEFAULT_MAX_HEIGHT = 320;

    private final UUID id = UUID.randomUUID();
    private final Map<Vec3i, Key> blocks = new HashMap<>();
    private final Map<Vec3i, PostedSign> signs = new HashMap<>();
    private final Set<Vec3i> unloaded = new HashSet<>();
    private final Map<Vec3i, Boolean> switches = new HashMap<>();
    private final Set<Key> passable = new LinkedHashSet<>(Blocks.AIR);

    private Stockpile stockpile = SimpleStockpile.empty();
    private AreaVault vault = AreaVault.empty();
    private Variables variables = new Variables();
    private int minHeight = DEFAULT_MIN_HEIGHT;
    private int maxHeight = DEFAULT_MAX_HEIGHT;

    public SimpleMechanicWorld() {
        passable.addAll(Blocks.WATER);
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Key blockAt(Vec3i position) {
        if (!isInBounds(position) || !isLoaded(position)) {
            return Blocks.AIR_KEY;
        }
        return blocks.getOrDefault(position, Blocks.AIR_KEY);
    }

    @Override
    public boolean setBlockAt(Vec3i position, Key block) {
        if (!isInBounds(position) || !isLoaded(position)) {
            return false;
        }
        if (Blocks.AIR.contains(block)) {
            blocks.remove(position);
        } else {
            blocks.put(position, block);
        }
        return true;
    }

    @Override
    public Optional<PostedSign> signAt(Vec3i position) {
        return Optional.ofNullable(signs.get(position));
    }

    @Override
    public boolean writeSign(Vec3i position, SignLines lines) {
        PostedSign existing = signs.get(position);
        if (existing == null) {
            return false;
        }
        signs.put(position, new PostedSign(position, lines, existing.facing()));
        return true;
    }

    @Override
    public AreaVault vault() {
        return vault;
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return !unloaded.contains(position);
    }

    @Override
    public boolean isPassable(Vec3i position) {
        return passable.contains(blockAt(position));
    }

    @Override
    public Stockpile stockpileAround(Vec3i position) {
        return stockpile;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public int maxHeight() {
        return maxHeight;
    }

    /** Puts a block in the world. */
    public SimpleMechanicWorld with(Vec3i position, Key block) {
        blocks.put(position, block);
        return this;
    }

    /** Puts a block in the world, named the way a sign names one. */
    public SimpleMechanicWorld with(Vec3i position, String block) {
        return with(position, Blocks.key(block));
    }

    /** Fills a box with one block. */
    public SimpleMechanicWorld filling(Vec3i from, Vec3i to, String block) {
        Key key = Blocks.key(block);
        for (int x = Math.min(from.x(), to.x()); x <= Math.max(from.x(), to.x()); x++) {
            for (int y = Math.min(from.y(), to.y()); y <= Math.max(from.y(), to.y()); y++) {
                for (int z = Math.min(from.z(), to.z()); z <= Math.max(from.z(), to.z()); z++) {
                    blocks.put(new Vec3i(x, y, z), key);
                }
            }
        }
        return this;
    }

    /** Hangs a sign at a position. */
    public SimpleMechanicWorld withSign(Vec3i position, BlockFace facing, String... lines) {
        signs.put(position, new PostedSign(position, SignLines.of(lines), facing));
        return this;
    }

    /** Puts a lever somewhere, switched off. */
    public SimpleMechanicWorld withSwitch(Vec3i position) {
        switches.put(position, false);
        return this;
    }

    /** Whether a lever put here is now on. */
    public boolean isSwitchOn(Vec3i position) {
        return Boolean.TRUE.equals(switches.get(position));
    }

    @Override
    public boolean workSwitchAt(Vec3i position) {
        Boolean thrown = switches.get(position);
        if (thrown == null) {
            return false;
        }
        switches.put(position, !thrown);
        return true;
    }

    /** Makes a position unreadable, standing in for a chunk that is not loaded. */
    public SimpleMechanicWorld unloadedAt(Vec3i position) {
        unloaded.add(position);
        return this;
    }

    /** Says a block is one something could stand in. */
    public SimpleMechanicWorld passing(String block) {
        passable.add(Blocks.key(block));
        return this;
    }

    /** Gives the mechanics somewhere to take materials from and put them back. */
    public SimpleMechanicWorld withStockpile(Stockpile stockpile) {
        this.stockpile = stockpile;
        return this;
    }

    /** Gives the mechanics a bottomless supply, which is what an admin sign has. */
    public SimpleMechanicWorld withUnlimitedStockpile() {
        return withStockpile(Stockpiles.unlimited());
    }

    /** Gives the world the variables a marquee reads. */
    public SimpleMechanicWorld withVariables(Variables variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public Variables variables() {
        return variables;
    }

    /** Gives the world somewhere the saved areas are kept. */
    public SimpleMechanicWorld withVault(AreaVault vault) {
        this.vault = vault;
        return this;
    }

    /** Gives the world a floor and a ceiling. */
    public SimpleMechanicWorld between(int minHeight, int maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        return this;
    }

    /** How many blocks of a kind are in a box. */
    public int count(Vec3i from, Vec3i to, String block) {
        Key key = Blocks.key(block);
        int found = 0;
        for (int x = Math.min(from.x(), to.x()); x <= Math.max(from.x(), to.x()); x++) {
            for (int y = Math.min(from.y(), to.y()); y <= Math.max(from.y(), to.y()); y++) {
                for (int z = Math.min(from.z(), to.z()); z <= Math.max(from.z(), to.z()); z++) {
                    if (blockAt(new Vec3i(x, y, z)).equals(key)) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    /** The stockpile the mechanics are drawing on. */
    public Stockpile stockpile() {
        return stockpile;
    }
}
