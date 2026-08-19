package com.xeonproductions.craftbookultimate.paper;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/**
 * Builds the plugin before the server starts.
 *
 * <p>Running here rather than in the plugin itself means the IC catalogue is assembled once,
 * during bootstrap, and handed to the plugin fully formed. Nothing the catalogue needs touches
 * the world, so it can all be decided before there is a world to touch.
 */
@NullMarked
public final class CraftBookBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLogger().info("Preparing CraftBook Ultimate");
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new CraftBookPlugin(ICCatalogue.build());
    }
}
