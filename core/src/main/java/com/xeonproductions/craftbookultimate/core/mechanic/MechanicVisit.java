package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpiles;
import java.util.Optional;
import java.util.OptionalDouble;
import org.jspecify.annotations.NullMarked;

/**
 * A mechanic being set off, and everything it may do about it.
 *
 * <p>The whole contract between a {@link SignMechanic} and the world, in the same way
 * {@code ChipState} is for a chip and {@code CartVisit} is for a cart mechanic. A mechanic reads
 * its sign, the blocks around it and the settings, and acts through the seams here; it never
 * touches a server.
 *
 * @param sign the mechanic's own sign
 * @param touchHeight how far up the clicked block's face the hand landed, from zero at the bottom
 *     to one at the top, or nothing when no hand was involved
 * @param world the world the mechanic is in
 * @param settings the settings in force
 * @param actor whoever set it off, or nothing when redstone did
 * @param askedToShut whether the mechanic has been told which way to go rather than left to work
 *     it out, which is what redstone does and a hand on the sign does not
 */
@NullMarked
public record MechanicVisit(
        PostedSign sign,
        OptionalDouble touchHeight,
        MechanicWorld world,
        Settings settings,
        Optional<Actor> actor,
        Optional<Boolean> askedToShut) {

    /** A mechanic worked by hand, without anybody saying where on the block the hand landed. */
    public static MechanicVisit byHand(
            PostedSign sign, MechanicWorld world, Settings settings, Actor actor) {
        return byHand(sign, OptionalDouble.empty(), world, settings, actor);
    }

    /** A mechanic worked by hand, saying how far up the clicked block the hand landed. */
    public static MechanicVisit byHand(
            PostedSign sign,
            OptionalDouble touchHeight,
            MechanicWorld world,
            Settings settings,
            Actor actor) {
        return new MechanicVisit(
                sign, touchHeight, world, settings, Optional.of(actor), Optional.empty());
    }

    /**
     * A mechanic driven by redstone.
     *
     * <p>Power arriving shuts it and power leaving opens it, so a mechanic wired to a lever
     * follows the lever rather than counting how many times it has been thrown.
     */
    public static MechanicVisit byRedstone(
            PostedSign sign, MechanicWorld world, Settings settings, boolean powered) {
        return new MechanicVisit(
                sign,
                OptionalDouble.empty(),
                world,
                settings,
                Optional.empty(),
                Optional.of(powered));
    }

    /** Tells whoever set the mechanic off that something has gone wrong. */
    public void complain(String message) {
        actor.ifPresent(who -> who.complain(message));
    }

    /** Tells whoever set the mechanic off how it went. */
    public void inform(String message) {
        actor.ifPresent(who -> who.inform(message));
    }

    /**
     * Where this mechanic gets its materials and puts them back.
     *
     * <p>The chests near its sign, unless the sign says it supplies itself, in which case it
     * neither needs anything nor keeps what it takes down.
     */
    public Stockpile stockpile() {
        return sign.isAdminSupplied()
                ? Stockpiles.unlimited()
                : world.stockpileAround(sign.position());
    }

    /** Whether a hand set this off rather than redstone. */
    public boolean isByHand() {
        return actor.isPresent();
    }
}
