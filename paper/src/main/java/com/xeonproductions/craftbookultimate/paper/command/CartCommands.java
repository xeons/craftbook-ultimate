// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.cart.CartMechanism;
import com.xeonproductions.craftbookultimate.core.cart.RailShape;
import com.xeonproductions.craftbookultimate.core.cart.mechanic.CartRouting;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.paper.cart.BukkitCart;
import com.xeonproductions.craftbookultimate.paper.cart.CartDispatcher;
import com.xeonproductions.craftbookultimate.paper.cart.CartMechanisms;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The commands a passenger uses.
 *
 * <p>Two of them, and both are things a rider does from inside a cart: saying where they are going
 * so the junctions route them, and picking a direction at a junction that has stopped to ask. The
 * third is for whoever is building a crafter and needs to know what a recipe is called.
 *
 * <p>Named as they always were, because people have them in their macros.
 */
@NullMarked
public final class CartCommands {

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    /** How fast a rider picking a direction is sent off. */
    private static final double CHOSEN_SPEED = 0.4;

    /** How many recipe names a search reports at once. */
    private static final int SHOWN_RESULTS = 20;

    private final CartDispatcher dispatcher;

    public CartCommands(CartDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * The command that says where a rider is going.
     *
     * <p>Every junction between here and there reads the one name, so this is the whole of telling
     * a railway where to take somebody.
     */
    public LiteralArgumentBuilder<CommandSourceStack> destinationCommand() {
        return Commands.literal("station")
                .requires(source -> source.getSender().hasPermission("craftbook.cart.station.use"))
                .executes(context -> {
                    describeDestination(context.getSource().getSender());
                    return SUCCESS;
                })
                .then(Commands.argument("station", StringArgumentType.word())
                        .executes(this::setDestination));
    }

    /**
     * The command that picks a direction at a junction.
     *
     * <p>The rider looks the way they want to go and asks; a rail that does not run that way
     * refuses, so a wrong turn is impossible rather than merely unhelpful.
     */
    public LiteralArgumentBuilder<CommandSourceStack> goCommand() {
        return Commands.literal("cbgo")
                .requires(source -> source.getSender().hasPermission("craftbook.cart.go"))
                .executes(this::goThatWay);
    }

    /** The command that looks up what a recipe is called on a crafter's sign. */
    public LiteralArgumentBuilder<CommandSourceStack> recipesCommand() {
        return Commands.literal("cbrecipes")
                .requires(source -> source.getSender().hasPermission("craftbook.cart.recipes"))
                .then(Commands.argument("query", StringArgumentType.word())
                        .executes(this::searchRecipes));
    }

    private void describeDestination(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a passenger has a destination.", NamedTextColor.RED));
            return;
        }
        Optional<String> going = dispatcher.stations().destination(player.getUniqueId());
        sender.sendMessage(going
                .map(name -> Component.text("You are heading for " + name + ".", NamedTextColor.GREEN))
                .orElseGet(() -> Component.text(
                        "You have not said where you are going. Try /station <name>.",
                        NamedTextColor.GRAY)));
    }

    private int setDestination(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a passenger has a destination.", NamedTextColor.RED));
            return 0;
        }

        String station = StringArgumentType.getString(context, "station");
        dispatcher.stations().setDestination(player.getUniqueId(), station);
        player.sendMessage(Component.text("Heading for " + station + ".", NamedTextColor.GREEN));
        player.sendMessage(Component.text(
                "You will have to say again after you log out.", NamedTextColor.GRAY));
        return SUCCESS;
    }

    private int goThatWay(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a passenger can pick a direction.", NamedTextColor.RED));
            return 0;
        }

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Minecart minecart)) {
            player.sendMessage(Component.text("You are not in a minecart.", NamedTextColor.RED));
            return 0;
        }

        Block under = minecart.getLocation().getBlock();
        Optional<CartMechanism> mechanism = CartMechanisms.atRail(under);
        if (mechanism.isEmpty() || !(under.getBlockData() instanceof Rail rail)) {
            player.sendMessage(Component.text("You are not on a rail.", NamedTextColor.RED));
            return 0;
        }

        BlockFace wanted = CartRouting.facingFromYaw(player.getYaw());
        RailShape shape = RailShape.valueOf(rail.getShape().name());
        if (!CartRouting.sendAlong(new BukkitCart(minecart), shape, wanted, CHOSEN_SPEED)) {
            player.sendMessage(Component.text(
                    "The rail does not run " + wanted.name().toLowerCase(Locale.ROOT) + ".",
                    NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(Component.text(
                "Going " + wanted.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GOLD));
        return SUCCESS;
    }

    private int searchRecipes(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String query = StringArgumentType.getString(context, "query");
        List<String> found = dispatcher.recipes().search(query);

        if (found.isEmpty()) {
            sender.sendMessage(Component.text("No recipe matches " + query + ".", NamedTextColor.RED));
            return 0;
        }

        sender.sendMessage(Component.text(
                found.size() + " recipes match " + query + ":", NamedTextColor.YELLOW));
        for (String name : found.subList(0, Math.min(SHOWN_RESULTS, found.size()))) {
            sender.sendMessage(Component.text("  " + name, NamedTextColor.GRAY));
        }
        if (found.size() > SHOWN_RESULTS) {
            sender.sendMessage(Component.text(
                    "  and " + (found.size() - SHOWN_RESULTS) + " more.", NamedTextColor.DARK_GRAY));
        }
        return SUCCESS;
    }
}
