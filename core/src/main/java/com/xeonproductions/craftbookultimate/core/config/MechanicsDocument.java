// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.config;

import com.xeonproductions.craftbookultimate.core.dispenser.DispenserRecipe;
import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import com.xeonproductions.craftbookultimate.core.mechanic.SneakState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What the mechanics file says, and how it becomes {@link MechanicSettings}.
 *
 * <p>Its own file, and its own document, because the mechanics are the half of this plugin an
 * operator is most likely to want to change and there are twenty of them. A section per mechanic,
 * named after the mechanic, is how both codebases this was written from arranged the same thing:
 * upstream keeps one file keyed by name, and the fork wrote a file per mechanic into a folder.
 *
 * <p>Every mechanic gets a section whether or not it has anything to configure, so the file answers
 * two questions rather than one — what may be changed, and what there is at all. A mechanic with
 * nothing of its own carries only whether it runs.
 *
 * <p>Every one of them starts switched off, and switching one on is its own section saying
 * {@code enabled: true} rather than a list of names somewhere else. A list is written in a
 * different place from the settings it governs, so an operator whose mechanic does nothing has to
 * know to look in two places; here the answer is the first line of what they were already reading.
 *
 * <p>Off to begin with because a mechanic answers to ordinary blocks. Switching Chairs on makes
 * every stair on the server a seat and switching HeadDrops on changes what every death leaves
 * behind, neither of which anybody built and neither of which an operator should discover by
 * having it happen. A chip is the other way round — it does nothing until somebody writes its
 * sign — which is why the chips are on out of the box and these are not.
 *
 * <p>Two rules belong to no single mechanic and stay at the top of the file: whether redstone works
 * a mechanic's sign at all, and whether a powered block goes out when its source is mined away.
 *
 * <p>Anything the file does not mention is filled in from the defaults and written back, values
 * already there are never overwritten, and an entry that cannot be understood is reported and
 * skipped — all exactly as {@link ConfigDocument} does it, and for the same reasons.
 */
@NullMarked
public final class MechanicsDocument {

    /** Whether a mechanic runs at all, which every section carries. */
    private static final String ENABLED = "enabled";

    private static final String REDSTONE = "redstone";
    private static final String DEPOWER_ON_REMOVAL = "depower-on-source-removal";

    private static final String CHAIR_BLOCKS = Mechanics.CHAIRS + ".blocks";
    private static final String CHAIR_REQUIRE_SIGN = Mechanics.CHAIRS + ".require-sign";
    private static final String CHAIR_SIGN_DISTANCE = Mechanics.CHAIRS + ".max-sign-distance";
    private static final String CHAIR_FACING = Mechanics.CHAIRS + ".face-correct-direction";
    private static final String CHAIR_EXIT_AT_ENTRY = Mechanics.CHAIRS + ".exit-at-last-position";
    private static final String CHAIR_HEAL_AMOUNT = Mechanics.CHAIRS + ".heal-amount";
    private static final String CHAIR_HEAL_RATE = Mechanics.CHAIRS + ".heal-rate";

    private static final String HEAD_PLAYERS = Mechanics.HEAD_DROPS + ".player-heads";
    private static final String HEAD_MOBS = Mechanics.HEAD_DROPS + ".mob-heads";
    private static final String HEAD_PLAYER_KILLS = Mechanics.HEAD_DROPS + ".player-kills-only";
    private static final String HEAD_DROP_RATE = Mechanics.HEAD_DROPS + ".drop-rate";
    private static final String HEAD_LOOTING = Mechanics.HEAD_DROPS + ".looting-rate-modifier";
    private static final String HEAD_SHOW_NAME = Mechanics.HEAD_DROPS + ".show-name-on-click";
    private static final String HEAD_IGNORED = Mechanics.HEAD_DROPS + ".ignored-names";

    private static final String GATE_BLOCKS = Mechanics.GATE + ".blocks";
    private static final String GATE_RADIUS = Mechanics.GATE + ".radius";
    private static final String GATE_CLICKING = Mechanics.GATE + ".clicking";

    private static final String LIFT_JUMPING = Mechanics.ELEVATOR + ".jumping";
    private static final String LIFT_BUTTONS = Mechanics.ELEVATOR + ".buttons";
    private static final String LIFT_TOLERANCE = Mechanics.ELEVATOR + ".tolerance";

    private static final String MAX_AREA_BLOCKS = Mechanics.AREA + ".max-blocks";
    private static final String MAX_AREAS = Mechanics.AREA + ".max-per-name";

    private static final String GLOWSTONE_OFF = Mechanics.GLOW_STONE + ".off-block";
    private static final String FIRE_BLOCKS = Mechanics.NETHERRACK + ".fire-blocks";

    private static final String LIGHT_SWITCH_RANGE = Mechanics.LIGHT_SWITCH + ".range";
    private static final String LIGHT_SWITCH_LIGHTS = Mechanics.LIGHT_SWITCH + ".max-lights";

    private static final String LIGHT_STONE_ITEM = Mechanics.LIGHT_STONE + ".item";
    private static final String AMMETER_ITEM = Mechanics.AMMETER + ".item";

    private static final String BOUNCE_BLOCKS = Mechanics.BOUNCE_BLOCKS + ".blocks";
    private static final String AUTO_BOUNCE_BLOCKS = Mechanics.BOUNCE_BLOCKS + ".automatic";
    private static final String BOUNCE_SENSITIVITY = Mechanics.BOUNCE_BLOCKS + ".sensitivity";

    private static final String TELEPORTER_BUTTONS = Mechanics.TELEPORTER + ".buttons";
    private static final String TELEPORTER_REQUIRE_SIGN = Mechanics.TELEPORTER + ".require-sign";
    private static final String TELEPORTER_RANGE = Mechanics.TELEPORTER + ".range";

