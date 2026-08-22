// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.mechanic.SneakState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What the settings file says, and how it becomes {@link Settings}.
 *
 * <p>Anything the file does not mention is filled in from the defaults and written back, so a
 * server started on a new version finds the settings it has just gained rather than silently
 * running without them. Values already in the file are never overwritten; only the comments are
 * rewritten, so an explanation improved in one version reaches a server that has been running
 * since an earlier one.
 *
 * <p>An entry that cannot be understood is reported and skipped rather than taking the rest of the
 * file down with it.
 *
 * <p>Every setting's name, default and explanation lives here rather than in either binding, so
 * the two platforms cannot drift into disagreeing about what an operator's file means. Reading and
 * writing the file itself is {@link ConfigTree}, and what a block name means is {@link BlockNames};
 * those are the only two things a server is needed for.
 */
@NullMarked
public final class ConfigDocument {

    private final BlockNames names;
    private final Consumer<String> report;

    /**
     * @param names how a written block name becomes a block
     * @param report where to send a complaint about an entry that could not be understood
     */
    public ConfigDocument(BlockNames names, Consumer<String> report) {
        this.names = names;
        this.report = report;
    }

    /**
     * Fills in what the file was missing, says what everything is for, and reads it.
     *
     * <p>The tree is left holding a complete file, for the caller to write back out.
     */
    public Settings applyTo(ConfigTree tree) {
        fillIn(tree);
        explain(tree);
        return build(tree);
    }

    private static final String ENABLED = "enabled";
    private static final String DISABLED_WORLDS = "disabled-worlds";
    private static final String DISABLED_CHIPS = "ics.disabled";
    private static final String MAX_RADIUS = "ics.max-radius";
    private static final String MAX_WIDTH = "ics.max-width";
    private static final String MAX_LENGTH = "ics.max-length";
    private static final String MAX_PLANTER_WIDTH = "ics.max-planter-width";
    private static final String PLACEABLE_BLOCKS = "ics.placeable-blocks";

    private static final String CARTS_DISABLED = "carts.disabled";
    private static final String CART_BLOCKS = "carts.blocks";
    private static final String CART_BOOSTERS = "carts.boosters";
    private static final String CART_LAUNCH_SPEED = "carts.launch-speed";
    private static final String CART_WATER_BUCKETS = "carts.return-water-buckets";
    private static final String HABIT_DECAY_AFTER = "vehicles.carts.decay-empty-after";
    private static final String HABIT_DECAY_AFTER_EXIT = "vehicles.carts.decay-only-after-exit";
    private static final String HABIT_REMOVE_ON_EXIT = "vehicles.carts.remove-on-exit";
    private static final String HABIT_GIVE_BACK = "vehicles.carts.give-cart-back";
    private static final String HABIT_PICK_UP = "vehicles.carts.pick-up-items";
    private static final String HABIT_BLOCK_MOBS = "vehicles.carts.block-mobs";
    private static final String HABIT_CLIMB_SPEED = "vehicles.carts.climb-speed";
    private static final String HABIT_PLATE_CROSSINGS = "vehicles.carts.plate-crossings";
    private static final String HABIT_THROUGH_EMPTY = "vehicles.carts.pass-through-empty-carts";
    private static final String HABIT_THROUGH_FULL = "vehicles.carts.pass-through-full-carts";
    private static final String HABIT_RUN_DOWN = "vehicles.carts.run-down-what-it-hits";
    private static final String HABIT_RUN_DOWN_HURTS = "vehicles.carts.run-down-only-hurts";
    private static final String HABIT_RUN_DOWN_CARTS = "vehicles.carts.run-down-other-carts";

    private static final String BOAT_DECAY_AFTER = "vehicles.boats.decay-empty-after";
    private static final String BOAT_DECAY_AFTER_EXIT = "vehicles.boats.decay-only-after-exit";
    private static final String BOAT_REMOVE_ON_EXIT = "vehicles.boats.remove-on-exit";
    private static final String BOAT_GIVE_BACK = "vehicles.boats.give-boat-back";
    private static final String BOAT_WATER_ONLY = "vehicles.boats.water-place-only";
    private static final String BOAT_RUN_DOWN = "vehicles.boats.run-down-what-it-hits";
    private static final String BOAT_RUN_DOWN_HURTS = "vehicles.boats.run-down-only-hurts";
    private static final String BOAT_RUN_DOWN_BOATS = "vehicles.boats.run-down-other-boats";
    private static final String PIPES_ENABLED = "pipes.enabled";
    private static final String PIPES_MAX_LENGTH = "pipes.max-length";
    private static final String PIPES_STACK_PER_PULL = "pipes.stack-per-pull";

