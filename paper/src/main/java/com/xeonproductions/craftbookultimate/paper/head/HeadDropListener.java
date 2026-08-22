// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.head;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.HeadSettings;
import com.xeonproductions.craftbookultimate.core.head.HeadDrops;
import com.xeonproductions.craftbookultimate.core.head.MobHeads;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Skull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * The head a death leaves behind.
 *
 * <p>The chance and the naming are {@link HeadDrops}; whose face a creature wears is
 * {@link MobHeads}. What is here is the two things only a server can do: read what the killer was
 * holding, and build the item.
 *
 * <p>The head goes into the death's own drops rather than being spawned beside them, so it obeys
 * whatever else the server does with a death — a plugin collecting drops collects it, and a death
 * whose drops are cancelled leaves no head either. A player death that keeps its inventory is the
 * exception, since its drops are swept away rather than dropped.
 *
 * <p>The fork also re-dropped a head when its <em>block</em> was mined, because the game of the day
 * dropped a blank one and lost whose it was. The game has kept the face on a mined head since the
 * flattening, so there is nothing left for that to fix and it is not here.
 */
@NullMarked
public final class HeadDropListener implements Listener {

    private final Configuration configuration;

    public HeadDropListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity died = event.getEntity();
        if (!configuration.settings()
                .runsMechanicIn(HeadDrops.NAME, died.getWorld().getName())) {
            return;
        }

        HeadSettings heads = configuration.settings().mechanics().heads();
        if (!heads.anythingAtAll()) {
            return;
        }

        Player killer = died.getKiller();
        if (heads.playerKillsOnly() && killer == null) {
            return;
        }
        if (killer != null && !killer.hasPermission(HeadDrops.KILL)) {
            return;
        }

        Key creature = died.getType().getKey();
        boolean player = died instanceof Player;
        if (player ? !heads.playerHeads() : !heads.mobHeads()) {
            return;
        }
        if (player && HeadDrops.isIgnored(heads.ignoredNames(), died.getName())) {
            return;
        }

        double chance = HeadDrops.chanceOf(
                heads.dropRate(), heads.lootingRateModifier(), lootingOf(killer));
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        headOf(creature, player ? (Player) died : null)
                .ifPresent(head -> drop(event, head));
    }

    /** Says whose head a placed one is, for anybody who wants to know. */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }
        if (!configuration.settings().runsMechanicIn(
                HeadDrops.NAME, event.getClickedBlock().getWorld().getName())) {
            return;
        }

        HeadSettings heads = configuration.settings().mechanics().heads();
        if (!heads.showNameOnClick()
                || !(event.getClickedBlock().getState(false) instanceof Skull skull)) {
            return;
        }

        ResolvableProfile profile = skull.getProfile();
        String name = profile == null ? null : profile.name();
        if (name == null || HeadDrops.isIgnored(heads.ignoredNames(), name)) {
            return;
        }

        // A creature's head is a player head wearing an account's face, so the account is looked
        // up before the name is read out: "the severed head of MHF_Cow" tells nobody anything.
        UUID owner = profile.uuid();
        Key creature = owner == null
                ? playerKey()
                : MobHeads.creatureOwning(owner).orElseGet(HeadDropListener::playerKey);

        event.getPlayer().sendMessage(Component.text(
                HeadDrops.describe(creature, name), NamedTextColor.YELLOW));
    }

    /**
     * Leaves a head where something died.
     *
     * <p>With the rest of the drops, so whatever else the server does with a death reaches the head
     * too — a plugin gathering drops gathers it, and a death whose drops are cleared leaves none.
     *
     * <p>Except where a player dies under {@code keepInventory}, which clears the drops and hands
     * everything back: a head put in there would be swept up with the rest and the killer would get
     * nothing. That one is dropped on the ground instead, which is where it was earned.
     */
    private static void drop(EntityDeathEvent event, ItemStack head) {
        if (event instanceof PlayerDeathEvent death && death.getKeepInventory()) {
            event.getEntity().getWorld()
                    .dropItemNaturally(event.getEntity().getLocation(), head);
            return;
        }
        event.getDrops().add(head);
    }

    /**
     * The head that comes off something, if anything does.
     *
     * <p>The game's own head first, then a player head wearing the face kept for the creature.
     * A creature with neither drops nothing, which is most of them.
     */
    private static Optional<ItemStack> headOf(Key creature, @org.jspecify.annotations.Nullable
            Player player) {

        Optional<Key> vanilla = HeadDrops.vanillaHead(creature);
        if (vanilla.isPresent() && player == null) {
            return materialOf(vanilla.get()).map(ItemStack::new);
        }

        Optional<Material> head = materialOf(HeadDrops.PLAYER_HEAD);
        if (head.isEmpty()) {
            return Optional.empty();
        }

        ResolvableProfile profile;
        String named;
        if (player != null) {
            profile = ResolvableProfile.resolvableProfile()
                    .uuid(player.getUniqueId())
                    .name(player.getName())
                    .build();
            named = HeadDrops.nameOf(creature, player.getName());
        } else {
            Optional<MobHeads.Owner> owner = MobHeads.ownerOf(creature);
            if (owner.isEmpty()) {
                return Optional.empty();
            }
            profile = ResolvableProfile.resolvableProfile()
                    .uuid(owner.get().id())
                    .name(owner.get().name())
                    .build();
            named = HeadDrops.nameOf(creature, "");
        }

        // The face itself is never written into the item, only whose it is. The server fills the
        // rest in when somebody first looks at the head and remembers it afterwards, so a world
        // full of heads costs one lookup each rather than a texture apiece on disk.
        ItemStack stack = new ItemStack(head.get());
        stack.setData(DataComponentTypes.PROFILE, profile);
        // Italics off, since the game slants a renamed item and a head is not a curiosity.
        stack.setData(DataComponentTypes.CUSTOM_NAME,
                Component.text(named).decoration(TextDecoration.ITALIC, false));
        return Optional.of(stack);
    }

    /** How much looting was on whatever did the killing, or none for a killer holding nothing. */
    private static int lootingOf(@org.jspecify.annotations.Nullable Player killer) {
        return killer == null
                ? 0
                : killer.getInventory().getItemInMainHand()
                        .getEnchantmentLevel(Enchantment.LOOTING);
    }

    /** The material a block or item name means, or nothing where the server has no such thing. */
    private static Optional<Material> materialOf(Key name) {
        NamespacedKey key = NamespacedKey.fromString(name.asString());
        return key == null ? Optional.empty() : Optional.ofNullable(Registry.MATERIAL.get(key));
    }

    private static Key playerKey() {
        return Key.key(Key.MINECRAFT_NAMESPACE, "player");
    }
}
