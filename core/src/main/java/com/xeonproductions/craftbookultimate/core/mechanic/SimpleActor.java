// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An actor that exists only in memory and remembers what it was told.
 *
 * <p>Lets a test check that a mechanic refused for the reason it should have, and that a lift put
 * somebody where it said it would, with no server involved.
 */
@NullMarked
public final class SimpleActor implements Actor {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final String name;
    private final List<String> heard = new ArrayList<>();
    private final Set<String> permissions = new LinkedHashSet<>();

    private @Nullable Vec3i position;
    private @Nullable Landing sentTo;
    private boolean sneaking;
    private boolean allowedEverything = true;
    private boolean movable = true;

    public SimpleActor(String name) {
        this.name = name;
    }

    /** An actor standing at a position. */
    public static SimpleActor at(Vec3i position) {
        SimpleActor actor = new SimpleActor("tester");
        actor.position = position;
        return actor;
    }

    /** An actor who is not in the world at all, which is what a console is. */
    public static SimpleActor named(String name) {
        return new SimpleActor(name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void tell(Component message) {
        heard.add(PLAIN.serialize(message));
    }

    @Override
    public boolean mayUse(String permission) {
        return allowedEverything || permissions.contains(permission);
    }

    @Override
    public boolean isSneaking() {
        return sneaking;
    }

    @Override
    public Optional<Vec3i> position() {
        return Optional.ofNullable(position);
    }

    @Override
    public boolean moveTo(Landing landing) {
        if (!movable) {
            return false;
        }
        sentTo = landing;
        position = landing.block();
        return true;
    }

    /** Everything this actor has been told, in the order it was told. */
    public List<String> heard() {
        return List.copyOf(heard);
    }

    /** Whether this actor was told something containing a phrase. */
    public boolean wasTold(String phrase) {
        return heard.stream().anyMatch(line -> line.contains(phrase));
    }

    /** Where this actor was last sent, if anywhere. */
    public Optional<Landing> sentTo() {
        return Optional.ofNullable(sentTo);
    }

    /** Whether this actor has been sent anywhere. */
    public boolean wasMoved() {
        return sentTo != null;
    }

    /** Makes this actor crouch. */
    public SimpleActor sneaking() {
        this.sneaking = true;
        return this;
    }

    /** Limits this actor to the permissions named. */
    public SimpleActor allowedOnly(String... nodes) {
        allowedEverything = false;
        permissions.clear();
        permissions.addAll(List.of(nodes));
        return this;
    }

    /** Makes this actor refuse to be moved, standing in for one the server will not send. */
    public SimpleActor immovable() {
        this.movable = false;
        return this;
    }

    @Override
    public String toString() {
        return "Actor[" + name + (position == null ? "" : " at " + position) + ']';
    }
}
