// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * What a dispenser can put on the rails, or in the water.
 *
 * <p>Wider than {@link CartType} because a dispenser also handles boats, which are not carts at
 * all but are stored in a chest and put out the same way. Narrower because nobody dispenses a
 * spawner cart.
 *
 * <p>Each kind knows both the item it is stored as and where it can be put down, which is what
 * lets one dispenser sign serve all of them.
 */
@NullMarked
public enum VehicleKind {

    /** The plain minecart. */
    MINECART("minecart", "minecart", true),

    /** The chest minecart, written on a sign as {@code storage}. */
    STORAGE("storage", "chest_minecart", true),

    /** The hopper minecart. */
    HOPPER("hopper", "hopper_minecart", true),

    /** The furnace minecart, written on a sign as {@code powered}. */
    POWERED("powered", "furnace_minecart", true),

    /** A boat, which goes on water rather than on rails. */
    BOAT("boat", "oak_boat", false);

    private final String signName;
    private final Key item;
    private final boolean ridesOnRails;

    VehicleKind(String signName, String item, boolean ridesOnRails) {
        this.signName = signName;
        this.item = Blocks.key(item);
        this.ridesOnRails = ridesOnRails;
    }

    /** What a sign calls this. */
    public String signName() {
        return signName;
    }

    /**
     * The item this is stored as.
     *
     * <p>A boat has one item per wood, so this is the plain one; a dispenser filled with birch
     * boats matches on the item actually in the chest rather than on this.
     */
    public Key item() {
        return item;
    }

    /** Whether it needs a rail to be put down on, rather than water. */
    public boolean ridesOnRails() {
        return ridesOnRails;
    }

    /** The cart this becomes once it is out, empty for a boat. */
    public Optional<CartType> cartType() {
        return switch (this) {
            case MINECART -> Optional.of(CartType.RIDEABLE);
            case STORAGE -> Optional.of(CartType.CHEST);
            case HOPPER -> Optional.of(CartType.HOPPER);
            case POWERED -> Optional.of(CartType.FURNACE);
            case BOAT -> Optional.empty();
        };
    }

    /** What a name on a sign refers to, if it refers to one of these. */
    public static Optional<VehicleKind> bySignName(String written) {
        String name = written.trim().toLowerCase(Locale.ROOT);
        for (VehicleKind kind : values()) {
            if (kind.signName.equals(name)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