    private static final String XP_BLOCK = Mechanics.XP_STORER + ".block";
    private static final String XP_PER_BOTTLE = Mechanics.XP_STORER + ".per-bottle";
    private static final String XP_REQUIRES_BOTTLE = Mechanics.XP_STORER + ".requires-bottle";
    private static final String XP_SNEAK = Mechanics.XP_STORER + ".sneaking";

    private static final String TREE_BLOCKS = Mechanics.TREE_LOPPER + ".blocks";
    private static final String TREE_TOOLS = Mechanics.TREE_LOPPER + ".tools";
    private static final String TREE_MAX = Mechanics.TREE_LOPPER + ".max-size";
    private static final String TREE_DIAGONALS = Mechanics.TREE_LOPPER + ".diagonals";
    private static final String TREE_ANY = Mechanics.TREE_LOPPER + ".any-listed-block";
    private static final String TREE_SINGLE_USE = Mechanics.TREE_LOPPER + ".single-use";
    private static final String TREE_LEAVES = Mechanics.TREE_LOPPER + ".leaves";
    private static final String TREE_BREAK_LEAVES = Mechanics.TREE_LOPPER + ".break-leaves";
    private static final String TREE_SAPLINGS = Mechanics.TREE_LOPPER + ".place-saplings";

    private static final String VEIN_BLOCKS = Mechanics.VEIN_MINER + ".blocks";
    private static final String VEIN_TOOLS = Mechanics.VEIN_MINER + ".tools";
    private static final String VEIN_MAX = Mechanics.VEIN_MINER + ".max-size";
    private static final String VEIN_DIAGONALS = Mechanics.VEIN_MINER + ".diagonals";
    private static final String VEIN_ANY = Mechanics.VEIN_MINER + ".any-listed-block";
    private static final String VEIN_SINGLE_USE = Mechanics.VEIN_MINER + ".single-use";

    private static final String HIDDEN_ANY_SIDE = Mechanics.HIDDEN_SWITCH + ".any-side";

    private static final String FALLING_LADDERS = Mechanics.BETTER_PHYSICS + ".falling-ladders";

    private static final String SNOW_PILING = Mechanics.SNOW + ".piling";
    private static final String SNOW_DISPERSION = Mechanics.SNOW + ".dispersion";
    private static final String SNOW_FREEZES = Mechanics.SNOW + ".freezes-water";
    private static final String SNOW_MELTS = Mechanics.SNOW + ".melts-in-sunlight";
    private static final String SNOW_PARTIAL_MELT = Mechanics.SNOW + ".partial-melt-only";
    private static final String SNOWBALLS_PILE = Mechanics.SNOW + ".snowballs-pile";

    private final BlockNames names;
    private final Consumer<String> report;

    /**
     * @param names how a written block name becomes a block
     * @param report where to send a complaint about an entry that could not be understood
     */
    public MechanicsDocument(BlockNames names, Consumer<String> report) {
        this.names = names;
        this.report = report;
    }

    /**
     * Fills in what the file was missing, says what everything is for, and reads it.
     *
     * <p>The tree is left holding a complete file, for the caller to write back out.
     */
    public MechanicSettings applyTo(ConfigTree tree) {
        fillIn(tree);
        explain(tree);
        return build(tree);
    }

