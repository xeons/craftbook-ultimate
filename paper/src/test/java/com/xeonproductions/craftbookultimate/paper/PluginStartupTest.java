// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICCatalogue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Starting the plugin up.
 *
 * <p>Everything else in this module tests a piece of the plugin with the pieces around it mocked,
 * which says nothing at all about whether the plugin will start. It did not, for eleven commits:
 * the chair commands were registered before the seats they need were built, so {@code onEnable}
 * threw on every server that tried to load it while every test here passed. See finding 149.
 *
 * <p>One test, and it asserts almost nothing, because what it is for is the throwing. Any field
 * asked for before it is assigned, any listener wanting something not yet built, any command whose
 * dependency moves, fails here and nowhere else.
 */
@DisplayName("Starting the plugin")
class PluginStartupTest {

    @AfterEach
    void stopTheServer() {
        MockBukkit.unmock();
    }

    @Test
    void enablesWithoutThrowing() {
        MockBukkit.mock();

        CraftBookPlugin plugin = MockBukkit.load(CraftBookPlugin.class, ICCatalogue.build());

        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void stopsWithoutThrowing() {
        // onDisable saves the passwords and the shared state, and nothing else exercises it.
        MockBukkit.mock();

        CraftBookPlugin plugin = MockBukkit.load(CraftBookPlugin.class, ICCatalogue.build());
        plugin.getServer().getPluginManager().disablePlugin(plugin);

        assertThat(plugin.isEnabled()).isFalse();
    }
}
