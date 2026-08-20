package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ICMode;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that say something rather than do something.
 *
 * <p>Three audiences, and which one a chip has is what separates these from one another. Some
 * speak to whoever is standing near the sign, which is a matter of asking the world who is there.
 * Some speak to one player anywhere on the server, or to everybody at once, which is a matter of
 * handing a name and a piece of text to the {@linkplain ChipState#announcer() announcer} and never
 * touching anybody's blocks. The rest write to the server's log, where the audience is whoever
 * reads it afterwards.
 *
 * <p>The chips that measure a distance take it on line 1, from one to sixty-four blocks, and
 * default to sixty-four. Signs built under the old plugin have that line stamped with the chip's
 * own name in front of the number, so the number is read from the end of the line and a line
 * carrying only a name means the default.
 *
 * <p>Distance is measured from the block the sign hangs on, as everywhere else in the plugin, so a
 * chip's own sign is never a block of its reach.
 *
 * <p>A vanished player is neither spoken to nor named. Somebody who has taken trouble not to be
 * seen should not be given away by a log line saying how far off they are standing.
 *
 * <p>Two placeholders are understood by the chips that look around them. {@code %p} is the nearest
 * player's name, and {@code %a}, which only the fuller of the two log chips reads, is every player
 * in range with their distances. Where nobody is in range both read {@code [NONE_FOUND]}.
 */
@NullMarked
public final class Messages {

    /** The line carrying how far a chip reaches. */
    private static final int RANGE_LINE = 0;

    /** The line carrying the first half of what to say, or the chip's one setting. */
    private static final int SUBJECT_LINE = 2;

    /** The line carrying the second half of what to say. */
    private static final int MESSAGE_LINE = 3;

    /** How far the chips that look around them reach when their sign does not say. */
    private static final int DEFAULT_RANGE = 64;

    /** The shortest distance a sign may ask for. */
    private static final int MIN_RANGE = 1;

    /** The furthest a sign may ask for. */
    private static final int MAX_RANGE = 64;

    /** The number at the end of a range line, whatever the old plugin stamped in front of it. */
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    /** Stands for the nearest player's name. */
    private static final String NEAREST_MARK = "%p";

    /** Stands for every player in range, with their distances. */
    private static final String EVERYONE_MARK = "%a";

    /** What either placeholder reads as when nobody is in range. */
    private static final String NOBODY = "[NONE_FOUND]";

    /** What a line written by these chips is prefixed with in the log. */
    private static final String LOG_PREFIX = "[CB!] ";

    /** What the fuller log chip prefixes a line with when it has somebody to report. */
    private static final String IN_RANGE_PREFIX = "[CB!] In range players: ";

    /** How a player and their distance are written into a log line. */
    private static final String DISTANCE_FORMAT = "%s distance: %.1f";

    /** Written after the model reference to say the message lives in a book. */
    private static final char BOOK_MODE = 'B';

    /** Where a book-driven chip looks for its book when the sign does not say. */
    private static final Vec3i DEFAULT_BOOK_OFFSET = new Vec3i(0, 1, 0);

    /** Separates the parts of the offset to a book's container, and of a wait. */
    private static final String FIELD_SEPARATOR = ":";

    /** What a line in a book opens with when it is a wait rather than a message. */
    private static final String DELAY_OPENING = "[DELAY:";

    /** What such a line closes with. */
    private static final char DELAY_CLOSING = ']';

    /** Ticks in a second, which is the unit the scheduler counts in. */
    private static final int TICKS_PER_SECOND = 20;

    /** Milliseconds in a tick. */
    private static final int MILLISECONDS_PER_TICK = 50;

    /** The longest a book may hold a message back, so a mistyped page cannot lose one for a day. */
    private static final long MAX_DELAY_TICKS = 3600L * TICKS_PER_SECOND;

    /** Stands in for a line break in the one chip whose message may carry them. */
    private static final String LINE_BREAK_MARK = "/n";

    /**
     * Reads the {@code &} colour codes a builder writes on a sign.
     *
     * <p>Only the message named nearby has ever taken them, and signs in the world are written
     * that way, so they are part of what that chip's sign means. Nothing here writes them back
     * out: they become a component the moment they are read.
     */
    private static final LegacyComponentSerializer COLOUR_CODES =
            LegacyComponentSerializer.legacyAmpersand();

    private Messages() {}

    /**
     * Says something to one named player, wherever they are.
     *
     * <p>Line 3 is the account name and line 4 is what to say. The output reports whether they
     * were online to hear it, which is what makes this usable as a presence check that also
     * speaks.
     */
    public static ICLogic playerMessenger() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            String name = state.sign().trimmedText(SUBJECT_LINE);
            String message = state.sign().text(MESSAGE_LINE);
            state.setMainOutput(
                    !name.isEmpty() && state.announcer().toNamed(name, Component.text(message)));
        };
    }

    /**
     * Says something to everybody online.
     *
     * <p>Line 3 is what to say. The output is high while the chip is being driven and there is
     * something to say, so a builder can chain another chip off it.
     */
    public static ICLogic messageAll() {
        return state -> {
            String message = state.sign().text(SUBJECT_LINE);
            if (!state.isAnyInputActive() || message.isBlank()) {
                state.setMainOutput(false);
                return;
            }

            state.announcer().toEveryone(Component.text(message));
            state.setMainOutput(true);
        };
    }

    /**
     * Says something to everybody standing within range.
     *
     * <p>Lines 3 and 4 together are what to say. Written with a {@code B} after the model
     * reference the message comes out of a book instead: line 3 then reads {@code x:y:z}, the
     * offset to the container holding it, which defaults to the block directly above, and every
     * line of every page is said in turn.
     *
     * <p>A book may also hold waits, written {@code [DELAY:20]} for twenty ticks,
     * {@code [DELAY:3:S]} for three seconds or {@code [DELAY:500:MS]} for half a second. A wait
     * holds back everything after it rather than only the next line, so a page reads as a script
     * played at the speed it is written at. Waits are counted in ticks, so one shorter than a tick
     * is rounded up to one, and a page cannot hold a message back by more than an hour.
     */
    public static ICLogic messageNearby() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            List<String> script = readsFromBook(state) ? bookLines(state) : signLines(state);
            for (Bystander person : peopleNear(state, rangeOn(state))) {
                say(state, person, script);
            }
            state.setMainOutput(true);
        };
    }

    /**
     * Says something to everybody within range, naming the nearest of them.
     *
     * <p>Lines 3 and 4 together are what to say, and unlike the rest of these chips it is written
     * with {@code &} colour codes. {@code %p} becomes the nearest player's name and {@code /n}
     * becomes a line break, so one sign can greet whoever walked up in front of everybody standing
     * around.
     *
     * <p>With nobody in range nothing is said at all, since there would be no name to put in.
     */
    public static ICLogic namedNearby() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            List<Bystander> audience = peopleNear(state, rangeOn(state));
            Optional<Bystander> nearest = nearestOf(audience, centreOf(state));
            if (nearest.isEmpty()) {
                state.setMainOutput(false);
                return;
            }

            String written = (state.sign().text(SUBJECT_LINE) + state.sign().text(MESSAGE_LINE))
                    .replace(NEAREST_MARK, nearest.get().name())
                    .replace(LINE_BREAK_MARK, "\n");
            Component message = COLOUR_CODES.deserialize(written);
            for (Bystander person : audience) {
                person.tell(message);
            }
            state.setMainOutput(true);
        };
    }

    /**
     * Writes a line to the server's log.
     *
     * <p>Lines 3 and 4 together are the line, prefixed so an operator can pick the plugin's own
     * lines out of a log.
     */
    public static ICLogic serverLog() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            state.announcer().toLog(LOG_PREFIX + messageOn(state));
            state.setMainOutput(true);
        };
    }

    /**
     * Writes a line to the log naming the nearest player.
     *
     * <p>Lines 3 and 4 together are the line and {@code %p} becomes the nearest player's name.
     * With nobody in range the line is still written, with {@code [NONE_FOUND]} in their place,
     * and the output stays low.
     *
     * <p>Written with a {@code +} after the model reference the same line is also said to the
     * player it named, which is how a doorway logs who came through and tells them it did.
     */
    public static ICLogic serverLogNearby() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            Optional<Bystander> nearest =
                    nearestOf(peopleNear(state, rangeOn(state)), centreOf(state));
            String written = messageOn(state);

            if (nearest.isEmpty()) {
                state.announcer().toLog(LOG_PREFIX + written.replace(NEAREST_MARK, NOBODY));
                state.setMainOutput(false);
                return;
            }

            String line = LOG_PREFIX + written.replace(NEAREST_MARK, nearest.get().name());
            state.announcer().toLog(line);
            if (tellsNearbyPlayers(state)) {
                nearest.get().tell(Component.text(line));
            }
            state.setMainOutput(true);
        };
    }

    /**
     * Writes a line to the log naming everybody in range and how far off they are.
     *
     * <p>The same lines and the same {@code %p} as the plainer log chip, and one more: {@code %a}
     * becomes every player in range, each with their distance to a tenth of a block. The nearest
     * player is written the same way, so {@code %p} says both who and how far.
     *
     * <p>Written with a {@code +} the line is also said to everybody it named.
     */
    public static ICLogic serverLogNearbyPlus() {
        return state -> {
            if (!state.isAnyInputActive()) {
                state.setMainOutput(false);
                return;
            }

            Vec3d centre = centreOf(state);
            List<Bystander> audience = peopleNear(state, rangeOn(state));
            String written = messageOn(state);

            if (audience.isEmpty()) {
                state.announcer()
                        .toLog(LOG_PREFIX
                                + written.replace(NEAREST_MARK, NOBODY).replace(EVERYONE_MARK, NOBODY));
                state.setMainOutput(false);
                return;
            }

            Bystander nearest = nearestOf(audience, centre).orElseThrow();
            String line = IN_RANGE_PREFIX
                    + written
                            .replace(EVERYONE_MARK, everybodyWithDistances(audience, centre))
                            .replace(NEAREST_MARK, withDistance(nearest, centre));

            state.announcer().toLog(line);
            if (tellsNearbyPlayers(state)) {
                Component said = Component.text(line);
                for (Bystander person : audience) {
                    person.tell(said);
                }
            }
            state.setMainOutput(true);
        };
    }

    /**
     * How far a chip reaches.
     *
     * <p>The number is read from the end of line 1 rather than from the whole of it, because the
     * old plugin stamped the chip's own name in front of it when the sign was made. A line with no
     * number on the end, stamped or blank, means the full sixty-four blocks.
     */
    static int rangeOn(ChipState state) {
        Matcher found = TRAILING_NUMBER.matcher(state.sign().trimmedText(RANGE_LINE));
        if (!found.find()) {
            return DEFAULT_RANGE;
        }
        try {
            return Math.clamp(Integer.parseInt(found.group(1)), MIN_RANGE, MAX_RANGE);
        } catch (NumberFormatException e) {
            return DEFAULT_RANGE;
        }
    }

    /** What a sign says, across the two lines that carry it. */
    private static String messageOn(ChipState state) {
        return state.sign().text(SUBJECT_LINE) + state.sign().text(MESSAGE_LINE);
    }

    /** Where a chip measures its range from, which is the block its sign hangs on. */
    private static Vec3d centreOf(ChipState state) {
        return Vec3d.middleOf(state.backPosition());
    }

    /** The players within range, leaving out anybody who is not meant to be seen. */
    private static List<Bystander> peopleNear(ChipState state, int range) {
        List<Bystander> people = new ArrayList<>();
        for (Bystander bystander : state.world().bystandersNear(centreOf(state), range)) {
            if (bystander.isPlayer() && bystander.isVisible()) {
                people.add(bystander);
            }
        }
        return people;
    }

    /** The closest of a group to a point. */
    private static Optional<Bystander> nearestOf(List<Bystander> people, Vec3d centre) {
        Bystander closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Bystander person : people) {
            double distance = person.position().distanceSquared(centre);
            if (distance < closestDistance) {
                closest = person;
                closestDistance = distance;
            }
        }
        return Optional.ofNullable(closest);
    }

    /** One player written with how far off they are. */
    private static String withDistance(Bystander person, Vec3d centre) {
        return String.format(
                Locale.ROOT,
                DISTANCE_FORMAT,
                person.name(),
                Math.sqrt(person.position().distanceSquared(centre)));
    }

    /** Everybody written that way, in one string. */
    private static String everybodyWithDistances(List<Bystander> people, Vec3d centre) {
        StringBuilder out = new StringBuilder();
        for (Bystander person : people) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(withDistance(person, centre));
        }
        return out.toString();
    }

    /** Whether the sign asks for what is logged to be said to the players it names. */
    private static boolean tellsNearbyPlayers(ChipState state) {
        return state.mode().behaviour() == ICMode.Behaviour.LOG_TO_NEARBY_PLAYERS;
    }

    /** Whether the sign asks for the message to come out of a book. */
    private static boolean readsFromBook(ChipState state) {
        String mode = state.modeText();
        return !mode.isEmpty() && mode.charAt(mode.length() - 1) == BOOK_MODE;
    }

    /** What a sign says, as the one message it is. */
    private static List<String> signLines(ChipState state) {
        String message = messageOn(state);
        return message.isBlank() ? List.of() : List.of(message);
    }

    /** What the book by a sign says, one entry per line of every page. */
    private static List<String> bookLines(ChipState state) {
        Vec3i where = state.backPosition()
                .add(offsetOn(state.sign().trimmedText(SUBJECT_LINE)).orElse(DEFAULT_BOOK_OFFSET));

        List<String> lines = new ArrayList<>();
        for (String page : state.world().bookPagesAt(where)) {
            lines.addAll(List.of(page.split("\\R")));
        }
        return lines;
    }

    /** An {@code x:y:z} offset, or empty if the line is not one. */
    private static Optional<Vec3i> offsetOn(String written) {
        String[] parts = written.split(FIELD_SEPARATOR);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Vec3i(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Says a script to one person.
     *
     * <p>Everything before the first wait is said at once; everything after it is scheduled, each
     * line carrying the waits that came before it, so the lines arrive in the order they are
     * written however far apart they are spaced.
     */
    private static void say(ChipState state, Bystander person, List<String> script) {
        long waited = 0;
        for (String line : script) {
            Optional<Long> wait = waitOn(line);
            if (wait.isPresent()) {
                waited = Math.min(MAX_DELAY_TICKS, waited + wait.get());
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }

            Component said = Component.text(line);
            if (waited <= 0) {
                person.tell(said);
            } else {
                state.scheduler()
                        .runLater(
                                () -> {
                                    if (person.isPresent()) {
                                        person.tell(said);
                                    }
                                },
                                waited);
            }
        }
    }

    /**
     * How long a book line asks to wait for, in ticks.
     *
     * <p>A bare number is counted in ticks, and {@code T}, {@code S} and {@code MS} name ticks,
     * seconds and milliseconds. A wait shorter than a tick is rounded up to one, since that is the
     * shortest the server can measure.
     *
     * @return the wait, or empty if the line is a message rather than a wait
     */
    private static Optional<Long> waitOn(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith(DELAY_OPENING)
                || trimmed.length() <= DELAY_OPENING.length()
                || trimmed.charAt(trimmed.length() - 1) != DELAY_CLOSING) {
            return Optional.empty();
        }

        String[] parts = trimmed
                .substring(DELAY_OPENING.length(), trimmed.length() - 1)
                .split(FIELD_SEPARATOR);
        long amount;
        try {
            amount = Long.parseLong(parts[0].trim());
        } catch (NumberFormatException e) {
            return Optional.of(0L);
        }
        if (amount <= 0) {
            return Optional.of(0L);
        }

        String unit = parts.length > 1 ? parts[1].trim().toUpperCase(Locale.ROOT) : "T";
        return Optional.of(switch (unit) {
            case "T", "TICKS" -> amount;
            case "S", "SECONDS" -> amount * TICKS_PER_SECOND;
            case "MS", "MILLISECONDS" -> Math.max(1, amount / MILLISECONDS_PER_TICK);
            default -> 0L;
        });
    }
}
