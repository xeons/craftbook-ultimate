package com.xeonproductions.craftbookultimate.paper.cart;

import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.paper.stock.ContainerStockpile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * Several containers treated as one store.
 *
 * <p>What a loading bay is: a row of chests beside the track that fill and empty as one, so a
 * builder can add another chest rather than having to make the first one bigger.
 *
 * <p>Takes from the first container that has what is wanted and gives to the first with room,
 * which spreads a load across the row in the order the blocks were found.
 */
@NullMarked
public record SpreadStockpile(List<Inventory> inventories) implements Stockpile {

    private List<Stockpile> parts() {
        List<Stockpile> parts = new ArrayList<>(inventories.size());
        for (Inventory inventory : inventories) {
            parts.add(new ContainerStockpile(inventory));
        }
        return parts;
    }

    /** Whether there is anywhere at all to put things. */
    public boolean isEmpty() {
        return inventories.isEmpty();
    }

    @Override
    public int count(Key item) {
        int total = 0;
        for (Stockpile part : parts()) {
            total += part.count(item);
        }
        return total;
    }

    @Override
    public int take(Key item, int amount) {
        int taken = 0;
        for (Stockpile part : parts()) {
            if (taken >= amount) {
                break;
            }
            taken += part.take(item, amount - taken);
        }
        return taken;
    }

    @Override
    public int give(Key item, int amount) {
        int refused = amount;
        for (Stockpile part : parts()) {
            if (refused <= 0) {
                break;
            }
            refused = part.give(item, refused);
        }
        return Math.max(0, refused);
    }

    @Override
    public int countRoomFor(Key item) {
        int room = 0;
        for (Stockpile part : parts()) {
            room += part.countRoomFor(item);
        }
        return room;
    }

    @Override
    public Map<Key, Integer> contents() {
        Map<Key, Integer> all = new LinkedHashMap<>();
        for (Stockpile part : parts()) {
            part.contents().forEach((item, count) -> all.merge(item, count, Integer::sum));
        }
        return all;
    }
}
