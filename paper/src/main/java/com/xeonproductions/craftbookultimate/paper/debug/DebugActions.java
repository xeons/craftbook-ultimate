// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.paper.debug;

import com.xeonproductions.craftbookultimate.core.debug.DebugMode;
import com.xeonproductions.craftbookultimate.core.ic.BandAwareICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ChipReport;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.radio.Band;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.ic.ChipInspector;
import com.xeonproductions.craftbookultimate.paper.ic.ICInstance;
import com.xeonproductions.craftbookultimate.paper.ic.ICManager;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * What each debugging mode actually does to a chip.
 *
 * <p>One place, so that the stick and the commands cannot come to differ about what triggering a
 * chip means. The stick decides which of these to call from its own mode, a command names one
 * directly, and the menu offers them as clickable lines; none of them knows which way it was
 * reached.
 *
 * <p>Every one of these reads or writes blocks around the chip, so all of them belong to the region
 * owning its sign. The callers arrange that.
 */
@NullMarked
public final class DebugActions {

    /** How many fields are listed before the reply gives up. */
    private static final int FIELD_LIMIT = 40;

    /** How many chips a listing names before it counts the rest. */
    private static final int LIST_LIMIT = 20;

    private final ICManager manager;
    private final AreaOutline outline;

    public DebugActions(ICManager manager, AreaOutline outline) {
        this.manager = manager;
        this.outline = outline;
    }

    /** Runs a mode against a chip, permission having already been checked. */
    public void run(DebugMode mode, Audience audience, ICInstance chip) {
        switch (mode) {
            case MENU -> menu(audience, chip);
            case TRIGGER -> trigger(audience, chip);
            case AREA -> area(audience, chip);
            case FIELDS -> fields(audience, chip);
            case RELOAD -> reload(audience, chip);
            case TICKING -> ticking(audience, chip);
            case BAND -> band(audience, chip);
        }
    }

    /** Whether a mode has anything to say about a particular chip. */
    public boolean applies(DebugMode mode, ICInstance chip) {
        return switch (mode) {
            case AREA -> ChipInspector.area(chip).isPresent();
            case BAND -> chip.logic() instanceof BandAwareICLogic;
            default -> true;
        };
    }

    /**
     * The report, and then every other mode as something to click.
     *
     * <p>The report comes first because on its own it answers the question most of the time. The
     * buttons are for when it does not.
     */
    public void menu(Audience audience, ICInstance chip) {
        ChipReport report = ChipInspector.inspect(chip);
        report.describe().forEach(audience::sendMessage);

        if (!(audience instanceof Player player)) {
            return;
        }

        for (DebugMode mode : DebugMode.CYCLE) {
            if (mode == DebugMode.MENU
                    || !player.hasPermission(mode.permission())
                    || !applies(mode, chip)) {
                continue;
            }
            audience.sendMessage(button(mode, chip.world().getUID(), chip.signPosition()));
        }
    }

    /**
     * One clickable line.
     *
     * <p>The chip is looked up again when the button is pressed rather than captured by it. Minutes
     * may pass, and in that time the sign may have been broken, its chunk unloaded, or the whole
     * chip replaced by a different one.
     */
    private Component button(DebugMode mode, UUID world, Vec3i at) {
        return Component.text("  [" + mode.title() + "]", NamedTextColor.AQUA)
                .hoverEvent(Component.text(mode.description(), NamedTextColor.GRAY))
                .clickEvent(ClickEvent.callback(who -> chipAt(world, at).ifPresentOrElse(
                        chip -> run(mode, who, chip),
                        () -> who.sendMessage(Component.text(
                                "That chip is not loaded any more.", NamedTextColor.RED)))));
    }

    /** The chip at a place, if one is still loaded there. */
    public Optional<ICInstance> chipAt(UUID world, Vec3i at) {
        World found = Bukkit.getWorld(world);
        return found == null ? Optional.empty() : manager.at(Positions.toBlock(found, at));
    }

    /** Sets the chip off without touching an input. */
    public void trigger(Audience audience, ICInstance chip) {
        chip.trigger(-1);
        audience.sendMessage(Component.text(
                "Set off " + chip.definition().model()
                        + ". Its inputs are untouched, so it read them exactly as they stand.",
                NamedTextColor.GREEN));
    }

    /** Outlines the stretch of world the chip works on. */
    public void area(Audience audience, ICInstance chip) {
        Optional<Bounds> bounds = ChipInspector.area(chip);
        if (bounds.isEmpty()) {
            audience.sendMessage(Component.text(
                    "This chip does not say what area it works on.", NamedTextColor.RED));
            return;
        }

        audience.sendMessage(Component.text("Area  ", NamedTextColor.AQUA)
                .append(Component.text(bounds.get().describe(), NamedTextColor.GRAY)));

        if (audience instanceof Player player) {
            outline.show(player, chip.world(), bounds.get());
        }
    }

