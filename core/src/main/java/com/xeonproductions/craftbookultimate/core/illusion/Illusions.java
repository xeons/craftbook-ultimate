package com.xeonproductions.craftbookultimate.core.illusion;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * Showing people something other than what is there.
 *
 * <p>Only the weather so far. A chip may give one player, a permission group or a whole world a
 * sky of its own, and the world itself is untouched: nobody else sees any difference, and the rain
 * that is or is not falling goes on doing whatever it was doing.
 *
 * <p>Like {@link com.xeonproductions.craftbookultimate.core.message.Announcer}, this is the
 * audience that is the server rather than a place, and it is safe from any region's thread for the
 * same reason — a name and a value cross it, nothing else. A chip that has the people in front of
 * it already should use {@link
 * com.xeonproductions.craftbookultimate.core.entity.Bystander#showSky} instead, which is the same
 * operation without a lookup.
 *
 * <p>An illusion lasts until it is taken away. Nothing here expires on its own, so a chip that
 * shows somebody a false sky is responsible for giving them the real one back.
 */
@NullMarked
public interface Illusions {

    /**
     * Shows one player a sky of their own.
     *
     * <p>The name is matched the way a sign names somebody: the first player whose account name
     * contains it, so a sign may carry a recognisable fragment rather than an exact spelling.
     *
     * @param nameFragment part of an account name
     * @param sky what to show them, or {@link Sky#REAL} to give them the world's weather back
     * @return whether anybody answered to that name
     */
    boolean showSkyToNamed(String nameFragment, Sky sky);

    /**
     * Shows everybody in a permission group a sky of their own.
     *
     * @param group the group name, as {@code group.<name>} in the permissions
     * @return how many people were shown it
     */
    int showSkyToGroup(String group, Sky sky);

    /**
     * Shows everybody in a world a sky of their own.
     *
     * @param world which world, by the id {@link
     *     com.xeonproductions.craftbookultimate.core.world.ChipWorld#id()} gives
     * @return how many people were shown it
     */
    int showSkyIn(UUID world, Sky sky);
}
