// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.ic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.radio.Band;
import com.xeonproductions.craftbookultimate.paper.adapter.Directions;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.harness.ChipWorld;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * One chip driving another.
 *
 * <p>A chip drives its outputs by writing a lever, and the server raises a redstone event for a
 * lever a player clicks and for nothing a plugin writes. So a chip's output was heard by the world
 * and by no chip at all, and a clock could not drive a transmitter without a run of redstone dust
 * between them raising an event of its own. See finding 150.
 */
@DisplayName("One chip driving another")
class ChipChainingTest {

    private ChipWorld game;

    @BeforeEach
    void setUp() {
        game = new ChipWorld();
    }

    @AfterEach
    void tearDown() {
        game.close();
    }

    /** Puts a lever on a block, which is what a chip reads as a wired pin and writes to. */
    private Block leverAt(int x, int y, int z) {
        Block block = game.world().getBlockAt(x, y, z);
        block.setType(Material.LEVER);
        Switch lever = (Switch) block.getBlockData();
        lever.setPowered(false);
        block.setBlockData(lever);
        return block;
    }

    /** Where a chip's pin really sits, worked out from its own wiring rather than guessed at. */
    private Block pin(Block sign, org.bukkit.block.BlockFace facing, PinLayout layout, boolean input) {
        com.xeonproductions.craftbookultimate.core.math.BlockFace front =
                Directions.toDomain(facing).orElseThrow();
        Vec3i at = layout.pinPosition(
                input ? 0 : layout.outputPin(0),
                Positions.toDomain(sign),
                front);
        return Positions.toBlock(game.world(), at);
    }

    private static boolean isOn(Block lever) {
        return lever.getBlockData() instanceof Switch flipped && flipped.isPowered();
    }

    /** The step from a chip's sign to one of its pins, for a chip facing this way. */
    private Vec3i offsetTo(PinLayout layout, int pin, org.bukkit.block.BlockFace facing) {
        com.xeonproductions.craftbookultimate.core.math.BlockFace front =
                Directions.toDomain(facing).orElseThrow();
        Vec3i origin = new Vec3i(0, 0, 0);
        return layout.pinPosition(pin, origin, front);
    }

    @Test
    void drivesASecondChipThroughTheLeverBetweenThem() {
        // A repeater whose output lever is the transmitter's input pin: the whole of a chip chain,
        // and the build that was reported broken. Nothing but the repeater's own write tells the
        // transmitter anything, because the server raises no event for it.
        org.bukkit.block.BlockFace facing = BlockFace.SOUTH;
        Vec3i toInput = offsetTo(PinLayout.SISO, 0, facing);
        Vec3i toOutput = offsetTo(PinLayout.SISO, PinLayout.SISO.outputPin(0), facing);

        Block repeater = game.signAt(0, 64, 0, facing, "", "[MC1000]", "", "");

        // The transmitter sits so that its input pin is exactly the repeater's output pin.
        Vec3i shift = toOutput.subtract(toInput);
        Block transmitter = game.signAt(
                shift.x(), 64 + shift.y(), shift.z(), facing, "", "[MC1110]", "chained", "");

        Block shared = Positions.toBlock(
                game.world(), Positions.toDomain(repeater).add(toOutput));
        leverAt(shared.getX(), shared.getY(), shared.getZ());

        Block driving = Positions.toBlock(
                game.world(), Positions.toDomain(repeater).add(toInput));
        Block lever = leverAt(driving.getX(), driving.getY(), driving.getZ());

        game.manager().load(repeater);
        game.manager().load(transmitter);

        Switch on = (Switch) lever.getBlockData();
        on.setPowered(true);
        lever.setBlockData(on);
        game.manager().triggerAt(lever);

        assertThat(isOn(shared)).as("the repeater drove its own output").isTrue();
        assertThat(game.services().radio().isPowered(Band.parse("", "chained").orElseThrow()))
                .as("and the transmitter heard about it")
                .isTrue();
    }

    @Test
    void carriesAClockThroughATransmitterOntoItsBand() {
        // The reported build: a clock driving a transmitter, and a receiver following the band.
        Block transmitter = game.signAt(0, 64, 0, BlockFace.SOUTH, "", "[MC1110]", "testband", "");
        game.manager().load(transmitter);

        Block input = pin(transmitter, BlockFace.SOUTH, PinLayout.SISO, true);
        Block lever = leverAt(input.getX(), input.getY(), input.getZ());

        Switch on = (Switch) lever.getBlockData();
        on.setPowered(true);
        lever.setBlockData(on);

        game.manager().triggerAt(lever);

        assertThat(game.services().radio().isPowered(Band.parse("", "testband").orElseThrow()))
                .isTrue();
    }

    @Test
    void letsAReceiverFollowTheBandItsTransmitterDrives() {
        game.services().radio().transmit(Band.parse("", "testband").orElseThrow(), true);

        Block receiver = game.signAt(0, 64, 0, BlockFace.SOUTH, "", "[MC0111]", "testband", "");
        game.manager().load(receiver);

        Block output = pin(receiver, BlockFace.SOUTH, PinLayout.SISO, false);
        Block lever = leverAt(output.getX(), output.getY(), output.getZ());

        game.manager().at(receiver).orElseThrow().tick();

        assertThat(isOn(lever)).isTrue();
    }

    @Test
    void startsTheReceiverTicking() {
        // MC0111 is the self-triggering number, so the chip must be ticking without an S suffix.
        Block receiver = game.signAt(0, 64, 0, BlockFace.SOUTH, "", "[MC0111]", "testband", "");
        game.manager().load(receiver);

        assertThat(game.manager().at(receiver).orElseThrow().isSelfTriggering()).isTrue();
    }
}
