// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.listener;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLine;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.sign.SignSupport;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import com.xeonproductions.craftbookultimate.sponge.adapter.Signs;
import com.xeonproductions.craftbookultimate.sponge.ic.ChipTitle;
import com.xeonproductions.craftbookultimate.sponge.ic.ICManager;
import com.xeonproductions.craftbookultimate.sponge.mechanic.PlayerActor;
import com.xeonproductions.craftbookultimate.sponge.platform.ServerSchedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.value.ListValue;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.entity.ChangeSignEvent;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * Reviewing a chip's sign as it is written, and noticing when one is destroyed.
 *
 * <p>This is the only place a sign is ever refused. A sign already in the world is read through the
 * manager on chunk load and never comes past here, which is what makes refusing safe: a rule added
 * later cannot invalidate anything already built.
 */
@NullMarked
public final class ICSignListener {

    private static final int IDENTIFIER_LINE = ICManager.IDENTIFIER_LINE;

    private static final int TITLE_LINE = ChipTitle.LINE;

    /** The four sides a wall sign can hang on. */
    private static final List<BlockFace> AROUND =
            List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    private static final String OWN_IDENTITY = "uuid";

    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    private final ICManager manager;
    private final ServerSchedulers schedulers;

    public ICSignListener(ICManager manager, ServerSchedulers schedulers) {
        this.manager = manager;
        this.schedulers = schedulers;
    }

    @Listener(order = Order.LATE)
    public void onSignChange(ChangeSignEvent event) {
        if (!event.isFrontSide()) {
            return;
        }

        Optional<ServerPlayer> writer = event.cause().first(ServerPlayer.class);
        if (writer.isEmpty()) {
            return;
        }

        ServerLocation where = event.sign().serverLocation();
        ServerWorld world = where.world();
        Vec3i position = Positions.toDomain(where);

        // Whether this sign was already a chip decides two things: what the builder is told, and
        // whether rubbing the model number out has to stop something.
        boolean wasChip = manager.at(world, position).isPresent();

        ListValue.Mutable<Component> lines = event.text();
        String identifier = PLAIN.serialize(lines.get(IDENTIFIER_LINE));

        Optional<ICRegistry.Resolution> resolved =
                ICLine.parse(identifier).flatMap(line -> manager.registry().resolve(line));
        if (resolved.isEmpty()) {
            if (wasChip) {
                reconcile(world, position);
            }
            return;
        }

        ICLine parsed = ICLine.parse(identifier).orElseThrow();
        ICDefinition definition = resolved.get().definition();
        ServerPlayer player = writer.get();
        Settings settings = manager.services().configuration().settings();

        if (!settings.allowsWorld(world.key().value())
                || !settings.allowsChip(definition.allModels())) {
            refuse(event, player,
                    "The " + definition.name() + " chip is switched off here.");
            return;
        }

        if (!player.hasPermission(definition.permission())) {
            refuse(event, player,
                    "You do not have permission to create the " + definition.name() + " chip.");
            return;
        }

        if (Signs.facing(world, position).isEmpty()) {
            refuse(event, player,
                    "A chip has to be on a wall sign, so that it has a block behind it to work from.");
            return;
        }

        SignLines written = SignLines.of(lines.get());

        if (refusedForMissingLines(event, definition, written, player)) {
            return;
        }

        // Asked of a throwaway logic instance: the chip itself is not made until the sign has been
        // written, and a chip naming something that is not there should never be made at all.
        Optional<String> problem = definition
                .newLogic()
                .reviewSign(written, manager.services(), new PlayerActor(player));
        if (problem.isPresent()) {
            refuse(event, player, problem.get());
            return;
        }

        writeCanonicalLines(lines, definition, parsed, resolved.get().selfTriggering(), player);

        player.sendMessage(Component.text(
                (wasChip ? "Rebuilt " : "Created ") + definition.name(), NamedTextColor.YELLOW));

        reconcile(world, position);
    }

    private static void refuse(ChangeSignEvent event, ServerPlayer player, String because) {
        player.sendMessage(Component.text(because, NamedTextColor.RED));
        event.setCancelled(true);
    }

