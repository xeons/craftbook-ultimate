package com.xeonproductions.craftbookultimate.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xeonproductions.craftbookultimate.core.area.AreaAnchor;
import com.xeonproductions.craftbookultimate.core.area.AreaName;
import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.config.MechanicSettings;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.paper.area.Selections;
import com.xeonproductions.craftbookultimate.paper.area.StructureVault;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The commands that fill and empty the store a toggled area reads from.
 *
 * <p>A sign can only name an area somebody has already saved, so this is where every area comes
 * from. Picking out a region is part of it: there is no world editor to borrow a selection from,
 * so {@code pos1} and {@code pos2} pick out the block being looked at.
 *
 * <p>An area saved without a namespace is the player's own, kept under their name. Naming
 * {@code GLOBAL}, or somebody else, takes a permission.
 */
@NullMarked
public final class AreaCommands {

    /** What Brigadier takes as a command having done something. */
    private static final int SUCCESS = 1;

    /** How many area names are listed at once. */
    private static final int SHOWN = 40;

    /** The permission to save an area at all. */
    public static final String SAVE = "craftbook.area.save";

    /** The permission to save one under somebody else's name. */
    public static final String SAVE_OTHER = "craftbook.area.save.other";

    /** The permission to save one larger than the settings allow, or past the count they allow. */
    public static final String BYPASS_LIMIT = "craftbook.area.save.bypass-limit";

    /** The permission to delete an area. */
    public static final String DELETE = "craftbook.area.delete";

    /** The permission to delete one under somebody else's name. */
    public static final String DELETE_OTHER = "craftbook.area.delete.other";

    /** The permission to list the areas saved under a name. */
    public static final String LIST = "craftbook.area.list";

    /** The permission to list the areas saved under somebody else's name. */
    public static final String LIST_OTHER = "craftbook.area.list.other";

    private final StructureVault vault;
    private final Selections selections;
    private final Configuration configuration;

    public AreaCommands(StructureVault vault, Selections selections, Configuration configuration) {
        this.vault = vault;
        this.selections = selections;
        this.configuration = configuration;
    }

