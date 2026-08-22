// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.mechanic;

import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A lever hidden behind the wall it works.
 *
 * <p>An {@code [X]} sign is put on the back of a block and the levers or buttons touching that
 * sign are what actually get thrown. From the front there is a plain wall; clicking it works
 * whatever is behind. A door with no visible switch, in other words, and the only sign mechanic
 * whose sign is never the thing that gets clicked.
 *
 * <p><b>A key may be named on the first line.</b> Written there and the switch only answers to
 * somebody holding one, in either hand. That is upstream's spelling of the same idea; the fork
 * asked for the key through a chat prompt and kept it in block data the builder could never see
 * again, which is the arrangement the toggled areas already rejected. A line anybody can read and
 * anybody can change is worth more than one that survives being renamed.
 *
 * <p>The switches thrown are the ones directly above and below the sign and the two to either side
 * of it, chosen so that a sign on a north wall reaches east and west rather than into the wall.
 * A lever is flipped and left; a button is pressed and springs back, exactly as under a hand.
 */
@NullMarked
public final class HiddenSwitch implements SignMechanic {

    /** The plain sign. */
    public static final String SIGN = "[X]";

    /** The spelling a sign carries when a page reader shares the block. */
    public static final String BOOK_SIGN = "[Book][X]";

    /** The line a key is named on, which is the same line the building mechanics supply from. */
    public static final int KEY_LINE = 0;

    private static final HiddenSwitch INSTANCE = new HiddenSwitch();

    private HiddenSwitch() {
    }

    /** The one of these there is. */
    public static HiddenSwitch instance() {
        return INSTANCE;
    }

    @Override
    public String name() {
        return Mechanics.HIDDEN_SWITCH;
    }

    @Override
    public List<String> signNames() {
        return List.of(SIGN, BOOK_SIGN);
    }

    @Override
    public boolean act(MechanicVisit visit) {
        // Redstone reaching the sign would be a switch working itself, which is nothing.
        Optional<Actor> who = visit.actor();
        if (who.isEmpty()) {
            return false;
        }
        Actor actor = who.get();

        Optional<Key> key = keyOn(visit.sign(), visit.world());
        if (key.isPresent() && !actor.held().contains(key.get())) {
            actor.complain("The key does not fit.");
            return true;
        }

        boolean worked = false;
        for (Vec3i place : switchesAround(visit.sign())) {
            worked |= visit.world().workSwitchAt(place);
        }

        if (worked) {
            actor.inform("You hear the muffled click of a switch.");
        }
        return worked;
    }

    /**
     * The key a sign asks for, or nothing where it asks for none.
     *
     * <p>A line naming something the server has never heard of is treated as naming nothing, so a
     * misspelt key leaves a switch anybody can work rather than one nobody can.
     */
    public static Optional<Key> keyOn(PostedSign sign, MechanicWorld world) {
        String written = sign.line(KEY_LINE).trim();
        return written.isEmpty() ? Optional.empty() : world.resolveItem(written);
    }

    /**
     * The four places a hidden switch throws.
     *
     * <p>Above, below, and the two along the wall the sign is on. Never the block the sign hangs
     * on and never the one in front of it, since neither can hold a switch the sign is behind.
     */
    public static List<Vec3i> switchesAround(PostedSign sign) {
        List<Vec3i> places = new ArrayList<>(4);
        places.add(sign.position().offset(BlockFace.UP));
        places.add(sign.position().offset(BlockFace.DOWN));
        places.add(sign.position().offset(sign.left()));
        places.add(sign.position().offset(sign.right()));
        return places;
    }

    /** Whether a sign carries one of these names. */
    public static boolean isHiddenSwitch(SignLines lines) {
        return INSTANCE.claims(lines);
    }
}