    /**
     * Starts the chip again once the sign has actually been written.
     *
     * <p>A tick later, because during the event the block still carries what was there before, and
     * a chip loaded from that would be the old one.
     */
    private void reconcile(ServerWorld world, Vec3i position) {
        schedulers.at(world, position).runLater(() -> manager.reload(world, position), 1);
    }

    /**
     * Notices a chip being destroyed, however it was.
     *
     * <p>Both by its own sign going and by whatever the sign hung on going, because from the
     * builder's side those are the same thing happening.
     */
    @Listener(order = Order.LATE)
    public void onBlocksBroken(ChangeBlockEvent.All event) {
        Optional<ServerPlayer> breaker = event.cause().first(ServerPlayer.class);
        ServerWorld world = event.world();

        for (BlockTransaction transaction : event.transactions(Operations.BREAK.get()).toList()) {
            Optional<ServerLocation> at = transaction.original().location();
            if (at.isEmpty()) {
                continue;
            }

            Vec3i broken = Positions.toDomain(at.get());

            manager.unload(world, broken)
                    .ifPresent(chip -> reportDestroyed(breaker, chip.definition(), false));

            for (Vec3i hanging : chipSignsHangingOn(world, broken)) {
                manager.unload(world, hanging)
                        .ifPresent(chip -> reportDestroyed(breaker, chip.definition(), true));
            }
        }
    }

    private static void reportDestroyed(
            Optional<ServerPlayer> player, ICDefinition chip, boolean throughItsSupport) {
        player.ifPresent(told -> told.sendMessage(Component.text(
                "Destroyed the " + chip.name() + " chip"
                        + (throughItsSupport ? ", by taking away what its sign hung on." : "."),
                NamedTextColor.RED)));
    }

    private List<Vec3i> chipSignsHangingOn(ServerWorld world, Vec3i support) {
        List<Vec3i> found = new ArrayList<>();

        for (BlockFace side : AROUND) {
            Vec3i candidate = support.add(side.deltaX(), side.deltaY(), side.deltaZ());
            Optional<BlockFace> facing = Signs.facing(world, candidate);
            if (facing.isEmpty()) {
                continue;
            }
            if (SignSupport.hangsOn(candidate, facing.get(), support)) {
                found.add(candidate);
            }
        }
        return found;
    }

    private static boolean refusedForMissingLines(
            ChangeSignEvent event, ICDefinition definition, SignLines written, ServerPlayer player) {

        LineReview review = LineReview.of(definition, written);

        if (review.broken()) {
            player.sendMessage(Component.text(
                    "The " + definition.name() + " chip needs more than that.", NamedTextColor.RED));
            review.missing().forEach(blank ->
                    player.sendMessage(Component.text("  " + blank.said(), NamedTextColor.RED)));
            event.setCancelled(true);
            return true;
        }

        review.defaulted().forEach(blank ->
                player.sendMessage(Component.text("  " + blank.said(), NamedTextColor.YELLOW)));
        return false;
    }

    /** Writes the sign back in the spelling the plugin uses, whatever the builder typed. */
    private void writeCanonicalLines(
            ListValue.Mutable<Component> lines,
            ICDefinition definition,
            ICLine written,
            boolean selfTriggering,
            ServerPlayer player) {

        ICLine canonical = definition.canonicalLine(written, selfTriggering);

        lines.set(TITLE_LINE, Component.text(definition.shorthand()));
        lines.set(IDENTIFIER_LINE, Component.text(canonical.render()));

        writePlayerIdentity(lines, definition, player);
    }

    /** Fills in whoever wrote the sign, where the chip asks to be told in so many words. */
    private void writePlayerIdentity(
            ListValue.Mutable<Component> lines, ICDefinition definition, ServerPlayer player) {
        definition.playerIdentityLine().ifPresent(line -> {
            String written = PLAIN.serialize(lines.get(line)).trim();
            if (written.toLowerCase(Locale.ROOT).equals(OWN_IDENTITY)) {
                lines.set(line, Component.text(player.uniqueId().toString()));
            }
        });
    }
}
