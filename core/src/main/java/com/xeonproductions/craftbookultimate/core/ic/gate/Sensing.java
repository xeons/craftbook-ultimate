package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.DroppedItem;
import com.xeonproductions.craftbookultimate.core.entity.EntitySpec;
import com.xeonproductions.craftbookultimate.core.entity.ItemCriteria;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that report what is standing near them.
 *
 * <p>All of them drive their output and change nothing, so a sensor is safe to put anywhere. Every
 * one has a ticking variant under its own model number, which is how a sensor is used to watch a
 * doorway rather than to answer a question when it is asked one.
 *
 * <p>Line 3 says what to look for and line 4 says where to look. What "where" means differs: the
 * two above-and-below sensors take a column measured from the first place somebody could stand,
 * the near sensors take a plain distance, and the area sensor takes a box of its own.
 *
 * <p>Spectators and vanished players are never sensed. Somebody walking through walls or
 * deliberately hidden should not set off a tripwire they pass.
 */
@NullMarked
public final class Sensing {

    /** The line saying what to look for. */
    private static final int SUBJECT_LINE = 2;

    /** The line saying where to look. */
    private static final int AREA_LINE = 3;

    /** Separates the fields of an area. */
    private static final String FIELD_SEPARATOR = ":";

    /** Separates an area from the offset to its middle. */
    private static final char OFFSET_SEPARATOR = '/';

    /** Separates the thing being ridden from its rider. */
    private static final char RIDER_SEPARATOR = '+';

    /** Written after the model reference to say the checks live in a book. */
    private static final char BOOK_MODE = 'B';

    /** Where a book-driven chip looks for its book when the sign does not say. */
    private static final Vec3i DEFAULT_BOOK_OFFSET = new Vec3i(0, 1, 0);

    /** How far across the two above-and-below sensors watch when their sign does not say. */
    private static final int DEFAULT_WATCH_RADIUS = 1;

    /** The furthest across they may watch. */
    private static final int MAX_WATCH_RADIUS = 5;

    /** How tall a column they watch when their sign does not say. */
    private static final int DEFAULT_WATCH_LENGTH = 1;

    /** The tallest column they may watch. */
    private static final int MAX_WATCH_LENGTH = 256;

    /** The furthest they may be shifted from where somebody would stand. */
    private static final int MAX_WATCH_OFFSET = 5;

    /** How far below the standing spot the below sensor starts looking. */
    private static final int BELOW_DROP = 2;

    /** How far the near sensors reach when their sign does not say. */
    private static final int DEFAULT_NEAR_RANGE = 5;

    /** The furthest the creature sensors may reach. */
    private static final int MAX_NEAR_RANGE = 64;

    /** The furthest the item sensors may reach. */
    private static final int MAX_ITEM_RANGE = 30;

    /** How far the area sensor reaches on each axis when its sign does not say. */
    private static final Vec3i DEFAULT_AREA = new Vec3i(3, 1, 3);

    /** Where the middle of that area sits when its sign does not say. */
    private static final Vec3i DEFAULT_AREA_OFFSET = new Vec3i(0, 1, 0);

    /** The furthest the area sensor may reach on any axis. */
    private static final int MAX_AREA_REACH = 16;

    /** How far the middle of the area may be moved, in either direction. */
    private static final int MIN_AREA_OFFSET = -16;

    private static final int MAX_AREA_OFFSET = 10;

    private Sensing() {}

    /**
     * Reports whether a creature is standing on the block above this one.
     *
     * <p>Line 3 says what counts, in the same spelling the spawner uses, and defaults to anything
     * alive. The watched block is the first place above the sign's support that something could
     * stand, so a sensor buried under a path watches the path.
     */
    public static SelfTriggeringICLogic mobAbove() {
        return sensor(Sensing::senseMobAbove);
    }

    /**
     * Reports whether a player is standing above this block.
     *
     * <p>Line 3 picks which players, as {@code p:Notch}, {@code g:admin} or {@code m:ott}, each of
     * which may be turned around with a {@code !}. Line 4 reads {@code radius[:height[:up]]}: how
     * far across to watch, how tall a column, and how far above the standing spot to start.
     */
    public static SelfTriggeringICLogic playerAbove() {
        return sensor(state -> sensePeopleInColumn(state, false));
    }