    private static final String MECHANICS_DISABLED = "mechanics.disabled";
    private static final String MECHANICS_REDSTONE = "mechanics.redstone";
    private static final String GATE_BLOCKS = "mechanics.gate-blocks";
    private static final String GATE_RADIUS = "mechanics.gate-radius";
    private static final String GATE_CLICKING = "mechanics.gate-clicking";
    private static final String LIFT_JUMPING = "mechanics.lift-jumping";
    private static final String LIFT_BUTTONS = "mechanics.lift-buttons";
    private static final String LIFT_TOLERANCE = "mechanics.lift-tolerance";
    private static final String MAX_AREA_BLOCKS = "mechanics.max-area-blocks";
    private static final String MAX_AREAS = "mechanics.max-areas-per-name";
    private static final String GLOWSTONE_OFF = "mechanics.glowstone-off-block";
    private static final String FIRE_BLOCKS = "mechanics.fire-blocks";
    private static final String DEPOWER_ON_REMOVAL = "mechanics.depower-on-source-removal";
    private static final String LIGHT_SWITCH_RANGE = "mechanics.light-switch-range";
    private static final String LIGHT_SWITCH_LIGHTS = "mechanics.light-switch-max-lights";
    private static final String LIGHT_STONE_ITEM = "mechanics.light-stone-item";
    private static final String AMMETER_ITEM = "mechanics.ammeter-item";
    private static final String BOUNCE_BLOCKS = "mechanics.bounce-blocks";
    private static final String AUTO_BOUNCE_BLOCKS = "mechanics.auto-bounce-blocks";
    private static final String BOUNCE_SENSITIVITY = "mechanics.bounce-sensitivity";
    private static final String TELEPORTER_BUTTONS = "mechanics.teleporter-buttons";
    private static final String TELEPORTER_REQUIRE_SIGN = "mechanics.teleporter-require-sign";
    private static final String TELEPORTER_RANGE = "mechanics.teleporter-range";
    private static final String XP_BLOCK = "mechanics.xp-storer-block";
    private static final String XP_PER_BOTTLE = "mechanics.xp-per-bottle";
    private static final String XP_REQUIRES_BOTTLE = "mechanics.xp-requires-bottle";
    private static final String XP_SNEAK = "mechanics.xp-sneak-state";
    private static final String SNOW_PILING = "mechanics.snow.piling";
    private static final String SNOW_DISPERSION = "mechanics.snow.dispersion";
    private static final String SNOW_FREEZES = "mechanics.snow.freezes-water";
    private static final String SNOW_MELTS = "mechanics.snow.melts-in-sunlight";
    private static final String SNOW_PARTIAL_MELT = "mechanics.snow.partial-melt-only";
    private static final String SNOWBALLS_PILE = "mechanics.snow.snowballs-pile";

