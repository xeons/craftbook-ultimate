package com.xeonproductions.craftbookultimate.core.pipe;

import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What a pipe will and will not carry.
 *
 * <p>Two lists on a sign: line 3 names what is wanted and line 4 what is refused, each a row of
 * item names separated by commas. A blank third line wants everything, which is what a pipe with
 * no sign at all does, and a blank fourth refuses nothing.
 *
 * <p>Names are resolved the same way every other sign in the plugin resolves them, so the
 * pre-flattening spellings a world is full of — {@code 35:14} for red wool — go on working.
 *
 * @param wanted what may pass, or empty to mean anything may
 * @param refused what may not pass, whatever the wanted list says
 */
@NullMarked
public record PipeFilter(Set<Key> wanted, Set<Key> refused) {

    /** The line naming what is wanted. */
    public static final int WANTED_LINE = 2;

    /** The line naming what is refused. */
    public static final int REFUSED_LINE = 3;

    /** Separates one item from the next. */
    private static final String SEPARATOR = ",";

    /** A filter that carries anything, which is what a pipe with no sign does. */
    public static final PipeFilter ANYTHING = new PipeFilter(Set.of(), Set.of());

    /** Copies the lists so a filter cannot be changed once it is read. */
    public PipeFilter {
        wanted = Set.copyOf(wanted);
        refused = Set.copyOf(refused);
    }

    /**
     * Reads the two lists off a sign.
     *
     * <p>A name nothing resolves to is dropped rather than refusing the sign, so one mistyped item
     * in a list of six leaves the other five working. {@link #problemWith} is what tells the
     * builder about it while they are still standing there.
     *
     * @param items how to work out which item a name means
     */
    public static PipeFilter on(SignLines lines, Function<String, Optional<Key>> items) {
        return new PipeFilter(
                itemsOn(lines.trimmedText(WANTED_LINE), items),
                itemsOn(lines.trimmedText(REFUSED_LINE), items));
    }

    /**
     * What is wrong with the two lists, if anything.
     *
     * <p>Checked as the sign is written, so a builder hears about a name that means nothing while
     * they can still fix it rather than wondering later why their sorter passes everything.
     */
    public static Optional<String> problemWith(SignLines lines, Function<String, Optional<Key>> items) {
        for (int line : new int[] {WANTED_LINE, REFUSED_LINE}) {
            for (String written : namesOn(lines.trimmedText(line))) {
                if (items.apply(written).isEmpty()) {
                    return Optional.of("There is no item called " + written + ".");
                }
            }
        }
        return Optional.empty();
    }

    /** Whether this filter asks anything at all. */
    public boolean isAnything() {
        return wanted.isEmpty() && refused.isEmpty();
    }

    /**
     * Whether an item may pass.
     *
     * <p>Refusing wins over wanting, so an item named on both lines does not pass. That is the
     * useful way round: the second list is how a builder carves an exception out of the first.
     */
    public boolean carries(Key item) {
        if (refused.contains(item)) {
            return false;
        }
        return wanted.isEmpty() || wanted.contains(item);
    }

    /** The items a line names, dropping the ones that mean nothing. */
    private static Set<Key> itemsOn(String written, Function<String, Optional<Key>> items) {
        Set<Key> found = new LinkedHashSet<>();
        for (String name : namesOn(written)) {
            items.apply(name).ifPresent(found::add);
        }
        return found;
    }

    /** The names a line carries, whether or not any of them mean anything. */
    private static List<String> namesOn(String written) {
        List<String> names = new ArrayList<>();
        for (String name : written.split(SEPARATOR)) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }
}
