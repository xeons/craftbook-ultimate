// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import org.jspecify.annotations.NullMarked;

/**
 * The catalogue, written out for people to read.
 *
 * <p>Generated rather than kept by hand because it is the one document that would otherwise go
 * wrong quietly. A hundred and more chips, each with a number, a shorthand, a shape of wiring and a
 * permission, is more than anybody will keep in step with the code by remembering to; and a
 * builder who is told the wrong number for a chip finds out by writing a sign that does nothing.
 *
 * <p>Everything here comes from the registry itself, so the page cannot say a chip exists that does
 * not, or miss one that does.
 */
@NullMarked
public final class ICDocs {

    /** How wide the description column is allowed to get before the table stops being readable. */
    private static final int DESCRIPTION_LIMIT = 96;

    private ICDocs() {}

    /**
     * The whole catalogue as one page of Markdown.
     *
     * @param registry the chips to write about
     */
    public static String markdown(ICRegistry registry) {
        List<ICDefinition> chips = new ArrayList<>(registry.definitions());
        chips.sort(Comparator.comparing(ICDefinition::model));

        StringBuilder page = new StringBuilder();
        preamble(page, chips);
        summary(page, chips);
        detail(page, chips);
        return page.toString();
    }

    /** What a builder needs to know before any of the numbers mean anything. */
    private static void preamble(StringBuilder page, List<ICDefinition> chips) {
        long restricted = chips.stream().filter(ICDefinition::restricted).count();
        long models = chips.stream()
                .mapToLong(chip -> 1 + chip.aliases().size()
                        + (chip.selfTriggeringModel().isPresent() ? 1 : 0))
                .sum();

        page.append("""
                # Integrated circuits

                An integrated circuit — a chip — is a sign on a wall that does something when
                redstone reaches it. Write a model number on the second line of a sign, put the
                sign on a wall, and the block behind it becomes a component you can wire into.

                > This page is generated from the catalogue itself. Do not edit it by hand: run
                > `./gradlew generateIcDocs` instead.

                ## Writing the sign

                ```
                Line 1   REPEATER        filled in for you
                Line 2   [MC1000]        the model number, in brackets
                Line 3   whatever the chip needs told
                Line 4   whatever else it needs told
                ```

                Where an entry below says a line **takes** something, those are the spellings the
                chip really accepts: they come from the chip's own reader rather than from
                anything written about it. A line the chip cannot read is refused as the sign is
                written, and says what it would have taken.

                Pins are counted from one, the way they read on the page. A chip that names its
                inputs reads all of them; one that says only input 1 is read leaves the other two
                wired to nothing, so a lever on them does nothing whatever.

                Line 2 is the whole declaration. Write the model number in brackets and the rest
                fills itself in: line 1 becomes the chip's shorthand, so you can read at a glance
                what a wall of signs is doing.

                Lines 3 and 4 are what the chip is told, and they mean something different for
                every chip. Each entry below says what they are for, and one reading **nothing**
                is a line that chip does not read at all.

                A line marked **required** is one the chip cannot work without. Leaving it blank is
                refused as you write the sign, rather than leaving you a chip that looks built and
                does nothing. Everything else has a sensible default and the entry says what it is;
                leaving one of those blank is fine and you are told what you have defaulted to.

                ### A chip whose sign says too little

                A chip that was already standing before its lines were written down here cannot be
                refused — it is in the world already. Instead **its first line is written in red**,
                so that a sign doing nothing looks like one from across the room. Fill the missing
                line in and the red comes off again, so a red title always means a chip that is
                broken now.

                Only a required line does that. A blank line the chip has a default for leaves a
                working chip and is not marked.

                An operator can ask for the list rather than walking the map:

                ```
                /craftbook check
                ```

                It names every loaded chip that cannot work and says which line each is short of.
                It writes nothing.

                ### Naming it by its shorthand instead

                You can name a chip by its shorthand rather than its number, and then it goes after
                an **equals sign** rather than in brackets:

                ```
                =REPEATER            the same chip as [MC1000]
                =REPEATER ST         the self-triggering form
                =RE T FLIP           a shorthand may have spaces in it
                ```

                The sign is rewritten as you place it, so one written `=REPEATER` reads back
                `[MC1000]` afterwards. Both spellings reach the same chip; the brackets are not
                interchangeable with the equals sign, and `[REPEATER]` names nothing.

                ### What goes after the brackets

                Anything after the closing bracket modifies the chip. `S` asks for the
                self-triggering form, and the rest is the mode:

                ```
                [MC1000]S            self-triggering
                [MC1000]!            outputs inverted
                [MC1000]S!           both
                ```

                | Character | What it does |
                | --- | --- |
                | `S` | Run on its own rather than waiting for redstone. |
                | `!` | Invert every output the chip writes. |
                | `+` | Say what the chip did to whoever is nearby. |
                | `1` | Go back to off after acting rather than staying on. |
                | `=` | Skip the usual step up or down when acting on the world. |
                | `r` | Run the chip's effect the other way about. |
                | `t` | Read the weather as a thunderstorm rather than as rain. |
                | `-` | Leave anything that is not a player alone. |
                | `p` | Act as a teleport pad. |
                | `P` | Act as a teleport pad that insists on a pressure plate. |

                Most chips read only one or two of these and ignore the rest; `!` is the one the
                plugin itself applies, to whatever the chip writes. A character that means nothing
                is ignored rather than breaking the sign.

                An `*` may appear on a sign you did not write it on. It marks a chip whose creation
                was already checked, and the plugin puts it there.

                ### Moving the pins about

                Six letters after the brackets rename the chip's pins, so a build can be wired from
                a different side: `[MC1000]badcfe` swaps them in pairs. The letters run `a` to `f`
                and stand for the pins in their usual order.

                ### The wiring itself

                Every chip has one arrangement of pins and it is **not** chosen on the sign — each
                entry below says which its is. The codes mean:

                | Code | Inputs | Outputs | Where they sit |
                | --- | --- | --- | --- |
                | `SISO` | 1 | 1 | One in, one out. The plain arrangement. |
                | `3ISO` | 3 | 1 | Three in, one out. What the logic gates use. |
                | `AISO` | 4 | 1 | Four in, around the sign. Any of them sets the chip off. |
                | `UISO` | 4 | 1 | Four in, clustered in front. For a chip in a floor or a ceiling. |
                | `AIZO` | 3 | 0 | Three in, none out. For a chip whose whole effect is on the world. |
                | `SI3O` | 1 | 3 | One in, three out. |
                | `SI5O` | 1 | 5 | One in, five out. |
                | `3I3O` | 3 | 3 | Three in, three out. The adders and subtractors. |
                | `3I5O` | 3 | 5 | Three in, five out. The demultiplexer. |

                ### Chips that run on their own

                Some chips act on their own rather than waiting for redstone — a clock, a sensor
                watching for somebody to walk past. Those have a second model number for the
                self-triggering form, given as **runs on its own** in their entry, and `[MC1420]S`
                asks the same of any chip that can do it.

                """);

        page.append("There are **")
                .append(chips.size())
                .append(" chips**, answering to **")
                .append(models)
                .append(" model numbers**. ")
                .append(restricted)
                .append(" of them are restricted, meaning they are not granted to everybody by ")
                .append("default: those can move blocks, hurt people or reach a long way, so an ")
                .append("operator decides who may build one.\n\n");
    }

