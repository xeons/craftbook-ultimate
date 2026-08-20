package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleBystander;
import com.xeonproductions.craftbookultimate.core.entity.SimpleRoster;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.illusion.SimpleIllusions;
import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.message.SimpleAnnouncer;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The chips that show weather the world is not having")
class WeatherIllusionsTest {

    /** Where every chip in these tests hangs its sign. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    /** The block it hangs on, which is what its reach is measured from. */
    private static final Vec3i BEHIND = new Vec3i(0, 64, -1);

    private final SimpleIllusions illusions = new SimpleIllusions();
    private final ChipServices services =
            ChipServices.create(SimpleRoster.empty(), new SimpleAnnouncer(), illusions);
    private final SimpleChipWorld world = new SimpleChipWorld();

    private SimpleChipState.Builder chip(String... lines) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .services(services)
                .world(world)
                .at(SIGN, BlockFace.SOUTH)
                .sign(lines);
    }

    /** Somebody standing a given number of blocks from the block the sign hangs on. */
    private SimpleBystander standing(String name, double away) {
        SimpleBystander person =
                SimpleBystander.player(name).at(Vec3d.middleOf(BEHIND).add(0, 0, -away));
        world.withBystander(person);
        return person;
    }

    @Nested
    @DisplayName("showing rain that is not falling")
    class ShowingRainThatIsNotFalling {

        @Test
        void showsItToEverybodyInTheWorldWhenTheSignNamesNobody() {
            illusions.with("Alice", world.id()).with("Bob", world.id());
            SimpleChipState state =
                    chip("", "[MCX235]", "", "").inputs(true, false, false).build();

            WeatherIllusions.falseWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.DOWNFALL);
            assertThat(illusions.shownTo("Bob")).isEqualTo(Sky.DOWNFALL);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void showsItToOnlyTheNamedPlayer() {
            illusions.with("Alice", world.id()).with("Bob", world.id());
            SimpleChipState state =
                    chip("", "[MCX235]", "p:Alice", "").inputs(true, false, false).build();

            WeatherIllusions.falseWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.DOWNFALL);
            assertThat(illusions.shownTo("Bob")).isEqualTo(Sky.REAL);
        }

        @Test
        void showsItToEverybodyInANamedGroup() {
            illusions.with("Alice", "builders", world.id()).with("Bob", "", world.id());
            SimpleChipState state =
                    chip("", "[MCX235]", "g:builders", "").inputs(true, false, false).build();

            WeatherIllusions.falseWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.DOWNFALL);
            assertThat(illusions.shownTo("Bob")).isEqualTo(Sky.REAL);
        }

        @Test
        void showsNothingWhileItIsAlreadyRaining() {
            illusions.with("Alice", world.id());
            world.setRaining(true, 100);
            SimpleChipState state =
                    chip("", "[MCX235]", "", "").inputs(true, false, false).build();

            WeatherIllusions.falseWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void givesTheRealSkyBackWhenNothingDrivesItAnyMore() {
            illusions.with("Alice", world.id());
            SimpleChipState state =
                    chip("", "[MCX235]", "", "").inputs(true, false, false).build();
            ICLogic chip = WeatherIllusions.falseWeather();
            chip.trigger(state);

            chip.trigger(state.withInput(0, false));

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void givesTheRealSkyBackWhenItIsUnloaded() {
            illusions.with("Alice", world.id());
            SimpleChipState state =
                    chip("", "[MCX235]", "", "").inputs(true, false, false).build();
            ICLogic chip = WeatherIllusions.falseWeather();
            chip.trigger(state);

            chip.unload(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
        }

        @Test
        void leavesEverybodyAloneWhenItNeverShowedAnybodyAnything() {
            illusions.with("Alice", world.id());
            illusions.showSkyToNamed("Alice", Sky.CLEAR);
            SimpleChipState state =
                    chip("", "[MCX235]", "", "").inputs(false, false, false).build();

            WeatherIllusions.falseWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.CLEAR);
        }
    }

    @Nested
    @DisplayName("hiding rain that is falling")
    class HidingRainThatIsFalling {

        @Test
        void hidesItFromEverybodyInTheWorld() {
            illusions.with("Alice", world.id());
            world.setRaining(true, 100);
            SimpleChipState state =
                    chip("", "[MCX237]", "", "").inputs(true, false, false).build();

            WeatherIllusions.hideWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.CLEAR);
        }

        @Test
        void hidesAThunderstormAsWellAsPlainRain() {
            illusions.with("Alice", world.id());
            world.setThundering(true, 100);
            SimpleChipState state =
                    chip("", "[MCX237]", "", "").inputs(true, false, false).build();

            WeatherIllusions.hideWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.CLEAR);
        }

        @Test
        void hidesNothingWhileTheSkyIsAlreadyClear() {
            illusions.with("Alice", world.id());
            SimpleChipState state =
                    chip("", "[MCX237]", "", "").inputs(true, false, false).build();

            WeatherIllusions.hideWeather().trigger(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
            assertThat(state.mainOutput()).isFalse();
        }
    }

    @Nested
    @DisplayName("fooling whoever is standing close enough")
    class FoolingWhoeverIsStandingCloseEnough {

        @Test
        void showsRainToSomebodyInRange() {
            SimpleBystander near = standing("Alice", 5);
            SimpleChipState state =
                    chip("", "[MCX236]", "10", "").inputs(true, false, false).build();

            WeatherIllusions.distanceFalseWeather().trigger(state);

            assertThat(near.shownSky()).isEqualTo(Sky.DOWNFALL);
            assertThat(state.mainOutput()).isTrue();
        }

        @Test
        void leavesSomebodyBeyondItsReachAlone() {
            SimpleBystander far = standing("Alice", 20);
            SimpleChipState state =
                    chip("", "[MCX236]", "10", "").inputs(true, false, false).build();

            WeatherIllusions.distanceFalseWeather().trigger(state);

            assertThat(far.shownSky()).isEqualTo(Sky.REAL);
        }

        @Test
        void reachesTenBlocksWhenTheSignDoesNotSay() {
            SimpleBystander near = standing("Alice", 8);
            SimpleChipState state =
                    chip("", "[MCX236]", "", "").inputs(true, false, false).build();

            WeatherIllusions.distanceFalseWeather().trigger(state);

            assertThat(near.shownSky()).isEqualTo(Sky.DOWNFALL);
        }

        @Test
        void greetsSomebodyWalkingIntoRangeOnceOnly() {
            SimpleBystander near = standing("Alice", 5);
            SimpleChipState state = chip("", "[MCX236]", "10", "mind the weather")
                    .inputs(true, false, false)
                    .build();
            SelfTriggeringICLogic chip = WeatherIllusions.distanceFalseWeather();

            chip.trigger(state);
            chip.tick(state);
            chip.tick(state);

            assertThat(near.plainMessages()).containsExactly("mind the weather");
        }

        @Test
        void givesTheRealSkyBackToSomebodyWhoWalksAway() {
            SimpleBystander walker = standing("Alice", 5);
            illusions.with("Alice", world.id());
            SimpleChipState state =
                    chip("", "[MCX236]", "10", "").inputs(true, false, false).build();
            SelfTriggeringICLogic chip = WeatherIllusions.distanceFalseWeather();
            chip.trigger(state);

            walker.at(Vec3d.middleOf(BEHIND).add(0, 0, -40));
            chip.tick(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
        }

        @Test
        void greetsSomebodyAgainOnTheirNextVisit() {
            SimpleBystander walker = standing("Alice", 5);
            SimpleChipState state = chip("", "[MCX236]", "10", "mind the weather")
                    .inputs(true, false, false)
                    .build();
            SelfTriggeringICLogic chip = WeatherIllusions.distanceFalseWeather();
            chip.trigger(state);

            walker.at(Vec3d.middleOf(BEHIND).add(0, 0, -40));
            chip.tick(state);
            walker.at(Vec3d.middleOf(BEHIND).add(0, 0, -5));
            chip.tick(state);

            assertThat(walker.plainMessages()).containsExactly("mind the weather", "mind the weather");
        }

        @Test
        void putsEverybodyBackWhenNothingDrivesItAnyMore() {
            standing("Alice", 5);
            illusions.with("Alice", world.id());
            SimpleChipState state =
                    chip("", "[MCX236]", "10", "").inputs(true, false, false).build();
            SelfTriggeringICLogic chip = WeatherIllusions.distanceFalseWeather();
            chip.trigger(state);

            chip.trigger(state.withInput(0, false));

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
            assertThat(state.mainOutput()).isFalse();
        }

        @Test
        void putsEverybodyBackWhenItIsUnloaded() {
            standing("Alice", 5);
            illusions.with("Alice", world.id());
            SimpleChipState state =
                    chip("", "[MCX236]", "10", "").inputs(true, false, false).build();
            SelfTriggeringICLogic chip = WeatherIllusions.distanceFalseWeather();
            chip.trigger(state);

            chip.unload(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
        }

        @Test
        void putsEverybodyBackOnceTheRealWeatherCatchesUp() {
            standing("Alice", 5);
            illusions.with("Alice", world.id());
            SimpleChipState state =
                    chip("", "[MCX236]", "10", "").inputs(true, false, false).build();
            SelfTriggeringICLogic chip = WeatherIllusions.distanceFalseWeather();
            chip.trigger(state);

            world.setRaining(true, 100);
            chip.tick(state);

            assertThat(illusions.shownTo("Alice")).isEqualTo(Sky.REAL);
        }

        @Test
        void hidesTheRainFromSomebodyInRange() {
            SimpleBystander near = standing("Alice", 5);
            world.setRaining(true, 100);
            SimpleChipState state =
                    chip("", "[MCX238]", "10", "").inputs(true, false, false).build();

            WeatherIllusions.distanceHideWeather().trigger(state);

            assertThat(near.shownSky()).isEqualTo(Sky.CLEAR);
        }

        @Test
        void staysStillWhileNothingDrivesIt() {
            SimpleBystander near = standing("Alice", 5);
            SimpleChipState state =
                    chip("", "[MCX236]", "10", "").inputs(false, false, false).build();

            WeatherIllusions.distanceFalseWeather().tick(state);

            assertThat(near.shownSky()).isEqualTo(Sky.REAL);
        }
    }
}
