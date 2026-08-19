package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.entity.SimpleTraveller;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Transporters and destinations")
class TransportTest {

    /** The sign of a chip facing south, so the block it hangs on is one step north. */
    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private static final Vec3i BEHIND_SIGN = SIGN.offset(BlockFace.SOUTH.opposite());

    private final ChipServices services = ChipServices.create();

    private SimpleChipState.Builder chip(SimpleChipWorld world, String model, String name) {
        return SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .services(services)
                .world(world)
                .at(SIGN, BlockFace.SOUTH)
                .sign("", model, name, "");
    }

    /** A world with a solid block behind the sign, so the arrival point is the block above it. */
    private static SimpleChipWorld worldWithWall() {
        return new SimpleChipWorld().withBlock(BEHIND_SIGN, "stone");
    }

    private static Vec3i aboveWall() {
        return BEHIND_SIGN.offset(BlockFace.UP);
    }

    @Nested
    @DisplayName("a destination")
    class ADestination {

        @Test
        void answersToItsNameWhileItIsDriven() {
            SimpleChipState state =
                    chip(worldWithWall(), "[MCU113]", "atrium").inputs(true, false, false).build();

            Transport.destination().load(state);

            assertThat(services.destinations().find("atrium")).isPresent();
        }

        @Test
        void answersToItsNameWithNothingWiredToIt() {
            SimpleChipState state = chip(worldWithWall(), "[MCU113]", "atrium")
                    .inputs(false, false, false)
                    .connected(false, false, false)
                    .build();

            Transport.destination().load(state);

            assertThat(services.destinations().find("atrium")).isPresent();
        }

        @Test
        void staysSilentWhileItsWiringIsOff() {
            SimpleChipState state = chip(worldWithWall(), "[MCU113]", "atrium")
                    .inputs(false, false, false)
                    .build();

            Transport.destination().load(state);

            assertThat(services.destinations().find("atrium")).isEmpty();
        }

        @Test
        void givesUpItsNameWhenItIsSwitchedOff() {
            SimpleChipState on =
                    chip(worldWithWall(), "[MCU113]", "atrium").inputs(true, false, false).build();
            SelfTriggeringICLogic chip = Transport.destination();

            chip.load(on);
            chip.trigger(on.withInput(0, false));

            assertThat(services.destinations().find("atrium")).isEmpty();
        }

        @Test
        void givesUpItsNameWhenItIsUnloaded() {
            SimpleChipState state =
                    chip(worldWithWall(), "[MCU113]", "atrium").inputs(true, false, false).build();
            SelfTriggeringICLogic chip = Transport.destination();

            chip.load(state);
            chip.unload();

            assertThat(services.destinations().find("atrium")).isEmpty();
        }

        @Test
        void landsArrivalsAboveTheBlockItHangsOn() {
            SimpleChipState state =
                    chip(worldWithWall(), "[MCU113]", "atrium").inputs(true, false, false).build();

            Transport.destination().load(state);

            Landing landing = services.destinations().find("atrium").orElseThrow();
            assertThat(landing.block()).isEqualTo(aboveWall());
        }

        @Test
        void facesArrivalsTheWayTheSignsBackPoints() {
            SimpleChipState state =
                    chip(worldWithWall(), "[MCU113]", "atrium").inputs(true, false, false).build();

            Transport.destination().load(state);

            assertThat(services.destinations().find("atrium").orElseThrow().facing())
                    .isEqualTo(BlockFace.NORTH);
        }

        @Test
        void movesItsArrivalPointUpWhenSomebodyBuildsOverIt() {
            SimpleChipWorld world = worldWithWall();
            SimpleChipState state =
                    chip(world, "[MCU113]", "atrium").inputs(true, false, false).build();
            SelfTriggeringICLogic chip = Transport.destination();
            chip.load(state);

            world.withBlock(aboveWall(), "stone");
            chip.tick(state);

            assertThat(services.destinations().find("atrium").orElseThrow().block())
                    .isEqualTo(aboveWall().offset(BlockFace.UP));
        }

