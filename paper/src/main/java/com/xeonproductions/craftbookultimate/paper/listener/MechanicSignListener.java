package com.xeonproductions.craftbookultimate.paper.listener;

import com.xeonproductions.craftbookultimate.core.config.Configuration;
import com.xeonproductions.craftbookultimate.core.mechanic.PostedSign;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanic;
import com.xeonproductions.craftbookultimate.core.mechanic.SignMechanics;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
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
 * written, and a builder without the permission to make one is told so rather than being left
 * with a sign that looks right and does nothing.
 */
@NullMarked
public final class MechanicSignListener implements Listener {

    /** The permission to have a mechanic build out of nothing rather than out of nearby chests. */
    public static final String ADMIN_PERMISSION = "craftbook.mechanic.admin";

    private final Configuration configuration;

    public MechanicSignListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        if (event.getSide() != Side.FRONT) {
            return;
        }

        SignLines lines = SignLines.of(event.lines());
        Optional<SignMechanics.Claim> claim = SignMechanics.claiming(lines);
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
        event.line(PostedSign.NAME_LINE, Component.text(claim.get().signName()));

        if (lines.trimmedText(PostedSign.SUPPLY_LINE).equalsIgnoreCase(PostedSign.ADMIN)
                && !builder.hasPermission(ADMIN_PERMISSION)) {
            event.line(PostedSign.SUPPLY_LINE, Component.empty());
            builder.sendMessage(Component.text(
                    "You may not make one that supplies itself, so it will use nearby chests.",
                    NamedTextColor.RED));
        }

        builder.sendMessage(Component.text(
                "Made a " + mechanic.name().toLowerCase(Locale.ROOT) + ".",
                NamedTextColor.YELLOW));
    }

    private static void refuse(SignChangeEvent event, Player builder, String why) {
        builder.sendMessage(Component.text(why, NamedTextColor.RED));
        event.setCancelled(true);
    }
}
