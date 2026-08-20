package com.xeonproductions.craftbookultimate.paper;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.paper.command.CatalogueCommands;
import com.xeonproductions.craftbookultimate.paper.command.CraftBookCommands;
import com.xeonproductions.craftbookultimate.paper.command.SwitchCommands;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import com.xeonproductions.craftbookultimate.paper.store.FireworkFiles;
import com.xeonproductions.craftbookultimate.paper.store.PasswordFile;
import com.xeonproductions.craftbookultimate.paper.listener.ICChunkListener;
import com.xeonproductions.craftbookultimate.paper.listener.ICRedstoneListener;
import com.xeonproductions.craftbookultimate.paper.listener.ICSignListener;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
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
    private @Nullable PasswordFile passwordFile;
    private @Nullable FireworkFiles fireworkFiles;
    private @Nullable ChipServices services;

    /**
     * @param icRegistry the chip catalogue, built during bootstrap
     */
    public CraftBookPlugin(ICRegistry icRegistry) {
        this.icRegistry = icRegistry;
    }

    @Override
    public void onEnable() {
        RegionSchedulers regionSchedulers = new RegionSchedulers(this);
        ChipServices chipServices = ChipServices.create();
        ICManager manager = new ICManager(icRegistry, regionSchedulers, chipServices);

        this.schedulers = regionSchedulers;
        this.icManager = manager;
        this.services = chipServices;
        this.passwordFile = new PasswordFile(getDataPath());
        this.fireworkFiles = new FireworkFiles(getDataPath());

        registerPermissions();
        loadPasswords();
        loadFireworkShows();
        registerCommands(chipServices);

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
        savePasswords();
        getComponentLogger().info(Component.text("Disabled"));
    }

    /**
     * Declares a permission for every chip in the catalogue.
     *
     * <p>Each one hangs off whichever wildcard matches it, so an operator can grant every safe
     * chip or every restricted one in a single node without listing them, and a permission plugin
     * shows the whole set rather than only the ones somebody has already tried to build.
     */
    private void registerPermissions() {
        PluginManager manager = getServer().getPluginManager();
        Permission safe = wildcard(manager, "craftbook.ic.safe.*");
        Permission restricted = wildcard(manager, "craftbook.ic.restricted.*");

        for (ICDefinition definition : icRegistry.definitions()) {
            Permission node = new Permission(
                    definition.permission(),
                    "Build the " + definition.name() + " chip.",
                    definition.restricted() ? PermissionDefault.OP : PermissionDefault.TRUE);
            manager.addPermission(node);
            node.addParent(definition.restricted() ? restricted : safe, true);
        }
    }

    /** The wildcard permission of a name, which the descriptor declares. */
    private static Permission wildcard(PluginManager manager, String name) {
        Permission existing = manager.getPermission(name);
        if (existing != null) {
            return existing;
        }
        Permission created = new Permission(name);
        manager.addPermission(created);
        return created;
    }

    /** Puts the plugin's commands into the server's command tree. */
    private void registerCommands(ChipServices chipServices) {
        SwitchCommands switchCommands = new SwitchCommands(
                chipServices.switchboard(),
                chipServices.guardedSwitchboard(),
                chipServices.passwords(),
                this::runOffThread,
                this::savePasswords);

        new CraftBookCommands(new CatalogueCommands(icRegistry), switchCommands).registerOn(this);
    }

    /**
     * Runs work away from the thread that ticks the world.
     *
     * <p>Checking a password is slow on purpose. Doing it on a region's own thread would let
     * anybody hold that region up by typing the command over and over.
     */
    private void runOffThread(Runnable work) {
        getServer().getAsyncScheduler().runNow(this, task -> work.run());
    }

    /** Reads the switch passwords back in, if any were saved. */
    private void loadPasswords() {
        if (passwordFile == null || services == null) {
            return;
        }
        try {
            int read = passwordFile.load(services.passwords());
            if (read > 0) {
                getComponentLogger().info(Component.text("Read " + read + " switch passwords"));
            }
        } catch (IOException e) {
            getComponentLogger().error(
                    Component.text("Could not read " + passwordFile.path() + "; guarded switches "
                            + "will not open until it is fixed"), e);
        }
    }

    /** Reads the firework display scripts an operator has left in the plugin's folder. */
    private void loadFireworkShows() {
        if (fireworkFiles == null || services == null) {
            return;
        }
        try {
            int read = fireworkFiles.load(services.shows());
            if (read > 0) {
                getComponentLogger().info(Component.text("Read " + read + " firework displays"));
            }
        } catch (IOException e) {
            getComponentLogger().error(
                    Component.text("Could not read " + fireworkFiles.path() + "; no firework "
                            + "display will play until it is fixed"), e);
        }
    }

    /** Writes the switch passwords out. Safe to call from any thread. */
    private void savePasswords() {
        if (passwordFile == null || services == null) {
            return;
        }
        try {
            passwordFile.save(services.passwords());
        } catch (IOException e) {
            getComponentLogger().error(
                    Component.text("Could not write " + passwordFile.path()), e);
        }
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