    /** Puts the default of every setting the file does not already carry into it. */
    private static void fillIn(ConfigTree tree) {
        MechanicSettings defaults = MechanicSettings.DEFAULTS;
        setIfAbsent(tree, REDSTONE, defaults.redstone());
        setIfAbsent(tree, DEPOWER_ON_REMOVAL, defaults.depowerOnSourceRemoval());

        // Every mechanic, so the file is also the list of what there is. A section is created by
        // its enabled line whether or not the mechanic has anything else to say.
        for (String mechanic : Mechanics.ALL) {
            setIfAbsent(tree, mechanic + "." + ENABLED, defaults.allows(mechanic));
        }

        ChairSettings chairs = defaults.chair();
        setIfAbsent(tree, CHAIR_BLOCKS, new ArrayList<>(ChairSettings.DEFAULT_BLOCK_NAMES));
        setIfAbsent(tree, CHAIR_REQUIRE_SIGN, chairs.requireSign());
        setIfAbsent(tree, CHAIR_SIGN_DISTANCE, chairs.maxSignDistance());
        setIfAbsent(tree, CHAIR_FACING, chairs.faceCorrectDirection());
        setIfAbsent(tree, CHAIR_EXIT_AT_ENTRY, chairs.exitAtEntry());
        setIfAbsent(tree, CHAIR_HEAL_AMOUNT, chairs.healAmount());
        setIfAbsent(tree, CHAIR_HEAL_RATE, chairs.healRate());

        HeadSettings heads = defaults.heads();
        setIfAbsent(tree, HEAD_PLAYERS, heads.playerHeads());
        setIfAbsent(tree, HEAD_MOBS, heads.mobHeads());
        setIfAbsent(tree, HEAD_PLAYER_KILLS, heads.playerKillsOnly());
        setIfAbsent(tree, HEAD_DROP_RATE, heads.dropRate());
        setIfAbsent(tree, HEAD_LOOTING, heads.lootingRateModifier());
        setIfAbsent(tree, HEAD_SHOW_NAME, heads.showNameOnClick());
        setIfAbsent(tree, HEAD_IGNORED, new ArrayList<>(heads.ignoredNames()));

        GateSettings gate = defaults.gate();
        setIfAbsent(tree, GATE_BLOCKS, written(gate.blocks()));
        setIfAbsent(tree, GATE_RADIUS, gate.radius());
        setIfAbsent(tree, GATE_CLICKING, gate.clicking());

        ElevatorSettings lifts = defaults.elevator();
        setIfAbsent(tree, LIFT_JUMPING, lifts.jumping());
        setIfAbsent(tree, LIFT_BUTTONS, lifts.buttons());
        setIfAbsent(tree, LIFT_TOLERANCE, lifts.tolerance());

        AreaSettings areas = defaults.area();
        setIfAbsent(tree, MAX_AREA_BLOCKS, areas.maxBlocks());
        setIfAbsent(tree, MAX_AREAS, areas.maxPerNamespace());

        PowerableSettings powerables = defaults.powerables();
        setIfAbsent(tree, GLOWSTONE_OFF, powerables.glowstoneOffBlock().asString());
        setIfAbsent(tree, FIRE_BLOCKS, written(powerables.fireBlocks()));

        LightSwitchSettings switches = defaults.lightSwitch();
        setIfAbsent(tree, LIGHT_SWITCH_RANGE, switches.range());
        setIfAbsent(tree, LIGHT_SWITCH_LIGHTS, switches.maxLights());

        MeterSettings meters = defaults.meters();
        setIfAbsent(tree, AMMETER_ITEM, meters.ammeterItem().asString());
        setIfAbsent(tree, LIGHT_STONE_ITEM, meters.lightStoneItem().asString());

        BounceSettings bounces = defaults.bounce();
        setIfAbsent(tree, BOUNCE_BLOCKS, written(bounces.blocks()));
        bounces.automatic().forEach((block, throwing) ->
                setIfAbsent(tree, AUTO_BOUNCE_BLOCKS + "." + block.value(), throwing));
        setIfAbsent(tree, BOUNCE_SENSITIVITY, bounces.sensitivity());

        TeleporterSettings teleporters = defaults.teleporter();
        setIfAbsent(tree, TELEPORTER_BUTTONS, teleporters.buttons());
        setIfAbsent(tree, TELEPORTER_REQUIRE_SIGN, teleporters.requireSign());
        setIfAbsent(tree, TELEPORTER_RANGE, teleporters.range());

        XpSettings xp = defaults.xp();
        setIfAbsent(tree, XP_BLOCK, xp.block().asString());
        setIfAbsent(tree, XP_PER_BOTTLE, xp.perBottle());
        setIfAbsent(tree, XP_REQUIRES_BOTTLE, xp.requiresBottle());
        setIfAbsent(tree, XP_SNEAK, xp.sneaking().written());

        TreeSettings felling = defaults.tree();
        LopperSettings trees = felling.lopper();
        setIfAbsent(tree, TREE_BLOCKS, new ArrayList<>(LopperSettings.DEFAULT_TREE_BLOCKS));
        setIfAbsent(tree, TREE_TOOLS, new ArrayList<>(LopperSettings.DEFAULT_AXES));
        setIfAbsent(tree, TREE_MAX, trees.maxSize());
        setIfAbsent(tree, TREE_DIAGONALS, trees.diagonals());
        setIfAbsent(tree, TREE_ANY, trees.anyListedBlock());
        setIfAbsent(tree, TREE_SINGLE_USE, trees.singleUse());
        setIfAbsent(tree, TREE_LEAVES, new ArrayList<>(LopperSettings.DEFAULT_LEAVES));
        setIfAbsent(tree, TREE_BREAK_LEAVES, felling.breakLeaves());
        setIfAbsent(tree, TREE_SAPLINGS, felling.placeSaplings());

        LopperSettings veins = defaults.vein();
        setIfAbsent(tree, VEIN_BLOCKS, new ArrayList<>(LopperSettings.DEFAULT_VEIN_BLOCKS));
        setIfAbsent(tree, VEIN_TOOLS, new ArrayList<>(LopperSettings.DEFAULT_PICKAXES));
        setIfAbsent(tree, VEIN_MAX, veins.maxSize());
        setIfAbsent(tree, VEIN_DIAGONALS, veins.diagonals());
        setIfAbsent(tree, VEIN_ANY, veins.anyListedBlock());
        setIfAbsent(tree, VEIN_SINGLE_USE, veins.singleUse());

        setIfAbsent(tree, HIDDEN_ANY_SIDE, defaults.hiddenSwitchAnySide());
        setIfAbsent(tree, FALLING_LADDERS, defaults.fallingLadders());

        for (DispenserRecipe recipe : DispenserRecipe.values()) {
            setIfAbsent(tree, machineKey(recipe), defaults.dispensers().allows(recipe));
        }

        SnowSettings snow = defaults.snow();
        setIfAbsent(tree, SNOW_PILING, snow.piling());
        setIfAbsent(tree, SNOW_DISPERSION, snow.dispersion());
        setIfAbsent(tree, SNOW_FREEZES, snow.freezesWater());
        setIfAbsent(tree, SNOW_MELTS, snow.meltsInSunlight());
        setIfAbsent(tree, SNOW_PARTIAL_MELT, snow.partialMeltOnly());
        setIfAbsent(tree, SNOWBALLS_PILE, snow.snowballsPile());
    }

    private static void setIfAbsent(ConfigTree tree, String path, Object value) {
        if (!tree.has(path)) {
            tree.set(path, value);
        }
    }

