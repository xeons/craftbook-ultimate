package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.mechanic.PostedSign;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanic;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanics;
import com.xeonproductions.craftbookultimate.core.mechanic.SignReview;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.mechanic.MechanicDispatcher;
import com.xeonproductions.craftbookultimate.paper.mechanic.PlayerActor;
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
 * Checks a sign mechanic's sign as it is written.
 *
 * <p>A builder standing at the sign can fix what is wrong with it; a builder wondering later why
 * their bridge does nothing cannot. So the name is put into its proper spelling as the sign is
 * written, a builder without the permission to make one is told so rather than being left with a
 * sign that looks right and does nothing, and the mechanic itself gets a look at what was typed.
 */
@NullMarked
public final class MechanicSignListener implements Listener {

    /** The permission to have a mechanic build out of nothing rather than out of nearby chests. */
    public static final String ADMIN_PERMISSION = "craftbook.mechanic.admin";

    private final Configuration configuration;
    private final MechanicDispatcher dispatcher;

    public MechanicSignListener(Configuration configuration, MechanicDispatcher dispatcher) {
        this.configuration = configuration;
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        if (event.getSide() != Side.FRONT) {
            return;
        }

        SignLines written = SignLines.of(event.lines());
        Optional<SignMechanics.Claim> claim = SignMechanics.claiming(written);
        if (claim.isEmpty()) {
            return;
        }

        SignMechanic mechanic = claim.get().mechanic();
        Player builder = event.getPlayer();

        if (!SignMechanics.isRunning(
                mechanic, configuration.settings(), event.getBlock().getWorld().getName())) {
            refuse(event, builder, "The " + mechanic.name() + " mechanic is switched off here.");
            return;
        }
        if (!builder.hasPermission(mechanic.buildPermission())) {
            refuse(event, builder, "You do not have permission to make a "
                    + mechanic.name().toLowerCase(Locale.ROOT) + ".");
            return;
        }

        // However the builder typed it, the sign carries the proper spelling from here on.
        SignLines lines = written.withLine(PostedSign.NAME_LINE, claim.get().signName());

        if (lines.trimmedText(PostedSign.SUPPLY_LINE).equalsIgnoreCase(PostedSign.ADMIN)
                && !builder.hasPermission(ADMIN_PERMISSION)) {
            lines = lines.withLine(PostedSign.SUPPLY_LINE, Component.empty());
            builder.sendMessage(Component.text(
                    "You may not make one that supplies itself, so it will use nearby chests.",
                    NamedTextColor.RED));
        }

        SignReview review = mechanic.review(
                lines,
                new PlayerActor(builder),
                dispatcher.worldOf(event.getBlock().getWorld()));

        switch (review) {
            case SignReview.Refused refused -> refuse(event, builder, refused.why());
            case SignReview.Accepted accepted -> {
                write(event, accepted.lines());
                builder.sendMessage(Component.text(
                        "Made a " + mechanic.name().toLowerCase(Locale.ROOT) + ".",
                        NamedTextColor.YELLOW));
            }
        }
    }

    /** Puts the reviewed lines onto the sign being written. */
    private static void write(SignChangeEvent event, SignLines lines) {
        for (int line = 0; line < SignLines.LINE_COUNT; line++) {
            event.line(line, lines.line(line));
        }
    }

    private static void refuse(SignChangeEvent event, Player builder, String why) {
        builder.sendMessage(Component.text(why, NamedTextColor.RED));
        event.setCancelled(true);
    }
}
