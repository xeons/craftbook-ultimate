package com.xeonproductions.craftbookultimate.paper.debug;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

/**
 * The stick a builder points at a chip to find out what it is doing.
 *
 * <p>A plain stick carrying two pieces of persistent data: that it is one of these at all, and
 * which mode it is in. Kept in the item rather than against the player, so a stick can be handed to
 * somebody, left in a chest, or carried in each hand set differently — and so that a stick found
 * years later still works.
 *
 * <p>The item is checked by its data and never by its name, so renaming one in an anvil does not
 * break it and naming an ordinary stick does not make one.
 */
@NullMarked
public final class DebugStick {

    /** The permission to be given one, and to use it at all. */
    public static final String PERMISSION = "craftbook.debug";

    /** What the stick is made of. */
    private static final Material MATERIAL = Material.STICK;

    private final NamespacedKey isDebugStick;
    private final NamespacedKey mode;

    public DebugStick(org.bukkit.plugin.Plugin plugin) {
        this.isDebugStick = new NamespacedKey(plugin, "debug_stick");
        this.mode = new NamespacedKey(plugin, "debug_stick_mode");
    }

    /** A new stick, in the default mode. */
    public ItemStack create() {
        ItemStack stick = new ItemStack(MATERIAL);
        write(stick, DebugMode.DEFAULT);
        return stick;
    }

    /** Whether an item is one of these. */
    public boolean isOne(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(isDebugStick, PersistentDataType.BYTE);
    }

    /**
     * The mode a stick is in.
     *
     * <p>A stick whose mode cannot be read falls back to the default rather than refusing to work.
     * The mode is a convenience; losing it should cost a builder one click, not the tool.
     */
    public DebugMode modeOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return DebugMode.DEFAULT;
        }
        String written = meta.getPersistentDataContainer()
                .getOrDefault(mode, PersistentDataType.STRING, "");
        return DebugMode.byName(written).orElse(DebugMode.DEFAULT);
    }

    /** Puts a stick into a mode, rewriting its name and lore to match. */
    public void write(ItemStack stick, DebugMode chosen) {
        ItemMeta meta = stick.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(isDebugStick, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(mode, PersistentDataType.STRING, chosen.name());

        meta.displayName(Component.text("IC Debug Stick", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("  " + chosen.title(), NamedTextColor.GOLD)));

        meta.lore(List.of(
                line(chosen.description(), NamedTextColor.GRAY),
                Component.empty(),
                line("Right click a chip's sign to use it", NamedTextColor.DARK_GRAY),
                line("Crouch and right click the air to change mode", NamedTextColor.DARK_GRAY)));

        stick.setItemMeta(meta);
    }

    /**
     * Moves a stick to the next mode its holder is allowed to use.
     *
     * @return the mode it landed on, or empty if the holder may use no other mode
     */
    public Optional<DebugMode> cycle(ItemStack stick, org.bukkit.entity.Player holder) {
        DebugMode from = modeOf(stick);
        DebugMode next = from.next();

        while (next != from && !holder.hasPermission(next.permission())) {
            next = next.next();
        }
        if (next == from) {
            return Optional.empty();
        }

        write(stick, next);
        return Optional.of(next);
    }

    private static Component line(String text, NamedTextColor colour) {
        return Component.text(text, colour).decoration(TextDecoration.ITALIC, false);
    }
}