    /** Says what each setting is for, in the file itself. */
    private static void explain(ConfigTree tree) {
        tree.header(List.of(
                "CraftBook Ultimate: the mechanics.",
                "",
                "A mechanic is something built in the world rather than written on an IC sign: a",
                "bridge, a gate, a lift, a block that answers redstone. One section each, named",
                "the way it is switched on, and every mechanic has one whether or not it has",
                "anything else to configure.",
                "",
                "EVERY MECHANIC STARTS SWITCHED OFF. Set enabled to true in a section to turn that",
                "one on. A fresh server therefore behaves exactly as the game does until you have",
                "said otherwise, which matters because a mechanic answers to ordinary blocks:",
                "turning Chairs on makes every stair a seat, and turning HeadDrops on changes what",
                "every death leaves behind. Neither is something a builder opted into.",
                "",
                "Switching one off again leaves everything already built where it is and stops it",
                "working. Nothing is ever removed by switching a mechanic off.",
                "",
                "How wide a bridge or a door may be, how far it may run and what it may be made of",
                "are not here. Those are the same limits the building chips use and live under ics",
                "in config.yml.",
                "",
                "Changes take effect on the next /craftbook reload."));

        tree.comment(REDSTONE, List.of(
                "Whether redstone arriving beside a sign works the mechanic on it.",
                "Power arriving shuts it and power leaving opens it, so a mechanic on a lever",
                "always agrees with the lever."));

        tree.comment(DEPOWER_ON_REMOVAL, List.of(
                "",
                "Whether a powered block goes out when the redstone feeding it is mined away.",
                "Off by default, and deliberately: powering a light and then mining the redstone",
                "is how a builder makes one that stays on. Switching a lever off still turns it",
                "off either way, this being only about the source being taken out of the world.",
                "",
                "GlowStone, JackOLantern and Netherrack all read it."));

        tree.comment(Mechanics.AMMETER, List.of(
                "",
                "Reads how much redstone power a block is carrying, held up to it in the hand."));

        tree.comment(AMMETER_ITEM, List.of("What is held up to the block."));

        tree.comment(Mechanics.AREA, List.of(
                "",
                "Swaps a whole saved region in and out. An area is saved with /area save and lives",
                "in the areas folder, one file of blocks and one saying where they go."));

        tree.comment(MAX_AREA_BLOCKS, List.of(
                "The most blocks one saved area may hold. Zero is no limit."));

        tree.comment(MAX_AREAS, List.of(
                "The most areas any one name may have saved. Zero is no limit.",
                "Saving over an area that already exists does not count against this."));

        tree.comment(Mechanics.BANNER_COPIER, List.of(
                "",
                "Hands out copies of the banner beside its sign, one blank banner at a time."));

        tree.comment(Mechanics.BOOK_COPIER, List.of(
                "",
                "Hands out copies of the written book in the item frame beside its sign."));

        tree.comment(Mechanics.BOUNCE_BLOCKS, List.of(
                "",
                "Throws whoever jumps on it."));

        tree.comment(BOUNCE_BLOCKS, List.of(
                "What throws somebody, when a [Jump] sign under it says how hard."));

        tree.comment(AUTO_BOUNCE_BLOCKS, List.of(
                "What throws somebody with no sign at all, and how hard. The throw is written the",
                "way a sign writes it: a number for straight up, three for a push along the",
                "ground, and a leading ! to ignore which way the jumper is facing."));

        tree.comment(BOUNCE_SENSITIVITY, List.of(
                "How much of a jump counts as one. Lower notices a smaller hop."));

        tree.comment(Mechanics.BRIDGE, List.of(
                "",
                "Runs a walkway out across a gap and pulls it back again."));

        tree.comment(Mechanics.CHAIRS, List.of(
                "",
                "Sits somebody down on a stair. Nothing is built: any block below seats whoever",
                "right-clicks it with an empty hand and stays an ordinary block for every other",
                "purpose. What holds them up is an invisible marker the game rides, so stopping",
                "the plugin leaves nobody stuck."));

        tree.comment(CHAIR_BLOCKS, List.of(
                "What may be sat on. An entry is a block name, a tag written with a leading #",
                "such as #minecraft:stairs, or a name from before the flattening. A stair laid",
                "upside down is never a chair, whatever this says."));

        tree.comment(CHAIR_REQUIRE_SIGN, List.of(
                "Whether a chair only works with a sign beside it. Off, so any allowed block is a",
                "chair; on, a builder has to say which ones are."));

        tree.comment(CHAIR_SIGN_DISTANCE, List.of(
                "How far from the block that was clicked that sign may be. The search follows a",
                "run of the same block, so a bench of eight stairs can share one sign at its end."));

        tree.comment(CHAIR_FACING, List.of(
                "Whether sitting down turns somebody to face the way the block does, so a row of",
                "chairs all faces the same way rather than however each person walked up."));

        tree.comment(CHAIR_EXIT_AT_ENTRY, List.of(
                "Whether standing up puts somebody back where they sat down from. Off, they are",
                "left where the chair was, on the nearest footing that will hold them."));

        tree.comment(CHAIR_HEAL_AMOUNT, List.of(
                "How much a chair with a [Sit Heal] sign heals by each turn, in half hearts.",
                "Zero stops healing chairs working without stopping ordinary ones."));

        tree.comment(CHAIR_HEAL_RATE, List.of(
                "How many ticks apart those turns are. 20 is a second."));

        tree.comment(Mechanics.DOOR, List.of(
                "",
                "Fills a doorway from its lintel or its threshold."));

        tree.comment(Mechanics.ELEVATOR, List.of(
                "",
                "Carries people between floors. Builds nothing, so none of the building limits",
                "reach it."));

        tree.comment(LIFT_JUMPING, List.of(
                "Whether jumping and crouching work a [Lift UpDown] sign fixed to the block",
                "somebody is standing on."));

        tree.comment(LIFT_BUTTONS, List.of(
                "Whether a button two blocks in front of a lift's sign works it."));

        tree.comment(LIFT_TOLERANCE, List.of(
                "How far a lift will drop somebody below the far sign to find them a floor.",
                "Beyond this it says there is no floor rather than dropping them down a shaft."));

        tree.comment(Mechanics.GATE, List.of(
                "",
                "Drops a curtain of fence or pane from its lintel. The one mechanic made of",
                "something other than what its sign says, which is why it has a material list."));

        tree.comment(GATE_BLOCKS, List.of(
                "What a gate may be made of. An entry is a block name, a tag written with a",
                "leading # such as #minecraft:fences, or a name from before the flattening.",
                "The glass, iron and nether gate signs each take only their own material out of",
                "this list; the plain sign takes any of it."));

        tree.comment(GATE_RADIUS, List.of(
                "How far around its sign a gate looks for its own material. The D forms of each",
                "gate sign ignore this and look barely past themselves, which is how two gates",
                "standing side by side are kept from catching one another."));

        tree.comment(GATE_CLICKING, List.of(
                "Whether a gate whose sign ends in C answers to a hand on its own fence."));

        tree.comment(Mechanics.GLOW_STONE, List.of(
                "",
                "Glowstone that goes dark rather than away."));

        tree.comment(GLOWSTONE_OFF, List.of(
                "What a glowstone looks like while it is dark. Powering it turns it back into",
                "glowstone, and taking the power away turns it into this."));

        tree.comment(Mechanics.HEAD_DROPS, List.of(
                "",
                "Drops the head of whatever was killed. Nothing is built and nothing has a sign,",
                "so switching this on changes what every death in the world leaves behind.",
                "",
                "The game has a head of its own for a player, a zombie, a creeper, a skeleton, a",
                "wither skeleton, a dragon and a piglin. Every other creature drops a player head",
                "wearing its face, which is the only way the game has of showing one. Those faces",
                "are fetched from Mojang the first time each is handed out, so a server with no",
                "way out to the internet gets a blank head instead."));

        tree.comment(HEAD_PLAYERS, List.of(
                "Whether a player killed drops their own head."));

        tree.comment(HEAD_MOBS, List.of(
                "Whether a creature killed drops its head."));

        tree.comment(HEAD_PLAYER_KILLS, List.of(
                "Whether only a death a player caused drops anything. Off, a creature that falls",
                "into its own cactus leaves a head behind as well."));

        tree.comment(HEAD_DROP_RATE, List.of(
                "How likely a head is, from 0 to 1. 0.05 is one death in twenty."));

        tree.comment(HEAD_LOOTING, List.of(
                "What each level of looting adds to that. A chance of 0.05 with a modifier of",
                "0.05 and a looting III sword comes to 0.20."));

        tree.comment(HEAD_SHOW_NAME, List.of(
                "Whether right-clicking a head already placed says whose it is."));

        tree.comment(HEAD_IGNORED, List.of(
                "Accounts whose head is never handed out or named, whatever killed them.",
                "The one here belongs to a library that uses a head as a marker in the world."));

        tree.comment(Mechanics.JACK_O_LANTERN, List.of(
                "",
                "A carved pumpkin that lights up while it is powered."));

        tree.comment(Mechanics.LIGHT_STONE, List.of(
                "",
                "Reads how much light is falling on a block, held up to it in the hand."));

        tree.comment(LIGHT_STONE_ITEM, List.of("What is held up to the block."));

        tree.comment(Mechanics.LIGHT_SWITCH, List.of(
                "",
                "Turns every torch within reach on or off at once. Both numbers are ceilings",
                "rather than what a switch always uses: a sign may ask for less, and asking for",
                "more gets as much as it is allowed."));

        tree.comment(LIGHT_SWITCH_RANGE, List.of(
                "How far a light switch reaches out for torches. A sign may ask for less on its",
                "third line."));

        tree.comment(LIGHT_SWITCH_LIGHTS, List.of(
                "The most torches one light switch turns. A sign may ask for fewer on its fourth",
                "line."));

        tree.comment(Mechanics.MAP_COPIER, List.of(
                "",
                "Hands out the map numbered on its own sign, one blank map at a time."));

        tree.comment(Mechanics.MARQUEE, List.of(
                "",
                "A sign that tells whoever clicks it what a variable says. Line 3 names the",
                "variable and line 4 the namespace, which is the shared one when it is blank.",
                "",
                "It only ever reads. The variables themselves are made with /var, and a sign",
                "naming one nobody has made is refused as it is written."));

        tree.comment(Mechanics.NETHERRACK, List.of(
                "",
                "A block that catches light on top of itself while it is powered."));

        tree.comment(FIRE_BLOCKS, List.of(
                "What catches light. The block is never changed; what changes is the air above",
                "it."));

        tree.comment(Mechanics.SIGN_COPIER, List.of(
                "",
                "Copies what one sign says onto another."));

        tree.comment(Mechanics.TREE_LOPPER, List.of(
                "",
                "Fells a whole tree from one log of it. Nothing is built and nothing carries a",
                "sign: an axe in the hand and a log in front of it are the whole declaration.",
                "The vein miner below is the same mechanic pointed at ores, so the settings are",
                "the same settings and mean the same things."));

        tree.comment(TREE_BLOCKS, List.of(
                "What counts as part of a tree. An entry is a block name or a tag written with a",
                "leading # such as #minecraft:logs, which gains a wood a later version of the",
                "game adds without this file being touched."));

        tree.comment(TREE_TOOLS, List.of(
                "What has to be in the hand. Nothing happens with an empty hand or a shovel, so",
                "a builder taking one log out of a wall still takes one log."));

        tree.comment(TREE_MAX, List.of(
                "The most blocks one swing takes, counting the log that was actually broken.",
                "Zero switches the mechanic off. A tree larger than this is felled as far as the",
                "limit, nearest blocks first, so the rest is still standing to swing at again."));

        tree.comment(TREE_DIAGONALS, List.of(
                "Whether logs touching only at an edge or a corner count as the same tree. Off,",
                "since two trees grown up against one another share corners and felling both",
                "from one swing is rarely what was meant."));

        tree.comment(TREE_ANY, List.of(
                "Whether a swing takes any block on the list rather than only more of the one",
                "that was broken. Off, so felling an oak leaves the spruce growing against it."));

        tree.comment(TREE_SINGLE_USE, List.of(
                "Whether the whole tree costs the axe one point of wear rather than one a log."));

        tree.comment(TREE_LEAVES, List.of(
                "What counts as leaves, for the setting below."));

        tree.comment(TREE_BREAK_LEAVES, List.of(
                "Whether the leaves come down with the trunk. Off, and worth leaving off on a",
                "busy server: a canopy is far more blocks than a trunk and it is what the",
                "max-size limit will be spent on."));

        tree.comment(TREE_SAPLINGS, List.of(
                "Whether a sapling is put back where the trunk stood. Which sapling is worked",
                "out from the wood that was felled, so a wood added later replants itself."));

        tree.comment(Mechanics.VEIN_MINER, List.of(
                "",
                "Mines a whole seam from one ore of it: the tree lopper pointed downward, and the",
                "same code underneath, so every setting here means what it means above.",
                "",
                "New in this plugin rather than ported from either CraftBook, so nothing about it",
                "is fixed by what an old world already contains."));

        tree.comment(VEIN_BLOCKS, List.of(
                "What counts as a seam. Tags are expanded, so #minecraft:copper_ores covers the",
                "stone and the deepslate forms both."));

        tree.comment(VEIN_TOOLS, List.of(
                "What has to be in the hand."));

        tree.comment(VEIN_MAX, List.of(
                "The most blocks one swing takes. Zero switches the mechanic off."));

        tree.comment(VEIN_DIAGONALS, List.of(
                "Whether ores touching only at an edge or a corner count as one seam. Worth more",
                "here than for trees, since the game scatters a seam rather than stacking it."));

        tree.comment(VEIN_ANY, List.of(
                "Whether a swing takes any block on the list rather than only more of the one",
                "that was broken. Off, so mining iron leaves the gold beside it. Turning it on",
                "is chiefly worth it for a seam that crosses from stone into deepslate, since",
                "the game gives those two halves different names."));

        tree.comment(VEIN_SINGLE_USE, List.of(
                "Whether the whole seam costs the pickaxe one point of wear rather than one an",
                "ore."));

        tree.comment(Mechanics.HIDDEN_SWITCH, List.of(
                "",
                "A lever hidden behind the wall it works. An [X] sign goes on the back of a block",
                "and the levers or buttons touching that sign are what get thrown; from the front",
                "there is a plain wall.",
                "",
                "A key may be named on the sign's first line, and then the switch only answers to",
                "somebody holding one."));

        tree.comment(HIDDEN_ANY_SIDE, List.of(
                "Whether clicking any side of the block works the switch, rather than only the",
                "side opposite the sign."));

        tree.comment(Mechanics.BETTER_PHYSICS, List.of(
                "",
                "Ladders that fall when what they stood on goes away, so mining the bottom rung",
                "of a shaft brings the rest of the ladder down instead of leaving it floating."));

        tree.comment(FALLING_LADDERS, List.of(
                "Whether ladders fall. The only thing this mechanic does, and it keeps its own",
                "line because both CraftBooks had one."));

        tree.comment(Mechanics.DISPENSER_RECIPES, List.of(
                "",
                "A dispenser loaded in a pattern that makes it do something other than dispense.",
                "Nothing is consumed beyond one of every stack, so a machine goes on working",
                "until its stacks run out.",
                "",
                "One switch each rather than one for the lot, since these are six unrelated",
                "things that happen to be built the same way."));

        tree.comment(machineKey(DispenserRecipe.CANNON), List.of(
                "Throws a lit stick of dynamite. TNT in the middle, gunpowder in the sides, fire",
                "charges in the corners."));

        tree.comment(machineKey(DispenserRecipe.FAN), List.of(
                "Blows whatever is in front of it away, up to five blocks and weaker with each.",
                "A piston in the middle, leaves in the sides, cobwebs in the corners."));

        tree.comment(machineKey(DispenserRecipe.VACUUM), List.of(
                "The fan turned round. A sticky piston in the middle and the rest as the fan."));

        tree.comment(machineKey(DispenserRecipe.FIRE_ARROWS), List.of(
                "Shoots an arrow that is on fire. An arrow in the middle, fire charges in the",
                "sides, the corners empty."));

        tree.comment(machineKey(DispenserRecipe.SNOW_SHOOTER), List.of(
                "Shoots a snowball. A potion in the middle, snow in the sides."));

        tree.comment(machineKey(DispenserRecipe.XP_SHOOTER), List.of(
                "Throws a bottle of experience. A glass bottle in the middle, redstone in the",
                "sides."));

        tree.comment(Mechanics.SNOW, List.of(
                "",
                "Snow that piles, slumps and melts. Every part of it below is off as well as the",
                "mechanic itself, so turning this on is two decisions: enabled says the mechanic",
                "runs, and each line below says which of its behaviours it does. Nothing here is",
                "built and nothing has a sign, so all of it changes every snowy block at once."));

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

        tree.comment(Mechanics.TELEPORTER, List.of(
                "",
                "Sends whoever clicks it somewhere else in the same world."));

        tree.comment(TELEPORTER_BUTTONS, List.of(
                "Whether a button on the far side of a teleporter sign works it, so a builder can",
                "hide the sign behind the wall."));

        tree.comment(TELEPORTER_REQUIRE_SIGN, List.of(
                "Whether the far end needs a teleporter sign of its own. On, this means a",
                "teleporter can only send somebody where a builder has said they may arrive."));

        tree.comment(TELEPORTER_RANGE, List.of(
                "How far a teleporter may send somebody. A negative number is no limit."));

        tree.comment(Mechanics.XP_STORER, List.of(
                "",
                "Turns the experience somebody is carrying into bottles."));

        tree.comment(XP_BLOCK, List.of("What is clicked to do the bottling."));

        tree.comment(XP_PER_BOTTLE, List.of(
                "How much experience one bottle costs. Whatever will not pay for a whole bottle",
                "stays with the player rather than being lost."));

        tree.comment(XP_REQUIRES_BOTTLE, List.of(
                "Whether the player has to be carrying empty bottles for it to fill."));

        tree.comment(XP_SNEAK, List.of(
                "Whether the player must be crouching to use one: must, must-not or either."));
    }

