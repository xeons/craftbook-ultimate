// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

/**
 * Reading the chip catalogue.
 *
 * <p>A builder standing in front of a sign wants to know what a model number does, what its pins
 * are called and how it wants its lines filled in. The generated page says the same things, but
 * only to somebody who is not currently in the game.
 */
@NullMarked
public final class CatalogueActions {

    /** The permission to ask anything of the plugin at all. */
    public static final String COMMAND = "craftbook.command";

    /** How many chips a page of the listing holds. */
    private static final int PAGE_SIZE = 8;

    private final ICRegistry registry;

    public CatalogueActions(ICRegistry registry) {
        this.registry = registry;
    }

    /** What the plugin is and how many chips it knows. */
    public boolean summary(Caller caller) {
        caller.heading("CraftBook Ultimate");
        caller.detail(registry.size()
                + " integrated circuits registered. Try /craftbook ic list.");
        return true;
    }

    /** One page of the catalogue. */
    public boolean list(Caller caller, int page) {
        List<ICDefinition> all = registry.definitions();
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        if (page > pages) {
            caller.refuse("There are only " + pages + " pages.");
            return false;
        }

        caller.heading("Integrated circuits, page " + page + " of " + pages);

        int from = (page - 1) * PAGE_SIZE;
        for (ICDefinition definition : all.subList(from, Math.min(from + PAGE_SIZE, all.size()))) {
            caller.send(Component.text("  " + definition.model(), NamedTextColor.AQUA)
                    .append(Component.text("  " + definition.name(), NamedTextColor.GRAY)));
        }
        return true;
    }

    /**
     * Everything about one chip.
     *
     * <p>Looked up by model number first and shorthand second, because both are written on signs
     * and a builder reading one off a sign has no reason to know which they are holding.
     */
    public boolean info(Caller caller, String wanted) {
        Optional<ICDefinition> found = registry.byModel(wanted);
        if (found.isEmpty()) {
            found = registry.byShorthand(wanted);
        }
        if (found.isEmpty()) {
            caller.refuse("No chip is called " + wanted + ".");
            return false;
        }

        ICDefinition definition = found.get();
        caller.heading(definition.model() + "  " + definition.name());
        caller.detail("  " + definition.description());
        line(caller, "Shorthand", "=" + definition.shorthand());
        line(caller, "Pins", definition.defaultLayout().code());
        line(caller, "Permission", definition.permission());

        if (definition.restricted()) {
            line(caller, "Restricted", "needs elevated permission to build");
        }
        if (definition.requiresAuthorisation()) {
            line(caller, "Authorisation", "will not act until its area is clear");
        }
        if (definition.supportsSelfTriggering()) {
            line(caller, "Self triggering", definition.selfTriggeringModel()
                    .map(model -> "add S to the model, or use " + model)
                    .orElse("add S to the model"));
        }
        return true;
    }

    /** The model numbers that carry on from what has been typed so far. */
    public List<String> models(String typed) {
        String prefix = typed.toUpperCase(Locale.ROOT);
        List<String> matching = new ArrayList<>();
        for (ICDefinition definition : registry.definitions()) {
            if (definition.model().startsWith(prefix)) {
                matching.add(definition.model());
            }
        }
        return matching;
    }

    private static void line(Caller caller, String label, String value) {
        caller.send(Component.text("  " + label + ": ", NamedTextColor.DARK_GRAY)
                .append(Component.text(value, NamedTextColor.WHITE)));
    }
}
