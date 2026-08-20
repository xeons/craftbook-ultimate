package com.xeonproductions.craftbookultimate.core.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;

/**
 * An {@link Announcer} that remembers what was said instead of saying it.
 *
 * <p>Who is online is set by the test rather than read from anywhere, so a chip that reports
 * whether it found its player can be exercised both ways without a server.
 */
@NullMarked
public final class SimpleAnnouncer implements Announcer {

    private final Set<String> online = new LinkedHashSet<>();
    private final List<Component> everyone = new ArrayList<>();
    private final Map<String, List<Component>> named = new LinkedHashMap<>();
    private final List<String> log = new ArrayList<>();

    /** Says who is on the server, replacing whoever was before. */
    public SimpleAnnouncer withOnline(String... names) {
        online.clear();
        for (String name : names) {
            online.add(name.toLowerCase(Locale.ROOT));
        }
        return this;
    }

    @Override
    public void toEveryone(Component message) {
        everyone.add(message);
    }

    @Override
    public boolean toNamed(String name, Component message) {
        if (!online.contains(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        named.computeIfAbsent(name.toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(message);
        return true;
    }

    @Override
    public void toLog(String line) {
        log.add(line);
    }

    /** Everything said to the whole server, in order. */
    public List<String> everyone() {
        return plain(everyone);
    }

    /** Everything said to one player, in order. */
    public List<String> to(String name) {
        return plain(named.getOrDefault(name.toLowerCase(Locale.ROOT), List.of()));
    }

    /** Everything written to the log, in order. */
    public List<String> log() {
        return List.copyOf(log);
    }

    private static List<String> plain(List<Component> messages) {
        return messages.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList();
    }
}
