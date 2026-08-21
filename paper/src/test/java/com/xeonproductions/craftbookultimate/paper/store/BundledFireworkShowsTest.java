package com.xeonproductions.craftbookultimate.paper.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.effect.FireworkShow;
import com.xeonproductions.craftbookultimate.core.effect.FireworkShows;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The shows that ship with the plugin.
 *
 * <p>A script the parser cannot make sense of is skipped line by line rather than refused, which is
 * right for a file an operator wrote and wrong for one shipped in the jar: a typo in one of these
 * would make a quieter display, not an error, and nobody would find out. So each is read here the
 * way the plugin reads it and checked for having survived.
 */
@DisplayName("The firework shows that ship with the plugin")
class BundledFireworkShowsTest {

    /** How few steps counts as a show that mostly failed to parse. */
    private static final int LEAST_INTERESTING = 12;

    static List<String> bundled() {
        return FireworkFiles.BUNDLED;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bundled")
    void isInTheJarWhereTheUnpackerLooksForIt(String name) throws IOException {
        assertThat(read(name)).isNotEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bundled")
    void isNamedSomethingASignCanAskFor(String name) {
        assertThat(FireworkShows.isUsableName(name.substring(0, name.lastIndexOf('.')))).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bundled")
    void parsesIntoAShowWorthWatching(String name) throws IOException {
        FireworkShow show = FireworkShow.parse(read(name), dialectOf(name));

        assertThat(show.steps()).hasSizeGreaterThan(LEAST_INTERESTING);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bundled")
    void setsOffFireworksRatherThanOnlyWaiting(String name) throws IOException {
        FireworkShow show = FireworkShow.parse(read(name), dialectOf(name));

        assertThat(show.steps()).filteredOn(step -> step instanceof FireworkShow.Step.Launch)
                .hasSizeGreaterThan(5);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bundled")
    void leavesTimeBetweenOneBurstAndTheNext(String name) throws IOException {
        FireworkShow show = FireworkShow.parse(read(name), dialectOf(name));

        int ticks = show.steps().stream()
                .filter(step -> step instanceof FireworkShow.Step.Wait)
                .mapToInt(step -> ((FireworkShow.Step.Wait) step).ticks())
                .sum();

        assertThat(ticks).isGreaterThan(100);
    }

    @Test
    void spellsEveryNamedEffectTheSameWayItIsLaunched() throws IOException {
        // "start gold" files an effect under exactly the text that follows, and "launch gold"
        // finds it by exactly the text that follows. Neither is folded, so a capital in one and
        // not the other loses the rocket silently.
        for (String name : bundled()) {
            if (dialectOf(name) != FireworkShow.Dialect.NAMED) {
                continue;
            }

            List<String> declared = wordsAfter(read(name), "start");
            for (String fired : wordsAfter(read(name), "launch")) {
                assertThat(declared).as("%s launches %s", name, fired).contains(fired);
            }
        }
    }

    /** The argument of every line beginning with a command, as the parser would split it. */
    private static List<String> wordsAfter(List<String> lines, String command) {
        return lines.stream()
                .map(line -> line.indexOf('#') < 0 ? line : line.substring(0, line.indexOf('#')))
                .map(String::trim)
                .filter(line -> line.startsWith(command + " "))
                .map(line -> line.substring(command.length() + 1).trim())
                .toList();
    }

    private static FireworkShow.Dialect dialectOf(String name) {
        return name.endsWith(".fwk") ? FireworkShow.Dialect.NAMED : FireworkShow.Dialect.PLAIN;
    }

    /** A bundled show, read the way the unpacker reads it. */
    private static List<String> read(String name) throws IOException {
        try (InputStream bundled =
                FireworkFiles.class.getResourceAsStream("/" + FireworkFiles.FOLDER_NAME + "/" + name)) {
            if (bundled == null) {
                return List.of();
            }
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(bundled, StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        }
    }
}
