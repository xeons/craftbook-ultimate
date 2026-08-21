// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.effect;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A firework display written as a script.
 *
 * <p>A show is a list of things to do in order: launch a firework somewhere, play a sound, wait a
 * while. Everything is placed relative to the sign that runs it, so the same script can be dropped
 * on several builds.
 *
 * <p>Two spellings are understood, because both are already in use. The plain one gives each
 * launch on one line:
 *
 * <pre>
 *   # a red ball two blocks up, then a pause
 *   launch:0,2,0;1;BALL;255,0,0;255,255,255;trail
 *   wait:20
 * </pre>
 *
 * <p>The other builds effects by name first and then fires them, which suits a show that repeats
 * the same burst many times:
 *
 * <pre>
 *   start bigred
 *   set.shape ball_large
 *   set.color 255,0,0
 *   set.trail
 *   build
 *   location 0,2,0
 *   launch bigred
 *   wait 20
 * </pre>
 */
@NullMarked
public record FireworkShow(List<Step> steps) {

    /** A show with nothing in it. */
    public static final FireworkShow EMPTY = new FireworkShow(List.of());

    /** How long a firework's fuse burns for each step of flight power. */
    private static final int TICKS_PER_FLIGHT_POWER = 10;

    /** How many steps of flight power a second of duration asks for. */
    private static final int FLIGHT_POWER_PER_SECOND = 2;

    /** The most steps a script may hold, so a runaway file cannot hang a region. */
    private static final int MAX_STEPS = 4096;

    public FireworkShow {
        steps = List.copyOf(steps);
    }

    /** Whether this show has anything to do. */
    public boolean isEmpty() {
        return steps.isEmpty();
    }

    /** One thing a show does. */
    public sealed interface Step {

        /** Pauses before going on. */
        record Wait(int ticks) implements Step {}

        /**
         * Sets off a firework.
         *
         * @param offset where it launches, relative to the sign running the show
         * @param fuseTicks how long before it goes off
         * @param burst what it looks like when it does
         */
        record Launch(Vec3d offset, int fuseTicks, FireworkBurst burst) implements Step {}

        /**
         * Plays a sound.
         *
         * @param offset where it comes from, relative to the sign running the show
         */
        record Sound(Vec3d offset, Key sound, float volume, float pitch) implements Step {}
    }

    /** Which spelling a script is written in. */
    public enum Dialect {
        /** One launch per line, fields separated by semicolons. */
        PLAIN,
        /** Effects built up by name, then fired. */
        NAMED
    }

    /** Reads a script. Anything it cannot make sense of is skipped rather than failing the show. */
    public static FireworkShow parse(List<String> lines, Dialect dialect) {
        return dialect == Dialect.PLAIN ? parsePlain(lines) : parseNamed(lines);
    }

    private static FireworkShow parsePlain(List<String> lines) {
        List<Step> steps = new ArrayList<>();

        for (String raw : lines) {
            String line = stripComment(raw);
            if (line.isEmpty() || steps.size() >= MAX_STEPS) {
                continue;
            }

            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String command = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String argument = line.substring(colon + 1).trim();

            switch (command) {
                case "wait" -> number(argument).ifPresent(ticks -> steps.add(new Step.Wait((int) (double) ticks)));
                case "launch" -> parsePlainLaunch(argument).ifPresent(steps::add);
                default -> {
                    // An unknown command is a typo in somebody's file, not a reason to stop.
                }
            }
        }

        return new FireworkShow(steps);
    }

    /** Reads {@code x,y,z;duration;shape;r,g,b;r,g,b[;twinkle|trail]}. */
    private static Optional<Step> parsePlainLaunch(String argument) {
        String[] fields = argument.split(";");
        if (fields.length < 5) {
            return Optional.empty();
        }

        Optional<Vec3d> offset = point(fields[0]);
        Optional<Double> duration = number(fields[1]);
        Optional<Integer> colour = colour(fields[3]);
        Optional<Integer> fade = colour(fields[4]);
        if (offset.isEmpty() || duration.isEmpty() || colour.isEmpty() || fade.isEmpty()) {
            return Optional.empty();
        }

        FireworkBurst.Shape shape = FireworkBurst.Shape.parse(fields[2]).orElse(FireworkBurst.Shape.BALL);
        boolean flicker = fields.length > 5 && fields[5].trim().equalsIgnoreCase("twinkle");
        boolean trail = fields.length > 5 && fields[5].trim().equalsIgnoreCase("trail");

        FireworkBurst burst =
                new FireworkBurst(shape, List.of(colour.get()), List.of(fade.get()), flicker, trail);
        return Optional.of(new Step.Launch(offset.get(), fuseFor(duration.get(), false), burst));
    }

