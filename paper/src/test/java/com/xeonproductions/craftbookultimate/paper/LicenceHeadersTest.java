// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Whether every source file still says what it is licensed under.
 *
 * <p>A file written next year is the one that goes bare, because nothing about writing it prompts
 * anybody to copy two lines off a neighbour. So the build asks instead, and it asks about both
 * modules from one place rather than being duplicated into each.
 */
@DisplayName("Every source file")
class LicenceHeadersTest {

    /** The whole of the licence declaration, as its first line. */
    private static final String IDENTIFIER = "// SPDX-License-Identifier: GPL-3.0-or-later";

    /**
     * How the line naming the holder begins.
     *
     * <p>Only the opening is fixed. The year moves and the holders may one day be several, and
     * neither is something the build should have an opinion about.
     */
    private static final String COPYRIGHT = "// Copyright (C) ";

    /** Both modules' sources, from the module this test runs in. */
    private static final List<Path> TREES =
            List.of(
                    Path.of("..", "core", "src"),
                    Path.of("..", "paper", "src"),
                    Path.of("..", "sponge", "src"));

    @Test
    @DisplayName("names the licence it is under, on its first line")
    void namesTheLicenceItIsUnder() throws IOException {
        List<String> bare = new ArrayList<>();

        for (Path file : sources()) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.size() < 2
                    || !lines.get(0).equals(IDENTIFIER)
                    || !lines.get(1).startsWith(COPYRIGHT)) {
                bare.add(file.toString());
            }
        }

        assertThat(bare)
                .as("These files have no licence header. Add:%n%s%n%s2026 <holder>%n",
                        IDENTIFIER, COPYRIGHT)
                .isEmpty();
    }

    @Test
    @DisplayName("is actually being looked at")
    void isActuallyBeingLookedAt() throws IOException {
        // A check that walks the wrong place passes for the wrong reason and goes on passing
        // forever, which is worse than not having it. So the walk has to find the codebase.
        assertThat(sources()).hasSizeGreaterThan(300);
    }

    /** Every Java file in either module. */
    private static List<Path> sources() throws IOException {
        List<Path> found = new ArrayList<>();
        for (Path tree : TREES) {
            assertThat(tree).as("source tree").isDirectory();
            try (Stream<Path> walk = Files.walk(tree)) {
                walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(found::add);
            }
        }
        return found;
    }
}
