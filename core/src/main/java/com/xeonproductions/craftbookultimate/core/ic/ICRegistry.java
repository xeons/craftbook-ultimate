// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * The catalogue of every chip the plugin knows how to build.
 *
 * <p>The registry owns the mapping from what a player writes on a sign to the chip they get.
 * That includes retired model numbers kept as aliases, readable shorthands, and the separate
 * numbers a few chips use for their self-triggering variant.
 *
 * <p>Registration is expected to happen once at startup and lookup many times afterwards, so
 * lookups are plain map reads. Instances are not thread safe during registration; once built,
 * they are only read.
 */
@NullMarked
public final class ICRegistry {

    private final Map<String, ICDefinition> byModel = new HashMap<>();
    private final Map<String, ICDefinition> byShorthand = new HashMap<>();
    private final Map<String, ICDefinition> bySelfTriggeringModel = new HashMap<>();

    /**
     * The outcome of resolving a sign's identifier line.
     *
     * @param definition the chip that was named
     * @param selfTriggering whether this particular sign asked for the ticking variant
     */
    public record Resolution(ICDefinition definition, boolean selfTriggering) {}

    /**
     * Adds a chip to the catalogue.
     *
     * @throws IllegalStateException if any of its model numbers or its shorthand is already taken
     */
    public ICRegistry register(ICDefinition definition) {
        for (String model : definition.allModels()) {
            claimModel(model, definition);
        }

        definition.selfTriggeringModel().ifPresent(stModel -> {
            claimModel(stModel, definition);
            bySelfTriggeringModel.put(stModel, definition);
        });

        ICDefinition shorthandClash = byShorthand.putIfAbsent(definition.shorthand(), definition);
        if (shorthandClash != null && shorthandClash != definition) {
            throw new IllegalStateException("Shorthand " + definition.shorthand()
                    + " is claimed by both " + shorthandClash.model() + " and " + definition.model());
        }

        return this;
    }

    /** Adds several chips at once. */
    public ICRegistry registerAll(Collection<ICDefinition> definitions) {
        definitions.forEach(this::register);
        return this;
    }

    /**
     * Resolves a parsed identifier line to the chip it names.
     *
     * @param line the parsed second line of an IC sign
     * @return the chip and whether this sign wants it ticking, or empty if nothing matches
     */
    public Optional<Resolution> resolve(ICLine line) {
        Optional<ICDefinition> found = switch (line.kind()) {
            case MODEL -> Optional.ofNullable(byModel.get(line.identifier()));
            case SHORTHAND -> Optional.ofNullable(byShorthand.get(line.identifier()));
        };

        return found.map(definition -> new Resolution(definition, isSelfTriggering(line, definition)));
    }

    /**
     * Parses and resolves a raw identifier line in one step.
     *
     * @param raw the raw second line of a sign
     */
    public Optional<Resolution> resolve(String raw) {
        return ICLine.parse(raw).flatMap(this::resolve);
    }

    /**
     * Whether a sign should get the ticking variant of the chip it names.
     *
     * <p>Three things can ask for it: the {@code S} suffix on the sign, using the chip's separate
     * self-triggering model number, or the chip itself insisting because it has no input to
     * react to.
     */
    private boolean isSelfTriggering(ICLine line, ICDefinition definition) {
        if (line.selfTriggering()) {
            return true;
        }
        if (line.kind() == ICLine.Kind.MODEL && bySelfTriggeringModel.get(line.identifier()) == definition) {
            return true;
        }
        return definition.newLogic() instanceof SelfTriggeringICLogic logic && logic.alwaysSelfTriggering();
    }

    /** Looks up a chip by any of its model numbers. */
    public Optional<ICDefinition> byModel(String model) {
        return Optional.ofNullable(byModel.get(model.trim().toUpperCase(Locale.ROOT)));
    }

    /** Looks up a chip by its shorthand. */
    public Optional<ICDefinition> byShorthand(String shorthand) {
        return Optional.ofNullable(byShorthand.get(shorthand.trim().toUpperCase(Locale.ROOT)));
    }

    /** Every registered chip, ordered by model number so listings are stable. */
    public List<ICDefinition> definitions() {
        return byModel.values().stream()
                .distinct()
                .sorted(Comparator.comparing(ICDefinition::model))
                .toList();
    }

    /** The number of distinct chips registered. */
    public int size() {
        return (int) byModel.values().stream().distinct().count();
    }

    private void claimModel(String model, ICDefinition definition) {
        ICDefinition existing = byModel.putIfAbsent(model, definition);
        if (existing != null && existing != definition) {
            throw new IllegalStateException("Model number " + model
                    + " is claimed by both " + existing.model() + " and " + definition.model());
        }
    }
}
