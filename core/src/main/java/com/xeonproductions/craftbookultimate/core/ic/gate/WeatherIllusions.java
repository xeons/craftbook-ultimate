package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that show people weather the world is not having.
 *
 * <p>Two things they can do and two audiences they can do it to, which is what makes four chips.
 * One pair shows rain that is not falling, the other hides rain that is; one of each pair picks
 * its audience by name and the other by how close they are standing.
 *
 * <p>Nothing here changes the weather. The world goes on raining or not raining, everybody outside
 * the audience sees what is really happening, and the illusion is undone the moment the chip stops
 * being driven. That is what separates these from the weather control chips, which change the
 * weather for everybody and leave it changed.
 *
 * <p>An illusion is only put up when it would show something different. Faking rain while it is
 * already raining shows nobody anything, so the chip stays quiet and its output stays low rather
 * than claiming to have done something.
 *
 * <p>Each chip remembers who it has fooled, so it can put exactly those people back when it is
 * switched off and can leave everybody else alone. A chip that is unloaded puts them back first.
 */
@NullMarked
public final class WeatherIllusions {

    /** The line naming the audience, on the two chips that pick one. */
    private static final int AUDIENCE_LINE = 2;

    /** The line carrying how far the two distance chips reach. */
    private static final int RADIUS_LINE = 2;

    /** The line carrying what to say to somebody as they walk into range. */
    private static final int GREETING_LINE = 3;

    /** How far the distance chips reach when their sign does not say. */
    private static final int DEFAULT_RADIUS = 10;

    /** The shortest distance their sign may ask for. */
    private static final int MIN_RADIUS = 1;

    /** The furthest their sign may ask for. */
    private static final int MAX_RADIUS = 127;

    /** Separates the kind of audience from its name. */
    private static final char AUDIENCE_SEPARATOR = ':';

    /** Written before a name to mean one player. */
    private static final char ONE_PLAYER = 'p';

    /** Written before a name to mean a permission group. */
    private static final char GROUP = 'g';

    private WeatherIllusions() {}

    /**
     * Shows rain to people it is not raining on.
     *
     * <p>Line 3 says who: blank for everybody in this world, {@code p:Name} for one player
     * wherever they are, or {@code g:builders} for everybody in a permission group.
     */
    public static ICLogic falseWeather() {
        return new NamedAudience(Sky.DOWNFALL);
    }

    /**
     * Hides the rain from people it is raining on.
     *
     * <p>The same line 3 as {@link #falseWeather()}.
     */
    public static ICLogic hideWeather() {
        return new NamedAudience(Sky.CLEAR);
    }

    /**
     * Shows rain to everybody standing within a distance of the sign.
     *
     * <p>Line 3 is how far, from one to a hundred and twenty-seven blocks, and defaults to ten.
     * Line 4 is something to say to somebody as they walk into range, said once per visit.
     *
     * <p>Ticking, it keeps up with people walking in and out. Not ticking, it works out who is in
     * range when its inputs change and leaves them fooled until they change again.
     */
    public static SelfTriggeringICLogic distanceFalseWeather() {
        return new NearbyAudience(Sky.DOWNFALL);
    }

    /**
     * Hides the rain from everybody standing within a distance of the sign.
     *
     * <p>The same lines as {@link #distanceFalseWeather()}.
     */
    public static SelfTriggeringICLogic distanceHideWeather() {
        return new NearbyAudience(Sky.CLEAR);
    }

    /**
     * Whether showing a sky would show anybody anything.
     *
     * <p>Rain counts as falling during a thunderstorm as well as during ordinary rain, since both
     * are weather a player can see coming down.
     */
    private static boolean worthShowing(ChipState state, Sky sky) {
        boolean reallyFalling = state.world().isRaining() || state.world().isThundering();
        return sky == Sky.DOWNFALL ? !reallyFalling : reallyFalling;
    }

    /**
     * Who a sign names.
     *
     * @param kind which of the three ways it names them
     * @param name the player or group named, empty for a whole world
     */
    private record Audience(Kind kind, String name) {

        /** The three ways a sign can name an audience. */
        enum Kind {
            /** Everybody in the world the sign is in. */
            WORLD,
            /** One player, wherever they are. */
            PLAYER,
            /** Everybody in a permission group. */
            GROUP
        }

        /**
         * Reads the audience line.
         *
         * <p>Anything that is not a recognised prefix followed by a colon means the whole world,
         * so a line somebody has written a note on fools everybody rather than nobody.
         */
        static Audience on(String written) {
            if (written.length() < 2 || written.charAt(1) != AUDIENCE_SEPARATOR) {
                return new Audience(Kind.WORLD, "");
            }

            String name = written.substring(2).trim();
            return switch (Character.toLowerCase(written.charAt(0))) {
                case ONE_PLAYER -> new Audience(Kind.PLAYER, name);
                case GROUP -> new Audience(Kind.GROUP, name);
                default -> new Audience(Kind.WORLD, "");
            };
        }

