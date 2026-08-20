package com.xeonproductions.craftbookultimate.core.cart;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * A world for a cart mechanic, held in memory.
 *
 * <p>Only what has been put in it exists: anywhere unmentioned is air, has no rail and holds no
 * sign. That keeps a test to the few blocks it actually cares about.
 */
@NullMarked
public final class SimpleCartWorld implements CartWorld {

    /** The floor, matching an ordinary world's. */
    private static final int MIN_HEIGHT = -64;

    /** One past the ceiling, matching an ordinary world's. */
    private static final int MAX_HEIGHT = 320;

    private final Map<Vec3i, Key> blocks = new HashMap<>();
    private final Map<Vec3i, RailShape> rails = new HashMap<>();
    private final Map<Vec3i, CartMechanism.MechanismSign> signs = new HashMap<>();
    private final Map<Vec3i, SimpleStockpile> containers = new HashMap<>();
    private final Map<String, CartRecipe> recipes = new HashMap<>();
    private final List<Cart> carts = new ArrayList<>();
    private final List<Bystander> people = new ArrayList<>();
    private final List<Dropped> dropped = new ArrayList<>();
    private final List<Cart> spawned = new ArrayList<>();
    private boolean everythingLoaded = true;
    private Optional<Set<String>> knownItems = Optional.empty();

    @Override
    public Key blockAt(Vec3i position) {
        return blocks.getOrDefault(position, Blocks.AIR_KEY);
    }

    @Override
    public boolean isRail(Vec3i position) {
        return rails.containsKey(position);
    }

    @Override
    public Optional<RailShape> railShapeAt(Vec3i position) {
        return Optional.ofNullable(rails.get(position));
    }

    @Override
    public boolean setRailShapeAt(Vec3i position, RailShape shape) {
        if (!rails.containsKey(position)) {
            return false;
        }
        rails.put(position, shape);
        return true;
    }

    @Override
    public Optional<CartMechanism.MechanismSign> signAt(Vec3i position) {
        return Optional.ofNullable(signs.get(position));
    }

    @Override
    public boolean isLoaded(Vec3i position) {
        return everythingLoaded;
    }

    @Override
    public int minHeight() {
        return MIN_HEIGHT;
    }

    @Override
    public int maxHeight() {
        return MAX_HEIGHT;
    }

    @Override
    public List<Cart> cartsNear(Vec3d centre, double radius) {
        List<Cart> near = new ArrayList<>();
        for (Cart cart : carts) {
            if (cart.isPresent() && cart.position().distanceSquared(centre) <= radius * radius) {
                near.add(cart);
            }
        }
        return near;
    }

    @Override
    public List<Bystander> playersNear(Vec3d centre, double radius) {
        List<Bystander> near = new ArrayList<>();
        for (Bystander person : people) {
            if (person.isPresent()
                    && person.isVisible()
                    && person.position().distanceSquared(centre) <= radius * radius) {
                near.add(person);
            }
        }
        return near;
    }

    @Override
    public Stockpile containersAt(List<Vec3i> positions) {
        // A view over the real containers rather than a copy of them, so what a mechanic takes
        // and gives is still there for a test to look at afterwards.
        return new SpreadStore(gather(positions));
    }

    @Override
    public boolean dropItem(Vec3d at, Key item, int count) {
        dropped.add(new Dropped(at, item, count));
        return true;
    }

    @Override
    public Optional<Cart> spawnVehicle(Vec3d at, VehicleKind kind, Optional<String> name) {
        Optional<CartType> type = kind.cartType();
        if (type.isEmpty()) {
            return Optional.empty();
        }
        SimpleCart cart = SimpleCart.of(type.get()).at(at);
        name.ifPresent(cart::named);
        carts.add(cart);
        spawned.add(cart);
        return Optional.of(cart);
    }

    @Override
    public Optional<CartRecipe> recipeNamed(String signName) {
        return Optional.ofNullable(recipes.get(signName));
    }

    @Override
    public Optional<Key> resolveItem(String written) {
        Optional<Key> parsed = CartWorld.super.resolveItem(written);
        // With no server behind it any well-formed name parses, so a test that cares whether a
        // name is really an item says which ones exist.
        return knownItems.isEmpty()
                ? parsed
                : parsed.filter(key -> knownItems.get().contains(key.value()));
    }

