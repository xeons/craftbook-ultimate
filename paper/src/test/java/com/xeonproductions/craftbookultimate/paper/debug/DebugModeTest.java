package com.xeonproductions.craftbookultimate.paper.debug;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The debug stick's modes")
class DebugModeTest {

    @Test
    void cycleThroughEveryOneAndBackToTheStart() {
        Set<DebugMode> seen = new HashSet<>();

        DebugMode mode = DebugMode.DEFAULT;
        for (int step = 0; step < DebugMode.CYCLE.size(); step++) {
            seen.add(mode);
            mode = mode.next();
        }

        assertThat(seen).containsExactlyInAnyOrderElementsOf(DebugMode.CYCLE);
        assertThat(mode).isEqualTo(DebugMode.DEFAULT);
    }

    @Test
    void leaveNoneOutOfTheCycle() {
        // A mode that cannot be cycled to is one only reachable by command, which would be a
        // stick with a setting nobody can select.
        assertThat(DebugMode.CYCLE).containsExactlyInAnyOrder(DebugMode.values());
    }

    @Test
    void startOnTheOneThatOffersAllTheOthers() {
        assertThat(DebugMode.DEFAULT).isEqualTo(DebugMode.MENU);
    }

    @Test
    void haveAPermissionEach() {
        Set<String> permissions = new HashSet<>();
        for (DebugMode mode : DebugMode.values()) {
            permissions.add(mode.permission());
            assertThat(mode.permission()).startsWith(DebugStick.PERMISSION + ".");
        }

        assertThat(permissions).hasSameSizeAs(DebugMode.values());
    }

    @Test
    void areFoundByNameHoweverItIsCapitalised() {
        assertThat(DebugMode.byName("trigger")).contains(DebugMode.TRIGGER);
        assertThat(DebugMode.byName("  TRIGGER  ")).contains(DebugMode.TRIGGER);
        assertThat(DebugMode.byName("nothing")).isEmpty();
    }

    @Test
    void allSayWhatTheyDo() {
        for (DebugMode mode : DebugMode.values()) {
            assertThat(mode.title()).as("%s title", mode).isNotBlank();
            assertThat(mode.description()).as("%s description", mode).isNotBlank();
        }
    }
}