    /**
     * Reports whether a player is standing below this block.
     *
     * <p>The same lines as {@link #playerAbove()}, except that the column hangs beneath the
     * standing spot rather than sitting above it and the second number is how deep it runs.
     */
    public static SelfTriggeringICLogic playerBelow() {
        return sensor(state -> sensePeopleInColumn(state, true));
    }

    /**
     * Reports whether a player is within a distance.
     *
     * <p>Line 3 picks which players and line 4 is how far to reach, which defaults to five blocks
     * and cannot exceed sixty-four.
     */
    public static SelfTriggeringICLogic playerNear() {
        return sensor(state -> {
            EntitySpec wanted = subjectOn(state, EntitySpec.Person.ANY);
            double range =
                    boundedNumber(state.sign().trimmedText(AREA_LINE), 1, MAX_NEAR_RANGE, DEFAULT_NEAR_RANGE);

            List<Bystander> found =
                    state.world().bystandersNear(Vec3d.centreOf(state.backPosition()), range);
            state.setMainOutput(anyMatches(found, wanted));
        });
    }

    /**
     * Reports whether a creature is within a distance.
     *
     * <p>Line 3 says what counts and defaults to anything alive; line 4 is how far to reach.
     * Unlike the player sensors this one measures from the sign itself.
     */
    public static SelfTriggeringICLogic mobNear() {
        return sensor(state -> {
            EntitySpec wanted = subjectOn(state, new EntitySpec.Category(EntitySpec.Group.CREATURES));
            double range =
                    boundedNumber(state.sign().trimmedText(AREA_LINE), 1, MAX_NEAR_RANGE, DEFAULT_NEAR_RANGE);

            List<Bystander> found =
                    state.world().bystandersNear(Vec3d.middleOf(state.signPosition()), range);
            state.setMainOutput(anyMatches(found, wanted));
        });
    }

