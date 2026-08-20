package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.entity.PotionDose;
import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.paper.stock.ContainerStockpile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Registry;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;

/**
 * Something standing near a chip in a real world.
 *
 * <p>Damage is dealt as magic, which is the game's own way of hurting something past its armour.
 * That matches what these chips have always done: a trap under a floor is not meant to be defeated
 * by wearing a helmet.
 */
@NullMarked
public record BukkitBystander(Entity entity) implements Bystander {

    /** The permission node a player's group membership is read from. */
    private static final String GROUP_PREFIX = "group.";

    /**
     * The metadata key the vanish plugins agree on.
     *
     * <p>Hiding a player is not something the server itself does, so the only way to know is to
     * read what whichever plugin did it left behind. Every one of them sets this.
     */
    private static final String VANISHED = "vanished";

    @Override
    public Key type() {
        return entity.getType().getKey();
    }

    @Override
    public Vec3d position() {
        return new Vec3d(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ());
    }

    @Override
    public boolean isPlayer() {
        return entity instanceof Player;
    }

    @Override
    public boolean isLiving() {
        return entity instanceof LivingEntity;
    }

    @Override
    public boolean isMonster() {
        return entity instanceof Monster;
    }

    @Override
    public boolean isAnimal() {
        return entity instanceof Animals;
    }

    @Override
    public boolean isTamed() {
        return entity instanceof Tameable tameable && tameable.isTamed();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isVisible() {
        if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        // The metadata flag is deprecated and is still the one every vanish plugin sets, so it is
        // what there is to read.
        for (MetadataValue value : entity.getMetadata(VANISHED)) {
            if (value.asBoolean()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Optional<ItemView> heldItem() {
        if (!(entity instanceof LivingEntity living)) {
            return Optional.empty();
        }
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) {
            return Optional.empty();
        }
        return viewOf(equipment.getItemInMainHand());
    }

    /**
     * What a stack looks like to a chip.
     *
     * @return the stack, or empty if there is nothing there
     */
    public static Optional<ItemView> viewOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> displayName = Optional.empty();
        List<String> lore = List.of();
        ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
        if (meta != null) {
            if (meta.hasDisplayName() && meta.displayName() != null) {
                displayName = Optional.of(
                        PlainTextComponentSerializer.plainText().serialize(meta.displayName()));
            }
            if (meta.hasLore() && meta.lore() != null) {
                List<String> lines = new ArrayList<>();
                for (Component line : meta.lore()) {
                    lines.add(PlainTextComponentSerializer.plainText().serialize(line));
                }
                lore = lines;
            }
        }

        return Optional.of(new ItemView(stack.getType().getKey(), stack.getAmount(), displayName, lore));
    }

    @Override
    public String name() {
        if (entity instanceof Player player) {
            return player.getName();
        }
        return PlainTextComponentSerializer.plainText().serialize(entity.name());
    }

    @Override
    public boolean isInGroup(String group) {
        return entity instanceof Player player && player.hasPermission(GROUP_PREFIX + group);
    }

    @Override
    public Traits traits() {
        if (entity instanceof Creeper creeper) {
            return Traits.ofCreeper(creeper.isPowered());
        }
        if (entity instanceof Pig pig) {
            return Traits.ofPig(pig.hasSaddle());
        }
        if (entity instanceof Sheep sheep && sheep.getColor() != null) {
            return Traits.ofSheep(sheep.getColor().name().toLowerCase(Locale.ROOT));
        }
        return Traits.NONE;
    }

    @Override
    public Optional<Key> carriedItem() {
        if (!(entity instanceof Item item)) {
            return Optional.empty();
        }
        return Optional.of(item.getItemStack().getType().getKey());
    }

    @Override
    public boolean tell(Component message) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        player.sendMessage(message);
        return true;
    }

    @Override
    public boolean showSky(Sky sky) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        PlayerSkies.show(player, sky);
        return true;
    }

    @Override
    public Optional<Stockpile> inventory() {
        if (!(entity instanceof Player player)) {
            return Optional.empty();
        }
        return Optional.of(new ContainerStockpile(player.getInventory()));
    }

    @Override
    public Optional<UUID> uniqueId() {
        return Optional.of(entity.getUniqueId());
    }

    @Override
    public List<Bystander> riders() {
        List<Bystander> riding = new ArrayList<>();
        for (Entity passenger : entity.getPassengers()) {
            riding.add(new BukkitBystander(passenger));
        }
        return riding;
    }

    @Override
    public boolean isPresent() {
        return entity.isValid();
    }

    @Override
    public boolean damage(double amount) {
        if (!(entity instanceof LivingEntity living) || !living.isValid()) {
            return false;
        }
        living.damage(amount, DamageSource.builder(DamageType.MAGIC).build());
        return true;
    }

    @Override
    public boolean remove() {
        if (!entity.isValid()) {
            return false;
        }
        entity.remove();
        return true;
    }

    @Override
    public boolean applyEffects(List<PotionDose> doses) {
        if (!(entity instanceof LivingEntity living) || !living.isValid()) {
            return false;
        }

        boolean applied = false;
        for (PotionDose dose : doses) {
            PotionEffectType effect = Registry.MOB_EFFECT.get(dose.effect());
            if (effect == null) {
                continue;
            }
            applied |= living.addPotionEffect(
                    new PotionEffect(effect, dose.durationTicks(), dose.amplifier()));
        }
        return applied;
    }
}
