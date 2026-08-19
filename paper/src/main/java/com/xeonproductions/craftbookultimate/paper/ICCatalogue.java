package com.xeonproductions.craftbookultimate.paper;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.gate.Arithmetic;
import com.xeonproductions.craftbookultimate.core.ic.gate.Latches;
import com.xeonproductions.craftbookultimate.core.ic.gate.LogicGates;
import com.xeonproductions.craftbookultimate.core.ic.gate.Routing;
import com.xeonproductions.craftbookultimate.core.ic.gate.Sensors;
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

        return registry;
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
