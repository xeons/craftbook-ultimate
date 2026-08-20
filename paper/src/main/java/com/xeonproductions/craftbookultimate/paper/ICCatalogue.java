package com.xeonproductions.craftbookultimate.paper;

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
import com.xeonproductions.craftbookultimate.core.ic.gate.Routing;
import com.xeonproductions.craftbookultimate.core.ic.gate.Sensing;
import com.xeonproductions.craftbookultimate.core.ic.gate.Sensors;
import com.xeonproductions.craftbookultimate.core.ic.gate.Transport;
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

        return registry;
    }

    private static void registerSensing(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCM116", "MOB ABOVE?")
                .name("Mob Above")
                .description("Outputs high while a creature is standing above the sign's support.")
                .selfTriggeringModel("MCO116")
                .logic(Sensing::mobAbove)
                .build());

        registry.register(ICDefinition.builder("MCX116", "PLAYER ABOVE?")
                .name("Player Above")
                .description("Outputs high while a player is standing above the sign's support.")
                .selfTriggeringModel("MCZ116")
                .logic(Sensing::playerAbove)
                .build());

        registry.register(ICDefinition.builder("MCX117", "PLAYER BELOW?")
                .name("Player Below")
                .description("Outputs high while a player is standing below the sign's support.")
                .selfTriggeringModel("MCZ117")
                .logic(Sensing::playerBelow)
                .build());

        registry.register(ICDefinition.builder("MCX118", "PLAYER NEAR?")
                .name("Player Near")
                .description("Outputs high while a player is within range.")
                .selfTriggeringModel("MCZ118")
                .logic(Sensing::playerNear)
                .build());

        registry.register(ICDefinition.builder("MCX119", "MOB NEAR?")
                .name("Mob Near")
                .description("Outputs high while a creature is within range.")
                .selfTriggeringModel("MCZ119")
                .logic(Sensing::mobNear)
                .build());

        registry.register(ICDefinition.builder("MCX138", "ITEM NEAR?")
                .name("Item Near")
                .description("Outputs high while a matching stack is lying within range.")
                .restricted()
                .selfTriggeringModel("MCZ138")
                .logic(Sensing::itemNear)
                .build());

        registry.register(ICDefinition.builder("MCX139", "HELD ITEM NEAR?")
                .name("Held Item Near")
                .description("Outputs high while a player within range is holding a matching item.")
                .restricted()
                .selfTriggeringModel("MCZ139")
                .logic(Sensing::heldItemNear)
                .build());

        registry.register(ICDefinition.builder("MCX140", "IN AREA")
                .name("In Area")
                .description("Outputs high while something is inside a box measured from the sign.")
                .layout(PinLayout.UISO)
                .restricted()
                .selfTriggeringModel("MCU140")
                .logic(Sensing::inArea)
                .build());

        registry.register(ICDefinition.builder("MC1500", "PLAYER ONLINE?")
                .name("Player Online")
                .description("Outputs high while a named player is logged in.")
                .selfTriggeringModel("MC0500")
                .logic(Sensing::playerOnline)
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
                .logic(Spawners::entitySpawner)
                .build());

        registry.register(ICDefinition.builder("MCX201", "ITEM SPAWNER")
                .name("Item Spawner")
                .description("Drops items above itself, out of nothing.")
                .layout(PinLayout.AISO)
                .restricted()
                .aliases("MC1201")
                .logic(Spawners::itemSpawner)
                .build());
    }

    private static void registerContainers(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX202", "CHEST DISPENSER")
                .name("Chest Dispenser")
                .description("Drops items taken out of a nearby container.")
                .layout(PinLayout.AISO)
                .aliases("MC1202")
                .logic(Containers::chestDispenser)
                .build());

        registry.register(ICDefinition.builder("MCX203", "CHEST COLLECTOR")
                .name("Chest Collector")
                .description("Picks up dropped items and puts them in a nearby container.")
                .layout(PinLayout.AISO)
                .selfTriggeringModel("MCZ203")
                .logic(Containers::chestCollector)
                .build());
    }

    private static void registerProjectiles(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1240", "ARROW SHOOTER")
                .name("Arrow Shooter")
                .description("Shoots a single arrow out of the back of the sign.")
                .layout(PinLayout.AISO)
                .restricted()
                .logic(Projectiles::arrowShooter)
                .build());

        registry.register(ICDefinition.builder("MC1241", "ARROW BARRAGE")
                .name("Arrow Barrage")
                .description("Shoots five arrows out of the back of the sign.")
                .layout(PinLayout.AISO)
                .restricted()
                .logic(Projectiles::arrowBarrage)
                .build());

        registry.register(ICDefinition.builder("MCX242", "SNOW SHOOTER")
                .name("Snow Shooter")
                .description("Throws a single snowball.")
                .restricted()
                .logic(Projectiles::snowShooter)
                .build());

        registry.register(ICDefinition.builder("MCX243", "SNOW BARRAGE")
                .name("Snow Barrage")
                .description("Throws five snowballs.")
                .restricted()
                .logic(Projectiles::snowBarrage)
                .build());

        registry.register(ICDefinition.builder("MCX244", "EGG SHOOTER")
                .name("Egg Shooter")
                .description("Throws a single egg.")
                .restricted()
                .logic(Projectiles::eggShooter)
                .build());

        registry.register(ICDefinition.builder("MCX245", "EGG BARRAGE")
                .name("Egg Barrage")
                .description("Throws five eggs.")
                .restricted()
                .logic(Projectiles::eggBarrage)
                .build());

        registry.register(ICDefinition.builder("MCX246", "FIREBALL")
                .name("Fireball")
                .description("Launches a ghast fireball, aimed by the sign.")
                .restricted()
                .logic(Projectiles::fireballShooter)
                .build());
    }

    private static void registerLightning(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX255", "LIGHTNING")
                .name("Lightning")
                .description("Strikes one place with lightning.")
                .restricted()
                .logic(LightningChips::lightning)
                .build());

        registry.register(ICDefinition.builder("MC1203", "ZEUS BOLT")
                .name("Zeus Bolt")
                .description("Strikes an area with lightning, at a chance per block.")
                .layout(PinLayout.AISO)
                .restricted()
                .logic(() -> LightningChips.zeusBolt(THREAD_LOCAL_RANDOM))
                .build());

        registry.register(ICDefinition.builder("MCX256", "HOLY SMITE")
                .name("Holy Smite")
                .description("Strikes everything within range with lightning.")
                .restricted()
                .selfTriggeringModel("MCZ256")
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
                .logic(Combat::mobZapper)
                .build());

        registry.register(ICDefinition.builder("MCX133", "HUMANS ONLY")
                .name("Humans Only")
                .description("Removes everything but players from within range.")
                .layout(PinLayout.SISO)
                .restricted()
                .selfTriggeringModel("MCZ133")
                .logic(Combat::humansOnly)
                .build());

        registry.register(ICDefinition.builder("MCX131", "HIT PLAYER ABV")
                .name("Hit Player Above")
                .description("Hurts players standing above it.")
                .layout(PinLayout.UISO)
                .restricted()
                .selfTriggeringModel("MCU131")
                .logic(Combat::hitPlayerAbove)
                .build());

        registry.register(ICDefinition.builder("MCX132", "HIT MOB ABOVE")
                .name("Hit Mob Above")
                .description("Hurts creatures standing above it.")
                .layout(PinLayout.UISO)
                .restricted()
                .selfTriggeringModel("MCU132")
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
                .logic(Effects::potionArea)
                .build());

        registry.register(ICDefinition.builder("MCX250", "PARTICLE")
                .name("Particle")
                .description("Shows a particle, optionally offset from the sign.")
                .logic(Effects::particleEmitter)
                .build());

        registry.register(ICDefinition.builder("MC1250", "FIREWORKS")
                .name("Fireworks")
                .description("Sets off a firework.")
                .layout(PinLayout.AISO)
                .restricted()
                .logic(() -> Effects.fireworks(THREAD_LOCAL_RANDOM))
                .build());

        registry.register(ICDefinition.builder("MC1253", "FIREWORK")
                .name("Programmable Firework Display")
                .description("Plays a firework display from a script.")
                .layout(PinLayout.AISO)
                .restricted()
                .logic(FireworkDisplay::display)
                .build());
    }

    private static void registerBuffers(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1000", "REPEATER")
                .name("Repeater")
                .description("Repeats a redstone signal.")
                .layout(PinLayout.AISO)
                .logic(TimeChips::delayedRepeater)
                .build());

        registry.register(ICDefinition.builder("MC1001", "INVERTER")
                .name("Inverter")
                .description("Inverts a redstone signal.")
                .layout(PinLayout.AISO)
                .logic(TimeChips::delayedInverter)
                .build());
    }

    private static void registerLogic(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC3002", "AND")
                .name("And Gate")
                .description("Outputs high if all inputs are high.")
                .logic(LogicGates::and)
                .build());

        registry.register(ICDefinition.builder("MC3003", "NAND")
                .name("Nand Gate")
                .description("Outputs high if any input is low.")
                .logic(LogicGates::nand)
                .build());

        registry.register(ICDefinition.builder("MC3020", "XOR")
                .name("Xor Gate")
                .description("Outputs high if the inputs are different.")
                .logic(LogicGates::xor)
                .build());

        registry.register(ICDefinition.builder("MC3021", "XNOR")
                .name("Xnor Gate")
                .description("Outputs high if the inputs are the same.")
                .logic(LogicGates::xnor)
                .build());
    }

    private static void registerLatches(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC3030", "RS-NOR")
                .name("RS-Nor Latch")
                .description("A compact RS-Nor latch.")
                .logic(Latches::rsNorLatch)
                .build());

        // MC3031 and MC3033 have always behaved identically, so one implementation serves both
        // and each keeps its own number.
        registry.register(ICDefinition.builder("MC3033", "RS-NAND")
                .name("RS-Nand Latch")
                .description("A compact RS-Nand latch.")
                .logic(Latches::rsNandLatch)
                .build());

        registry.register(ICDefinition.builder("MC3031", "INV RS-NAND")
                .name("Inverse RS-Nand Latch")
                .description("A compact inverse RS-Nand latch.")
                .logic(Latches::rsNandLatch)
                .build());

        registry.register(ICDefinition.builder("MC3032", "JK FLIP")
                .name("JK Flip Flop")
                .description("A compact JK flip flop.")
                .logic(Latches::jkFlipFlop)
                .build());

        registry.register(ICDefinition.builder("MC3034", "EDGE-D")
                .name("Edge-Trigger D Flip Flop")
                .description("A compact edge-triggered D flip flop.")
                .logic(Latches::edgeTriggeredDFlipFlop)
                .build());

        registry.register(ICDefinition.builder("MC3036", "LEVEL-D")
                .name("Level-Trigger D Flip Flop")
                .description("A compact level-triggered D flip flop.")
                .logic(Latches::levelTriggeredDFlipFlop)
                .build());

        registry.register(ICDefinition.builder("MC1017", "RE T FLIP")
                .name("Toggle Flip Flop RE")
                .description("Toggles output on high.")
                .logic(() -> Latches.toggleFlipFlop(true))
                .build());

        registry.register(ICDefinition.builder("MC1018", "FE T FLIP")
                .name("Toggle Flip Flop FE")
                .description("Toggles output on low.")
                .logic(() -> Latches.toggleFlipFlop(false))
                .build());

        registry.register(ICDefinition.builder("MC3050", "COMBO")
                .name("Combination Lock")
                .description("Outputs high if the correct combination is entered.")
                .logic(Latches::combinationLock)
                .build());

        registry.register(ICDefinition.builder("MC3102", "COUNTER")
                .name("Counter")
                .description("Increments on redstone signal, outputs high on reaching the limit.")
                .logic(() -> Latches.counterFromSign(true))
                .build());

        registry.register(ICDefinition.builder("MC3101", "DOWN COUNTER")
                .name("Down Counter")
                .description("Decrements on redstone signal, outputs high on reaching zero.")
                .logic(() -> Latches.counterFromSign(false))
                .build());
    }

    private static void registerArithmetic(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC4000", "FULL ADDER")
                .name("Full Adder")
                .description("A compact full adder.")
                .layout(PinLayout.THREE_I_3O)
                .logic(Arithmetic::fullAdder)
                .build());

        registry.register(ICDefinition.builder("MC4010", "HALF ADDER")
                .name("Half Adder")
                .description("A compact half adder.")
                .layout(PinLayout.THREE_I_3O)
                .logic(Arithmetic::halfAdder)
                .build());

        registry.register(ICDefinition.builder("MC4100", "FULL SUBTR")
                .name("Full Subtractor")
                .description("A compact full subtractor.")
                .layout(PinLayout.THREE_I_3O)
                .logic(Arithmetic::fullSubtractor)
                .build());

        registry.register(ICDefinition.builder("MC4110", "HALF SUBTR")
                .name("Half Subtractor")
                .description("A compact half subtractor.")
                .layout(PinLayout.THREE_I_3O)
                .logic(Arithmetic::halfSubtractor)
                .build());
    }

    private static void registerRouting(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC4200", "DISPATCH")
                .name("Dispatcher")
                .description("Outputs the centre input on the selected outputs.")
                .layout(PinLayout.THREE_I_3O)
                .logic(Routing::dispatcher)
                .build());

        registry.register(ICDefinition.builder("MC3040", "MULTIPLEXER")
                .name("Multiplexer")
                .description("Outputs input 1 or 2 depending on the state of input 0.")
                .logic(Routing::multiplexer)
                .build());

        registry.register(ICDefinition.builder("MC4040", "DEMULTIPLEXER")
                .name("Demultiplexer 2-Bit")
                .description("Raises the output selected by the input.")
                .layout(PinLayout.THREE_I_5O)
                .logic(() -> Routing.demultiplexer(1, 2))
                .build());

        registry.register(ICDefinition.builder("MC1020", "RANDOM BIT")
                .name("Random Bit")
                .description("Randomly sets the output high.")
                .logic(ICCatalogue::randomBits)
                .build());

        registry.register(ICDefinition.builder("MC2020", "RANDOM 3")
                .name("Random 3-Bit")
                .description("Randomly sets the outputs high.")
                .layout(PinLayout.SI3O)
                .logic(ICCatalogue::randomBits)
                .build());

        registry.register(ICDefinition.builder("MC6020", "RANDOM 5")
                .name("Random 5-Bit")
                .description("Randomly sets the outputs high.")
                .layout(PinLayout.SI5O)
                .logic(ICCatalogue::randomBits)
                .build());
    }

    private static void registerBlockPlacers(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX207", "BRIDGE")
                .name("Bridge")
                .description("Places a set type and amount of blocks.")
                .layout(PinLayout.AISO)
                .requiresAuthorisation()
                .logic(() -> BlockPlacers.bridge(false))
                .build());

        registry.register(ICDefinition.builder("MCX209", "BRIDGE+")
                .name("Bridge+")
                .description("Places blocks, replacing whatever is already there.")
                .layout(PinLayout.AISO)
                .restricted()
                .logic(() -> BlockPlacers.bridge(true))
                .build());

        registry.register(ICDefinition.builder("MCX208", "DOOR")
                .name("Door")
                .description("Places a set type and amount of blocks.")
                .layout(PinLayout.AISO)
                .requiresAuthorisation()
                .logic(() -> BlockPlacers.door(false))
                .build());

        registry.register(ICDefinition.builder("MCX210", "DOOR+")
                .name("Door+")
                .description("Places blocks, replacing whatever is already there.")
                .layout(PinLayout.AISO)
                .restricted()
                .logic(() -> BlockPlacers.door(true))
                .build());

        registry.register(ICDefinition.builder("MCX206", "FLEX SET")
                .name("Flex Set")
                .description("Sets a block at a specified location.")
                .layout(PinLayout.AISO)
                .logic(BlockPlacers::flexSet)
                .build());

        registry.register(ICDefinition.builder("MC1207", "FLEX SET ADMIN")
                .name("Flex Set Admin")
                .description("Sets a block at a specified location, without paying for it.")
                .restricted()
                .logic(BlockPlacers::flexSetAdmin)
                .build());

        registry.register(ICDefinition.builder("MC1205", "SET ABOVE")
                .name("Set Block Above")
                .description("Sets a block above the IC block.")
                .restricted()
                .logic(BlockPlacers::setBlockAbove)
                .build());

        registry.register(ICDefinition.builder("MC1206", "SET BELOW")
                .name("Set Block Below")
                .description("Sets a block below the IC block.")
                .restricted()
                .logic(BlockPlacers::setBlockBelow)
                .build());
    }

    private static void registerWeather(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX233", "WEATHER CONTROL")
                .name("Simple Weather Control")
                .description("Turns the weather on for a set duration while the input is held.")
                .restricted()
                .logic(WeatherChips::simpleWeatherControl)
                .build());

        registry.register(ICDefinition.builder("MCT233", "WEATHER CTRL ADV")
                .name("Weather Control")
                .description("Sets rain and thunder using three inputs.")
                .restricted()
                .logic(WeatherChips::weatherControl)
                .build());

        registry.register(ICDefinition.builder("MC3231", "T CONTROL ADV")
                .name("Time Control Advanced")
                .description("Moves the world to the next morning or night when clocked.")
                .restricted()
                .logic(WeatherChips::timeControlAdvanced)
                .build());
    }

    private static void registerSensors(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1260", "SENSE WATER")
                .name("Water Sensor")
                .description("Outputs high if water is detected.")
                .logic(Sensors::waterSensor)
                .build());

        registry.register(ICDefinition.builder("MC1261", "SENSE LAVA")
                .name("Lava Sensor")
                .description("Outputs high if lava is detected.")
                .logic(Sensors::lavaSensor)
                .build());

        registry.register(ICDefinition.builder("MC1262", "SENSE LIGHT")
                .name("Light Sensor")
                .description("Outputs high if the specified light level is detected.")
                .logic(Sensors::lightSensor)
                .build());

        registry.register(ICDefinition.builder("MCX230", "IS IT RAIN")
                .name("Rain Sensor")
                .description("Outputs high while it is raining.")
                .logic(Sensors::rainSensor)
                .build());

        registry.register(ICDefinition.builder("MCX231", "IS IT A STORM")
                .name("Storm Sensor")
                .description("Outputs high while a thunderstorm is running.")
                .logic(Sensors::stormSensor)
                .build());

        registry.register(ICDefinition.builder("MCX205", "DETECT BLOCK")
                .name("Block Detector")
                .description("Detects a block above or below.")
                .layout(PinLayout.AISO)
                .logic(Sensors::blockDetector)
                .build());
    }

    private static void registerTimeChips(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1420", "CLOCK")
                .name("Clock")
                .description("Toggles its output every X ticks.")
                .selfTriggeringModel("MC0420")
                .logic(TimeChips::clock)
                .build());

        registry.register(ICDefinition.builder("MC1230", "SENSE DAY")
                .name("Daylight Sensor")
                .description("Outputs high while the world time is within the day.")
                .selfTriggeringModel("MC0230")
                .logic(TimeChips::daySensor)
                .build());

        registry.register(ICDefinition.builder("MCX027", "BETWEEN TIME")
                .name("Between Time")
                .description("Outputs high if the time is between the specified ticks.")
                .logic(TimeChips::betweenTime)
                .build());

        registry.register(ICDefinition.builder("MC1025", "TIME MODULUS")
                .name("World Time Modulus")
                .description("Outputs high when the world time mod X is at least Y.")
                .logic(TimeChips::worldTimeModulus)
                .build());

        registry.register(ICDefinition.builder("MC1026", "UNIX TIME")
                .name("Unix Time Modulus")
                .description("Outputs high when unix time mod X is at least Y.")
                .logic(TimeChips::unixTimeModulus)
                .build());

        registry.register(ICDefinition.builder("MCX010", "PULSE")
                .name("Pulse")
                .description("Sends a burst of pulses when triggered.")
                .logic(TimeChips::pulse)
                .build());

        registry.register(ICDefinition.builder("MCX011", "SIGNAL EXTENDER")
                .name("Signal Extender")
                .description("Holds the output high for a while after the input ends.")
                .logic(TimeChips::signalExtender)
                .build());
    }

    private static void registerControl(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX120", "COMMAND CTRL")
                .name("Command Controlled")
                .description("Follows a switch that anyone may throw by command.")
                .selfTriggeringModel("MCZ120")
                .logic(Control::commandControlled)
                .build());

        registry.register(ICDefinition.builder("MCX121", "PASSWORD CTRL")
                .name("Password Controlled")
                .description("Follows a switch that takes a password to throw.")
                .selfTriggeringModel("MCZ121")
                .logic(Control::passwordControlled)
                .build());

        registry.register(ICDefinition.builder("MC2022", "BITSHIFT")
                .name("Bit Shift")
                .description("Remembers a row of bits and rotates them along.")
                .logic(Control::bitShift)
                .build());

        registry.register(ICDefinition.builder("MCU440", "^MONOFLOP")
                .name("Monoflop")
                .description("Waits out a countdown, then turns on.")
                .layout(PinLayout.AISO)
                .logic(Control::monoflop)
                .build());

        registry.register(ICDefinition.builder("MCX295", "TRIGGER READER")
                .name("Trigger Reader")
                .description("Mirrors the redstone at somewhere else in the world.")
                .selfTriggeringModel("MCZ295")
                .logic(Control::triggerReader)
                .build());
    }

    private static void registerFarming(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX211", "TOGGLE BLOCK")
                .name("Toggle Block")
                .description("Swaps one block between two kinds as its input changes.")
                .layout(PinLayout.AISO)
                .logic(BlockSwappers::toggleBlock)
                .build());

        registry.register(ICDefinition.builder("MC1249", "BLOCK REPLACER")
                .name("Block Replacer")
                .description("Swaps a block between two kinds and lets the change spread outward.")
                .restricted()
                .logic(BlockSwappers::blockReplacer)
                .build());

        registry.register(ICDefinition.builder("MCX213", "HARVESTER")
                .name("Harvester")
                .description("Gathers a grown crop out of an area into nearby containers.")
                .layout(PinLayout.AISO)
                .requiresAuthorisation()
                .logic(BlockPlacers::harvester)
                .build());

        registry.register(ICDefinition.builder("MCX215", "AREA PLANTER")
                .name("Area Planter")
                .description("Plants dropped seeds across a field of ground.")
                .layout(PinLayout.AISO)
                .selfTriggeringModel("MCZ215")
                .logic(Farming::areaPlanter)
                .build());

        registry.register(ICDefinition.builder("MCX216", "PLANTER")
                .name("Planter")
                .description("Plants a dropped seed above the block the sign hangs on.")
                .layout(PinLayout.AISO)
                .selfTriggeringModel("MCZ216")
                .logic(Farming::planter)
                .build());
    }

    private static void registerWireless(ICRegistry registry) {
        registry.register(ICDefinition.builder("MC1110", "TRANSMITTER")
                .name("Wireless Transmitter")
                .description("Transmits a wireless redstone signal.")
                .layout(PinLayout.AIZO)
                .playerIdentityLine(Wireless.WIDE_BAND_LINE)
                .logic(Wireless::transmitter)
                .build());

        registry.register(ICDefinition.builder("MC1111", "RECEIVER")
                .name("Wireless Receiver")
                .description("Receives a wireless redstone signal.")
                .selfTriggeringModel("MC0111")
                .playerIdentityLine(Wireless.WIDE_BAND_LINE)
                .logic(Wireless::receiver)
                .build());

        registry.register(ICDefinition.builder("MC6543", "REDCODER")
                .name("Analog Transmitter")
                .description("Transmits a band per redstone power level.")
                .layout(PinLayout.AISO)
                .playerIdentityLine(Wireless.WIDE_BAND_LINE)
                .logic(Wireless::analogTransmitter)
                .build());
    }

    private static void registerTransport(ICRegistry registry) {
        registry.register(ICDefinition.builder("MCX112", "TRANSPORTER")
                .name("Transporter")
                .description("Sends whoever is standing on it to a named destination.")
                .restricted()
                .logic(Transport::transporter)
                .build());

        registry.register(ICDefinition.builder("MCU113", "DESTINATION")
                .name("Destination")
                .description("Receives whoever a transporter sends to its name.")
                .restricted()
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
