package com.xeonproductions.craftbookultimate.paper;

import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/**
 * The plugin itself.
 *
 * <p>Holds the pieces that outlive any one mechanic: the chip catalogue, assembled during
 * bootstrap, and the scheduler factory that binds work to the region owning it.
 */
@NullMarked
public final class CraftBookPlugin extends JavaPlugin {

    private final ICRegistry icRegistry;

    private RegionSchedulers schedulers;

    /**
     * @param icRegistry the chip catalogue, built during bootstrap
     */
    public CraftBookPlugin(ICRegistry icRegistry) {
        this.icRegistry = icRegistry;
    }

    @Override
    public void onEnable() {
        this.schedulers = new RegionSchedulers(this);

        getComponentLogger().info(
                net.kyori.adventure.text.Component.text(
                        "Enabled with " + icRegistry.size() + " integrated circuits"
                                + (isFolia() ? ", running regionised" : "")));
    }

    @Override
    public void onDisable() {
        getComponentLogger().info(net.kyori.adventure.text.Component.text("Disabled"));
    }

    /** The chip catalogue. */
    public ICRegistry icRegistry() {
        return icRegistry;
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

    /** The running server, for the few places that need it without an injected reference. */
    static org.bukkit.Server server() {
        return Bukkit.getServer();
    }
}
