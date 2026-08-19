package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.radio.Radio;
import com.xeonproductions.craftbookultimate.core.transport.Destinations;
import org.jspecify.annotations.NullMarked;

/**
 * The registries chips use to reach one another rather than the blocks around them.
 *
 * <p>Everything else a chip sees belongs to its own corner of the world. These are shared by every
 * chip on the server, because the whole point of a wireless band or a named destination is that
 * the two ends are nowhere near each other.
 *
 * <p>They are handed to chips rather than reached through static state, so a test can stand up its
 * own pair and two tests cannot leak into each other.
 *
 * @param radio what every wireless band is carrying
 * @param destinations which destination answers to each name
 */
@NullMarked
public record ChipServices(Radio radio, Destinations destinations) {

    /** A fresh set, with nothing transmitting and no destinations claimed. */
    public static ChipServices create() {
        return new ChipServices(new Radio(), new Destinations());
    }
}
