// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.sponge.ic;

import com.xeonproductions.craftbookultimate.core.entity.Bystander;
import com.xeonproductions.craftbookultimate.core.entity.ItemView;
import com.xeonproductions.craftbookultimate.core.entity.PotionDose;
import com.xeonproductions.craftbookultimate.core.illusion.Sky;
import com.xeonproductions.craftbookultimate.core.math.Vec3d;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.sponge.stock.InventoryStockpile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.Monster;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.animal.Pig;
import org.spongepowered.api.entity.living.animal.Sheep;
import org.spongepowered.api.entity.living.monster.Creeper;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Equipable;
import org.spongepowered.api.item.inventory.ItemStackLike;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.math.vector.Vector3d;

/** Whatever is standing near a chip, as the chip sees it. */
@NullMarked
public record SpongeBystander(Entity entity) implements Bystander {

    private static final String GROUP_PREFIX = "group.";

    @Override
    public Key type() {
        return keyOf(entity.type());
    }

    @Override
    public Vec3d position() {
        Vector3d at = entity.position();
        return new Vec3d(at.x(), at.y(), at.z());
    }

    @Override
    public boolean isPlayer() {
        return entity instanceof ServerPlayer;
    }

    @Override
    public boolean isLiving() {
        return entity instanceof Living;
    }

    @Override
    public boolean isMonster() {
        return entity instanceof Monster;
    }

    @Override
    public boolean isAnimal() {
        return entity instanceof Animal;
    }

    @Override
    public boolean isTamed() {
        return entity.get(Keys.IS_TAMED).orElse(false);
    }

    /**
     * Whether a chip should notice somebody.
     *
     * <p>A spectator is not there to be sensed, and neither is anybody a vanish plugin has hidden.
     * Sponge carries vanishing as a proper state rather than as metadata a plugin agrees to set,
     * so the answer is the server's own rather than a convention.
     */
    @Override
    public boolean isVisible() {
        if (entity.get(Keys.GAME_MODE).filter(GameModes.SPECTATOR.get()::equals).isPresent()) {
            return false;
        }
        return !entity.get(Keys.VANISH_STATE).map(VanishState::invisible).orElse(false);
    }

    @Override
    public Optional<ItemView> heldItem() {
        if (!(entity instanceof Equipable equipable)) {
            return Optional.empty();
        }
        return equipable.equipped(EquipmentTypes.MAINHAND)
                .filter(held -> !held.isEmpty())
                .map(SpongeBystander::viewOf);
    }

    /** What a stack looks like to a chip reading names and lore off it. */
    public static ItemView viewOf(ItemStackLike stack) {
        Optional<String> displayName = stack.get(Keys.CUSTOM_NAME)
                .map(name -> PlainTextComponentSerializer.plainText().serialize(name));

        List<String> lore = new ArrayList<>();
        for (Component line : stack.get(Keys.LORE).orElse(List.of())) {
            lore.add(PlainTextComponentSerializer.plainText().serialize(line));
        }

        return new ItemView(keyOf(stack.type()), stack.quantity(), displayName, lore);
    }

    @Override
    public String name() {
        if (entity instanceof ServerPlayer player) {
            return player.name();
        }
        return entity.get(Keys.DISPLAY_NAME)
                .map(name -> PlainTextComponentSerializer.plainText().serialize(name))
                .orElseGet(() -> type().value());
    }

    @Override
    public boolean isInGroup(String group) {
        return entity instanceof ServerPlayer player
                && player.hasPermission(GROUP_PREFIX + group);
    }

    @Override
    public Traits traits() {
        if (entity instanceof Creeper creeper) {
            return Traits.ofCreeper(creeper.get(Keys.IS_CHARGED).orElse(false));
        }
        if (entity instanceof Pig pig) {
            return Traits.ofPig(pig.get(Keys.IS_SADDLED).orElse(false));
        }
        if (entity instanceof Sheep sheep) {
            Optional<DyeColor> colour = sheep.get(Keys.DYE_COLOR);
            if (colour.isPresent()) {
                return Traits.ofSheep(nameOf(colour.get()));
            }
        }
        return Traits.NONE;
    }

    @Override
    public Optional<Key> carriedItem() {
        return entity.get(Keys.ITEM_STACK_SNAPSHOT).map(held -> keyOf(held.type()));
    }

    @Override
    public boolean tell(Component message) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        player.sendMessage(message);
        return true;
    }

    /**
     * Showing somebody weather the world is not having.
     *
     * <p>Never, on this platform. Sponge has no per-player weather and no packet API to build one
     * out of, so the chips that ask are told no and show nobody anything. Reported rather than
     * pretended, so a builder gets the same answer as the sign.
     */
    @Override
    public boolean showSky(Sky sky) {
        return false;
    }

    @Override
    public Optional<Stockpile> inventory() {
        if (!(entity instanceof ServerPlayer player)) {
            return Optional.empty();
        }
        return Optional.of(new InventoryStockpile(player.inventory()));
    }

    @Override
    public Optional<UUID> uniqueId() {
        return Optional.of(entity.uniqueId());
    }

    @Override
    public List<Bystander> riders() {
        List<Bystander> riding = new ArrayList<>();
        for (Entity passenger : entity.get(Keys.PASSENGERS).orElse(List.of())) {
            riding.add(new SpongeBystander(passenger));
        }
        return riding;
    }

    @Override
    public boolean isPresent() {
        return !entity.isRemoved();
    }

    @Override
    public boolean moveTo(Vec3d position) {
        if (entity.isRemoved()) {
            return false;
        }
        // Keeps them in their own world and looking the way they were; only the place changes.
        entity.setPosition(new Vector3d(position.x(), position.y(), position.z()));
        return true;
    }

    @Override
    public boolean damage(double amount) {
        if (!(entity instanceof Living living) || living.isRemoved()) {
            return false;
        }
        return living.damage(amount, DamageSource.builder().type(DamageTypes.MAGIC).build());
    }

    @Override
    public boolean remove() {
        if (entity.isRemoved()) {
            return false;
        }
        entity.remove();
        return true;
    }

    /**
     * Dosing somebody with potion effects.
     *
     * <p>Sponge holds the whole list of what is affecting somebody under one key, so the doses are
     * added to what is already there and offered back together rather than one at a time.
     */
    @Override
    public boolean applyEffects(List<PotionDose> doses) {
        if (!(entity instanceof Living living) || living.isRemoved()) {
            return false;
        }

        List<PotionEffect> effects = new ArrayList<>(living.get(Keys.POTION_EFFECTS).orElse(List.of()));
        boolean applied = false;
        for (PotionDose dose : doses) {
            Optional<PotionEffectType> type = RegistryTypes.POTION_EFFECT_TYPE
                    .get()
                    .findValue(ResourceKey.of(dose.effect()));
            if (type.isEmpty()) {
                continue;
            }
            effects.add(PotionEffect.builder()
                    .potionType(type.get())
                    .duration(Ticks.of(dose.durationTicks()))
                    .amplifier(dose.amplifier())
                    .build());
            applied = true;
        }

        return applied && living.offer(Keys.POTION_EFFECTS, effects).isSuccessful();
    }

    private static String nameOf(DyeColor colour) {
        return RegistryTypes.DYE_COLOR.get().valueKey(colour).value().toLowerCase(Locale.ROOT);
    }

    static Key keyOf(EntityType<?> type) {
        return RegistryTypes.ENTITY_TYPE.get().valueKey(type);
    }

    static Key keyOf(ItemType type) {
        return RegistryTypes.ITEM_TYPE.get().valueKey(type);
    }
}
