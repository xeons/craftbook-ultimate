package com.xeonproductions.craftbookultimate.core.entity;

import com.xeonproductions.craftbookultimate.core.world.BlockReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * What a sign names when it names a creature.
 *
 * <p>The same text serves two purposes. A spawner reads it as a recipe for something to put in the
 * world; a zapper or a sensor reads it as a description of what to look for. Both spellings are
 * frozen, since signs carrying them are already in the ground.
 *
 * <p>The simple forms are a kind of thing ({@code pig}, {@code zombie}), a whole group
 * ({@code mobs}, {@code animals}), a player ({@code player}, {@code p:Notch}, {@code g:admin},
 * {@code m:ott}), a dropped stack ({@code item:35@14}) or one of a family ({@code minecart},
 * {@code arrow}, {@code entityhorse}). Three kinds take a property after an {@code @}:
 * {@code sheep@13} for a green sheep, {@code pig@1} for a saddled one and {@code creeper@1} for a
 * charged one.
 *
 * <p>Riders stack with {@code +}, so {@code pig+zombie} is a zombie on a pig. A backslash steps
 * back down the stack, so {@code pig+cow+pig\cow} puts both a pig and a cow on the cow. Extra data
 * goes in braces after the thing it applies to, as {@code zombie{IsBaby:1b}}; that is used when
 * spawning and ignored when matching, and only the spawner can carry it.
 */
@NullMarked
public sealed interface EntitySpec {

    /** Whether something standing near a chip is one of the things this names. */
    boolean matches(Bystander bystander);

    /** Whether a chip could put one of these in the world. */
    default boolean isSpawnable() {
        return false;
    }

    /**
     * How the {@code item:} form works out which item a sign means.
     *
     * <p>A sign written before the flattening names one by number and damage, which only the
     * server can resolve, so a chip passes {@code ChipWorld::resolveItem}. The default understands
     * modern names and the numeric forms that need no server table.
     */
    Function<String, Optional<Key>> DEFAULT_ITEMS =
            written -> BlockReference.parse(written).flatMap(BlockReference::asKey);

    /**
     * Reads a whole entity description, riders and all.
     *
     * @param written the text as it appears on the sign
     * @param items how to work out which item an {@code item:} form names
     * @return the description, or empty if the text does not make sense
     */
    static Optional<EntitySpec> parse(String written, Function<String, Optional<Key>> items) {
        return Stacking.parse(written, items);
    }

    /** Reads a whole entity description, resolving item names without a server. */
    static Optional<EntitySpec> parse(String written) {
        return parse(written, DEFAULT_ITEMS);
    }

    /**
     * Reads one entity description, with no riders and no extra data.
     *
     * <p>Most chips only ever name one thing; only the spawner builds stacks.
     */
    static Optional<EntitySpec> parseOne(String written, Function<String, Optional<Key>> items) {
        return Descriptors.parse(written, items);
    }

    /** Reads one entity description, resolving item names without a server. */
    static Optional<EntitySpec> parseOne(String written) {
        return parseOne(written, DEFAULT_ITEMS);
    }

    /** Every hostile mob, or every animal. */
    record Category(Group group) implements EntitySpec {

        @Override
        public boolean matches(Bystander bystander) {
            return switch (group) {
                case MONSTERS -> bystander.isMonster();
                case ANIMALS -> bystander.isAnimal();
                case CREATURES -> bystander.isMonster() || bystander.isAnimal();
            };
        }
    }

    /**
     * The groups a sign can name wholesale.
     *
     * <p>{@link #CREATURES} covers both of the others. No word on a sign selects it — it is what a
     * chip means when its sign says nothing and it watches for anything alive.
     */
    enum Group {
        MONSTERS,
        ANIMALS,
        CREATURES
    }

    /**
     * One kind of thing, with any properties the sign asked for.
     *
     * @param type the kind, named the way the game names it
     * @param wanted the properties it must have, or which to give it when spawning
     */
    record OfType(Key type, Expectations wanted) implements EntitySpec {

        /** One kind of thing with nothing else asked of it. */
        public static OfType of(Key type) {
            return new OfType(type, Expectations.ANY);
        }

        @Override
        public boolean matches(Bystander bystander) {
            return bystander.type().equals(type) && wanted.satisfiedBy(bystander.traits());
        }

        @Override
        public boolean isSpawnable() {
            return true;
        }
    }

    /**
     * Any of a family of related kinds.
     *
     * <p>Signs name a few families by one word: every minecart is {@code minecart}, every horse is
     * {@code entityhorse}. A family is a fixed set of kinds rather than a question put to the
     * game, so a kind added by a later version has to be added here to join one.
     */
    record OfAnyType(Set<Key> types) implements EntitySpec {

