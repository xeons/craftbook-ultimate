// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic;

import static com.xeonproductions.craftbookultimate.core.ic.LineForms.block;
import static com.xeonproductions.craftbookultimate.core.ic.LineForms.blockPair;
import static com.xeonproductions.craftbookultimate.core.ic.LineForms.either;
import static com.xeonproductions.craftbookultimate.core.ic.LineForms.entity;
import static com.xeonproductions.craftbookultimate.core.ic.LineForms.itemFilter;
import static com.xeonproductions.craftbookultimate.core.ic.LineForms.offset;
import static com.xeonproductions.craftbookultimate.core.ic.LineForms.offsetAndBlock;
import static com.xeonproductions.craftbookultimate.core.ic.LineForms.variable;
import static com.xeonproductions.craftbookultimate.core.ic.LineSpec.optional;
import static com.xeonproductions.craftbookultimate.core.ic.LineSpec.required;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.gate.Arithmetic;
import com.xeonproductions.craftbookultimate.core.ic.gate.BlockPlacers;
import com.xeonproductions.craftbookultimate.core.ic.gate.BlockSwappers;
import com.xeonproductions.craftbookultimate.core.ic.gate.Combat;
import com.xeonproductions.craftbookultimate.core.ic.gate.Containers;
import com.xeonproductions.craftbookultimate.core.ic.gate.Control;
import com.xeonproductions.craftbookultimate.core.ic.gate.Effects;
import com.xeonproductions.craftbookultimate.core.ic.gate.Farming;
import com.xeonproductions.craftbookultimate.core.ic.gate.FireworkDisplay;
import com.xeonproductions.craftbookultimate.core.ic.gate.LightningChips;
import com.xeonproductions.craftbookultimate.core.ic.gate.Projectiles;
import com.xeonproductions.craftbookultimate.core.ic.gate.Spawners;
import com.xeonproductions.craftbookultimate.core.ic.gate.Latches;
import com.xeonproductions.craftbookultimate.core.ic.gate.LogicGates;
import com.xeonproductions.craftbookultimate.core.ic.gate.Messages;
import com.xeonproductions.craftbookultimate.core.ic.gate.Music;
import com.xeonproductions.craftbookultimate.core.ic.gate.Routing;
import com.xeonproductions.craftbookultimate.core.ic.gate.Sensing;
import com.xeonproductions.craftbookultimate.core.ic.gate.Sensors;
import com.xeonproductions.craftbookultimate.core.ic.gate.Transport;
import com.xeonproductions.craftbookultimate.core.ic.gate.VariableChips;
import com.xeonproductions.craftbookultimate.core.ic.gate.WeatherIllusions;
import com.xeonproductions.craftbookultimate.core.ic.gate.Wireless;
import com.xeonproductions.craftbookultimate.core.ic.gate.WeatherChips;
import com.xeonproductions.craftbookultimate.core.ic.gate.TimeChips;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NullMarked;

/**
 * The catalogue of chips this build offers.
 *
 * <p>Model numbers and shorthands are the text players have written on signs, so the entries here
 * are a compatibility surface rather than a matter of taste. Where two numbers name chips that do
 * the same thing, one definition carries both.
 *
 * <p>Only the chips whose behaviour needs nothing from the world appear so far. The ones that
 * place blocks, move entities or measure time are added as their platform support lands.
 */
@NullMarked
public final class ICCatalogue {

    private ICCatalogue() {}

    /** Builds a registry holding every chip this build offers. */
    public static ICRegistry build() {
        ICRegistry registry = new ICRegistry();

        registerBuffers(registry);
        registerLogic(registry);
        registerLatches(registry);
        registerArithmetic(registry);
        registerRouting(registry);
        registerTimeChips(registry);
        registerSensors(registry);
        registerWeather(registry);
        registerBlockPlacers(registry);
        registerFarming(registry);
        registerControl(registry);
        registerWireless(registry);
        registerTransport(registry);
        registerSpawners(registry);
        registerContainers(registry);
        registerProjectiles(registry);
        registerLightning(registry);
        registerCombat(registry);
        registerEffects(registry);
        registerSensing(registry);
        registerMessages(registry);
        registerWeatherIllusions(registry);
        registerMusic(registry);
        registerVariables(registry);

        return registry;
    }

    /**
     * The chips that read and change the values everything on the server shares.
     *
     * <p>These three come from upstream rather than from the fork this rewrite is otherwise
     * porting, so their model numbers are upstream's. Their sign grammar is upstream's too, which
     * is what lets a world built against it keep working.
     */
    private static void registerVariables(ICRegistry registry) {
        registry.register(ICDefinition.builder("VAR100", "VAR MODIFIER")
                .name("Variable Modifier")
                .description("Does a sum to a variable, such as adding one to it.")
                .layout(PinLayout.SISO)
                .thirdLine(required("the variable to change", variable()))
                .fourthLine(required("the sum to do, as function:amount such as +:1"))
                .logic(VariableChips::modifier)
                .build());

        registry.register(ICDefinition.builder("VAR170", "IS AT LEAST")
                .name("Is At Least")
                .description("Outputs high while a variable has reached a number.")
                .layout(PinLayout.SISO)
                .thirdLine(required("the variable to watch", variable()))
                .fourthLine(required("the number it must reach"))
                .logic(VariableChips::isAtLeast)
                .build());

        registry.register(ICDefinition.builder("VAR200", "ITEM COUNTER")
                .name("Item Counter")
                .description("Counts what is in the container above it into a variable.")
                .layout(PinLayout.SISO)
                .thirdLine(required("the variable to add the count to", variable()))
                .fourthLine(optional("what to count; blank counts everything", entity()))
                .logic(VariableChips::itemCounter)
                .build());
    }

    private static void registerSensing(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCM116", "MOB ABOVE?")
                .name("Mob Above")
                .description("Outputs high while a creature is standing above the sign's support.")
                .selfTriggeringModel("MCO116")
                .thirdLine(optional("what counts as a creature; blank means anything alive", entity()))
                .fourthLine(optional("how far to look"))
                .logic(Sensing::mobAbove)
                .build());

        registry.register(ICDefinition.builder("MCX116", "PLAYER ABOVE?")
                .name("Player Above")
                .description("Outputs high while a player is standing above the sign's support.")
                .selfTriggeringModel("MCZ116")
                .thirdLine(optional("which players; blank means anyone", entity()))
                .fourthLine(optional("radius[:height[:up]]"))
                .logic(Sensing::playerAbove)
                .build());

