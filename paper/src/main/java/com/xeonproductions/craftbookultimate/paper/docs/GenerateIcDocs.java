// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.docs;

import com.xeonproductions.craftbookultimate.core.ic.ICDocs;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.paper.ICCatalogue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;

/**
 * Writes the catalogue out as a page somebody can read.
 *
 * <p>Run by {@code ./gradlew generateIcDocs} rather than by the plugin, because the answer only
 * changes when the catalogue does. Building it at that moment means the page is reviewed in the
 * same change that alters what it describes, instead of being a thing somebody has to remember to
 * regenerate on a server afterwards.
 *
 * <p>The catalogue itself touches no server API, so this needs no server to run.
 */
@NullMarked
public final class GenerateIcDocs {

    /** Where the page goes when nothing else is asked for. */
    private static final String DEFAULT_OUTPUT = "docs/ics.md";

    private GenerateIcDocs() {}

    /**
     * Writes the page.
     *
     * @param arguments where to write it, or nothing for {@value #DEFAULT_OUTPUT}
     */
    public static void main(String[] arguments) throws IOException {
        Path output = Path.of(arguments.length > 0 ? arguments[0] : DEFAULT_OUTPUT);

        ICRegistry registry = ICCatalogue.build();
        String page = ICDocs.markdown(registry);

        ICDocs.whatIsMissing(registry, page).ifPresent(model -> {
            throw new IllegalStateException(
                    "The catalogue holds " + model + " but the page does not describe it");
        });

        Path folder = output.getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        Files.writeString(output, page, StandardCharsets.UTF_8);

        System.out.println("Wrote " + registry.size() + " chips to " + output.toAbsolutePath());
    }
}
