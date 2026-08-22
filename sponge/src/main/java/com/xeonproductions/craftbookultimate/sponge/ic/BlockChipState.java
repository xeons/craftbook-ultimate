// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.platform.TimeSource;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.sign.SignSupport;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.sponge.adapter.Signs;
import com.xeonproductions.craftbookultimate.sponge.stock.NearbyStockpiles;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * A chip's own state, read off the blocks around its sign.
 *
 * <p>Where the pins are is the domain's business — {@link PinLayout} works it out from the sign's
 * position and which way it faces — and this only reads and writes the blocks it is told about.
 */
@NullMarked
public final class BlockChipState implements ChipState {

    private final ServerWorld world;
    private final Vec3i signPosition;
    private final BlockFace front;
    private final PinLayout layout;
    private final ICMode mode;
    private final int triggeredInput;
    private final Scheduler scheduler;
    private final TimeSource time;
    private final ChipWorld chipWorld;
    private final ChipServices services;
    private @Nullable Stockpile stockpile;

    private BlockChipState(Builder builder) {
        this.world = builder.world;
        this.signPosition = builder.signPosition;
        this.front = builder.front;
        this.layout = builder.layout;
        this.mode = builder.mode;
        this.triggeredInput = builder.triggeredInput;
        this.scheduler = builder.scheduler;
        this.time = builder.time == null ? new WorldTime(builder.world) : builder.time;
        this.chipWorld = new SpongeChipWorld(builder.world);
        this.services = builder.services;
    }

    private record WorldTime(ServerWorld world) implements TimeSource {

        /** Ticks in a Minecraft day, which is what the day time has to be folded into. */
        private static final long TICKS_PER_DAY = 24_000L;

        @Override
        public long worldTicks() {
            return world.properties().gameTime().asTicks().ticks();
        }

        /**
         * Where in the day it is, from dawn.
         *
         * <p>The day time counts on past one day rather than wrapping, so it is folded — a chip
         * asking whether it is night wants a number between nought and a day, not the count since
         * the world was made.
         */
        @Override
        public long timeOfDay() {
            return Math.floorMod(world.properties().dayTime().asTicks().ticks(), TICKS_PER_DAY);
        }

        @Override
        public long unixSeconds() {
            return System.currentTimeMillis() / 1000L;
        }
    }

    public static Builder at(
            ServerWorld world, Vec3i signPosition, BlockFace front, PinLayout layout) {
        return new Builder(world, signPosition, front, layout);
    }

    public PinLayout layout() {
        return layout;
    }

    public Vec3i backPosition() {
        return SignSupport.of(signPosition, front);
    }

    public Vec3i pinPosition(int pin) {
        return layout.pinPosition(mode.slotFor(pin), signPosition, front);
    }

    private BlockState blockAt(Vec3i position) {
        return world.block(position.x(), position.y(), position.z());
    }

    @Override
    public int inputCount() {
        return layout.inputCount();
    }

    @Override
    public int outputCount() {
        return layout.outputCount();
    }

    @Override
    public boolean input(int index) {
        return Redstone.isPowered(world, pinPosition(checkInput(index)));
    }

    @Override
    public int inputPower(int index) {
        return Redstone.powerLevel(blockAt(pinPosition(checkInput(index))));
    }

    @Override
    public boolean isConnected(int index) {
        return Redstone.isPowerSource(blockAt(pinPosition(checkInput(index))));
    }

    @Override
    public boolean hasPowerSourceBehind() {
        return Redstone.isAlwaysOn(blockAt(backPosition()));
    }

    @Override
    public boolean output(int index) {
        return blockAt(pinPosition(layout.outputPin(index)))
                .get(Keys.IS_POWERED)
                .orElse(false);
    }

    /**
     * Drives an output.
     *
     * <p>A chip drives its outputs through levers. Anything else on the pin is something the builder
     * put there for their own reasons and is left alone.
     */
    @Override
    public void setOutput(int index, boolean value) {
        boolean powered = mode.invertsOutputs() != value;
        Vec3i position = pinPosition(layout.outputPin(index));
        BlockState lever = blockAt(position);

        if (!lever.type().equals(BlockTypes.LEVER.get())) {
            return;
        }
        if (lever.get(Keys.IS_POWERED).orElse(false) == powered) {
            return;
        }

        lever.with(Keys.IS_POWERED, powered).ifPresent(driven ->
                world.setBlock(position.x(), position.y(), position.z(), driven));
    }

    @Override
    public int triggeredInput() {
        return triggeredInput;
    }

    @Override
    public SignLines sign() {
        return signState().map(Signs::read).orElse(SignLines.EMPTY);
    }

    @Override
    public void setSignLine(int index, String text) {
        signState().ifPresent(sign -> Signs.writeLine(sign, index, text));
    }

    @Override
    public ICMode mode() {
        return mode;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public TimeSource time() {
        return time;
    }

    @Override
    public ChipWorld world() {
        return chipWorld;
    }

    @Override
    public ChipServices services() {
        return services;
    }

    @Override
    public Stockpile stockpile() {
        // Scanning for containers walks a cube of blocks, so it is put off until a chip actually
        // asks, and then kept for the rest of this run.
        if (stockpile == null) {
            stockpile = NearbyStockpiles.around(world, backPosition());
        }
        return stockpile;
    }

    @Override
    public Stockpile stockpileNear(Vec3i centre, int radius, Set<Key> kinds) {
        return NearbyStockpiles.around(world, centre, radius, kinds);
    }

    @Override
    public Vec3i signPosition() {
        return signPosition;
    }

    @Override
    public BlockFace facing() {
        return front;
    }

    public Optional<Sign> signState() {
        return Signs.at(world, signPosition);
    }

    private int checkInput(int index) {
        if (index < 0 || index >= layout.inputCount()) {
            throw new IndexOutOfBoundsException(
                    "Input " + index + " is outside layout " + layout.code());
        }
        return index;
    }

    public static final class Builder {

        private final ServerWorld world;
        private final Vec3i signPosition;
        private final BlockFace front;
        private final PinLayout layout;
        private ICMode mode = ICMode.NONE;
        private int triggeredInput = -1;
        private Scheduler scheduler = RejectingScheduler.INSTANCE;
        private ChipServices services = ChipServices.create();
        private @Nullable TimeSource time;

        private Builder(ServerWorld world, Vec3i signPosition, BlockFace front, PinLayout layout) {
            this.world = world;
            this.signPosition = signPosition;
            this.front = front;
            this.layout = layout;
        }

        public Builder mode(ICMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder services(ChipServices services) {
            this.services = services;
            return this;
        }

        public Builder time(TimeSource time) {
            this.time = time;
            return this;
        }

        public Builder triggeredInput(int index) {
            this.triggeredInput = index;
            return this;
        }

        public BlockChipState build() {
            return new BlockChipState(this);
        }
    }

    /**
     * What a chip gets when nobody gave it a scheduler.
     *
     * <p>Throwing here rather than quietly doing nothing, because a chip that wanted to run later
     * and silently did not is indistinguishable from a chip that is wired wrong.
     */
    private enum RejectingScheduler implements Scheduler {
        INSTANCE;

        @Override
        public Task runLater(Runnable task, long delayTicks) {
            throw new IllegalStateException(
                    "This chip needs a scheduler; build its state with scheduler(...)");
        }

        @Override
        public Task runRepeating(Runnable task, long delayTicks, long periodTicks) {
            throw new IllegalStateException(
                    "This chip needs a scheduler; build its state with scheduler(...)");
        }
    }
}
