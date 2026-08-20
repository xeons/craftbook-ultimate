package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.platform.ManualScheduler;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A cart arriving at a mechanism, put together by hand.
 *
 * <p>Everything a mechanic reads is set here, so a test says what the world looks like rather than
 * building one. The scheduler is a manual one, so a test can step past a delay rather than wait
 * for it.
 */
@NullMarked
public final class SimpleCartVisit implements CartVisit {

    private final Cart cart;
    private final CartMechanism mechanism;
    private final SimpleCartWorld world;
    private final Stations stations;
    private final ManualScheduler scheduler;
    private final Settings settings;
    private final Wiring wiring;
    private final boolean minor;
    private final Vec3d from;

    private SimpleCartVisit(Builder builder) {
        this.cart = builder.cart;
        this.mechanism = builder.mechanism;
        this.world = builder.world;
        this.stations = builder.stations;
        this.scheduler = builder.scheduler;
        this.settings = builder.settings;
        this.wiring = builder.wiring;
        this.minor = builder.minor;
        this.from = builder.from != null ? builder.from : Vec3d.centreOf(builder.mechanism.rail());
    }

    /**
     * A visit to a mechanism made of a base block and a sign under it.
     *
     * <p>Lays the rail, puts the block under it and hangs the sign two below, which is the
     * commonest way of building one, and points the cart at the middle of the rail.
     *
     * @param baseBlock the block under the rail
     * @param facing the way the sign looks
     * @param lines what the sign says
     */
    public static Builder at(Vec3i rail, String baseBlock, BlockFace facing, String... lines) {
        Vec3i base = rail.offset(BlockFace.DOWN);
        Vec3i signPosition = base.offset(BlockFace.DOWN);
        CartMechanism.MechanismSign sign =
                new CartMechanism.MechanismSign(signPosition, SignLines.of(lines), facing);

        SimpleCartWorld world = new SimpleCartWorld()
                .withRail(rail, RailShape.NORTH_SOUTH)
                .withBlock(base, baseBlock);
        world.withSign(signPosition, facing, lines);

        return new Builder(
                CartMechanism.signed(
                        rail,
                        base,
                        Blocks.key(baseBlock),
                        sign),
                world);
    }

    /** A visit to a mechanism that is a block and a rail and nothing else. */
    public static Builder at(Vec3i rail, String baseBlock) {
        Vec3i base = rail.offset(BlockFace.DOWN);
        SimpleCartWorld world = new SimpleCartWorld()
                .withRail(rail, RailShape.NORTH_SOUTH)
                .withBlock(base, baseBlock);

        return new Builder(
                CartMechanism.unsigned(
                        rail,
                        base,
                        Blocks.key(baseBlock)),
                world);
    }

    @Override
    public Cart cart() {
        return cart;
    }

    @Override
    public CartMechanism mechanism() {
        return mechanism;
    }

    @Override
    public boolean isMinor() {
        return minor;
    }

    @Override
    public Vec3d from() {
        return from;
    }

    @Override
    public Wiring wiring() {
        return wiring;
    }

    @Override
    public CartWorld world() {
        return world;
    }

    @Override
    public Stations stations() {
        return stations;
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    /** The world this visit happens in, for a test to look at afterwards. */
    public SimpleCartWorld simpleWorld() {
        return world;
    }

    /** The scheduler this visit uses, so a test can step past a delay. */
    public ManualScheduler manualScheduler() {
        return scheduler;
    }

    /** Assembles a visit. */
    public static final class Builder {

        private final CartMechanism mechanism;
        private final SimpleCartWorld world;
        private Cart cart = SimpleCart.rideable();
        private Stations stations = new Stations();
        private final ManualScheduler scheduler = new ManualScheduler();
        private Settings settings = Settings.DEFAULTS;
        private Wiring wiring = Wiring.NONE;
        private boolean minor;
        private Vec3d from;

        private Builder(CartMechanism mechanism, SimpleCartWorld world) {
            this.mechanism = mechanism;
            this.world = world;
            this.from = Vec3d.centreOf(mechanism.rail());
        }

        /** The cart that has arrived. */
        public Builder cart(Cart cart) {
            this.cart = cart;
            if (cart instanceof SimpleCart simple) {
                simple.at(Vec3d.centreOf(mechanism.rail()));
            }
            return this;
        }

        /** Where every rider has said they are going. */
        public Builder stations(Stations stations) {
            this.stations = stations;
            return this;
        }

        /** The settings in force. */
        public Builder settings(Settings settings) {
            this.settings = settings;
            return this;
        }

        /** What has been wired to the mechanism. */
        public Builder wiring(Wiring wiring) {
            this.wiring = wiring;
            return this;
        }

        /** Makes this a move within a single block, which most mechanics ignore. */
        public Builder minor() {
            this.minor = true;
            return this;
        }

        /** Where the cart was before this move. */
        public Builder from(Vec3d from) {
            this.from = from;
            return this;
        }

        /** Puts the cart's arrival off the middle of the rail, as a cart still rolling is. */
        public Builder stillRolling() {
            this.from = Vec3d.centreOf(mechanism.rail()).add(new Vec3d(0.9, 0, 0));
            return this;
        }

        /** Changes the world before the visit, to put chests, rails or signs in it. */
        public Builder world(Consumer<SimpleCartWorld> change) {
            change.accept(world);
            return this;
        }

        /** Puts a block somewhere in the world. */
        public Builder withBlock(Vec3i position, Key block) {
            world.withBlock(position, block);
            return this;
        }

        public SimpleCartVisit build() {
            return new SimpleCartVisit(this);
        }
    }

    /** Where the sign of a mechanism built by {@link #at(Vec3i, String, BlockFace, String...)} is. */
    public Optional<Vec3i> signPosition() {
        return mechanism.sign().map(CartMechanism.MechanismSign::position);
    }
}
