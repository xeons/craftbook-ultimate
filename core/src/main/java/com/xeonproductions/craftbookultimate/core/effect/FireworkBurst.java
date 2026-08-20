package com.xeonproductions.craftbookultimate.core.effect;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * What a firework looks like when it goes off.
 *
 * <p>Colours are packed as {@code 0xRRGGBB}, which is how a show script writes them and how the
 * game stores them.
 *
 * @param shape the pattern the sparks make
 * @param colours the colours the sparks start as
 * @param fades the colours they turn before dying, which may be empty
 * @param flicker whether the sparks twinkle
 * @param trail whether the sparks leave a tail
 */
@NullMarked
public record FireworkBurst(
        Shape shape, List<Integer> colours, List<Integer> fades, boolean flicker, boolean trail) {

    /** The default colour used when a script asks for a burst without saying what colour. */
    public static final int WHITE = 0xFFFFFF;

    public FireworkBurst {
        colours = colours.isEmpty() ? List.of(WHITE) : List.copyOf(colours);
        fades = List.copyOf(fades);
    }

    /** A plain burst of one colour. */
    public static FireworkBurst of(Shape shape, int colour) {
        return new FireworkBurst(shape, List.of(colour), List.of(), false, false);
    }

    /** The patterns a firework's sparks can make. */
    public enum Shape {
        BALL,
        BALL_LARGE,
        BURST,
        CREEPER,
        STAR;

        /** Reads a shape as a show script names it. */
        public static Optional<Shape> parse(String written) {
            String cleaned = written.trim().toUpperCase(Locale.ROOT);
            for (Shape shape : values()) {
                if (shape.name().equals(cleaned)) {
                    return Optional.of(shape);
                }
            }
            return Optional.empty();
        }
    }
}
