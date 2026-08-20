package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.farm.Plantables;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.ChipWorld;
import com.xeonproductions.craftbookultimate.core.world.Placement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that plant what has been dropped near them.
 *
 * <p>Both take seeds and saplings lying on the ground and put them in the earth, one at a time,
 * consuming the item as they go. A farm is therefore fed by throwing a stack at it or by piping
 * one in, and the chip does the sowing.
 *
 * <p>Neither decides for itself what will grow where. It asks the world whether the plant could
 * legally sit in the position, so a seed goes on farmland, a sapling on soil and cocoa on the
 * side of a jungle log without any of that being written down here.
 *
 * <p>Line 3 names the item to plant, so a chip fed a mixed pile only picks out what it was told
 * to. Line 4 says where to plant it.
 */
@NullMarked
public final class Farming {

    /** The line naming the item this chip plants. */
    private static final int ITEM_LINE = 2;

    /** The line giving the area or the height to plant at. */
    private static final int AREA_LINE = 3;

    /** How far a single planter looks for something to plant. */
    private static final int PLANTER_REACH = 5;

    /** How far past its own area an area planter reaches for items. */
    private static final int AREA_PLANTER_EXTRA_REACH = 3;

    /** How long a ticking area planter waits between passes over its field. */
    private static final int AREA_PLANTER_INTERVAL_TICKS = 20;

    private Farming() {}

    /**
     * Plants one thing in one place.
     *
     * <p>Line 4 gives how far above the block the sign hangs on to plant, which defaults to one
     * and is never less, so the chip always works above its own support rather than inside it.
     *
     * <p>Ticking, it plants whenever something suitable is lying nearby. Not ticking, it plants
     * when its input is driven. The output reports whether anything was planted.
     */
    public static SelfTriggeringICLogic planter() {
        return new Planter();
    }

    /**
     * Plants across a field rather than in one place.
     *
     * <p>Line 4 reads {@code width:length} with an optional {@code :height}, which is how far
     * above the block the sign hangs on the field sits and defaults to one. The field runs away
     * from the sign, centred on its column.
     *
     * <p>Ticking, its input is a brake: it sows while nothing drives it and stops while something
     * does. Not ticking, it sows on the pulse instead. A ticking one sows once a second rather
     * than every tick, since a field does not need the attention.
     */
    public static SelfTriggeringICLogic areaPlanter() {
        return new AreaPlanter();
    }

    /**
     * Puts one item in the ground.
     *
     * @param world the world to plant in
     * @param position where the plant should end up
     * @param item the stack to take one from
     * @return true if something was planted and one was taken
     */
    private static boolean plant(ChipWorld world, Vec3i position, DroppedItem item) {
        Optional<Key> planted = Plantables.plantedForm(item.type());
        if (planted.isEmpty() || !world.isAir(position)) {
            return false;
        }

        Placement how = placementFor(world, position, planted.get());
        if (!world.canPlace(position, planted.get(), how)) {
            return false;
        }
        if (!world.setBlockAt(position, planted.get(), how)) {
            return false;
        }
        return item.takeOne();
    }

    /**
     * How a plant should be put down.
     *
     * <p>Cocoa hangs off the side of a jungle log rather than standing on the ground, so the one
     * facing the world will accept is looked for. Everything else goes down the ordinary way.
     */
    private static Placement placementFor(ChipWorld world, Vec3i position, Key planted) {
        if (!Plantables.attachesSideways(planted)) {
            return Placement.NORMAL;
        }

        for (BlockFace face : BlockFace.horizontals()) {
            Placement against = Placement.facing(face);
            if (world.canPlace(position, planted, against)) {
                return against;
            }
        }
        return Placement.NORMAL;
    }

    /** The item a chip's sign says to plant, if it names one that can be planted. */
    private static Optional<Key> plantableOn(ChipState state) {
        return state.world()
                .resolveItem(state.sign().trimmedText(ITEM_LINE))
                .filter(Plantables::isPlantable);
    }

    /** The stacks lying near a place that a chip would plant. */
    private static List<DroppedItem> wantedItemsNear(ChipState state, Vec3i centre, int radius, Key wanted) {
        List<DroppedItem> usable = new ArrayList<>();
        for (DroppedItem item : state.world().itemsNear(centre, radius)) {
            if (item.isPresent() && item.type().equals(wanted)) {
                usable.add(item);
            }
        }
        return usable;
    }

    /** Plants one thing directly above the block the sign hangs on. */
    private static final class Planter implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                sow(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            sow(state);
        }

