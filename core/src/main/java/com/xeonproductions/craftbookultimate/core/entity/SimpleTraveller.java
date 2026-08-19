package com.xeonproductions.craftbookultimate.core.entity;

import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.transport.Landing;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A traveller that exists only in memory and records where it was sent.
 *
 * <p>Lets a test check that a chip moved the right people to the right place without a world or
 * any entities involved.
 */
@NullMarked
public final class SimpleTraveller implements Traveller {

    private final String name;
    private Vec3i position;
    private @Nullable Landing sentTo;
    private boolean movable = true;

    public SimpleTraveller(String name, Vec3i position) {
        this.name = name;
        this.position = position;
    }

    /** A traveller standing at a position, named after it. */
    public static SimpleTraveller at(Vec3i position) {
        return new SimpleTraveller(position.toString(), position);
    }

    @Override
    public Vec3i position() {
        return position;
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

    /** Where this traveller was last sent, if anywhere. */
    public Optional<Landing> sentTo() {
        return Optional.ofNullable(sentTo);
    }

    /** Whether this traveller has been sent anywhere. */
    public boolean wasMoved() {
        return sentTo != null;
    }

    /** Makes this traveller refuse to be moved, standing in for one the server will not send. */
    public SimpleTraveller immovable() {
        this.movable = false;
        return this;
    }

    @Override
    public String toString() {
        return "Traveller[" + name + " at " + position + ']';
    }
}
