package com.xeonproductions.craftbookultimate.paper.area;

import com.xeonproductions.craftbookultimate.core.area.AreaAnchor;
import com.xeonproductions.craftbookultimate.core.area.AreaName;
import com.xeonproductions.craftbookultimate.core.area.AreaVault;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;
import org.jspecify.annotations.NullMarked;

/**
 * The saved areas, kept in the game's own structure format.
 *
 * <p>Each area is two files side by side in the plugin's {@code areas} folder: a {@code .nbt}
 * holding the blocks, written by the same code a structure block writes with, and a {@code .anchor}
 * saying which world it came out of and where in it. The structure format records what a building
 * is made of and not where it stood, so the place is kept beside it rather than inside it. An area
 * small enough for one can therefore be opened in a structure block, and a structure somebody has
 * built elsewhere can be dropped in with an anchor written by hand.
 *
 * <p>Blocks only. Entities are deliberately left out: putting an area up spawns whatever it holds
 * and taking it down again clears blocks, so an area carrying item frames would leave a fresh set
 * behind on every toggle.
 *
 * <p>Reading and writing the files happens on whichever thread asked. Everything that touches the
 * world is handed to the region that owns the area, which on a regionised server need not be the
 * one that owns the sign: an area's place is absolute and a sign may name one anywhere.
 */
@NullMarked
public final class StructureVault implements AreaVault {

    /** The folder inside the plugin's own that holds every namespace. */
    public static final String AREAS = "areas";

    /** What the blocks are kept in, which is the game's own structure format. */
    private static final String STRUCTURE = ".nbt";

    /** What the place is kept in. */
    private static final String ANCHOR = ".anchor";

    /** What marks a line in an anchor as a note rather than a value. */
    private static final String COMMENT = "#";

    /** The notes written above a new anchor, explaining it to whoever opens it. */
    private static final List<String> ANCHOR_NOTES = List.of(
            "# Where the area beside this file belongs.",
            "# The blocks are in the .nbt next to it, in the game's own structure format.",
            "# Move the area by changing the origin; the size must match the structure.");

    private final Path directory;
    private final Server server;
    private final RegionSchedulers schedulers;
    private final Consumer<String> report;
    private final Random random = new Random();

    /**
     * @param directory the plugin's own folder
     * @param server what holds the worlds and reads the structure files
     * @param schedulers binds work to the region owning the place it touches
     * @param report where to send a complaint about a file that cannot be read or written
     */
    public StructureVault(
            Path directory, Server server, RegionSchedulers schedulers, Consumer<String> report) {
        this.directory = directory;
        this.server = server;
        this.schedulers = schedulers;
        this.report = report;
    }

    /** Where the areas are kept. */
    public Path path() {
        return directory.resolve(AREAS);
    }

    @Override
    public boolean has(AreaName name) {
        return Files.isRegularFile(structureFile(name)) && Files.isRegularFile(anchorFile(name));
    }

