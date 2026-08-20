package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.entity.Roster;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.jspecify.annotations.NullMarked;

/**
 * Who is on a real server.
 *
 * <p>Only names leave this, and a name is an immutable string, so a chip in any region may ask
 * without reaching into anybody's world.
 */
@NullMarked
public record BukkitRoster(Server server) implements Roster {

    /** The metadata key the vanish plugins agree on. */
    private static final String VANISHED = "vanished";

    @Override
    public List<String> visibleNames() {
        List<String> names = new ArrayList<>();
        for (Player player : server.getOnlinePlayers()) {
            if (!isHidden(player)) {
                names.add(player.getName());
            }
        }
        return names;
    }

    /**
     * Whether a plugin has hidden this player.
     *
     * <p>Hiding is not something the server does, so the only way to know is to read what whichever
     * plugin did it left behind. The metadata flag is deprecated and is still the one every vanish
     * plugin sets, so it is what there is to read.
     */
    @SuppressWarnings("deprecation")
    private static boolean isHidden(Player player) {
        for (MetadataValue value : player.getMetadata(VANISHED)) {
            if (value.asBoolean()) {
                return true;
            }
        }
        return false;
    }
}
