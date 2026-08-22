// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What reading a sign line needs beyond the text on it.
 *
 * <p>Almost nothing, which is the point. A line is read the same way whether it is being checked as
 * somebody writes it or acted on as the chip runs, and the only thing either needs from outside is
 * what a written block or item name means — the one question a server has to answer, since a sign
 * from before the flattening names a block by number.
 *
 * <p>{@link #lenient()} is what that comes to with no server at all: the modern names and the
 * numeric forms that need no lookup table. It is what the tests use, and it is deliberately
 * generous — a check that refused a sign because it could not reach a server would be worse than
 * no check.
 */
@NullMarked
public interface LineContext {

    /** What a written block or item name means, or nothing where no such thing exists. */
    Optional<Key> item(String written);

    /** A context that resolves whatever names need no server. */
    static LineContext lenient() {
        return of(EntitySpec.DEFAULT_ITEMS);
    }

    /** A context that resolves names the way a particular world does. */
    static LineContext of(Function<String, Optional<Key>> items) {
        return items::apply;
    }
}