    private static FireworkShow parseNamed(List<String> lines) {
        List<Step> steps = new ArrayList<>();
        Map<String, List<FireworkBurst>> effects = new HashMap<>();

        Builder building = null;
        String buildingAs = null;
        Vec3d where = Vec3d.ZERO;
        double duration = 0.5;
        boolean precise = false;

        for (String raw : lines) {
            String line = stripComment(raw);
            if (line.isEmpty() || steps.size() >= MAX_STEPS) {
                continue;
            }

            String command = firstWord(line).toLowerCase(Locale.ROOT);
            String argument = afterFirstWord(line);

            switch (command) {
                case "start" -> {
                    building = new Builder();
                    buildingAs = argument;
                }
                case "build" -> {
                    if (building != null && buildingAs != null) {
                        effects.computeIfAbsent(buildingAs, ignored -> new ArrayList<>())
                                .add(building.build());
                    }
                    building = null;
                    buildingAs = null;
                }
                case "location" -> where = point(argument).orElse(where);
                case "duration" -> {
                    String[] parts = argument.split("\\s+");
                    duration = number(parts[0]).orElse(duration);
                    precise = parts.length > 1 && parts[1].equalsIgnoreCase("precise");
                }
                case "wait" -> {
                    Optional<Double> ticks = number(argument);
                    if (ticks.isPresent()) {
                        steps.add(new Step.Wait((int) (double) ticks.get()));
                    }
                }
                case "sound" -> parseSound(argument).ifPresent(steps::add);
                case "launch" -> {
                    for (FireworkBurst burst : effects.getOrDefault(argument, List.of())) {
                        steps.add(new Step.Launch(where, fuseFor(duration, precise), burst));
                    }
                }
                default -> {
                    if (command.startsWith("set.") && building != null) {
                        building.set(command.substring("set.".length()), argument);
                    }
                }
            }
        }

        return new FireworkShow(steps);
    }

    /** Reads {@code name [x,y,z [volume [pitch]]]}. */
    private static Optional<Step> parseSound(String argument) {
        String[] fields = argument.trim().split("\\s+");
        if (fields.length == 0 || fields[0].isEmpty()) {
            return Optional.empty();
        }

        Key sound;
        try {
            String named = fields[0].toLowerCase(Locale.ROOT);
            sound = named.indexOf(':') >= 0 ? Key.key(named) : Key.key(Key.MINECRAFT_NAMESPACE, named);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        Vec3d offset = fields.length > 1 ? point(fields[1]).orElse(Vec3d.ZERO) : Vec3d.ZERO;
        float volume = fields.length > 2 ? number(fields[2]).orElse(1.0).floatValue() : 1.0f;
        float pitch = fields.length > 3 ? number(fields[3]).orElse(1.0).floatValue() : 1.0f;
        return Optional.of(new Step.Sound(offset, sound, volume, pitch));
    }

    /**
     * How long a firework's fuse burns.
     *
     * <p>A precise duration is in ticks. An ordinary one is in steps of flight power, the same
     * measure a firework star uses, each of which is roughly half a second of climb.
     */
    private static int fuseFor(double duration, boolean precise) {
        int scaled = (int) duration * FLIGHT_POWER_PER_SECOND;
        return precise ? Math.max(1, scaled) : Math.max(1, (scaled + 1) * TICKS_PER_FLIGHT_POWER);
    }

    /** Everything a {@code set.} line can say about an effect. */
    private static final class Builder {

        private FireworkBurst.Shape shape = FireworkBurst.Shape.BALL;
        private final List<Integer> colours = new ArrayList<>();
        private final List<Integer> fades = new ArrayList<>();
        private boolean flicker;
        private boolean trail;

        void set(String property, String argument) {
            switch (property.toLowerCase(Locale.ROOT)) {
                case "shape" -> shape = FireworkBurst.Shape.parse(argument).orElse(shape);
                case "color", "colour" -> colour(argument).ifPresent(colours::add);
                case "fade" -> colour(argument).ifPresent(fades::add);
                case "flicker", "twinkle" -> flicker = true;
                case "trail" -> trail = true;
                default -> {
                    // Unknown properties are somebody's typo, not a reason to lose the effect.
                }
            }
        }

        FireworkBurst build() {
            return new FireworkBurst(shape, List.copyOf(colours), List.copyOf(fades), flicker, trail);
        }
    }

    /** Everything before a {@code #}, trimmed. */
    private static String stripComment(String line) {
        int comment = line.indexOf('#');
        return (comment < 0 ? line : line.substring(0, comment)).trim();
    }

    private static String firstWord(String line) {
        int space = line.indexOf(' ');
        return space < 0 ? line : line.substring(0, space);
    }

    private static String afterFirstWord(String line) {
        int space = line.indexOf(' ');
        return space < 0 ? "" : line.substring(space + 1).trim();
    }

    /** Reads {@code x,y,z}. */
    private static Optional<Vec3d> point(String written) {
        String[] parts = written.trim().split(",");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    new Vec3d(
                            Double.parseDouble(parts[0].trim()),
                            Double.parseDouble(parts[1].trim()),
                            Double.parseDouble(parts[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Reads {@code r,g,b} as a packed colour. */
    private static Optional<Integer> colour(String written) {
        String[] parts = written.trim().split(",");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            int red = Math.clamp(Integer.parseInt(parts[0].trim()), 0, 255);
            int green = Math.clamp(Integer.parseInt(parts[1].trim()), 0, 255);
            int blue = Math.clamp(Integer.parseInt(parts[2].trim()), 0, 255);
            return Optional.of((red << 16) | (green << 8) | blue);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Double> number(String written) {
        try {
            return Optional.of(Double.parseDouble(written.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