    /** Turns what the file says into the settings the mechanics read. */
    private MechanicSettings build(ConfigTree tree) {
        MechanicSettings defaults = MechanicSettings.DEFAULTS;
        return MechanicSettings.builder()
                .enabled(enabled(tree))
                .redstone(tree.bool(REDSTONE, defaults.redstone()))
                .depowerOnSourceRemoval(
                        tree.bool(DEPOWER_ON_REMOVAL, defaults.depowerOnSourceRemoval()))
                .chair(chair(tree, defaults.chair()))
                .heads(heads(tree, defaults.heads()))
                .gate(gate(tree, defaults.gate()))
                .elevator(elevator(tree, defaults.elevator()))
                .area(area(tree, defaults.area()))
                .powerables(powerables(tree, defaults.powerables()))
                .lightSwitch(lightSwitch(tree, defaults.lightSwitch()))
                .meters(meters(tree, defaults.meters()))
                .bounce(bounce(tree, defaults.bounce()))
                .teleporter(teleporter(tree, defaults.teleporter()))
                .xp(xp(tree, defaults.xp()))
                .snow(snow(tree, defaults.snow()))
                .tree(felling(tree, defaults.tree()))
                .vein(mining(tree, defaults.vein()))
                .dispensers(dispensers(tree, defaults.dispensers()))
                .hiddenSwitchAnySide(
                        tree.bool(HIDDEN_ANY_SIDE, defaults.hiddenSwitchAnySide()))
                .fallingLadders(tree.bool(FALLING_LADDERS, defaults.fallingLadders()))
                .build();
    }

