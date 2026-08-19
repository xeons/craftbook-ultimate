package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/**
 * The commands for reading the chip catalogue.
 *
 * <p>A builder standing in front of a sign wants to know what a model number does, what its pins
 * are called and how it wants its lines filled in. Until the documentation generator exists this
 * is where that lives.
 */
@NullMarked
public final class CatalogueCommands {

    /** How many chips a page of the listing holds. */
    private static final int PAGE_SIZE = 8;

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    private final ICRegistry registry;

    public CatalogueCommands(ICRegistry registry) {
        this.registry = registry;
    }

    /** The plugin's own command, under which everything not tied to one chip lives. */
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("craftbook")
                .requires(source -> source.getSender().hasPermission("craftbook.command"))
                .executes(context -> {
                    summary(context.getSource().getSender());
                    return SUCCESS;
                })
                .then(Commands.literal("ic")
                        .then(Commands.literal("list")
                                .executes(context -> list(context, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> list(
                                                context, IntegerArgumentType.getInteger(context, "page")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("chip", StringArgumentType.word())
                                        .suggests(models())
                                        .executes(this::info))));
    }

    private void summary(CommandSender sender) {
        sender.sendMessage(Component.text("CraftBook Ultimate", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(
                registry.size() + " integrated circuits registered. Try /craftbook ic list.",
                NamedTextColor.GRAY));
    }

    private int list(CommandContext<CommandSourceStack> context, int page) {
        CommandSender sender = context.getSource().getSender();
        List<ICDefinition> all = registry.definitions();
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        if (page > pages) {
            sender.sendMessage(Component.text(
                    "There are only " + pages + " pages.", NamedTextColor.RED));
            return 0;
        }

        sender.sendMessage(Component.text(
                "Integrated circuits, page " + page + " of " + pages, NamedTextColor.YELLOW));

        int from = (page - 1) * PAGE_SIZE;
        for (ICDefinition definition : all.subList(from, Math.min(from + PAGE_SIZE, all.size()))) {
            sender.sendMessage(Component.text("  " + definition.model(), NamedTextColor.AQUA)
                    .append(Component.text("  " + definition.name(), NamedTextColor.GRAY)));
        }
        return SUCCESS;
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String wanted = StringArgumentType.getString(context, "chip");

        Optional<ICDefinition> found = registry.byModel(wanted);
        if (found.isEmpty()) {
            found = registry.byShorthand(wanted);
        }
        if (found.isEmpty()) {
            sender.sendMessage(Component.text(
                    "No chip is called " + wanted + ".", NamedTextColor.RED));
            return 0;
        }

        ICDefinition definition = found.get();
        sender.sendMessage(Component.text(
                definition.model() + "  " + definition.name(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  " + definition.description(), NamedTextColor.GRAY));
        line(sender, "Shorthand", "=" + definition.shorthand());
        line(sender, "Pins", definition.defaultLayout().code());
        line(sender, "Permission", definition.permission());

        if (definition.restricted()) {
            line(sender, "Restricted", "needs elevated permission to build");
        }
        if (definition.requiresAuthorisation()) {
            line(sender, "Authorisation", "will not act until its area is clear");
        }
        if (definition.supportsSelfTriggering()) {
            line(sender, "Self triggering", definition.selfTriggeringModel()
                    .map(model -> "add S to the model, or use " + model)
                    .orElse("add S to the model"));
        }
        return SUCCESS;
    }

    private static void line(CommandSender sender, String label, String value) {
        sender.sendMessage(Component.text("  " + label + ": ", NamedTextColor.DARK_GRAY)
                .append(Component.text(value, NamedTextColor.WHITE)));
    }

    /** Suggests the model numbers and shorthands the catalogue holds. */
    private SuggestionProvider<CommandSourceStack> models() {
        return (context, builder) -> {
            String typed = builder.getRemaining().toUpperCase(Locale.ROOT);
            for (ICDefinition definition : registry.definitions()) {
                if (definition.model().startsWith(typed)) {
                    builder.suggest(definition.model());
                }
            }
            return builder.buildFuture();
        };
    }
}
