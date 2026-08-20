package com.xeonproductions.craftbookultimate.core.effect;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Reading a firework display script")
class FireworkShowTest {

    private static FireworkShow plain(String... lines) {
        return FireworkShow.parse(List.of(lines), FireworkShow.Dialect.PLAIN);
    }

    private static FireworkShow named(String... lines) {
        return FireworkShow.parse(List.of(lines), FireworkShow.Dialect.NAMED);
    }

    @Nested
    @DisplayName("one launch per line")
    class OneLaunchPerLine {

        @Test
        void readsALaunch() {
            FireworkShow show = plain("launch:0,2,0;1;BALL;255,0,0;0,255,0;trail");

            assertThat(show.steps()).hasSize(1);
            FireworkShow.Step.Launch launch = (FireworkShow.Step.Launch) show.steps().get(0);
            assertThat(launch.offset()).isEqualTo(new Vec3d(0, 2, 0));
            assertThat(launch.burst().shape()).isEqualTo(FireworkBurst.Shape.BALL);
            assertThat(launch.burst().colours()).containsExactly(0xFF0000);
            assertThat(launch.burst().fades()).containsExactly(0x00FF00);
            assertThat(launch.burst().trail()).isTrue();
            assertThat(launch.burst().flicker()).isFalse();
        }

        @Test
        void readsAWait() {
            FireworkShow show = plain("wait:20");

            assertThat(show.steps()).containsExactly(new FireworkShow.Step.Wait(20));
        }

        @Test
        void keepsTheStepsInTheOrderTheyWereWritten() {
            FireworkShow show = plain(
                    "launch:0,0,0;1;BALL;255,0,0;0,0,0",
                    "wait:5",
                    "launch:0,0,0;1;STAR;0,0,255;0,0,0");

            assertThat(show.steps()).hasSize(3);
            assertThat(show.steps().get(1)).isEqualTo(new FireworkShow.Step.Wait(5));
        }

        @Test
        void skipsCommentsAndBlankLines() {
            FireworkShow show = plain("# a comment", "", "wait:5");

            assertThat(show.steps()).hasSize(1);
        }

        @Test
        void skipsALineItCannotRead() {
            FireworkShow show = plain("launch:not;enough", "wait:5");

            assertThat(show.steps()).containsExactly(new FireworkShow.Step.Wait(5));
        }
    }

    @Nested
    @DisplayName("effects built by name")
    class EffectsBuiltByName {

        @Test
        void firesAnEffectItBuiltEarlier() {
            FireworkShow show = named(
                    "start bigred",
                    "set.shape ball_large",
                    "set.color 255,0,0",
                    "set.trail",
                    "build",
                    "launch bigred");

            assertThat(show.steps()).hasSize(1);
            FireworkShow.Step.Launch launch = (FireworkShow.Step.Launch) show.steps().get(0);
            assertThat(launch.burst().shape()).isEqualTo(FireworkBurst.Shape.BALL_LARGE);
            assertThat(launch.burst().colours()).containsExactly(0xFF0000);
            assertThat(launch.burst().trail()).isTrue();
        }

        @Test
        void firesTheSameEffectAsOftenAsItIsAskedTo() {
            FireworkShow show = named(
                    "start one", "set.color 255,0,0", "build", "launch one", "launch one");

            assertThat(show.steps()).hasSize(2);
        }

        @Test
        void firesNothingForANameItNeverBuilt() {
            assertThat(named("launch missing").isEmpty()).isTrue();
        }

        @Test
        void launchesWhereTheLastLocationSaid() {
            FireworkShow show = named(
                    "start one", "set.color 255,0,0", "build", "location 3,4,5", "launch one");

            FireworkShow.Step.Launch launch = (FireworkShow.Step.Launch) show.steps().get(0);
            assertThat(launch.offset()).isEqualTo(new Vec3d(3, 4, 5));
        }

        @Test
        void readsASoundWithItsOwnPlaceAndLoudness() {
            FireworkShow show = named("sound entity.firework_rocket.blast 1,2,3 0.5 2.0");

            FireworkShow.Step.Sound sound = (FireworkShow.Step.Sound) show.steps().get(0);
            assertThat(sound.sound())
                    .isEqualTo(Key.key("minecraft", "entity.firework_rocket.blast"));
            assertThat(sound.offset()).isEqualTo(new Vec3d(1, 2, 3));
            assertThat(sound.volume()).isEqualTo(0.5f);
            assertThat(sound.pitch()).isEqualTo(2.0f);
        }

        @Test
        void putsACountedDurationOnTheFuse() {
            FireworkShow show =
                    named("start one", "set.color 255,0,0", "build", "duration 1", "launch one");

            FireworkShow.Step.Launch launch = (FireworkShow.Step.Launch) show.steps().get(0);
            assertThat(launch.fuseTicks()).isEqualTo(30);
        }

        @Test
        void takesAPreciseDurationAsTicks() {
            FireworkShow show = named(
                    "start one", "set.color 255,0,0", "build", "duration 5 precise", "launch one");

            FireworkShow.Step.Launch launch = (FireworkShow.Step.Launch) show.steps().get(0);
            assertThat(launch.fuseTicks()).isEqualTo(10);
        }

        @Test
        void stripsACommentFromTheEndOfALine() {
            FireworkShow show = named("wait 20 # pause here");

            assertThat(show.steps()).containsExactly(new FireworkShow.Step.Wait(20));
        }
    }

    @Test
    void readsNothingFromAnEmptyFile() {
        assertThat(plain().isEmpty()).isTrue();
        assertThat(named().isEmpty()).isTrue();
    }
}