    /**
     * The mechanics whose own section says they run.
     *
     * <p>Only the mechanics that exist are asked about, so a section an operator has invented or
     * one left behind by a mechanic that has been renamed cannot switch anything on.
     *
     * <p>A mechanic the file does not mention at all falls back to the default, which is off. That
     * matters for a file an operator has trimmed by hand and for a mechanic added by a version
     * newer than the file: neither starts working on its own.
     */
    private static Set<String> enabled(ConfigTree tree) {
        MechanicSettings defaults = MechanicSettings.DEFAULTS;
        Set<String> on = new LinkedHashSet<>();
        for (String mechanic : Mechanics.ALL) {
            if (tree.bool(mechanic + "." + ENABLED, defaults.allows(mechanic))) {
                on.add(mechanic);
            }
        }
        return on;
    }

    private ChairSettings chair(ConfigTree tree, ChairSettings defaults) {
        Set<Key> blocks = names.blocks(tree.strings(CHAIR_BLOCKS), report);
        return new ChairSettings(
                blocks.isEmpty() ? defaults.blocks() : blocks,
                tree.bool(CHAIR_REQUIRE_SIGN, defaults.requireSign()),
                tree.integer(CHAIR_SIGN_DISTANCE, defaults.maxSignDistance()),
                tree.bool(CHAIR_FACING, defaults.faceCorrectDirection()),
                tree.bool(CHAIR_EXIT_AT_ENTRY, defaults.exitAtEntry()),
                tree.number(CHAIR_HEAL_AMOUNT, defaults.healAmount()),
                tree.integer(CHAIR_HEAL_RATE, defaults.healRate()));
    }