    /** Puts the default of every setting the file does not already carry into it. */
    private static void fillIn(ConfigTree tree) {
        Settings defaults = Settings.DEFAULTS;
        setIfAbsent(tree, ENABLED, defaults.enabled());
        setIfAbsent(tree, DISABLED_WORLDS, new ArrayList<>(defaults.disabledWorlds()));
        setIfAbsent(tree, DISABLED_CHIPS, new ArrayList<>(defaults.disabledChips()));
        setIfAbsent(tree, MAX_RADIUS, defaults.maxRadius());
        setIfAbsent(tree, MAX_WIDTH, defaults.maxWidth());
        setIfAbsent(tree, MAX_LENGTH, defaults.maxLength());
        setIfAbsent(tree, MAX_PLANTER_WIDTH, defaults.maxPlanterWidth());
        setIfAbsent(tree, PLACEABLE_BLOCKS, names(defaults.placeableBlocks()));

        CartSettings carts = defaults.carts();
        setIfAbsent(tree, CARTS_DISABLED, new ArrayList<>(carts.disabled()));
        setIfAbsent(tree, CART_LAUNCH_SPEED, carts.launchSpeed());
        setIfAbsent(tree, CART_WATER_BUCKETS, carts.returnWaterBuckets());
        carts.blocks().forEach((mechanic, block) ->
                setIfAbsent(tree, CART_BLOCKS + "." + mechanic, block.asString()));
        carts.boosters().forEach((block, multiplier) ->
                setIfAbsent(tree, CART_BOOSTERS + "." + block.value(), multiplier));

        CartHabits habits = defaults.vehicles().carts();
        setIfAbsent(tree, HABIT_DECAY_AFTER, habits.decayEmptyAfter());
        setIfAbsent(tree, HABIT_DECAY_AFTER_EXIT, habits.decayOnlyAfterExit());
        setIfAbsent(tree, HABIT_REMOVE_ON_EXIT, habits.removeOnExit());
        setIfAbsent(tree, HABIT_GIVE_BACK, habits.giveCartBack());
        setIfAbsent(tree, HABIT_PICK_UP, habits.pickUpItems());
        setIfAbsent(tree, HABIT_BLOCK_MOBS, habits.blockMobs());
        setIfAbsent(tree, HABIT_CLIMB_SPEED, habits.climbSpeed());
        setIfAbsent(tree, HABIT_PLATE_CROSSINGS, habits.plateIntersections());
        setIfAbsent(tree, HABIT_THROUGH_EMPTY, habits.passThroughEmptyCarts());
        setIfAbsent(tree, HABIT_THROUGH_FULL, habits.passThroughFullCarts());
        setIfAbsent(tree, HABIT_RUN_DOWN, habits.runDownEntities());
        setIfAbsent(tree, HABIT_RUN_DOWN_HURTS, habits.runDownOnlyHurts());
        setIfAbsent(tree, HABIT_RUN_DOWN_CARTS, habits.runDownOtherCarts());

        BoatHabits boats = defaults.vehicles().boats();
        setIfAbsent(tree, BOAT_DECAY_AFTER, boats.decayEmptyAfter());
        setIfAbsent(tree, BOAT_DECAY_AFTER_EXIT, boats.decayOnlyAfterExit());
        setIfAbsent(tree, BOAT_REMOVE_ON_EXIT, boats.removeOnExit());
        setIfAbsent(tree, BOAT_GIVE_BACK, boats.giveBoatBack());
        setIfAbsent(tree, BOAT_WATER_ONLY, boats.waterPlaceOnly());
        setIfAbsent(tree, BOAT_RUN_DOWN, boats.runDownEntities());
        setIfAbsent(tree, BOAT_RUN_DOWN_HURTS, boats.runDownOnlyHurts());
        setIfAbsent(tree, BOAT_RUN_DOWN_BOATS, boats.runDownOtherBoats());

        PipeSettings pipes = defaults.pipes();
        setIfAbsent(tree, PIPES_ENABLED, pipes.enabled());
        setIfAbsent(tree, PIPES_MAX_LENGTH, pipes.maxLength());
        setIfAbsent(tree, PIPES_STACK_PER_PULL, pipes.stackPerPull());

        MechanicSettings mechanics = defaults.mechanics();
        setIfAbsent(tree, MECHANICS_DISABLED, new ArrayList<>(mechanics.disabled()));
        setIfAbsent(tree, MECHANICS_REDSTONE, mechanics.redstone());
        setIfAbsent(tree, GATE_BLOCKS, names(mechanics.gateBlocks()));
        setIfAbsent(tree, GATE_RADIUS, mechanics.gateRadius());
        setIfAbsent(tree, GATE_CLICKING, mechanics.gateClicking());
        setIfAbsent(tree, LIFT_JUMPING, mechanics.liftJumping());
        setIfAbsent(tree, LIFT_BUTTONS, mechanics.liftButtons());
        setIfAbsent(tree, LIFT_TOLERANCE, mechanics.liftTolerance());
        setIfAbsent(tree, MAX_AREA_BLOCKS, mechanics.maxAreaBlocks());
        setIfAbsent(tree, MAX_AREAS, mechanics.maxAreasPerNamespace());
        setIfAbsent(tree, GLOWSTONE_OFF, mechanics.glowstoneOffBlock().asString());
        setIfAbsent(tree, FIRE_BLOCKS, names(mechanics.fireBlocks()));
        setIfAbsent(tree, DEPOWER_ON_REMOVAL, mechanics.depowerOnSourceRemoval());
        setIfAbsent(tree, LIGHT_SWITCH_RANGE, mechanics.lightSwitchRange());
        setIfAbsent(tree, LIGHT_SWITCH_LIGHTS, mechanics.lightSwitchMaxLights());
        setIfAbsent(tree, LIGHT_STONE_ITEM, mechanics.lightStoneItem().asString());
        setIfAbsent(tree, AMMETER_ITEM, mechanics.ammeterItem().asString());
        setIfAbsent(tree, BOUNCE_BLOCKS, names(mechanics.bounceBlocks()));
        mechanics.autoBounceBlocks().forEach((block, throwing) ->
                setIfAbsent(tree, AUTO_BOUNCE_BLOCKS + "." + block.value(), throwing));
        setIfAbsent(tree, BOUNCE_SENSITIVITY, mechanics.bounceSensitivity());
        setIfAbsent(tree, TELEPORTER_BUTTONS, mechanics.teleporterButtons());
        setIfAbsent(tree, TELEPORTER_REQUIRE_SIGN, mechanics.teleporterRequireSign());
        setIfAbsent(tree, TELEPORTER_RANGE, mechanics.teleporterRange());
        setIfAbsent(tree, XP_BLOCK, mechanics.xpStorerBlock().asString());
        setIfAbsent(tree, XP_PER_BOTTLE, mechanics.xpPerBottle());
        setIfAbsent(tree, XP_REQUIRES_BOTTLE, mechanics.xpRequiresBottle());
        setIfAbsent(tree, XP_SNEAK, mechanics.xpSneakState().written());
        setIfAbsent(tree, SNOW_PILING, mechanics.snow().piling());
        setIfAbsent(tree, SNOW_DISPERSION, mechanics.snow().dispersion());
        setIfAbsent(tree, SNOW_FREEZES, mechanics.snow().freezesWater());
        setIfAbsent(tree, SNOW_MELTS, mechanics.snow().meltsInSunlight());
        setIfAbsent(tree, SNOW_PARTIAL_MELT, mechanics.snow().partialMeltOnly());
        setIfAbsent(tree, SNOWBALLS_PILE, mechanics.snow().snowballsPile());
    }

    private static void setIfAbsent(ConfigTree tree, String path, Object value) {
        if (!tree.has(path)) {
            tree.set(path, value);
        }
    }