        public OfAnyType {
            types = Set.copyOf(types);
        }

        @Override
        public boolean matches(Bystander bystander) {
            return types.contains(bystander.type());
        }
    }

    /**
     * A stack lying on the ground.
     *
     * @param item which item, or empty for any
     */
    record Dropped(Optional<Key> item) implements EntitySpec {

        /** The kind of thing a dropped stack is. */
        public static final Key ITEM_ENTITY = Key.key(Key.MINECRAFT_NAMESPACE, "item");

        @Override
        public boolean matches(Bystander bystander) {
            if (!bystander.type().equals(ITEM_ENTITY)) {
                return false;
            }
            return item.isEmpty() || bystander.carriedItem().filter(item.get()::equals).isPresent();
        }

        @Override
        public boolean isSpawnable() {
            return item.isPresent();
        }
    }

    /**
     * A player, either any of them or a particular sort.
     *
     * @param match how the parameter is compared
     * @param parameter the name, group or fragment being looked for
     * @param negated whether the sign asked for players that do <em>not</em> match
     */
    record Person(Match match, String parameter, boolean negated) implements EntitySpec {

        /** Any player at all. */
        public static final Person ANY = new Person(Match.ANY, "", false);

        /** The ways a sign can pick players out. */
        public enum Match {
            /** Every player. */
            ANY,
            /** The player with exactly this account name. */
            NAMED,
            /** Every player in this permission group. */
            IN_GROUP,
            /** Every player whose name contains this fragment. */
            NAME_CONTAINS
        }

        @Override
        public boolean matches(Bystander bystander) {
            if (!bystander.isPlayer()) {
                return false;
            }
            boolean fits =
                    switch (match) {
                        case ANY -> true;
                        case NAMED -> bystander.name().equals(parameter);
                        case IN_GROUP -> bystander.isInGroup(parameter);
                        case NAME_CONTAINS ->
                                bystander
                                        .name()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(parameter.toLowerCase(Locale.ROOT));
                    };
            return fits != negated;
        }
    }

    /**
     * Something with riders on it.
     *
     * <p>Matching allows more riders than were asked for, so a sign looking for a pig with a
     * zombie on it finds one that also has a chicken.
     *
     * @param vehicle what is underneath
     * @param riders what sits on it, each of which may carry riders of its own
     */
    record Mounted(EntitySpec vehicle, List<EntitySpec> riders) implements EntitySpec {

        public Mounted {
            riders = List.copyOf(riders);
        }

