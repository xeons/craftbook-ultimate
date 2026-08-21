package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.paper.debug.DebugActions;
import com.xeonproductions.craftbookultimate.paper.debug.DebugMode;
import com.xeonproductions.craftbookultimate.paper.debug.DebugStick;
import com.xeonproductions.craftbookultimate.paper.ic.ICInstance;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.area.Selections;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * The debugging commands, which act on the chip you are looking at.
 *
 * <p>The stick and these do the same things through the same code. The stick is the better tool at
 * a workbench — point it and click — and these are the better tool for everything else: they can be
 * bound to a key, put in a command block, or run against somebody else's build without carrying
 * anything. Looking at the block rather than typing its coordinates is how {@code /area pos1}
 * already works, so it is what a builder here expects.
 *
 * <p>Each runs on the region owning the chip's sign, since every mode reads blocks around it.
 */
@NullMarked
public final class DebugCommands {

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    private final ICManager manager;
    private final DebugActions actions;
    private final DebugStick sticks;
    private final RegionSchedulers schedulers;

    public DebugCommands(
            ICManager manager,
            DebugActions actions,
            DebugStick sticks,
            RegionSchedulers schedulers) {
        this.manager = manager;
        this.actions = actions;
        this.sticks = sticks;
        this.schedulers = schedulers;
    }

    /** The whole {@code /craftbook debug} command. */
    public LiteralArgumentBuilder<CommandSourceStack> debugCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("debug")
                .requires(source -> source.getSender().hasPermission(DebugStick.PERMISSION))
                .executes(context -> onLookedAtChip(context, DebugMode.MENU))
                .then(Commands.literal("stick")
                        .requires(source -> source.getSender().hasPermission(DebugStick.PERMISSION))
                        .executes(this::giveStick));

        for (DebugMode mode : DebugMode.CYCLE) {
            root = root.then(Commands.literal(mode.name().toLowerCase(java.util.Locale.ROOT))
                    .requires(source -> source.getSender().hasPermission(mode.permission()))
                    .executes(context -> onLookedAtChip(context, mode)));
        }
        return root;
    }

    /** Hands over a debug stick. */
    private int giveStick(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            error(context, "Only a player can be handed a stick.");
            return 0;
        }

        ItemStack stick = sticks.create();
        if (!player.getInventory().addItem(stick).isEmpty()) {
            error(context, "You have nowhere to put it.");
            return 0;
        }

        player.sendMessage(Component.text(
                "Right click a chip's sign with it. Crouch and right click the air to change what "
                        + "it does.",
                NamedTextColor.GREEN));
        return SUCCESS;
    }

    /**
     * Runs a mode against the chip the sender is looking at.
     *
     * <p>Refused from anywhere that is not a player, because there is nothing to look along. The
     * stick has the same limit for the same reason, and neither is worth working around: a chip is
     * a thing in a place, and naming one without pointing at it means naming a world and three
     * numbers.
     */
    private int onLookedAtChip(CommandContext<CommandSourceStack> context, DebugMode mode) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            error(context, "Only a player can use this, since it acts on the chip you are looking "
                    + "at.");
            return 0;
        }

        Block block = Positions.toBlock(player.getWorld(), Selections.pointedAtBy(player));
        Optional<ICInstance> chip = manager.at(block);
        if (chip.isEmpty()) {
            error(context, "You are not looking at a chip. Look at a chip's own sign.");
            return 0;
        }

        ICInstance found = chip.get();
        if (!actions.applies(mode, found)) {
            error(context, "The " + mode.title() + " mode has nothing to say about a "
                    + found.definition().name() + ".");
            return 0;
        }

        schedulers.executeAt(found.world(), found.signPosition(), () -> {
            if (!found.isUnloaded()) {
                actions.run(mode, player, found);
            }
        });
        return SUCCESS;
    }

    private static void error(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().getSender().sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
