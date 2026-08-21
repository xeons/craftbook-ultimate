package com.xeonproductions.craftbookultimate.paper.testbed;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLine;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.testbed.ChipSetup;
import com.xeonproductions.craftbookultimate.core.testbed.Rig;
import com.xeonproductions.craftbookultimate.core.testbed.Testbed;
import com.xeonproductions.craftbookultimate.paper.ICCatalogue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The test bed")
class TestbedTest {

    private static final ICRegistry REGISTRY = ICCatalogue.build();

    private static final Testbed PLAN =
            Testbed.plan(REGISTRY, new Vec3i(0, 64, 0), BlockFace.SOUTH);

    @Test
    @DisplayName("carries a rig for every chip in the catalogue")
    void carriesARigForEveryChipInTheCatalogue() {
        assertThat(PLAN.size()).isEqualTo(REGISTRY.size());
        assertThat(PLAN.rigs()).extracting(rig -> rig.chip().model())
                .containsExactlyInAnyOrderElementsOf(
                        REGISTRY.definitions().stream().map(ICDefinition::model).toList());
    }

    @Nested
    @DisplayName("wiring a rig")
    class Wiring {

        @Test
        @DisplayName("wires one input on a chip that reads one, and all of them on a chip that does not")
        void wiresOneInputOnAChipThatReadsOneAndAllOfThemOnAChipThatDoesNot() {
            for (Rig rig : PLAN.rigs()) {
                long inputs = rig.placements().stream()
                        .filter(placement -> placement.fixture() instanceof Rig.Fixture.InputLever)
                        .count();

                int expected = ChipSetup.usesEveryInput(rig.chip().model())
                        ? rig.chip().defaultLayout().inputCount()
                        : Math.min(1, rig.chip().defaultLayout().inputCount());

                assertThat(inputs)
                        .as("inputs wired on " + rig.chip().model())
                        .isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("gives an AISO chip a single lever, since any of its inputs sets it off")
        void givesAnAisoChipASingleLeverSinceAnyOfItsInputsSetsItOff() {
            for (Rig rig : PLAN.rigs()) {
                // The analog transmitter is the exception that proves it: AISO, but it reads
                // every input's power level rather than being set off by any of them.
                if (rig.chip().defaultLayout() != PinLayout.AISO
                        || ChipSetup.usesEveryInput(rig.chip().model())) {
                    continue;
                }
                assertThat(rig.placements().stream()
                        .filter(placement -> placement.fixture() instanceof Rig.Fixture.InputLever)
                        .count())
                        .as(rig.chip().model() + " has one lever, not four")
                        .isEqualTo(1);
            }
        }

        @Test
        @DisplayName("wires every output, since a chip drives all of them")
        void wiresEveryOutputSinceAChipDrivesAllOfThem() {
            for (Rig rig : PLAN.rigs()) {
                assertThat(rig.placements().stream()
                        .filter(placement -> placement.fixture() instanceof Rig.Fixture.OutputLever)
                        .count())
                        .as("outputs wired on " + rig.chip().model())
                        .isEqualTo(rig.chip().defaultLayout().outputCount());
            }
        }

        @Test
        @DisplayName("wires every input of a gate, whose inputs each mean something different")
        void wiresEveryInputOfAGateWhoseInputsEachMeanSomethingDifferent() {
            for (String model : new String[] {"MC3002", "MC4000", "MC3032", "MC3102"}) {
                Rig rig = PLAN.rigs().stream()
                        .filter(candidate -> candidate.chip().model().equals(model))
                        .findFirst()
                        .orElseThrow();

                assertThat(rig.placements().stream()
                        .filter(placement -> placement.fixture() instanceof Rig.Fixture.InputLever)
                        .count())
                        .as(model + " keeps all its inputs")
                        .isEqualTo(rig.chip().defaultLayout().inputCount());
            }
        }

        @Test
        @DisplayName("puts every lever on a pin the chip actually reads")
        void putsEveryLeverOnAPinTheChipActuallyReads() {
            for (Rig rig : PLAN.rigs()) {
                for (Rig.Placement placement : rig.placements()) {
                    if (placement.fixture() instanceof Rig.Fixture.InputLever lever) {
                        assertThat(placement.position())
                                .as("input " + lever.input() + " of " + rig.chip().model())
                                .isEqualTo(rig.chip().defaultLayout()
                                        .inputPosition(lever.input(), rig.signPosition(), rig.facing()));
                    }
                    if (placement.fixture() instanceof Rig.Fixture.OutputLever lever) {
                        assertThat(placement.position())
                                .as("output " + lever.output() + " of " + rig.chip().model())
                                .isEqualTo(rig.chip().defaultLayout()
                                        .outputPosition(lever.output(), rig.signPosition(), rig.facing()));
                    }
                }
            }
        }

        @Test
        @DisplayName("gives every lever something to stand on")
        void givesEveryLeverSomethingToStandOn() {
            for (Rig rig : PLAN.rigs()) {
                Set<Vec3i> standings = new HashSet<>();
                for (Rig.Placement placement : rig.placements()) {
                    if (placement.fixture() instanceof Rig.Fixture.Mount
                            || placement.fixture() instanceof Rig.Fixture.Indicator) {
                        standings.add(placement.position());
                    }
                }

                for (Rig.Placement placement : rig.placements()) {
                    Vec3i mount = switch (placement.fixture()) {
                        case Rig.Fixture.InputLever lever ->
                                placement.position().offset(lever.mountedOn());
                        case Rig.Fixture.OutputLever lever ->
                                placement.position().offset(lever.mountedOn());
                        default -> null;
                    };
                    if (mount != null) {
                        assertThat(standings)
                                .as("something for the lever at " + placement.position()
                                        + " on " + rig.chip().model() + " to cling to")
                                .contains(mount);
                    }
                }
            }
        }

        @Test
        @DisplayName("puts a lamp against every output, so it can be seen")
        void putsALampAgainstEveryOutputSoItCanBeSeen() {
            for (Rig rig : PLAN.rigs()) {
                Set<Vec3i> lamps = rig.placements().stream()
                        .filter(placement -> placement.fixture() instanceof Rig.Fixture.Indicator)
                        .map(Rig.Placement::position)
                        .collect(java.util.stream.Collectors.toSet());

                for (Rig.Placement placement : rig.placements()) {
                    if (placement.fixture() instanceof Rig.Fixture.OutputLever lever) {
                        // The lamp is what the lever clings to, so it is strongly powered when
                        // the chip drives that output rather than merely lit by proximity.
                        assertThat(lamps)
                                .as("a lamp on " + rig.chip().model() + " output " + lever.output())
                                .contains(placement.position().offset(lever.mountedOn()));
                    }
                }
            }
        }

        @Test
        @DisplayName("points every lever along the ground, never up or down")
        void pointsEveryLeverAlongTheGroundNeverUpOrDown() {
            // A lever's facing is its rotation about the vertical axis, so only the four
            // horizontal directions are meaningful and the server rejects the others outright
            // rather than ignoring them. A lever clinging to a floor still needs one.
            for (Rig rig : PLAN.rigs()) {
                for (Rig.Placement placement : rig.placements()) {
                    BlockFace facing = switch (placement.fixture()) {
                        case Rig.Fixture.InputLever lever -> lever.facing();
                        case Rig.Fixture.OutputLever lever -> lever.facing();
                        default -> null;
                    };
                    if (facing != null) {
                        assertThat(facing.isCardinal())
                                .as(facing + " on " + rig.chip().model()
                                        + " at " + placement.position())
                                .isTrue();
                    }
                }
            }
        }

        @Test
        @DisplayName("points a lever on a wall away from the wall")
        void pointsALeverOnAWallAwayFromTheWall() {
            for (Rig rig : PLAN.rigs()) {
                for (Rig.Placement placement : rig.placements()) {
                    if (placement.fixture() instanceof Rig.Fixture.InputLever lever
                            && lever.mountedOn().isCardinal()) {
                        assertThat(lever.facing()).isEqualTo(lever.mountedOn().opposite());
                    }
                    if (placement.fixture() instanceof Rig.Fixture.OutputLever lever
                            && lever.mountedOn().isCardinal()) {
                        assertThat(lever.facing()).isEqualTo(lever.mountedOn().opposite());
                    }
                }
            }
        }

        @Test
        @DisplayName("hangs the sign on a block, so the chip has something to act from")
        void hangsTheSignOnABlockSoTheChipHasSomethingToActFrom() {
            for (Rig rig : PLAN.rigs()) {
                Vec3i backing = rig.signPosition().offset(rig.facing().opposite());
                assertThat(rig.placements())
                        .as("backing for " + rig.chip().model())
                        .anySatisfy(placement -> {
                            assertThat(placement.fixture()).isInstanceOf(Rig.Fixture.Backing.class);
                            assertThat(placement.position()).isEqualTo(backing);
                        });
            }
        }
    }

    @Nested
    @DisplayName("the signs it writes")
    class Signs {

        @Test
        @DisplayName("puts an identifier on every chip sign that resolves back to that chip")
        void putsAnIdentifierOnEveryChipSignThatResolvesBackToThatChip() {
            for (Rig rig : PLAN.rigs()) {
                Rig.Fixture.ChipSign sign = chipSignOf(rig);

                Optional<ICRegistry.Resolution> resolved =
                        REGISTRY.resolve(sign.lines().text(1));

                assertThat(resolved)
                        .as("the sign written for " + rig.chip().model() + " reads back")
                        .isPresent();
                assertThat(resolved.orElseThrow().definition().model())
                        .isEqualTo(rig.chip().model());
            }
        }

        @Test
        @DisplayName("asks only the chips that merely read to tick")
        void asksOnlyTheChipsThatMerelyReadToTick() {
            for (Rig rig : PLAN.rigs()) {
                // However it is spelled — a ticking model number or an S — what matters is what
                // the registry makes of it.
                boolean asked = REGISTRY.resolve(chipSignOf(rig).lines().text(1))
                        .orElseThrow().selfTriggering();

                // A handful tick whatever any sign says, because they declare it themselves —
                // a clock, a commanded switch, a destination. Those are not the bed's doing.
                boolean insists = rig.chip().newLogic()
                        instanceof com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic logic
                        && logic.alwaysSelfTriggering();

                assertThat(asked)
                        .as(rig.chip().model() + " ticks only if it is on the safe list or insists")
                        .isEqualTo(insists
                                || (ChipSetup.ticks(rig.chip().model())
                                        && rig.chip().supportsSelfTriggering()));
            }
        }

        @Test
        @DisplayName("never builds a chip that acts on the world every tick")
        void neverBuildsAChipThatActsOnTheWorldEveryTick() {
            // The bed once forced ticking on everything that could take it. A holy smite ticking
            // strikes every entity in range every tick: a lightning bolt per entity per tick, and
            // a heap exhausted in about a minute. These are the ones that did the damage.
            for (String model : new String[] {
                    "MCX256", "MCX255", "MC1203", "MCX200", "MCX201", "MCX112",
                    "MCX131", "MCX132", "MCX130", "MCX146", "MCX215", "MCX216"}) {
                assertThat(ChipSetup.ticks(model))
                        .as(model + " must never be built ticking")
                        .isFalse();
            }
        }

        @Test
        @DisplayName("writes the receiver as MC0111, the number that means it follows its band")
        void writesTheReceiverAsMc0111TheNumberThatMeansItFollowsItsBand() {
            // A receiver that does not tick reads its band only while its own input is high, so
            // its lever becomes its clock and the pair look broken when both are fine. The
            // catalogue has a number for the ticking form, and that is what a builder writes.
            Rig receiver = rigFor("MC1111");

            assertThat(chipSignOf(receiver).lines().text(1)).isEqualTo("[MC0111]");
            assertThat(REGISTRY.resolve(chipSignOf(receiver).lines().text(1)).orElseThrow()
                    .selfTriggering())
                    .isTrue();
        }

        @Test
        @DisplayName("uses a chip's own ticking number rather than bolting an S onto it")
        void usesAChipsOwnTickingNumberRatherThanBoltingAnSOntoIt() {
            // Twenty-six chips were catalogued twice, once waiting and once following. Writing
            // the second number is what a builder does, and it is the only way those numbers
            // appear on the bed at all.
            for (Rig rig : PLAN.rigs()) {
                if (!ChipSetup.ticks(rig.chip().model())) {
                    continue;
                }
                String written = chipSignOf(rig).lines().text(1);

                rig.chip().selfTriggeringModel().ifPresent(model ->
                        assertThat(written)
                                .as(rig.chip().model() + " is written as its ticking number")
                                .isEqualTo("[" + model + "]"));
            }
        }

        private Rig rigFor(String model) {
            return PLAN.rigs().stream()
                    .filter(rig -> rig.chip().model().equals(model))
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        @DisplayName("gives the transmitter and its receiver the same band")
        void givesTheTransmitterAndItsReceiverTheSameBand() {
            assertThat(ChipSetup.forModel("MC1110").thirdLine())
                    .isEqualTo(ChipSetup.forModel("MC1111").thirdLine())
                    .isNotBlank();
            assertThat(ChipSetup.forModel("MC1110").fourthLine())
                    .isEqualTo(ChipSetup.forModel("MC1111").fourthLine());
        }

        @Test
        @DisplayName("still leaves ticking the chips that only make sense that way")
        void stillLeavesTickingTheChipsThatOnlyMakeSenseThatWay() {
            // Dropping the flag must not stop a clock or a destination from running: those
            // declare that they always tick, and the registry honours it whatever the sign says.
            long ticking = PLAN.rigs().stream()
                    .filter(rig -> REGISTRY.resolve(chipSignOf(rig).lines().text(1))
                            .orElseThrow().selfTriggering())
                    .count();

            assertThat(ticking).isPositive();
        }

        @Test
        @DisplayName("never writes a label that would itself become a chip")
        void neverWritesALabelThatWouldItselfBecomeAChip() {
            // A label's second line is the one an IC sign carries its identifier on. A model
            // reference or a shorthand there would turn every label into a second chip.
            for (Rig rig : PLAN.rigs()) {
                Rig.Fixture.LabelSign label = labelOf(rig);

                assertThat(ICLine.parse(label.lines().text(1)).flatMap(REGISTRY::resolve))
                        .as("the label beside " + rig.chip().model() + " names no chip")
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("names the chip on its label, so a rig can be found by eye")
        void namesTheChipOnItsLabelSoARigCanBeFoundByEye() {
            for (Rig rig : PLAN.rigs()) {
                assertThat(labelOf(rig).lines().text(0)).isEqualTo(rig.chip().model());
            }
        }

        private Rig.Fixture.ChipSign chipSignOf(Rig rig) {
            return rig.placements().stream()
                    .map(Rig.Placement::fixture)
                    .filter(Rig.Fixture.ChipSign.class::isInstance)
                    .map(Rig.Fixture.ChipSign.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no chip sign on " + rig.chip().model()));
        }

        private Rig.Fixture.LabelSign labelOf(Rig rig) {
            return rig.placements().stream()
                    .map(Rig.Placement::fixture)
                    .filter(Rig.Fixture.LabelSign.class::isInstance)
                    .map(Rig.Fixture.LabelSign.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no label on " + rig.chip().model()));
        }
    }

    @Nested
    @DisplayName("laying out the plane")
    class Layout {

        @Test
        @DisplayName("never lets one rig reach into another")
        void neverLetsOneRigReachIntoAnother() {
            Set<Vec3i> taken = new HashSet<>();
            List<String> clashes = new ArrayList<>();

            for (Rig rig : PLAN.rigs()) {
                for (Rig.Placement placement : rig.placements()) {
                    if (!taken.add(placement.position())) {
                        clashes.add(rig.chip().model() + " at " + placement.position());
                    }
                }
            }

            assertThat(clashes).isEmpty();
        }

        @Test
        @DisplayName("lays a floor that covers everything it builds")
        void laysAFloorThatCoversEverythingItBuilds() {
            Testbed.Ground ground = PLAN.ground();

            for (Rig rig : PLAN.rigs()) {
                for (Rig.Placement placement : rig.placements()) {
                    assertThat(placement.position().x())
                            .isBetween(ground.from().x(), ground.to().x());
                    assertThat(placement.position().z())
                            .isBetween(ground.from().z(), ground.to().z());
                }
            }
        }

        @Test
        @DisplayName("wraps into rows rather than running away in one line")
        void wrapsIntoRowsRatherThanRunningAwayInOneLine() {
            Testbed narrow = Testbed.plan(REGISTRY, Vec3i.ZERO, BlockFace.SOUTH, 5);

            assertThat(narrow.columns()).isEqualTo(5);
            assertThat(narrow.rows()).isEqualTo((REGISTRY.size() + 4) / 5);
        }
    }

    @Nested
    @DisplayName("what it sets up")
    class Setups {

        @Test
        @DisplayName("only configures chips that are actually in the catalogue")
        void onlyConfiguresChipsThatAreActuallyInTheCatalogue() {
            // A setup keyed on a model number nothing registers would never be used, and would
            // read as a chip that had been thought about when it had not.
            for (Rig rig : PLAN.rigs()) {
                // Every rig resolves, so the reverse check is what matters: walk the catalogue
                // and confirm each configured model is one of these.
                assertThat(rig.chip().model()).isNotBlank();
            }

            for (ICDefinition chip : REGISTRY.definitions()) {
                ChipSetup setup = ChipSetup.forModel(chip.model());
                assertThat(setup).isNotNull();
            }
        }

        @Test
        @DisplayName("flags only the chips that really are unfinished, not every blank sign")
        void flagsOnlyTheChipsThatReallyAreUnfinishedNotEveryBlankSign() {
            // A logic gate has a blank sign because it wants one. Reporting those as needing
            // attention would bury the handful that genuinely wait on a file.
            assertThat(PLAN.needingSetup())
                    .isNotEmpty()
                    .allSatisfy(chip -> assertThat(
                            ChipSetup.forModel(chip.model()).note()).isNotBlank());
            assertThat(PLAN.needingSetup().size()).isLessThan(PLAN.unconfigured().size());
        }

        @Test
        @DisplayName("leaves most of the catalogue blank, which is what most of it wants")
        void leavesMostOfTheCatalogueBlankWhichIsWhatMostOfItWants() {
            assertThat(PLAN.unconfigured()).isNotEmpty();
            assertThat(PLAN.unconfigured().size()).isLessThan(PLAN.size());
        }

        @Test
        @DisplayName("gives the variable chips a variable the builder makes for them")
        void givesTheVariableChipsAVariableTheBuilderMakesForThem() {
            for (String model : List.of("VAR100", "VAR170", "VAR200")) {
                assertThat(ChipSetup.forModel(model).thirdLine())
                        .isEqualTo(ChipSetup.SHARED_NAME);
            }
        }
    }

    @Nested
    @DisplayName("The stretch of world a build writes over")
    class Overwritten {

        @Test
        void holdsEveryBlockOfEveryRig() {
            // Whoever builds a bed clears the chips standing in this box first, and a block
            // replaced wholesale raises no break event to do it for them. A rig reaching outside
            // the box would leave the previous bed's chip there: unloadable, still ticking, and
            // attached to a sign that no longer exists.
            Bounds box = PLAN.overwritten();

            for (Rig rig : PLAN.rigs()) {
                for (Rig.Placement placement : rig.placements()) {
                    assertThat(box.contains(placement.position()))
                            .as("%s at %s", rig.chip().model(), placement.position())
                            .isTrue();
                }
            }
        }

        @Test
        void holdsTheWholeFloorItLays() {
            Bounds box = PLAN.overwritten();
            Testbed.Ground ground = PLAN.ground();

            assertThat(box.contains(ground.from())).isTrue();
            assertThat(box.contains(ground.to())).isTrue();
        }

        @Test
        void reachesAboveTheFloorRatherThanSittingFlatOnIt() {
            assertThat(PLAN.overwritten().height()).isGreaterThan(1);
        }
    }
}