    /**
     * Reports whether a matching stack is lying nearby.
     *
     * <p>Line 3 is one thing to check, written as {@code ID:35@14}, {@code STACK:64},
     * {@code NAME:Key} or {@code LORE:quest}. Line 4 is how far to reach, up to thirty blocks.
     *
     * <p>Writing {@code B} after the model reference reads the checks out of a book instead, one
     * per line, which is how a sensor asks for more than one thing at once. Line 3 then gives the
     * offset to the container holding the book, and defaults to the block above.
     */
    public static SelfTriggeringICLogic itemNear() {
        return sensor(state -> {
            Optional<ItemCriteria> wanted = criteriaOn(state);
            if (wanted.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            int range = (int) boundedNumber(
                    state.sign().trimmedText(AREA_LINE), 1, MAX_ITEM_RANGE, DEFAULT_NEAR_RANGE);

            for (DroppedItem item : state.world().itemsNear(state.backPosition(), range)) {
                if (item.isPresent() && wanted.get().matches(item.stack())) {
                    state.setMainOutput(true);
                    return;
                }
            }
            state.setMainOutput(false);
        });
    }

    /**
     * Reports whether a nearby player is holding a matching item.
     *
     * <p>The same checks as {@link #itemNear()} on line 3, and the same reach on line 4, but read
     * from what people are holding rather than from what is lying on the ground.
     */
    public static SelfTriggeringICLogic heldItemNear() {
        return sensor(state -> {
            Optional<ItemCriteria> wanted = ItemCriteria.parse(
                    state.sign().trimmedText(SUBJECT_LINE), state.world()::resolveItem);
            if (wanted.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            double range =
                    boundedNumber(state.sign().trimmedText(AREA_LINE), 1, MAX_ITEM_RANGE, DEFAULT_NEAR_RANGE);

            for (Bystander bystander :
                    state.world().bystandersNear(Vec3d.centreOf(state.backPosition()), range)) {
                if (!bystander.isPlayer() || !bystander.isVisible()) {
                    continue;
                }
                if (bystander.heldItem().filter(wanted.get()::matches).isPresent()) {
                    state.setMainOutput(true);
                    return;
                }
            }
            state.setMainOutput(false);
        });
    }

    /**
     * Reports whether something is inside a box.
     *
     * <p>Line 3 names what to look for and may name a rider after a {@code +}, so
     * {@code pig+player} finds somebody riding a pig. Line 4 reads
     * {@code width:height:length[/x:y:z]}, where the three numbers are how far the box reaches
     * from its middle on each axis and the offset moves that middle away from the sign.
     */
    public static SelfTriggeringICLogic inArea() {
        return sensor(state -> {
            Optional<EntitySpec> wanted = areaSubjectOn(state);
            if (wanted.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            Area area = Area.on(state);
            Vec3d middle = Vec3d.middleOf(state.signPosition()).add(Vec3d.of(area.offset()));
            Vec3d reach = Vec3d.of(area.reach());

            List<Bystander> found = state.world().bystandersIn(middle.subtract(reach), middle.add(reach));
            state.setMainOutput(anyMatches(found, wanted.get()));
        });
    }

    /**
     * Reports whether a player is logged in.
     *
     * <p>Line 3 is the name to look for, and matches anybody whose name contains it, so a short
     * fragment covers a family of accounts. Players who have hidden themselves are not reported.
     */
    public static SelfTriggeringICLogic playerOnline() {
        return sensor(state -> {
            String wanted = state.sign().trimmedText(SUBJECT_LINE);
            state.setMainOutput(!wanted.isEmpty() && state.services().roster().anyNameContains(wanted));
        });
    }

    /** Reports whether a creature is in the block above the sign's support. */
    private static void senseMobAbove(ChipState state) {
        EntitySpec wanted = subjectOn(state, new EntitySpec.Category(EntitySpec.Group.CREATURES));
        Optional<Vec3i> spot = state.world().firstPassableAtOrAbove(state.backPosition());
        if (spot.isEmpty()) {
            state.setMainOutput(false);
            return;
        }

        Vec3i at = spot.get();
        List<Bystander> found = state.world().bystandersIn(
                new Vec3d(at.x() - 1, at.y(), at.z() - 1),
                new Vec3d(at.x() + 1, at.y() + 1, at.z() + 1));
        state.setMainOutput(anyMatches(found, wanted));
    }

    /**
     * Reports whether a player is in a column above or below where somebody would stand.
     *
     * @param below whether the column hangs beneath that spot rather than sitting above it
     */
    private static void sensePeopleInColumn(ChipState state, boolean below) {
        EntitySpec wanted = subjectOn(state, EntitySpec.Person.ANY);
        Optional<Vec3i> spot = state.world().firstStandingSpotAtOrAbove(state.backPosition());
        if (spot.isEmpty()) {
            state.setMainOutput(false);
            return;
        }

        Column column = Column.on(state);
        Vec3i at = spot.get();
        double floor = below
                ? at.y() - BELOW_DROP - column.offset() - column.length()
                : at.y() + column.offset();
        double ceiling = below
                ? at.y() - BELOW_DROP - column.offset()
                : at.y() + column.offset() + column.length();

        List<Bystander> found = state.world().bystandersIn(
                new Vec3d(at.x() - column.radius(), floor, at.z() - column.radius()),
                new Vec3d(at.x() + column.radius(), ceiling, at.z() + column.radius()));
        state.setMainOutput(anyMatches(found, wanted));
    }

    /**
     * Wraps a reading into a chip that takes it while driven, or on every tick.
     *
     * <p>A sensor nothing is driving keeps whatever it last reported rather than going low, which
     * is how a build reads an answer once and then holds it.
     */
    private static SelfTriggeringICLogic sensor(Consumer<ChipState> reading) {
        return new Sensor(reading);
    }

    /** One sensor, reading while driven or on every tick. */
    private record Sensor(Consumer<ChipState> reading) implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                reading.accept(state);
            }
        }

        @Override
        public void tick(ChipState state) {
            reading.accept(state);
        }
    }

    /** Whether anything found is what was asked for, ignoring whatever is not really there. */
    private static boolean anyMatches(List<Bystander> found, EntitySpec wanted) {
        for (Bystander bystander : found) {
            if (bystander.isVisible() && wanted.matches(bystander)) {
                return true;
            }
        }
        return false;
    }

    /** What a chip's sign says to look for, or the chip's own default when the line is blank. */
    private static EntitySpec subjectOn(ChipState state, EntitySpec fallback) {
        String written = state.sign().trimmedText(SUBJECT_LINE);
        if (written.isEmpty()) {
            return fallback;
        }
        return EntitySpec.parse(written, state.world()::resolveItem).orElse(fallback);
    }