    /**
     * What the chip is holding internally.
     *
     * <p>Read by reflection, because a chip's own state is private and rightly so — a counter's
     * running total, a display's position in its script. There is no other way to see it, and
     * seeing it is most of what separates a chip that is broken from one that is doing exactly
     * what it was told.
     */
    public void fields(Audience audience, ICInstance chip) {
        Object logic = chip.logic();
        List<Component> said = new ArrayList<>();

        for (Class<?> level = logic.getClass();
                level != null && level != Object.class && said.size() < FIELD_LIMIT;
                level = level.getSuperclass()) {
            for (Field field : level.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && said.size() < FIELD_LIMIT) {
                    said.add(describeField(field, logic));
                }
            }
        }

        audience.sendMessage(Component.text(
                chip.definition().model() + " is holding:", NamedTextColor.GOLD));
        if (said.isEmpty()) {
            audience.sendMessage(Component.text(
                    "  nothing at all. This chip works entirely from its sign and its pins.",
                    NamedTextColor.GRAY));
            return;
        }
        said.forEach(audience::sendMessage);
    }

    private static Component describeField(Field field, Object holder) {
        String value;
        try {
            field.setAccessible(true);
            value = String.valueOf(field.get(holder));
        } catch (RuntimeException | IllegalAccessException e) {
            value = "(cannot be read)";
        }
        return Component.text("  " + field.getName() + "  ", NamedTextColor.AQUA)
                .append(Component.text(value, NamedTextColor.GRAY))
                .append(Component.text(
                        "  " + field.getType().getSimpleName(), NamedTextColor.DARK_GRAY));
    }

    /**
     * Stops the chip and starts it again.
     *
     * <p>Which is exactly what a chunk load does, so this is how to find out whether a chip that
     * misbehaves after a restart misbehaves because of the way it starts.
     */
    public void reload(Audience audience, ICInstance chip) {
        Block block = Positions.toBlock(chip.world(), chip.signPosition());
        manager.unload(block);
        Optional<ICInstance> started = manager.load(block);

        audience.sendMessage(started
                .map(fresh -> Component.text(
                        "Stopped and started " + fresh.definition().model()
                                + ", exactly as a chunk load would.",
                        NamedTextColor.GREEN))
                .orElse(Component.text(
                        "Stopped it, and it would not start again: the sign no longer describes a "
                                + "chip that is switched on here.",
                        NamedTextColor.RED)));
    }

    /** Every chip currently ticking on its own. */
    public void ticking(Audience audience, ICInstance near) {
        List<ICInstance> chips = manager.loaded().stream()
                .filter(ICInstance::isSelfTriggering)
                .toList();

        audience.sendMessage(Component.text(
                chips.size() + " of " + manager.loadedCount()
                        + " loaded chips are ticking on their own.",
                NamedTextColor.GOLD));

        for (ICInstance chip : chips.stream().limit(LIST_LIMIT).toList()) {
            boolean here = chip.signKey().equals(near.signKey());
            Vec3i at = chip.signPosition();
            audience.sendMessage(Component.text(
                    "  " + chip.definition().model() + "  "
                            + at.x() + "," + at.y() + "," + at.z()
                            + " in " + chip.world().getName() + (here ? "   <- this one" : ""),
                    here ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        }
        if (chips.size() > LIST_LIMIT) {
            audience.sendMessage(Component.text(
                    "  and " + (chips.size() - LIST_LIMIT) + " more.", NamedTextColor.GRAY));
        }
    }

    /**
     * What a wireless chip's band is called, and what the shared registry holds for it.
     *
     * <p>The two ends of a wireless pair cannot see one another, so a transmitter and a receiver
     * that disagree about their channel look exactly like a pair that agree. Reading the band off
     * each end in turn is the only way to tell.
     */
    public void band(Audience audience, ICInstance chip) {
        if (!(chip.logic() instanceof BandAwareICLogic aware)) {
            audience.sendMessage(Component.text("This is not a wireless chip.", NamedTextColor.RED));
            return;
        }

        Optional<Band> band = aware.band(chip.inspectionState());
        if (band.isEmpty()) {
            audience.sendMessage(Component.text(
                    "This chip's sign names no channel, so it talks to nothing.",
                    NamedTextColor.RED));
            return;
        }

        audience.sendMessage(Component.text("Band  ", NamedTextColor.AQUA)
                .append(Component.text(band.get().toString(), NamedTextColor.GOLD)));

        audience.sendMessage(chip.services().radio().signal(band.get())
                .map(on -> Component.text(
                        "      carrying " + (on ? "on" : "off"),
                        on ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .orElse(Component.text(
                        "      nothing has ever transmitted on it, so a receiver here holds "
                                + "whatever it is already showing.",
                        NamedTextColor.YELLOW)));
    }
}