        registry.register(ICDefinition.builder("MCX117", "PLAYER BELOW?")
                .name("Player Below")
                .description("Outputs high while a player is standing below the sign's support.")
                .selfTriggeringModel("MCZ117")
                .thirdLine(optional("which players; blank means anyone", entity()))
                .fourthLine(optional("radius[:height[:up]]"))
                .logic(Sensing::playerBelow)
                .build());

        registry.register(ICDefinition.builder("MCX118", "PLAYER NEAR?")
                .name("Player Near")
                .description("Outputs high while a player is within range.")
                .selfTriggeringModel("MCZ118")
                .thirdLine(optional("which players; blank means anyone", entity()))
                .fourthLine(optional("how far to reach, defaulting to five blocks"))
                .logic(Sensing::playerNear)
                .build());

        registry.register(ICDefinition.builder("MCX119", "MOB NEAR?")
                .name("Mob Near")
                .description("Outputs high while a creature is within range.")
                .selfTriggeringModel("MCZ119")
                .thirdLine(optional("what counts; blank means anything alive", entity()))
                .fourthLine(optional("how far to reach"))
                .logic(Sensing::mobNear)
                .build());

        registry.register(ICDefinition.builder("MCX138", "ITEM NEAR?")
                .name("Item Near")
                .description("Outputs high while a matching stack is lying within range.")
                .restricted()
                .selfTriggeringModel("MCZ138")
                .thirdLine(required("one thing to check, or where the book is when reading from one",
                        either(itemFilter(), offset())))
                .fourthLine(optional("how far to reach, up to thirty blocks"))
                .logic(Sensing::itemNear)
                .build());

        registry.register(ICDefinition.builder("MC1265", "INV SNS ITM")
                .name("Item Not Near")
                .description("Outputs high while no matching stack is lying within range.")
                .selfTriggeringModel("MCZ265")
                .thirdLine(required("one thing to check, or where the book is when reading from one",
                        either(itemFilter(), offset())))
                .fourthLine(optional("how far to reach, up to thirty blocks"))
                .logic(Sensing::itemNotNear)
                .build());

        registry.register(ICDefinition.builder("MCX139", "HELD ITEM NEAR?")
                .name("Held Item Near")
                .description("Outputs high while a player within range is holding a matching item.")
                .restricted()
                .selfTriggeringModel("MCZ139")
                .thirdLine(required("one thing to check, or where the book is when reading from one",
                        either(itemFilter(), offset())))
                .fourthLine(optional("how far to reach, up to thirty blocks"))
                .logic(Sensing::heldItemNear)
                .build());

        registry.register(ICDefinition.builder("MCX140", "IN AREA")
                .name("In Area")
                .description("Outputs high while something is inside a box measured from the sign.")
                .layout(PinLayout.UISO)
                .restricted()
                .selfTriggeringModel("MCU140")
                .thirdLine(required("what to look for, with a rider after a +", entity()))
                .fourthLine(optional("width:height:length[/x:y:z]"))
                .logic(Sensing::inArea)
                .build());

