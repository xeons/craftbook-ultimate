package com.xeonproductions.craftbookultimate.core.illusion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * An {@link Illusions} that remembers what everybody was shown instead of showing it.
 *
 * <p>Who is online, which group they are in and which world they are in are all set by the test,
 * so a chip that picks its audience off a sign can be exercised every way it can be written.
 */
@NullMarked
public final class SimpleIllusions implements Illusions {

    /** Somebody who might be shown something. */
    private record Person(String name, String group, UUID world) {}

    private final List<Person> online = new ArrayList<>();
    private final Map<String, Sky> shown = new LinkedHashMap<>();

    /** Puts somebody on the server, in a group and a world. */
    public SimpleIllusions with(String name, String group, UUID world) {
        online.add(new Person(name, group, world));
        return this;
    }

    /** Puts somebody on the server in no group in particular. */
    public SimpleIllusions with(String name, UUID world) {
        return with(name, "", world);
    }

    @Override
    public boolean showSkyToNamed(String nameFragment, Sky sky) {
        for (Person person : online) {
            if (person.name().contains(nameFragment)) {
                shown.put(key(person.name()), sky);
                return true;
            }
        }
        return false;
    }

    @Override
    public int showSkyToGroup(String group, Sky sky) {
        int count = 0;
        for (Person person : online) {
            if (person.group().equals(group)) {
                shown.put(key(person.name()), sky);
                count++;
            }
        }
        return count;
    }

    @Override
    public int showSkyIn(UUID world, Sky sky) {
        int count = 0;
        for (Person person : online) {
            if (person.world().equals(world)) {
                shown.put(key(person.name()), sky);
                count++;
            }
        }
        return count;
    }

    /**
     * What somebody is being shown.
     *
     * <p>{@link Sky#REAL} for anybody nothing has been done to, since that is what they see.
     */
    public Sky shownTo(String name) {
        return shown.getOrDefault(key(name), Sky.REAL);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
