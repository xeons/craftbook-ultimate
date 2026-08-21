package com.xeonproductions.craftbookultimate.paper.ic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.paper.harness.ChipWorld;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Keeping track of the chips in the world")
class ICManagerTest {

    private ChipWorld game;

    @BeforeEach
    void setUp() {
        game = new ChipWorld();
    }

    @AfterEach
    void tearDown() {
        game.close();
    }

    /** A command controlled chip, which is one of the few that can be told to tick or not to. */
    private Block switchSign(int x, int y, int z, String identifier) {
        return game.signAt(x, y, z, BlockFace.SOUTH, "COMMAND CTRL", identifier, "door", "");
    }

    @Nested
    @DisplayName("A sign that has been rewritten")
    class Rewritten {

        @Test
        void isNotPickedUpByLoadingAlone() {
            // Loading is what a chunk arriving does, and a chunk may arrive twice. It has to leave
            // a chip that is already there alone, which is exactly why rewriting a sign needs
            // something else.
            Block sign = switchSign(0, 64, 1, "[MCX120]");
            game.manager().load(sign);

            game.write(sign, "COMMAND CTRL", "[MCX120]S", "door", "");

            assertThat(game.manager().load(sign)).isEmpty();
        }

        @Test
        void keepsTheOldChipTickingUntilItIsReloaded() {
            Block sign = switchSign(0, 64, 1, "[MCX120]");
            game.manager().load(sign);
            assertThat(game.manager().at(sign).orElseThrow().isSelfTriggering()).isFalse();

            game.write(sign, "COMMAND CTRL", "[MCX120]S", "door", "");
            game.manager().load(sign);

            assertThat(game.manager().at(sign).orElseThrow().isSelfTriggering()).isFalse();
        }

        @Test
        void startsTickingWhenAnSIsAddedAndItIsReloaded() {
            Block sign = switchSign(0, 64, 1, "[MCX120]");
            game.manager().load(sign);

            game.write(sign, "COMMAND CTRL", "[MCX120]S", "door", "");
            game.manager().reload(sign);

            assertThat(game.manager().at(sign).orElseThrow().isSelfTriggering()).isTrue();
        }

        @Test
        void stopsTickingWhenTheSIsTakenAwayAgain() {
            Block sign = switchSign(0, 64, 1, "[MCX120]S");
            game.manager().load(sign);
            assertThat(game.manager().at(sign).orElseThrow().isSelfTriggering()).isTrue();

            game.write(sign, "COMMAND CTRL", "[MCX120]", "door", "");
            game.manager().reload(sign);

            assertThat(game.manager().at(sign).orElseThrow().isSelfTriggering()).isFalse();
        }

        @Test
        void becomesTheChipItsNewModelNumberNames() {
            Block sign = switchSign(0, 64, 1, "[MCX120]");
            game.manager().load(sign);

            game.write(sign, "REPEATER", "[MC1000]", "", "");
            game.manager().reload(sign);

            assertThat(game.manager().at(sign).orElseThrow().definition().model())
                    .isEqualTo("MC1000");
        }

        @Test
        void leavesNothingBehindWhenItNoLongerNamesAChip() {
            Block sign = switchSign(0, 64, 1, "[MCX120]");
            game.manager().load(sign);

            game.write(sign, "COMMAND CTRL", "just a sign now", "door", "");

            assertThat(game.manager().reload(sign)).isEmpty();
            assertThat(game.manager().at(sign)).isEmpty();
            assertThat(game.manager().loadedCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Clearing a stretch of world")
    class UnloadWithin {

        @Test
        void stopsTheChipsStandingInside() {
            game.manager().load(switchSign(0, 64, 1, "[MCX120]"));
            game.manager().load(switchSign(4, 64, 1, "[MCX120]"));
            assertThat(game.manager().loadedCount()).isEqualTo(2);

            int stopped = game.manager().unloadWithin(
                    game.world().getUID(),
                    new Bounds(new Vec3i(-1, 60, -1), new Vec3i(10, 70, 10)));

            assertThat(stopped).isEqualTo(2);
            assertThat(game.manager().loadedCount()).isZero();
        }

        @Test
        void leavesTheChipsOutsideAlone() {
            Block inside = switchSign(0, 64, 1, "[MCX120]");
            Block outside = switchSign(40, 64, 1, "[MCX120]");
            game.manager().load(inside);
            game.manager().load(outside);

            game.manager().unloadWithin(
                    game.world().getUID(),
                    new Bounds(new Vec3i(-1, 60, -1), new Vec3i(10, 70, 10)));

            assertThat(game.manager().at(inside)).isEmpty();
            assertThat(game.manager().at(outside)).isPresent();
        }

        @Test
        void givesBackWhatTheChipsInsideHadClaimed() {
            // The reason this matters beyond a tidy map: a command controlled chip that is never
            // stopped keeps its switch registered, and a switch nothing follows still answers the
            // command that throws it.
            game.manager().load(switchSign(0, 64, 1, "[MCX120]"));
            assertThat(game.services().switchboard().isKnown("door")).isTrue();

            game.manager().unloadWithin(
                    game.world().getUID(),
                    new Bounds(new Vec3i(-1, 60, -1), new Vec3i(10, 70, 10)));

            assertThat(game.services().switchboard().isKnown("door")).isFalse();
        }
    }
}