    private static HeadSettings heads(ConfigTree tree, HeadSettings defaults) {
        List<String> ignored = tree.strings(HEAD_IGNORED);
        return new HeadSettings(
                tree.bool(HEAD_PLAYERS, defaults.playerHeads()),
                tree.bool(HEAD_MOBS, defaults.mobHeads()),
                tree.bool(HEAD_PLAYER_KILLS, defaults.playerKillsOnly()),
                tree.number(HEAD_DROP_RATE, defaults.dropRate()),
                tree.number(HEAD_LOOTING, defaults.lootingRateModifier()),
                tree.bool(HEAD_SHOW_NAME, defaults.showNameOnClick()),
                Set.copyOf(ignored));
    }

    private GateSettings gate(ConfigTree tree, GateSettings defaults) {
        Set<Key> blocks = names.blocks(tree.strings(GATE_BLOCKS), report);
        return new GateSettings(
                blocks.isEmpty() ? defaults.blocks() : blocks,
                tree.integer(GATE_RADIUS, defaults.radius()),
                tree.bool(GATE_CLICKING, defaults.clicking()));
    }

    private static ElevatorSettings elevator(ConfigTree tree, ElevatorSettings defaults) {
        return new ElevatorSettings(
                tree.bool(LIFT_JUMPING, defaults.jumping()),
                tree.bool(LIFT_BUTTONS, defaults.buttons()),
                tree.integer(LIFT_TOLERANCE, defaults.tolerance()));
    }

    private static AreaSettings area(ConfigTree tree, AreaSettings defaults) {
        return new AreaSettings(
                tree.integer(MAX_AREA_BLOCKS, defaults.maxBlocks()),
                tree.integer(MAX_AREAS, defaults.maxPerNamespace()));
    }

    private PowerableSettings powerables(ConfigTree tree, PowerableSettings defaults) {
        Set<Key> fireBlocks = names.blocks(tree.strings(FIRE_BLOCKS), report);
        return new PowerableSettings(
                names.block(tree.text(GLOWSTONE_OFF, defaults.glowstoneOffBlock().asString()))
                        .orElseGet(defaults::glowstoneOffBlock),
                fireBlocks.isEmpty() ? defaults.fireBlocks() : fireBlocks);
    }

