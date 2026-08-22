// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.copier;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.copier.Copiers;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The three signs that hand out copies, in one listener.
 *
 * <p>One rather than three, in the spirit of the cart and sign mechanics: all three answer the same
 * right-click, and telling them apart is reading one line. The rules they share — which names are
 * claimed, how many patterns a banner may carry, what a map number may be — are in {@link Copiers},
 * and what is here is the part only a server can do.
 *
 * <p>These are not on the {@code SignMechanic} seam, and deliberately. Every mechanic there is
 * about blocks: it puts a structure up, takes it down, or swaps one in. These are about the item in
 * somebody's hand, which a {@code MechanicVisit} does not carry and should not have to.
 */
@NullMarked
public final class CopierListener implements Listener {

    /** Where a banner may be, relative to the sign, for the sign to copy it. */
    private static final int[] BANNER_OFFSETS = {1, 2, -1, -2};

    /** How far from the sign an item frame is looked for. */
    private static final double FRAME_REACH = 2;

    private final Configuration configuration;

    public CopierListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }

        Optional<Sign> found = copierSign(event.getClickedBlock());
        if (found.isEmpty()) {
            return;
        }

        Sign sign = found.get();
        SignLines lines = Signs.read(sign);
        Optional<String> claimed = Copiers.claimed(lines.trimmedText(1));
        if (claimed.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        String name = claimed.get();
        if (!configuration.settings().mechanics().allows(name)
                || !player.hasPermission(Copiers.usePermission(name))) {
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        boolean copied = switch (name) {
            case Copiers.BANNER_SIGN -> copyBanner(player, sign, held);
            case Copiers.BOOK_SIGN -> copyBook(player, sign, held);
            case Copiers.MAP_SIGN -> copyMap(player, lines, held);
            default -> false;
        };

        if (copied) {
            event.setCancelled(true);
        }
    }

    /**
     * The copier sign a click landed on.
     *
     * <p>A click on the sign itself counts, and so does one on the block it hangs on — a builder
     * putting a sign on a bookshelf means the bookshelf to be the thing you click, and the sign to
     * be the label. Only the block behind is followed, so a sign around the corner from what was
     * clicked is not dragged in.
     */
    private static Optional<Sign> copierSign(Block clicked) {
        if (clicked.getState() instanceof Sign sign) {
            return Optional.of(sign);
        }

        for (BlockFace face : List.of(
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            Block beside = clicked.getRelative(face);
            if (!(beside.getState() instanceof Sign sign)) {
                continue;
            }
            // Only a sign hanging on the block that was clicked, rather than any sign touching it.
            if (beside.getBlockData() instanceof Directional facing
                    && beside.getRelative(facing.getFacing().getOppositeFace()).equals(clicked)) {
                return Optional.of(sign);
            }
        }
        return Optional.empty();
    }

    /** Copies the banner above or below the sign onto a blank one the player is holding. */
    private static boolean copyBanner(Player player, Sign sign, ItemStack held) {
        if (!(held.getItemMeta() instanceof BannerMeta blank) || !blank.getPatterns().isEmpty()) {
            return false;
        }

        Optional<Banner> banner = bannerNear(sign);
        if (banner.isEmpty()) {
            refuse(player, "There is no banner above or below the sign to copy.");
            return false;
        }

        Banner copying = banner.get();
        if (copying.getPatterns().size() > Copiers.MAX_BANNER_PATTERNS) {
            refuse(player, "That banner has more than " + Copiers.MAX_BANNER_PATTERNS
                    + " patterns, so it cannot be copied.");
            return false;
        }

        ItemStack made = new ItemStack(held.getType());
        made.editMeta(BannerMeta.class, meta -> meta.setPatterns(copying.getPatterns()));
        return handOver(player, held, made);
    }

    /** Copies the written book in an item frame near the sign onto a book and quill. */
    private static boolean copyBook(Player player, Sign sign, ItemStack held) {
        if (held.getType() != Material.WRITABLE_BOOK) {
            return false;
        }

        Optional<ItemStack> written = writtenBookNear(sign);
        if (written.isEmpty()) {
            refuse(player, "There is no written book in a frame near the sign to copy.");
            return false;
        }
        if (!(written.get().getItemMeta() instanceof BookMeta original)) {
            return false;
        }

        ItemStack made = new ItemStack(Material.WRITTEN_BOOK);
        made.editMeta(BookMeta.class, meta -> {
            meta.title(original.title());
            meta.author(original.author());
            meta.pages(original.pages());
            meta.setGeneration(nextGeneration(original.getGeneration()));
        });
        return handOver(player, held, made);
    }

    /**
     * Hands out the map the sign names.
     *
     * <p>The number is read off the sign every time rather than remembered, so an operator editing
     * the sign changes what the copier gives out without anything having to be rebuilt.
     */
    private static boolean copyMap(Player player, SignLines lines, ItemStack held) {
        if (held.getType() != Material.MAP && held.getType() != Material.FILLED_MAP) {
            return false;
        }

        OptionalInt number = Copiers.mapNumber(lines.trimmedText(Copiers.MAP_NUMBER_LINE));
        if (number.isEmpty()) {
            refuse(player, "The first line of that sign is not a map number.");
            return false;
        }

        MapView view = Bukkit.getMap(number.getAsInt());
        if (view == null) {
            refuse(player, "There is no map numbered " + number.getAsInt() + ".");
            return false;
        }

        ItemStack made = new ItemStack(Material.FILLED_MAP);
        made.editMeta(MapMeta.class, meta -> meta.setMapView(view));
        return handOver(player, held, made);
    }

    /**
     * A copy made once the blank is spent, or nothing where there is no room for it.
     *
     * <p>The blank is only taken once the copy is known to fit, so somebody with a full inventory
     * loses nothing by trying.
     */
    private static boolean handOver(Player player, ItemStack blank, ItemStack made) {
        if (!player.getInventory().addItem(made).isEmpty()) {
            refuse(player, "You have no room for it.");
            return false;
        }
        blank.setAmount(blank.getAmount() - 1);
        return true;
    }

    /** The banner a copier sign is labelling, above it or below it. */
    private static Optional<Banner> bannerNear(Sign sign) {
        Block at = sign.getBlock();
        for (int offset : BANNER_OFFSETS) {
            Block candidate = at.getRelative(0, offset, 0);
            if (candidate.getState() instanceof Banner banner) {
                return Optional.of(banner);
            }
        }
        return Optional.empty();
    }

    /** The written book in an item frame near the sign, if there is one. */
    private static Optional<ItemStack> writtenBookNear(Sign sign) {
        Block at = sign.getBlock();
        for (Entity nearby : at.getWorld().getNearbyEntities(
                at.getLocation().add(0.5, 0.5, 0.5), FRAME_REACH, FRAME_REACH, FRAME_REACH)) {
            if (nearby instanceof ItemFrame frame
                    && frame.getItem().getType() == Material.WRITTEN_BOOK) {
                return Optional.of(frame.getItem());
            }
        }
        return Optional.empty();
    }

    /**
     * What a copy of a book is marked as.
     *
     * <p>A copy of a copy is the last generation the game has, and copying one of those is refused
     * by the game itself — so anything already at or past that stays where it is rather than
     * wrapping round to an original.
     */
    private static BookMeta.Generation nextGeneration(BookMeta.@Nullable Generation generation) {
        if (generation == null || generation == BookMeta.Generation.ORIGINAL) {
            return BookMeta.Generation.COPY_OF_ORIGINAL;
        }
        return BookMeta.Generation.COPY_OF_COPY;
    }

    private static void refuse(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
