package com.xeonproductions.craftbookultimate.core.ic.gate;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.effect.FireworkShow;
import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.ic.SimpleChipState;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.world.SimpleChipWorld;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The programmable firework display")
class FireworkDisplayTest {

    private static final Vec3i SIGN = new Vec3i(0, 64, 0);

    private final SimpleChipWorld world = new SimpleChipWorld();
    private final ChipServices services = ChipServices.create();

    private SimpleChipState chip(String showName, String stopOnLow, boolean driven) {
        return SimpleChipState.forLayout(PinLayout.AISO)
                .at(SIGN, BlockFace.SOUTH)
                .world(world)
                .services(services)
                .sign("DISPLAY", "[MC1253]", showName, stopOnLow)
                .inputs(driven, false, false, false)
                .build();
    }

    private void record(String name, String... lines) {
        services.shows().put(name, FireworkShow.parse(List.of(lines), FireworkShow.Dialect.PLAIN));
    }

    @Test
    void playsTheShowItsSignNames() {
        record("party", "launch:0,0,0;1;BALL;255,0,0;0,0,0");

        FireworkDisplay.display().trigger(chip("party", "", true));

        assertThat(world.fireworks()).hasSize(1);
    }

    @Test
    void playsNothingForAShowTheServerHasNoScriptFor() {
        FireworkDisplay.display().trigger(chip("missing", "", true));

        assertThat(world.fireworks()).isEmpty();
    }

    @Test
    void waitsBeforeGoingOnToTheNextStep() {
        record(
                "party",
                "launch:0,0,0;1;BALL;255,0,0;0,0,0",
                "wait:20",
                "launch:0,0,0;1;STAR;0,0,255;0,0,0");
        SimpleChipState state = chip("party", "", true);

        FireworkDisplay.display().trigger(state);
        assertThat(world.fireworks()).hasSize(1);

        state.manualScheduler().advance(20);
        assertThat(world.fireworks()).hasSize(2);
    }

    @Test
    void doesNotStartAShowThatIsAlreadyRunning() {
        record("party", "launch:0,0,0;1;BALL;255,0,0;0,0,0", "wait:20");
        ICLogic display = FireworkDisplay.display();
        SimpleChipState state = chip("party", "", true);

        display.trigger(state);
        display.trigger(state);

        assertThat(world.fireworks()).hasSize(1);
    }

    @Test
    void cutsTheShowShortWhenAskedToAndTheInputDrops() {
        record(
                "party",
                "launch:0,0,0;1;BALL;255,0,0;0,0,0",
                "wait:20",
                "launch:0,0,0;1;STAR;0,0,255;0,0,0");
        ICLogic display = FireworkDisplay.display();
        SimpleChipState running = chip("party", "true", true);

        display.trigger(running);
        display.trigger(running.withInput(0, false));
        running.manualScheduler().advance(20);

        assertThat(world.fireworks()).hasSize(1);
    }

    @Test
    void runsToTheEndWhenItWasNotAskedToStop() {
        record(
                "party",
                "launch:0,0,0;1;BALL;255,0,0;0,0,0",
                "wait:20",
                "launch:0,0,0;1;STAR;0,0,255;0,0,0");
        ICLogic display = FireworkDisplay.display();
        SimpleChipState running = chip("party", "", true);

        display.trigger(running);
        display.trigger(running.withInput(0, false));
        running.manualScheduler().advance(20);

        assertThat(world.fireworks()).hasSize(2);
    }
}
