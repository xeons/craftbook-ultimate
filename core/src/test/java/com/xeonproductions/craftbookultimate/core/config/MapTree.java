// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A settings file held in memory.
 *
 * <p>Flat, keyed by the dotted path, because that is the whole of what the document addresses a
 * value by. Children are worked out by looking for the paths that start with one.
 */
final class MapTree implements ConfigTree {

    final Map<String, Object> values = new LinkedHashMap<>();
    final Map<String, List<String>> comments = new LinkedHashMap<>();
    List<String> header = List.of();

    @Override
    public boolean has(String path) {
        return values.containsKey(path);
    }

    @Override
    public void set(String path, Object value) {
        values.put(path, value);
    }

    @Override
    public void comment(String path, List<String> lines) {
        comments.put(path, lines);
    }

    @Override
    public void header(List<String> lines) {
        header = lines;
    }

    @Override
    public boolean bool(String path, boolean fallback) {
        return values.get(path) instanceof Boolean value ? value : fallback;
    }

    @Override
    public String text(String path, String fallback) {
        return values.get(path) instanceof String value ? value : fallback;
    }

    @Override
    public int integer(String path, int fallback) {
        return values.get(path) instanceof Number value ? value.intValue() : fallback;
    }

    @Override
    public long count(String path, long fallback) {
        return values.get(path) instanceof Number value ? value.longValue() : fallback;
    }

    @Override
    public double number(String path, double fallback) {
        return values.get(path) instanceof Number value ? value.doubleValue() : fallback;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> strings(String path) {
        return values.get(path) instanceof List<?> value ? (List<String>) value : List.of();
    }

    @Override
    public Set<String> childrenOf(String path) {
        String prefix = path + ".";
        Set<String> names = new LinkedHashSet<>();
        for (String known : values.keySet()) {
            if (!known.startsWith(prefix)) {
                continue;
            }
            String rest = known.substring(prefix.length());
            int nested = rest.indexOf('.');
            names.add(nested < 0 ? rest : rest.substring(0, nested));
        }
        return names;
    }
}
