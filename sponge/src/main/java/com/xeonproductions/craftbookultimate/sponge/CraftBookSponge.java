// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge;

import com.google.inject.Inject;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICCatalogue;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.sponge.adapter.LegacyBlocks;
import com.xeonproductions.craftbookultimate.sponge.adapter.Positions;
import com.xeonproductions.craftbookultimate.sponge.config.ConfigFile;
import com.xeonproductions.craftbookultimate.sponge.game.GameInternals;
import com.xeonproductions.craftbookultimate.sponge.ic.ICManager;
import com.xeonproductions.craftbookultimate.sponge.ic.SpongeAnnouncer;
import com.xeonproductions.craftbookultimate.sponge.ic.SpongeIllusions;
import com.xeonproductions.craftbookultimate.sponge.ic.SpongeRoster;
import com.xeonproductions.craftbookultimate.sponge.listener.ICChunkListener;
import com.xeonproductions.craftbookultimate.sponge.listener.ICRedstoneListener;
import com.xeonproductions.craftbookultimate.sponge.listener.ICSignListener;
import com.xeonproductions.craftbookultimate.sponge.platform.ServerSchedulers;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

/**
 * The plugin, on SpongeVanilla.
 *
 * <p>Only the chips so far. The cart mechanics, the pipes, the sign mechanics, the commands and the
 * debugging tools are bound on Paper and not yet here; what is written down about that is in
 * {@code docs/sponge.md} and {@code TODO.md} rather than left for somebody to discover.
 */
@NullMarked
@Plugin("craftbookultimate")
public final class CraftBookSponge {

    private final PluginContainer plugin;
    private final Logger logger;
    private final Path directory;
    private final ICRegistry registry = ICCatalogue.build();

    private @Nullable ICManager manager;
    private @Nullable ChipServices services;
    private @Nullable ConfigFile configFile;

    @Inject
    public CraftBookSponge(
            PluginContainer plugin, Logger logger, @ConfigDir(sharedRoot = false) Path directory) {
        this.plugin = plugin;
        this.logger = logger;
        this.directory = directory;
    }

    /**
     * Starts once the server is up.
     *
     * <p>Started rather than starting, because the worlds have to be there: a chip is loaded from a
     * sign already in the world, and there is nothing to read before the worlds are open.
     */
    @Listener
    public void onServerStarted(StartedEngineEvent<Server> event) {
        Server server = event.engine();

        ServerSchedulers serverSchedulers = new ServerSchedulers(plugin);
        ChipServices chipServices = ChipServices.create(
                new SpongeRoster(server),
                new SpongeAnnouncer(server, logger),
                new SpongeIllusions(server));
        ICManager icManager = new ICManager(registry, serverSchedulers, chipServices);

        this.services = chipServices;
        this.manager = icManager;
        this.configFile = new ConfigFile(directory, this::reportSetting);

        // Before any chip is picked up, so a world or a chip the settings exclude is never started
        // only to be stopped again.
        loadSettings();

        reportWhatTheGameWillAnswer();

        // The lookup is handed over so that Sponge can reach these listeners without the module
        // having to open itself to it; the overload without one is deprecated for that reason.
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Sponge.eventManager().registerListeners(
                plugin, new ICSignListener(icManager, serverSchedulers), lookup);
        Sponge.eventManager().registerListeners(plugin, new ICRedstoneListener(icManager), lookup);
        Sponge.eventManager().registerListeners(plugin, new ICChunkListener(icManager), lookup);

        adoptAlreadyLoadedChunks(server, icManager);
    }

    @Listener
    public void onServerStopping(StoppingEngineEvent<Server> event) {
        if (manager != null) {
            manager.unloadAll();
        }
    }

    /**
     * Reads the settings file, and carries on with the defaults if it cannot be read.
     *
     * <p>A settings file somebody has broken should stop the settings working, not the plugin: a
     * server full of chips that all stop at once is a worse outcome than one running as it did
     * before the file was edited.
     */
    private void loadSettings() {
        if (configFile == null || services == null) {
            return;
        }

        try {
            services.configuration().replaceWith(configFile.load());
        } catch (IOException e) {
            logger.error("Could not read {}, so the defaults are in use: {}",
                    configFile.path(), e.getMessage());
            services.configuration().replaceWith(Settings.DEFAULTS);
        }
    }

    private void reportSetting(String complaint) {
        logger.warn(complaint);
    }

    /**
     * Says once, at start-up, whether the game is answering the questions the API cannot.
     *
     * <p>Worth a line in the log because the difference is visible to builders — old block
     * spellings, what a harvester pays out, whether a weather chip shows anybody anything — and
     * an operator seeing those go wrong should not have to guess why.
     */
    private void reportWhatTheGameWillAnswer() {
        if (GameInternals.get().isAvailable()) {
            return;
        }

        logger.warn("This server's Minecraft is not answering the questions SpongeAPI cannot, so:");
        logger.warn("  block spellings like 35:14 will not resolve");
        logger.warn("  a harvester pays out the block rather than what it would really drop");
        logger.warn("  the weather illusion chips will show nobody anything");
        if (!LegacyBlocks.readsLegacySpellings()) {
            logger.warn("Chips naming blocks the old way will report as unreadable rather than guess.");
        }
    }

    /**
     * Picks up the chips in chunks that were already loaded before this started.
     *
     * <p>The spawn chunks are open before a plugin is asked to start, so their load event has
     * already been and gone by the time the listener exists. Everything after this arrives through
     * the listener in the ordinary way.
     *
     * <p>Unloaded first, because starting twice would leave two of every ticking chip. A chunk that
     * has gone in the meantime is skipped rather than asked anything: an unloaded chunk answers
     * every question by throwing.
     */
    private void adoptAlreadyLoadedChunks(Server server, ICManager icManager) {
        for (ServerWorld world : server.worldManager().worlds()) {
            for (WorldChunk chunk : world.loadedChunks()) {
                if (chunk.isEmpty()) {
                    continue;
                }

                Vector3i position = chunk.chunkPosition();
                icManager.unloadChunk(world.uniqueId(), position.x(), position.z());

                List<BlockEntity> signs = new ArrayList<>();
                for (BlockEntity entity : chunk.blockEntities()) {
                    if (entity instanceof Sign) {
                        signs.add(entity);
                    }
                }

                for (BlockEntity sign : signs) {
                    icManager.load(world, Positions.toDomain(sign.serverLocation()));
                }
            }
        }
    }
}