    /** Says what each setting is for, in the file itself. */
    private static void explain(ConfigTree tree) {
        tree.header(List.of(
                "CraftBook Ultimate.",
                "",
                "Everything here is either a limit on how far a chip may reach or a statement",
                "about what may run at all. None of it changes what a sign means: a sign asking",
                "for more than it is allowed gets as much as it is allowed, so changing a limit",
                "shortens or lengthens what an existing build does rather than breaking it.",
                "",
                "Changes take effect on the next /craftbook reload."));

        tree.comment(ENABLED, List.of(
                "Whether chips run at all.",
                "Setting this to false leaves every sign where it is and stops it working,",
                "which is how the plugin is taken out of service without losing anything."));

        tree.comment(DISABLED_WORLDS, List.of(
                "",
                "Worlds where no chip runs, by name. Chips there are left alone, not removed."));

        tree.comment("ics", List.of(
                "",
                "The integrated circuits: the chips built by writing a model reference on a sign."));

        tree.comment(DISABLED_CHIPS, List.of(
                "Chips that are never created and never run, by model number, such as MCX203.",
                "A chip answering to more than one number is switched off by any of them."));

        tree.comment(MAX_RADIUS, List.of(
                "",
                "The furthest a chip may reach when its sign gives a radius.",
                "Only the chips that take a free radius rather than a named range read this;",
                "the bolt strike is the one that does."));

        tree.comment(MAX_WIDTH, List.of(
                "",
                "The widest a bridge, a door or a harvested area may be, in blocks across."));

        tree.comment(MAX_LENGTH, List.of(
                "The furthest a bridge, a door or a harvested area may run from its sign.",
                "A door reads this as its height."));

        tree.comment(MAX_PLANTER_WIDTH, List.of(
                "The largest field an area planter may sow, along either side."));

        tree.comment(PLACEABLE_BLOCKS, List.of(
                "",
                "What a bridge, a door or a flex set may place. Anything else is refused.",
                "Taking a block away is never refused, so striking one off this list leaves the",
                "structures already made of it able to retract rather than stuck out.",
                "",
                "An entry is a block name, a tag written with a leading # such as",
                "#minecraft:planks, or a name from before the flattening such as 35:14.",
                "An empty list allows any block at all."));

        tree.comment("carts", List.of(
                "",
                "The minecart mechanics: a block under a piece of rail that a cart rolls over."));

        tree.comment(CARTS_DISABLED, List.of(
                "Mechanics that never run, by name, such as Craft. The signs are left alone."));

        tree.comment(CART_BLOCKS, List.of(
                "",
                "Which block builds which mechanic. Two mechanics may share a block, in which",
                "case their signs tell them apart. The message sign has no block of its own and",
                "works wherever it is hung."));

        tree.comment(CART_BOOSTERS, List.of(
                "",
                "How much a booster block multiplies a passing cart's speed by. Above one speeds",
                "a cart up and below one slows it down; the very large number is what sends a",
                "cart off at its top speed."));

        tree.comment("vehicles", List.of(
                "",
                "How everything a player rides behaves, with nothing built and no sign anywhere.",
                "All of it is off out of the box: a server that has never been configured runs",
                "carts and boats exactly as the game does. Carts and boats are kept apart even",
                "where the habit is the same, because clearing abandoned carts off a station says",
                "nothing whatever about the boats on a lake."));

        tree.comment("vehicles.carts", List.of(
                "",
                "How every cart behaves, whether or not it is standing on a mechanism. The two",
                "numbers switch their own habit off when they are zero, since waiting no time and",
                "climbing at no speed both mean not doing it."));

        tree.comment("vehicles.boats", List.of(
                "",
                "How every boat behaves, wherever it is floating. The wait switches decay off when",
                "it is zero.",
                "",
                "The fork this was ported from also offered boats that work on land and boats with",
                "a speed set on them. Neither is here: the server fields behind both are written",
                "and never read, on this platform and on Sponge, so the settings would have done",
                "nothing while looking as though they worked."));

        tree.comment(HABIT_DECAY_AFTER, List.of(
                "How many ticks a cart may stand empty before it is taken away. 0 leaves empty",
                "carts alone; " + CartHabits.CUSTOMARY_DECAY_TICKS + " is two seconds."));

        tree.comment(HABIT_DECAY_AFTER_EXIT, List.of(
                "Whether only a cart somebody has got out of decays. Turning this off starts the",
                "clock on every cart the moment it is placed, including ones nobody has touched."));

        tree.comment(HABIT_REMOVE_ON_EXIT, List.of(
                "",
                "Whether a cart is taken away the moment its rider steps out, so a station is",
                "never left with a row of abandoned carts."));

        tree.comment(HABIT_GIVE_BACK, List.of(
                "Whether taking it away hands the rider the cart back. Creative mode gets",
                "nothing, having lost nothing."));

        tree.comment(HABIT_PICK_UP, List.of(
                "",
                "Whether a storage cart gathers up items it runs over. A stack that will not",
                "all fit is left where it lies rather than half taken."));

        tree.comment(HABIT_BLOCK_MOBS, List.of(
                "Whether creatures are kept out of carts, leaving them for people."));

        tree.comment(HABIT_CLIMB_SPEED, List.of(
                "",
                "How fast a cart climbs a ladder or a vine, which it cannot do in the game",
                "itself. 0 for a cart that cannot climb; " + CartHabits.CUSTOMARY_CLIMB_SPEED
                        + " is a comfortable pace."));

        tree.comment(HABIT_PLATE_CROSSINGS, List.of(
                "Whether a pressure plate carries a cart straight across it as a crossroads,",
                "at full speed, instead of the rail turning it."));

        tree.comment(HABIT_THROUGH_EMPTY, List.of(
                "",
                "Whether a cart passes through an empty one rather than shunting it, so a siding",
                "of spares does not block the line."));

        tree.comment(HABIT_THROUGH_FULL, List.of(
                "Whether a cart passes through a laden or occupied one, so goods and people can",
                "share a track."));

        tree.comment(HABIT_RUN_DOWN, List.of(
                "",
                "Whether a cart with somebody aboard hurts what it runs into. An empty cart",
                "rolling downhill never does."));

        tree.comment(HABIT_RUN_DOWN_HURTS, List.of(
                "Whether running something down stops at hurting it. Nothing is removed when",
                "this is on, including the things that cannot be hurt."));

        tree.comment(BOAT_DECAY_AFTER, List.of(
                "How many ticks a boat may sit empty before it is taken away. 0 leaves empty",
                "boats alone; " + BoatHabits.CUSTOMARY_DECAY_TICKS + " is two seconds."));

        tree.comment(BOAT_DECAY_AFTER_EXIT, List.of(
                "Whether only a boat somebody has got out of decays. Turning this off starts the",
                "clock on every boat the moment it is placed, including ones nobody has touched."));

        tree.comment(BOAT_REMOVE_ON_EXIT, List.of(
                "Whether a boat is taken away the moment its rider steps out."));

        tree.comment(BOAT_GIVE_BACK, List.of(
                "Whether taking it away hands the rider the boat back."));

        tree.comment(BOAT_WATER_ONLY, List.of(
                "Whether a boat may only be put down on water, rather than anywhere at all."));

        tree.comment(BOAT_RUN_DOWN, List.of(
                "Whether an occupied boat hurts what it runs into."));

        tree.comment(BOAT_RUN_DOWN_HURTS, List.of(
                "Whether running something down stops at hurting it, rather than removing it."));

        tree.comment(BOAT_RUN_DOWN_BOATS, List.of(
                "Whether an occupied boat runs down other boats as well as creatures."));

        tree.comment(HABIT_RUN_DOWN_CARTS, List.of(
                "Whether an occupied cart runs down other carts as well as creatures."));

        tree.comment(CART_LAUNCH_SPEED, List.of(
                "",
                "How fast a mechanic that launches a cart launches it: a delay letting one go, a",
                "launcher somebody has climbed into, or a dispenser told to push."));

        tree.comment(CART_WATER_BUCKETS, List.of(
                "Whether crafting in a cart gives a water bucket back full rather than empty.",
                "Vanilla gives back an empty one; this is a kindness to anybody crafting in bulk."));

        tree.comment("pipes", List.of(
                "",
                "The pipes: a run of glass or panes that carries items from one container to",
                "another when the block at its head is powered. Both ways of building one work.",
                "A sticky piston starts a run of glass, which branches wherever it touches more",
                "glass, goes straight over a pane, keeps to its own colour where it is stained,",
                "and hands what reaches it to whatever a plain piston points at. A piston with an",
                "[Extractor] sign starts a run of panes instead, which spreads at every pane and",
                "fills any container it touches.",
                "",
                "A sign named [Pipe] or [Extractor] may name what a way out will take on its third",
                "line and what it will refuse on its fourth, separated by commas."));

        tree.comment(PIPES_ENABLED, List.of(
                "Whether pipes carry anything. The blocks stay where they are either way."));

        tree.comment(PIPES_MAX_LENGTH, List.of(
                "How many blocks of pipe are followed before the search gives up. A pipe past",
                "this carries items as far as the limit reaches rather than refusing to work."));

        tree.comment(PIPES_STACK_PER_PULL, List.of(
                "Whether one pulse moves a single stack. Turning this off empties as much of the",
                "container as the pipe can find room for, which is faster and far more work."));

        tree.comment("mechanics", List.of(
                "",
                "The sign mechanics: the bridges, doors, gates and lifts. How wide a bridge or a",
                "door may be, how far it may run and what it may be made of come from the ics",
                "section above, because those are the same limits."));

        tree.comment(MECHANICS_DISABLED, List.of(
                "Mechanics that never run, by name: Bridge, Door, Gate or Elevator.",
                "The signs are left alone, not removed."));

        tree.comment(MECHANICS_REDSTONE, List.of(
                "",
                "Whether redstone arriving beside a sign works the mechanic on it.",
                "Power arriving shuts it and power leaving opens it, so a mechanic on a lever",
                "always agrees with the lever."));

        tree.comment(GATE_BLOCKS, List.of(
                "",
                "What a gate may be made of. An entry is a block name, a tag written with a",
                "leading # such as #minecraft:fences, or a name from before the flattening.",
                "The glass, iron and nether gate signs each take only their own material out of",
                "this list; the plain sign takes any of it."));

        tree.comment(GATE_RADIUS, List.of(
                "",
                "How far around its sign a gate looks for its own material. The D forms of each",
                "gate sign ignore this and look barely past themselves, which is how two gates",
                "standing side by side are kept from catching one another."));

        tree.comment(GATE_CLICKING, List.of(
                "Whether a gate whose sign ends in C answers to a hand on its own fence."));

        tree.comment(LIFT_JUMPING, List.of(
                "",
                "Whether jumping and crouching work a [Lift UpDown] sign fixed to the block",
                "somebody is standing on."));

        tree.comment(LIFT_BUTTONS, List.of(
                "Whether a button two blocks in front of a lift's sign works it."));

        tree.comment(LIFT_TOLERANCE, List.of(
                "How far a lift will drop somebody below the far sign to find them a floor.",
                "Beyond this it says there is no floor rather than dropping them down a shaft."));

        tree.comment(MAX_AREA_BLOCKS, List.of(
                "",
                "The most blocks one saved area may hold. Zero is no limit.",
                "An area is saved with /area save and lives in the areas folder, one file of",
                "blocks and one saying where they go."));

        tree.comment(MAX_AREAS, List.of(
                "The most areas any one name may have saved. Zero is no limit.",
                "Saving over an area that already exists does not count against this."));

        tree.comment(GLOWSTONE_OFF, List.of(
                "",
                "What a glowstone looks like while it is dark. Powering it turns it back into",
                "glowstone, and taking the power away turns it into this."));

        tree.comment(FIRE_BLOCKS, List.of(
                "What catches light on top of itself while it is powered. The block is never",
                "changed; what changes is the air above it."));

        tree.comment(DEPOWER_ON_REMOVAL, List.of(
                "Whether a powered block goes out when the redstone feeding it is mined away.",
                "Off by default, and deliberately: powering a light and then mining the redstone",
                "is how a builder makes one that stays on. Switching a lever off still turns it",
                "off either way — this is only about the power source being taken out of the",
                "world."));

        tree.comment(LIGHT_SWITCH_RANGE, List.of(
                "",
                "How far a light switch reaches out for torches. A sign may ask for less on its",
                "third line."));

        tree.comment(LIGHT_SWITCH_LIGHTS, List.of(
                "The most torches one light switch turns. A sign may ask for fewer on its fourth",
                "line."));

        tree.comment(LIGHT_STONE_ITEM, List.of(
                "What is held up to a block to read its light level off it."));

        tree.comment(AMMETER_ITEM, List.of(
                "What is held up to a block to read how much redstone power it carries."));

        tree.comment(BOUNCE_BLOCKS, List.of(
                "",
                "What throws somebody who jumps on it, when a [Jump] sign under it says how hard."));

        tree.comment(AUTO_BOUNCE_BLOCKS, List.of(
                "What throws somebody with no sign at all, and how hard. The throw is written the",
                "way a sign writes it: a number for straight up, three for a push along the",
                "ground, and a leading ! to ignore which way the jumper is facing."));

        tree.comment(BOUNCE_SENSITIVITY, List.of(
                "How much of a jump counts as one. Lower notices a smaller hop."));

        tree.comment(TELEPORTER_BUTTONS, List.of(
                "",
                "Whether a button on the far side of a teleporter sign works it, so a builder can",
                "hide the sign behind the wall."));

        tree.comment(TELEPORTER_REQUIRE_SIGN, List.of(
                "Whether the far end needs a teleporter sign of its own. On, this means a",
                "teleporter can only send somebody where a builder has said they may arrive."));

        tree.comment(TELEPORTER_RANGE, List.of(
                "How far a teleporter may send somebody. A negative number is no limit."));

        tree.comment(XP_BLOCK, List.of(
                "",
                "What turns the experience somebody is carrying into bottles when it is clicked."));

        tree.comment(XP_PER_BOTTLE, List.of(
                "How much experience one bottle costs. Whatever will not pay for a whole bottle",
                "stays with the player rather than being lost."));

        tree.comment(XP_REQUIRES_BOTTLE, List.of(
                "Whether the player has to be carrying empty bottles for it to fill."));

        tree.comment(XP_SNEAK, List.of(
                "Whether the player must be crouching to use one: must, must-not or either."));

        tree.comment("mechanics.snow", List.of(
                "",
                "How snow behaves. Every part of it is off out of the box: a server that has never",
                "been configured runs snow exactly as the game does. Unlike the mechanics above,",
                "nothing here is built and nothing has a sign — switching any of it on changes",
                "every snowy block in the world."));

        tree.comment(SNOW_PILING, List.of(
                "Whether snow keeps piling past the height the game stops at."));

        tree.comment(SNOW_DISPERSION, List.of(
                "Whether a pile slumps into the lower ground beside it, so drifts settle into a",
                "slope rather than standing in columns."));

        tree.comment(SNOW_FREEZES, List.of("Whether water under snow turns to ice."));

        tree.comment(SNOW_MELTS, List.of(
                "Whether snow in the warm and under open sky goes away again."));

        tree.comment(SNOW_PARTIAL_MELT, List.of(
                "Whether melting stops at the depth the game would have left, rather than",
                "clearing the ground entirely."));

        tree.comment(SNOWBALLS_PILE, List.of(
                "Whether a thrown snowball leaves snow where it lands."));
    }

