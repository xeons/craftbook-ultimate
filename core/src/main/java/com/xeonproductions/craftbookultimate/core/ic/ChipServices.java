package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.control.PasswordStore;
import com.xeonproductions.craftbookultimate.core.control.Switchboard;
import com.xeonproductions.craftbookultimate.core.radio.Radio;
import com.xeonproductions.craftbookultimate.core.transport.Destinations;
import org.jspecify.annotations.NullMarked;

/**
 * The registries chips use to reach one another, and the people typing commands at them.
 *
 * <p>Everything else a chip sees belongs to its own corner of the world. These are shared by every
 * chip on the server, because the whole point of a wireless band, a named destination or a
 * commanded switch is that the two ends are nowhere near each other.
 *
 * <p>They are handed to chips rather than reached through static state, so a test can stand up its
 * own set and two tests cannot leak into each other.
 *
 * <p>The two switchboards are deliberately separate. A switch called {@code door} that anyone may
 * throw and one of the same name that takes a password are different switches, and merging them
 * would let the first be used to open the second.
 *
 * @param radio what every wireless band is carrying
 * @param destinations which destination answers to each name
 * @param switchboard the switches anyone may throw by command
 * @param guardedSwitchboard the switches that take a password
 * @param passwords the passwords guarding those switches
 */
@NullMarked
public record ChipServices(
        Radio radio,
        Destinations destinations,
        Switchboard switchboard,
        Switchboard guardedSwitchboard,
        PasswordStore passwords) {

    /** A fresh set, with nothing transmitting, no destinations claimed and no switches known. */
    public static ChipServices create() {
        return new ChipServices(
                new Radio(), new Destinations(), new Switchboard(), new Switchboard(), new PasswordStore());
    }
}
