// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A minecart held in memory.
 *
 * <p>Records what a mechanic did to it rather than doing anything, so a test can roll a cart over
 * a junction and then assert on which way it left.
 */
@NullMarked
public final class SimpleCart implements Cart {

    /** How fast a cart goes when it is pushed as hard as it can be. */
    private static final double DEFAULT_MAXIMUM_SPEED = 0.4;

    private final CartType type;
    private Vec3d position = Vec3d.ZERO;
    private Vec3d velocity = Vec3d.ZERO;
    private double maximumSpeed = DEFAULT_MAXIMUM_SPEED;
    private Optional<String> customName = Optional.empty();
    private final List<Bystander> riders = new ArrayList<>();
    private SimpleStockpile contents = SimpleStockpile.empty();
    private Optional<ItemView> firstStored = Optional.empty();
    private boolean present = true;
    private int seats = 1;

    public SimpleCart(CartType type) {
        this.type = type;
    }

    /** A plain minecart, which is the one people ride. */
    public static SimpleCart rideable() {
        return new SimpleCart(CartType.RIDEABLE);
    }

    /** A chest minecart, empty to start with. */
    public static SimpleCart storage() {
        return new SimpleCart(CartType.CHEST);
    }

    /** A hopper minecart, empty to start with. */
    public static SimpleCart hopper() {
        return new SimpleCart(CartType.HOPPER);
    }

    /** A cart of any kind. */
    public static SimpleCart of(CartType type) {
        return new SimpleCart(type);
    }

    @Override
    public CartType type() {
        return type;
    }

    @Override
    public Vec3d position() {
        return position;
    }

    @Override
    public Vec3d velocity() {
        return velocity;
    }

    @Override
    public boolean setVelocity(Vec3d velocity) {
        if (!present) {
            return false;
        }
        this.velocity = velocity;
        return true;
    }

    @Override
    public double maximumSpeed() {
        return maximumSpeed;
    }

    @Override
    public List<Bystander> riders() {
        return List.copyOf(riders);
    }

    @Override
    public Optional<String> customName() {
        return customName;
    }

    @Override
    public Optional<Stockpile> contents() {
        return type.holdsItems() ? Optional.of(contents) : Optional.empty();
    }

    @Override
    public Optional<ItemView> firstStoredItem() {
        if (firstStored.isPresent()) {
            return firstStored;
        }
        // Nothing has been singled out, so the first slot is simply the first thing it holds.
        for (Map.Entry<Key, Integer> stored : contents.contents().entrySet()) {
            return Optional.of(ItemView.of(stored.getKey(), stored.getValue()));
        }
        return Optional.empty();
    }

    @Override
    public boolean board(Bystander rider) {
        if (!present || riders.size() >= seats) {
            return false;
        }
        riders.add(rider);
        return true;
    }

    @Override
    public List<Bystander> ejectRiders() {
        if (!present) {
            return List.of();
        }
        List<Bystander> aboard = List.copyOf(riders);
        riders.clear();
        return aboard;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean remove() {
        if (!present) {
            return false;
        }
        present = false;
        return true;
    }

    @Override
    public boolean teleport(Vec3d to) {
        if (!present) {
            return false;
        }
        this.position = to;
        return true;
    }

    /** Puts the cart somewhere. */
    public SimpleCart at(Vec3d position) {
        this.position = position;
        return this;
    }

    /** Sets it going. */
    public SimpleCart moving(Vec3d velocity) {
        this.velocity = velocity;
        return this;
    }

    /** Says how fast it can be pushed. */
    public SimpleCart withMaximumSpeed(double maximumSpeed) {
        this.maximumSpeed = maximumSpeed;
        return this;
    }

    /** Names the cart, as an anvil would. */
    public SimpleCart named(String name) {
        this.customName = Optional.of(name);
        return this;
    }

    /** Puts somebody in it, past whatever room it has. */
    public SimpleCart carrying(Bystander rider) {
        riders.add(rider);
        return this;
    }

    /** Says how many it seats, which is one unless a test says otherwise. */
    public SimpleCart seating(int seats) {
        this.seats = seats;
        return this;
    }

    /** Puts something in its hold. */
    public SimpleCart holding(Key item, int count) {
        contents.give(item, count);
        return this;
    }

    /** Gives it a hold of a particular size and contents. */
    public SimpleCart withContents(SimpleStockpile contents) {
        this.contents = contents;
        return this;
    }

    /** Puts a particular thing in the first slot, whatever else the hold has in it. */
    public SimpleCart withFirstSlot(ItemView item) {
        this.firstStored = Optional.of(item);
        return this;
    }

    /** Takes it out of the world, as breaking it would. */
    public SimpleCart removed() {
        this.present = false;
        return this;
    }

    /** What the cart is carrying, for a test to assert on. */
    public SimpleStockpile hold() {
        return contents;
    }
}
