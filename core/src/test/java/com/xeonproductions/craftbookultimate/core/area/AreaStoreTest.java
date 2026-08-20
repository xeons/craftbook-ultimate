package com.xeonproductions.craftbookultimate.core.area;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Saved areas")
class AreaStoreTest {

    private static final UUID WORLD = UUID.fromString("2f9e4f4f-0000-4000-8000-00000000abcd");

    @Nested
    @DisplayName("naming one")
    class NamingOne {

        @Test
        void keepsWhoseItIsApartFromWhichOfTheirsItIs() {
            assertThat(AreaName.parse("alice", "cellar"))
                    .contains(new AreaName("alice", "cellar"));
        }

        @Test
        void foldsTheIdentifierToLowerCaseSoOneAreaHasOneName() {
            assertThat(new AreaName("alice", "Cellar")).isEqualTo(new AreaName("alice", "cellar"));
        }

        @Test
        void leavesTheNamespaceCaseAloneBecauseAPlayerNameHasOne() {
            assertThat(new AreaName("Alice", "cellar").namespace()).isEqualTo("Alice");
        }

        @Test
        void takesOffTheDashesASignWearsToShowWhatIsStanding() {
            assertThat(AreaName.parse("alice", "-cellar-"))
                    .contains(new AreaName("alice", "cellar"));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", "  ", "a name with spaces", "fourteencharsx", "cellar!"})
        void refusesSomethingNoFileCouldBeCalled(String written) {
            assertThat(AreaName.parse("alice", written)).isEmpty();
        }

        @Test
        void refusesANamespaceNoFolderCouldBeCalled() {
            assertThat(AreaName.parse("../etc", "cellar")).isEmpty();
        }

        @Test
        void knowsTheOneEverybodyShares() {
            assertThat(new AreaName(AreaName.GLOBAL, "spawn").isGlobal()).isTrue();
            assertThat(new AreaName("alice", "spawn").isGlobal()).isFalse();
        }

        @Test
        void treatsTheTwoDashesAsNamingNothing() {
            assertThat(AreaName.namesSomething(AreaName.NONE)).isFalse();
            assertThat(AreaName.namesSomething("")).isFalse();
            assertThat(AreaName.namesSomething("-night-")).isTrue();
        }

        @Test
        void readsBackAsSomethingAPersonCanFollow() {
            assertThat(new AreaName("alice", "cellar")).hasToString("alice/cellar");
        }
    }

    @Nested
    @DisplayName("saying where one belongs")
    class SayingWhereOneBelongs {

        @Test
        void coversTheBoxBetweenTwoCorners() {
            AreaAnchor anchor = AreaAnchor.between(
                    WORLD, new Vec3i(10, 64, 10), new Vec3i(12, 66, 14));

            assertThat(anchor.origin()).isEqualTo(new Vec3i(10, 64, 10));
            assertThat(anchor.size()).isEqualTo(new Vec3i(3, 3, 5));
            assertThat(anchor.far()).isEqualTo(new Vec3i(12, 66, 14));
        }

        @Test
        void doesNotMindWhichWayRoundTheCornersArePickedOut() {
            AreaAnchor one = AreaAnchor.between(WORLD, new Vec3i(12, 66, 14), new Vec3i(10, 64, 10));
            AreaAnchor other = AreaAnchor.between(WORLD, new Vec3i(10, 64, 10), new Vec3i(12, 66, 14));

            assertThat(one).isEqualTo(other);
        }

        @Test
        void countsTheBlocksItCovers() {
            assertThat(AreaAnchor.between(WORLD, new Vec3i(0, 0, 0), new Vec3i(1, 1, 1)).volume())
                    .isEqualTo(8);
        }

        @Test
        void refusesToBeBuiltAroundNothing() {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    new AreaAnchor(WORLD, Vec3i.ZERO, new Vec3i(0, 1, 1)));
        }
    }

    @Nested
    @DisplayName("keeping where one belongs between restarts")
    class KeepingWhereOneBelongs {

        @Test
        void putsItBackWhereItWas() {
            AreaAnchor anchor = AreaAnchor.between(
                    WORLD, new Vec3i(-40, 61, 300), new Vec3i(-36, 68, 312));

            assertThat(AreaAnchor.read(anchor.save())).contains(anchor);
        }

        @Test
        void doesNotMindWhichOrderTheLinesAreIn() {
            List<String> lines = List.of(
                    "size 2 2 2", "origin 1 2 3", "world " + WORLD);

            assertThat(AreaAnchor.read(lines))
                    .contains(new AreaAnchor(WORLD, new Vec3i(1, 2, 3), new Vec3i(2, 2, 2)));
        }

        @ParameterizedTest(name = "missing {0}")
        @ValueSource(strings = {"world", "origin", "size"})
        void refusesAFileThatDoesNotSayEverything(String missing) {
            List<String> lines = List.of(
                    "world " + WORLD, "origin 1 2 3", "size 2 2 2").stream()
                    .filter(line -> !line.startsWith(missing))
                    .toList();

            assertThat(AreaAnchor.read(lines)).isEmpty();
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {
            "origin one two three", "size 0 1 1", "world not-a-world", "origin 1 2"
        })
        void refusesAFileWithALineItCannotRead(String broken) {
            String key = broken.split(" ")[0] + " ";
            List<String> lines = new ArrayList<>(
                    List.of("world " + WORLD, "origin 1 2 3", "size 2 2 2"));
            lines.replaceAll(line -> line.startsWith(key) ? broken : line);

            assertThat(AreaAnchor.read(lines)).isEmpty();
        }

        @Test
        void carriesOnPastALineItDoesNotRecognise() {
            List<String> lines = List.of(
                    "nonsense here", "world " + WORLD, "origin 1 2 3", "size 2 2 2");

            assertThat(AreaAnchor.read(lines)).isPresent();
        }
    }

    @Nested
    @DisplayName("a vault holding nothing")
    class AVaultHoldingNothing {

        private final AreaVault empty = AreaVault.empty();

        @Test
        void hasNoAreasAndDoesNothing() {
            AreaName name = new AreaName("alice", "cellar");

            assertThat(empty.has(name)).isFalse();
            assertThat(empty.anchorOf(name)).isEmpty();
            assertThat(empty.restore(name)).isFalse();
            assertThat(empty.clear(name)).isFalse();
            assertThat(empty.capture(name)).isFalse();
            assertThat(empty.delete(name)).isFalse();
            assertThat(empty.idsIn("alice")).isEmpty();
            assertThat(empty.countIn("alice")).isZero();
        }
    }
}