    /** The command that saves, deletes and lists areas. */
    public LiteralArgumentBuilder<CommandSourceStack> areaCommand() {
        return Commands.literal("area")
                .then(Commands.literal("pos1").executes(context -> pickCorner(context, true)))
                .then(Commands.literal("pos2").executes(context -> pickCorner(context, false)))
                .then(Commands.literal("save")
                        .requires(source -> source.getSender().hasPermission(SAVE))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> save(context, Optional.empty()))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .suggests(namespaces())
                                        .executes(context -> save(context, namespaceOf(context))))))
                .then(Commands.literal("delete")
                        .requires(source -> source.getSender().hasPermission(DELETE))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ownIds())
                                .executes(context -> delete(context, Optional.empty()))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .suggests(namespaces())
                                        .executes(context -> delete(context, namespaceOf(context))))))
                .then(Commands.literal("list")
                        .requires(source -> source.getSender().hasPermission(LIST))
                        .executes(context -> list(context, Optional.empty()))
                        .then(Commands.argument("namespace", StringArgumentType.word())
                                .suggests(namespaces())
                                .executes(context -> list(context, namespaceOf(context)))));
    }

    /** Picks out one corner of the box a save will cover. */
    private int pickCorner(CommandContext<CommandSourceStack> context, boolean firstCorner) {
        Optional<Player> player = playerOf(context);
        if (player.isEmpty()) {
            return 0;
        }

        Vec3i at = Selections.pointedAtBy(player.get());
        if (firstCorner) {
            selections.setFirst(player.get(), at);
        } else {
            selections.setSecond(player.get(), at);
        }

        tell(player.get(), "Corner " + (firstCorner ? "one" : "two") + " at "
                + at.x() + ", " + at.y() + ", " + at.z() + ".");
        selections.selectionOf(player.get()).ifPresent(anchor -> tell(
                player.get(), "That is " + anchor.volume() + " blocks."));
        return SUCCESS;
    }

    private int save(CommandContext<CommandSourceStack> context, Optional<String> asked) {
        Optional<Player> player = playerOf(context);
        if (player.isEmpty()) {
            return 0;
        }
        Player who = player.get();

        String namespace = asked.orElse(who.getName());
        if (!namespace.equalsIgnoreCase(who.getName()) && !who.hasPermission(SAVE_OTHER)) {
            error(who, "You may not save areas under that name.");
            return 0;
        }
        if (!AreaName.isUsableNamespace(namespace)) {
            error(who, "That is not a name areas can be kept under.");
            return 0;
        }

        String written = StringArgumentType.getString(context, "id");
        Optional<AreaName> name = AreaName.parse(namespace, written);
        if (name.isEmpty()) {
            error(who, "An area's name is letters, digits and underscores, up to thirteen of them.");
            return 0;
        }

        Optional<AreaAnchor> selection = selections.selectionOf(who);
        if (selection.isEmpty()) {
            error(who, "Pick out two corners first, with /area pos1 and /area pos2.");
            return 0;
        }

        AreaAnchor anchor = selection.get();
        MechanicSettings settings = configuration.settings().mechanics();
        boolean unlimited = who.hasPermission(BYPASS_LIMIT);

        if (!unlimited && !settings.allowsAreaOf(anchor.volume())) {
            error(who, "That is " + anchor.volume() + " blocks, and an area may hold "
                    + settings.maxAreaBlocks() + ".");
            return 0;
        }
        if (!unlimited
                && !vault.has(name.get())
                && !settings.allowsAnotherArea(vault.countIn(namespace))) {
            error(who, "There are already " + vault.countIn(namespace) + " areas under "
                    + namespace + ", and " + settings.maxAreasPerNamespace() + " are allowed.");
            return 0;
        }

        if (!vault.save(name.get(), anchor)) {
            error(who, "That area could not be saved.");
            return 0;
        }
        tell(who, "Saved " + name.get() + ", " + anchor.volume() + " blocks.");
        return SUCCESS;
    }

    private int delete(CommandContext<CommandSourceStack> context, Optional<String> asked) {
        CommandSender sender = context.getSource().getSender();
        String namespace = asked.orElseGet(() -> sender.getName());

        if (!namespace.equalsIgnoreCase(sender.getName()) && !sender.hasPermission(DELETE_OTHER)) {
            error(sender, "You may not delete areas under that name.");
            return 0;
        }

        Optional<AreaName> name = AreaName.parse(
                namespace, StringArgumentType.getString(context, "id"));
        if (name.isEmpty() || !vault.delete(name.get())) {
            error(sender, "There is no such area.");
            return 0;
        }
        tell(sender, "Deleted " + name.get() + ".");
        return SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> context, Optional<String> asked) {
        CommandSender sender = context.getSource().getSender();
        String namespace = asked.orElseGet(() -> sender.getName());

        if (!namespace.equalsIgnoreCase(sender.getName()) && !sender.hasPermission(LIST_OTHER)) {
            error(sender, "You may not list the areas under that name.");
            return 0;
        }
        if (!AreaName.isUsableNamespace(namespace)) {
            error(sender, "That is not a name areas can be kept under.");
            return 0;
        }

        List<String> ids = vault.idsIn(namespace);
        if (ids.isEmpty()) {
            error(sender, "There are no areas saved under " + namespace + ".");
            return 0;
        }

        sender.sendMessage(Component.text(
                ids.size() + " areas under " + namespace + ":", NamedTextColor.YELLOW));
        for (String id : ids.subList(0, Math.min(SHOWN, ids.size()))) {
            sender.sendMessage(Component.text("  " + id, NamedTextColor.GRAY));
        }
        if (ids.size() > SHOWN) {
            sender.sendMessage(Component.text(
                    "  and " + (ids.size() - SHOWN) + " more.", NamedTextColor.DARK_GRAY));
        }
        return SUCCESS;
    }

    /** Suggests the namespaces that hold anything, and the one everybody shares. */
    private SuggestionProvider<CommandSourceStack> namespaces() {
        return (context, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            String own = context.getSource().getSender().getName();
            for (String namespace : List.of(own, AreaName.GLOBAL)) {
                if (namespace.toLowerCase(Locale.ROOT).startsWith(typed)) {
                    builder.suggest(namespace);
                }
            }
            return builder.buildFuture();
        };
    }

    /** Suggests the areas the sender has saved under their own name. */
    private SuggestionProvider<CommandSourceStack> ownIds() {
        return (context, builder) -> {
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String id : vault.idsIn(context.getSource().getSender().getName())) {
                if (id.startsWith(typed)) {
                    builder.suggest(id);
                }
            }
            return builder.buildFuture();
        };
    }

    private static Optional<String> namespaceOf(CommandContext<CommandSourceStack> context) {
        return Optional.of(StringArgumentType.getString(context, "namespace"));
    }

    /** Whoever ran the command, where that was somebody standing in the world. */
    private static Optional<Player> playerOf(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        error(sender, "Only somebody standing in the world can do that.");
        return Optional.empty();
    }

    private static void tell(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.YELLOW));
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