    /** Turns what the file says into the settings the chips read. */
    private Settings build(ConfigTree tree) {
        Settings defaults = Settings.DEFAULTS;
        return Settings.builder()
                .enabled(tree.bool(ENABLED, defaults.enabled()))
                .disabledWorlds(Set.copyOf(tree.strings(DISABLED_WORLDS)))
                .disabledChips(Set.copyOf(tree.strings(DISABLED_CHIPS)))
                .maxRadius(tree.integer(MAX_RADIUS, defaults.maxRadius()))
                .maxWidth(tree.integer(MAX_WIDTH, defaults.maxWidth()))
                .maxLength(tree.integer(MAX_LENGTH, defaults.maxLength()))
                .maxPlanterWidth(tree.integer(MAX_PLANTER_WIDTH, defaults.maxPlanterWidth()))
                .placeableBlocks(names.blocks(tree.strings(PLACEABLE_BLOCKS), report))
                .carts(carts(tree, defaults.carts()))
                .vehicles(vehicles(tree, defaults.vehicles()))
                .pipes(pipes(tree, defaults.pipes()))
                .mechanics(mechanics(tree, defaults.mechanics()))
                .build();
    }

    /** Reads what an operator has said about the sign mechanics. */
    private MechanicSettings mechanics(ConfigTree tree, MechanicSettings defaults) {
        Set<Key> gateBlocks = names.blocks(tree.strings(GATE_BLOCKS), report);
        Set<Key> fireBlocks = names.blocks(tree.strings(FIRE_BLOCKS), report);
        Set<Key> bounceBlocks = names.blocks(tree.strings(BOUNCE_BLOCKS), report);

        // Each named block carries the throw it gives, in the same grammar a [Jump] sign uses.
        Map<Key, String> autoBounces = new LinkedHashMap<>();
        for (String written : tree.childrenOf(AUTO_BOUNCE_BLOCKS)) {
            Optional<Key> block = names.block(written);
            if (block.isEmpty()) {
                report.accept("No block called " + written + ", so nothing bounces off it");
                continue;
            }
            autoBounces.put(block.get(), tree.text(AUTO_BOUNCE_BLOCKS + "." + written, ""));
        }

        return new MechanicSettings(
                Set.copyOf(tree.strings(MECHANICS_DISABLED)),
                tree.bool(MECHANICS_REDSTONE, defaults.redstone()),
                gateBlocks.isEmpty() ? defaults.gateBlocks() : gateBlocks,
                tree.integer(GATE_RADIUS, defaults.gateRadius()),
                tree.bool(GATE_CLICKING, defaults.gateClicking()),
                tree.bool(LIFT_JUMPING, defaults.liftJumping()),
                tree.bool(LIFT_BUTTONS, defaults.liftButtons()),
                tree.integer(LIFT_TOLERANCE, defaults.liftTolerance()),
                tree.integer(MAX_AREA_BLOCKS, defaults.maxAreaBlocks()),
                tree.integer(MAX_AREAS, defaults.maxAreasPerNamespace()),
                names.block(tree.text(GLOWSTONE_OFF, defaults.glowstoneOffBlock().asString()))
                        .orElseGet(defaults::glowstoneOffBlock),
                fireBlocks.isEmpty() ? defaults.fireBlocks() : fireBlocks,
                tree.bool(DEPOWER_ON_REMOVAL, defaults.depowerOnSourceRemoval()),
                tree.integer(LIGHT_SWITCH_RANGE, defaults.lightSwitchRange()),
                tree.integer(LIGHT_SWITCH_LIGHTS, defaults.lightSwitchMaxLights()),
                names.block(tree.text(LIGHT_STONE_ITEM, defaults.lightStoneItem().asString()))
                        .orElseGet(defaults::lightStoneItem),
                names.block(tree.text(AMMETER_ITEM, defaults.ammeterItem().asString()))
                        .orElseGet(defaults::ammeterItem),
                bounceBlocks.isEmpty() ? defaults.bounceBlocks() : bounceBlocks,
                autoBounces.isEmpty() ? defaults.autoBounceBlocks() : autoBounces,
                tree.number(BOUNCE_SENSITIVITY, defaults.bounceSensitivity()),
                tree.bool(TELEPORTER_BUTTONS, defaults.teleporterButtons()),
                tree.bool(TELEPORTER_REQUIRE_SIGN, defaults.teleporterRequireSign()),
                tree.number(TELEPORTER_RANGE, defaults.teleporterRange()),
                names.block(tree.text(XP_BLOCK, defaults.xpStorerBlock().asString()))
                        .orElseGet(defaults::xpStorerBlock),
                tree.integer(XP_PER_BOTTLE, defaults.xpPerBottle()),
                tree.bool(XP_REQUIRES_BOTTLE, defaults.xpRequiresBottle()),
                SneakState.of(tree.text(XP_SNEAK, defaults.xpSneakState().written()),
                        defaults.xpSneakState()),
                snow(tree, defaults.snow()));
    }