    /**
     * What the area sensor is looking for.
     *
     * <p>Its third line names one thing and optionally its rider, which is a narrower spelling
     * than the stacking grammar the spawner uses and predates it.
     */
    private static Optional<EntitySpec> areaSubjectOn(ChipState state) {
        String written = state.sign().trimmedText(SUBJECT_LINE);
        int separator = written.indexOf(RIDER_SEPARATOR);
        if (separator < 0) {
            return EntitySpec.parseOne(written, state.world()::resolveItem);
        }

        Optional<EntitySpec> ridden =
                EntitySpec.parseOne(written.substring(0, separator), state.world()::resolveItem);
        Optional<EntitySpec> rider =
                EntitySpec.parseOne(written.substring(separator + 1), state.world()::resolveItem);
        if (ridden.isEmpty() || rider.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EntitySpec.Mounted(ridden.get(), List.of(rider.get())));
    }

    /** What the item sensor is looking for, from its sign or from a book. */
    private static Optional<ItemCriteria> criteriaOn(ChipState state) {
        if (!readsFromBook(state)) {
            return ItemCriteria.parse(state.sign().trimmedText(SUBJECT_LINE), state.world()::resolveItem);
        }

        Vec3i where = state.backPosition()
                .add(tripleOn(state.sign().trimmedText(SUBJECT_LINE)).orElse(DEFAULT_BOOK_OFFSET));

        ItemCriteria wanted = ItemCriteria.ANY;
        for (String page : state.world().bookPagesAt(where)) {
            for (String line : page.split("\\R")) {
                if (!line.isBlank()) {
                    wanted = wanted.and(line, state.world()::resolveItem).orElse(wanted);
                }
            }
        }
        return wanted.isAny() ? Optional.empty() : Optional.of(wanted);
    }

    private static boolean readsFromBook(ChipState state) {
        String mode = state.modeText();
        return !mode.isEmpty() && mode.charAt(mode.length() - 1) == BOOK_MODE;
    }

    /**
     * The column the two above-and-below sensors watch.
     *
     * @param radius how far across it reaches from the standing spot
     * @param length how tall or how deep it runs
     * @param offset how far from the standing spot it starts
     */
    private record Column(int radius, int length, int offset) {

        static Column on(ChipState state) {
            String[] parts = state.sign().trimmedText(AREA_LINE).split(FIELD_SEPARATOR);
            return new Column(
                    (int) boundedNumber(field(parts, 0), 1, MAX_WATCH_RADIUS, DEFAULT_WATCH_RADIUS),
                    (int) boundedNumber(field(parts, 1), 0, MAX_WATCH_LENGTH, DEFAULT_WATCH_LENGTH),
                    (int) boundedNumber(field(parts, 2), 0, MAX_WATCH_OFFSET, 0));
        }

        private static String field(String[] parts, int index) {
            return index < parts.length ? parts[index] : "";
        }
    }

    /**
     * The box the area sensor watches.
     *
     * @param reach how far it reaches from its middle on each axis
     * @param offset how far its middle sits from the sign
     */
    private record Area(Vec3i reach, Vec3i offset) {

        static Area on(ChipState state) {
            String written = state.sign().trimmedText(AREA_LINE);
            int separator = written.indexOf(OFFSET_SEPARATOR);
            String sides = separator < 0 ? written : written.substring(0, separator);

            Vec3i reach = tripleOn(sides)
                    .map(read -> held(read, 1, MAX_AREA_REACH))
                    .orElse(DEFAULT_AREA);

            Vec3i offset = separator < 0
                    ? DEFAULT_AREA_OFFSET
                    : tripleOn(written.substring(separator + 1))
                            .map(read -> held(read, MIN_AREA_OFFSET, MAX_AREA_OFFSET))
                            .orElse(DEFAULT_AREA_OFFSET);

            return new Area(reach, offset);
        }

        private static Vec3i held(Vec3i read, int lowest, int highest) {
            return new Vec3i(
                    Math.clamp(read.x(), lowest, highest),
                    Math.clamp(read.y(), lowest, highest),
                    Math.clamp(read.z(), lowest, highest));
        }
    }

    /** Reads an {@code x:y:z} triple. */
    private static Optional<Vec3i> tripleOn(String written) {
        String[] parts = written.split(FIELD_SEPARATOR);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Vec3i(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** A number from a sign, held within bounds, falling back when the text is not one. */
    private static double boundedNumber(String written, int lowest, int highest, int fallback) {
        String trimmed = written.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        try {
            return Math.clamp(Integer.parseInt(trimmed), lowest, highest);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
