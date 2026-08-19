package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.platform.TimeSource;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.paper.stock.NearbyStockpiles;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Powerable;
import org.jspecify.annotations.NullMarked;

/**
 * A chip's view of the blocks around its sign.
 *
 * <p>This is the binding between the platform-independent chip logic and the world. Pin positions
 * come from the {@link PinLayout}, resolved against the sign's own position and facing; reading a
 * pin reads the block there, and driving an output flips the lever there.
 *
 * <p>Reads go to the world each time rather than being captured up front, because a chip may
 * drive an output and then read it back within a single run.
 *
 * <p>An instance belongs to one region and must only be used from the thread that owns it.
 */
@NullMarked
public final class BlockChipState implements ChipState {

    private final World world;
    private final Vec3i signPosition;
    private final BlockFace front;
    private final PinLayout layout;
    private final ICMode mode;
    private final int triggeredInput;
    private final Scheduler scheduler;
    private final TimeSource time;
    private final ChipWorld chipWorld;
    private final ChipServices services;
    private @org.jspecify.annotations.Nullable Stockpile stockpile;

    private BlockChipState(Builder builder) {
        this.world = builder.world;
        this.signPosition = builder.signPosition;
        this.front = builder.front;
        this.layout = builder.layout;
        this.mode = builder.mode;
        this.triggeredInput = builder.triggeredInput;
        this.scheduler = builder.scheduler;
        this.time = builder.time == null ? new WorldTime(builder.world) : builder.time;
        this.chipWorld = new BukkitChipWorld(builder.world);
        this.services = builder.services;
    }

    /** Reads the clocks a chip in a world cares about. */
    private record WorldTime(World world) implements TimeSource {

        @Override
        public long worldTicks() {
            return world.getFullTime();
        }

        @Override
        public long timeOfDay() {
            return world.getTime();
        }

        @Override
        public long unixSeconds() {
            return System.currentTimeMillis() / 1000L;
        }
    }

    /**
     * Starts building a chip state.
     *
     * @param world the world the chip is in
     * @param signPosition the position of the chip's sign
     * @param front the direction the sign's text faces
     * @param layout the chip's pin layout
     */
    public static Builder at(World world, Vec3i signPosition, BlockFace front, PinLayout layout) {
        return new Builder(world, signPosition, front, layout);
    }

    /** The pin layout this chip is wired for. */
    public PinLayout layout() {
        return layout;
    }

    /** The block the chip's sign occupies. */
    public Block signBlock() {
        return Positions.toBlock(world, signPosition);
    }

    /**
     * The block the sign hangs on, which is where a chip acts from.
     *
     * <p>Directly behind the sign, one step against its facing.
     */
    public Block backingBlock() {
        return Positions.toBlock(world, signPosition.offset(front.opposite()));
    }

    /** The block at a pin, resolved through the layout and any pin permutation. */
    public Block pinBlock(int pin) {
        return Positions.toBlock(world, layout.pinPosition(mode.slotFor(pin), signPosition, front));
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
        return Redstone.isPowered(pinBlock(checkInput(index)));
    }

    @Override
    public int inputPower(int index) {
        return Redstone.powerLevel(pinBlock(checkInput(index)));
    }

    @Override
    public boolean isConnected(int index) {
        return Redstone.isPowerSource(pinBlock(checkInput(index)));
    }

    @Override
    public boolean hasPowerSourceBehind() {
        return Redstone.isAlwaysOn(backingBlock());
    }

    @Override
    public boolean output(int index) {
        Block block = pinBlock(layout.outputPin(index));
        return block.getBlockData() instanceof Powerable powerable && powerable.isPowered();
    }

    @Override
    public void setOutput(int index, boolean value) {
        boolean powered = mode.invertsOutputs() != value;
        Block block = pinBlock(layout.outputPin(index));

        // A chip drives its outputs through levers. Anything else on the pin is something the
        // builder put there for their own reasons and is left alone.
        if (block.getType() != Material.LEVER) {
            return;
        }
        if (!(block.getBlockData() instanceof Powerable lever) || lever.isPowered() == powered) {
            return;
        }

        lever.setPowered(powered);
        block.setBlockData(lever, true);
    }

    @Override
    public int triggeredInput() {
        return triggeredInput;
    }

    @Override
    public SignLines sign() {
        return Signs.at(signBlock()).map(Signs::read).orElse(SignLines.EMPTY);
    }

    @Override
    public void setSignLine(int index, String text) {
        Signs.at(signBlock()).ifPresent(sign -> Signs.writeLine(sign, index, text));
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
    public Vec3i signPosition() {
        return signPosition;
    }

    @Override
    public BlockFace facing() {
        return front;
    }

    /** The chip's sign, if it is still there. */
    public java.util.Optional<Sign> signState() {
        return Signs.at(signBlock());
    }

    private int checkInput(int index) {
        if (index < 0 || index >= layout.inputCount()) {
            throw new IndexOutOfBoundsException(
                    "Input " + index + " is outside layout " + layout.code());
        }
        return index;
    }

    /** Assembles a {@link BlockChipState}. */
    public static final class Builder {

        private final World world;
        private final Vec3i signPosition;
        private final BlockFace front;
        private final PinLayout layout;
        private ICMode mode = ICMode.NONE;
        private int triggeredInput = -1;
        private Scheduler scheduler = RejectingScheduler.INSTANCE;
        private ChipServices services = ChipServices.create();
        private @org.jspecify.annotations.Nullable TimeSource time;

        private Builder(World world, Vec3i signPosition, BlockFace front, PinLayout layout) {
            this.world = world;
            this.signPosition = signPosition;
            this.front = front;
            this.layout = layout;
        }

        /** Sets the mode written on the sign. */
        public Builder mode(ICMode mode) {
            this.mode = mode;
            return this;
        }

        /** Sets the scheduler a chip uses to act after a delay. */
        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /** Sets the registries this chip shares with every other chip on the server. */
        public Builder services(ChipServices services) {
            this.services = services;
            return this;
        }

        /** Overrides the clock this chip reads, which otherwise comes from its world. */
        public Builder time(TimeSource time) {
            this.time = time;
            return this;
        }

        /** Records which input caused this run, or {@code -1} for a tick. */
        public Builder triggeredInput(int index) {
            this.triggeredInput = index;
            return this;
        }

        public BlockChipState build() {
            return new BlockChipState(this);
        }
    }

    /**
     * Stands in when a chip state is built without a scheduler.
     *
     * <p>Refusing loudly beats handing back a scheduler that silently drops work, which would
     * make a chip that acts after a delay simply never act.
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
            return runLater(task, delayTicks);
        }
    }
}
