// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.radio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

/**
 * What every wireless band is currently carrying.
 *
 * <p>Transmitters write here and receivers read, which is the whole of wireless redstone: the two
 * ends never see each other and need not be anywhere near each other. A receiver polls on its own
 * tick rather than being called back, so a transmitter never reaches into the region a receiver
 * lives in and no work crosses a thread boundary beyond the map itself.
 *
 * <p>A band that has never been transmitted on reads as unknown rather than as off, and a
 * receiver on one leaves its output alone. That matters after a restart, when nothing has
 * transmitted yet: a receiver holds whatever it was last driving instead of dropping every
 * wireless line on the server to low.
 *
 * <p>Safe to use from any number of regions at once.
 */
@NullMarked
public final class Radio {

    /** Separates the parts of a band when it is written out. */
    private static final char SEPARATOR = ' ';

    /** Stands in for the shared namespace, which has no name of its own. */
    private static final String BLANK_NAMESPACE = "-";

    private final Map<Band, Boolean> signals = new ConcurrentHashMap<>();

    /**
     * Sets what a band is carrying.
     *
     * @param band the band to drive
     * @param powered whether the band is carrying a signal
     */
    public void transmit(Band band, boolean powered) {
        signals.put(band, powered);
    }

    /**
     * What a band is carrying.
     *
     * @return the signal, or empty if nothing has ever transmitted on this band
     */
    public Optional<Boolean> signal(Band band) {
        return Optional.ofNullable(signals.get(band));
    }

    /** Whether a band is carrying a signal, treating a band never transmitted on as off. */
    public boolean isPowered(Band band) {
        return signals.getOrDefault(band, Boolean.FALSE);
    }

    /** The number of bands anything has transmitted on. */
    public int bandCount() {
        return signals.size();
    }

    /**
     * Every band and what it carries, written one to a line.
     *
     * <p>The signal comes first and the namespace second, so whatever follows the second space is
     * the channel name, which comes off a sign and may have spaces of its own. A namespace has
     * none, since it is either blank or a unique id.
     */
    public List<String> save() {
        List<String> lines = new ArrayList<>();
        signals.keySet().stream()
                .sorted(Comparator.comparing(Band::wide).thenComparing(Band::narrow))
                .forEach(band -> lines.add(
                        signals.get(band) + String.valueOf(SEPARATOR)
                                + (band.wide().isEmpty() ? BLANK_NAMESPACE : band.wide())
                                + SEPARATOR + band.narrow()));
        return lines;
    }

    /**
     * Reads bands back in, as {@link #save()} wrote them.
     *
     * <p>A line that is not a signal, a namespace and a channel is skipped, so somebody editing
     * the file by hand cannot cost themselves every other band.
     *
     * @return how many bands were read
     */
    public int load(List<String> lines) {
        int read = 0;
        for (String line : lines) {
            int afterSignal = line.indexOf(SEPARATOR);
            if (afterSignal <= 0) {
                continue;
            }
            int afterNamespace = line.indexOf(SEPARATOR, afterSignal + 1);
            if (afterNamespace < 0 || afterNamespace == line.length() - 1) {
                continue;
            }

            String signal = line.substring(0, afterSignal).trim().toLowerCase(Locale.ROOT);
            if (!signal.equals("true") && !signal.equals("false")) {
                continue;
            }

            String namespace = line.substring(afterSignal + 1, afterNamespace);
            String channel = line.substring(afterNamespace + 1);
            if (channel.isBlank()) {
                continue;
            }

            transmit(
                    new Band(namespace.equals(BLANK_NAMESPACE) ? "" : namespace, channel),
                    signal.equals("true"));
            read++;
        }
        return read;
    }

    /** Forgets every band, as though nothing had ever transmitted. */
    public void clear() {
        signals.clear();
    }
}