    /** Reads what an operator has said about the minecart mechanics. */
    private CartSettings carts(ConfigTree tree, CartSettings defaults) {
        Map<String, Key> mechanicBlocks = new LinkedHashMap<>();
        for (String mechanic : tree.childrenOf(CART_BLOCKS)) {
            String written = tree.text(CART_BLOCKS + "." + mechanic, "");
            if (written.isBlank()) {
                continue;
            }
            Optional<Key> block = names.block(written);
            if (block.isEmpty()) {
                report.accept("No block called " + written
                        + ", so the " + mechanic + " cart mechanic cannot be built");
                continue;
            }
            mechanicBlocks.put(mechanic, block.get());
        }

        Map<Key, Double> boosters = new LinkedHashMap<>();
        for (String written : tree.childrenOf(CART_BOOSTERS)) {
            Optional<Key> block = names.block(written);
            if (block.isEmpty()) {
                report.accept("No block called " + written + ", so it boosts nothing");
                continue;
            }
            boosters.put(block.get(), tree.number(CART_BOOSTERS + "." + written, 1));
        }

        return new CartSettings(
                Set.copyOf(tree.strings(CARTS_DISABLED)),
                mechanicBlocks.isEmpty() ? defaults.blocks() : mechanicBlocks,
                boosters.isEmpty() ? defaults.boosters() : boosters,
                tree.number(CART_LAUNCH_SPEED, defaults.launchSpeed()),
                tree.bool(CART_WATER_BUCKETS, defaults.returnWaterBuckets()));
    }

