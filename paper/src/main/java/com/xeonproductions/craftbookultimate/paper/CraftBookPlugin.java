package com.xeonproductions.craftbookultimate.paper;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import com.xeonproductions.craftbookultimate.paper.listener.ICChunkListener;
import com.xeonproductions.craftbookultimate.paper.listener.ICRedstoneListener;
import com.xeonproductions.craftbookultimate.paper.listener.ICSignListener;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The plugin itself.
 *
 * <p>Holds the pieces that outlive any one mechanic: the chip catalogue assembled during
 * bootstrap, the scheduler factory that binds work to the region owning it, and the manager
 * tracking which chips are currently loaded.
 */
@NullMarked
public final class CraftBookPlugin extends JavaPlugin {

    private final ICRegistry icRegistry;

    private @Nullable RegionSchedulers schedulers;
    private @Nullable ICManager icManager;

    /**
     * @param icRegistry the chip catalogue, built during bootstrap
     */
    public CraftBookPlugin(ICRegistry icRegistry) {
        this.icRegistry = icRegistry;
    }

    @Override
    public void onEnable() {
        RegionSchedulers regionSchedulers = new RegionSchedulers(this);
        ICManager manager = new ICManager(icRegistry, regionSchedulers, ChipServices.create());

        this.schedulers = regionSchedulers;
        this.icManager = manager;

        getServer().getPluginManager().registerEvents(new ICSignListener(manager, regionSchedulers), this);
        getServer().getPluginManager().registerEvents(new ICRedstoneListener(manager), this);
        getServer().getPluginManager().registerEvents(new ICChunkListener(manager), this);

        adoptAlreadyLoadedChunks(manager);

        getComponentLogger().info(Component.text(
                "Enabled with " + icRegistry.size() + " integrated circuits"
                        + (isFolia() ? ", running regionised" : "")));
    }

    @Override
    public void onDisable() {
        if (icManager != null) {
            icManager.unloadAll();
        }
        getComponentLogger().info(Component.text("Disabled"));
    }

    /**
     * Picks up chips in chunks that were already loaded when the plugin enabled.
     *
     * <p>On a reload the chunk load events have long since fired, so without this the circuitry
     * around every online player would stay dead until its chunk cycled.
     */
    private void adoptAlreadyLoadedChunks(ICManager manager) {
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                List<Block> signs = new ArrayList<>();
                for (BlockState state : chunk.getTileEntities(false)) {
                    if (state instanceof Sign) {
                        signs.add(state.getBlock());
                    }
                }
                if (!signs.isEmpty()) {
                    // Each chunk is adopted on the region that owns it, which is a requirement on
                    // a regionised server and harmless on a plain one.
                    schedulers().at(chunk.getBlock(0, 0, 0).getLocation())
                            .runLater(() -> manager.loadAll(signs), 1);
                }
            }
        }
    }

    /** The chip catalogue. */
    public ICRegistry icRegistry() {
        return icRegistry;
    }

    /** The chips currently loaded in the world. */
    public ICManager icManager() {
        if (icManager == null) {
            throw new IllegalStateException("The IC manager is not available until the plugin is enabled");
        }
        return icManager;
    }

    /** Builds schedulers bound to a place in the world. */
    public RegionSchedulers schedulers() {
        if (schedulers == null) {
            throw new IllegalStateException("Schedulers are not available until the plugin is enabled");
        }
        return schedulers;
    }

    /**
     * Whether the server runs regions on separate threads.
     *
     * <p>Everything here works either way. This only reports which server is underneath, for
     * logging and for the few places that can take a cheaper path when there is a single main
     * thread.
     */
    public static boolean isFolia() {
        return FoliaCheck.PRESENT;
    }

    /** Resolves the regionised-server check once, on first use. */
    private static final class FoliaCheck {

        private static final boolean PRESENT = detect();

        private FoliaCheck() {}

        private static boolean detect() {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