        @Test
        void leavesAnExistingNameToTheDestinationAlreadyUsingIt() {
            SimpleChipState first =
                    chip(worldWithWall(), "[MCU113]", "atrium").inputs(true, false, false).build();
            SimpleChipState second = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .services(services)
                    .world(worldWithWall())
                    .at(new Vec3i(100, 64, 100), BlockFace.SOUTH)
                    .sign("", "[MCU113]", "atrium", "")
                    .inputs(true, false, false)
                    .build();

            Transport.destination().load(first);
            Transport.destination().load(second);

            assertThat(services.destinations().find("atrium").orElseThrow().block())
                    .isEqualTo(aboveWall());
        }

        @Test
        void cannotTakeANameAwayFromTheDestinationUsingItByBeingUnloaded() {
            SimpleChipState first =
                    chip(worldWithWall(), "[MCU113]", "atrium").inputs(true, false, false).build();
            SimpleChipState second = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                    .services(services)
                    .world(worldWithWall())
                    .at(new Vec3i(100, 64, 100), BlockFace.SOUTH)
                    .sign("", "[MCU113]", "atrium", "")
                    .inputs(true, false, false)
                    .build();
            SelfTriggeringICLogic loser = Transport.destination();

            Transport.destination().load(first);
            loser.load(second);
            loser.unload();

            assertThat(services.destinations().find("atrium")).isPresent();
        }

        @Test
        void ticksWhetherOrNotItsSignAsksItTo() {
            assertThat(Transport.destination().alwaysSelfTriggering()).isTrue();
        }
    }

    @Nested
    @DisplayName("a transporter")
    class ATransporter {

        @Test
        void sendsWhoeverIsStandingInItsDoorway() {
            services.destinations().claim("atrium", new Object(), landingAt(new Vec3i(200, 70, 200)));

            SimpleTraveller traveller = SimpleTraveller.at(aboveWall());
            SimpleChipWorld world = worldWithWall().withTraveller(aboveWall(), traveller);
            SimpleChipState state =
                    chip(world, "[MCX112]", "atrium").inputs(true, false, false).build();

            Transport.transporter().trigger(state);

            assertThat(traveller.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(200, 70, 200));
            assertThat(state.output(0)).isTrue();
        }

        @Test
        void staysQuietWhenNothingIsDrivingIt() {
            services.destinations().claim("atrium", new Object(), landingAt(new Vec3i(200, 70, 200)));

            SimpleTraveller traveller = SimpleTraveller.at(aboveWall());
            SimpleChipWorld world = worldWithWall().withTraveller(aboveWall(), traveller);
            SimpleChipState state =
                    chip(world, "[MCX112]", "atrium").inputs(false, false, false).build();

            Transport.transporter().trigger(state);

            assertThat(traveller.wasMoved()).isFalse();
        }

        @Test
        void doesNothingWhenNothingAnswersToItsName() {
            SimpleTraveller traveller = SimpleTraveller.at(aboveWall());
            SimpleChipWorld world = worldWithWall().withTraveller(aboveWall(), traveller);
            SimpleChipState state = chip(world, "[MCX112]", "nowhere")
                    .inputs(true, false, false)
                    .build();

            Transport.transporter().trigger(state);

            assertThat(traveller.wasMoved()).isFalse();
            assertThat(state.output(0)).isFalse();
        }

        @Test
        void reportsNobodySentWhenTheDoorwayIsEmpty() {
            services.destinations().claim("atrium", new Object(), landingAt(new Vec3i(200, 70, 200)));
            SimpleChipState state = chip(worldWithWall(), "[MCX112]", "atrium")
                    .inputs(true, false, false)
                    .build();

            Transport.transporter().trigger(state);

            assertThat(state.output(0)).isFalse();
        }

