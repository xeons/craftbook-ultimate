package com.xeonproductions.craftbookultimate.core.ic;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.control.PasswordStore;
import com.xeonproductions.craftbookultimate.core.control.Switchboard;
import com.xeonproductions.craftbookultimate.core.effect.FireworkShows;
import com.xeonproductions.craftbookultimate.core.entity.Roster;
import com.xeonproductions.craftbookultimate.core.entity.SimpleRoster;
import com.xeonproductions.craftbookultimate.core.illusion.Illusions;
import com.xeonproductions.craftbookultimate.core.illusion.SimpleIllusions;
import com.xeonproductions.craftbookultimate.core.message.Announcer;
import com.xeonproductions.craftbookultimate.core.message.SimpleAnnouncer;
import com.xeonproductions.craftbookultimate.core.music.Songs;
import com.xeonproductions.craftbookultimate.core.radio.Radio;
import com.xeonproductions.craftbookultimate.core.transport.Destinations;
import org.jspecify.annotations.NullMarked;

/**
 * The registries chips use to reach one another, the people typing commands at them, and the
 * settings they all run under.
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
 * @param shows the firework displays the server has scripts for
 * @param songs the music the server has files for
 * @param roster who is on the server
 * @param announcer how a chip speaks to the server rather than to a place
 * @param illusions how a chip shows somebody something other than what is there
 * @param configuration the settings an operator has put in force
 */
@NullMarked
public record ChipServices(
        Radio radio,
        Destinations destinations,
        Switchboard switchboard,
        Switchboard guardedSwitchboard,
        PasswordStore passwords,
        FireworkShows shows,
        Songs songs,
        Roster roster,
        Announcer announcer,
        Illusions illusions,
        Configuration configuration) {

    /** A fresh set, with nothing transmitting, no destinations claimed and no switches known. */
    public static ChipServices create() {
        return create(SimpleRoster.empty(), new SimpleAnnouncer(), new SimpleIllusions());
    }

    /** A fresh set reading a particular roster, saying anything it is told to say to nobody. */
    public static ChipServices create(Roster roster) {
        return create(roster, new SimpleAnnouncer(), new SimpleIllusions());
    }

    /** A fresh set speaking through a particular announcer, reading an empty roster. */
    public static ChipServices create(Roster roster, Announcer announcer) {
        return create(roster, announcer, new SimpleIllusions());
    }

    /**
     * A fresh set reading a particular roster, speaking through a particular announcer and showing
     * illusions through a particular one of those, which is how the plugin supplies the real ones.
     */
    public static ChipServices create(Roster roster, Announcer announcer, Illusions illusions) {
        return new ChipServices(
                new Radio(),
                new Destinations(),
                new Switchboard(),
                new Switchboard(),
                new PasswordStore(),
                new FireworkShows(),
                new Songs(),
                roster,
                announcer,
                illusions,
                new Configuration());
    }
}