    /** What an operator has said about the pipes. */
    private static PipeSettings pipes(ConfigTree tree, PipeSettings defaults) {
        return new PipeSettings(
                tree.bool(PIPES_ENABLED, defaults.enabled()),
                tree.integer(PIPES_MAX_LENGTH, defaults.maxLength()),
                tree.bool(PIPES_STACK_PER_PULL, defaults.stackPerPull()));
    }

    /** What an operator has said about how carts and boats behave everywhere. */
    private static VehicleHabits vehicles(ConfigTree tree, VehicleHabits defaults) {
        return new VehicleHabits(habits(tree, defaults.carts()), boats(tree, defaults.boats()));
    }

    /** How every boat behaves, wherever it is floating. */
    private static BoatHabits boats(ConfigTree tree, BoatHabits defaults) {
        return new BoatHabits(
                tree.count(BOAT_DECAY_AFTER, defaults.decayEmptyAfter()),
                tree.bool(BOAT_DECAY_AFTER_EXIT, defaults.decayOnlyAfterExit()),
                tree.bool(BOAT_REMOVE_ON_EXIT, defaults.removeOnExit()),
                tree.bool(BOAT_GIVE_BACK, defaults.giveBoatBack()),
                tree.bool(BOAT_WATER_ONLY, defaults.waterPlaceOnly()),
                tree.bool(BOAT_RUN_DOWN, defaults.runDownEntities()),
                tree.bool(BOAT_RUN_DOWN_HURTS, defaults.runDownOnlyHurts()),
                tree.bool(BOAT_RUN_DOWN_BOATS, defaults.runDownOtherBoats()));
    }

