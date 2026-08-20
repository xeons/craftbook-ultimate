package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A curtain of fence that drops from a lintel and winds back up into it.
 *
 * <p>Unlike a bridge or a door the gate has no second sign and no measured box. It looks around
 * itself for its own material, follows every run of it that is joined up — sideways, and a step
 * up or down so that an arch stays one gate — and drops each column of that run to the ground.
 * Opening it winds every column back up until only the row hanging from the lintel is left.
 *
 * <p>Six materials, each with its own sign so that two gates standing beside each other in
 * different materials do not catch one another: the plain sign takes anything a gate may be made
 * of, and the glass, iron and nether signs take only their own. Each has a {@code D} form that
 * looks barely past its own sign, for a gate with neighbours, and each may be followed by a
 * {@code C} for a gate that answers to a hand on the fence as well as on the sign.
 */
@NullMarked
public final class Gate implements SignMechanic {

    /** What a builder adds to the name for a gate that answers to a hand on its own material. */
    public static final char CLICKABLE = 'C';

    /** How far a {@code D} gate looks to either side of its sign. */
    private static final int SMALL_RADIUS = 1;

    /** How far below its sign a {@code D} gate looks. */
    private static final int SMALL_DROP = 2;

    /** The signs a gate may carry, before the clickable form of each is added. */
    private static final List<String> PLAIN = List.of(
            "[Gate]", "[DGate]",
            "[GlassGate]", "[GlassDGate]",
            "[IronGate]", "[IronDGate]",
            "[NetherGate]", "[NetherDGate]");

    private static final List<String> NAMES = bothForms();

    /** The sides a run of gate carries on along, and the steps up and down it may take. */
    private static final int[][] NEIGHBOURS = neighbours();

    @Override
    public String name() {
        return "Gate";
    }

    @Override
    public List<String> signNames() {
        return NAMES;
    }