        @Test
        void picksPeopleUpFromThePadAboveItInPadMode() {
            services.destinations().claim("atrium", new Object(), landingAt(new Vec3i(200, 70, 200)));

            Vec3i pad = SIGN.offset(BlockFace.SOUTH).add(0, 2, 0);
            SimpleTraveller onThePad = SimpleTraveller.at(pad);
            SimpleTraveller inTheDoorway = SimpleTraveller.at(aboveWall());
            SimpleChipWorld world = worldWithWall()
                    .withTraveller(pad, onThePad)
                    .withTraveller(aboveWall(), inTheDoorway);

            SimpleChipState state = chip(world, "[MCX112]", "atrium")
                    .inputs(true, false, false)
                    .mode(ICMode.parse("p"))
                    .build();

            Transport.transporter().trigger(state);

            assertThat(onThePad.wasMoved()).isTrue();
            assertThat(inTheDoorway.wasMoved()).isFalse();
        }

        @Test
        void releasesThePlateSomebodyLeftBehindInForcedPlateMode() {
            services.destinations().claim("atrium", new Object(), landingAt(new Vec3i(200, 70, 200)));

            Vec3i pad = SIGN.offset(BlockFace.SOUTH).add(0, 2, 0);
            SimpleChipWorld world = worldWithWall()
                    .withTraveller(pad, SimpleTraveller.at(pad))
                    .withPressedPlate(pad);

            SimpleChipState state = chip(world, "[MCX112]", "atrium")
                    .inputs(true, false, false)
                    .mode(ICMode.parse("P"))
                    .build();

            Transport.transporter().trigger(state);

            assertThat(world.isPlatePressed(pad)).isFalse();
        }

        @Test
        void leavesThePlateAloneInPlainPadMode() {
            services.destinations().claim("atrium", new Object(), landingAt(new Vec3i(200, 70, 200)));

            Vec3i pad = SIGN.offset(BlockFace.SOUTH).add(0, 2, 0);
            SimpleChipWorld world = worldWithWall()
                    .withTraveller(pad, SimpleTraveller.at(pad))
                    .withPressedPlate(pad);

            SimpleChipState state = chip(world, "[MCX112]", "atrium")
                    .inputs(true, false, false)
                    .mode(ICMode.parse("p"))
                    .build();

            Transport.transporter().trigger(state);

            assertThat(world.isPlatePressed(pad)).isTrue();
        }

        @Test
        void reportsNobodySentWhenTheServerRefusesToMoveThem() {
            services.destinations().claim("atrium", new Object(), landingAt(new Vec3i(200, 70, 200)));

            SimpleTraveller stuck = SimpleTraveller.at(aboveWall()).immovable();
            SimpleChipWorld world = worldWithWall().withTraveller(aboveWall(), stuck);
            SimpleChipState state =
                    chip(world, "[MCX112]", "atrium").inputs(true, false, false).build();

            Transport.transporter().trigger(state);

            assertThat(state.output(0)).isFalse();
        }
    }

    @Test
    void sendsSomebodyFromOneEndOfThePairToTheOther() {
        SimpleChipWorld far = new SimpleChipWorld().withBlock(new Vec3i(500, 64, 498), "stone");
        SimpleChipState destination = SimpleChipState.forLayout(PinLayout.THREE_I_SO)
                .services(services)
                .world(far)
                .at(new Vec3i(500, 64, 499), BlockFace.SOUTH)
                .sign("", "[MCU113]", "atrium", "")
                .inputs(true, false, false)
                .build();

        SimpleTraveller traveller = SimpleTraveller.at(aboveWall());
        SimpleChipWorld near = worldWithWall().withTraveller(aboveWall(), traveller);
        SimpleChipState transporter =
                chip(near, "[MCX112]", "atrium").inputs(true, false, false).build();

        Transport.destination().load(destination);
        Transport.transporter().trigger(transporter);

        assertThat(traveller.sentTo().orElseThrow().block()).isEqualTo(new Vec3i(500, 65, 498));
        assertThat(traveller.sentTo().orElseThrow().world()).isEqualTo(far.id());
    }

    private static Landing landingAt(Vec3i block) {
        return new Landing(UUID.randomUUID(), block, BlockFace.NORTH);
    }
}
