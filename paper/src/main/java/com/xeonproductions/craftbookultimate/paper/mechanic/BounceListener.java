// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.mechanic.Bounces;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

/**
 * The block that throws whoever jumps on it.
 *
 * <p>Watches for somebody leaving the ground, looks at what they left, and throws them if it was a
 * bounce block. What the throw is comes from the {@code [Jump]} sign under the block, or from the
 * settings for a block an operator has said needs no sign.
 *
 * <p>Only players, and deliberately. The fork threw any entity that moved, which meant every item
 * dropped on a bounce block and every arrow that landed on one was launched as well.
 */
@NullMarked
public final class BounceListener implements Listener {

    /** How far up the block a jumper must still be for this to be a jump rather than a fall. */
    private static final double NEAR_THE_GROUND = 0.25;

    /** How far a bounce cancels a fall, which is far enough that no landing hurts. */
    private static final float NO_FALL_DAMAGE = -20;

    private final Configuration configuration;

    public BounceListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        if (!configuration.settings()
                .runsMechanicIn(Bounces.NAME, from.getWorld().getName())) {
            return;
        }
        MechanicSettings settings = configuration.settings().mechanics();
        double rise = event.getTo().getY() - from.getY();
        if (rise <= settings.bounce().sensitivity()
                || from.getY() - Math.floor(from.getY()) >= NEAR_THE_GROUND) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(Bounces.USE)) {
            return;
        }

        Block stood = from.getBlock().getRelative(BlockFace.DOWN);
        throwOf(stood, settings).ifPresent(bounce -> {
            Vec3d speed = bounce.forFacing(player.getLocation().getYaw());
            player.setVelocity(new Vector(speed.x(), speed.y(), speed.z()));
            player.setFallDistance(NO_FALL_DAMAGE);
        });
    }

    /**
     * What a block throws somebody, if it throws them at all.
     *
     * <p>A block an operator named throws without a sign; anything else needs a {@code [Jump]}
     * sign on the block beneath it saying how hard. The named blocks are asked about first, so a
     * server that has named a block does not also need a sign under every one of them.
     */
    private static Optional<Bounces.Bounce> throwOf(Block stood, MechanicSettings settings) {
        Key block = stood.getType().getKey();

        String automatic = settings.bounce().automatic().get(block);
        if (automatic != null) {
            return Bounces.parse(automatic);
        }

        if (!settings.bounce().blocks().contains(block)) {
            return Optional.empty();
        }
        if (!(stood.getRelative(BlockFace.DOWN).getState() instanceof Sign sign)) {
            return Optional.empty();
        }

        SignLines lines = Signs.read(sign);
        if (!lines.trimmedText(1).equalsIgnoreCase(Bounces.SIGN_NAME)) {
            return Optional.empty();
        }
        return Bounces.parse(lines.trimmedText(Bounces.VELOCITY_LINE));
    }
}
