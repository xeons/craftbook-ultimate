// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.paper.harness.ChipWorld;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.SignChangeEvent;
import com.xeonproductions.craftbookultimate.core.ic.LineContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@DisplayName("Writing and breaking a chip")
class ICSignListenerTest {

    private ChipWorld game;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        game = new ChipWorld();
        game.server().getPluginManager().registerEvents(
                // A lenient name resolver, because the real one asks the server what is a block
                // as it starts up and the mock server has no answer for that. What is under test
                // here is the review rather than the block table.
                new ICSignListener(game.manager(), new RegionSchedulers(game.plugin()),
                        LineContext.lenient()),
                game.plugin());
        player = game.server().addPlayer();
    }

    @AfterEach
    void tearDown() {
        game.close();
    }

    /** A chip on a wall sign facing south, so it hangs on the block to its north. */
    private Block chipAt(int x, int y, int z) {
        Block sign = game.signAt(x, y, z, BlockFace.SOUTH, "COMMAND CTRL", "[MCX120]", "door", "");
        game.manager().load(sign);
        return sign;
    }

    private void breakBlock(Block block) {
        game.server().getPluginManager().callEvent(new BlockBreakEvent(block, player));
    }

    /** Everything the player has been told since last asked, run together. */
    private String toldSince() {
        StringBuilder said = new StringBuilder();
        for (String message = player.nextMessage(); message != null; message = player.nextMessage()) {
            said.append(message).append('\n');
        }
        return said.toString();
    }

    @Nested
    @DisplayName("by breaking its own sign")
    class TheSign {

        @Test
        void stopsTheChip() {
            Block sign = chipAt(0, 64, 1);

            breakBlock(sign);

            assertThat(game.manager().at(sign)).isEmpty();
        }

        @Test
        void saysSoToWhoeverBrokeIt() {
            Block sign = chipAt(0, 64, 1);

            breakBlock(sign);

            assertThat(toldSince()).contains("Destroyed the Command Controlled chip");
        }

        @Test
        void givesBackWhateverItHadClaimed() {
            Block sign = chipAt(0, 64, 1);
            assertThat(game.services().switchboard().isKnown("door")).isTrue();

            breakBlock(sign);

            assertThat(game.services().switchboard().isKnown("door")).isFalse();
        }
    }

    @Nested
    @DisplayName("by breaking the block its sign hangs on")
    class TheSupport {

        @Test
        void stopsTheChipToo() {
            // The sign is destroyed a moment later by the game itself, and that raises no event of
            // its own. Left to the block that was broken, nothing would ever stop this chip.
            Block sign = chipAt(0, 64, 1);

            breakBlock(game.supportOf(sign, BlockFace.SOUTH));

            assertThat(game.manager().at(sign)).isEmpty();
        }

        @Test
        void saysSoAndSaysWhy() {
            Block sign = chipAt(0, 64, 1);

            breakBlock(game.supportOf(sign, BlockFace.SOUTH));

            assertThat(toldSince())
                    .contains("Destroyed the Command Controlled chip")
                    .contains("what its sign hung on");
        }

        @Test
        void takesDownEveryChipHangingOnIt() {
            // Four signs can hang on one block, and all four go with it.
            Block support = game.wallAt(0, 64, 0);
            Block north =
                    game.signAt(0, 64, -1, BlockFace.NORTH, "COMMAND CTRL", "[MCX120]", "a", "");
            Block south =
                    game.signAt(0, 64, 1, BlockFace.SOUTH, "COMMAND CTRL", "[MCX120]", "b", "");
            game.manager().load(north);
            game.manager().load(south);

            breakBlock(support);

            assertThat(game.manager().at(north)).isEmpty();
            assertThat(game.manager().at(south)).isEmpty();
        }
    }

    @Nested
    @DisplayName("by breaking a block that merely stands near it")
    class SomethingElse {

        @Test
        void leavesTheChipAlone() {
            // This block is beside the sign, not behind it. Reading the rule backwards would take
            // down the chip that was never touched and leave the one that really was hanging there.
            Block sign = chipAt(0, 64, 1);
            Block beside = game.wallAt(1, 64, 1);

            breakBlock(beside);

            assertThat(game.manager().at(sign)).isPresent();
        }

        @Test
        void saysNothingAtAll() {
            chipAt(0, 64, 1);
            toldSince();

            breakBlock(game.wallAt(1, 64, 1));

            assertThat(toldSince()).doesNotContain("Destroyed");
        }
    }

    @Nested
    @DisplayName("writing a sign whose line the chip cannot read")
    class UnreadableLines {

        /** Writes a sign the way a builder does, and answers whether it was refused. */
        private boolean refused(String model, String third, String fourth) {
            // The item sensor is restricted, and the permission check comes first. What is under
            // test here is the line review, so the player is given the chip they are building.
            player.addAttachment(game.plugin(), "craftbook.ic.restricted.mcx138", true);
            Block sign = game.signAt(9, 70, 9, BlockFace.SOUTH, "", model, third, fourth);
            // A list the listener can write back into, since accepting a sign rewrites its
            // identifier line into the spelling the rest of the plugin reads.
            SignChangeEvent event = new SignChangeEvent(
                    sign, player, new ArrayList<>(List.of(
                            Component.text(""), Component.text(model),
                            Component.text(third), Component.text(fourth))),
                    Side.FRONT);
            game.server().getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        @Test
        @DisplayName("is refused, rather than left to look like a wiring fault")
        void isRefused() {
            assertThat(refused("[MCX138]", "item:stone", "")).isTrue();
        }

        @Test
        @DisplayName("says what was written and what the line would have taken")
        void saysWhatWouldHaveWorked() {
            refused("[MCX138]", "item:stone", "");

            assertThat(toldSince())
                    .contains("item:stone")
                    .contains("ID:<item>");
        }

        @Test
        @DisplayName("is accepted once the line is written the way the chip reads it")
        void isAcceptedOnceItIsRight() {
            assertThat(refused("[MCX138]", "ID:stone", "")).isFalse();
        }
    }
}
