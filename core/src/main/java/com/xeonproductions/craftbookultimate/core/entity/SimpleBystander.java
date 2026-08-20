package com.xeonproductions.craftbookultimate.core.entity;

import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
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
    private final Set<String> groups = new LinkedHashSet<>();
    private final List<Bystander> riders = new ArrayList<>();
    private final List<PotionDose> doses = new ArrayList<>();
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

    /** How much damage a chip has dealt it. */
    public double damageTaken() {
        return damageTaken;
    }

    /** The potion effects a chip has given it, in the order they were applied. */
    public List<PotionDose> doses() {
        return List.copyOf(doses);
    }
}
