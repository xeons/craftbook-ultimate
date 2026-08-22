// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xeonproductions.craftbookultimate.core.config.BlockNames;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.mechanic.Mechanics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * The two settings files, read and written through the YAML a server actually uses.
 *
 * <p>{@code core} already pins what every setting is called and what it defaults to, against a
 * settings file held in a map. What that cannot reach is the YAML itself: whether a section named
 * after a mechanic survives being written out and read back, and whether a value an operator wrote
 * is still there afterwards. Both files are written on every load, so a fault there costs an
 * operator their settings rather than merely failing to gain a new one.
 */
@DisplayName("The settings files on disk")
class ConfigFileTest {

    @TempDir
    private Path directory;

    private final List<String> complaints = new ArrayList<>();

    private ServerMock server;

    @BeforeEach
    void startServer() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    private Settings load() throws IOException {
        return new ConfigFile(directory, new AnyBlock(), complaints::add).load();
    }

    private Path mechanicsFile() {
        return directory.resolve(ConfigFile.MECHANICS_FILE_NAME);
    }

    private Path configFile() {
        return directory.resolve(ConfigFile.FILE_NAME);
    }

    @Nested
    @DisplayName("on a server that has never had them")
    class Fresh {

        @Test
        @DisplayName("are both written")
        void areBothWritten() throws IOException {
            load();

            assertThat(configFile()).isRegularFile();
            assertThat(mechanicsFile()).isRegularFile();
        }

        @Test
        @DisplayName("come out as the defaults")
        void comeOutAsTheDefaults() throws IOException {
            assertThat(load()).isEqualTo(Settings.DEFAULTS);
        }

        @Test
        @DisplayName("leave the mechanics out of the main file entirely")
        void leaveTheMechanicsOutOfTheMainFile() throws IOException {
            load();

            String written = Files.readString(configFile());
            assertThat(written).doesNotContain("gate-radius", "lift-tolerance", "xp-per-bottle");
        }

        @Test
        @DisplayName("name a section after every mechanic there is")
        void nameASectionAfterEveryMechanic() throws IOException {
            load();

            String written = Files.readString(mechanicsFile());
            for (String mechanic : Mechanics.ALL) {
                assertThat(written).contains(mechanic + ":");
            }
        }

        @Test
        @DisplayName("say what each setting is for")
        void sayWhatEachSettingIsFor() throws IOException {
            load();

            assertThat(Files.readString(mechanicsFile())).contains("#");
        }
    }

    @Nested
    @DisplayName("read again")
    class Again {

        @Test
        @DisplayName("keep what an operator wrote about a mechanic")
        void keepWhatAnOperatorWrote() throws IOException {
            load();
            String written = Files.readString(mechanicsFile())
                    .replace("radius: 5", "radius: 2");
            Files.writeString(mechanicsFile(), written);

            assertThat(load().mechanics().gate().radius()).isEqualTo(2);
            assertThat(Files.readString(mechanicsFile())).contains("radius: 2");
        }

        @Test
        @DisplayName("keep a mechanic switched off")
        void keepAMechanicSwitchedOff() throws IOException {
            load();
            Files.writeString(mechanicsFile(), Files.readString(mechanicsFile())
                    .replace("Gate:\n  enabled: true", "Gate:\n  enabled: false"));

            MechanicSettings mechanics = load().mechanics();

            assertThat(mechanics.allows(Mechanics.GATE)).isFalse();
            assertThat(mechanics.allows(Mechanics.BRIDGE)).isTrue();
        }

        @Test
        @DisplayName("come out the same twice, having written themselves once")
        void comeOutTheSameTwice() throws IOException {
            Settings first = load();
            Settings second = load();

            assertThat(second).isEqualTo(first);
        }
    }

    @Nested
    @DisplayName("where one file is unreadable")
    class Broken {

        @Test
        @DisplayName("say which one it was")
        void sayWhichOneItWas() throws IOException {
            Files.writeString(mechanicsFile(), "Gate: [this is not a section\n");

            assertThatThrownBy(ConfigFileTest.this::load)
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining(ConfigFile.MECHANICS_FILE_NAME);
        }
    }

    /**
     * A server that knows every block it is asked about.
     *
     * <p>The real one cannot be used here: it reads the pre-flattening names through
     * {@code LegacyBlocks}, which asks the server what is a block as it starts up, and the mock
     * server has no answer for that. What is under test is the YAML rather than the block names,
     * and those have their own tests.
     */
    private record AnyBlock() implements BlockNames {

        @Override
        public Optional<Key> block(String written) {
            try {
                return Optional.of(Key.key(written));
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }

        @Override
        public Set<Key> tagged(String tag) {
            return Set.of();
        }
    }
}
