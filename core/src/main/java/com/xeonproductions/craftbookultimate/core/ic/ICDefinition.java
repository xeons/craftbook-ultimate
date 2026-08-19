package com.xeonproductions.craftbookultimate.core.ic;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * One entry in the IC catalogue: what a chip is called, how it is wired, and how to build one.
 *
 * <p>A definition is immutable and carries no state of its own. Each IC in the world gets its
 * own {@link ICLogic} from {@link #newLogic()}, so a chip is free to keep private state without
 * that leaking between signs.
 *
 * @param model the catalogue number written on signs, such as {@code MC1000}
 * @param shorthand the readable alias, such as {@code REPEATER}
 * @param name the display name
 * @param description one line explaining what the chip does
 * @param defaultLayout the pin layout used when the sign does not name one
 * @param restricted whether creating one needs elevated permission
 * @param aliases other model numbers that resolve to this same chip
 * @param selfTriggeringModel a separate model number meaning the self-triggering variant
 * @param logicFactory builds a fresh logic instance for one chip
 */
@NullMarked
public record ICDefinition(
        String model,
        String shorthand,
        String name,
        String description,
        PinLayout defaultLayout,
        boolean restricted,
        boolean requiresAuthorisation,
        Set<String> aliases,
        Optional<String> selfTriggeringModel,
        Supplier<ICLogic> logicFactory) {

    /** Model numbers are letters and digits only, matching what the sign grammar accepts. */
    private static final Pattern MODEL_PATTERN = Pattern.compile("[A-Z0-9]{2,16}");

    /** The permission prefix for chips anyone may build. */
    private static final String SAFE_PERMISSION_PREFIX = "craftbook.ic.safe.";

    /** The permission prefix for chips that need elevated permission. */
    private static final String RESTRICTED_PERMISSION_PREFIX = "craftbook.ic.restricted.";

    public ICDefinition {
        model = normaliseModel(model);
        shorthand = shorthand.trim().toUpperCase(Locale.ROOT);
        if (shorthand.isEmpty()) {
            throw new IllegalArgumentException("IC " + model + " must have a shorthand");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("IC " + model + " must have a name");
        }

        Set<String> normalisedAliases = new LinkedHashSet<>();
        for (String alias : aliases) {
            String normalised = normaliseModel(alias);
            if (normalised.equals(model)) {
                throw new IllegalArgumentException(
                        "IC " + model + " lists its own model number as an alias");
            }
            normalisedAliases.add(normalised);
        }
        aliases = Set.copyOf(normalisedAliases);

        Optional<String> normalisedSelfTriggering = selfTriggeringModel.map(ICDefinition::normaliseModel);
        if (normalisedSelfTriggering.filter(model::equals).isPresent()) {
            throw new IllegalArgumentException(
                    "IC " + model + " uses its own model number for its self-triggering variant");
        }
        selfTriggeringModel = normalisedSelfTriggering;
    }

    /**
     * Starts building a definition.
     *
     * @param model the catalogue number, such as {@code MC1000}
     * @param shorthand the readable alias, such as {@code REPEATER}
     */
    public static Builder builder(String model, String shorthand) {
        return new Builder(model, shorthand);
    }

    /** Builds a logic instance for one chip in the world. */
    public ICLogic newLogic() {
        return logicFactory.get();
    }

    /** The permission a player needs to create this chip. */
    public String permission() {
        return (restricted ? RESTRICTED_PERMISSION_PREFIX : SAFE_PERMISSION_PREFIX)
                + model.toLowerCase(Locale.ROOT);
    }

    /** Whether this chip can tick on its own. */
    public boolean supportsSelfTriggering() {
        return newLogic() instanceof SelfTriggeringICLogic;
    }

    /** The canonical sign text for this chip, such as {@code [MC1000]}. */
    public String modelReference() {
        return "[" + model + "]";
    }

    /**
     * The identifier line a sign should carry once this chip has been created on it.
     *
     * <p>A player may name a chip by shorthand, in lower case, with the flags in either order.
     * The line they end up with always names the chip by its catalogue number, so later reads see
     * one spelling. Their mode string is carried over untouched, and the restricted marker is
     * applied for a chip that needs one, recording that its creation was permission checked.
     *
     * @param written the line as the player typed it
     * @param selfTriggering whether this sign asked for the ticking variant
     */
    public ICLine canonicalLine(ICLine written, boolean selfTriggering) {
        return new ICLine(
                ICLine.Kind.MODEL, model, selfTriggering, requiresAuthorisation, written.mode());
    }

    /** Every model number that resolves to this chip, including its own and any aliases. */
    public Set<String> allModels() {
        Set<String> models = new LinkedHashSet<>();
        models.add(model);
        models.addAll(aliases);
        return Set.copyOf(models);
    }

    private static String normaliseModel(String raw) {
        String normalised = raw.trim().toUpperCase(Locale.ROOT);
        if (!MODEL_PATTERN.matcher(normalised).matches()) {
            throw new IllegalArgumentException(
                    "IC model number must be 2 to 16 letters or digits, got \"" + raw + "\"");
        }
        return normalised;
    }

    /** Assembles an {@link ICDefinition}. */
    public static final class Builder {

        private final String model;
        private final String shorthand;
        private String name = "";
        private String description = "";
        private PinLayout defaultLayout = PinLayout.defaultLayout();
        private boolean restricted;
        private boolean requiresAuthorisation;
        private final Set<String> aliases = new LinkedHashSet<>();
        private Optional<String> selfTriggeringModel = Optional.empty();
        private Supplier<ICLogic> logicFactory =
                () -> {
                    throw new IllegalStateException("No logic supplied");
                };

        private Builder(String model, String shorthand) {
            this.model = model;
            this.shorthand = shorthand;
        }

        /** Sets the display name. */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Sets the one-line description. */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** Sets the pin layout used when the sign does not name one. */
        public Builder layout(PinLayout layout) {
            this.defaultLayout = layout;
            return this;
        }

        /** Marks this chip as needing elevated permission to create. */
        public Builder restricted() {
            this.restricted = true;
            return this;
        }

        /**
         * Marks this chip as needing authorisation before it will act.
         *
         * <p>Used by the chips that build: one is created unauthorised, and refuses to run while
         * the area it would build in still holds the block it places. That stops a chip being
         * dropped over someone's structure and used to take it apart.
         */
        public Builder requiresAuthorisation() {
            this.requiresAuthorisation = true;
            return this;
        }

        /**
         * Adds model numbers that should resolve to this chip.
         *
         * <p>Used when two chips are merged into one implementation, so signs carrying the
         * retired number keep working.
         */
        public Builder aliases(String... models) {
            for (String alias : models) {
                aliases.add(alias);
            }
            return this;
        }

        /**
         * Declares a separate model number that means the self-triggering variant of this chip.
         *
         * <p>A few chips were catalogued twice, once ticking and once not.
         */
        public Builder selfTriggeringModel(String model) {
            this.selfTriggeringModel = Optional.of(model);
            return this;
        }

        /** Sets how a logic instance for one chip is built. */
        public Builder logic(Supplier<ICLogic> factory) {
            this.logicFactory = factory;
            return this;
        }

        public ICDefinition build() {
            return new ICDefinition(
                    model,
                    shorthand,
                    name.isBlank() ? shorthand : name,
                    description,
                    defaultLayout,
                    restricted,
                    requiresAuthorisation,
                    aliases,
                    selfTriggeringModel,
                    logicFactory);
        }
    }
}
