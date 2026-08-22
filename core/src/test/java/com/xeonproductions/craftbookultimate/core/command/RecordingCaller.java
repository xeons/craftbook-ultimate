// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.command;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.Nullable;

/** A caller that writes down what it was told, so a command can be exercised without a server. */
final class RecordingCaller implements Caller {

    private final List<String> heard = new ArrayList<>();
    private final Set<String> permissions = new LinkedHashSet<>();

    private String name = "Somebody";
    private @Nullable Standing standing;

    @Override
    public void send(Component message) {
        heard.add(PlainTextComponentSerializer.plainText().serialize(message));
    }

    @Override
    public boolean may(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Standing> standing() {
        return Optional.ofNullable(standing);
    }

    RecordingCaller called(String called) {
        this.name = called;
        return this;
    }

    RecordingCaller allowed(String permission) {
        permissions.add(permission);
        return this;
    }

    RecordingCaller standingAt(UUID world, int x, int y, int z) {
        this.standing = new Standing(world, new Vec3i(x, y, z));
        return this;
    }

    /** Everything said, in the order it was said. */
    List<String> heard() {
        return List.copyOf(heard);
    }

    /** Everything said, run together, for asking whether something was mentioned at all. */
    String everything() {
        return String.join("\n", heard);
    }
}
