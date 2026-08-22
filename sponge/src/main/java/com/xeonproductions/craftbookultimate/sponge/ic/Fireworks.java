// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.effect.FireworkBurst;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.item.FireworkEffect;
import org.spongepowered.api.item.FireworkShape;
import org.spongepowered.api.item.FireworkShapes;
import org.spongepowered.api.util.Color;

/** Turning a described burst into the game's own idea of one. */
@NullMarked
final class Fireworks {

    private Fireworks() {}

    static FireworkEffect toEffect(FireworkBurst burst) {
        FireworkEffect.Builder builder = FireworkEffect.builder()
                .shape(shapeOf(burst))
                .flicker(burst.flicker())
                .trail(burst.trail());

        for (int colour : burst.colours()) {
            builder.color(Color.ofRgb(colour));
        }
        for (int fade : burst.fades()) {
            builder.fade(Color.ofRgb(fade));
        }
        return builder.build();
    }

    /**
     * Which shape a burst asks for.
     *
     * <p>Paired by hand rather than by name, because the two vocabularies disagree on two of the
     * five: what a show script calls a ball is Sponge's small ball, and what it calls
     * {@code BALL_LARGE} is Sponge's {@code LARGE_BALL}. Matching on names would have quietly
     * turned both into whatever the fallback was.
     */
    private static FireworkShape shapeOf(FireworkBurst burst) {
        return switch (burst.shape()) {
            case BALL -> FireworkShapes.SMALL_BALL.get();
            case BALL_LARGE -> FireworkShapes.LARGE_BALL.get();
            case BURST -> FireworkShapes.BURST.get();
            case CREEPER -> FireworkShapes.CREEPER.get();
            case STAR -> FireworkShapes.STAR.get();
        };
    }
}
