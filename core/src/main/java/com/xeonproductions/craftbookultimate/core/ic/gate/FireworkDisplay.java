// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.effect.FireworkShow;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.platform.Scheduler;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The chip that plays a firework display from a script.
 *
 * <p>Line 3 names the show, which an operator has put in the plugin's {@code fireworks} folder.
 * Line 4 says whether dropping the input should cut the show short; without it the show runs to
 * the end whatever the redstone does afterwards.
 *
 * <p>A show that is already running is not restarted, so holding the input on does not stack a
 * dozen copies of the same display on top of each other.
 */
@NullMarked
public final class FireworkDisplay {

    /** The line naming the show. */
    private static final int SHOW_LINE = 2;

    /** The line saying whether dropping the input cuts the show short. */
    private static final int STOP_LINE = 3;

    private FireworkDisplay() {}

    /** Plays a named firework display. */
    public static ICLogic display() {
        return new Display();
    }

    /** One display chip, and the show it currently has in the air. */
    private static final class Display implements ICLogic {

        private @Nullable Run running;

        @Override
        public void trigger(ChipState state) {
            if (state.isAnyInputActive()) {
                start(state);
                return;
            }
            if (stopsOnLow(state)) {
                stop();
            }
        }

        @Override
        public void unload(ChipState state) {
            stop();
        }

        private void start(ChipState state) {
            if (running != null && !running.isFinished()) {
                return;
            }

            Optional<FireworkShow> show =
                    state.services().shows().find(state.sign().trimmedText(SHOW_LINE));
            if (show.isEmpty() || show.get().isEmpty()) {
                return;
            }

            running = new Run(state, show.get().steps());
            running.next();
        }

        private void stop() {
            if (running != null) {
                running.abandon();
                running = null;
            }
        }

        private static boolean stopsOnLow(ChipState state) {
            return Boolean.parseBoolean(state.sign().trimmedText(STOP_LINE).trim());
        }
    }

    /**
     * One playing of a show.
     *
     * <p>Steps run straight through until a wait, at which point the rest is handed to the chip's
     * own scheduler and so to the thread that owns the chip's blocks.
     */
    private static final class Run {

        private final ChipState state;
        private final List<FireworkShow.Step> steps;
        private int position;
        private boolean abandoned;
        private Scheduler.@Nullable Task pending;

        Run(ChipState state, List<FireworkShow.Step> steps) {
            this.state = state;
            this.steps = steps;
        }

        boolean isFinished() {
            // A show part way through a wait has run out of steps for now but is not over, so the
            // outstanding task counts as much as the position does.
            return abandoned || (position >= steps.size() && pending == null);
        }

        void abandon() {
            abandoned = true;
            if (pending != null) {
                pending.cancel();
                pending = null;
            }
        }

        void next() {
            pending = null;
            while (!abandoned && position < steps.size()) {
                FireworkShow.Step step = steps.get(position++);
                if (step instanceof FireworkShow.Step.Wait wait) {
                    pending = state.scheduler().runLater(this::next, Math.max(1, wait.ticks()));
                    return;
                }
                perform(step);
            }
        }

        private void perform(FireworkShow.Step step) {
            Vec3d origin = Vec3d.middleOf(state.signPosition());
            switch (step) {
                case FireworkShow.Step.Launch launch ->
                        state.world()
                                .launchFirework(
                                        origin.add(launch.offset()), launch.burst(), launch.fuseTicks());
                case FireworkShow.Step.Sound sound ->
                        state.world()
                                .playSound(
                                        origin.add(sound.offset()),
                                        sound.sound(),
                                        sound.volume(),
                                        sound.pitch());
                case FireworkShow.Step.Wait ignored -> {
                    // Handled before we get here, so that the rest of the show can be scheduled.
                }
            }
        }
    }
}
