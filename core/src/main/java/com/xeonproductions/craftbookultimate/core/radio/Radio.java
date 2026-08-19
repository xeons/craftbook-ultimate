package com.xeonproductions.craftbookultimate.core.radio;

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

    /** Forgets every band, as though nothing had ever transmitted. */
    public void clear() {
        signals.clear();
    }
}
