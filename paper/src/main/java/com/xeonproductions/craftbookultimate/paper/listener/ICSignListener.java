package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.config.Settings;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICLine;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import com.xeonproductions.craftbookultimate.paper.mechanic.PlayerActor;
import com.xeonproductions.craftbookultimate.paper.ic.ChipTitle;
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
    private static final int TITLE_LINE = ChipTitle.LINE;

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

        Block block = event.getBlock();

        // Whether this sign was already a chip decides two things: what the builder is told, and
        // whether rubbing the model number out has to stop something.
        boolean wasChip = manager.at(block).isPresent();

        String identifier = PLAIN.serialize(event.line(IDENTIFIER_LINE));
        Optional<ICRegistry.Resolution> resolved =
                ICLine.parse(identifier).flatMap(line -> manager.registry().resolve(line));
        if (resolved.isEmpty()) {
            if (wasChip) {
                reconcile(block);
            }
            return;
        }

        ICLine parsed = ICLine.parse(identifier).orElseThrow();
        ICDefinition definition = resolved.get().definition();
        Player player = event.getPlayer();
        Settings settings = manager.services().configuration().settings();

        if (!settings.allowsWorld(block.getWorld().getName())
                || !settings.allowsChip(definition.allModels())) {
            player.sendMessage(Component.text(
                    "The " + definition.name() + " chip is switched off here.",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission(definition.permission())) {
            player.sendMessage(Component.text(
                    "You do not have permission to create the " + definition.name() + " chip.",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (Signs.facing(block).isEmpty()) {
            player.sendMessage(Component.text(
                    "A chip has to be on a wall sign, so that it has a block behind it to work from.",
                    NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        SignLines written = SignLines.of(event.lines());

        if (refusedForMissingLines(event, definition, written, player)) {
            return;
        }

        // Asked of a throwaway logic instance: the chip itself is not made until the sign has
        // been written, and a chip naming something that is not there should never be made at all.
        Optional<String> problem = definition
                .newLogic()
                .reviewSign(written, manager.services(), new PlayerActor(player));
        if (problem.isPresent()) {
            player.sendMessage(Component.text(problem.get(), NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        writeCanonicalLines(event, definition, parsed, resolved.get().selfTriggering());

        player.sendMessage(Component.text(
                (wasChip ? "Rebuilt " : "Created ") + definition.name(), NamedTextColor.YELLOW));

        reconcile(block);
    }

    /**
     * Builds the chip this sign now describes, replacing whatever was there.
     *
     * <p>Left until the next tick because the sign's own state is not written until after this
     * event, so reading it here would read the text the builder is replacing.
     *
     * <p>Replacing rather than adding is what makes an edit take effect. A chip is built from the
     * sign as it stood, and carries that reading for as long as it runs — its wiring, its mode,
     * whether it ticks — so a chip left in place after its sign changed is a chip running under
     * text nobody can see any more.
     */
    private void reconcile(Block block) {
        schedulers.at(block.getLocation()).runLater(() -> manager.reload(block), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        manager.unload(event.getBlock());
    }

    /**
     * Checks the sign against what the chip says its lines are for.
     *
     * <p>A blank line the chip cannot work without refuses the sign, because the alternative is a
     * chip that looks built and does nothing — a melody with no file named returns before it plays
     * a note, and says so to nobody. A blank line the chip has a default for is allowed and
     * mentioned, so a builder who meant to fill it in finds out while they are still standing
     * there.
     *
     * <p>Only signs being written now are ever seen here. A sign already in the world is read
     * through the chip manager on chunk load and never comes past this, so nothing existing can be
     * refused by a rule added later.
     *
     * @return true if the sign was refused
     */
    private static boolean refusedForMissingLines(
            SignChangeEvent event, ICDefinition definition, SignLines written, Player player) {

        LineReview review = LineReview.of(definition, written);

        if (review.broken()) {
            player.sendMessage(Component.text(
                    "The " + definition.name() + " chip needs more than that.", NamedTextColor.RED));
            review.missing().forEach(blank ->
                    player.sendMessage(Component.text("  " + blank.said(), NamedTextColor.RED)));
            event.setCancelled(true);
            return true;
        }

        review.defaulted().forEach(blank ->
                player.sendMessage(Component.text("  " + blank.said(), NamedTextColor.YELLOW)));
        return false;
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
