// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICDocs;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.ICCatalogue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Whether the catalogue page in the repository still describes the catalogue.
 *
 * <p>A generated document is only worth having while somebody notices when it stops being true, and
 * nobody notices a stale page by reading it — a wrong model number reads exactly like a right one.
 * So the build notices instead, in the same change that alters what the page describes.
 */
@DisplayName("The catalogue page")
class IcDocsAreCurrentTest {

    /** Where the page lives, from the module this test runs in. */
    private static final Path PAGE = Path.of("..", "docs", "ics.md");

    @Test
    @DisplayName("says what the catalogue actually holds")
    void saysWhatTheCatalogueActuallyHolds() throws IOException {
        ICRegistry registry = ICCatalogue.build();
        String expected = ICDocs.markdown(registry);

        assertThat(PAGE)
                .as("docs/ics.md is missing. Run ./gradlew generateIcDocs")
                .exists();

        String committed = Files.readString(PAGE, StandardCharsets.UTF_8).replace("\r\n", "\n");

        assertThat(committed)
                .as("docs/ics.md no longer matches the catalogue. Run ./gradlew generateIcDocs")
                .isEqualTo(expected.replace("\r\n", "\n"));
    }
}
