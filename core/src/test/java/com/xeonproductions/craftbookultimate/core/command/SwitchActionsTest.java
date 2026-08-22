// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.control.PasswordStore;
import com.xeonproductions.craftbookultimate.core.control.Switchboard;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The switch commands")
class SwitchActionsTest {

    private final Switchboard open = new Switchboard();
    private final Switchboard guarded = new Switchboard();
    private final PasswordStore passwords = new PasswordStore();

    private int passwordSaves;
    private int switchSaves;

    /** Runs the slow work straight away, so a test sees the answer rather than waiting for it. */
    private final SwitchActions actions = new SwitchActions(
            open,
            guarded,
            passwords,
            Runnable::run,
            () -> passwordSaves++,
            () -> switchSaves++);

    private final RecordingCaller caller = new RecordingCaller();

    @Nested
    @DisplayName("throwing an unguarded switch")
    class Open {

        @Test
        @DisplayName("says so when no chip is following that name")
        void refusesAnUnknownSwitch() {
            assertThat(actions.toggleOpen(caller, "gate", Optional.empty())).isFalse();
            assertThat(caller.everything()).contains("No chip is following");
        }

        @Test
        @DisplayName("turns one on, and writes the positions out")
        void turnsItOn() {
            open.register("gate");
            open.set("gate", false);

            assertThat(actions.toggleOpen(caller, "gate", Optional.of("on"))).isTrue();

            assertThat(open.state("gate")).contains(true);
            assertThat(switchSaves).isEqualTo(1);
        }

        @Test
        @DisplayName("will not toggle one that has never been thrown, since there is no other side")
        void refusesToggleOfAnUnthrownSwitch() {
            open.register("gate");

            assertThat(actions.toggleOpen(caller, "gate", Optional.empty())).isFalse();
            assertThat(caller.everything()).contains("never been thrown");
        }

        @Test
        @DisplayName("refuses a word that is not one of the three")
        void refusesAnUnknownMode() {
            open.register("gate");
            open.set("gate", false);

            assertThat(actions.toggleOpen(caller, "gate", Optional.of("sideways"))).isFalse();
            assertThat(caller.everything()).contains("on, off or state");
        }

        @Test
        @DisplayName("reports where one is standing without moving it")
        void reportsState() {
            open.register("gate");
            open.set("gate", true);

            assertThat(actions.toggleOpen(caller, "gate", Optional.of("state"))).isTrue();

            assertThat(open.state("gate")).contains(true);
            assertThat(switchSaves).isZero();
        }
    }

    @Nested
    @DisplayName("throwing a guarded switch")
    class Guarded {

        @Test
        @DisplayName("says so when the switch has no password yet")
        void refusesWhenThereIsNoPassword() {
            guarded.register("vault");
            guarded.set("vault", false);

            assertThat(actions.toggleGuarded(caller, "vault", "hunter2", Optional.of("on")))
                    .isFalse();
            assertThat(caller.everything()).contains("no password yet");
        }

        @Test
        @DisplayName("leaves it alone when the password is wrong")
        void refusesTheWrongPassword() {
            guarded.register("vault");
            guarded.set("vault", false);
            passwords.setPassword("vault", "hunter2");

            actions.toggleGuarded(caller, "vault", "wrong", Optional.of("on"));

            assertThat(guarded.state("vault")).contains(false);
            assertThat(caller.everything()).contains("Wrong password");
        }

        @Test
        @DisplayName("throws it when the password is right")
        void acceptsTheRightPassword() {
            guarded.register("vault");
            guarded.set("vault", false);
            passwords.setPassword("vault", "hunter2");

            actions.toggleGuarded(caller, "vault", "hunter2", Optional.of("on"));

            assertThat(guarded.state("vault")).contains(true);
        }
    }

    @Nested
    @DisplayName("setting a password")
    class Passwords {

        @Test
        @DisplayName("will not overwrite one that is already set")
        void refusesToOverwrite() {
            guarded.register("vault");
            guarded.set("vault", false);
            passwords.setPassword("vault", "hunter2");

            assertThat(actions.addPassword(caller, "vault", "other")).isFalse();
            assertThat(caller.everything()).contains("already has a password");
        }

        @Test
        @DisplayName("says whether one is set without saying what it is")
        void saysWhetherOneIsSet() {
            passwords.setPassword("vault", "hunter2");

            actions.hasPassword(caller, "vault");

            assertThat(caller.everything()).contains("has a password").doesNotContain("hunter2");
        }
    }
}
