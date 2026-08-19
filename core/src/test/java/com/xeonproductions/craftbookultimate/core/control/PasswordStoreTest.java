package com.xeonproductions.craftbookultimate.core.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Switch passwords")
class PasswordStoreTest {

    private final PasswordStore store = new PasswordStore();

    @Nested
    @DisplayName("setting one")
    class SettingOne {

        @Test
        void putsAPasswordOnANameThatHasNone() {
            assertThat(store.setPassword("door", "hunter2")).isTrue();
            assertThat(store.hasPassword("door")).isTrue();
        }

        @Test
        void refusesToOverwriteOneAlreadySet() {
            store.setPassword("door", "hunter2");

            assertThat(store.setPassword("door", "something else")).isFalse();
            assertThat(store.matches("door", "hunter2")).isTrue();
        }

        @Test
        void refusesABlankPassword() {
            assertThat(store.setPassword("door", "")).isFalse();
            assertThat(store.hasPassword("door")).isFalse();
        }
    }

    @Nested
    @DisplayName("checking one")
    class CheckingOne {

        @Test
        void acceptsTheRightPassword() {
            store.setPassword("door", "hunter2");

            assertThat(store.matches("door", "hunter2")).isTrue();
        }

        @Test
        void rejectsTheWrongOne() {
            store.setPassword("door", "hunter2");

            assertThat(store.matches("door", "hunter3")).isFalse();
            assertThat(store.matches("door", "")).isFalse();
        }

        @Test
        void rejectsEverythingForANameWithNoPassword() {
            // An unguarded name must not be openable by guessing that it is unguarded.
            assertThat(store.matches("door", "")).isFalse();
            assertThat(store.matches("door", "anything")).isFalse();
        }
    }

    @Nested
    @DisplayName("changing one")
    class ChangingOne {

        @Test
        void takesTheOldPassword() {
            store.setPassword("door", "hunter2");

            assertThat(store.changePassword("door", "hunter2", "hunter3")).isTrue();
            assertThat(store.matches("door", "hunter3")).isTrue();
            assertThat(store.matches("door", "hunter2")).isFalse();
        }

        @Test
        void refusesTheWrongOldPassword() {
            store.setPassword("door", "hunter2");

            assertThat(store.changePassword("door", "wrong", "hunter3")).isFalse();
            assertThat(store.matches("door", "hunter2")).isTrue();
        }

        @Test
        void refusesToChangeANameWithNoPassword() {
            assertThat(store.changePassword("door", "anything", "hunter3")).isFalse();
        }
    }

    @Nested
    @DisplayName("keeping them")
    class KeepingThem {

        @Test
        void writesNothingThatCouldBeTypedBackIn() {
            store.setPassword("door", "hunter2");

            assertThat(store.save()).hasSize(1);
            assertThat(store.save().get(0)).doesNotContain("hunter2");
        }

        @Test
        void readsBackWhatItWrote() {
            store.setPassword("door", "hunter2");
            List<String> saved = store.save();

            PasswordStore reloaded = new PasswordStore();
            assertThat(reloaded.load(saved)).isEqualTo(1);
            assertThat(reloaded.matches("door", "hunter2")).isTrue();
            assertThat(reloaded.matches("door", "hunter3")).isFalse();
        }

        @Test
        void skipsALineItCannotRead() {
            store.setPassword("door", "hunter2");
            List<String> lines = new java.util.ArrayList<>(store.save());
            lines.add("damaged");

            PasswordStore reloaded = new PasswordStore();

            assertThat(reloaded.load(lines)).isEqualTo(1);
            assertThat(reloaded.matches("door", "hunter2")).isTrue();
        }

        @Test
        void leavesOutANameThatCouldNotBeReadBack() {
            // A colon separates the fields, so a name carrying one would split wrongly.
            store.setPassword("a:b", "hunter2");

            assertThat(store.save()).isEmpty();
            assertThat(PasswordStore.isSaveableName("a:b")).isFalse();
        }
    }

    @Test
    void saltsEachPasswordSeparately() {
        // Two names with the same password must not look the same in the file.
        store.setPassword("front", "hunter2");
        store.setPassword("back", "hunter2");

        List<String> saved = store.save();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).split(":")[2]).isNotEqualTo(saved.get(1).split(":")[2]);
    }

    @Test
    void takesAPasswordOffAName() {
        store.setPassword("door", "hunter2");

        assertThat(store.removePassword("door")).isTrue();
        assertThat(store.hasPassword("door")).isFalse();
        assertThat(store.removePassword("door")).isFalse();
    }
}