    private static LightSwitchSettings lightSwitch(
            ConfigTree tree, LightSwitchSettings defaults) {
        return new LightSwitchSettings(
                tree.integer(LIGHT_SWITCH_RANGE, defaults.range()),
                tree.integer(LIGHT_SWITCH_LIGHTS, defaults.maxLights()));
    }

    private MeterSettings meters(ConfigTree tree, MeterSettings defaults) {
        return new MeterSettings(
                names.item(tree.text(AMMETER_ITEM, defaults.ammeterItem().asString()))
                        .orElseGet(defaults::ammeterItem),
                names.item(tree.text(LIGHT_STONE_ITEM, defaults.lightStoneItem().asString()))
                        .orElseGet(defaults::lightStoneItem));
    }

    private BounceSettings bounce(ConfigTree tree, BounceSettings defaults) {
        Set<Key> blocks = names.blocks(tree.strings(BOUNCE_BLOCKS), report);

        // Each named block carries the throw it gives, in the same grammar a [Jump] sign uses.
        Map<Key, String> automatic = new LinkedHashMap<>();
        for (String written : tree.childrenOf(AUTO_BOUNCE_BLOCKS)) {
            Optional<Key> block = names.block(written);
            if (block.isEmpty()) {
                report.accept("No block called " + written + ", so nothing bounces off it");
                continue;
            }
            automatic.put(block.get(), tree.text(AUTO_BOUNCE_BLOCKS + "." + written, ""));
        }

        return new BounceSettings(
                blocks.isEmpty() ? defaults.blocks() : blocks,
                automatic.isEmpty() ? defaults.automatic() : automatic,
                tree.number(BOUNCE_SENSITIVITY, defaults.sensitivity()));
    }

    private static TeleporterSettings teleporter(
            ConfigTree tree, TeleporterSettings defaults) {
        return new TeleporterSettings(
                tree.bool(TELEPORTER_BUTTONS, defaults.buttons()),
                tree.bool(TELEPORTER_REQUIRE_SIGN, defaults.requireSign()),
                tree.number(TELEPORTER_RANGE, defaults.range()));
    }

    private XpSettings xp(ConfigTree tree, XpSettings defaults) {
        return new XpSettings(
                names.block(tree.text(XP_BLOCK, defaults.block().asString()))
                        .orElseGet(defaults::block),
                tree.integer(XP_PER_BOTTLE, defaults.perBottle()),
                tree.bool(XP_REQUIRES_BOTTLE, defaults.requiresBottle()),
                SneakState.of(tree.text(XP_SNEAK, defaults.sneaking().written()),
                        defaults.sneaking()));
    }

    /** The name a dispenser machine is switched off by. */
    private static String machineKey(DispenserRecipe recipe) {
        return Mechanics.DISPENSER_RECIPES + "." + recipe.settingName();
    }

    private TreeSettings felling(ConfigTree tree, TreeSettings defaults) {
        LopperSettings run = lopper(
                tree,
                defaults.lopper(),
                TREE_BLOCKS,
                TREE_TOOLS,
                TREE_MAX,
                TREE_DIAGONALS,
                TREE_ANY,
                TREE_SINGLE_USE);
        return new TreeSettings(
                run,
                names.blocks(tree.strings(TREE_LEAVES), report),
                tree.bool(TREE_BREAK_LEAVES, defaults.breakLeaves()),
                tree.bool(TREE_SAPLINGS, defaults.placeSaplings()));
    }

    private LopperSettings mining(ConfigTree tree, LopperSettings defaults) {
        return lopper(
                tree,
                defaults,
                VEIN_BLOCKS,
                VEIN_TOOLS,
                VEIN_MAX,
                VEIN_DIAGONALS,
                VEIN_ANY,
                VEIN_SINGLE_USE);
    }

    /**
     * One lopper run's settings, read the same way for the trees and for the seams.
     *
     * <p>An empty list is left empty rather than falling back to the defaults, unlike the gate's
     * materials: a list somebody has deliberately emptied means they want the mechanic to take
     * nothing, and the mechanic says so by not running at all.
     */
    private LopperSettings lopper(
            ConfigTree tree,
            LopperSettings defaults,
            String blocks,
            String tools,
            String max,
            String diagonals,
            String any,
            String singleUse) {
        return new LopperSettings(
                names.blocks(tree.strings(blocks), report),
                names.items(tree.strings(tools), report),
                tree.integer(max, defaults.maxSize()),
                tree.bool(diagonals, defaults.diagonals()),
                tree.bool(any, defaults.anyListedBlock()),
                tree.bool(singleUse, defaults.singleUse()));
    }

    private static DispenserSettings dispensers(ConfigTree tree, DispenserSettings defaults) {
        Set<DispenserRecipe> allowed = new LinkedHashSet<>();
        for (DispenserRecipe recipe : DispenserRecipe.values()) {
            if (tree.bool(machineKey(recipe), defaults.allows(recipe))) {
                allowed.add(recipe);
            }
        }
        return new DispenserSettings(allowed);
    }

    private static SnowSettings snow(ConfigTree tree, SnowSettings defaults) {
        return new SnowSettings(
                tree.bool(SNOW_PILING, defaults.piling()),
                tree.bool(SNOW_DISPERSION, defaults.dispersion()),
                tree.bool(SNOW_FREEZES, defaults.freezesWater()),
                tree.bool(SNOW_MELTS, defaults.meltsInSunlight()),
                tree.bool(SNOW_PARTIAL_MELT, defaults.partialMeltOnly()),
                tree.bool(SNOWBALLS_PILE, defaults.snowballsPile()));
    }

    /** The names to write for a set of blocks. */
    private static List<String> written(Set<Key> blocks) {
        List<String> names = new ArrayList<>(blocks.size());
        for (Key block : blocks) {
            names.add(block.asString());
        }
        return names;
    }
}