        registry.register(ICDefinition.builder("MC1500", "PLAYER ONLINE?")
                .name("Player Online")
                .description("Outputs high while a named player is logged in.")
                .selfTriggeringModel("MC0500")
                .thirdLine(optional("the name to look for, matching anybody whose name contains it"))
                .logic(Sensing::playerOnline)
                .build());
    }

    private static void registerMusic(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX251", "SOUND EFFECT")
                .name("Sound Effect")
                .description("Plays one sound, named in full or by its shorthand.")
                .restricted()
                .thirdLine(required("the sound, as entity.creeper.primed or the shorthand ENCRPR"))
                .fourthLine(optional("an x:y:z offset from the sign"))
                .logic(Music::soundEffect)
                .build());

        registry.register(ICDefinition.builder("MCU700", "MELODY")
                .name("Melody")
                .description("Plays a MIDI file through an adjacent note block.")
                .layout(PinLayout.UISO)
                .restricted()
                .thirdLine(required("the MIDI file to play, or a name ending .p for a playlist"))
                .fourthLine(optional("flags separated by colons: loop, random"))
                .logic(() -> Music.melody(ThreadLocalRandom.current()))
                .build());

        registry.register(ICDefinition.builder("MCU705", "TUNE")
                .name("Tune")
                .description("Plays a tune written on the sign, through an adjacent note block.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(required("the tune, optionally with ticks between notes in front, as 3:0c2e2g2"))
                .fourthLine(optional("more of the same tune, run on from line 3"))
                .logic(Music::tune)
                .build());

        registry.register(ICDefinition.builder("MCU706", "JUKEBOX")
                .name("Jukebox")
                .description("Plays a record through an adjacent jukebox.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(required("the record's name as the game calls it, such as 13 or mellohi"))
                .logic(Music::jukebox)
                .build());
    }

    private static void registerWeatherIllusions(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX235", "FALSE WEATHER")
                .name("False Weather")
                .description("Shows rain to people it is not raining on.")
                .restricted()
                .thirdLine(optional("who sees it: blank for everybody here, p:Name or g:group"))
                .logic(WeatherIllusions::falseWeather)
                .build());

        registry.register(ICDefinition.builder("MCX236", "DIST FALSE RAIN")
                .name("Distance False Weather")
                .description("Shows rain to everybody standing within a distance of the sign.")
                .restricted()
                .selfTriggeringModel("MCZ236")
                .thirdLine(optional("how far, from one to a hundred and twenty-seven, defaulting to ten"))
                .fourthLine(optional("something to say as somebody walks into range"))
                .logic(WeatherIllusions::distanceFalseWeather)
                .build());

        registry.register(ICDefinition.builder("MCX237", "HIDE WEATHER")
                .name("Hide Weather")
                .description("Hides the rain from people it is raining on.")
                .restricted()
                .thirdLine(optional("who sees it: blank for everybody here, p:Name or g:group"))
                .logic(WeatherIllusions::hideWeather)
                .build());

        registry.register(ICDefinition.builder("MCX238", "DIST HIDE RAIN")
                .name("Distance Hide Weather")
                .description("Hides the rain from everybody standing within a distance of the sign.")
                .restricted()
                .selfTriggeringModel("MCZ238")
                .thirdLine(optional("how far, from one to a hundred and twenty-seven, defaulting to ten"))
                .fourthLine(optional("something to say as somebody walks into range"))
                .logic(WeatherIllusions::distanceHideWeather)
                .build());
    }

    private static void registerMessages(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1510", "MESSAGE PLAYER")
                .name("Player Messenger")
                .description("Says something to one named player, wherever they are.")
                .restricted()
                .thirdLine(required("the account to message"))
                .fourthLine(required("what to say"))
                .logic(Messages::playerMessenger)
                .build());

        registry.register(ICDefinition.builder("MC1511", "MESSAGE ALL")
                .name("Message All")
                .description("Says something to everybody online.")
                .restricted()
                .thirdLine(required("what to say"))
                .logic(Messages::messageAll)
                .build());

        registry.register(ICDefinition.builder("MCX512", "MESSAGENEARBY")
                .name("Message Nearby")
                .description("Says something to everybody standing within range.")
                .restricted()
                .thirdLine(required("what to say"))
                .fourthLine(optional("the rest of what to say"))
                .logic(Messages::messageNearby)
                .build());

        registry.register(ICDefinition.builder("MCX513", "NAMED NEARBY")
                .name("Message Named Nearby")
                .description("Says something to everybody within range, naming the nearest.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(required("who to tell; %p means the nearest player"))
                .fourthLine(required("what to say"))
                .logic(Messages::namedNearby)
                .build());

        registry.register(ICDefinition.builder("MCX515", "SERVER LOG")
                .name("Server Log")
                .description("Writes a line to the server's log.")
                .restricted()
                .thirdLine(required("the line to write to the log"))
                .logic(Messages::serverLog)
                .build());

        registry.register(ICDefinition.builder("MCX516", "S-LOG NEARBY")
                .name("Server Log Nearby")
                .description("Writes a line to the log naming the nearest player.")
                .restricted()
                .thirdLine(required("the line to write; %p becomes the nearest player"))
                .fourthLine(optional("the rest of the line"))
                .logic(Messages::serverLogNearby)
                .build());

        registry.register(ICDefinition.builder("MCX517", "S-LOG NEARBY+")
                .name("Server Log Nearby+")
                .description("Writes a line to the log naming everybody in range and how far off.")
                .restricted()
                .thirdLine(required("the line to write; %p and %a become the nearest player and how far"))
                .fourthLine(optional("the rest of the line"))
                .logic(Messages::serverLogNearbyPlus)
                .build());
    }

    private static void registerSpawners(ICRegistry registry) {
        // MC1200 asked a player for the entity in chat and kept it outside the sign; MCX200 puts
        // the same thing on the sign, so one implementation serves both and the sign is the record.
        registry.register(ICDefinition.builder("MCX200", "ENTITY SPAWNER")
                .name("Entity Spawner")
                .description("Spawns creatures above itself.")
                .layout(PinLayout.AISO)
                .restricted()
                .aliases("MC1200")
                .thirdLine(required("what to spawn", entity()))
                .fourthLine(optional("how many, defaulting to one"))
                .logic(Spawners::entitySpawner)
                .build());

        registry.register(ICDefinition.builder("MCX201", "ITEM SPAWNER")
                .name("Item Spawner")
                .description("Drops items above itself, out of nothing.")
                .layout(PinLayout.AISO)
                .restricted()
                .aliases("MC1201")
                .thirdLine(required("the item to drop"))
                .fourthLine(optional("how many, up to a stack, defaulting to one"))
                .logic(Spawners::itemSpawner)
                .build());
    }

    private static void registerContainers(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX202", "CHEST DISPENSER")
                .name("Chest Dispenser")
                .description("Drops items taken out of a nearby container.")
                .layout(PinLayout.AISO)
                .aliases("MC1202")
                .thirdLine(optional("the item; blank or -1 means any item"))
                .fourthLine(optional("amount, with an optional @x:y:z naming a container"))
                .logic(Containers::chestDispenser)
                .build());

        registry.register(ICDefinition.builder("MCX203", "CHEST COLLECTOR")
                .name("Chest Collector")
                .description("Picks up dropped items and puts them in a nearby container.")
                .layout(PinLayout.AISO)
                .selfTriggeringModel("MCZ203")
                .thirdLine(optional("the item to pick up; blank means any item"))
                .fourthLine(optional("range, with an optional :x:y:z naming a container"))
                .logic(Containers::chestCollector)
                .build());
    }

    private static void registerProjectiles(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1240", "ARROW SHOOTER")
                .name("Arrow Shooter")
                .description("Shoots a single arrow out of the back of the sign.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(optional("speed[:spread]"))
                .fourthLine(optional("a vertical velocity"))
                .logic(Projectiles::arrowShooter)
                .build());

        registry.register(ICDefinition.builder("MC1241", "ARROW BARRAGE")
                .name("Arrow Barrage")
                .description("Shoots five arrows out of the back of the sign.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(optional("speed[:spread]"))
                .fourthLine(optional("a vertical velocity"))
                .logic(Projectiles::arrowBarrage)
                .build());

        registry.register(ICDefinition.builder("MCX242", "SNOW SHOOTER")
                .name("Snow Shooter")
                .description("Throws a single snowball.")
                .restricted()
                .thirdLine(optional("speed[:spread]"))
                .fourthLine(optional("a vertical velocity"))
                .logic(Projectiles::snowShooter)
                .build());

        registry.register(ICDefinition.builder("MCX243", "SNOW BARRAGE")
                .name("Snow Barrage")
                .description("Throws five snowballs.")
                .restricted()
                .thirdLine(optional("speed[:spread]"))
                .fourthLine(optional("a vertical velocity"))
                .logic(Projectiles::snowBarrage)
                .build());

        registry.register(ICDefinition.builder("MCX244", "EGG SHOOTER")
                .name("Egg Shooter")
                .description("Throws a single egg.")
                .restricted()
                .thirdLine(optional("speed[:spread]"))
                .fourthLine(optional("a vertical velocity"))
                .logic(Projectiles::eggShooter)
                .build());

        registry.register(ICDefinition.builder("MCX245", "EGG BARRAGE")
                .name("Egg Barrage")
                .description("Throws five eggs.")
                .restricted()
                .thirdLine(optional("speed[:spread]"))
                .fourthLine(optional("a vertical velocity"))
                .logic(Projectiles::eggBarrage)
                .build());

        registry.register(ICDefinition.builder("MCX246", "FIREBALL")
                .name("Fireball")
                .description("Launches a ghast fireball, aimed by the sign.")
                .restricted()
                .thirdLine(optional("speed[:spread]"))
                .fourthLine(optional("rotation[:pitch], from -1 straight down to 1 straight up"))
                .logic(Projectiles::fireballShooter)
                .build());
    }

    private static void registerLightning(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX255", "LIGHTNING")
                .name("Lightning")
                .description("Strikes one place with lightning.")
                .restricted()
                .thirdLine(optional("how far above or below the sign's support to strike"))
                .logic(LightningChips::lightning)
                .build());

        registry.register(ICDefinition.builder("MC1203", "ZEUS BOLT")
                .name("Zeus Bolt")
                .description("Strikes an area with lightning, at a chance per block.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(optional("the reach, one number or x,y,z, optionally =x:y:z to move the middle"))
                .fourthLine(optional("the chance out of a hundred that any one block is struck"))
                .logic(() -> LightningChips.zeusBolt(THREAD_LOCAL_RANDOM))
                .build());

        registry.register(ICDefinition.builder("MCX256", "HOLY SMITE")
                .name("Holy Smite")
                .description("Strikes everything within range with lightning.")
                .restricted()
                .selfTriggeringModel("MCZ256")
                .fourthLine(optional("how far to reach, defaulting to five blocks"))
                .logic(LightningChips::holySmite)
                .build());
    }

    private static void registerCombat(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX130", "MOB ZAPPER")
                .name("Mob Zapper")
                .description("Removes creatures within range.")
                .layout(PinLayout.SISO)
                .restricted()
                .selfTriggeringModel("MCZ130")
                .thirdLine(optional("what to remove; blank means hostile mobs", entity()))
                .fourthLine(optional("how far to reach, defaulting to five blocks"))
                .logic(Combat::mobZapper)
                .build());

        registry.register(ICDefinition.builder("MCX133", "HUMANS ONLY")
                .name("Humans Only")
                .description("Removes everything but players from within range.")
                .layout(PinLayout.SISO)
                .restricted()
                .selfTriggeringModel("MCZ133")
                .fourthLine(optional("how far to reach, defaulting to five blocks"))
                .logic(Combat::humansOnly)
                .build());

        registry.register(ICDefinition.builder("MCX131", "HIT PLAYER ABV")
                .name("Hit Player Above")
                .description("Hurts players standing above it.")
                .layout(PinLayout.UISO)
                .restricted()
                .selfTriggeringModel("MCU131")
                .thirdLine(optional("which players; blank means anyone", entity()))
                .fourthLine(optional("how hard to hit"))
                .logic(Combat::hitPlayerAbove)
                .build());

        registry.register(ICDefinition.builder("MCX132", "HIT MOB ABOVE")
                .name("Hit Mob Above")
                .description("Hurts creatures standing above it.")
                .layout(PinLayout.UISO)
                .restricted()
                .selfTriggeringModel("MCU132")
                .thirdLine(optional("what to hit; blank means anything that is not a player", entity()))
                .fourthLine(optional("how hard to hit"))
                .logic(Combat::hitMobAbove)
                .build());
    }

    private static void registerEffects(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX146", "POTION AREA")
                .name("Potion Area")
                .description("Gives potion effects to whatever is in an area.")
                .layout(PinLayout.AISO)
                .restricted()
                .selfTriggeringModel("MCU146")
                .thirdLine(required("effect:seconds:strength, such as SP:5:1; INF never wears off"))
                .fourthLine(optional("range[:x:y:z][@filter]"))
                .logic(Effects::potionArea)
                .build());

        registry.register(ICDefinition.builder("MCX250", "PARTICLE")
                .name("Particle")
                .description("Shows a particle, optionally offset from the sign.")
                .thirdLine(required("the particle, with a block after a colon where it takes one"))
                .fourthLine(optional("an axis letter and a distance, such as Y3"))
                .logic(Effects::particleEmitter)
                .build());

        registry.register(ICDefinition.builder("MC1250", "FIREWORKS")
                .name("Fireworks")
                .description("Sets off a firework.")
                .layout(PinLayout.AISO)
                .restricted()
                .noLines()
                .logic(() -> Effects.fireworks(THREAD_LOCAL_RANDOM))
                .build());

        registry.register(ICDefinition.builder("MC1253", "FIREWORK")
                .name("Programmable Firework Display")
                .description("Plays a firework display from a script.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(required("the show, from the plugin's fireworks folder"))
                .fourthLine(optional("whether dropping the input cuts the show short"))
                .logic(FireworkDisplay::display)
                .build());
    }

    private static void registerBuffers(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1000", "REPEATER")
                .name("Repeater")
                .description("Repeats a redstone signal.")
                .layout(PinLayout.AISO)
                .thirdLine(optional("how long to delay, such as 20T or 2S; blank repeats at once"))
                .logic(TimeChips::delayedRepeater)
                .build());

        registry.register(ICDefinition.builder("MC1001", "INVERTER")
                .name("Inverter")
                .description("Inverts a redstone signal.")
                .layout(PinLayout.AISO)
                .thirdLine(optional("how long to delay, such as 20T or 2S; blank inverts at once"))
                .logic(TimeChips::delayedInverter)
                .build());
    }

    private static void registerLogic(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC3002", "AND")
                .name("And Gate")
                .description("Outputs high if all inputs are high.")
                .noLines()
                .inputs("one of the inputs", "one of the inputs", "one of the inputs")
                .outputs("high while every wired input is high")
                .logic(LogicGates::and)
                .build());

        registry.register(ICDefinition.builder("MC3003", "NAND")
                .name("Nand Gate")
                .description("Outputs high if any input is low.")
                .noLines()
                .inputs("one of the inputs", "one of the inputs", "one of the inputs")
                .outputs("low while every wired input is high")
                .logic(LogicGates::nand)
                .build());

        registry.register(ICDefinition.builder("MC3020", "XOR")
                .name("Xor Gate")
                .description("Outputs high if the inputs are different.")
                .noLines()
                .inputs("one of the inputs", "one of the inputs", "one of the inputs")
                .outputs("high while an odd number of wired inputs are high")
                .logic(LogicGates::xor)
                .build());

        registry.register(ICDefinition.builder("MC3021", "XNOR")
                .name("Xnor Gate")
                .description("Outputs high if the inputs are the same.")
                .noLines()
                .inputs("one of the inputs", "one of the inputs", "one of the inputs")
                .outputs("high while an even number of wired inputs are high")
                .logic(LogicGates::xnor)
                .build());
    }

    private static void registerLatches(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC3030", "RS-NOR")
                .name("RS-Nor Latch")
                .description("A compact RS-Nor latch.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .inputs("set", "reset", "reset as well; either one wins over set")
                .outputs("Q, the value being held")
                .logic(Latches::rsNorLatch)
                .build());

        // MC3031 and MC3033 have always behaved identically, so one implementation serves both
        // and each keeps its own number.
        registry.register(ICDefinition.builder("MC3033", "RS-NAND")
                .name("RS-Nand Latch")
                .description("A compact RS-Nand latch.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .inputs("the data, sampled on the clock", "the clock, which acts as it rises", "reset, which wins over everything")
                .outputs("Q, the value sampled")
                .logic(Latches::rsNandLatch)
                .build());

        registry.register(ICDefinition.builder("MC3031", "INV RS-NAND")
                .name("Inverse RS-Nand Latch")
                .description("A compact inverse RS-Nand latch.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .inputs("set, which wins over reset", "reset", "not read")
                .outputs("Q, the value being held")
                .logic(Latches::rsNandLatch)
                .build());

        registry.register(ICDefinition.builder("MC3032", "JK FLIP")
                .name("JK Flip Flop")
                .description("A compact JK flip flop.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .inputs("the clock, which acts as it falls", "J, which sets", "K, which resets")
                .outputs("Q, which toggles when J and K are both high")
                .logic(Latches::jkFlipFlop)
                .build());

        registry.register(ICDefinition.builder("MC3034", "EDGE-D")
                .name("Edge-Trigger D Flip Flop")
                .description("A compact edge-triggered D flip flop.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .inputs("the clock; the data is followed while this is high", "the data", "reset, applied after the sample")
                .outputs("Q, the value being followed")
                .logic(Latches::edgeTriggeredDFlipFlop)
                .build());

        registry.register(ICDefinition.builder("MC3036", "LEVEL-D")
                .name("Level-Trigger D Flip Flop")
                .description("A compact level-triggered D flip flop.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .inputs("the clock; the data is followed while this is high",
                        "the data",
                        "reset, applied after the sample")
                .outputs("Q, the value being followed")
                .logic(Latches::levelTriggeredDFlipFlop)
                .build());

        registry.register(ICDefinition.builder("MC1017", "RE T FLIP")
                .name("Toggle Flip Flop RE")
                .description("Toggles output on high.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .logic(() -> Latches.toggleFlipFlop(true))
                .build());

        registry.register(ICDefinition.builder("MC1018", "FE T FLIP")
                .name("Toggle Flip Flop FE")
                .description("Toggles output on low.")
                .fourthLine(optional("where the chip keeps its state; it writes this itself"))
                .logic(() -> Latches.toggleFlipFlop(false))
                .build());

        registry.register(ICDefinition.builder("MC3050", "COMBO")
                .name("Combination Lock")
                .description("Outputs high if the correct combination is entered.")
                .thirdLine(required("the combination, three characters where X means that input must be high"))
                .inputs("the first of the combination", "the second of the combination", "the third of the combination")
                .outputs("high while the combination on line 3 is entered")
                .logic(Latches::combinationLock)
                .build());

        registry.register(ICDefinition.builder("MC3102", "COUNTER")
                .name("Counter")
                .description("Increments on redstone signal, outputs high on reaching the limit.")
                .thirdLine(optional("the limit to count to, optionally followed by :INF to keep going"))
                .fourthLine(optional("where the chip keeps its total; it writes this itself"))
                .inputs("counts up a step", "resets the count to zero", "not read")
                .outputs("high on reaching the limit")
                .logic(() -> Latches.counterFromSign(true))
                .build());

        registry.register(ICDefinition.builder("MC3101", "DOWN COUNTER")
                .name("Down Counter")
                .description("Decrements on redstone signal, outputs high on reaching zero.")
                .thirdLine(optional("the limit to count to, optionally followed by :INF to keep going"))
                .fourthLine(optional("where the chip keeps its total; it writes this itself"))
                .inputs("counts down a step", "resets the count to the limit", "not read")
                .outputs("high on reaching zero")
                .logic(() -> Latches.counterFromSign(false))
                .build());
    }

    private static void registerArithmetic(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC4000", "FULL ADDER")
                .name("Full Adder")
                .description("A compact full adder.")
                .layout(PinLayout.THREE_I_3O)
                .noLines()
                .inputs("the carry in", "one addend", "the other addend")
                .outputs("the sum", "the carry out", "the carry out again, so it can feed two places")
                .logic(Arithmetic::fullAdder)
                .build());

        registry.register(ICDefinition.builder("MC4010", "HALF ADDER")
                .name("Half Adder")
                .description("A compact half adder.")
                .layout(PinLayout.THREE_I_3O)
                .noLines()
                .inputs("not read", "one addend", "the other addend")
                .outputs("the sum", "the carry out", "the carry out again, so it can feed two places")
                .logic(Arithmetic::halfAdder)
                .build());

        registry.register(ICDefinition.builder("MC4100", "FULL SUBTR")
                .name("Full Subtractor")
                .description("A compact full subtractor.")
                .layout(PinLayout.THREE_I_3O)
                .noLines()
                .inputs("the minuend", "the subtrahend", "the borrow in")
                .outputs("the difference", "the borrow out", "the borrow out again, so it can feed two places")
                .logic(Arithmetic::fullSubtractor)
                .build());

        registry.register(ICDefinition.builder("MC4110", "HALF SUBTR")
                .name("Half Subtractor")
                .description("A compact half subtractor.")
                .layout(PinLayout.THREE_I_3O)
                .noLines()
                .inputs("not read", "the minuend", "the subtrahend")
                .outputs("the difference", "the borrow out", "the borrow out again, so it can feed two places")
                .logic(Arithmetic::halfSubtractor)
                .build());
    }

    private static void registerRouting(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC4200", "DISPATCH")
                .name("Dispatcher")
                .description("Outputs the centre input on the selected outputs.")
                .layout(PinLayout.THREE_I_3O)
                .noLines()
                .inputs("the value to send", "sends it to output 2", "sends it to output 3")
                .outputs("the value, while nothing is selected", "the value, while input 2 is high", "the value, while input 3 is high")
                .logic(Routing::dispatcher)
                .build());

        registry.register(ICDefinition.builder("MC3040", "MULTIPLEXER")
                .name("Multiplexer")
                .description("Passes on one of two inputs, chosen by the third.")
                .noLines()
                .inputs("which input to pass on: high for input 2, low for input 3", "passed on while input 1 is high", "passed on while input 1 is low")
                .outputs("whichever input was chosen")
                .logic(Routing::multiplexer)
                .build());

        registry.register(ICDefinition.builder("MC4040", "DEMULTIPLEXER")
                .name("Demultiplexer 2-Bit")
                .description("Raises the output selected by the input.")
                .layout(PinLayout.THREE_I_5O)
                .noLines()
                .inputs("not read", "the low bit of the address", "the high bit of the address")
                .outputs("high while the address is 0", "high while the address is 1", "high while the address is 2", "high while the address is 3", "never raised")
                .logic(() -> Routing.demultiplexer(1, 2))
                .build());

        registry.register(ICDefinition.builder("MC2999", "MARQUEE")
                .name("Marquee")
                .description("Moves one raised output along its three outputs, a step per pulse.")
                .layout(PinLayout.SI3O)
                .thirdLine(optional("which output to start from"))
                .logic(Routing::marquee)
                .build());

        registry.register(ICDefinition.builder("MC1020", "RANDOM BIT")
                .name("Random Bit")
                .description("Randomly sets the output high.")
                .thirdLine(optional("max on its own, or min:max"))
                .logic(ICCatalogue::randomBits)
                .build());

        registry.register(ICDefinition.builder("MC2020", "RANDOM 3")
                .name("Random 3-Bit")
                .description("Randomly sets the outputs high.")
                .layout(PinLayout.SI3O)
                .thirdLine(optional("max on its own, or min:max"))
                .logic(ICCatalogue::randomBits)
                .build());

        registry.register(ICDefinition.builder("MC6020", "RANDOM 5")
                .name("Random 5-Bit")
                .description("Randomly sets the outputs high.")
                .layout(PinLayout.SI5O)
                .thirdLine(optional("max on its own, or min:max"))
                .logic(ICCatalogue::randomBits)
                .build());
    }

    private static void registerBlockPlacers(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX207", "BRIDGE")
                .name("Bridge")
                .description("Places a set type and amount of blocks.")
                .layout(PinLayout.AISO)
                .requiresAuthorisation()
                .thirdLine(required("the block to build from", block()))
                .fourthLine(required("width:length, with an optional :verticalOffset"))
                .logic(() -> BlockPlacers.bridge(false))
                .build());

        registry.register(ICDefinition.builder("MCX209", "BRIDGE+")
                .name("Bridge+")
                .description("Places blocks, replacing whatever is already there.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(required("the block to build from", block()))
                .fourthLine(required("width:length, with an optional :verticalOffset"))
                .logic(() -> BlockPlacers.bridge(true))
                .build());

        registry.register(ICDefinition.builder("MCX208", "DOOR")
                .name("Door")
                .description("Places a set type and amount of blocks.")
                .layout(PinLayout.AISO)
                .requiresAuthorisation()
                .thirdLine(required("the block to build from", block()))
                .fourthLine(required("width:height, with an optional :verticalOffset"))
                .logic(() -> BlockPlacers.door(false))
                .build());

        registry.register(ICDefinition.builder("MCX210", "DOOR+")
                .name("Door+")
                .description("Places blocks, replacing whatever is already there.")
                .layout(PinLayout.AISO)
                .restricted()
                .thirdLine(required("the block to build from", block()))
                .fourthLine(required("width:height, with an optional :verticalOffset"))
                .logic(() -> BlockPlacers.door(true))
                .build());

        registry.register(ICDefinition.builder("MCX206", "FLEX SET")
                .name("Flex Set")
                .description("Sets a block at a specified location.")
                .layout(PinLayout.AISO)
                .thirdLine(required("where to put it and what to put there", offsetAndBlock()))
                .fourthLine(optional("h to hold the block until the input drops"))
                .logic(BlockPlacers::flexSet)
                .build());

        registry.register(ICDefinition.builder("MC1207", "FLEX SET ADMIN")
                .name("Flex Set Admin")
                .description("Sets a block at a specified location, without paying for it.")
                .restricted()
                .thirdLine(required("where to put it and what to put there", offsetAndBlock()))
                .fourthLine(optional("h to hold the block until the input drops"))
                .logic(BlockPlacers::flexSetAdmin)
                .build());

        registry.register(ICDefinition.builder("MC1205", "SET ABOVE")
                .name("Set Block Above")
                .description("Sets a block above the IC block.")
                .restricted()
                .thirdLine(required("the block to place", block()))
                .fourthLine(optional("Force to replace whatever is already there"))
                .logic(BlockPlacers::setBlockAbove)
                .build());

        registry.register(ICDefinition.builder("MC1206", "SET BELOW")
                .name("Set Block Below")
                .description("Sets a block below the IC block.")
                .restricted()
                .thirdLine(required("the block to place", block()))
                .fourthLine(optional("Force to replace whatever is already there"))
                .logic(BlockPlacers::setBlockBelow)
                .build());
    }

    private static void registerWeather(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX233", "WEATHER CONTROL")
                .name("Simple Weather Control")
                .description("Turns the weather on for a set duration while the input is held.")
                .restricted()
                .thirdLine(optional("how long the weather lasts once started, defaulting to a full day"))
                .logic(WeatherChips::simpleWeatherControl)
                .build());

        registry.register(ICDefinition.builder("MCT233", "WEATHER CTRL ADV")
                .name("Weather Control")
                .description("Sets rain and thunder using three inputs.")
                .restricted()
                .noLines()
                .inputs("the clock, which acts as it rises", "asks for rain", "asks for thunder")
                .logic(WeatherChips::weatherControl)
                .build());

        registry.register(ICDefinition.builder("MC3231", "T CONTROL ADV")
                .name("Time Control Advanced")
                .description("Moves the world to the next morning or night when clocked.")
                .restricted()
                .noLines()
                .inputs("the clock, which acts as it rises",
                        "morning while high, night while low",
                        "not read")
                .logic(WeatherChips::timeControlAdvanced)
                .build());
    }

    private static void registerSensors(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1260", "SENSE WATER")
                .name("Water Sensor")
                .description("Outputs high if water is detected.")
                .thirdLine(optional("a vertical offset from the sign's support, defaulting to one"))
                .logic(Sensors::waterSensor)
                .build());

        registry.register(ICDefinition.builder("MC1261", "SENSE LAVA")
                .name("Lava Sensor")
                .description("Outputs high if lava is detected.")
                .thirdLine(optional("a vertical offset from the sign's support, defaulting to one"))
                .logic(Sensors::lavaSensor)
                .build());

        registry.register(ICDefinition.builder("MC1262", "SENSE LIGHT")
                .name("Light Sensor")
                .description("Outputs high if the specified light level is detected.")
                .thirdLine(optional("the light level to compare against, defaulting to eight"))
                .fourthLine(optional("a vertical offset from the sign's support"))
                .logic(Sensors::lightSensor)
                .build());

        registry.register(ICDefinition.builder("MCX230", "IS IT RAIN")
                .name("Rain Sensor")
                .description("Outputs high while it is raining.")
                .noLines()
                .logic(Sensors::rainSensor)
                .build());

        registry.register(ICDefinition.builder("MCX231", "IS IT A STORM")
                .name("Storm Sensor")
                .description("Outputs high while a thunderstorm is running.")
                .noLines()
                .logic(Sensors::stormSensor)
                .build());

        registry.register(ICDefinition.builder("MC1267", "SENSE MOVE")
                .name("Movement Sensor")
                .description("Outputs high while something within range is moving.")
                .selfTriggeringModel("MCZ267")
                .thirdLine(optional("what counts, defaulting to anything alive", entity()))
                .fourthLine(optional("how far to reach, up to ten blocks"))
                .logic(Sensing::movementNear)
                .build());

        registry.register(ICDefinition.builder("MCX205", "DETECT BLOCK")
                .name("Block Detector")
                .description("Detects a block above or below.")
                .layout(PinLayout.AISO)
                .thirdLine(required("the block to look for"))
                .fourthLine(optional("how far down to search"))
                .logic(Sensors::blockDetector)
                .build());
    }

    private static void registerTimeChips(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1420", "CLOCK")
                .name("Clock")
                .description("Toggles its output every X ticks.")
                .selfTriggeringModel("MC0420")
                .thirdLine(optional("the period in ticks, from 3 to 1000, defaulting to 20"))
                .fourthLine(optional("where the chip keeps its count; it writes this itself"))
                .logic(TimeChips::clock)
                .build());

        registry.register(ICDefinition.builder("MC1230", "SENSE DAY")
                .name("Daylight Sensor")
                .description("Outputs high while the world time is within the day.")
                .selfTriggeringModel("MC0230")
                .thirdLine(optional("the start of the window, in ticks through the day"))
                .fourthLine(optional("the end of the window"))
                .logic(TimeChips::daySensor)
                .build());

        registry.register(ICDefinition.builder("MCX027", "BETWEEN TIME")
                .name("Between Time")
                .description("Outputs high if the time is between the specified ticks.")
                .thirdLine(optional("the start, in ticks through the day"))
                .fourthLine(optional("the end, defaulting to the whole day"))
                .logic(TimeChips::betweenTime)
                .build());

        registry.register(ICDefinition.builder("MC1025", "TIME MODULUS")
                .name("World Time Modulus")
                .description("Outputs high when the world time mod X is at least Y.")
                .thirdLine(optional("the divisor, defaulting to 2"))
                .fourthLine(optional("the threshold, defaulting to 0"))
                .logic(TimeChips::worldTimeModulus)
                .build());

        registry.register(ICDefinition.builder("MC1026", "UNIX TIME")
                .name("Unix Time Modulus")
                .description("Outputs high when unix time mod X is at least Y.")
                .thirdLine(optional("the divisor, defaulting to 2"))
                .fourthLine(optional("the threshold, defaulting to 0"))
                .logic(TimeChips::unixTimeModulus)
                .build());

        registry.register(ICDefinition.builder("MCX010", "PULSE")
                .name("Pulse")
                .description("Sends a burst of pulses when triggered.")
                .thirdLine(optional("how long each pulse lasts in milliseconds, 100 to 1000"))
                .fourthLine(optional("how many pulses, 1 to 10"))
                .logic(TimeChips::pulse)
                .build());

        registry.register(ICDefinition.builder("MCX011", "SIGNAL EXTENDER")
                .name("Signal Extender")
                .description("Holds the output high for a while after the input ends.")
                .thirdLine(optional("how long to hold, such as 500, 20T or 2S"))
                .logic(TimeChips::signalExtender)
                .build());
    }

    private static void registerControl(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX120", "COMMAND CTRL")
                .name("Command Controlled")
                .description("Follows a switch that anyone may throw by command.")
                .selfTriggeringModel("MCZ120")
                .thirdLine(required("the switch to follow"))
                .logic(Control::commandControlled)
                .build());

        registry.register(ICDefinition.builder("MCX121", "PASSWORD CTRL")
                .name("Password Controlled")
                .description("Follows a switch that takes a password to throw.")
                .selfTriggeringModel("MCZ121")
                .thirdLine(required("the switch to follow"))
                .logic(Control::passwordControlled)
                .build());

        registry.register(ICDefinition.builder("MC2022", "BITSHIFT")
                .name("Bit Shift")
                .description("Remembers a row of bits and rotates them along.")
                .thirdLine(optional("how many bits, from 2 to 64, defaulting to eight"))
                .fourthLine(optional("the bits themselves; the chip writes these"))
                .inputs("rotates the row along, while input 2 is high",
                        "writes input 3 into the first bit, while input 1 is high",
                        "the bit being written")
                .outputs("the first bit of the row")
                .logic(Control::bitShift)
                .build());

        registry.register(ICDefinition.builder("MCU440", "^MONOFLOP")
                .name("Monoflop")
                .description("Waits out a countdown, then turns on.")
                .layout(PinLayout.AISO)
                .thirdLine(optional("count:rate, optionally followed by :onCount"))
                .logic(Control::monoflop)
                .build());

        registry.register(ICDefinition.builder("MC1266", "SENSE POWER")
                .name("Power Sensor")
                .description("Outputs high while power is arriving at somewhere else in the world.")
                .selfTriggeringModel("MCZ266")
                .thirdLine(required("a step from the sign", offset()))
                .logic(Control::powerSensor)
                .build());

        registry.register(ICDefinition.builder("MCX295", "TRIGGER READER")
                .name("Trigger Reader")
                .description("Mirrors the redstone at somewhere else in the world.")
                .selfTriggeringModel("MCZ295")
                .thirdLine(required("a step from the sign", offset()))
                .logic(Control::triggerReader)
                .build());
    }

    private static void registerFarming(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX211", "TOGGLE BLOCK")
                .name("Toggle Block")
                .description("Swaps one block between two kinds as its input changes.")
                .layout(PinLayout.AISO)
                .thirdLine(required("the two blocks it swaps between", blockPair('|')))
                .fourthLine(required("one axis step from the sign's support, such as Y+1"))
                .logic(BlockSwappers::toggleBlock)
                .build());

        registry.register(ICDefinition.builder("MC1249", "BLOCK REPLACER")
                .name("Block Replacer")
                .description("Swaps a block between two kinds and lets the change spread outward.")
                .restricted()
                .thirdLine(required("the two blocks it swaps between", blockPair('|')))
                .fourthLine(optional("delay:mode:physics"))
                .logic(BlockSwappers::blockReplacer)
                .build());

        registry.register(ICDefinition.builder("MCX213", "HARVESTER")
                .name("Harvester")
                .description("Gathers a grown crop out of an area into nearby containers.")
                .layout(PinLayout.AISO)
                .requiresAuthorisation()
                .thirdLine(required("the block to harvest"))
                .fourthLine(optional("width:length:height, with an optional /verticalOffset"))
                .logic(BlockPlacers::harvester)
                .build());

        registry.register(ICDefinition.builder("MCX215", "AREA PLANTER")
                .name("Area Planter")
                .description("Plants dropped seeds across a field of ground.")
                .layout(PinLayout.AISO)
                .selfTriggeringModel("MCZ215")
                .thirdLine(required("the crop to plant"))
                .fourthLine(optional("width:length, with an optional :height"))
                .logic(Farming::areaPlanter)
                .build());

        registry.register(ICDefinition.builder("MCX216", "PLANTER")
                .name("Planter")
                .description("Plants a dropped seed above the block the sign hangs on.")
                .layout(PinLayout.AISO)
                .selfTriggeringModel("MCZ216")
                .thirdLine(required("the item to plant"))
                .fourthLine(optional("how far above the sign's support, defaulting to one"))
                .logic(Farming::planter)
                .build());
    }

    private static void registerWireless(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1110", "TRANSMITTER")
                .name("Wireless Transmitter")
                .description("Transmits a wireless redstone signal.")
                .layout(PinLayout.AIZO)
                .playerIdentityLine(Wireless.WIDE_BAND_LINE)
                .thirdLine(required("the channel to transmit on"))
                .fourthLine(optional("a namespace around the channel; uuid means your own"))
                .logic(Wireless::transmitter)
                .build());

        registry.register(ICDefinition.builder("MC1111", "RECEIVER")
                .name("Wireless Receiver")
                .description("Receives a wireless redstone signal.")
                .selfTriggeringModel("MC0111")
                .playerIdentityLine(Wireless.WIDE_BAND_LINE)
                .thirdLine(required("the channel to follow"))
                .fourthLine(optional("a namespace around the channel; uuid means your own"))
                .logic(Wireless::receiver)
                .build());

        registry.register(ICDefinition.builder("MC6543", "REDCODER")
                .name("Analog Transmitter")
                .description("Transmits a band per redstone power level.")
                .layout(PinLayout.AISO)
                .playerIdentityLine(Wireless.WIDE_BAND_LINE)
                .thirdLine(required("channel[:first:last][:T]"))
                .fourthLine(optional("a namespace around the channel"))
                .inputs("power in; the strongest of the four is what is transmitted",
                        "power in; the strongest of the four is what is transmitted",
                        "power in; the strongest of the four is what is transmitted",
                        "power in; the strongest of the four is what is transmitted")
                .logic(Wireless::analogTransmitter)
                .build());

        registry.register(ICDefinition.builder("MC3456", "MARQUEETRANSMIT")
                .name("Marquee Transmitter")
                .description("Steps along a run of numbered bands, one at a time.")
                .playerIdentityLine(Wireless.WIDE_BAND_LINE)
                .thirdLine(required("channel:first:last"))
                .fourthLine(optional("a namespace around the channel"))
                .inputs("a pulse steps to the next band in the run",
                        "puts the run back to its beginning",
                        "holds the run where it is, however hard input 1 is pulsed")
                .logic(Wireless::marqueeTransmitter)
                .build());
    }

    private static void registerTransport(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX112", "TRANSPORTER")
                .name("Transporter")
                .description("Sends whoever is standing on it to a named destination.")
                .restricted()
                .thirdLine(required("the destination to send people to"))
                .logic(Transport::transporter)
                .build());

        registry.register(ICDefinition.builder("MCU113", "DESTINATION")
                .name("Destination")
                .description("Receives whoever a transporter sends to its name.")
                .restricted()
                .thirdLine(required("the name this destination answers to"))
                .logic(Transport::destination)
                .build());
    }

    /**
     * A generator that resolves to the calling thread's own stream on every draw.
     *
     * <p>Resolving per draw rather than once matters on a regionised server, where a chip is
     * created on one thread and may then run on the thread that owns its region. Each region ends
     * up drawing from its own stream instead of contending over a shared one.
     */
    private static final RandomGenerator THREAD_LOCAL_RANDOM = new RandomGenerator() {
        @Override
        public long nextLong() {
            return ThreadLocalRandom.current().nextLong();
        }

        @Override
        public int nextInt(int bound) {
            return ThreadLocalRandom.current().nextInt(bound);
        }
    };

    private static ICLogic randomBits() {
        return Routing.randomBitsFromSign(THREAD_LOCAL_RANDOM);
    }
}
