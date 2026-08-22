// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.testbed;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What a chip's sign says on a test bed, and what is put around it.
 *
 * <p>Many chips need nothing: a gate wired to a lever does something the moment it is flipped, and
 * plenty of the world chips have sensible defaults for both their lines. The rest read their third
 * and fourth lines and are inert without them.
 *
 * <p>Everything in {@link #CATALOGUE} was taken from the chip's own source rather than guessed. A
 * wrong line here is worse than a blank one — a chip configured with grammar it does not accept
 * reads exactly like a chip that is broken, which is the opposite of what a test bed is for. Where
 * a format could not be read off the source with confidence the chip is left blank on purpose.
 *
 * <p>The two configurable lines are always sign lines three and four, whatever a chip's own
 * documentation calls them: the gates number them inconsistently, some counting the sign's lines
 * from one and some counting the indices from zero, but both mean the same two lines.
 *
 * @param thirdLine what to write on the chip's third line
 * @param fourthLine what to write on its fourth line
 * @param props what to put around it
 * @param note what the label should say about this rig, blank when it is ready to try
 */
@NullMarked
public record ChipSetup(
        String thirdLine, String fourthLine, Map<PropPlace, Rig.Fixture> props, String note) {

    /** Told on the label of a chip that needs a file an operator has to supply. */
    public static final String NEEDS_FILES = "needs a file";

    /** The name the test bed uses wherever a chip wants a channel, a switch or a variable. */
    public static final String SHARED_NAME = "testbed";

    public ChipSetup {
        props = Map.copyOf(props);
    }

    /** Somewhere around a chip that a rig may put something. */
    public enum PropPlace {

        /** The block above the one the sign hangs on, where the container chips look. */
        ABOVE_BACKING(1),

        /** The block below the one the sign hangs on. */
        BELOW_BACKING(-1);

        private final int up;

        PropPlace(int up) {
            this.up = up;
        }

        /** Where this is, given a sign. */
        public Vec3i resolve(Vec3i signPosition, BlockFace facing) {
            return SignSupport.of(signPosition, facing).add(0, up, 0);
        }
    }

    /** A chip that needs nothing said to it. */
    public static ChipSetup bare() {
        return new ChipSetup("", "", Map.of(), "");
    }

    /** A chip whose configuration the test bed cannot supply. */
    public static ChipSetup unconfigured(String note) {
        return new ChipSetup("", "", Map.of(), note);
    }

    /** A chip that needs only its third line. */
    public static ChipSetup of(String thirdLine) {
        return new ChipSetup(thirdLine, "", Map.of(), "");
    }

    /** A chip that needs both its lines. */
    public static ChipSetup of(String thirdLine, String fourthLine) {
        return new ChipSetup(thirdLine, fourthLine, Map.of(), "");
    }

    /** This setup with something put around the chip as well. */
    public ChipSetup with(PropPlace place, Rig.Fixture fixture) {
        Map<PropPlace, Rig.Fixture> extended = new HashMap<>(props);
        extended.put(place, fixture);
        return new ChipSetup(thirdLine, fourthLine, extended, note);
    }

    /** The setup for a chip, or a bare one where the table does not name it. */
    public static ChipSetup forModel(String model) {
        return CATALOGUE.getOrDefault(model, bare());
    }

    /** Whether the table has an opinion about a chip. */
    public static boolean isKnown(String model) {
        return CATALOGUE.containsKey(model);
    }

    /**
     * Whether a chip should be built ticking.
     *
     * <p>Opt-in, and deliberately short. Several chips act on the world on every tick with no
     * input check — a holy smite strikes everything in range, a transporter moves whoever is
     * standing there — so a bed built ticking wholesale is a dead server within the minute. That
     * is not a hypothetical; it is what the first version of this did.
     *
     * <p>What is listed here are the chips whose tick only <em>reads</em>: a sensor looking at a
     * block, a receiver looking at a band, a comparison looking at a variable. All of them drive
     * an output and touch nothing else, so ticking one costs a lookup and changes nothing.
     *
     * <p>They need it because without it their output only moves when their own input is pulsed,
     * which on a test bed means a receiver whose lever is really a clock — the far end changes and
     * nothing happens until you toggle the near one. That reads as a broken chip when both ends
     * are working perfectly.
     */
    public static boolean ticks(String model) {
        return TICKS.contains(model);
    }

    /** The chips whose tick reads something and drives an output, and does nothing else. */
    private static final Set<String> TICKS = Set.of(
            // Follows a wireless band. Without this its own lever is its clock.
            "MC1111",
            // Sensors reading a block, a liquid, a light level or the weather.
            "MC1260", "MC1261", "MC1262", "MCX230", "MCX231", "MCX205",
            // Sensors reading who and what is nearby.
            "MCM116", "MCX116", "MCX117", "MCX118", "MCX119", "MCX138", "MCX139", "MCX140",
            "MC1500",
            // Readers of the clock.
            "MC1230", "MCX027", "MC1025", "MC1026",
            // Reads a variable, and reads a block's redstone.
            "VAR170", "MCX295");

    private static final Key COAL = Key.key("minecraft", "coal");
    private static final Key STONE = Key.key("minecraft", "stone");
    private static final Key JUKEBOX = Key.key("minecraft", "jukebox");

    /** A chest of something, put above the block the sign hangs on. */
    private static ChipSetup withChest(ChipSetup setup, Key item, int count) {
        return setup.with(PropPlace.ABOVE_BACKING, new Rig.Fixture.Chest(Map.of(item, count)));
    }

    /**
     * What each chip needs told, by model number.
     *
     * <p>Absent means the chip is left blank, which for most of the catalogue is right: a logic
     * gate wants nothing, and many world chips default both their lines sensibly.
     */
    private static final Map<String, ChipSetup> CATALOGUE = buildCatalogue();

    private static Map<String, ChipSetup> buildCatalogue() {
        Map<String, ChipSetup> table = new HashMap<>();

        // Timing. Every one of these is a period, a window or a count.
        table.put("MC1420", of("20"));
        table.put("MC1230", of("0", "12000"));
        table.put("MCX027", of("0", "12000"));
        table.put("MC1025", of("2", "0"));
        table.put("MC1026", of("2", "0"));
        table.put("MCX010", of("100", "1"));
        table.put("MCX011", of("1000"));

        // Sensors. The light sensor takes its threshold first and its offset second.
        table.put("MC1260", of("1"));
        table.put("MC1261", of("1"));
        table.put("MC1262", of("8", "0"));
        table.put("MCX205", of("stone", "3").with(PropPlace.BELOW_BACKING, new Rig.Fixture.Prop(STONE)));

        // Counters and the combination lock, which read a limit and a combination.
        table.put("MC3101", of("5"));
        table.put("MC3102", of("5"));
        table.put("MC3050", of("XXX"));

        // Wireless, and the two that name a run of bands rather than one.
        table.put("MC1110", of(SHARED_NAME));
        table.put("MC1111", of(SHARED_NAME));
        table.put("MC6543", of(SHARED_NAME));
        table.put("MC3456", of(SHARED_NAME + ":0:3"));

        // Switches thrown by command, and the two ends of a transporter.
        table.put("MCX120", of(SHARED_NAME));
        table.put("MCX121", of(SHARED_NAME));
        table.put("MCX112", of(SHARED_NAME));
        table.put("MCU113", of(SHARED_NAME));

        // The variables. The command that builds the bed makes this variable first.
        table.put("VAR100", of(SHARED_NAME, "+:1"));
        table.put("VAR170", of(SHARED_NAME, "5"));
        table.put("VAR200", withChest(of(SHARED_NAME, "coal"), COAL, 12));

        // Blocks placed and swapped. The builders pay out of the chest above them.
        table.put("MCX206", of("Y+1:stone"));
        table.put("MC1207", of("Y+1:stone"));
        table.put("MC1205", of("stone"));
        table.put("MC1206", of("stone"));
        table.put("MCX211", of("stone|glass", "Y+1"));
        table.put("MC1249", of("stone|glass"));
        table.put("MCX207", withChest(of("stone", "3:5"), STONE, 64));
        table.put("MCX209", withChest(of("stone", "3:5"), STONE, 64));
        table.put("MCX208", withChest(of("stone", "3:4"), STONE, 64));
        table.put("MCX210", withChest(of("stone", "3:4"), STONE, 64));
        table.put("MCX213", of("wheat", "3:3:1"));

        // Farming.
        table.put("MCX216", of("wheat_seeds", "1"));
        table.put("MCX215", of("wheat_seeds", "3:3"));

        // Spawning and dispensing.
        table.put("MCX200", of("pig", "1"));
        table.put("MCX201", of("coal", "1"));
        table.put("MCX202", withChest(of("coal", "1"), COAL, 64));
        table.put("MCX203", withChest(of("", "5"), COAL, 1));

        // Shooters. A speed and a vertical velocity, except the fireball's rotation.
        for (String model : new String[] {"MC1240", "MC1241", "MCX242", "MCX243", "MCX244", "MCX245"}) {
            table.put(model, of("1", "0"));
        }
        table.put("MCX246", of("1", "0"));

        // Lightning: an offset, a reach and a chance, and a reach.
        table.put("MCX255", of("3"));
        table.put("MC1203", of("5", "50"));
        table.put("MCX256", of("", "5"));

        // Hurting things, which all take a reach and some a strength.
        table.put("MCX130", of("", "5"));
        table.put("MCX133", of("", "5"));
        table.put("MCX131", of("", "1"));
        table.put("MCX132", of("", "1"));

        // Effects. The potion names an effect by its short code.
        table.put("MCX146", of("SP:5:1"));
        table.put("MCX250", of("flame", "Y2"));

        // Sound. The jukebox needs one to play on, against the block the sign hangs on.
        table.put("MCX251", of("entity.creeper.primed"));
        table.put("MCU705", of("3:0c2e2g2"));
        table.put("MCU706", of("13").with(PropPlace.ABOVE_BACKING, new Rig.Fixture.Prop(JUKEBOX)));

        // Talking and logging.
        table.put("MC1511", of("Testbed says hello"));
        table.put("MCX512", of("Testbed says", "hello"));
        table.put("MCX515", of("Testbed log line"));
        table.put("MCX516", of("Testbed saw", "%p"));
        table.put("MCX517", of("Testbed saw", "%p"));

        // Sensing people and creatures, which mostly default sensibly but want a reach.
        table.put("MCX116", of("", "3"));
        table.put("MCX117", of("", "3"));
        table.put("MCX118", of("", "5"));
        table.put("MCX119", of("", "5"));
        table.put("MCX140", of("player", "5:3:5"));

        // Weather, real and imagined.
        table.put("MCX233", of("6000"));
        table.put("MCX236", of("10"));
        table.put("MCX238", of("10"));

        // Memory and timing oddments.
        table.put("MC2022", of("8"));
        table.put("MCU440", of("20:1"));
        table.put("MCX295", of("0:1:0"));

        // The two that read a file nobody can supply from here.
        table.put("MCU700", unconfigured(NEEDS_FILES));
        table.put("MC1253", unconfigured(NEEDS_FILES));

        return Map.copyOf(table);
    }
}
