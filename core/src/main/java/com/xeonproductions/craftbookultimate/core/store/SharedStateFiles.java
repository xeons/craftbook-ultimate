// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.store;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Where the registries chips share are kept between restarts.
 *
 * <p>Four files, one per registry, each a plain list somebody can read and edit. What goes in
 * them is what a player set on purpose and would notice losing: which way each commanded switch is
 * thrown, what each wireless band is carrying, and what each variable holds.
 *
 * <p>Nothing a chip keeps on its own sign is here, because a sign is saved with the world already.
 * Nor is anything a chip works out afresh when it loads: a destination republishes itself, and a
 * cart's rider says again where they are going.
 */
@NullMarked
public final class SharedStateFiles {

    private final StateFile switches;
    private final StateFile guardedSwitches;
    private final StateFile bands;
    private final StateFile variables;

    public SharedStateFiles(Path directory) {
        this.switches = new StateFile(directory, "switches.txt", List.of(
                "# Which way each switch driven by the MCX120 chip is thrown.",
                "# One switch a line: true or false, a space, then the switch's name.",
                "# A switch no chip is following keeps its place here until one is."));
        this.guardedSwitches = new StateFile(directory, "guarded-switches.txt", List.of(
                "# Which way each switch driven by the MCX121 chip is thrown.",
                "# The passwords guarding them are in switch-passwords.txt.",
                "# One switch a line: true or false, a space, then the switch's name."));
        this.variables = new StateFile(directory, "variables.txt", List.of(
                "# The named values the VAR chips and the /var commands share.",
                "# One variable a line: its namespace, its name, then its value.",
                "# The shared namespace is called global. None of the three may contain a space."));
        this.bands = new StateFile(directory, "wireless-bands.txt", List.of(
                "# What each wireless band was last carrying.",
                "# One band a line: true or false, the namespace, then the channel name.",
                "# A dash in place of the namespace means the shared one."));
    }

    /**
     * Reads everything back in.
     *
     * @return how many entries were read across all three
     * @throws IOException if a file exists but cannot be read
     */
    public int load(ChipServices services) throws IOException {
        return services.switchboard().load(switches.read())
                + services.guardedSwitchboard().load(guardedSwitches.read())
                + services.radio().load(bands.read())
                + services.variables().load(variables.read());
    }

    /**
     * Writes everything out.
     *
     * @throws IOException if a file cannot be written
     */
    public void save(ChipServices services) throws IOException {
        switches.write(services.switchboard().save());
        guardedSwitches.write(services.guardedSwitchboard().save());
        bands.write(services.radio().save());
        variables.write(services.variables().save());
    }
}