    /** Puts a block somewhere. */
    public SimpleCartWorld withBlock(Vec3i position, Key block) {
        blocks.put(position, block);
        return this;
    }

    /** Puts a block somewhere, by its bare name. */
    public SimpleCartWorld withBlock(Vec3i position, String block) {
        return withBlock(position, Blocks.key(block));
    }

    /** Lays a piece of rail. */
    public SimpleCartWorld withRail(Vec3i position, RailShape shape) {
        rails.put(position, shape);
        return this;
    }

    /** Puts up a sign. */
    public SimpleCartWorld withSign(Vec3i position, BlockFace facing, String... lines) {
        signs.put(position, new CartMechanism.MechanismSign(position, SignLines.of(lines), facing));
        return this;
    }

    /** Puts a container somewhere, with whatever is in it. */
    public SimpleCartWorld withContainer(Vec3i position, SimpleStockpile contents) {
        containers.put(position, contents);
        return this;
    }

    /** Puts a cart in the world, for something to find. */
    public SimpleCartWorld withCart(Cart cart) {
        carts.add(cart);
        return this;
    }

    /** Puts somebody in the world, for something to find. */
    public SimpleCartWorld withPerson(Bystander person) {
        people.add(person);
        return this;
    }

    /** Teaches it a recipe. */
    public SimpleCartWorld withRecipe(CartRecipe recipe) {
        recipes.put(recipe.name(), recipe);
        return this;
    }

    /**
     * Says which items exist, so a name that is not one of them resolves to nothing.
     *
     * <p>Without this any well-formed name is an item, which is what a world with no server behind
     * it can tell. A test checking that a mistyped sign is refused has to say otherwise.
     */
    public SimpleCartWorld knowingOnly(String... items) {
        this.knownItems = Optional.of(Set.of(items));
        return this;
    }

    /** Makes everywhere unreadable, as an unloaded chunk would. */
    public SimpleCartWorld unloaded() {
        this.everythingLoaded = false;
        return this;
    }

    /** Everything that has been dropped on the ground. */
    public List<Dropped> droppedItems() {
        return List.copyOf(dropped);
    }

    /** Every vehicle that has been put out. */
    public List<Cart> spawnedVehicles() {
        return List.copyOf(spawned);
    }

    /** The container at a position, for a test to assert on. */
    public Optional<SimpleStockpile> containerAt(Vec3i position) {
        return Optional.ofNullable(containers.get(position));
    }

    private List<SimpleStockpile> gather(List<Vec3i> positions) {
        List<SimpleStockpile> found = new ArrayList<>();
        for (Vec3i position : positions) {
            SimpleStockpile container = containers.get(position);
            if (container != null && !found.contains(container)) {
                found.add(container);
            }
        }
        return found;
    }

    /** Something lying on the ground. */
    public record Dropped(Vec3d at, Key item, int count) {}

    /**
     * Several containers treated as one store.
     *
     * <p>Takes from the first that has what is wanted and gives to the first with room, which is
     * how a row of chests beside a track behaves.
     */
    private record SpreadStore(List<SimpleStockpile> parts) implements Stockpile {

        @Override
        public int count(Key item) {
            int total = 0;
            for (SimpleStockpile part : parts) {
                total += part.count(item);
            }
            return total;
        }

        @Override
        public int take(Key item, int amount) {
            int taken = 0;
            for (SimpleStockpile part : parts) {
                taken += part.take(item, amount - taken);
                if (taken >= amount) {
                    break;
                }
            }
            return taken;
        }

        @Override
        public int give(Key item, int amount) {
            int refused = amount;
            for (SimpleStockpile part : parts) {
                refused = part.give(item, refused);
                if (refused <= 0) {
                    break;
                }
            }
            return Math.max(0, refused);
        }

        @Override
        public int countRoomFor(Key item) {
            int room = 0;
            for (SimpleStockpile part : parts) {
                room += part.countRoomFor(item);
            }
            return room;
        }

        @Override
        public Map<Key, Integer> contents() {
            Map<Key, Integer> all = new LinkedHashMap<>();
            for (SimpleStockpile part : parts) {
                part.contents().forEach((item, count) -> all.merge(item, count, Integer::sum));
            }
            return all;
        }
    }
}
