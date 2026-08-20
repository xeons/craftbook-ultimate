package com.xeonproductions.craftbookultimate.core.area;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * A vault that exists only in memory and remembers what it was asked to do.
 *
 * <p>Lets a test check that a sign put the right area back, took the right one away and wrote
 * down the right one on the way, with no files and no world involved.
 */
@NullMarked
public final class SimpleAreaVault implements AreaVault {

    private final Map<AreaName, AreaAnchor> saved = new LinkedHashMap<>();
    private final List<AreaName> restored = new ArrayList<>();
    private final List<AreaName> cleared = new ArrayList<>();
    private final List<AreaName> captured = new ArrayList<>();
    private final List<AreaName> deleted = new ArrayList<>();

    private boolean working = true;

    /** Puts an area in the vault, somewhere arbitrary in a world. */
    public SimpleAreaVault with(AreaName name, UUID world) {
        saved.put(name, new AreaAnchor(world, Vec3i.ZERO, new Vec3i(1, 1, 1)));
        return this;
    }

    /** Puts an area in the vault at a place. */
    public SimpleAreaVault with(AreaName name, AreaAnchor anchor) {
        saved.put(name, anchor);
        return this;
    }

    /** Makes every request fail, standing in for a store that cannot be read or written. */
    public SimpleAreaVault broken() {
        this.working = false;
        return this;
    }

    @Override
    public boolean has(AreaName name) {
        return saved.containsKey(name);
    }

    @Override
    public Optional<AreaAnchor> anchorOf(AreaName name) {
        return Optional.ofNullable(saved.get(name));
    }

    @Override
    public boolean restore(AreaName name) {
        if (!working || !has(name)) {
            return false;
        }
        restored.add(name);
        return true;
    }

    @Override
    public boolean clear(AreaName name) {
        if (!working || !has(name)) {
            return false;
        }
        cleared.add(name);
        return true;
    }

    @Override
    public boolean capture(AreaName name) {
        if (!working || !has(name)) {
            return false;
        }
        captured.add(name);
        return true;
    }

    @Override
    public List<String> idsIn(String namespace) {
        return saved.keySet().stream()
                .filter(name -> name.namespace().equalsIgnoreCase(namespace))
                .map(AreaName::id)
                .sorted()
                .toList();
    }

    @Override
    public boolean delete(AreaName name) {
        if (saved.remove(name) == null) {
            return false;
        }
        deleted.add(name);
        return true;
    }

    /** The areas that were put back, in order. */
    public List<AreaName> restored() {
        return List.copyOf(restored);
    }

    /** The areas whose space was emptied, in order. */
    public List<AreaName> cleared() {
        return List.copyOf(cleared);
    }

    /** The areas that were written down as they stood, in order. */
    public List<AreaName> captured() {
        return List.copyOf(captured);
    }

    /** The areas that were forgotten, in order. */
    public List<AreaName> deleted() {
        return List.copyOf(deleted);
    }
}