        /**
         * Shows this audience a sky.
         *
         * @return whether anybody was shown it
         */
        boolean show(ChipState state, Sky sky) {
            if (name.isEmpty() && kind != Kind.WORLD) {
                return false;
            }
            return switch (kind) {
                case WORLD -> state.illusions().showSkyIn(state.world().id(), sky) > 0;
                case PLAYER -> state.illusions().showSkyToNamed(name, sky);
                case GROUP -> state.illusions().showSkyToGroup(name, sky) > 0;
            };
        }
    }

    /** Fools whoever a sign names, wherever they are standing. */
    private static final class NamedAudience implements ICLogic {

        private final Sky sky;

        /** Whether this chip currently has an illusion up that it needs to take down. */
        private boolean showing;

        NamedAudience(Sky sky) {
            this.sky = sky;
        }

        @Override
        public void trigger(ChipState state) {
            Audience audience = Audience.on(state.sign().trimmedText(AUDIENCE_LINE));

            if (state.isAnyInputActive()) {
                if (!showing && worthShowing(state, sky) && audience.show(state, sky)) {
                    showing = true;
                }
            } else if (showing) {
                audience.show(state, Sky.REAL);
                showing = false;
            }

            state.setMainOutput(showing);
        }

        @Override
        public void unload(ChipState state) {
            if (showing) {
                Audience.on(state.sign().trimmedText(AUDIENCE_LINE)).show(state, Sky.REAL);
                showing = false;
            }
        }
    }

    /** Fools whoever is standing close enough, and stops as they walk away. */
    private static final class NearbyAudience implements SelfTriggeringICLogic {

        private final Sky sky;

        /** Everybody this chip is currently fooling, by account name. */
        private final Set<String> fooled = new LinkedHashSet<>();

        /** Everybody it has already greeted on this visit, so nobody is greeted twice. */
        private final Set<String> greeted = new LinkedHashSet<>();

        NearbyAudience(Sky sky) {
            this.sky = sky;
        }

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                fool(state);
            } else {
                release(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            if (state.isAnyInputActive()) {
                fool(state);
            }
        }

        @Override
        public void unload(ChipState state) {
            release(state);
        }

        /** Shows everybody in range the illusion, and puts back anybody who has left. */
        private void fool(ChipState state) {
            if (!worthShowing(state, sky)) {
                release(state);
                return;
            }

            int radius = radiusOn(state);
            String greeting = state.sign().text(GREETING_LINE);
            Set<String> inRange = new LinkedHashSet<>();

            for (Bystander person : peopleNear(state, radius)) {
                String name = key(person.name());
                inRange.add(name);

                if (fooled.add(name)) {
                    person.showSky(sky);
                }
                if (!greeting.isBlank() && greeted.add(name)) {
                    person.tell(Component.text(greeting));
                }
            }

            putBackEverybodyBut(state, inRange);
            greeted.retainAll(inRange);
            state.setMainOutput(!fooled.isEmpty());
        }

        /** Puts everybody this chip had fooled back to the real sky. */
        private void release(ChipState state) {
            putBackEverybodyBut(state, Set.of());
            greeted.clear();
            state.setMainOutput(false);
        }

        /**
         * Puts back everybody being fooled who is not in a set.
         *
         * <p>Somebody who has walked out of range is no longer among the people the world reports
         * standing nearby, so they are put back by name rather than through the reference the chip
         * no longer has.
         */
        private void putBackEverybodyBut(ChipState state, Set<String> keep) {
            fooled.removeIf(name -> {
                if (keep.contains(name)) {
                    return false;
                }
                state.illusions().showSkyToNamed(name, Sky.REAL);
                return true;
            });
        }

        private static List<Bystander> peopleNear(ChipState state, int radius) {
            return state.world().bystandersNear(Vec3d.middleOf(state.backPosition()), radius).stream()
                    .filter(Bystander::isPlayer)
                    .toList();
        }

        /** How far this chip reaches. */
        private static int radiusOn(ChipState state) {
            try {
                int written = Integer.parseInt(state.sign().trimmedText(RADIUS_LINE));
                return Math.clamp(written, MIN_RADIUS, MAX_RADIUS);
            } catch (NumberFormatException e) {
                return DEFAULT_RADIUS;
            }
        }

        private static String key(String name) {
            return name.toLowerCase(Locale.ROOT);
        }
    }
}
