// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * A server that knows every block except the ones a test says it does not.
 *
 * <p>That way round because a real server knows thousands, including every block the defaults
 * name — a stub knowing only a handful would fail to round-trip the defaults and would be
 * testing itself rather than the document.
 */
final class StubBlockNames implements BlockNames {

    final Set<String> unknown = new LinkedHashSet<>();

    final Map<String, Set<Key>> tags = new LinkedHashMap<>();

    @Override
    public Optional<Key> block(String written) {
        String name = written.startsWith(Key.MINECRAFT_NAMESPACE + ":")
                ? written.substring(Key.MINECRAFT_NAMESPACE.length() + 1)
                : written;
        return unknown.contains(name) ? Optional.empty() : Optional.of(key(name));
    }

    @Override
    public Set<Key> tagged(String tag) {
        return tags.getOrDefault(tag, Set.of());
    }

    /** A block by its plain name, which is how every test writes one. */
    static Key key(String name) {
        return Key.key(Key.MINECRAFT_NAMESPACE, name);
    }
}