    /** Every chip in one table, which is what somebody looking for a chip actually reads. */
    private static void summary(StringBuilder page, List<ICDefinition> chips) {
        page.append("## Every chip\n\n");
        page.append("| Model | Shorthand | Name | What it does |\n");
        page.append("| --- | --- | --- | --- |\n");
        for (ICDefinition chip : chips) {
            page.append("| [`").append(chip.model()).append("`](#").append(anchorFor(chip))
                    .append(") | `").append(chip.shorthand())
                    .append("` | ").append(chip.name())
                    .append(chip.restricted() ? " *(restricted)*" : "")
                    .append(" | ").append(shorten(chip.description()))
                    .append(" |\n");
        }
        page.append('\n');
    }

    /** A section per chip, for when the table has told somebody which one they want. */
    private static void detail(StringBuilder page, List<ICDefinition> chips) {
        page.append("## The chips in detail\n\n");
        for (ICDefinition chip : chips) {
            page.append("### ").append(chip.model()).append(" — ").append(chip.name()).append("\n\n");
            page.append(chip.description()).append("\n\n");

            row(page, "Write on the sign", "`[" + chip.model() + "]`, or `=" + chip.shorthand() + "`");
            row(page, "Line 3", lineOf(chip, ICDefinition.THIRD_LINE));
            row(page, "Line 4", lineOf(chip, ICDefinition.FOURTH_LINE));
            row(page, "Wiring", wiringOf(chip.defaultLayout()));
            pins(page, chip);
            chip.selfTriggeringModel().ifPresent(model ->
                    row(page, "Runs on its own as", "`[" + model + "]`"));
            if (!chip.aliases().isEmpty()) {
                row(page, "Also answers to", joined(chip.aliases()));
            }
            row(page, "Permission", "`" + chip.permission() + "`");
            if (chip.restricted()) {
                row(page, "Restricted", "not granted to everybody by default");
            }
            if (chip.requiresAuthorisation()) {
                row(page, "Needs arming",
                        "created inert, and does nothing until its area is clear");
            }
            chip.playerIdentityLine().ifPresent(line -> row(page, "Names you",
                    "Writing `uuid` on line " + (line + 1) + " is replaced by your own player id."));
            page.append('\n');
        }
    }

