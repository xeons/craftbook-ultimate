// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper;

import com.xeonproductions.craftbookultimate.core.cart.Stations;
import com.xeonproductions.craftbookultimate.core.cart.mechanic.CartMechanics;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanic;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanics;
import com.xeonproductions.craftbookultimate.core.mechanic.ToggleArea;
import com.xeonproductions.craftbookultimate.paper.area.Selections;
import com.xeonproductions.craftbookultimate.paper.area.StructureVault;
import com.xeonproductions.craftbookultimate.paper.cart.CartDispatcher;
import com.xeonproductions.craftbookultimate.paper.cart.CartRecipes;
import com.xeonproductions.craftbookultimate.paper.pipe.PipeDispatcher;
import com.xeonproductions.craftbookultimate.core.pipe.PipeNetworks;
import com.xeonproductions.craftbookultimate.paper.command.AreaCommands;
import com.xeonproductions.craftbookultimate.paper.command.CartCommands;
import com.xeonproductions.craftbookultimate.paper.command.CatalogueCommands;
import com.xeonproductions.craftbookultimate.paper.command.CheckCommands;
import com.xeonproductions.craftbookultimate.paper.command.DebugCommands;
import com.xeonproductions.craftbookultimate.paper.debug.AreaOutline;
import com.xeonproductions.craftbookultimate.paper.debug.DebugActions;
import com.xeonproductions.craftbookultimate.paper.debug.DebugMode;
import com.xeonproductions.craftbookultimate.paper.debug.DebugStick;
import com.xeonproductions.craftbookultimate.paper.command.ConfigCommands;
import com.xeonproductions.craftbookultimate.paper.command.CraftBookCommands;
import com.xeonproductions.craftbookultimate.paper.command.MusicCommands;
import com.xeonproductions.craftbookultimate.paper.command.SwitchCommands;
import com.xeonproductions.craftbookultimate.paper.command.TestbedCommands;
import com.xeonproductions.craftbookultimate.paper.command.VariableCommands;
import com.xeonproductions.craftbookultimate.paper.config.ConfigFile;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitAnnouncer;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitIllusions;
import com.xeonproductions.craftbookultimate.paper.ic.BukkitRoster;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import com.xeonproductions.craftbookultimate.paper.store.FireworkFiles;
import com.xeonproductions.craftbookultimate.paper.store.MidiFiles;
import com.xeonproductions.craftbookultimate.paper.store.PasswordFile;
import com.xeonproductions.craftbookultimate.paper.store.SharedStateFiles;
import com.xeonproductions.craftbookultimate.paper.testbed.TestbedBuilder;
import com.xeonproductions.craftbookultimate.paper.listener.CartListener;
import com.xeonproductions.craftbookultimate.paper.listener.CartRedstoneListener;
import com.xeonproductions.craftbookultimate.paper.listener.CartHabitListener;
import com.xeonproductions.craftbookultimate.paper.listener.PipeListener;
import com.xeonproductions.craftbookultimate.paper.listener.CartSignListener;
import com.xeonproductions.craftbookultimate.paper.listener.ICChunkListener;
import com.xeonproductions.craftbookultimate.paper.listener.ICRedstoneListener;
import com.xeonproductions.craftbookultimate.paper.listener.DebugStickListener;
import com.xeonproductions.craftbookultimate.paper.listener.ICSignListener;
import com.xeonproductions.craftbookultimate.paper.listener.LiftMoveListener;
import com.xeonproductions.craftbookultimate.paper.listener.MechanicInteractListener;
import com.xeonproductions.craftbookultimate.paper.listener.MechanicRedstoneListener;
import com.xeonproductions.craftbookultimate.paper.listener.MechanicSignListener;
import com.xeonproductions.craftbookultimate.paper.listener.SelectionListener;
import com.xeonproductions.craftbookultimate.paper.mechanic.MechanicDispatcher;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private @Nullable MidiFiles midiFiles;
    private @Nullable SharedStateFiles sharedState;
    private @Nullable ConfigFile configFile;
    private @Nullable ChipServices services;
    private @Nullable CartDispatcher cartDispatcher;
    private final PipeNetworks pipeNetworks = new PipeNetworks();
    private @Nullable StructureVault areas;
    private final Selections selections = new Selections();
    private @Nullable DebugStick debugSticks;

    /**
     * @param icRegistry the chip catalogue, built during bootstrap
     */
    public CraftBookPlugin(ICRegistry icRegistry) {
        this.icRegistry = icRegistry;
    }

    @Override
    public void onEnable() {
        RegionSchedulers regionSchedulers = new RegionSchedulers(this);
        ChipServices chipServices = ChipServices.create(
                new BukkitRoster(getServer()),
                new BukkitAnnouncer(getServer(), getLogger()),
                new BukkitIllusions(getServer()));
        ICManager manager = new ICManager(icRegistry, regionSchedulers, chipServices);

        this.schedulers = regionSchedulers;
        this.icManager = manager;
        this.services = chipServices;
        this.passwordFile = new PasswordFile(getDataPath());
        this.fireworkFiles = new FireworkFiles(getDataPath());
        this.midiFiles = new MidiFiles(getDataPath());
        this.sharedState = new SharedStateFiles(getDataPath());
        this.configFile = new ConfigFile(getDataPath(), getServer(), this::reportSetting);

        // Before anything reads a setting, and before any chip is picked up, so a world or a
        // chip the settings exclude is never started only to be stopped again.
        loadSettings();

        CartDispatcher carts = new CartDispatcher(
                chipServices.configuration(),
                new Stations(),
                regionSchedulers,
                CartRecipes.readFrom(getServer()));
        this.cartDispatcher = carts;

        StructureVault areaVault = new StructureVault(
                getDataPath(), getServer(), regionSchedulers, this::reportSetting);
        this.areas = areaVault;
        this.debugSticks = new DebugStick(this);

        registerPermissions();
        loadPasswords();
        loadSharedState();
        loadFireworkShows();
        loadSongs();
        registerCommands(chipServices);

        getServer().getPluginManager().registerEvents(new ICSignListener(manager, regionSchedulers), this);
        getServer().getPluginManager().registerEvents(new ICRedstoneListener(manager), this);
        getServer().getPluginManager().registerEvents(new ICChunkListener(manager), this);
        getServer().getPluginManager().registerEvents(
                new DebugStickListener(manager, debugSticksTarget(), debugActions()), this);
        getServer().getPluginManager().registerEvents(
                new CartListener(carts, chipServices.configuration()), this);
        getServer().getPluginManager().registerEvents(
                new CartRedstoneListener(carts, chipServices.configuration()), this);
        getServer().getPluginManager().registerEvents(
                new CartSignListener(carts, chipServices.configuration()), this);
        getServer().getPluginManager().registerEvents(
                new CartHabitListener(carts, chipServices.configuration(), regionSchedulers), this);
        getServer().getPluginManager().registerEvents(
                new PipeListener(
                        new PipeDispatcher(chipServices.configuration(), pipeNetworks),
                        chipServices.configuration()),
                this);

        MechanicDispatcher mechanics =
                new MechanicDispatcher(chipServices.configuration(), areaVault);
        getServer().getPluginManager().registerEvents(new MechanicInteractListener(mechanics), this);
        getServer().getPluginManager().registerEvents(new MechanicRedstoneListener(mechanics), this);
        getServer().getPluginManager().registerEvents(new LiftMoveListener(mechanics), this);
        getServer().getPluginManager().registerEvents(
                new MechanicSignListener(chipServices.configuration(), mechanics), this);
        getServer().getPluginManager().registerEvents(new SelectionListener(selections), this);

        adoptAlreadyLoadedChunks(manager);

        getComponentLogger().info(Component.text(
                "Enabled with " + icRegistry.size() + " integrated circuits and "
                        + CartMechanics.all().size() + " minecart mechanics, "
                        + SignMechanics.all().size() + " sign mechanics"
                        + (isFolia() ? ", running regionised" : "")));
    }

    @Override
    public void onDisable() {
        if (icManager != null) {
            icManager.unloadAll();
        }
        savePasswords();
        saveSharedState();
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

        for (SignMechanic mechanic : SignMechanics.all()) {
            declare(manager, mechanic.buildPermission(),
                    "Make a " + mechanic.name() + ".", PermissionDefault.TRUE);
            declare(manager, mechanic.usePermission(),
                    "Work a " + mechanic.name() + " somebody has made.", PermissionDefault.TRUE);
        }
        declare(manager, MechanicSignListener.ADMIN_PERMISSION,
                "Make a mechanic that supplies itself rather than drawing on nearby chests.",
                PermissionDefault.OP);
        declareAreaPermissions(manager);
        declare(manager, PipeListener.BUILD,
                "Write a filter on a pipe's sign.", PermissionDefault.TRUE);
        declareVariablePermissions(manager);
        declare(manager, TestbedCommands.BUILD,
                "Build a test bed carrying a rig for every chip.", PermissionDefault.OP);
        declare(manager, CheckCommands.CHECK,
                "Ask which loaded chips cannot work as their signs are written.",
                PermissionDefault.OP);
        declareDebugPermissions(manager);

        for (ICDefinition definition : icRegistry.definitions()) {
            Permission node = new Permission(
                    definition.permission(),
                    "Build the " + definition.name() + " chip.",
                    definition.restricted() ? PermissionDefault.OP : PermissionDefault.TRUE);
            manager.addPermission(node);
            node.addParent(definition.restricted() ? restricted : safe, true);
        }
    }

    /**
     * Declares the permissions the toggled areas need beyond the pair every mechanic has.
     *
     * <p>Saving, deleting and listing your own areas is ordinary; doing any of it under
     * somebody else's name, or under the one everybody shares, is not.
     */
    private static void declareAreaPermissions(PluginManager manager) {
        declare(manager, AreaCommands.SAVE, "Save a region as an area.",
                PermissionDefault.TRUE);
        declare(manager, AreaCommands.DELETE, "Delete one of your own areas.",
                PermissionDefault.TRUE);
        declare(manager, AreaCommands.LIST, "List your own areas.", PermissionDefault.TRUE);
        declare(manager, AreaCommands.SAVE_OTHER, "Save an area under another name.",
                PermissionDefault.OP);
        declare(manager, AreaCommands.DELETE_OTHER, "Delete an area under another name.",
                PermissionDefault.OP);
        declare(manager, AreaCommands.LIST_OTHER, "List the areas under another name.",
                PermissionDefault.OP);
        declare(manager, AreaCommands.BYPASS_LIMIT,
                "Save areas larger, and more of them, than the settings allow.",
                PermissionDefault.OP);
        declare(manager, ToggleArea.SAVE_SIGN_PERMISSION,
                "Make an area sign that writes back over what it puts away.",
                PermissionDefault.OP);
        declare(manager, ToggleArea.GLOBAL_PERMISSION,
                "Make an area sign using the areas everybody shares.", PermissionDefault.OP);
        declare(manager, ToggleArea.OTHER_PERMISSION,
                "Make an area sign using somebody else's areas.", PermissionDefault.OP);
    }

    /**
     * Declares the permissions the variables need.
     *
     * <p>Reading and listing are ordinary; making, changing and removing are not, because a
     * variable is shared and a chip somebody else built may be reading it. Reaching into a
     * namespace that is not your own is separate again, and governs signs as well as commands.
     */
    private static void declareVariablePermissions(PluginManager manager) {
        declare(manager, VariableCommands.GET, "Read a variable.", PermissionDefault.TRUE);
        declare(manager, VariableCommands.LIST, "List the variables.", PermissionDefault.TRUE);
        declare(manager, VariableCommands.DEFINE, "Make a variable.", PermissionDefault.OP);
        declare(manager, VariableCommands.SET, "Change a variable.", PermissionDefault.OP);
        declare(manager, VariableCommands.DELETE, "Remove a variable.", PermissionDefault.OP);
        declare(manager, VariableChips.OTHER_NAMESPACE_PERMISSION,
                "Use a variable belonging to somebody else.", PermissionDefault.OP);
    }

    /** Declares a permission, leaving one the server already knows about alone. */
    private static void declare(
            PluginManager manager, String name, String description, PermissionDefault byDefault) {
        if (manager.getPermission(name) == null) {
            manager.addPermission(new Permission(name, description, byDefault));
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
                this::savePasswords,
                this::saveSharedState);

        new CraftBookCommands(
                        new CatalogueCommands(icRegistry),
                        switchCommands,
                        new ConfigCommands(this::rereadSettings),
                        new CartCommands(cartCommandsTarget()),
                        new MusicCommands(chipServices.songs()),
                        new AreaCommands(areaTarget(), selections, chipServices.configuration()),
                        new VariableCommands(chipServices.variables(), this::saveSharedState),
                        new TestbedCommands(icRegistry, new TestbedBuilder(icManagerTarget(), schedulersTarget())),
                        new CheckCommands(icManagerTarget()),
                        new DebugCommands(
                                icManagerTarget(),
                                debugActions(),
                                debugSticksTarget(),
                                schedulersTarget()))
                .registerOn(this);
    }

    /** The chip manager, which the test bed starts its chips through. */
    private ICManager icManagerTarget() {
        if (icManager == null) {
            throw new IllegalStateException("The chips are not available until enabled");
        }
        return icManager;
    }

    /** The region schedulers, which the test bed builds through. */
    private RegionSchedulers schedulersTarget() {
        if (schedulers == null) {
            throw new IllegalStateException("The schedulers are not available until enabled");
        }
        return schedulers;
    }

    /** The debug sticks, which the listener reads and the command hands out. */
    private DebugStick debugSticksTarget() {
        if (debugSticks == null) {
            throw new IllegalStateException("The debug sticks are not available until enabled");
        }
        return debugSticks;
    }

    /**
     * What the debugging modes do.
     *
     * <p>Built fresh for each caller rather than held, since it keeps nothing of its own and
     * holding one would only be another field to null-check.
     */
    private DebugActions debugActions() {
        return new DebugActions(icManagerTarget(), new AreaOutline(schedulersTarget()));
    }

    /**
     * The permissions the debugging tools need.
     *
     * <p>One to hold a stick at all and one per mode, so that a server can hand a builder the
     * report and the area outline without handing them the ability to set every chip on the map
     * off from a distance.
     */
    private void declareDebugPermissions(PluginManager manager) {
        declare(manager, DebugStick.PERMISSION,
                "Be given an IC debug stick, and use the debugging commands.",
                PermissionDefault.OP);
        for (DebugMode mode : DebugMode.CYCLE) {
            declare(manager, mode.permission(), mode.description() + ".", PermissionDefault.OP);
        }
    }

    /** The saved areas, which the commands fill and empty. */
    private StructureVault areaTarget() {
        if (areas == null) {
            throw new IllegalStateException("The saved areas are not available until enabled");
        }
        return areas;
    }

    /** The cart mechanics, which the commands drive. */
    private CartDispatcher cartCommandsTarget() {
        if (cartDispatcher == null) {
            throw new IllegalStateException("The cart mechanics are not available until enabled");
        }
        return cartDispatcher;
    }

    /**
     * Reads the settings file, putting whatever it says in force.
     *
     * <p>A file that cannot be read leaves whatever was already in force alone, so a typo made
     * while the server is running does not take every chip down with it.
     */
    private boolean loadSettings() {
        if (configFile == null || services == null) {
            return false;
        }
        try {
            services.configuration().replaceWith(configFile.load());
            return true;
        } catch (IOException e) {
            getComponentLogger().error(
                    Component.text("Could not read " + configFile.path()
                            + "; carrying on with the settings already in force"), e);
            return false;
        }
    }

    /**
     * Rereads the settings and starts every chip again under them.
     *
     * <p>Taking the chips down and picking them up again is what makes a change take effect: a
     * chip a setting has just switched off has to stop, and one it has just switched on has to
     * start. The signs themselves are never touched.
     *
     * <p>The firework scripts are reread too, since they are settings in every sense that matters
     * to whoever is writing one.
     *
     * <p>The exchange happens chunk by chunk on the region owning each chunk. Every chip that is
     * loaded is in a chunk that is loaded, so going through the chunks reaches all of them without
     * this thread stopping a chip belonging to another.
     */
    private Component rereadSettings() {
        if (!loadSettings() || icManager == null || services == null) {
            return Component.text("Could not read the settings; nothing has changed.",
                    NamedTextColor.RED);
        }

        // Scripts are small text files, so rereading them costs an operator's command the time to
        // list a folder. That is worth it: writing a display is an edit-and-look-at-it business,
        // and a restart between every attempt makes it a different and much worse one. The songs
        // are deliberately not reread here — a folder of MIDI files is far too slow to convert on
        // the thread this runs on.
        loadFireworkShows();

        adoptAlreadyLoadedChunks(icManager);
        // A narrowed limit shortens a pipe, so what was worked out under the old one is no longer
        // the answer.
        pipeNetworks.forgetEverything();

        Settings settings = services.configuration().settings();
        if (!settings.enabled()) {
            return Component.text("Settings reread. Chips are switched off.", NamedTextColor.YELLOW);
        }
        return Component.text("Settings reread.", NamedTextColor.YELLOW);
    }

    /** Passes a complaint about an entry in the settings file on to the console. */
    private void reportSetting(String complaint) {
        getComponentLogger().warn(Component.text(complaint));
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

    /**
     * Reads back the switches and bands the last run left set.
     *
     * <p>Before any chip is picked up, so a chip claiming a switch finds it where it was rather
     * than with no position at all.
     */
    private void loadSharedState() {
        if (sharedState == null || services == null) {
            return;
        }
        try {
            int read = sharedState.load(services);
            if (read > 0) {
                getComponentLogger().info(Component.text("Read " + read + " switches and bands"));
            }
        } catch (IOException e) {
            getComponentLogger().error(
                    Component.text("Could not read the saved switches and bands; every switch "
                            + "starts unthrown and every band silent"), e);
        }
    }

    /** Writes the switches and bands out. Safe to call from any thread. */
    private void saveSharedState() {
        if (sharedState == null || services == null) {
            return;
        }
        try {
            sharedState.save(services);
        } catch (IOException e) {
            getComponentLogger().error(
                    Component.text("Could not write the switches and bands out; they will start "
                            + "again from wherever the last successful save left them"), e);
        }
    }

    /** Reads the music an operator has left in the plugin's folder. */
    private void loadSongs() {
        if (midiFiles == null || services == null) {
            return;
        }
        try {
            int read = midiFiles.load(services.songs());
            if (read > 0) {
                getComponentLogger().info(Component.text("Read " + read + " songs and playlists"));
            }
        } catch (IOException e) {
            getComponentLogger().error(
                    Component.text("Could not read " + midiFiles.midiPath() + "; no melody will "
                            + "play until it is fixed"), e);
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
     * Starts the chips in every chunk that is already loaded, having stopped whatever was running
     * in it.
     *
     * <p>Used when the plugin enables, where the chunk load events have long since fired and
     * without this the circuitry around every online player would stay dead until its chunk
     * cycled, and again when the settings are reread, where a chip may have to stop or start
     * because of what they now say.
     *
     * <p>A chunk's signs are read and its chips exchanged in one piece of work on the region that
     * owns the chunk, so nothing here reads or writes a block belonging to another thread, and a
     * chip is never running under the old settings while another is running under the new.
     */
    private void adoptAlreadyLoadedChunks(ICManager manager) {
        for (World world : getServer().getWorlds()) {
            UUID id = world.getUID();
            for (Chunk chunk : world.getLoadedChunks()) {
                int chunkX = chunk.getX();
                int chunkZ = chunk.getZ();
                schedulers().at(chunk.getBlock(0, 0, 0).getLocation()).runLater(() -> {
                    if (!chunk.isLoaded()) {
                        return;
                    }
                    manager.unloadChunk(id, chunkX, chunkZ);

                    List<Block> signs = new ArrayList<>();
                    for (BlockState state : chunk.getTileEntities(false)) {
                        if (state instanceof Sign) {
                            signs.add(state.getBlock());
                        }
                    }
                    manager.loadAll(signs);
                }, 1);
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