        private static void sow(ChipState state) {
            Optional<Key> wanted = plantableOn(state);
            if (wanted.isEmpty()) {
                return;
            }

            Vec3i target = state.backPosition().add(0, heightOn(state), 0);
            ChipWorld world = state.world();
            if (!world.isLoaded(target) || !world.isInBounds(target)) {
                return;
            }

            for (DroppedItem item : wantedItemsNear(state, target, PLANTER_REACH, wanted.get())) {
                if (plant(world, target, item)) {
                    state.setMainOutput(true);
                    return;
                }
            }
            state.setMainOutput(false);
        }

        /** How far above its support the chip plants, which is never less than one block. */
        private static int heightOn(ChipState state) {
            String line = state.sign().trimmedText(AREA_LINE);
            if (line.isEmpty()) {
                return 1;
            }
            try {
                return Math.max(1, Integer.parseInt(line));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
    }

    /** Plants across a field of ground running away from the sign. */
    private static final class AreaPlanter implements SelfTriggeringICLogic {

        /** How many ticks have passed since the last pass over the field. */
        private int ticksWaited;

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                sow(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            // Driving a ticking area planter stops it, which is how a build pauses sowing without
            // taking the chip down.
            if (state.isAnyInputActive()) {
                return;
            }
            if (++ticksWaited < AREA_PLANTER_INTERVAL_TICKS) {
                return;
            }
            ticksWaited = 0;
            sow(state);
        }

        private static void sow(ChipState state) {
            Optional<Key> wanted = plantableOn(state);
            Optional<Field> field = Field.on(state);
            if (wanted.isEmpty() || field.isEmpty()) {
                return;
            }

            int reach = Math.max(field.get().width(), field.get().length()) + AREA_PLANTER_EXTRA_REACH;
            Vec3i centre = state.signPosition().add(0, field.get().height(), 0);
            List<DroppedItem> supply = wantedItemsNear(state, centre, reach, wanted.get());

            // A field that is already full walks every plot without planting anything, so there is
            // no point starting when there is nothing to sow.
            if (supply.isEmpty()) {
                return;
            }

            ChipWorld world = state.world();
            boolean sowed = false;
            int next = 0;

            for (Vec3i plot : field.get().plots(state)) {
                if (!world.isLoaded(plot) || !world.isInBounds(plot) || !world.isAir(plot)) {
                    continue;
                }

                while (next < supply.size() && !supply.get(next).isPresent()) {
                    next++;
                }
                if (next >= supply.size()) {
                    break;
                }

                if (plant(world, plot, supply.get(next))) {
                    sowed = true;
                }
            }

            state.setMainOutput(sowed);
        }

        /**
         * The ground an area planter sows.
         *
         * @param width how far across, centred on the sign's column
         * @param length how far away from the sign
         * @param height how far above the block the sign hangs on the ground lies
         */
        private record Field(int width, int length, int height) {

            /**
             * Reads {@code width:length[:height]} off the sign.
             *
             * <p>A field larger than the settings allow is sown at the size they allow.
             */
            static Optional<Field> on(ChipState state) {
                String[] parts = state.sign().trimmedText(AREA_LINE).split(":");
                if (parts.length < 2) {
                    return Optional.empty();
                }

                try {
                    int width = Integer.parseInt(parts[0].trim());
                    int length = Integer.parseInt(parts[1].trim());
                    int height = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 1;

                    if (width < 1 || length < 1) {
                        return Optional.empty();
                    }

                    Settings settings = state.settings();
                    return Optional.of(new Field(
                            settings.limitPlanterWidth(width),
                            settings.limitPlanterWidth(length),
                            height));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }

            /**
             * Every plot of the field, nearest the sign first.
             *
             * <p>Unlike the building chips the field begins directly above the block the sign
             * hangs on rather than one further out, so the first row of a farm is right against
             * the sign that runs it.
             */
            List<Vec3i> plots(ChipState state) {
                BlockFace away = state.facing().opposite();
                BlockFace across = away.rotateClockwise();

                Vec3i origin = state.signPosition()
                        .offset(away)
                        .offset(across, -(width / 2))
                        .add(0, height, 0);

                List<Vec3i> positions = new ArrayList<>(width * length);
                for (int along = 0; along < length; along++) {
                    for (int side = 0; side < width; side++) {
                        positions.add(origin.offset(away, along).offset(across, side));
                    }
                }
                return positions;
            }
        }
    }
}
