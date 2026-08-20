package com.xeonproductions.craftbookultimate.core.entity;

import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.stock.SimpleStockpile;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;

/**
 * Something standing near a chip, held in memory.
 *
 * <p>Records what a chip did to it rather than doing anything, so a test can put a zombie in front
 * of a chip and then assert on the damage it took.
 */
@NullMarked
public final class SimpleBystander implements Bystander {

    private final Key type;
    private Vec3d position;
    private boolean player;
    private boolean living = true;
    private boolean monster;
    private boolean animal;
    private String name = "";
    private Traits traits = Traits.NONE;
    private Optional<Key> carried = Optional.empty();
    private Optional<ItemView> held = Optional.empty();
    private boolean visible = true;
    private boolean tamed;
    private final Set<String> groups = new LinkedHashSet<>();
    private Optional<Stockpile> inventory = Optional.empty();
    private Optional<UUID> uniqueId = Optional.empty();
    private final List<Bystander> riders = new ArrayList<>();
    private final List<PotionDose> doses = new ArrayList<>();
    private final List<Component> messages = new ArrayList<>();
    private Sky shownSky = Sky.REAL;
    private double damageTaken;
    private boolean present = true;

    public SimpleBystander(Key type) {
        this.type = type;
        this.position = Vec3d.ZERO;
    }

    /** A vanilla creature by its bare name. */
    public static SimpleBystander of(String type) {
        return new SimpleBystander(Key.key(Key.MINECRAFT_NAMESPACE, type));
    }

    /** A hostile mob of the named kind. */
    public static SimpleBystander monster(String type) {
        return of(type).asMonster();
    }

    /** An animal of the named kind. */
    public static SimpleBystander animal(String type) {
        return of(type).asAnimal();
    }

    /** A player with the given account name. */
    public static SimpleBystander player(String name) {
        return of("player").asPlayer(name);
    }

    @Override
    public Key type() {
        return type;
    }

    @Override
    public Vec3d position() {
        return position;
    }

    @Override
    public boolean isPlayer() {
        return player;
    }

    @Override
    public boolean isLiving() {
        return living;
    }

    @Override
    public boolean isMonster() {
        return monster;
    }

    @Override
    public boolean isAnimal() {
        return animal;
    }

    @Override
    public boolean isTamed() {
        return tamed;
    }

    @Override
    public Optional<ItemView> heldItem() {
        return held;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isInGroup(String group) {
        return player && groups.contains(group);
    }

    @Override
    public Traits traits() {
        return traits;
    }

    @Override
    public Optional<Key> carriedItem() {
        return carried;
    }

    @Override
    public boolean tell(Component message) {
        if (!player) {
            return false;
        }
        messages.add(message);
        return true;
    }

    @Override
    public boolean showSky(Sky sky) {
        if (!player) {
            return false;
        }
        shownSky = sky;
        return true;
    }

    /** What sky it is being shown, which is the real one until something changes it. */
    public Sky shownSky() {
        return shownSky;
    }

    @Override
    public Optional<Stockpile> inventory() {
        return inventory;
    }

    @Override
    public Optional<UUID> uniqueId() {
        return uniqueId;
    }

    @Override
    public List<Bystander> riders() {
        return List.copyOf(riders);
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean damage(double amount) {
        if (!present) {
            return false;
        }
        damageTaken += amount;
        return true;
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
    public boolean applyEffects(List<PotionDose> applied) {
        if (!present || applied.isEmpty()) {
            return false;
        }
        doses.addAll(applied);
        return true;
    }

    /** Puts it somewhere. */
    public SimpleBystander at(Vec3d position) {
        this.position = position;
        return this;
    }

    /** Makes it something that is not alive, such as a minecart or a dropped stack. */
    public SimpleBystander asObject() {
        this.living = false;
        return this;
    }

    /** Makes it a player with the given account name. */
    public SimpleBystander asPlayer(String name) {
        this.player = true;
        this.name = name;
        if (uniqueId.isEmpty()) {
            // Worked out from the name so that a test can ask where this player said they were
            // going without having to keep hold of an id it never chose.
            this.uniqueId = Optional.of(idFor(name));
        }
        return this;
    }

    /** Makes the game count it as hostile. */
    public SimpleBystander asMonster() {
        this.monster = true;
        return this;
    }

    /** Makes the game count it as an animal. */
    public SimpleBystander asAnimal() {
        this.animal = true;
        return this;
    }

    /** Puts a player in a permission group. */
    public SimpleBystander inGroup(String group) {
        groups.add(group);
        return this;
    }

    /** Gives it the properties a sign can ask about. */
    public SimpleBystander withTraits(Traits traits) {
        this.traits = traits;
        return this;
    }

    /** Puts something in its hand. */
    public SimpleBystander holding(ItemView item) {
        this.held = Optional.of(item);
        return this;
    }

    /** The unique id a player of this name is given here. */
    public static UUID idFor(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    /** Gives a player a pack, with whatever is in it. */
    public SimpleBystander carryingPack(SimpleStockpile inventory) {
        this.inventory = Optional.of(inventory);
        return this;
    }

    /** Gives it the unique id the server would know it by. */
    public SimpleBystander withUniqueId(UUID uniqueId) {
        this.uniqueId = Optional.of(uniqueId);
        return this;
    }

    /** Hides it, as spectating or vanishing would. */
    public SimpleBystander hidden() {
        this.visible = false;
        return this;
    }

    /** Marks it as tamed by somebody. */
    public SimpleBystander tamed() {
        this.tamed = true;
        return this;
    }

    /** Makes it a dropped stack of the named item. */
    public SimpleBystander carrying(Key item) {
        this.carried = Optional.of(item);
        return this;
    }

    /** Puts something on top of it. */
    public SimpleBystander carrying(Bystander rider) {
        riders.add(rider);
        return this;
    }

    /** Everything that has been said to it. */
    public List<Component> messages() {
        return List.copyOf(messages);
    }

    /** Everything that has been said to it, as plain text. */
    public List<String> plainMessages() {
        return messages.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList();
    }

    /** How much damage a chip has dealt it. */
    public double damageTaken() {
        return damageTaken;
    }

    /** The potion effects a chip has given it, in the order they were applied. */
    public List<PotionDose> doses() {
        return List.copyOf(doses);
    }
}
