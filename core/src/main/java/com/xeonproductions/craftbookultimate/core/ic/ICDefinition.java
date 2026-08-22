// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
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
 * @param requiresAuthorisation whether one is created unarmed and refuses to act until its area
 *     is clear
 * @param playerIdentityLine the sign line, if any, on which writing {@code uuid} is replaced by
 *     the creating player's own unique id
 * @param aliases other model numbers that resolve to this same chip
 * @param selfTriggeringModel a separate model number meaning the self-triggering variant
 * @param inputs what each input pin does, in order, empty where nobody has said
 * @param outputs what each output pin carries, in order, empty where nobody has said
 * @param thirdLine what the sign's third line is for, absent when the chip reads none
 * @param fourthLine what the sign's fourth line is for, absent when the chip reads none
 * @param linesDocumented whether somebody has said what this chip's lines mean, which is how a
 *     chip that reads no lines is told apart from one nobody has got to yet
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
        OptionalInt playerIdentityLine,
        Set<String> aliases,
        Optional<String> selfTriggeringModel,
        List<String> inputs,
        List<String> outputs,
        Optional<LineSpec> thirdLine,
        Optional<LineSpec> fourthLine,
        boolean linesDocumented,
        Supplier<ICLogic> logicFactory) {

    /** Model numbers are letters and digits only, matching what the sign grammar accepts. */
    private static final Pattern MODEL_PATTERN = Pattern.compile("[A-Z0-9]{2,16}");

    /** The permission prefix for chips anyone may build. */
    private static final String SAFE_PERMISSION_PREFIX = "craftbook.ic.safe.";

    /** The permission prefix for chips that need elevated permission. */
    private static final String RESTRICTED_PERMISSION_PREFIX = "craftbook.ic.restricted.";

    public ICDefinition {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        model = normaliseModel(model);
        shorthand = shorthand.trim().toUpperCase(Locale.ROOT);
        if (shorthand.isEmpty()) {
            throw new IllegalArgumentException("IC " + model + " must have a shorthand");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("IC " + model + " must have a name");
        }

        if (playerIdentityLine.isPresent()) {
            int line = playerIdentityLine.getAsInt();
            if (line < 0 || line >= SignLines.LINE_COUNT) {
                throw new IllegalArgumentException(
                        "IC " + model + " names sign line " + line + ", which does not exist");
            }
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

    /** The sign line a chip's third line occupies, counting from zero. */
    public static final int THIRD_LINE = 2;

    /** The sign line a chip's fourth line occupies, counting from zero. */
    public static final int FOURTH_LINE = 3;

    /**
     * What one of the configurable lines is for.
     *
     * @param index the line, which is {@link #THIRD_LINE} or {@link #FOURTH_LINE}
     * @return what it means, or empty if this chip reads nothing there
     */
    public Optional<LineSpec> lineSpec(int index) {
        return switch (index) {
            case THIRD_LINE -> thirdLine;
            case FOURTH_LINE -> fourthLine;
            default -> Optional.empty();
        };
    }

    /**
     * Whether this chip reads more than its first input.
     *
     * <p>Most do not: one input sets the chip off and the other two are wired to nothing. The
     * ones that do read every input do so in their own way — a gate counts them, a latch takes
     * them as set, reset and clock — which is why the ones that matter say so pin by pin instead.
     *
     * <p>Answered from what the chip says about its pins rather than from a list kept beside it,
     * so a chip that gains an input and says so gains the right answer here at the same moment.
     */
    public boolean readsEveryInput() {
        return inputs.size() > 1;
    }

    /**
     * What one input pin does, if anybody has said.
     *
     * @param input the input, counting from zero
     */
    public Optional<String> inputMeaning(int input) {
        return input >= 0 && input < inputs.size()
                ? Optional.of(inputs.get(input))
                : Optional.empty();
    }

    /**
     * What one output pin carries, if anybody has said.
     *
     * @param output the output, counting from zero
     */
    public Optional<String> outputMeaning(int output) {
        return output >= 0 && output < outputs.size()
                ? Optional.of(outputs.get(output))
                : Optional.empty();
    }

    /** Whether this chip reads either of its configurable lines. */
    public boolean readsAnyLine() {
        return thirdLine.isPresent() || fourthLine.isPresent();
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
        private OptionalInt playerIdentityLine = OptionalInt.empty();
        private final Set<String> aliases = new LinkedHashSet<>();
        private Optional<String> selfTriggeringModel = Optional.empty();
        private List<String> inputs = List.of();
        private List<String> outputs = List.of();
        private Optional<LineSpec> thirdLine = Optional.empty();
        private Optional<LineSpec> fourthLine = Optional.empty();
        private boolean linesDocumented;
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
         * Names the line on which a player may write {@code uuid} to mean themselves.
         *
         * <p>Used by the chips whose channel name has a namespace around it. A builder who writes
         * {@code uuid} there gets their own unique id written in its place as the sign is made,
         * which gives them a set of channel names nobody else can transmit into by accident.
         *
         * @param index the line number, from zero to three
         */
        public Builder playerIdentityLine(int index) {
            this.playerIdentityLine = OptionalInt.of(index);
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

        /**
         * Says what each input pin does, in order.
         *
         * <p>Only worth saying for a chip that reads more than the first: everything else is set
         * off by input 1 and leaves the other two wired to nothing, which the page says for
         * itself. Where this is given it must name every input the chip reads, because that is
         * what tells a builder which of three levers matters.
         */
        public Builder inputs(String... meanings) {
            this.inputs = List.of(meanings);
            return this;
        }

        /**
         * Says what each output pin carries, in order.
         *
         * <p>Worth saying for a chip with more than one output, where the difference between them
         * is the whole point and nothing else records it.
         */
        public Builder outputs(String... meanings) {
            this.outputs = List.of(meanings);
            return this;
        }

        /**
         * Says what the sign's third line is for.
         *
         * <p>Use {@link LineSpec#required} where the chip does nothing at all without it, and
         * {@link LineSpec#optional} where it has a default. The first refuses a sign that leaves
         * the line blank; the second tells the builder what they have defaulted to.
         */
        public Builder thirdLine(LineSpec spec) {
            this.thirdLine = Optional.of(spec);
            this.linesDocumented = true;
            return this;
        }

        /** Says what the sign's fourth line is for, as {@link #thirdLine} does for the third. */
        public Builder fourthLine(LineSpec spec) {
            this.fourthLine = Optional.of(spec);
            this.linesDocumented = true;
            return this;
        }

        /**
         * Says that this chip reads neither of its configurable lines.
         *
         * <p>Said outright rather than left to silence, so that a chip nobody has documented is
         * distinguishable from one with nothing to document. A gate needs this; a melody does not.
         */
        public Builder noLines() {
            this.linesDocumented = true;
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
                    playerIdentityLine,
                    aliases,
                    selfTriggeringModel,
                    inputs,
                    outputs,
                    thirdLine,
                    fourthLine,
                    linesDocumented,
                    logicFactory);
        }
    }
}
