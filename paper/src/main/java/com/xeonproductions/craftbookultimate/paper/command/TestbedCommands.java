package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.math.BlockFace;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.testbed.Testbed;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.testbed.TestbedBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The command that builds a working rig for every chip in the catalogue.
 *
 * <p>Built rather than shipped as a file, for the reason the catalogue page is generated rather
 * than written: a chip added later would be missing from a saved plane and nobody would notice,
 * and a rig wired from a remembered pin layout rather than the real one reads as a broken chip.
 * Built from {@link ICRegistry}, the bed is right by construction and can be rebuilt after any
 * change.
 *
 * <p>It writes a great many blocks over whatever is already there, so it is an operator's command
 * and asks to be told twice.
 */
@NullMarked
public final class TestbedCommands {

    /** The permission to build one. */
    public static final String BUILD = "craftbook.testbed";

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    /** How far in front of whoever asked the plane starts. */
    private static final int DISTANCE = 4;

    /** How many chips are named individually before the reply gives up and counts them. */
    private static final int NAMES_SHOWN = 8;

    private final ICRegistry registry;
    private final TestbedBuilder builder;

    public TestbedCommands(ICRegistry registry, TestbedBuilder builder) {
        this.registry = registry;
        this.builder = builder;
    }

    /** The whole {@code /craftbook testbed} command. */
    public LiteralArgumentBuilder<CommandSourceStack> testbedCommand() {
        return Commands.literal("testbed")
                .requires(source -> source.getSender().hasPermission(BUILD))
                .executes(this::describe)
                .then(Commands.literal("build")
                        .executes(context -> build(context, 10))
                        .then(Commands.argument("columns", IntegerArgumentType.integer(1, 40))
                                .executes(context -> build(
                                        context, IntegerArgumentType.getInteger(context, "columns")))));
    }

    /** Says what building one would do, without doing it. */
    private int describe(CommandContext<CommandSourceStack> context) {
        Testbed plan = Testbed.plan(registry, Vec3i.ZERO, BlockFace.SOUTH);
        Testbed.Ground ground = plan.ground();

        tell(context, "A test bed of " + plan.size() + " rigs, "
                + plan.columns() + " to a row and " + plan.rows() + " rows deep.");
        tell(context, "It covers " + (ground.to().x() - ground.from().x() + 1) + " by "
                + (ground.to().z() - ground.from().z() + 1) + " blocks and overwrites everything "
                + "standing there.");

        tell(context, plan.unconfigured().size() + " have a blank third and fourth line. Most "
                + "want nothing there; the rest are yours to fill in.");

        List<ICDefinition> needing = plan.needingSetup();
        if (!needing.isEmpty()) {
            warn(context, needing.size() + " need a file an operator has to supply before they "
                    + "will do anything: " + names(needing) + ". Their labels say so.");
        }

        tell(context, "Run /craftbook testbed build to put it in front of you.");
        return SUCCESS;
    }

    /** Builds one in front of whoever asked. */
    private int build(CommandContext<CommandSourceStack> context, int columns) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            error(context, "Only a player can build a test bed, since it goes in front of them.");
            return 0;
        }

        BlockFace facing = BlockFace.SOUTH;
        Vec3i origin = Positions.toDomain(player.getLocation()).add(0, -1, DISTANCE);

        Testbed plan = Testbed.plan(registry, origin, facing, columns);
        int started = builder.build(player.getWorld(), plan);

        tell(context, "Built " + plan.size() + " rigs, and " + started + " chips came alive.");

        List<ICDefinition> needing = plan.needingSetup();
        if (!needing.isEmpty()) {
            warn(context, needing.size() + " wait on a file an operator supplies: " + names(needing));
        }
        return SUCCESS;
    }

    /** The first few model numbers, and a count of the rest. */
    private static String names(List<ICDefinition> chips) {
        StringBuilder listed = new StringBuilder();
        for (ICDefinition chip : chips.stream().limit(NAMES_SHOWN).toList()) {
            if (!listed.isEmpty()) {
                listed.append(", ");
            }
            listed.append(chip.model());
        }
        if (chips.size() > NAMES_SHOWN) {
            listed.append(" and ").append(chips.size() - NAMES_SHOWN).append(" more");
        }
        return listed.toString();
    }

    private static void tell(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().getSender().sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static void warn(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().getSender().sendMessage(Component.text(message, NamedTextColor.YELLOW));
    }

    private static void error(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().getSender().sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
