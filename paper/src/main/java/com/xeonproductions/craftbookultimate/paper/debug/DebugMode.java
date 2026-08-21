package com.xeonproductions.craftbookultimate.paper.debug;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What the debug stick does when it is used on a chip.
 *
 * <p>A stick carries one of these at a time and shift-right-clicking the air moves to the next.
 * {@link #MENU} is the useful default and the one everything else is reachable from, so a builder
 * who never cycles at all still has the whole tool.
 */
@NullMarked
public enum DebugMode {

    /** Report on the chip and offer everything else as clickable lines. */
    MENU("Menu", "Report on a chip, and offer the rest as buttons"),

    /** Run the chip as though an input had changed, without changing one. */
    TRIGGER("Trigger", "Set a chip off without touching its inputs"),

    /** Show the stretch of world the chip works on. */
    AREA("Area", "Outline the area a chip works on, in particles"),

    /** Show the chip's own internal state. */
    FIELDS("Fields", "Show what a chip is holding internally"),

    /** Stop the chip and start it again. */
    RELOAD("Reload", "Stop a chip and start it again"),

    /** List every chip that is ticking on its own. */
    TICKING("Ticking", "List every chip currently ticking on its own"),

    /** Show what a wireless chip's band is doing. */
    BAND("Band", "Show what a wireless chip's band is carrying");

    /** The order the stick cycles through, which leaves out nothing. */
    public static final List<DebugMode> CYCLE = List.of(values());

    /** What a stick does before anybody has cycled it. */
    public static final DebugMode DEFAULT = MENU;

    private final String title;
    private final String description;

    DebugMode(String title, String description) {
        this.title = title;
        this.description = description;
    }

    /** How the mode is named on the stick. */
    public String title() {
        return title;
    }

    /** One line saying what it does. */
    public String description() {
        return description;
    }

    /** The permission needed to use this mode. */
    public String permission() {
        return "craftbook.debug." + name().toLowerCase(Locale.ROOT);
    }

    /** The mode after this one, wrapping round. */
    public DebugMode next() {
        return CYCLE.get((CYCLE.indexOf(this) + 1) % CYCLE.size());
    }

    /** Reads a mode by name, however it is capitalised. */
    public static Optional<DebugMode> byName(String written) {
        for (DebugMode mode : values()) {
            if (mode.name().equalsIgnoreCase(written.trim())) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