    @Override
    public boolean act(MechanicVisit visit) {
        Style style = styleOf(visit.sign());
        Settings settings = visit.settings();
        MechanicWorld world = visit.world();

        int radius = style.small() ? SMALL_RADIUS : settings.mechanics().gateRadius();
        int drop = style.small() ? SMALL_DROP : radius;
        Vec3i origin = visit.sign().position();
        Panel search = Panel.between(
                origin.add(-radius, -drop, -radius),
                origin.add(radius, radius * 2, radius));

        List<List<Vec3i>> gates = findGates(world, search, style, settings);
        if (gates.isEmpty()) {
            visit.complain("There is no gate near this sign.");
            return false;
        }

        Stockpile stockpile = visit.stockpile();
        for (List<Vec3i> columns : gates) {
            Key material = world.blockAt(columns.get(0));
            boolean shut = visit.askedToShut()
                    .orElseGet(() -> !world.blockAt(columns.get(0).offset(BlockFace.DOWN))
                            .equals(material));

            for (Vec3i hanging : columns) {
                if (!toggleColumn(visit, hanging, material, shut, stockpile)) {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Whether a hand on a block should work a gate hanging from this sign.
     *
     * <p>Only for the gates whose sign asks for it, and only for their own material, so that
     * clicking a fence somewhere near an ordinary gate does nothing.
     */
    public boolean answersToTouchOn(PostedSign sign, Key block, Settings settings) {
        if (!settings.mechanics().gateClicking() || !claims(sign.lines())) {
            return false;
        }
        Style style = styleOf(sign);
        return style.clickable() && isGate(block, style, settings);
    }

    /**
     * Winds one column down to the ground or back up into the lintel.
     *
     * @return true to carry on with the rest of the gate, false when it has run out of blocks or
     *     out of room to put them
     */
    private static boolean toggleColumn(
            MechanicVisit visit, Vec3i hanging, Key material, boolean shut, Stockpile stockpile) {
        MechanicWorld world = visit.world();
        Vec3i at = hanging.offset(BlockFace.DOWN);

        if (shut) {
            while (world.isInBounds(at) && world.isPassable(at)) {
                if (!stockpile.takeAll(material, 1)) {
                    visit.complain("There are not enough blocks nearby to close the gate.");
                    return false;
                }
                if (!world.setBlockAt(at, material)) {
                    stockpile.give(material, 1);
                    return true;
                }
                at = at.offset(BlockFace.DOWN);
            }
            return true;
        }

        while (world.isInBounds(at) && world.blockAt(at).equals(material)) {
            if (!stockpile.hasRoomFor(material, 1)) {
                visit.complain("There is nowhere to put the gate's blocks.");
                return false;
            }
            if (!world.clearAt(at)) {
                return true;
            }
            stockpile.give(material, 1);
            at = at.offset(BlockFace.DOWN);
        }
        return true;
    }

    /**
     * Every gate within reach of the sign, each as the list of blocks its columns hang from.
     *
     * <p>Two runs of fence that are not joined up are two gates and are worked out separately, so
     * one that is already down does not decide which way the other goes.
     */
    private static List<List<Vec3i>> findGates(
            MechanicWorld world, Panel search, Style style, Settings settings) {
        List<List<Vec3i>> gates = new ArrayList<>();
        Set<Vec3i> claimed = new HashSet<>();

        for (int y = search.max().y(); y >= search.min().y(); y--) {
            for (int x = search.min().x(); x <= search.max().x(); x++) {
                for (int z = search.min().z(); z <= search.max().z(); z++) {
                    Vec3i start = new Vec3i(x, y, z);
                    if (claimed.contains(start) || !isGate(world.blockAt(start), style, settings)) {
                        continue;
                    }
                    List<Vec3i> columns = columnsOf(
                            world, search, start, style, settings, claimed);
                    if (!columns.isEmpty()) {
                        gates.add(columns);
                    }
                }
            }
        }
        return gates;
    }

    /**
     * The columns of the run of gate a block belongs to.
     *
     * <p>The run is followed sideways and a step up or down, which is what keeps an arch and a
     * stepped gate in one piece, and never outside what the sign can reach. Each place the run
     * passes over contributes one column, hanging from the highest of its blocks.
     */
    private static List<Vec3i> columnsOf(
            MechanicWorld world,
            Panel search,
            Vec3i start,
            Style style,
            Settings settings,
            Set<Vec3i> claimed) {
        Map<Long, Vec3i> highest = new HashMap<>();
        Deque<Vec3i> pending = new ArrayDeque<>();
        pending.add(start);
        claimed.add(start);

        while (!pending.isEmpty()) {
            Vec3i at = pending.removeFirst();
            highest.merge(
                    columnKey(at), at, (one, other) -> one.y() >= other.y() ? one : other);

            for (int[] step : NEIGHBOURS) {
                Vec3i next = at.add(step[0], step[1], step[2]);
                if (claimed.contains(next)
                        || !search.contains(next)
                        || !isGate(world.blockAt(next), style, settings)) {
                    continue;
                }
                claimed.add(next);
                pending.add(next);
            }
        }

        List<Vec3i> columns = new ArrayList<>();
        for (Vec3i top : highest.values()) {
            // A gate hangs from something. A run of fence with open sky above it is a fence.
            if (!world.isAir(top.offset(BlockFace.UP))) {
                columns.add(top);
            }
        }
        columns.sort((one, other) -> {
            int byX = Integer.compare(one.x(), other.x());
            return byX != 0 ? byX : Integer.compare(one.z(), other.z());
        });
        return columns;
    }

    /** What tells one column of a run from another, which is where it stands. */
    private static long columnKey(Vec3i position) {
        return ((long) position.x() << 32) ^ (position.z() & 0xffffffffL);
    }

    /** Whether a block is one this gate is made of. */
    private static boolean isGate(Key block, Style style, Settings settings) {
        return settings.mechanics().isGateBlock(block) && style.material().test(block);
    }

    /** What a gate's sign says it is made of and how far it looks. */
    private record Style(boolean small, boolean clickable, Predicate<Key> material) {}

    /** Reads a gate's sign. */
    private static Style styleOf(PostedSign sign) {
        String written = sign.name().toLowerCase(Locale.ROOT);
        boolean clickable = written.endsWith(String.valueOf(CLICKABLE).toLowerCase(Locale.ROOT));
        boolean small = written.contains("dgate");

        Predicate<Key> material;
        if (written.startsWith("[glass")) {
            material = block -> block.value().endsWith("glass_pane");
        } else if (written.startsWith("[iron")) {
            material = block -> block.value().equals("iron_bars");
        } else if (written.startsWith("[nether")) {
            material = block -> block.value().equals("nether_brick_fence");
        } else {
            material = block -> true;
        }
        return new Style(small, clickable, material);
    }

    /** Every gate sign, plain and clickable. */
    private static List<String> bothForms() {
        List<String> names = new ArrayList<>(PLAIN.size() * 2);
        for (String plain : PLAIN) {
            names.add(plain);
            names.add(plain + CLICKABLE);
        }
        return List.copyOf(names);
    }

    /** The sides a run of gate carries on along, and the steps up and down it may take. */
    private static int[][] neighbours() {
        List<int[]> steps = new ArrayList<>();
        steps.add(new int[] {0, 1, 0});
        steps.add(new int[] {0, -1, 0});
        for (BlockFace side : BlockFace.horizontals()) {
            for (int dy = -1; dy <= 1; dy++) {
                steps.add(new int[] {side.deltaX(), dy, side.deltaZ()});
            }
        }
        return steps.toArray(new int[0][]);
    }
}