    /** How snow piles, slumps and melts. */
    private static SnowSettings snow(ConfigTree tree, SnowSettings defaults) {
        return new SnowSettings(
                tree.bool(SNOW_PILING, defaults.piling()),
                tree.bool(SNOW_DISPERSION, defaults.dispersion()),
                tree.bool(SNOW_FREEZES, defaults.freezesWater()),
                tree.bool(SNOW_MELTS, defaults.meltsInSunlight()),
                tree.bool(SNOW_PARTIAL_MELT, defaults.partialMeltOnly()),
                tree.bool(SNOWBALLS_PILE, defaults.snowballsPile()));
    }

    /** How every cart behaves, whether or not it is standing on a mechanism. */
    private static CartHabits habits(ConfigTree tree, CartHabits defaults) {
        return new CartHabits(
                tree.count(HABIT_DECAY_AFTER, defaults.decayEmptyAfter()),
                tree.bool(HABIT_DECAY_AFTER_EXIT, defaults.decayOnlyAfterExit()),
                tree.bool(HABIT_REMOVE_ON_EXIT, defaults.removeOnExit()),
                tree.bool(HABIT_GIVE_BACK, defaults.giveCartBack()),
                tree.bool(HABIT_PICK_UP, defaults.pickUpItems()),
                tree.bool(HABIT_BLOCK_MOBS, defaults.blockMobs()),
                tree.number(HABIT_CLIMB_SPEED, defaults.climbSpeed()),
                tree.bool(HABIT_PLATE_CROSSINGS, defaults.plateIntersections()),
                tree.bool(HABIT_THROUGH_EMPTY, defaults.passThroughEmptyCarts()),
                tree.bool(HABIT_THROUGH_FULL, defaults.passThroughFullCarts()),
                tree.bool(HABIT_RUN_DOWN, defaults.runDownEntities()),
                tree.bool(HABIT_RUN_DOWN_HURTS, defaults.runDownOnlyHurts()),
                tree.bool(HABIT_RUN_DOWN_CARTS, defaults.runDownOtherCarts()));
    }

    /** The names to write for a set of blocks. */
    private static List<String> names(Set<Key> blocks) {
        List<String> written = new ArrayList<>(blocks.size());
        for (Key block : blocks) {
            written.add(block.asString());
        }
        return written;
    }
}