    @Override
    public Optional<AreaAnchor> anchorOf(AreaName name) {
        Path file = anchorFile(name);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank() && !line.stripLeading().startsWith(COMMENT))
                    .toList();
            return AreaAnchor.read(lines);
        } catch (IOException e) {
            report.accept("Could not read " + file + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean restore(AreaName name) {
        Optional<Placement> placement = placementOf(name);
        if (placement.isEmpty()) {
            return false;
        }

        Optional<Structure> structure = load(name);
        if (structure.isEmpty()) {
            return false;
        }

        AreaAnchor anchor = placement.get().anchor();
        World world = placement.get().world();
        schedulers.executeAt(world, anchor.origin(), () -> structure.get().place(
                cornerOf(world, anchor.origin()),
                false,
                StructureRotation.NONE,
                Mirror.NONE,
                0,
                1.0f,
                random));
        return true;
    }

    @Override
    public boolean clear(AreaName name) {
        Optional<Placement> placement = placementOf(name);
        if (placement.isEmpty()) {
            return false;
        }

        AreaAnchor anchor = placement.get().anchor();
        World world = placement.get().world();
        schedulers.executeAt(world, anchor.origin(), () -> emptyOut(world, anchor));
        return true;
    }

    @Override
    public boolean capture(AreaName name) {
        Optional<Placement> placement = placementOf(name);
        if (placement.isEmpty()) {
            return false;
        }

        AreaAnchor anchor = placement.get().anchor();
        World world = placement.get().world();
        schedulers.executeAt(world, anchor.origin(), () -> write(name, world, anchor));
        return true;
    }

    /**
     * Saves a region of the world as a new area, replacing whatever was under that name.
     *
     * <p>How an area comes to exist at all: somebody picks out two corners and names them. The
     * anchor is written first, so an area that exists has somewhere to go back to even if writing
     * the blocks then fails.
     *
     * @return true if the area was written down
     */
    public boolean save(AreaName name, AreaAnchor anchor) {
        World world = server.getWorld(anchor.world());
        if (world == null) {
            return false;
        }
        try {
            Files.createDirectories(folderOf(name.namespace()));
            List<String> lines = new ArrayList<>(ANCHOR_NOTES);
            lines.addAll(anchor.save());
            Files.write(anchorFile(name), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            report.accept("Could not write " + anchorFile(name) + ": " + e.getMessage());
            return false;
        }
        return write(name, world, anchor);
    }

    @Override
    public List<String> idsIn(String namespace) {
        // A namespace names a folder, so anything that is not a plain name is not looked up at
        // all rather than being resolved against the plugin's folder.
        if (!AreaName.isUsableNamespace(namespace)) {
            return List.of();
        }
        Path folder = folderOf(namespace);
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(file -> file.endsWith(STRUCTURE))
                    .map(file -> file.substring(0, file.length() - STRUCTURE.length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            report.accept("Could not read " + folder + ": " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean delete(AreaName name) {
        try {
            boolean removed = Files.deleteIfExists(structureFile(name));
            Files.deleteIfExists(anchorFile(name));
            return removed;
        } catch (IOException e) {
            report.accept("Could not delete " + structureFile(name) + ": " + e.getMessage());
            return false;
        }
    }

    /** Copies what stands in an area's place into its structure file. */
    private boolean write(AreaName name, World world, AreaAnchor anchor) {
        Structure structure = server.getStructureManager().createStructure();
        structure.fill(
                cornerOf(world, anchor.origin()),
                new BlockVector(anchor.size().x(), anchor.size().y(), anchor.size().z()),
                false);
        try {
            Files.createDirectories(folderOf(name.namespace()));
            server.getStructureManager().saveStructure(structureFile(name).toFile(), structure);
            return true;
        } catch (IOException e) {
            report.accept("Could not write " + structureFile(name) + ": " + e.getMessage());
            return false;
        }
    }

    /** Leaves air everywhere an area stands. */
    private static void emptyOut(World world, AreaAnchor anchor) {
        Vec3i origin = anchor.origin();
        Vec3i far = anchor.far();
        for (int x = origin.x(); x <= far.x(); x++) {
            for (int y = origin.y(); y <= far.y(); y++) {
                for (int z = origin.z(); z <= far.z(); z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    /** Reads an area's structure file, or nothing if it cannot be read. */
    private Optional<Structure> load(AreaName name) {
        Path file = structureFile(name);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(server.getStructureManager().loadStructure(file.toFile()));
        } catch (IOException | IllegalArgumentException e) {
            report.accept("Could not read " + file + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /** An area's place, together with the world it is in, where both are still there. */
    private Optional<Placement> placementOf(AreaName name) {
        return anchorOf(name).flatMap(anchor -> {
            World world = server.getWorld(anchor.world());
            return world == null ? Optional.empty() : Optional.of(new Placement(world, anchor));
        });
    }

    private static Location cornerOf(World world, Vec3i position) {
        return new Location(world, position.x(), position.y(), position.z());
    }

    private Path folderOf(String namespace) {
        return path().resolve(namespace);
    }

    private Path structureFile(AreaName name) {
        return folderOf(name.namespace()).resolve(name.id() + STRUCTURE);
    }

    private Path anchorFile(AreaName name) {
        return folderOf(name.namespace()).resolve(name.id() + ANCHOR);
    }

    /** An area's place and the world it is in. */
    private record Placement(World world, AreaAnchor anchor) {}
}
