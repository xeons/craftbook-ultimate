// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.transport;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

/**
 * Which destination currently answers to each name.
 *
 * <p>A transporter names a destination and is sent there; the two are matched by that name alone,
 * so they may be any distance apart and in different worlds.
 *
 * <p>A name holds one destination at a time. The first to claim it keeps it, and a second
 * destination on the same name is inert until the first releases it, which is what stops a
 * transporter's far end changing under it because somebody built a second pad somewhere.
 *
 * <p>Safe to use from any number of regions at once.
 */
@NullMarked
public final class Destinations {

    /** A claimed name, held by one destination. */
    private record Claim(Object owner, Landing landing) {}

    private final Map<String, Claim> claims = new ConcurrentHashMap<>();

    /**
     * Takes a name, if it is free.
     *
     * @param name the name transporters will use
     * @param owner the destination taking it, identified by object identity
     * @param landing where travellers sent to this name should arrive
     * @return true if the name is now held by this owner
     */
    public boolean claim(String name, Object owner, Landing landing) {
        Claim existing = claims.putIfAbsent(name, new Claim(owner, landing));
        if (existing == null) {
            return true;
        }
        if (existing.owner() == owner) {
            claims.put(name, new Claim(owner, landing));
            return true;
        }
        return false;
    }

    /**
     * Moves where a name's travellers arrive, without disturbing who holds it.
     *
     * <p>A destination whose arrival point has been built over works out a new one and publishes
     * it this way.
     *
     * @return true if this owner holds the name and the arrival point was updated
     */
    public boolean update(String name, Object owner, Landing landing) {
        Claim existing = claims.get(name);
        if (existing == null || existing.owner() != owner) {
            return false;
        }
        return claims.replace(name, existing, new Claim(owner, landing));
    }

    /**
     * Gives up a name.
     *
     * <p>Only the destination holding the name can release it, so a second destination built on a
     * name already in use cannot evict the one doing the work by being broken or unloaded.
     *
     * @return true if the name was held by this owner and is now free
     */
    public boolean release(String name, Object owner) {
        Claim existing = claims.get(name);
        if (existing == null || existing.owner() != owner) {
            return false;
        }
        return claims.remove(name, existing);
    }

    /** Whether an owner currently holds a name. */
    public boolean isHeldBy(String name, Object owner) {
        Claim existing = claims.get(name);
        return existing != null && existing.owner() == owner;
    }

    /** Where travellers sent to a name arrive, if anything answers to it. */
    public Optional<Landing> find(String name) {
        return Optional.ofNullable(claims.get(Objects.requireNonNull(name))).map(Claim::landing);
    }

    /** The number of names currently claimed. */
    public int size() {
        return claims.size();
    }

    /** Releases every name. */
    public void clear() {
        claims.clear();
    }
}
