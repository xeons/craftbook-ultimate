// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.area;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/** A vault that holds nothing and refuses everything. */
@NullMarked
enum EmptyAreaVault implements AreaVault {
    INSTANCE;

    @Override
    public boolean has(AreaName name) {
        return false;
    }

    @Override
    public Optional<AreaAnchor> anchorOf(AreaName name) {
        return Optional.empty();
    }

    @Override
    public boolean restore(AreaName name) {
        return false;
    }

    @Override
    public boolean clear(AreaName name) {
        return false;
    }

    @Override
    public boolean capture(AreaName name) {
        return false;
    }

    @Override
    public List<String> idsIn(String namespace) {
        return List.of();
    }

    @Override
    public boolean delete(AreaName name) {
        return false;
    }
}
