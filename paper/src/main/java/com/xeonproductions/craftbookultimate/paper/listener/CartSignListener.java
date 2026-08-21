// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.cart.CartSignRules;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.cart.BukkitCartWorld;
import com.xeonproductions.craftbookultimate.paper.cart.CartDispatcher;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Checks a cart mechanic's sign as it is written.
 *
 * <p>Two things are checked: whether the builder may make this mechanic at all, and whether what
 * they have written on it means anything. Both are worth doing here rather than later, because a
 * builder standing at the sign can fix it and a builder wondering why their junction never sorts
 * anything cannot.
 */
@NullMarked
public final class CartSignListener implements Listener {

    /** What a mechanic's permission is called, before its own name is added. */
    private static final String PERMISSION_PREFIX = "craftbook.cart.";

    private final CartDispatcher dispatcher;
    private final Configuration configuration;

    public CartSignListener(CartDispatcher dispatcher, Configuration configuration) {
        this.dispatcher = dispatcher;
        this.configuration = configuration;
    }

    /** The permission needed to build a mechanic. */
    public static String permissionFor(String mechanic) {
        return PERMISSION_PREFIX + mechanic.toLowerCase(Locale.ROOT);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        if (event.getSide() != Side.FRONT) {
            return;
        }

        SignLines lines = SignLines.of(event.lines());
        Optional<String> name = CartSignRules.nameOn(lines);
        if (name.isEmpty()) {
            return;
        }

        Player builder = event.getPlayer();
        String mechanic = name.get();

        if (!configuration.settings().allowsWorld(event.getBlock().getWorld().getName())
                || !configuration.settings().carts().allows(mechanic)) {
            refuse(event, builder, "The " + mechanic + " mechanic is switched off here.");
            return;
        }
        if (!builder.hasPermission(permissionFor(mechanic))) {
            refuse(event, builder, "You do not have permission to make a " + mechanic + ".");
            return;
        }

        Optional<String> problem = CartSignRules.problemWith(
                mechanic,
                lines,
                new BukkitCartWorld(event.getBlock().getWorld(), dispatcher.recipes()));
        if (problem.isPresent()) {
            refuse(event, builder, problem.get());
            return;
        }

        builder.sendMessage(Component.text("Made a " + mechanic + ".", NamedTextColor.YELLOW));
    }

    private static void refuse(SignChangeEvent event, Player builder, String why) {
        builder.sendMessage(Component.text(why, NamedTextColor.RED));
        event.setCancelled(true);
    }
}
