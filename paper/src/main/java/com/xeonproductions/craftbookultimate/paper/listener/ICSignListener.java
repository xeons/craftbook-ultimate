package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLine;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import com.xeonproductions.craftbookultimate.paper.platform.RegionSchedulers;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Creates and destroys chips as players write and break signs.
 *
 * <p>Writing a recognised model reference on the second line of a wall sign turns it into a chip.
 * The sign is tidied as it is created: the first line becomes the chip's shorthand, and the
 * identifier line is rewritten in its canonical spelling so that later reads do not have to cope
 * with however the player typed it.
 */
@NullMarked
public final class ICSignListener implements Listener {

    /** The line a player writes the model reference on. */
    private static final int IDENTIFIER_LINE = ICManager.IDENTIFIER_LINE;

    /** The line the chip's shorthand is written to. */
    private static final int TITLE_LINE = 0;

    /** What a player writes to mean their own unique id. */
    private static final String OWN_IDENTITY = "uuid";

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final ICManager manager;
    private final RegionSchedulers schedulers;

    public ICSignListener(ICManager manager, RegionSchedulers schedulers) {
        this.manager = manager;
        this.schedulers = schedulers;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        if (event.getSide() != Side.FRONT) {
            return;
        }

        String identifier = PLAIN.serialize(event.line(IDENTIFIER_LINE));
        Optional<ICLine> parsed = ICLine.parse(identifier);
        if (parsed.isEmpty()) {
            return;
        }

        Optional<ICRegistry.Resolution> resolved = manager.registry().resolve(parsed.get());
        if (resolved.isEmpty()) {
            return;
        }

        ICDefinition definition = resolved.get().definition();
        Player player = event.getPlayer();

        if (!player.hasPermission(definition.permission())) {
            player.sendMessage(Component.text(
                    "You do not have permission to create the " + definition.name() + " chip.",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (Signs.facing(event.getBlock()).isEmpty()) {
            player.sendMessage(Component.text(
                    "A chip has to be on a wall sign, so that it has a block behind it to work from.",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        writeCanonicalLines(event, definition, parsed.get(), resolved.get().selfTriggering());

        player.sendMessage(Component.text("Created " + definition.name(), NamedTextColor.YELLOW));

        // The sign's own state is not written until after this event, so the chip cannot be
        // started from here. Picking it up on the next tick reads the finished sign.
        Block block = event.getBlock();
        schedulers.at(block.getLocation()).runLater(() -> manager.load(block), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        manager.unload(event.getBlock());
    }

    /**
     * Rewrites the sign into the spelling the rest of the plugin reads.
     *
     * <p>The identifier keeps whichever flags and mode the player wrote, gains the restricted
     * marker when the chip needs one, and is written as a model reference so that a sign created
     * by shorthand still reads back as its catalogue number.
     */
    private void writeCanonicalLines(
            SignChangeEvent event,
            ICDefinition definition,
            ICLine written,
            boolean selfTriggering) {

        ICLine canonical = definition.canonicalLine(written, selfTriggering);

        event.line(TITLE_LINE, Component.text(definition.shorthand()));
        event.line(IDENTIFIER_LINE, Component.text(canonical.render()));

        writePlayerIdentity(event, definition);
    }

    /**
     * Replaces the word {@code uuid} with the player's own unique id, on the one line a chip says
     * may carry it.
     *
     * <p>The substitution happens once, as the sign is made, because that is the only moment the
     * player who wrote it is known. Afterwards the sign reads as an ordinary namespace and nothing
     * else has to know where it came from.
     */
    private void writePlayerIdentity(SignChangeEvent event, ICDefinition definition) {
        definition.playerIdentityLine().ifPresent(line -> {
            String written = PLAIN.serialize(event.line(line)).trim();
            if (written.toLowerCase(Locale.ROOT).equals(OWN_IDENTITY)) {
                event.line(line, Component.text(event.getPlayer().getUniqueId().toString()));
            }
        });
    }
}
