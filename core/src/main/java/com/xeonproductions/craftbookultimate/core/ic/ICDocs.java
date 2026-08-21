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
                Line 1   the shorthand, filled in for you
                Line 2   [MC1000]        the model number, in brackets
                Line 3   whatever the chip needs told
                Line 4   whatever else it needs told
                ```

                Line 2 is the whole declaration. Write the model number in brackets and the rest of
                the sign fills itself in: line 1 becomes the chip's shorthand, so you can read at a
                glance what a wall of signs is doing.

                You can write the shorthand in brackets instead of the number — `[REPEATER]` for
                `[MC1000]` — and it resolves to the same chip.

                ### Choosing the wiring

                Put an equals sign and a layout code after the model to change where the chip's
                pins sit: `[MC1000=SISO]`. Every chip has a layout it uses when the sign does not
                say, given in its entry below.

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
                self-triggering form, given as **runs on its own** in their entry.

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

            page.append("| | |\n| --- | --- |\n");
            row(page, "Write on the sign", "`[" + chip.model() + "]` or `[" + chip.shorthand() + "]`");
            row(page, "Wiring", wiringOf(chip.defaultLayout()));
            chip.selfTriggeringModel().ifPresent(model ->
                    row(page, "Runs on its own as", "`[" + model + "]`"));
            if (!chip.aliases().isEmpty()) {
                row(page, "Also answers to", joined(chip.aliases()));
            }
            row(page, "Permission", "`" + chip.permission() + "`");
            if (chip.restricted()) {
                row(page, "Restricted", "Yes — not granted to everybody by default.");
            }
            if (chip.requiresAuthorisation()) {
                row(page, "Needs arming",
                        "Yes — it is created inert and does nothing until its area is clear.");
            }
            chip.playerIdentityLine().ifPresent(line -> row(page, "Names you",
                    "Writing `uuid` on line " + (line + 1) + " is replaced by your own player id."));
            page.append('\n');
        }
    }

    /** One row of a chip's table of facts. */
    private static void row(StringBuilder page, String what, String value) {
        page.append("| **").append(what).append("** | ").append(value).append(" |\n");
    }

    /** How a layout reads to somebody who has to wire it up. */
    private static String wiringOf(PinLayout layout) {
        return "`" + layout.code() + "` — " + count(layout.inputCount(), "input")
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