        @Override
        public boolean matches(Bystander bystander) {
            if (!vehicle.matches(bystander)) {
                return false;
            }
            for (EntitySpec rider : riders) {
                if (bystander.riders().stream().noneMatch(rider::matches)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isSpawnable() {
            return vehicle.isSpawnable() && riders.stream().allMatch(EntitySpec::isSpawnable);
        }
    }

    /**
     * Something with extra data written after it in braces.
     *
     * <p>The data is passed through untouched to whatever spawns the thing, which is the only
     * place it is used. Matching ignores it and asks only about the thing itself, which costs
     * nothing in practice: the spawner is the one chip whose sign can carry data at all.
     *
     * @param base what the data applies to
     * @param data the braces and everything between them, as written
     */
    record WithData(EntitySpec base, String data) implements EntitySpec {

        @Override
        public boolean matches(Bystander bystander) {
            return base.matches(bystander);
        }

        @Override
        public boolean isSpawnable() {
            return base.isSpawnable();
        }
    }

    /**
     * The properties a sign can ask for after an {@code @}.
     *
     * <p>An absent field means the sign did not say, which matches anything and leaves the spawned
     * thing however the game makes it.
     */
    record Expectations(
            Optional<Boolean> charged, Optional<Boolean> saddled, Optional<String> dyeColour) {

        /** Nothing asked for. */
        public static final Expectations ANY =
                new Expectations(Optional.empty(), Optional.empty(), Optional.empty());

        /** Whether a creeper is charged. */
        public static Expectations charged(boolean charged) {
            return new Expectations(Optional.of(charged), Optional.empty(), Optional.empty());
        }

        /** Whether a pig is saddled. */
        public static Expectations saddled(boolean saddled) {
            return new Expectations(Optional.empty(), Optional.of(saddled), Optional.empty());
        }

        /** What colour a sheep is. */
        public static Expectations dyed(String colour) {
            return new Expectations(Optional.empty(), Optional.empty(), Optional.of(colour));
        }

        /** Whether something's properties are what was asked for. */
        public boolean satisfiedBy(Bystander.Traits traits) {
            if (charged.isPresent() && charged.get() != traits.charged()) {
                return false;
            }
            if (saddled.isPresent() && saddled.get() != traits.saddled()) {
                return false;
            }
            return dyeColour.isEmpty() || dyeColour.equals(traits.dyeColour());
        }
    }

    /** Reads one entity description: a kind, a group, a player or a family. */
    final class Descriptors {

        /** The kinds of minecart the numbered form names, in order. */
        private static final List<String> MINECART_KINDS =
                List.of("minecart", "chest_minecart", "furnace_minecart");

        /** Every kind of minecart, which is what the bare word names. */
        private static final Set<Key> MINECARTS =
                keys(
                        "minecart",
                        "chest_minecart",
                        "furnace_minecart",
                        "tnt_minecart",
                        "hopper_minecart",
                        "spawner_minecart",
                        "command_block_minecart");

        /** Every kind of horse, which is what {@code entityhorse} names. */
        private static final Set<Key> HORSES =
                keys("horse", "donkey", "mule", "skeleton_horse", "zombie_horse");

        /** Every kind of arrow. */
        private static final Set<Key> ARROWS = keys("arrow", "spectral_arrow");

        /** The prefixes that pick a player out, longest spelling of each last. */
        private static final List<String> PERSON_PREFIXES =
                List.of("p", "ply", "player", "g", "grp", "group", "m", "match");

        private Descriptors() {}

        static Optional<EntitySpec> parse(String written, Function<String, Optional<Key>> items) {
            String trimmed = written.trim();
            if (trimmed.isEmpty()) {
                return Optional.empty();
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);

            // A player spelling is answered here whether or not it makes sense, so that a
            // malformed one is refused rather than falling through to be read as a kind of thing.
            if (lower.equals("p") || lower.equals("ply") || lower.equals("player")) {
                return Optional.of(Person.ANY);
            }
            for (String prefix : PERSON_PREFIXES) {
                if (lower.startsWith(prefix + ":")) {
                    return parsePerson(trimmed, prefix);
                }
            }

            return switch (lower) {
                case "mob", "mobs" -> Optional.of(new Category(Group.MONSTERS));
                case "animal", "animals" -> Optional.of(new Category(Group.ANIMALS));
                case "entityhorse" -> Optional.of(new OfAnyType(HORSES));
                case "minecart" -> Optional.of(new OfAnyType(MINECARTS));
                case "arrow" -> Optional.of(new OfAnyType(ARROWS));
                case "item" -> Optional.of(new Dropped(Optional.empty()));
                default -> parseDetailed(lower, items);
            };
        }

        private static Optional<EntitySpec> parseDetailed(
                String lower, Function<String, Optional<Key>> items) {
            if (lower.startsWith("minecart:")) {
                return numbered(lower.substring("minecart:".length()), MINECART_KINDS.size())
                        .map(index -> OfType.of(vanilla(MINECART_KINDS.get(index))));
            }
            if (lower.startsWith("item:")) {
                return items.apply(lower.substring("item:".length()))
                        .map(item -> new Dropped(Optional.of(item)));
            }
            if (lower.startsWith("sheep@")) {
                return numbered(lower.substring("sheep@".length()), DyeColours.count())
                        .flatMap(DyeColours::byNumber)
                        .map(colour -> new OfType(vanilla("sheep"), Expectations.dyed(colour)));
            }
            if (lower.startsWith("pig@")) {
                return numbered(lower.substring("pig@".length()), 2)
                        .map(saddled -> new OfType(vanilla("pig"), Expectations.saddled(saddled == 1)));
            }
            if (lower.startsWith("creeper@")) {
                return numbered(lower.substring("creeper@".length()), 2)
                        .map(charged -> new OfType(vanilla("creeper"), Expectations.charged(charged == 1)));
            }
            return key(lower).map(OfType::of);
        }

        private static Optional<EntitySpec> parsePerson(String written, String prefix) {
            String parameter = written.substring(prefix.length() + 1);
            boolean negated = parameter.startsWith("!");
            if (negated) {
                parameter = parameter.substring(1);
            }
            if (parameter.isEmpty()) {
                return Optional.empty();
            }

            Person.Match match =
                    switch (prefix) {
                        case "p", "ply", "player" -> Person.Match.NAMED;
                        case "g", "grp", "group" -> Person.Match.IN_GROUP;
                        default -> Person.Match.NAME_CONTAINS;
                    };
            return Optional.of(new Person(match, parameter, negated));
        }

        private static Optional<Integer> numbered(String written, int exclusiveMax) {
            try {
                int value = Integer.parseInt(written.trim());
                return value < 0 || value >= exclusiveMax ? Optional.empty() : Optional.of(value);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        private static Optional<Key> key(String name) {
            String cleaned = name.trim().replace(' ', '_');
            if (cleaned.isEmpty()) {
                return Optional.empty();
            }
            try {
                Key key =
                        cleaned.indexOf(':') >= 0
                                ? Key.key(cleaned)
                                : Key.key(Key.MINECRAFT_NAMESPACE, cleaned);
                return key.value().isEmpty() ? Optional.empty() : Optional.of(key);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        private static Key vanilla(String name) {
            return Key.key(Key.MINECRAFT_NAMESPACE, name);
        }

        private static Set<Key> keys(String... names) {
            Set<Key> set = new LinkedHashSet<>();
            for (String name : names) {
                set.add(vanilla(name));
            }
            return Set.copyOf(set);
        }
    }

    /** Reads a description that may stack riders and carry extra data. */
    final class Stacking {

        /** Puts what follows on top of what came before. */
        private static final char RIDER = '+';

        /** Steps back down the stack, so what follows sits beside rather than on top. */
        private static final char DOWN = '\\';

        private static final char DATA_OPEN = '{';
        private static final char DATA_CLOSE = '}';

        private Stacking() {}

        static Optional<EntitySpec> parse(String written, Function<String, Optional<Key>> items) {
            String trimmed = written.trim();
            if (trimmed.isEmpty()) {
                return Optional.empty();
            }

            Node root = null;
            Node cursor = null;
            int unread = 0;
            int braceDepth = 0;
            boolean afterData = false;

            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c != RIDER && c != DOWN && c != DATA_OPEN && c != DATA_CLOSE) {
                    continue;
                }
                if (i == 0) {
                    return Optional.empty();
                }

                // A rider marker straight after a closing brace has no description of its own
                // between the two, since the data belonged to the thing before it.
                boolean readsDescription =
                        braceDepth == 0 && c != DATA_CLOSE && !(afterData && c == RIDER);

                if (readsDescription) {
                    Optional<EntitySpec> parsed = Descriptors.parse(trimmed.substring(unread, i), items);
                    if (parsed.isEmpty()) {
                        return Optional.empty();
                    }
                    if (cursor == null) {
                        root = new Node(parsed.get());
                        cursor = root;
                    } else {
                        cursor = cursor.addRider(parsed.get());
                    }
                }

                switch (c) {
                    case DOWN -> {
                        if (cursor == null || cursor.carrier == null) {
                            return Optional.empty();
                        }
                        cursor = cursor.carrier;
                    }
                    case DATA_OPEN -> braceDepth++;
                    case DATA_CLOSE -> {
                        if (braceDepth == 0 || cursor == null) {
                            return Optional.empty();
                        }
                        braceDepth--;
                        if (braceDepth == 0) {
                            cursor.spec = new WithData(cursor.spec, trimmed.substring(unread - 1, i + 1));
                        }
                    }
                    default -> {
                        // A rider marker needs nothing beyond the description already read.
                    }
                }

                if (braceDepth > (c == DATA_OPEN ? 1 : 0)) {
                    continue;
                }
                afterData = c == DATA_CLOSE;
                unread = i + 1;
            }

            if (braceDepth != 0) {
                return Optional.empty();
            }

            if (unread != trimmed.length()) {
                Optional<EntitySpec> parsed = Descriptors.parse(trimmed.substring(unread), items);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                if (cursor == null) {
                    root = new Node(parsed.get());
                } else {
                    cursor.addRider(parsed.get());
                }
            }

            return root == null ? Optional.empty() : Optional.of(root.toSpec());
        }

        /** One thing part-way through being read, and whatever is riding it so far. */
        private static final class Node {

            private EntitySpec spec;
            private final List<Node> riders = new ArrayList<>();
            private final @Nullable Node carrier;

            Node(EntitySpec spec) {
                this(spec, null);
            }

            Node(EntitySpec spec, @Nullable Node carrier) {
                this.spec = spec;
                this.carrier = carrier;
            }

            Node addRider(EntitySpec rider) {
                Node node = new Node(rider, this);
                riders.add(node);
                return node;
            }

            EntitySpec toSpec() {
                if (riders.isEmpty()) {
                    return spec;
                }
                List<EntitySpec> mounted = new ArrayList<>(riders.size());
                for (Node rider : riders) {
                    mounted.add(rider.toSpec());
                }
                return new Mounted(spec, mounted);
            }
        }
    }
}