    /**
     * What each pin of a chip does.
     *
     * <p>Only the chips that have something to say say it. Everything else is set off by its first
     * input and answers on its first output, which the wiring line already implies and which would
     * be one more identical sentence on eighty pages. What is said instead is the one thing a
     * builder cannot see from the layout: that the other two inputs are wired to nothing.
     */
    private static void pins(StringBuilder page, ICDefinition chip) {
        PinLayout layout = chip.defaultLayout();

        if (chip.readsEveryInput()) {
            for (int input = 0; input < layout.inputCount(); input++) {
                int shown = input + 1;
                chip.inputMeaning(input).ifPresent(meaning ->
                        row(page, "Input " + shown, meaning));
            }
        } else if (layout.inputCount() > 1) {
            row(page, "Inputs", "only input 1 is read; the others are wired to nothing");
        }

        // Said whatever the inputs turned out to be. A chip can be set off by one lever and still
        // answer on several pins that mean different things, and what those carry is exactly what
        // a builder cannot work out from the layout.
        for (int output = 0; output < layout.outputCount(); output++) {
            int shown = output + 1;
            chip.outputMeaning(output).ifPresent(meaning ->
                    row(page, "Output " + shown, meaning));
        }
    }

    /**
     * One of a chip's facts.
     *
     * <p>A list rather than a table, because five labelled values are a record and not a grid: a
     * table of them needs a header row that can only say "field" and "value", which is an empty
     * band above every chip on the page.
     */
    private static void row(StringBuilder page, String what, String value) {
        page.append("- **").append(what).append("** — ").append(value).append('\n');
    }

    /**
     * How one of a chip's configurable lines reads.
     *
     * <p>A chip that reads nothing there says so rather than being left out, so the entry has the
     * same shape for every chip and a blank line is visibly deliberate.
     */
    private static String lineOf(ICDefinition chip, int index) {
        return chip.lineSpec(index)
                .map(spec -> spec.meaning()
                        + (spec.required() ? " *(required)*" : "")
                        + takes(spec))
                .orElse("nothing");
    }

    /**
     * The spellings a line accepts, printed after what it is for.
     *
     * <p>Taken from the chip's own reader rather than from the sentence describing it, so a
     * spelling printed here is one the chip really accepts. A line that takes any text at all has
     * nothing to print, which is not the same as nothing being known about it — the sentence is
     * there either way.
     */
    private static String takes(LineSpec spec) {
        if (!spec.form().checksAnything()) {
            return "";
        }
        StringBuilder shapes = new StringBuilder();
        for (String shape : spec.accepted()) {
            shapes.append(shapes.isEmpty() ? "" : " ").append('`').append(shape).append('`');
        }
        return "<br>  Takes " + shapes;
    }

    /** How a layout reads to somebody who has to wire it up. */
    private static String wiringOf(PinLayout layout) {
        return "`" + layout.code() + "`, " + count(layout.inputCount(), "input")
                + ", " + count(layout.outputCount(), "output");
    }

    private static String count(int many, String thing) {
        if (many == 0) {
            return "no " + thing + "s";
        }
        return many + " " + thing + (many == 1 ? "" : "s");
    }

    /** A list of model numbers, each in code marks. */
    private static String joined(Iterable<String> models) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String model : models) {
            joiner.add("`" + model + "`");
        }
        return joiner.toString();
    }

    /**
     * The anchor GitHub gives a chip's heading.
     *
     * <p>Worked out the same way it does: lower case, spaces become hyphens, and anything that is
     * not a letter, a digit or a hyphen is dropped.
     */
    private static String anchorFor(ICDefinition chip) {
        String heading = chip.model() + " — " + chip.name();
        StringBuilder anchor = new StringBuilder();
        for (char letter : heading.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(letter)) {
                anchor.append(letter);
            } else if (letter == ' ' || letter == '-') {
                anchor.append('-');
            }
        }
        return anchor.toString();
    }

    /** A description cut to something a table cell can hold, on a word. */
    private static String shorten(String description) {
        String flattened = description.replace('\n', ' ').replace("|", "\\|").trim();
        if (flattened.length() <= DESCRIPTION_LIMIT) {
            return flattened;
        }
        int space = flattened.lastIndexOf(' ', DESCRIPTION_LIMIT);
        return flattened.substring(0, space < 0 ? DESCRIPTION_LIMIT : space) + "…";
    }

    /** Whether a page has been written for every chip the registry holds. */
    public static Optional<String> whatIsMissing(ICRegistry registry, String page) {
        for (ICDefinition chip : registry.definitions()) {
            if (!page.contains("### " + chip.model() + " — ")) {
                return Optional.of(chip.model());
            }
        }
        return Optional.empty();
    }
}
